# Header External Login Module 

TODO

## Prerequisites

* Java JDK 8
* Apache Maven +3.5
* Apache Sling 9 with `oak-auth-external` bundle installed.

## Installation

1. Deploy the `oak-auth-external` bundle to Sling 9. This is not provided by default in Sling 9.

2. Deploy this project to Sling.

```
$ mvn clean install sling:install
```

## Configuration

1. Log into the [Apache Sling Configuration Console](org.apache.felix.jaas.Configuration.factory) and create a 
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
