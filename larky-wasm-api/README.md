# Running WebAssembly from Larky (`@vgs//wasm`)

`@vgs//wasm` lets a Larky script run a WebAssembly module that you provide. Use it when you need
code that cannot be written in Larky, such as a payment processor's JavaScript library: compile it
to WebAssembly and call it from your script.

## What a module is

A module is a [WASI preview 1](https://github.com/WebAssembly/WASI/blob/main/legacy/preview1/docs.md)
command: it exports `_start` and its `memory`. Every call starts a fresh instance, so nothing
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

Returning from `_start` exits with 0; `proc_exit(n)` exits with `n`.

## Limits

| Limit | Default | When exceeded |
| --- | --- | --- |
| Linear memory | 64 MiB (1024 pages) | a module whose initial memory is larger fails to start; `memory.grow` past the limit returns -1 |
| stdout, and separately stderr | 1 MiB each | the call fails |
| Run time | a deadline set by the caller | the call fails |

A trap (for example `unreachable`, an out-of-bounds access, or an uncaught JavaScript exception)
also fails the call.

## Compiling JavaScript with Javy

[Javy](https://github.com/bytecodealliance/javy) compiles JavaScript to a WASI module that embeds
the QuickJS engine. Write a small wrapper around the vendor's script that reads JSON from stdin,
calls the vendor function, and writes JSON to stdout:

```js
// wrapper.js: concatenate after the vendor's encrypt.js
function readStdin() {
  const chunks = [];
  let total = 0;
  for (;;) {
    const buffer = new Uint8Array(4096);
    const n = Javy.IO.readSync(0, buffer);
    if (n === 0) break;
    chunks.push(buffer.subarray(0, n));
    total += n;
  }
  const all = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    all.set(chunk, offset);
    offset += chunk.length;
  }
  return new TextDecoder().decode(all);
}

function write(fd, text) {
  Javy.IO.writeSync(fd, new TextEncoder().encode(text));
}

let result;
try {
  const request = JSON.parse(readStdin());
  result = { encrypted: encrypt(request.pan, request.key) }; // the vendor's function
} catch (e) {
  write(2, String(e) + "\n");
  result = { error: String(e) };
}
write(1, JSON.stringify(result));
```

```sh
cat vendor/encrypt.js wrapper.js > encrypt_bundle.js
javy build -J simd-json-builtins=n -o encrypt.wasm encrypt_bundle.js
```

Notes:

- Use a static build (the default `javy build`); the module must not need a Javy plugin at run time.
- Pass `-J simd-json-builtins=n`. Javy's output uses WebAssembly SIMD instructions, which the Endive runtime runs only on Java 25+ (with `--add-modules jdk.incubator.vector`), and its SIMD JSON parser does not run correctly there; GraalWasm runs either build.
  A static module is about 1.3 MiB (`sample_encrypt.wasm` is 1,362,014 bytes).
- Javy cannot choose an exit code, and an uncaught exception traps the module, which loses its
  output. Catch errors and report them in the JSON you write, as above.
- The vendor script must not need `fetch`, timers, files, or Node.js modules; none of them exist
  here. The clock always reads 0.
- If the vendor ships an ES module or uses `import`, bundle it into one script first (for example
  with esbuild).
- `larky-wasm-conformance/src/test/resources/wasm/js/sample_encrypt.js` is a complete example, and
  `larky-wasm-conformance/build-fixtures.sh` builds it.

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

## Testing locally

Run your script with the module attached:

```sh
larky-runner -s script.star -i input.star --module encrypt.wasm=./encrypt.wasm
```

`--module NAME=PATH` makes `PATH` available to `wasm.module("NAME")`.
