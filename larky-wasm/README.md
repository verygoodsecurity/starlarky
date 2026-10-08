# larky-wasm

The WebAssembly runtime behind Larky's `@vgs//wasm`.

## Runtimes (for services that embed Larky)

`larky-wasm` runs modules on [Endive](https://github.com/bytecodealliance/endive), a pure-JVM
runtime; `larky-wasm/src/test` holds its conformance tests. `WasmRuntime` is the whole API.

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
errno (`void` for `proc_exit`). The registry validates names and signatures against the complete
WASI preview 1 ABI catalog, rejects duplicate exports, and caches unbound method handles.
Endive binds imports to the permitted handlers during instantiation; guest calls use those
handles directly. Runtime adapters must pass `limits.wasiHostPolicy()` to `WasiHost` and bind
all WASI imports through it, including unsupported functions, to preserve the same sandbox.
The internal `WasiHost` constructor requires a policy; adapters using the old constructor
must be updated when restacking, so they cannot silently discard a restrictive policy.

Adding a function requires an annotated implementation and an explicit policy grant. The default
policy lists its functions independently so a new implementation does not expand it automatically.
