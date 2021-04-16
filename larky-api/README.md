# Larky Versioned Engine API

## How to use the API

### 1) Include it as a dependency in your project.

#### Maven:
```
<dependency>
  <groupId>com.verygood.security</groupId>
  <artifactId>larky-api</artifactId>
  <version>*.*.*</version>
  <type>pom</type>
</dependency>
```
Note that if `com.verygood.security:larky` is already in your classpath, 
this may impact the usability of this API. 

### 2) Download the needed Larky fat-jars.

From the [github package registry](https://github.com/verygoodsecurity/starlarky/packages/673862),
download the Larky fat-jar files with the format `larky-*.*.*-jar-with-dependencies.jar` for the versions you want to include in your 
API, 
rename them to `larky-*.*.*-fat.jar`,
and place them in `/larky/lib`.

#### - Changing Larky fat-jar directory:
By default, the API will detect versions in `/larky/lib/larky-*.*.*-fat.jar`.
To change the root directory, set the `LARKY_LIB_HOME` environment variable and the API will then detect versions from
`$LARKY_LIB_HOME/larky-*.*.*-fat.jar`

#### - Downloading all versions with a script:
To download all available Larky fat-jar versions automatically, run the following script:
```shell
./scripts/get_resources.sh
```
