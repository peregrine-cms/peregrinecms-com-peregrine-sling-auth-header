# Header Login Module

1. Deploy this bundle.

```
$ mvn clean install sling:install
```


2. Test a header-based authentication request.

```
curl -s -v -H "REMOTE_USER: someuser" http://localhost:8080/
```
