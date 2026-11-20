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

## Runtime architecture: providers scoped to one run

[PR #830](https://github.com/verygoodsecurity/starlarky/pull/830) introduces this
design on top of [#829](https://github.com/verygoodsecurity/starlarky/pull/829)
(`@vgs//agent_gateway`) and [#828](https://github.com/verygoodsecurity/starlarky/pull/828)
(scripts loaded once with per-call names). This stack is not yet merged into this
branch; the APIs and tests below describe that incoming architecture.

- Compilation, loading, and invocation have different lifetimes. A
  `LarkyCompiledScript` can produce a `LarkyLoadedScript` whose top level runs once
  and whose module is frozen for repeated calls. Host values needed by calls must
  be supplied per call rather than captured in shared module state.
- `ModuleProviders` is an immutable registry of host-supplied provider references
  for one run. Its supported types are `LarkyVault` and `AgentGatewaySession`.
  Adding or replacing a provider returns a new registry; it does not freeze the
  provider's own state or make that provider thread-safe.
- Hosts pass the registry under `LarkyEvaluator.MODULE_PROVIDERS` in evaluation
  bindings or `LarkyLoadedScript.invoke`'s per-call values. This is an execution
  setting, like `STEP_LIMIT` and `EXPIRATION_MS`; it need not be declared as a
  script's per-call name. Reject wrong-typed bindings consistently in both paths.
- Evaluation and invocation install providers on the run's `StarlarkThread` via
  `setThreadLocal`. Annotated native entry points use `useStarlarkThread = true`
  and look up the provider when called. Do not store a request's provider in a
  singleton module or implement new implicit Java thread-state lookups.
- `load()` and module identity stay unchanged. `@vgs//vault` delegates to the
  run's `LarkyVault` when supplied, otherwise to the module's default vault.
  `@vgs//agent_gateway` exposes a shared `session` facade whose methods use the
  run's `AgentGatewaySession`, otherwise `LarkyAgentGateway.currentSession()`
  from the `ServiceLoader` host. With neither session source, methods fail even
  though loading the module is allowed. Fallback applies when a provider is
  absent, not when a supplied provider rejects an operation.
- Each loaded-script call gets a fresh `StarlarkThread` and its own provider
  registry. Load-context providers and expiration are not retained as defaults
  for later calls; the load's step limit remains the default. A call without
  providers uses module defaults, never the previous call's providers.
- Preserve the Java compatibility API: `VaultModule` still implements
  `LarkyVault`; `INSTANCE`, its public constructor, and `getVault()` remain.
  Direct Java calls use the module's default vault. Script calls retain their
  names, parameters, validation, and lowercase storage normalization regardless
  of the chosen provider.

For example, with `loaded` already created and `vaultForThisMessage` supplied by
the host, #830 allows the same script function to use a different vault per call:

```java
ModuleProviders providers = ModuleProviders.of()
    .with(LarkyVault.class, vaultForThisMessage);
loaded.invoke("process", Map.of(LarkyEvaluator.MODULE_PROVIDERS, providers), input);
```

When modifying this design, extend `ModuleProvidersTest`,
`AgentGatewayModuleTest`, and `LarkyLoadedScriptTest` from the stack. Cover
provider precedence, no-provider fallback, calls with different providers followed
by a call without them, rejected bindings, and unchanged argument validation and
Java behavior. Keep execution providers out of compiled-program cache keys and
cached values: they belong to the run, while `CACHE_NAMESPACE` separates cached
host-supplied module programs. Validate native-image metadata when changing
annotated entry points.

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

## Code Review Rules

- Prioritize defects introduced by this change: incorrect behavior, security or
  tenant-boundary failures, compatibility breaks, data loss, and demonstrated
  performance or availability risks. Read the surrounding implementation and
  relevant tests before concluding that a pattern is unsafe.
- Each finding needs a file/line, the triggering input or system state, its
  concrete impact, and a practical correction. Distinguish defects from questions,
  optional improvements, and pre-existing problems. Do not invent missing
  architecture or report unsupported speculation.
- In the review summary, state the actionable finding count and recommend a
  focused pass, normal review, or deep/specialist review, with reasons. Point the
  human reviewer to the highest-risk files and specific behavior to verify.
  Identify compatibility and deployment dependencies for stacked changes.
- State which tests or checks actually ran and which remain unverified. A build
  with tests skipped or a passing security scan does not establish tested behavior.
  Say when no actionable findings were identified in the material reviewed;
  incomplete context is not a clean assessment or permission to merge.
- Leave formatting and mechanical lint to existing tools. Avoid mandatory praise,
  broad refactors, arbitrary method-size limits, and repeating the same issue.
  Recommend shared framework changes only when a concrete duplicated or divergent
  behavior matters to this change.
- Apply [SECURITY.md](SECURITY.md) to the affected data flow. Review security
  boundaries and failure behavior even when a feature is off by default.
- Update existing architecture or domain documentation when its contract changes.
  Use verified owner identities for escalation; do not invent team handles.

### Common Java/JVM baseline

These rules apply to maintained JVM code; preserve Kotlin idioms in Kotlin modules.
Framework-specific checks apply only where the affected module uses that framework.

- Prefer existing shared clients, authentication, cryptography, and resilience
  facilities. Preserve public APIs, serialization, exception contracts, dependency
  injection, and the configured Java/runtime compatibility.
- Use an injected `java.time.Clock` for new time-dependent application logic so
  expiry and scheduling can be tested deterministically. Elapsed-time measurements
  have different requirements; do not flag a monotonic timer as wall-clock misuse.
- Check actual execution context before flagging blocking: protect event loops
  and constrained consumer pools, bound waits and queues, propagate cancellation,
  and avoid accidental common-pool or unbounded executor use. Support a finding
  with a starvation, contention, race, or resource-exhaustion scenario.
- Outbound calls need suitable deadlines and bounded retries/backoff. Check
  idempotency before retrying writes, retry amplification, connection pools, and
  poison-message behavior. Preserve tracing context across asynchronous work.
- For database code, check N+1 queries, unbounded reads, index/query changes, and
  transactions held across slow I/O. Where Spring transaction attributes control
  replica routing, use appropriate read-only transaction boundaries while
  preserving writes, consistency requirements, and inherited transaction behavior.
- Keep hot-path logging and metrics bounded. Avoid sensitive values, raw-ID metric
  labels, and unnecessary high-cardinality histograms. Expected retryable failures
  should normally be `WARN`; reserve `ERROR` for actionable failures under the
  service's alerting contract. Preserve useful, safe diagnostic context.
- Inspect the effective Maven parent/management chain before proposing version
  changes. Inherit versions already managed at the same value; centralize new
  managed versions in properties where the build convention supports it. Preserve
  explained security/compatibility pins, reactor references, build extensions,
  and repository-specific release tooling. Do not rewrite unrelated POMs.
- Add regression coverage for the changed success and failure paths. Match the
  existing test framework and formatting instead of introducing a new one.
