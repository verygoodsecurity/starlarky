# Running WebAssembly from Larky (`@vgs//wasm`)

`@vgs//wasm` lets a Larky script run a WebAssembly module that you provide. Use it when you need
code that cannot be written in Larky, such as a payment processor's JavaScript library: compile it
to WebAssembly and call it from your script.

## Host authorization and security boundary

WASM compilation and execution from Larky are disabled by default. The host must explicitly set
`-Dlarky.wasm.enabled=true` and supply a finite `LARKY_EVAL_EXPIRATION_MS` evaluation deadline.
Both conditions are checked for every load and run, including runs of previously loaded handles.
Attaching a module's bytes does not authorize its execution. Direct Java callers of `WasmRuntime`
remain responsible for choosing their own limits and host policy; this gate protects `@vgs//wasm`.

Enabling WASM changes the language's guarantees. WASM permits arbitrary loops and recursion;
a wall-clock deadline does not make it non-Turing-complete or provide deterministic instruction
fuel. `random_get` uses secure random bytes, so repeated runs need not return identical results.
The current integration is therefore not a deterministic sandbox.

The run deadline does not interrupt parsing or compilation, including waits for a shared cached
compilation. Larky checks expiration before compilation and after it returns, so an overdue compile
cannot return success. Endive preflights guest lengths before its parser allocates from them; both
engines cap declared table minima and future growth. These limits do not provide a total JVM heap,
CPU or Metaspace budget for parsing, compilation and execution. Hosts that need an enforceable
process-level resource boundary for hostile WASM should run it in a separately resource-limited
process. The opt-in, deadline, memory and output limits do not make this in-process integration a
complete sealed sandbox.

## What a module is

A module is a [WASI preview 1](https://github.com/WebAssembly/WASI/blob/main/legacy/preview1/docs.md)
command: it exports `_start` and its `memory`, imports only WASI preview 1 functions (with their
standard signatures), and has no start section. Every call starts a fresh instance, so nothing
carries over from one call to the next.

| What the module sees | Value |
| --- | --- |
| stdin | the input you pass |
| stdout | the result you get back |
| stderr | captured, for diagnostics |
| argv | `["module"]` |
| environment variables | none |
| files and directories | none (no preopens) |
| network | none |
| clocks (`clock_time_get`) | always 0; a clock id WASI does not define (above 3) returns `INVAL` (28) |
| `random_get` | secure random bytes |
| every other WASI function (`poll_oneoff`, `clock_res_get`, files, sockets, ...) | returns `NOSYS` (52) without doing anything |

The WASI functions that do something are `args_get`, `args_sizes_get`, `environ_get`,
`environ_sizes_get`, `clock_time_get`, `random_get`, `fd_read` (stdin), `fd_write` (stdout and
stderr), `fd_close`, `fd_fdstat_get`, `fd_seek` (ESPIPE), `fd_prestat_get` and
`fd_prestat_dir_name` (EBADF: no preopens), `sched_yield` and `proc_exit`. A pointer or length
outside the module's memory traps.

Returning from `_start` exits with 0; `proc_exit(n)` exits with `n`.

## Limits

| Limit | Default | Host setting | When exceeded |
| --- | --- | --- | --- |
| Linear memory | 64 MiB (1024 pages) | `larky.wasm.maxMemoryBytes` for Larky; `WasmRuntime.Limits` for Java callers | a module whose initial memory is larger fails to start; `memory.grow` past the limit returns -1 |
| Tables | 64 tables and 65,536 entries combined | `larky.wasm.maxTables`, `larky.wasm.maxTableElements` | oversized initial declarations fail compilation; `table.grow` past the capped maximum returns -1 |
| stdout, and separately stderr | 1 MiB each | `larky.wasm.maxOutputBytes` for Larky; `WasmRuntime.Limits` for Java callers | the call fails |
| Run time | a deadline set by the caller | `LARKY_EVAL_EXPIRATION_MS` for Larky; `WasmRuntime.Limits` for Java callers | the call fails |
| Module size and functions | 8 MiB and 20,000 functions | `larky.wasm.maxModuleBytes`, `larky.wasm.maxFunctions` | the module is not valid |
| Preflight locals | 50,000 per function; 100,000 total | `larky.wasm.maxLocalsPerFunction`, `larky.wasm.maxTotalLocals` | the module is not valid |
| Preflight type entries and function arity | 100,000 entries; 1,000 parameters or results | `larky.wasm.maxTypeEntries`, `larky.wasm.maxFunctionArity` | the module is not valid |
| Concurrent guest runs | 4 | `larky.wasm.maxConcurrentGuests` | a run waiting for admission fails at its deadline |
| Guest thread stack size hint | 8 MiB | `larky.wasm.guestStackBytes` | JVM-dependent; this is not a hard memory cap |

Set the compile and guest-thread properties from trusted host configuration before constructing a
runtime or starting any guest. Invalid values fail startup of the affected runtime; existing
runtime instances retain their compile limits, and the shared guest-thread pool takes its settings
on first use. Larky's compiled-program cache has a separate `larky.wasm.programCache.maxBytes`
setting (16 MiB by default). These settings bound individual dimensions; their sum does not
bound the process heap or CPU.

A trap (for example `unreachable` or an out-of-bounds access) also fails the call.

## Calling a module from Larky

The embedding host must authorize WASM and set the evaluation deadline first. For example:

```java
System.setProperty("larky.wasm.enabled", "true"); // host configuration, never customer input
var engine = new com.verygood.security.larky.jsr223.LarkyScriptEngine();
engine.put("larky.modules", java.util.Map.of("encrypt.wasm", wasmBytes));
var bindings = new javax.script.SimpleBindings();
bindings.put("LARKY_EVAL_EXPIRATION_MS", System.currentTimeMillis() + 1_000);
engine.eval(scriptSource, bindings);
```

With that host configuration, the script can call the attached module:

```python
load("@vgs//wasm", "wasm")

encrypt = wasm.module("encrypt.wasm")

def encrypt_pan(pan, key):
    result = encrypt.call({"pan": pan, "key": key})
    if "error" in result:
        fail(result["error"])
    return result["encrypted"]
```

- `.call(value)` writes `value` to stdin as JSON and returns stdout parsed as JSON.
- `.run(data)` writes the bytes `data` to stdin and returns stdout as bytes.
- `wasm.loads(data)` makes a module from its binary (bytes), and `wasm.dumps(module)` returns a
  module's binary; the names follow Python's `pickle`/`json` (`load` itself is a Larky keyword).

## Runtimes (for services that embed Larky)

`larky-wasm` runs modules on [Endive](https://github.com/bytecodealliance/endive), a pure-JVM
runtime, by default. [GraalWasm](https://www.graalvm.org/webassembly/) is optional: add
`org.graalvm.polyglot:polyglot` and `org.graalvm.polyglot:wasm-community` (25.4.4.1.1) to the
service and set `-Dlarky.wasm.runtime=graal`. Both run Larky's WASI functions, and the
conformance tests in `larky-wasm/src/test` require them to give the same results byte for byte.
`WasmRuntime` is the whole API.

Endive interprets modules by default. `-Dlarky.wasm.endive.mode=compiler` compiles each module to
JVM bytecode instead, which runs faster but costs time and Metaspace in proportion to the module
before any deadline applies, and cannot be interrupted; use it only for modules you trust. A
GraalVM native image cannot use it.

Larky compiles a module the first time a script uses it and keeps the result, by content, in a
cache of at most `-Dlarky.wasm.programCache.maxBytes` bytes of modules (16 MiB by default; least
recently used first out). Compiling is not interrupted by the script's deadline, and a compiled
module takes several times its size in memory: a 1.4 MB Javy module took 0.2 s to compile and
36 MiB of heap under Endive's interpreter, 0.04 s and 4.9 MiB under GraalWasm.
`-Dlarky.wasm.maxMemoryBytes` and `-Dlarky.wasm.maxOutputBytes` set each run's limits; a negative
or malformed value means the default.

How deeply a module can nest calls depends on the evaluating thread's stack and the runtime: on
a 1 MiB stack, about 860 nested calls when Endive interprets it and about 500 on GraalWasm,
which runs interpreter-only on JDK 21 (its optimizing runtime needs JDK 25). Deeper nesting
traps.

## Choosing the host functions

The embedding service selects an immutable `WasmRuntime.WasiHostPolicy` for each run:

```java
var policy = new WasmRuntime.WasiHostPolicy(Set.of(
    "args_get", "args_sizes_get", "environ_get", "environ_sizes_get",
    "fd_read", "fd_write", "fd_fdstat_get", "proc_exit"));
var limits = new WasmRuntime.Limits(64L << 20, deadlineEpochMs, 1 << 20, null, policy);
var result = program.run(stdin, limits);
```

`Set` above is `java.util.Set`. The existing four-argument `Limits` constructor and
`Limits.defaults()` retain the current sandbox behavior. `WasiHostPolicy.none()` denies
all host functions. Policies can differ between runs of the same compiled program.
Unknown function names and functions without an implementation are rejected in the policy.
Known unsupported functions, and implemented functions the policy disables, still link with
their standard WASI signatures and return `NOSYS` without side effects. A disabled `proc_exit`
traps because its signature has no return value. Unknown imports and incorrect signatures
remain invalid modules.

Internally, `WasiHostModule` owns fresh state for each run. Its `@WasiHostFunction` methods
use `GuestMemory` followed by Java `int` for WASM i32 and `long` for i64, returning an `int`
errno (`void` for `proc_exit`). These annotations declare the full WASI preview 1 catalog, including
`implemented = false` stubs that return `NOSYS` and cannot be granted by a policy. The registry
derives names, signatures and bindings from the declarations, validates signature syntax and
Java types, rejects duplicate exports, and caches unbound method handles. A test-only ABI
fixture checks names and signatures independently of the production registry.
Endive binds imports to the permitted handlers during instantiation; guest calls use those
handles directly. Runtime adapters must pass `limits.wasiHostPolicy()` to `WasiHost` and bind
all WASI imports through it, including unsupported functions, to preserve the same sandbox.
The internal `WasiHost` constructor requires a policy; adapters using the old constructor
must be updated when restacking, so they cannot silently discard a restrictive policy.

Adding a function requires an annotated implementation and an explicit policy grant. The default
policy lists its functions independently so a new implementation does not expand it automatically.
