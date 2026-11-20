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

### Experimental Kitesurf browser

Customer scripts load the embedded Kitesurf guest with
`load("@vgs//browsers", "kitesurf")`. A Java host grants the guest access to
approved origins and resolves value references during a Larky invocation.
See the [Wasm browser component](larky-wasm-browser/README.md) for form
discovery, hosted frames, and the Adyen sandbox test.

#### CDP rendering harness

A Java host can give a Larky script a browser for rendering trusted inline HTML.
Kitesurf runs in a separate local `workerd` process and uses the same page-worker
and WebAssembly architecture as the Workers deployment. Larky connects over the
Chrome DevTools Protocol. The browser capability is independent of
`@vgs//wasm` and its WASI command runtime.

Build and start [Kitesurf](https://github.com/cloudflare/kitesurf) on
`127.0.0.1:8787` using its local setup. The experimental adapter targets
[revision `a58b15d`](https://github.com/cloudflare/kitesurf/commit/a58b15dfabc0a238a4ab37c9ba9c1813c865bf21).
Leave `KITESURF_NATIVE_EVAL` unset to use the Workers-compatible JavaScript
evaluation path. Grant the browser binding only to trusted scripts and HTML.
Cloudflare documents that local `workerd` is unsuitable for running untrusted pages.

```java
try (var browser = new KitesurfBrowser(URI.create("ws://127.0.0.1:8787/devtools/browser"))) {
  var engine = new LarkyScriptEngine();
  engine.put("browser", browser);
  engine.eval("""
      page = browser.render("<h1>Hello from Kitesurf</h1>")
      text = page["text"]
      png = page["png"]
      """);
}
```

Import `KitesurfBrowser` from `com.verygood.security.larky.browser`.
Each call creates a fresh browser context and returns visible text and an 800 ×
600 PNG as Starlark bytes. The host supplies the loopback endpoint. The adapter
caps HTML at 256 KiB and uses the earlier of 15 seconds and the evaluation
deadline.

To run the live smoke test against a running engine:

```bash
mvn -B -ntp -pl larky -am -Dtest=KitesurfBrowserTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dlarky.kitesurf.testEndpoint=ws://127.0.0.1:8787/devtools/browser test
```

### Runlarky

_Runlarky_ is an example Larky invocation application.
It is a picocli command-line application, built as a GraalVM native executable, that runs Larky with input parameters.

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

## Developer setup

Building Starlarky requires JDK 21 or newer and Maven 3.9 or newer; a build of the whole project
stops at the start with a message saying so if either is older. With those installed, build and
install all modules from the repository root with:
```bash
mvn clean install
```

Maven must also be configured to retrieve artifacts from Github.
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
