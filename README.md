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
   
3. Test a header-based authentication request.

```
curl -s -v -H "REMOTE_USER: someuser" http://localhost:8080/
```
