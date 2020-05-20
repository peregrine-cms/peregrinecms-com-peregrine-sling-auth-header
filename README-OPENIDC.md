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

## Manual Installation

### Peregrine CMS Installation

1. Visit [https://github.com/headwirecom/peregrine-cms](https://github.com/headwirecom/peregrine-cms) and install
   Peregrine per the README.
   
2. Check that Peregrine has the `oak-auth-external` bundle installed it. If it doesn't, you don't have a version of
   Peregrine that supports the Header External Login Module.

### Header External Login Module Installation

1. Deploy this project to Peregrine.

```
$ mvn clean install sling:install
```

### Header External Login Module Configuration

1. Log into the [Apache Sling Configuration Console](http://localhost:8080/system/console/configMgr) and create a 
   configuration for the _Apache Felix JAAS Configuration Factory_ (`org.apache.felix.jaas.Configuration.factory`).
   The `jaas.ranking` must have a higher number than the other JAAS modules for this module to handle logins.
   
   * Control Flag (jaas.controlFlag) = `Sufficient`
   * Ranking (jaas.ranking) = `5000`
   * Realm Name (jaas.realmName) = `jackrabbit.oak`
   * Class Name (jaas.classname) = `com.peregrine.sling.auth.header.HeaderExternalLoginModule`
   * Options (jaas.options) = _leave empty_
   
2. Create a configuration for _Header Authentication Handler Configuration_.

   * Login Cookie (header.auth.login.cookie) = _leave empty_
   * Remote User Header (header.auth.remote.user.header) = `REMOTE_USER`
   * Shared Secret (header.auth.shared.secret) = `secret`
   * Username Whitelist Pattern (header.auth.username.whitelist) = `^[A-Za-z0-9+_.-]+@(.+)$`
   * User Profile Header Whitelist Pattern = `^OIDC_CLAIM_(.+)$` 
   
3. Create a configuration for _Apache Jackrabbit Oak Default Sync Handler_.
   (`org.apache.jackrabbit.oak.spi.security.authentication.external.impl.DefaultSyncHandler`).

   * Sync Handler Name (handler-name) = `default`
   * User auto membership (user.autoMembership) = `all_tenants` 
   * User Property Mapping (user.propertyMapping) = 
     * `preferences/firstLogin=firstLogin`
     * `profile/email=OIDC_CLAIM_email`
   * User Path Prefix (user.pathPrefix) = `tenants`
   * Leave all other defaults as-is.
   
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

2. Create a vhost file: `/etc/apache2/sites-available/peregrine.conf`:

```
<VirtualHost *:80>

    Protocols h2 http/1.1

    ModPagespeed unplugged

    SetOutputFilter DEFLATE

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

    # OIDCRedirectURI is a URL that must point to a protected path (i.e. Location below),
    # but does not point to any content on the server or the proxy. Basically, you can name 
    # this URI whatever you like. It also needs to be ignored by the proxy pass (see below).
    OIDCRedirectURI         /oidc/redirect_uri

    # Allow health check requests to be unauthenticated
    <Location "/content/sites/themecleanflex/index.html">
      Order deny,allow
      Allow from all
      Satisfy any
    </Location>

    <Location />
       AuthType openid-connect
       Require valid-user
    </Location>

    # Force HTTPS on load balancers
    RewriteEngine On
    RewriteCond %{HTTP:X-Forwarded-Proto} =http
    RewriteRule . https://%{SERVER_NAME}%{REQUEST_URI} [L,R=permanent]

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
$ sudo curl -L -O https://dl-ssl.google.com/dl/linux/direct/mod-pagespeed-stable_current_amd64.deb \
           && dpkg -i mod-pagespeed-*.deb \
           && a2enmod expires \
           && a2enmod pagespeed \
           && a2enmod proxy_http \
           && a2enmod rewrite \
           && a2enmod headers \
           && a2dissite 000-default \
           && a2ensite peregrine
```

4. Create a script to export all environment variables defined in the vhost.

```
#!/bin/bash

export APACHE_DOMAIN=Your domain
export APACHE_PROXY_URL=Absolute URL to your Peregrine instance
export OIDC_PROVIDER_METADATA_URL=Open ID Connect metadata URL
export OIDC_CLIENT_ID=Open ID Connect client ID 
export OIDC_CLIENT_SECRET=Open ID Connect client secret
export OIDC_CRYPTO_PASSPHRASE=Open ID Connect crypto pass phrase
export AUTH_HEADER_SHARED_SECRET=Set to the shared secret define in Header Authentication Handler Configuration in Felix
```

5. Source your environment variables script and start Apache.

## Docker Installation

The Docker images are the fast way to get to started.

1. Follow the steps in _Configure Google's OpenID Connect_ above.

2. Start the Peregrine CMS container.

```
$ docker pull peregrinecms/peregrine-cms:sso-20200519r1
$ docker run -it -p 8080:8080 peregrinecms/peregrine-cms:sso-20200519r1
```

3. Log into Peregrine directly as admin/admin on http://localhost:8080/ and set a shared secret in the 
   _Header Authentication Handler Configuration_ configuration console.

4. Start the Apache container. Set the environment variables accordingly.

```
$ docker pull peregrinecms/apache-stage:sso-20200519r1
$ docker run -dit --name ${DOCKER_CONTAINER_NAME} -p 8888:80 \
    -e APACHE_DOMAIN=SETME \
    -e APACHE_PROXY_URL=SETME \
    -e OIDC_PROVIDER_METADATA_URL=SETME \
    -e OIDC_CLIENT_ID=SETME \
    -e OIDC_CLIENT_SECRET=SETME \
    -e OIDC_CRYPTO_PASSPHRASE=SETME \
    -e AUTH_HEADER_SHARED_SECRET=SETME \
    peregrinecms/apache-stage:sso-20200519r1
```

5. Visit [http://localhost:8888](http://localhost:8888) and you should be redirected to your IDP (i.e. Google).
