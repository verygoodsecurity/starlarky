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
        * [Larky REPL](#larky-repl)
    * [Pylarky](#pylarky)
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

#### Larky REPL

Run `larky-runner` with no arguments to get an interactive Larky prompt. It has the same environment as a script run by the runner, so Python builtins and `load()` of `@stdlib`, `@vendor` and `@vgs` modules work. Use it to try out a snippet or poke at a module before putting it in a script.

**Install on Linux (x86-64).** Each release from 1.0.0 on attaches a native `larky-<version>-linux` binary. Download it from the [releases page](https://github.com/verygoodsecurity/starlarky/releases), then:

```bash
chmod +x larky-1.0.0-linux
./larky-1.0.0-linux
```

**Run it on any OS with Java 21** (macOS, for example). Build it from a clone and start it on the JVM:

```bash
mvn -B -q -pl runlarky -am -DskipTests package dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
java -cp "runlarky/target/runlarky-1.0.0-SNAPSHOT.jar:$(cat runlarky/target/classpath.txt)" \
  com.verygood.security.run.LarkyEntrypoint
```

It prints three SLF4J warnings when it starts; they're harmless.

If Maven can't download a dependency, set up GitHub Packages access first (see [Developer setup](#developer-setup)).

**Use it:**

```
Larky interactive interpreter. Type :help for help, :quit or Ctrl-D to exit.
>>> load("@stdlib//base64", "base64")
>>> base64.b64encode(b"hi")
b"aGk="
>>> def add(a, b):
...     return a + b
...
>>> add(2, 3)
5
```

- The value of an expression is printed. A block (`def`, `if`, `for`) ends at a blank line.
- Tab completes names, attributes (`base64.<Tab>`) and `load()` labels. Up and down arrows walk through history, which is kept in `~/.larky_history`.
- Ctrl-C cancels the line you're typing, or stops a running evaluation, without leaving the REPL.
- Commands: `:help`, `:load <file>` (run a `.star` file in the session), `:reset` (forget every binding), `:quit` or Ctrl-D.
- Input can also be piped in. There's no prompt then, and the exit status is 1 if any input failed: `echo 'print(1 + 1)' | larky-runner` prints `2`.

Script mode (`-s`, `-i`, `-o`, `-l`, `-d`) works as before.

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