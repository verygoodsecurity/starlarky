# larky-bench

An A/B benchmark for two builds of Larky. Its output follows
[absh](https://github.com/stepancheg/absh), and it adds what a JVM needs to measure warm calls
of tens of microseconds: both builds run in the same JVM, on the same thread, in alternating
blocks, and each fork checks that the two builds produced the same output.

```sh
mvn -f bench/pom.xml package          # bench/target/larky-bench.jar; not part of the root build
java -jar bench/target/larky-bench.jar \
  -a base/larky/target/larky-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
  -b candidate/larky/target/larky-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
  -w StarlarkCall --workload-src bench/workloads/StarlarkCall.java \
  -s "$PWD/bench/scenarios/http_headers.star#header_edits"
```

A build is a classpath. Larky's `jar-with-dependencies` jar includes libstarlark and Larky's
dependencies, so it works on its own. With the plain jars, list the Larky and libstarlark jars
and give the dependencies with `--extra-cp`.

## Output

For wall time, thread CPU time and thread allocation, all per call, it prints absh's summary of
each build. The summary has n, mean, std, se, min, max and median, a distribution on a shared
scale, and the B/A ratio with a 95% interval:

```
cpu us/call (one sample per block)
A: n=600 mean=14.305 std=3.617 se=0.148 min=8.800 max=23.467 median=14.800
B: n=600 mean=3.923 std=1.033 se=0.042 min=2.417 max=8.233 median=3.717
A:           ▆▄█▃▃▃▃▂▃▃█▇▃▂▂▆▄▃▃▃
B: ▇█▆▄▅▃▁▁▁▁
B/A: 0.274 0.271..0.278 (95% conf)
```

`summary.md` adds a table for each fork. It has each build's p50 and p95 latency per call, its
CPU time and allocation per call, the median across forks, and the number of forks in which B was
lower. `blocks.csv` and `latency.csv` hold the raw samples, and `logs/fork-N.log` holds each fork's
output.

## Method

- **Isolation.** Each build gets its own class loader, whose parent is the platform loader, so the
  two builds share only the JDK. JVM flags and system properties apply to both.
- **Pairing.** Each block runs `--chunk` calls of A and `--chunk` calls of B, back to back. The
  build that goes first alternates from block to block, and even forks start with B. Passing `-r`
  orders the two builds at random in each block instead. Warmup runs the same way, and its blocks
  are not recorded.
- **Samples.** A sample is the per-call mean over one block: wall time from `System.nanoTime`,
  CPU time and allocated bytes from `com.sun.management.ThreadMXBean` for the calling thread. The
  p50 and p95 values in the table are percentiles of the individual calls.
- **Ratio interval.** B/A is the ratio of the mean per-call values. The 95% interval comes from
  resampling blocks 2,000 times with a fixed seed. Each resample keeps a block's A and B samples
  together, so host noise that slows both of them does not widen the interval. The interval
  covers only the noise within this run. It does not cover a busier or quieter host on another
  day, so compare builds within one run.
- **Parity.** After warmup and again after measurement, each fork compares the workload's
  `fingerprint` for the two builds. If they differ, the fork fails and the run stops.

Warm the JIT before you measure: with too few warmup calls the first build to compile skews
the result. The defaults are 200 blocks of 60 calls, which gives 12,000 warmup and 12,000 measured
calls per build per fork, over 3 forks.

## Workloads

A workload is a class with JDK types only in its signatures, because it crosses class loaders:

- `public static Runnable prepare(String spec)`: does the setup once, and returns one call.
- `public static String fingerprint(String spec)` (optional; `inspect` is also accepted): a
  digest of the output, compared between the builds.
- `public static String describe()` (optional; `sourceJar` is also accepted): printed in the
  report, for example the jar the build loaded.

`--workload-src` compiles the workload against each build separately.

- **`workloads/StarlarkCall.java`.** Its spec is `path/to/file.star#function`. It evaluates the
  file once, and each call invokes `function()`. The fingerprint is `repr()` of the result. It
  requires a build whose engine implements `javax.script.Invocable`.
- **`scenarios/http_headers.star`.**
  - `request_from_dict` and `request_from_pairs` construct a `VGSHttpRequest`.
  - `request_and_header_edits` and `host_projection` cover the full request.
  - `headers_from_pairs`, `header_edits` and `header_lookups` cover the header multidict alone.
- **`workloads/atlas/AtlasRequestProfile.java`.** This runs Atlas Horizon's `ExecutionWrapper` in
  process, from the request or response protobuf to the serialized output, with the module cache
  and program cache warm. The scenarios are `response`, `short-response`, `ordinary` and
  `terminal`. Its fingerprint checks the output, and checks that each compile cache keeps a single
  program and the same module identities, with no warm library initialization. Run it with
  `examples/atlas-two-caches.sh`, giving Atlas's classes and dependencies in `ATLAS_CP`. Atlas
  names its generated helpers at random on every call. A benchmark-only overlay that makes those
  names stable (`ATLAS_OVERLAY`) lets the program cache hit, and without it the cache check fails.

## Cold start: absh itself

For whole processes, such as JVM startup or the native `larky-runner`, use absh directly. Its
`-m` option adds max RSS:

```sh
absh -m -n 30 \
  -a 'base/runlarky/target/larky-runner -s script.star' \
  -b 'candidate/runlarky/target/larky-runner -s script.star'
```
