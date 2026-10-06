# larky-wasm

The WebAssembly runtime behind Larky's `@vgs//wasm`.

## Runtimes (for services that embed Larky)

`larky-wasm` runs modules on [Endive](https://github.com/bytecodealliance/endive), a pure-JVM
runtime; `larky-wasm/src/test` holds its conformance tests. `WasmRuntime` is the whole API.
