<p align="center"><a href="https://www.verygoodsecurity.com/"><img src="https://avatars.githubusercontent.com/u/17788525" width="256" alt="VGS Logo"></a></p>
<p align="center"><b><i>Starlarky</i></b><br/>VGS edition of Google's safe and hermetically sealed Starlark language</p>
<p align="center">
<a href="https://circleci.com/gh/verygoodsecurity/starlarky/tree/master"><img src="https://circleci.com/gh/verygoodsecurity/starlarky/tree/master.svg?style=svg" alt="circleci-test"></a>
<a href="https://github.com/verygoodsecurity/starlarky/releases"><img src="https://img.shields.io/github/v/release/verygoodsecurity/starlarky"/></a>
<a href="https://pypi.org/project/pylarky/"><img src="https://img.shields.io/pypi/v/pylarky"/></a>
<a href="https://github.com/verygoodsecurity/starlarky/blob/master/LICENSE"><img src="https://img.shields.io/github/license/verygoodsecurity/starlarky"/></a>
<img src="https://img.shields.io/snyk/vulnerabilities/github/verygoodsecurity/starlarky"/>
</p>

<!-- toc -->
* [Description](#description)
* [Project overview](#project-overview)
    * [Libstarlark](#libstarlark)
    * [Larky](#larky)
    * [Runlarky](#runlarky)
    * [Pylarky](#pylarky)
<!-- tocstop -->

## Description

Starlarky is VGS in-house edition of Bazel hermetically-sealed language created by Google called [Starlark](https://github.com/bazelbuild/starlark).
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

To build run
```bash
mvn versions:set -DnewVersion=<your-versions> -pl libstarlark (optional)
mvn clean package -pl libstarlark
```

### Larky

https://github.com/verygoodsecurity/starlarky/blob/master/larky/src/main/java/com/verygood/security/larky/modules/README.md


### Runlarky

### Pylarky

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
