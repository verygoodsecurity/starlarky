# Running WebAssembly from Larky (`@vgs//wasm`)

`@vgs//wasm` lets a Larky script run a WebAssembly module that you provide. Use it when you need
code that cannot be written in Larky, such as a payment processor's JavaScript library: compile it
to WebAssembly and call it from your script.

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
| clocks (`clock_time_get`) | always 0 |
| `random_get` | secure random bytes |
| every other WASI function (`poll_oneoff`, `clock_res_get`, files, sockets, ...) | returns `NOSYS` (52) without doing anything |

The WASI functions that do something are `args_get`, `args_sizes_get`, `environ_get`,
`environ_sizes_get`, `clock_time_get`, `random_get`, `fd_read` (stdin), `fd_write` (stdout and
stderr), `fd_close`, `fd_fdstat_get`, `fd_seek` (ESPIPE), `fd_prestat_get` and
`fd_prestat_dir_name` (EBADF: no preopens), `sched_yield` and `proc_exit`. A pointer or length
outside the module's memory traps.

Returning from `_start` exits with 0; `proc_exit(n)` exits with `n`.

## Limits

| Limit | Default | When exceeded |
| --- | --- | --- |
| Linear memory | 64 MiB (1024 pages) | a module whose initial memory is larger fails to start; `memory.grow` past the limit returns -1 |
| stdout, and separately stderr | 1 MiB each | the call fails |
| Run time | a deadline set by the caller | the call fails |
| Module size | 8 MiB and 20,000 functions | the module is not valid |

A trap (for example `unreachable` or an out-of-bounds access) also fails the call.

## Calling a module from Larky

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
runtime; `larky-wasm/src/test` holds its conformance tests. `WasmRuntime` is the whole API.

Endive interprets modules by default. `-Dlarky.wasm.endive.mode=compiler` compiles each module to
JVM bytecode instead, which runs faster but costs time and Metaspace in proportion to the module
before any deadline applies, and cannot be interrupted; use it only for modules you trust. A
GraalVM native image cannot use it.

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
