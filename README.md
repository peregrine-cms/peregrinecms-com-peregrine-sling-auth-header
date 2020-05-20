# Header External Login Module 

The _Header External Login Module_ is a JAAS login module for Apache Sling that allows users to log in using a set of
HTTP headers. The intended use case for this login module is to delegate authentication to an upstream system. This
will typically be an SSO solution configured at the web-tier/web server that handles the user authentication and then
proxies the request to Sling. As a practical example, Apache HTTPD, [mod_auth_openidc](https://github.com/zmartzone/mod_auth_openidc) and 
[Google's OAuth 2.0 API](https://developers.google.com/identity/protocols/oauth2/openid-connect) were used during the
development of this module. If you are interested in using this module to add OpenID Connect support to your Sling application,
follow the _Prerequisites_ and _Installation_ section below, then refer to [README-OPENIDC.md](README-OPENIDC.md). To use
the Header External Module on its own, continue reading.

## How it Works

This module follows the "Pre-Authentication combined with Login Module Chain" approach described on the 
[Apache Jackrabbit](https://jackrabbit.apache.org/oak/docs/security/authentication/preauthentication.html) site.

1. The `HeaderAuthenticationHandler` is responsible for detecting a valid header authentication request and extracting
   the authenticated user name. It accomplishes this by performing some basic sanity checks such as shared key validation and
   extracting the user name from the `REMOTE_USER` (default) request header. It then creates a custom credentials 
   instance, `HeaderCredentials`, with the `REMOTE_USER` value set as the user ID. These credentials are then stored
   in a new `AuthenticationInfo` instance. It is important to note, that this module assumes that another upstream system
   has already authenticated the user.
   
   WARNING: For security, the communication between the client and Sling should be encrypted using HTTPS. Secondly,
   it is recommended that the client be a proxy server (i.e. Apache with mod_proxy) that handles the authentication
   and sends the HTTP headers to Sling. These headers should never be allowed to be sent directly by an end user and 
   the headers should never be returned by the proxy back to the end-user. Lastly, network-level ACLs should also be 
   implemented to only allow traffic to Sling from the proxy. 
   
2. The standard Jackrabbit Oak Login chain is observed and the module with the highest `JAAS ranking` is called first. 
   In our case, this will be the `HeaderExternalLoginModule`.
   The `HeaderExternalLoginModule` is responsible for checking that the Credential object is a `HeaderCredentials` object.
   If it is, the login module sets a "Pre-Authentication Marker" on the shared state to signal ton down stream login modules
   that the user is already authenticated. If this is a new user, a user account is created in the repository using the
   default _Sync Handler_. It should be noted that the `HeaderExternalLoginModule.login()` method always returns
   `false`. This is required to allow other modules in the chain to succeed and process the pre-authentication marker.

## Prerequisites

* Java JDK 8
* Apache Maven 3.5+
* Apache Sling 9 with `oak-auth-external` bundle installed.

## Installation

1. Deploy the `oak-auth-external` bundle to Sling 9. This is not provided by default in Sling 9.

2. Deploy this project to Sling.

```
$ mvn clean install sling:install
```

## Configuration

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
   * User Profile Header Whitelist Pattern = `X-Auth-Header(.+)$` 
   
3. Create a configuration for _Apache Jackrabbit Oak Default Sync Handler_.
   (`org.apache.jackrabbit.oak.spi.security.authentication.external.impl.DefaultSyncHandler`).

   * Sync Handler Name (handler-name) = `default`
   * User auto membership (user.autoMembership) = `all_tenants` 
   * User Property Mapping (user.propertyMapping) = 
     * `preferences/firstLogin=firstLogin`
   * User Path Prefix (user.pathPrefix) = `tenants`
   * Leave all other defaults as-is.

4. Test a header-based authentication request.

```
curl -s -v -L \
  -H "X-Auth-Header-Shared-Secret: secret" \
  -H "REMOTE_USER: me@domain.com" \
  http://localhost:8080/

```

If successful, you should have a user named `me@domain.com` created under `/home/users/tenants`.
