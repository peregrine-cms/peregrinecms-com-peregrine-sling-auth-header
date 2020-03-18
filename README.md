# Header Login Module

1. Deploy this bundle.

```
$ mvn clean install sling:install
```


2. Log into the [Apache Sling Configuration Console](org.apache.felix.jaas.Configuration.factory) and create a 
   configuration for the _Apache Felix JAAS Configuration Factory_ (org.apache.felix.jaas.Configuration.factory).
   
   * Control Flag (jaas.controlFlag) = Sufficient
   * Ranking (jaas.ranking) = 5000
   * Realm Name (jaas.realmName) = jackrabbit.oak
   * Class Name (jaas.classname) = com.peregrine.sling.auth.header.HeaderExternalLoginModule
   * Options (jaas.options) = _leave empty_
   
3. Create a configuration for _Apache Jackrabbit Oak Default Sync Handler_ 
   (org.apache.jackrabbit.oak.spi.security.authentication.external.impl.DefaultSyncHandler).

   * Sync Handler Name (handler-name) = default
   * Leave all other defaults as-is.

4. Test a header-based authentication request.

```
curl -s -v -H "REMOTE_USER: someuser" http://localhost:8080/
```
