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

## Build a Peregrine Image with the External Header Login Module

In this section, we will build a base version of Sling9 that includes all dependencies and configurations
using the Provisioning Model. Then, we will create a new Docker image for Peregrine that leverages our
custom version launchpad.

1. Build this project. This will install the bundle into your local Maven repository.

```
$ mvn clean install
```

2. Check out Peregrine CMS, switch to the `issue/322` branch and build it.

```
$ git clone https://github.com/headwirecom/peregrine-cms.git
$ cd peregrine-cms
$ git checkout issue/322
$ mvn clean install
```


3. Build Sling9 launchpad.

```
$ cd sling/peregrine-builder
$ mvn clean install
```

5. Copy the newly built Sling launchpad JAR to the peregrine-cms/resources directory.

```
$ cp target/com.peregrine-cms.sling.launchpad-9.1.jar ../../resources/
```

6. Build the Docker image.

```
cd ../../docker
$ ./builddocker.sh 
```
