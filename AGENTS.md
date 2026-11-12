# VGS Java Repo Coding Style Guide

Please analyze the code changes and provide the following information:
1. **Intent and Purpose**: Summarize the goal of the changes. Explain what functionality or behavior the changes aim to introduce, modify, or fix.
2. **Positive Feedback**: Explicitly acknowledge good coding practices, clever solutions, or marked improvements. A brief word of praise goes a long way.
3. **Potential Issues**: Identify any possible problems the changes might introduce, such as:
  - Bugs or unintended side effects.
  - Performance concerns.
  - Code quality issues (e.g., readability, maintainability, scalability).
  - Violations of best practices or standards.
4. **Suggestions for Improvement**: Recommend any improvements or alternative approaches. Focus on patterns and conventions that will simplify the code base. Favor convention over configuration with sensible defaults.
5. **PCI Compliance Review**: Check for any potential PCI violations, specifically storing data unencrypted (via persistence or logging).

# Key Principles
- **Readability:** Code should be easy to understand for all team members.
- **Maintainability:** Code should be easy to modify and extend. Methods should be small and named well.
- **Consistency:** Adhering to a consistent style across all projects improves
  collaboration and reduces errors.
- **Performance:** While readability is paramount, code should be efficient. Call out code that is heavy on database access or may take database locks that result in long transactions.

# Comments
- **Write clear and concise comments:** Explain the "why" behind the code, not the "what".
- **Comment sparingly:** Well-written code should be self-documenting where possible.
- **Use complete sentences:** Start comments with a capital letter and use proper punctuation.

## Logging
- **Log at appropriate levels:** DEBUG, INFO, WARNING, ERROR, CRITICAL - Use INFO and higher sparingly.
- **Reserve `ERROR` for non-retryable failures and genuine programming errors** (e.g.
  `NullPointerException`, illegal state, a poison message being discarded). A failure the
  system expects to recover from on its own — a retryable/transient condition that triggers
  an SQS redelivery, a bounded retry, or a circuit-breaker recovery — should log at `WARN`,
  not `ERROR`. `ERROR` is a paging/alert signal; retryable conditions that self-heal are
  noise at that level. Concretely: if the catch block rethrows a `*RetryableException`, sets
  a `shouldRetry`/visibility-extend flag, or otherwise hands the work back for another
  attempt, use `WARN`.
- **Provide context:** Include relevant information in log messages to aid debugging.

# Readability and Maintainability
- **Code formatter:** Recommend formatting to google's java style guide
- Follow Spring Boot best practices
- Use JPA projections for performance
- Implement sensible logging and comprehensive metrics
- Write integration tests for critical paths
- Prefer conventions over configurations
- Don't use magic numbers in code. Numbers should be defined as constants or variables with meaningful names.
- **Time and Dates:** Always use an injected `java.time.Clock` instance to obtain the current time or date instead of using static methods like `LocalDateTime.now()`, `Instant.now()`, or `System.currentTimeMillis()`. This ensures that time-dependent logic is deterministic and easily testable by injecting a fixed clock during tests.
- Method Size and Cyclomatic Complexity Recommendations
    - Cyclomatic Complexity should ideally be Optimal (<=10) or Acceptable/Moderate Complexity (11-20). Anything higher should be noted for refactoring.
    - Methods should Focus on Readability and Single Responsibility. Less than 50 lines where possible
    - Refactor long methods: Break down complex methods into smaller, more focused ones.
    - Simplify control structures: Reduce nested conditionals, loops, and utilize early returns (guard clauses).
    - Extract complex logic: Move intricate parts of the code into separate, smaller functions.
    - Use design patterns: Apply patterns like Strategy or State to manage decision-heavy logic efficiently.
- Naming
    - Choose names that clearly and accurately describe what the method does, why it exists, and how it should be used.
- Identify framework candidates
    - All network communications and fault tolerance code should be common. This allows us to focus on providing a common baseline of hehavior and a smaller footprint of technology to maintain.
    - Development of framework level code should be separate and apart from application level code. If someone is building a framework feature or a tool then they should not be writing application logic in the same change request.
    - If code does not appear to be boilerplate during feature development then this may indicate that the code being developed is going to be brittle or expensive to maintain. 
    - If you see code that does not look like it's part of a common library please highlight this finding.
 - To enhance actioning these particular findings please include the string "@team-tech-leads" in any specific to trigger a notification and discussion with the tech leads team to review the potential issue.
 - Recommend the engineer update [ARCHITECTURE.md](../ARCHITECTURE.md) if they are introducing a new pattern or change. Any changes to this doc should require review from "@team-tech-leads".

## Build & Dependency Management (Maven)

Apply these when reviewing any `pom.xml` change:
- **Inherit from the parent.** Do not add a `dependencyManagement` **or `pluginManagement`** entry that merely restates a version the parent chain already manages at the same value — delete it and inherit. Flag such re-declarations. (Check the full chain, e.g. `spring-parent-pom-25` → `base-pom-25` / `spring-boot-starter-parent`; plugins like `protobuf-maven-plugin` are managed there.)
- **No versions outside management.** A `<version>` on a `<dependency>` under `<dependencies>`, or on a `<plugin>` under `<build><plugins>`, should be moved into the root `<dependencyManagement>` / `<pluginManagement>` and referenced without a version. Legitimate exceptions: `<build><extensions>` (not manageable via `pluginManagement`) and plugin config coordinate strings (e.g. protoc/grpc artifacts).
- **Versions live in `<properties>`.** No literal version numbers inline; reference via `${...}`.
- **Override the parent only deliberately.** Any entry that pins a version different from the parent must carry a comment explaining why (CVE/vuln fix, compatibility hold, etc.). Do not silently override, and do not "dedupe" away a commented pin.
- Internal multi-module (reactor) dependencies use `${project.version}` and are exempt from the "no versions outside management" rule.

## Security Review
All changes must undergo a lightweight security assessment. Raise early concerns; prefer preventing insecure patterns over post-facto patching.

### Security Review Checklist
- Input validation: All external inputs validated, normalized, length-bounded.
- Authentication: No bypass; leverages shared auth framework (do not hand-roll JWT parsing).
- Authorization: Every protected operation enforces role/scope checks (no UI-only enforcement).
- Secrets: No secrets (API keys, private keys, tokens) hard-coded, committed, or echoed in logs.
- Cryptography: Use vetted libraries; no custom crypto. Confirm correct algorithm, mode, padding, IV randomness.
- Sensitive data (PAN, CVV, cardholder name, token, PII): Never logged in plaintext. Mask where operationally necessary.
- PCI scope: Card data only processed within approved components; tokenization preferred.
- Storage: Persistent writes of sensitive data encrypted at rest; transient caches have TTL and are cleared.
- Transport: External calls enforce TLS 1.2+; certificate pinning considered for high-risk endpoints.
- Error handling: No stack traces or internal IDs leaked to clients.
- Injection prevention: Use parameterized queries (SQL), safe template engines, escaping for HTML/JSON/headers.
- Deserialization: Avoid unsafe native deserialization of untrusted data.
- SSRF / RCE: No dynamic outbound calls formed directly from user input.
- Logging: Severity appropriate; debug logs must not contain secrets or full PAN.
- Dependency hygiene: Libraries free of known CVEs; run automated scan before merge.
- Concurrency: Synchronized mutable shared state or replaced with thread-safe constructs.
- DOS mitigation: Bounded loops, timeouts, connection pools sized sensibly, reactive flows not blocked.
- Third-party calls: Timeouts, circuit breakers, retries with jitter (common framework preferred).
- Framework candidates: If custom security patterns appear, tag "@team-tech-leads" and update [ARCHITECTURE.md](../ARCHITECTURE.md).

### Sensitive Data Handling
- Mask PAN: Show only first 6 and last 4 digits when strictly needed.
- Do not store CVV.
- Use tokenization for recurring references.
- In-memory exposure minimized; clear large buffers promptly.
- Avoid copying sensitive strings; prefer char[] only when required then overwrite.

### Logging Rules (Security)
- Never log full PAN, CVV, auth tokens, session IDs, private keys.
- Use structured logging with field-level masking.
- Redact failure reason details that could aid enumeration.

### Pull Request Expectations
Include a short "Security Impact" section:
- Data touched (PAN, PII, none).
- New external endpoints called.
- Changes in authN/authZ behavior.
- Any new secrets introduced (must be vaulted).

### Common Pitfalls
- Using `.block()` in reactive flows causing thread starvation and potential timeout amplification.
- Storing raw card data in cache without TTL.
- Re-implementing crypto or token parsing.
- Logging unsanitized user input.

### Escalation
Tag "@team-tech-leads" if:
- Introducing a new encryption model.
- Adding cross-service authorization pattern.
- Creating custom secret management or credential rotation logic.
- Expanding PCI boundary or handling new sensitive fields.
- Introducing a new concurrency/retry/tracing pattern or custom framework-like behavior.

## PCI Compliance (Expanded)
- Confirm no unencrypted PAN persistence.
- Review logs and metrics emission paths.
- Ensure test fixtures do not commit real PANs.
- Validate scrubbing in error handlers.

## Scalability & Performance Review

### Concurrency & Blocking
- No blocking calls on non-blocking / listener / event-loop threads (e.g., SQS listeners, reactive pipelines).
    - Java smells: `Thread.sleep`, `CompletableFuture.join()` / `.get()` (especially without timeout), `Future.get()`, `CountDownLatch.await()` without timeout.
    - Reactor/WebFlux smells: `.block()`, `.blockFirst()`, `.blockLast()`, `toFuture().get()` in request/consumer paths.
- All waits have explicit timeouts (locks, futures, latches, HTTP calls, DB calls where supported).
- Queues/buffers are bounded, with backpressure or concurrency limits (avoid unbounded buffering).
- Synchronization is minimal and not coarse-grained (`synchronized` hotspots, shared locks, contention risks are called out).

### Threading & Execution Context
- No heavy work on I/O threads (JSON parsing, crypto, compression, large mapping, DB calls) without explicit offloading.
- Avoid accidental parallelism (`parallelStream()` in services is usually a scalability risk).
- Executor usage is explicit and sized (avoid default `ForkJoinPool.commonPool()` for server workloads).
- Consumer concurrency is explicitly controlled (thread counts, prefetch/batch size, max in-flight).

### Database & Transactions
- No N+1 query patterns; avoid DB/network calls inside loops without batching.
- No unbounded queries (`findAll()` / full table scans / missing pagination for large tables).
- Transactions are scoped tightly:
    - No long transactions around network calls, slow loops, or large payload transforms.
    - Query patterns that change require index review (and query plan review for large tables).
- Prefer bulk operations (batch inserts/updates) where appropriate.
- **Read-Only Transactions:** All read-only database operations must be explicitly annotated with `@Transactional(readOnly = true)`. This is required to properly route read-only queries to the database read replica.


### Network Calls & Retries
- All outbound calls have connect/read timeouts (and overall deadline where possible).
- Retry policies are bounded and safe:
    - Budgeted retries, exponential backoff, jitter.
    - No retry storms (no retries inside large loops without caps).
    - Non-idempotent operations are not retried unless explicitly designed to be safe.
- Connection pooling is used (HTTP clients, DB pools) with sane pool sizing.

### Memory & Payload Size
- No unbounded in-memory accumulation (avoid reading full streams into memory, collecting huge result sets, building large lists in hot paths).
- Input sizes are validated and capped (request bodies, message payloads, JSON fields).
- Avoid excessive allocation/churn in hot paths (reuse `ObjectMapper`, avoid per-call client creation, avoid repeated expensive parsing).

### Logging, Metrics & Tracing Overhead
- Logging in hot paths is controlled:
    - Parameterized logs (avoid expensive string building).
    - No per-record INFO logs in high-volume flows.
- Tracing context propagation is correct and non-blocking (MDC/trace context preserved across threads/async boundaries).
- Avoid high-cardinality metrics/tags (e.g., raw IDs) that can explode metrics/traces cost.
- Percentiles-histograms are too expensive for high-cardinality or high-volume metrics; prefer simple counters/gauges where possible.
- Consider reducing log level and log size (such as not logging exceptions or full payloads) to reduce logging costs. But include enough context to be actionable for debugging such as card ID and error details.

### Load/Failure Behavior (Resilience)
- Concurrency limits/bulkheads/rate limits exist to prevent downstream overload (especially for consumers).
- Poison message handling defined (DLQ/redrive policy) to avoid tight failure loops.
- Graceful degradation preferred over OOM (shed load when saturated, bound internal queues).

### Explicit “Smells” Reviewers Should Flag
- Loops containing DB or outbound network calls.
- Missing timeouts anywhere.
- Blocking calls in listener/request threads.
- Unbounded collections/buffers.
- Excessive synchronization or shared mutable state.
- Scheduled jobs with `while(true)` / no jitter / no backoff.

## Sub-Doc Maintenance

When a PR touches a domain covered by a doc under `ai-docs/`, suggest the author also update the matching sub-doc so the trigger-table context stays accurate.
Always keep docs up to date.
Ensure all code is commented
