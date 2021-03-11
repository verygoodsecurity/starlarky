
https://github.com/verygoodsecurity/starlarky/blob/master/libstarlark/README.md
https://github.com/verygoodsecurity/starlarky/blob/master/larky/src/main/java/com/verygood/security/larky/modules/README.md

### Building and Running Tests

```bash
docker-compose build
docker-compose run local bash /src/build-and-test-java.sh
docker-compose run local bash /src/build-and-test-python.sh
```

### Run individual larky stdlib test

```bash
mvn -Dtest='StdLibTest*' -Dlarky.stdlib_test=test_bytes.star org.apache.maven.plugins:maven-surefire-plugin:3.0.0-M5:test -pl larky
```