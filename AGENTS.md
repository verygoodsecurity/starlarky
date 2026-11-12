# Working on Starlarky

Starlarky embeds a Python-like Starlark interpreter for running untrusted scripts.
Read [README.md](README.md), [CONTRIBUTING.md](CONTRIBUTING.md), and
[SECURITY.md](SECURITY.md) before changing runtime behavior.

## Repository map

- `libstarlark/`: the upstream-derived `net.starlark.java` interpreter and VGS
  extensions. Keep changes to upstream code small and easy to reconcile during
  updates. Check the current source and patches in `bin/`; older documentation
  may describe an earlier upstream revision.
- `larky/`: the JSR223 engine, module loading and caching, Python compatibility
  layer, native Java modules, and `.star` libraries in `src/main/resources/`.
- `runlarky/`: the picocli CLI and REPL, packaged as a GraalVM native executable.
- `pylarky/`: the Python wrapper around the bundled native runner.
- `bin/` and `third_party/`: upstream synchronization scripts, patches, and
  third-party material. Preserve license notices and keep vendor changes explicit.

## Build and validation

Run commands from the repository root. Use JDK 21 or newer and Maven 3.9 or newer.
GitHub Packages access is described in the README; keep credentials outside the
repository and never print them while diagnosing dependency resolution.

```bash
# Build and test the Java reactor without compiling a native image.
mvn clean install

# Test a module and its reactor dependencies.
mvn test -pl larky -am

# Run one Java test; upstream modules may have no matching test.
mvn test -pl larky -am -Dtest=LarkyScriptEngineModulesTest -Dsurefire.failIfNoSpecifiedTests=false

# Run one Starlark stdlib test through the Java test harness.
mvn test -pl larky -am -Dtest=StdLibTests -Dlarky.stdlib_test=test_bytes.star -Dsurefire.failIfNoSpecifiedTests=false

# CI's Docker Compose builds: native Java runner, then Python tests and wheel.
make test_and_package
make py_test_and_package
```

- Start with tests covering the changed behavior, then run the affected module
  suites. Interpreter changes can affect every module; include downstream tests.
- Java tests live under each module's `src/test/`. Follow the adjacent JUnit and
  Truth conventions; the repository uses both JUnit 4 and Jupiter tests.
- Starlark library tests live in `larky/src/test/resources/stdlib_tests/`,
  `vendor_tests/`, and `vgs_tests/`, with corresponding Java harnesses.
- Native validation matters when changing script-visible classes, annotations,
  resources, or CLI behavior. Run `ReflectConfigTest` in `runlarky` and the native
  build/smoke test in `build-and-test-java.sh`; JVM tests alone do not establish
  native-image compatibility.
- Python tests require the native runner first. `build-and-test-python.sh`
  copies it into `pylarky/`, runs pytest with uv, and builds the wheel. The Python
  requirement and dependencies are in `pyproject.toml` and `uv.lock`.
- Report the commands run and their results. If credentials, Docker, or GraalVM
  prevent a check, say which check remains unverified.

## Implementation conventions

- Match the surrounding code's formatting, naming, imports, and logging API.
  Keep changes focused; avoid repository-wide formatting or framework migrations.
  Comment non-obvious semantics and security decisions rather than every statement.
- Preserve script-facing names, parameter defaults, accepted types, return values,
  and error behavior unless the change deliberately updates that contract. Cover
  Python compatibility fixes with `.star` tests, including invalid inputs and
  boundary cases. Do not assume CPython features exist in Starlarky.
- For Java-backed modules, follow existing `@StarlarkBuiltin`, `@StarlarkMethod`,
  and `@Param` usage. Register modules in the appropriate `ModuleSupplier` set:
  `STD_MODULES`, `VGS_MODULES`, or test-only `TEST_MODULES`. Preserve `@stdlib//`,
  `@vendor//`, and `@vgs//` load namespaces and existing `.star` wrappers.
- Update the applicable files under
  `runlarky/src/main/resources/META-INF/native-image/` when reflection, resources,
  or dynamic proxies change. Use the current paths in `runlarky/pom.xml` rather
  than older examples of reflection configuration locations.
- Keep interpreter changes compatible with Starlark mutability and freezing,
  evaluation isolation, cancellation, and configured execution limits. Avoid
  process-wide mutable state containing script or request data. See the isolation,
  cache, and JSR223 tests before changing shared module or compiled-program reuse.
- Treat new script-visible Java functionality as an expansion of host capabilities.
  Follow [SECURITY.md](SECURITY.md) for loading, provider, crypto, and data handling
  requirements. Keep testing modules unavailable to normal embedded evaluations.
- Performance review should focus on interpreter hot paths, parsing/compilation,
  cache bounds and keys, large byte/integer operations, decompression, and native
  calls. Preserve resource limits and interruption checks; do not trade isolation
  or validation for cache hits or speed.
- Prefer extension work in `larky/` when it does not require changing the core
  interpreter. For upstream-derived changes, document the VGS-specific behavior
  and regression coverage. For an upstream sync, review
  `bin/update-starlark.py` and `bin/regenerate-differences-patch.sh` and preserve
  the local delta; these scripts can replace source trees.

## Maven and dependency changes

- The root POM aggregates the three Java modules; their POMs do not currently
  inherit from it. Do not assume root dependency/plugin management or a Spring
  parent governs module builds.
- Follow the affected POM's existing version properties and explicit dependency
  and plugin declarations. Keep reactor artifact versions aligned, and update
  related pins together when compatibility requires it. Explain security or
  compatibility overrides and preserve existing exclusion rationale.
- Avoid adding dependencies for functionality already available in the project.
  Check Java 21, GraalVM native-image support, transitive dependencies, licenses,
  and security impact when introducing or upgrading a library.
- Preserve annotation processors, service-provider registrations, and native-image
  configuration. Do not remove a dependency or change its scope solely because it
  looks unused: JUnit is intentionally available at compile scope in `larky` for
  the script testing API.

## Reviews and pull requests

- Describe the concrete behavior change, affected modules, compatibility impact,
  and validation. Follow [.github/PULL_REQUEST_TEMPLATE.md](.github/PULL_REQUEST_TEMPLATE.md)
  and link the relevant issue or Jira ticket when one exists.
- Prioritize actionable correctness, isolation, security, compatibility, and
  performance findings. Identify the affected code and a reproducible scenario;
  distinguish verified problems from questions and optional suggestions.
- Explain security impact when changing host capabilities, sensitive-data handling,
  provider selection, cache isolation, or resource limits. Request maintainer
  review for changes to these boundaries or to cryptographic behavior.
- Update the existing README, module documentation, or examples that describe the
  changed behavior. Do not invent documentation paths, team notification handles,
  service architecture requirements, or checks absent from this repository.
