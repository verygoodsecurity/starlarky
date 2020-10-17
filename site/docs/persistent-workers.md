---
layout: documentation
title: Persistent workers
---

# Persistent workers

_Persistent workers_ (often called _workers_ for short) is an
[execution strategy](https://docs.bazel.build/versions/master/user-manual.html#strategy-options)
that decreases start-up overhead, allows more JIT compilation, and enables
caching of for example the abstract syntax trees in the action execution. This
strategy achieves these improvements by sending multiple requests to a
long-running process. Persistent workers are available for
[Java (built-in)](https://cs.opensource.google/bazel/bazel/+/master:src/java_tools/buildjar/java/com/google/devtools/build/buildjar/BazelJavaBuilder.java),
[TypeScript](https://bazelbuild.github.io/rules_nodejs/TypeScript.html),
[Scala](https://github.com/bazelbuild/rules_scala), and more.

## Using persistent workers <a name="usage"></a>

[Bazel 0.27 and higher](https://blog.bazel.build/2019/06/19/list-strategy.html)
by default uses persistent workers when executing builds, though remote
execution takes precedence. For actions that do not support persistent workers,
Bazel falls back to regular workers instead. You can explicitly set your build
to use persistent workers by setting the `worker`
[strategy](user-manual.html#strategy-options) for the applicable tool mnemonics.
As a best practice, this example includes specifying `local` as a fallback to
the `worker` strategy:

```
bazel build //my:target --strategy=Javac=worker,local
```

Using the workers strategy instead of the local strategy can boost compilation
speed significantly, depending on implementation. For Java, we typically see 2–4
times faster builds, sometimes more for incremental compilation. Compiling Bazel
is about 2.5 times as fast with workers. For more details, see the
"[Choosing number of workers](#number-of-workers)" section.

If you also have a remote build environment that matches your local build
environment, you can use the experimental
[_dynamic_ strategy](https://blog.bazel.build/2019/02/01/dynamic-spawn-scheduler.html),
which races a remote execution and a worker execution. To enable the dynamic
strategy, pass the
[--experimental_spawn_scheduler](command-line-reference.html#flag--experimental_spawn_scheduler)
flag. This strategy automatically enables workers, so there is no need to
specify the `worker` strategy, but you can still use `local` or `sandboxed` as
fallbacks.

## Choosing number of workers <a name="number-of-workers"></a>

The default number of worker instances per mnemonic is 4, but can be adjusted
with the
[`worker_max_instances`](command-line-reference.html#flag--worker_max_instances)
flag. There is a trade-off between making good use of the available CPUs and the
amount of JIT compilation and cache hits you get. With more workers, more
targets will pay start-up costs of running non-JITted code and hitting cold
caches. If you have a small number of targets to build, a single worker may give
the best trade-off between compilation speed and resource usage (for example,
see [issue #8586](https://github.com/bazelbuild/bazel/issues/8586). The
`worker_max_instances` flag sets the maximum number of worker instances per
mnemonic and flag set (see below), so in a mixed system you could end up using
quite a lot of memory if you keep the default value. For incremental builds the
benefit of multiple worker instances is even smaller.

This graph shows the from-scratch compilation times for Bazel (target
`//src:bazel`) on a 6-core hyper-threaded Intel Xeon 3.5 GHz Linux workstation
with 64 GB of RAM. For each worker configuration, we ran 5 clean builds and took
the average of the last 4.

<p align="center">
<img width="596px" alt="Graph of performance improvements of clean builds" src="/assets/workers-clean-chart.png">
</p>

For this configuration, two workers give the fastest compile, though at only 14%
improvement compared to one worker. One worker is a good option if you want to
use less memory.

Incremental compilation typically benefits even more. Clean builds are
relatively rare, but changing a single file between compiles is common, in
particular in test-driven development. The above example also has some non-Java
packaging actions to it that can overshadow the incremental compile time.
Recompiling the Java sources only
(`//src/main/java/com/google/devtools/build/lib/bazel:BazelServer_deploy.jar`)
after changing an internal string constant in
[AbstractContainerizingSandboxedSpawn.java](https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/sandbox/AbstractContainerizingSandboxedSpawn.java)
gives a 3x speed-up (average of 20 incremental builds with one warmup build
discarded):

<p align="center">
<img width="592px" alt="Graph of performance improvements of incremental builds" src="/assets/workers-incremental-chart.png">
</p>

The speed-up depends on the change being made. We have measured a speed-up of a
factor 6 in the above situation when a commonly used constant was changed.

## Modifying persistent workers<a name="options"></a>

You can pass the
[`--worker_extra_flag`](command-line-reference.html#flag--worker_extra_flag)
flag to specify start-up flags to workers, keyed by mnemonic. For instance,
passing `--worker_extra_flag=javac=--debug` turns on debugging for Javac only.
Only one worker flag can be set per use of this flag, and only for one mnemonic.
Workers are not just created separately for each mnemonic, but also for
variations in their start-up flags. Each combination of mnemonic and start-up
flags is combined into a `WorkerKey`, and for each `WorkerKey` up to
`worker_max_instances` workers may be created. See the next section for how the
action configuration can also specify set-up flags.

You can use the
[`--high_priority_workers`](command-line-reference.html#flag--high_priority_workers)
flag to specify a mnemonic that should be run in preference to normal-priority
mnemonics. This can help prioritize actions that are always in the critical
path. If there are two or more high priority workers executing requests, all
other workers are prevented from running. This flag can be used multiple times.

Passing the
[`--worker_sandboxing`](command-line-reference.html#flag--worker_sandboxing)
flag makes each worker request use a separate sandbox directory for all its
inputs. Setting up the sandbox takes some extra time, especially on MacOS, but
gives a better correctness guarantee.

You can use the `--experimental_worker_allow_json_protocol` flag to allow
workers to communicate with Bazel through JSON instead of protocol buffers
(protobuf). The worker and the rule that consumes it can then be modified to
support JSON.

The
[`--worker_quit_after_build`](command-line-reference.html#flag--worker_quit_after_build)
flag is mainly useful for debugging and profiling. This flag forces all workers
to quit once a build is done. You can also pass
[`--worker_verbose`](command-line-reference.html#flag--worker_verbose) to get
more output about what the workers are doing.

Workers store their logs in the `<outputBase>/bazel-workers` directory, for
example
`/tmp/_bazel_larsrc/191013354bebe14fdddae77f2679c3ef/bazel-workers/worker-1-Javac.log`.
The file name includes the worker id and the mnemonic. Since there can be more
than one `WorkerKey` per mnemonic, you may see more than `worker_max_instances`
log files for a given mnemonic.

For Android builds, see details at the
[Android Build Performance page](android-build-performance.html).

## Implementing persistent workers<a name="implementation"></a>

Persistent workers are implemented for multiple languages, including Java,
[TypeScript](https://bazelbuild.github.io/rules_nodejs/TypeScript.html),
[Scala](https://github.com/bazelbuild/rules_scala),
[Kotlin](https://github.com/bazelbuild/rules_kotlin), and more. You can
implement persistent workers in other languages and for other tools, as well.

Each worker is a long-running process started by the Bazel server, which
functions as a _wrapper_ around the actual _tool_ (typically a compiler). In
order to benefit from persistent workers, the tool must support doing a sequence
of compilations, and the wrapper needs to translate between the tool's API and
the request/response format described below. The wrapper must recognize the
`--persistent_worker` command-line flag and only make itself persistent if that
flag is passed, otherwise it must do a one-shot compilation and exit. The same
worker program might be called with and without the `--persistent_worker` flag
in the same build, and is responsible for appropriately spawning and talking to
the tool, as well as shutting down workers on exit. Each worker instance is
assigned (but not chrooted to) a separate working directory under
`<outputBase>/bazel-workers`.

The Bazel server communicates with the worker using stdin/stdout. It supports
the use of protocol buffers or JSON strings. Support for JSON is experimental
and thus subject to change. It is guarded behind the
`--experimental_worker_allow_json_protocol` flag.

When using protobuf, the compilation requests are sent as
[`WorkerRequest`](https://github.com/bazelbuild/bazel/blob/master/src/main/protobuf/worker_protocol.proto)
protocol buffers in standard binary format, and responses are similarly returned
as
[`WorkerResponse`](https://github.com/bazelbuild/bazel/blob/master/src/main/protobuf/worker_protocol.proto)
protocol buffers. Each protocol buffer is preceded by its length in varint
format (see
[`MessageLite.writeDelimitedTo()`](https://developers.google.com/protocol-buffers/docs/reference/java/com/google/protobuf/MessageLite.html#writeDelimitedTo-java.io.OutputStream-).

JSON requests uphold the same structure as the protobuf, but uses standard JSON.
Bazel stores the requests as protobufs and converts them to JSON using
[protobuf's JSON format](https://source.corp.google.com/piper///depot/google3/java/com/google/protobuf/util/JsonFormat.java)
Responses are parsed by a JSON parser into the same structure as the
WorkResponse protobuf, then converted to proto manually.
JSON requests and responses are not preceded by a size indicator.

The request's `args` field should contain a list of strings that describe
the action to be done. The `inputs` field may contain input file names and their
hash digests, allowing the caching of intermediate results without having to
recompute the digest.

<p class="warning">Because responses are sent on stdout, neither the worker nor the underlying tool should write other messages into that stream.</p>

Writing other things to stdout crashes the worker. Any output that should be
shown to the user can be put in the `output` field of the response, and output
that should be logged should go to stderr. The wrapper should make sure that
what the tool writes on stdout is appropriately redirected.

To enable the `worker` strategy for an action, the `execution_requirements` for
that action must include `{"supports-workers": "1"}`. It can also include a
`requires-worker-protocol` requirement specifying whether Bazel should
communicate with that worker using `json` or `proto`. This is required for JSON
but is optional for proto since proto is the default. You can also add a
`worker-key-mnemonic` to the `execution_requirements` section, allowing the
mnemonic for workers to be separate from the mnemonic for the action. This can
be useful when the same executable is used for several mnemonics, though it
limits how much the user can control when to use workers.

The action definition must also contain an `arguments` definition with a
flag-file (`@`-preceded) argument at the end. Any non-flag-file arguments are
_startup flags_ that will be passed to the worker on startup, allowing
configuration common to all requests. The flag-file argument is used to read
arguments for each request, including possible _non-startup flags_. To pass an
argument starting with a literal `@`, start the argument with `@@` instead. If
an argument is also an external repository label, it will not be considered a
flag-file argument.

This example shows a Starlark configuration for a worker that uses JSON:

```python
args_file = ctx.actions.declare_file(ctx.label.name + "_args_file")
ctx.actions.write(
    output = args_file,
    content = "\n".join(["-g", "-source", "1.5"] + ctx.files.srcs),
)
ctx.actions.run(
    mnemonic = "SomeCompiler",
    executable = "bin/some_compiler_wrapper",
    inputs = inputs,
    outputs = outputs,
    arguments = [ "-max_mem=4G",  "@%s" % args_file.path],
    execution_requirements = {
        "supports-workers" : "1", "requires-worker-protocol" : "json" }
)
```
With this definition, the first use of this action would start with executing
the command line `/bin/some_compiler -max_mem=4G --persistent_worker`. A request
to compile `Foo.java` would then look like:

```prototext
arguments: [ "-g", "-source", "1.5", "Foo.java" ]
inputs: [
  {path: "symlinkfarm/input1" digest: "d49a..." },
  {path: "symlinkfarm/input2", digest: "093d..."},
]
```

The worker receives this on stdin in JSON format (because
"requires-worker-protocol" is set to JSON, and
`--experimental_worker_allow_json_protocol` is passed to the build to enable
this option). To communicate with the associated worker using binary-encoded
protobuf instead of json, `requires-worker-protocol` would be set to `proto`,
like this:
```
  execution_requirements = {
    "supports-workers" : "1" ,
    "requires-worker-protocol" : "proto"
  }
```
If you do not include `requires-worker-protocol` in the execution requirements,
Bazel will default the worker communication to use protobuf.

Bazel derives the `WorkerKey` from the mnemonic and the shared flags, so if this
configuration allowed changing the `max_mem` parameter, a separate worker would
be spawned for each value used. This can lead to excessive memory consumption if
too many variations are used.

Each worker can currently only process one request at a time. The experimental
[multiplex workers](multiplex-worker.html) feature allows using multiple
threads, if the underlying tool is multithreaded and the wrapper is set up to
understand this.

In [this GitHub repo](https://github.com/Ubehebe/bazel-worker-examples), you can
see example worker wrappers written in Java as well as in Python.

## How do workers affect sandboxing? <a name="sandboxing"></a>

Using the `worker` strategy by default does not run the action in a sandbox,
similar to the `local` strategy. You can set the `--worker_sandboxing` flag to
run all workers inside sandboxes, making sure each execution of the tool only
sees the input files it's supposed to have. The tool may still leak information
between requests internally, for instance through a cache. Using `dynamic`
strategy
[requires workers to be sandboxed](https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/exec/SpawnStrategyRegistry.java).

To allow correct use of compiler caches with workers, a digest is passed along
with each input file. Thus the compiler or the wrapper can check if the input is
still valid without having to read the file.

Even when using the input digests to guard against unwanted caching, sandboxed
workers offer less strict sandboxing than a pure sandbox, because the tool may
keep other internal state that has been affected by previous requests.

## Further reading <a name="further-reading"></a>

For more information on persistent workers, see:

*   [Original persistent workers blog post](https://blog.bazel.build/2015/12/10/java-workers.html)
*   [Haskell implementation description](https://www.tweag.io/blog/2019-09-25-bazel-ghc-persistent-worker-internship/)
*   [Blog post by Mike Morearty](https://medium.com/@mmorearty/how-to-create-a-persistent-worker-for-bazel-7738bba2cabb)
*   [Front End Development with Bazel: Angular/TypeScript and Persistent Workers
    w/ Asana](https://www.youtube.com/watch?v=0pgERydGyqo)
*   [Bazel strategies explained](https://jmmv.dev/2019/12/bazel-strategies.html)
*   [Informative worker strategy discussion on the bazel-discuss mailing list](https://groups.google.com/forum/#!msg/bazel-discuss/oAEnuhYOPm8/ol7hf4KWJgAJ)
