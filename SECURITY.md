# Security policy

Starlarky executes untrusted scripts inside a Java host, through its embedded
engine or CLI. Security depends on both the interpreter and the capabilities
the host makes available. This policy guides changes to that boundary; it is not
a claim that every embedding is sandboxed or PCI compliant.

## Reporting a vulnerability

Do not put sensitive payloads, credentials, or exploit details in public issues
or pull requests. Use **Report a vulnerability** in the repository's
[Security tab](https://github.com/verygoodsecurity/starlarky/security) if private
reporting is enabled. If it is unavailable, ask the maintainers to establish a
private reporting channel before sharing those details.

Include the affected version or commit, embedded/JVM/native/Python execution
mode, relevant host configuration, a minimal reproduction using synthetic data,
and the expected and observed impact. Redact secrets and customer information.

## Host capabilities and isolation

- Treat script source, bindings, supplied module files, load labels, and native
  method arguments as untrusted. Validate types, ranges, lengths, and paths at
  the Java boundary, including overflow and allocation risks.
- Review module resolution for unintended filesystem or classpath access and
  namespace bypasses. Keep test-only modules out of normal evaluations. Do not
  expose arbitrary Java reflection, class loading, process execution, filesystem,
  or network access to scripts without an explicit capability design and review.
- Preserve freezing of shared module values, immutable built-in types, and
  isolation between evaluations. Caches must not reuse mutable bindings, results,
  or host data across requests or tenants. Preserve the namespace separation of
  host-supplied module programs via `LarkyScriptEngine.CACHE_NAMESPACE`.
- The embedding host chooses the modules, credentials, providers, and data a
  script receives and enforces authorization and tenant separation. A private
  `_name` in a `.star` file is not access control: public functions can still
  reveal it or act as an oracle. See the host-owned module example in
  [README.md](README.md).
- Keep default `ServiceLoader` provider selection explicit and validate unexpected
  provider counts. Preserve the vault's failure behavior when neither a run-scoped
  nor a default implementation is available.
  `larky.modules.vgs.vault.enableInMemoryVault` is an explicit development/test
  opt-in, not a production vault implementation; do not enable it silently or
  substitute an in-memory vault after a provider failure.

### Providers scoped to one run (PR #830)

[PR #830](https://github.com/verygoodsecurity/starlarky/pull/830), stacked on
#828/#829 and not yet merged into this branch, adds explicit per-run providers.
See [AGENTS.md](AGENTS.md) for the architecture and compatibility contract.

- Treat `ModuleProviders` as host-granted capabilities, not script-controlled
  configuration. The host must choose a vault and session authorized for the
  current message and tenant, and ensure that the two agree in scope. Passing a
  registry does not itself authenticate the caller or authorize vault operations.
- Install providers on the current evaluation/call's `StarlarkThread`. Never
  capture them in shared modules, loaded-script defaults, or compilation caches.
  Repeated calls must not inherit another call's providers. The registry is
  immutable; provider implementations remain responsible for their own mutable
  state, concurrency, authorization, and lifetime.
- An explicit run provider wins over the module's default. Without it, legacy
  `ServiceLoader` behavior remains; absence is not an automatic deny. Hosts that
  require message-scoped capabilities must require the appropriate providers
  before invocation and secure any fallback provider's authorization behavior.
  Never retry a rejected operation through a more privileged fallback provider.
- Accept only the supported provider types and reject invalid registry bindings
  consistently in evaluation and invocation. A supplied gateway session works
  without a `ServiceLoader` host; with no supplied session, use the host's current
  session or fail. Do not fabricate a session when neither is available.
- Gateway card/text aliases are session stand-ins, not raw values or proof of
  authorization. They are revealed through the authorized vault. The session
  provider must enforce adapter capture permissions and storage bounds for
  `get`/`put`; a shared facade does not make session data safe to share between
  messages. Keep aliases and captured sensitive values out of host diagnostics.

## Resource exhaustion and native operations

- Preserve evaluation step/deadline limits and interruption behavior. Long-running
  Java operations must also consider cancellation and bounded work; interpreter
  checks alone do not bound a native method's cost. Hosts must configure limits
  appropriate to their workload rather than assume every execution is bounded.
- Validate sizes before allocating, expanding, or converting script-controlled
  values. Preserve bounds on caches, decompressed output, random-byte requests,
  and large integer operations. Test boundary and failure cases with small,
  controlled inputs rather than exhausting the test machine.
- Keep XML external entities and external resource resolution disabled where
  parsing untrusted content. Review serializers, parsers, regex operations, and
  compression for unsafe input handling and excessive CPU or memory use.
- Preserve safe subprocess argument handling and temporary-file cleanup in the
  Python wrapper. CLI output files, logs, and REPL history are persistent surfaces;
  use synthetic data and avoid entering secrets into history.

## Cryptography and sensitive data

- Use the project's vetted cryptographic providers rather than implementing new
  primitives. Validate algorithm parameters, keys, nonces/IVs, padding, encodings,
  and verification failures; use cryptographically secure randomness where needed.
- Existing compatibility APIs include legacy algorithms. Do not introduce new
  insecure defaults or silently change compatibility behavior. Explain the use
  case and security implications of algorithm or provider changes and add known
  vectors and negative tests.
- Never commit production credentials, private keys, real payment data, or PII.
  Use clearly synthetic fixtures and test-only keys. Do not add host logging,
  exception messages, metrics, or debug output that disclose secrets, full PAN,
  CVV, authentication tokens, or customer payloads.
- Payment and vault modules can process sensitive data by design. Minimize copies
  and retention, keep production authorization and protected storage in the host
  or configured provider, and do not persist CVV. Avoid claiming that tokenization
  or this library alone establishes PCI compliance.

## Security changes and verification

Add regression tests for the affected boundary, including rejected inputs and
cross-evaluation behavior where applicable. Test the native runner when changing
Java APIs or resources reachable from scripts, and the Python wrapper when changing
its process/file boundary. Document capability, provider, data exposure, or limit
changes in the PR. Review dependency vulnerabilities and existing security pins;
do not bypass security checks or suppress findings without a specific rationale.
