# Header External Login Module and Open ID Connect Sample Integration

This document describes the process for integrating Open ID Connect with a Sling-based CMS called 
[Peregrine CMS](https://www.peregrine-cms.com/content/sites/peregrine.html) with OpenID Connect. The sample architecture
includes the following systems:

* [Google's OAuth 2.0 API](https://developers.google.com/identity/protocols/oauth2/openid-connect) 
* Apache HTTPD with [mod_auth_openidc](https://github.com/zmartzone/mod_auth_openidc) 
* [Peregrine CMS](Header Authentication Handler Configuration)
* Header External Login Module (this project)

## Prerequisites

* Java JDK 8
* Apache Maven 3.5+

### Peregrine CMS Installation

1. Visit [https://github.com/headwirecom/peregrine-cms](https://github.com/headwirecom/peregrine-cms) and install
   Peregrine per the README.

### Header External Login Module Installation

1. Deploy this project to Peregrine.

```
$ mvn clean install sling:install
```

### Header External Login Module Configuration

1. Log into the [Apache Sling Configuration Console](http://localhost:8080/system/console/configMgr) and create a 
   configuration for the _Apache Felix JAAS Configuration Factory_ (`org.apache.felix.jaas.Configuration.factory`) with
   the following values:
   
   * Control Flag (jaas.controlFlag) = `Sufficient`
   * Ranking (jaas.ranking) = `5000`
   * Realm Name (jaas.realmName) = `jackrabbit.oak`
   * Class Name (jaas.classname) = `com.peregrine.sling.auth.header.HeaderExternalLoginModule`
   * Options (jaas.options) = _leave empty_
  
   *NOTE:* To validate this JAAS configuration, visit [http://localhost:8080/system/console/jaas](http://localhost:8080/system/console/jaas)
   and check that `com.peregrine.sling.auth.header.HeaderExternalLoginModule` has the highest rank. In a default Sling environment, the rank 
   should be greater than `1000` so that this login module runs first. 
    
2. Create a configuration for the _Header Authentication Handler Configuration_ with the following values. The only value that should
   be changed is the _Shared Secret_. Set this to a value of your choosing. The shared secret value will be used later in the Apache
   configuration.

   * Login Cookie (header.auth.login.cookie) = `mod_auth_openidc_session`
   * Remote User Header (header.auth.remote.user.header) = `REMOTE_USER`
   * Shared Secret (header.auth.shared.secret) = _mysecret_
   * Username Whitelist Pattern (header.auth.username.whitelist) = `^[A-Za-z0-9+_.-]+@(.+)$`
   * User Profile Header Whitelist Pattern = `^OIDC_CLAIM_(.+)$` 
   
   *NOTE:* With the exception of the _Shared Secret_, all values above should be used as defined to work correctly with 
   `mod_auth_openidc`.
   
3. Create a configuration for _Apache Jackrabbit Oak Default Sync Handler_.
   (`org.apache.jackrabbit.oak.spi.security.authentication.external.impl.DefaultSyncHandler`).

   * Sync Handler Name (handler-name) = `default`
   * User auto membership (user.autoMembership) = `all_tenants` 
   * User Property Mapping (user.propertyMapping) = 
     * `preferences/firstLogin="true"`
     * `profile/email=OIDC_CLAIM_email`
     * `profile/avatar=OIDC_CLAIM_picture`
   * User Path Prefix (user.pathPrefix) = `tenants`
   * Leave all other defaults as-is.
   
   *NOTE:* By default, `user.propertyMapping` contains a mapping for `rep:fullname=cn`. It can be removed since this
   only applies to LDAP. Additionally, you can define two types of user property mappings. Static property key/value
   pairs as shown with `preferences/firstLogin` and dynamic mappings as shown with the `OIDC_CLAIM_*` properties. Any
   HTTP header that is allowed by the _User Profile Header Whitelist Pattern_ in step #2 can be mapped to the repository.
   
###  Configure Google's OpenID Connect  

Refer to Google's [OpenID Connect](https://developers.google.com/identity/protocols/oauth2/openid-connect) documentation
and complete the following steps:

1. Obtain OAuth 2.0 credentials
2. Set a redirect URI
3. Customize the user consent screen

### Apache Installation and Configuration

This section assumes that Apache will be installed on Ubuntu.

1. Install Apache and the `mod_auth_openidc` module.

```
$ sudo apt update -q  && apt-get install -q -y \
         apache2 \
         libapache2-mod-auth-openidc
```

2. Create a vhost file: `/etc/apache2/sites-available/peregrine.conf` and replace the variables below accordingly:

* `${APACHE_DOMAIN}` - Your domain name
* `${APACHE_PROXY_URL}` - Absolute URL to your Peregrine instance (i.e. http://localhost:8080/)
* `${OIDC_PROVIDER_METADATA_URL}` - = Open ID Connect metadata URL (i.e. for Google it should be https://accounts.google.com/.well-known/openid-configuration)
* `${OIDC_CLIENT_ID}` - Your Open ID Connect client ID 
* `${OIDC_CLIENT_SECRET}` - Your Open ID Connect client secret
* `${OIDC_CRYPTO_PASSPHRASE}` = Your Open ID Connect crypto pass phrase. You can set this to any value you wish. The header login module has nothing to do with this.
* `${AUTH_HEADER_SHARED_SECRET}` - Set this to the shared secret defined in the _Header Authentication Handler Configuration_ in the Felix system console.

```
<VirtualHost *:80>
    ServerAdmin webmaster@${APACHE_DOMAIN}
    DocumentRoot "/var/www/html"
    ServerName ${APACHE_DOMAIN}
    ErrorLog "${APACHE_LOG_DIR}/${APACHE_DOMAIN}-error_log"
    CustomLog "${APACHE_LOG_DIR}/${APACHE_DOMAIN}-access_log" common

    <Directory /var/www/html>
        Options Indexes FollowSymLinks
        AllowOverride None
        Require all granted
    </Directory>

    OIDCProviderMetadataURL ${OIDC_PROVIDER_METADATA_URL}
    OIDCClientID            ${OIDC_CLIENT_ID}
    OIDCClientSecret        ${OIDC_CLIENT_SECRET}
    OIDCCryptoPassphrase    ${OIDC_CRYPTO_PASSPHRASE}
    OIDCScope               "openid email"
    OIDCPassClaimsAs        both
    OIDCAuthNHeader         REMOTE_USER

    # OIDCRedirectURI is a URI that must be under the protected path (i.e. '/') but should not point to any content on the server 
    # or be available on Sling/Peregrine. You can name this URI whatever you like. It also needs to be ignored by the proxy pass (see below).
    OIDCRedirectURI         /oidc/redirect_uri
    
    <Location />
       AuthType openid-connect
       Require valid-user
    </Location>

    ProxyRequests Off
    ProxyPreserveHost On
   
    RequestHeader set X-Auth-Header-Shared-Secret ${AUTH_HEADER_SHARED_SECRET}

    ProxyPass          /       ${APACHE_PROXY_URL}
    ProxyPassReverse   /       ${APACHE_PROXY_URL}

    ProxyPass "/oidc/redirect_uri" !

</VirtualHost>
```


3. Install and configure the modules required by the vhost configuration above.

```
$ sudo && a2enmod proxy_http \
           && a2enmod headers \
           && a2dissite 000-default \
           && a2ensite peregrine
```
