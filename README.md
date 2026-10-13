<p align="center"><a href="https://www.verygoodsecurity.com/"><img src="https://avatars.githubusercontent.com/u/17788525" width="256" alt="VGS Logo"></a></p>
<p align="center"><b><i>Starlarky</i></b><br/>VGS edition of Google's safe and hermetically sealed Starlark language</p>
<p align="center">
<a href="https://circleci.com/gh/verygoodsecurity/starlarky/tree/master"><img src="https://circleci.com/gh/verygoodsecurity/starlarky/tree/master.svg?style=svg" alt="circleci-test"></a>
<a href="https://github.com/verygoodsecurity/starlarky/releases"><img src="https://img.shields.io/github/v/release/verygoodsecurity/starlarky"/></a>
<a href="https://pypi.org/project/pylarky/"><img src="https://img.shields.io/pypi/v/pylarky"/></a>
<a href="https://github.com/verygoodsecurity/starlarky/blob/master/LICENSE"><img src="https://img.shields.io/github/license/verygoodsecurity/starlarky"/></a>


<!-- toc -->
* [Description](#description)
* [Project overview](#project-overview)
    * [Libstarlark](#libstarlark)
    * [Larky](#larky)
    * [Runlarky](#runlarky)
    * [Pylarky](#pylarky)
* [Examples](#examples)
    * [Shipping a host-owned module next to a script](#shipping-a-host-owned-module-next-to-a-script)
* [Developer setup](#developer-setup)
* [Depoyment process](#deployment-process)
<!-- tocstop -->

## Description
Starlarky is VGS in-house edition of [Bazel](https://bazel.build/)'s hermetically-sealed language created by Google called [Starlark](https://github.com/bazelbuild/starlark).
This language is used to run "unsafe" user-submitted code without exposing service at whole to possible attack and/or vulnerabilities.
Starlark has Python-like syntax and is created to support same structure of additional libraries. 
Key differences between Starlark and Python can be found [here](https://docs.bazel.build/versions/master/skylark/language.html#differences-with-python)


## Project overview

Starlarky is presented as a monorepo with different modules

### Libstarlark

_Libstarlark_ is a maven module, that contains Starlark compiler from [bazelbuild](https://github.com/bazelbuild/bazel/tree/master/src/main/java/net/starlark/java)
This module is being periodically updated from bazelbuild via this [script](https://github.com/verygoodsecurity/starlarky/blob/master/bin/update-starlark.py)
to maintain relevancy.

See more at Libstarlarky [README](https://github.com/verygoodsecurity/starlarky/blob/master/libstarlark/README.md)

To build run this command:
```bash
mvn versions:set -DnewVersion=<your-version> -pl libstarlark (optional)
mvn clean package -pl libstarlark
```

### Larky

_Larky_ is a maven module, that contains VGS additions to Starlark language.
Some additions ispired and taken from [Copybara](https://github.com/google/copybara/)

Here are some of them:
- JSR223 script engine
- Annotations to define additional libraries
- Extension [modules](https://github.com/verygoodsecurity/starlarky/blob/master/larky/src/main/java/com/verygood/security/larky/modules/README.md)

To build run this command:
```bash
mvn versions:set -DnewVersion=<your-version> -pl larky (optional)
mvn versions:set-property -Dproperty=libstarlark.version -DnewVersion=<larky-version> -pl larky
mvn clean package -pl larky
```

### Runlarky

_Runlarky_ is an example Larky invocation application
It builds as a Quarkus executable and gives ability to run Larky with input parameters.

To build run this command:
```bash
mvn versions:set -DnewVersion=<your-version> -pl runlarky (optional)
mvn versions:set-property -Dproperty=starlarky.version -DnewVersion=<larky-version> -pl runlarky
mvn clean package -pl runlarky -Pnative
```

This would build `larky-runner` executable in `runlarky/target` directory, that can be run from terminal

### Pylarky

_Pylarky_ is pip lib-wrapper for runlarky to make larky calls conveniently from Python.

### Building and Running Tests

```bash
docker-compose build
docker compose run --rm maven /bin/sh -c "./install_graalvm.sh && ./build-and-test-java.sh"
docker compose run --rm python /bin/sh -c "./build-and-test-python.sh"
```

### Run individual larky stdlib test

```bash
mvn -Dtest='StdLibTest*' -Dlarky.stdlib_test=test_bytes.star org.apache.maven.plugins:maven-surefire-plugin:3.0.0-M5:test -pl larky
```

## Examples

### Shipping a host-owned module next to a script

A host that runs a customer's script through the JSR223 engine can pass other files with it in
the `LarkyScriptEngine.MODULES` engine property (a `Map<String, String>` of file name to
source); the script loads them by name, e.g. `load("vgs_owned", "sign")` for `vgs_owned.star`.
Each loads into its own module. `LarkyScriptEngine.CACHE_NAMESPACE` (e.g. a tenant ID) keeps
the compiled programs of these files apart per namespace in Larky's cache.

[`OpaqueModuleExampleTest`](larky/src/test/java/com/verygood/security/larky/jsr223/OpaqueModuleExampleTest.java)
ships a module that keeps a vault alias in `_KEY_ALIAS` and exposes `sign(message)`, which
reveals the key behind the alias and returns an HMAC. Names that start with `_` are private to
their file, so `load("vgs_owned", "_KEY_ALIAS")` fails when the script is compiled with
`symbol '_KEY_ALIAS' is private and cannot be imported`.

```bash
mvn test -pl larky -am -Dtest=OpaqueModuleExampleTest -Dsurefire.failIfNoSpecifiedTests=false
```

What this hides, and what it does not:

- The value stays hidden only while no public function returns it, returns something derived
  from it that gives it away, or puts it in an error message. A private name is not access
  control.
- The script can call the public functions with any arguments, as often as it likes: the module
  is an oracle for whatever they do (here, an HMAC of any message the script chooses).
- The script can call `vault.reveal` itself on any alias it learns some other way.
- Storing a module per tenant, choosing which modules to pass with which script, and keeping
  tenants apart are up to the host (e.g. the proxy). Larky only loads the files it is given.

## Developer setup

In addition to having Maven installed, it must be configured to retrieve artifacts from Github.
1) Generate an access token using [Github's instructions](https://docs.github.com/en/github/authenticating-to-github/creating-a-personal-access-token).  The token needs `read:packages` scopes.
2) You must enable SSO for verygoodsecurity

3) Place the token in your `~/.m2/settings.xml` file.  For example (look for `github-username` and `github-api-key` to be replaced with your values):
```
<?xml version='1.0' encoding='us-ascii'?>
<settings xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0                           https://maven.apache.org/xsd/settings-1.0.0.xsd">
      <localRepository />
      <interactiveMode />
      <usePluginRegistry />
      <offline />
      <pluginGroups />
      <servers>
          <server>
              <id>github</id>
              <username>github-username</username>
              <password>github-api-key</password>
          </server>
      </servers>
      <mirrors />
      <proxies />
      <profiles />
      <activeProfiles />
    </settings>
```

## Deployment process
Release-please plugin manages creation of tag&release.