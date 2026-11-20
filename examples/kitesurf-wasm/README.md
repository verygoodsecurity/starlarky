# Embedded Kitesurf Wasm experiment

[The checkout script](../../larky/src/test/resources/kitesurf/wasm_checkout.star)
loads `@vgs//kitesurf`, opens a loopback checkout, discovers its email field, and
fills it using a value reference. The Java host supplies a synthetic value for
that reference. The guest retains the Kitesurf DOM between calls, and the script
receives only field metadata and whether the field is filled. The test checks
that inspection uses the same document and that the checkout was fetched once.

```python
load("@vgs//kitesurf", "kitesurf")

kitesurf.open(checkout)
form = kitesurf.inspect()
kitesurf.fill(form["fields"][0]["handle"], "checkout.email")
filled = kitesurf.inspect()
```

The example covers static HTML. The remaining work for Adyen is the page
JavaScript runtime, hosted iframe execution, and WebCrypto alongside the DOM.
The guest can count iframe elements while filling the containing document.

## Build and run

Use JDK 21 and Maven for Starlarky. The guest build uses Git, curl, Node.js, and
Rust 1.99.0 with the `wasm32-wasip1` target. The script fetches the pinned
[Kitesurf source](https://github.com/cloudflare/kitesurf/tree/a58b15dfabc0a238a4ab37c9ba9c1813c865bf21)
and builds the guest against Kitesurf's patched Rust dependencies.

```sh
rustup toolchain install 1.99.0 --profile minimal --target wasm32-wasip1
RUSTUP_TOOLCHAIN=1.99.0 examples/kitesurf-wasm/build.sh
mvn -B -ntp -pl larky -am -Dtest=KitesurfWasmModuleTest,BrowserWasmRuntimeTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dlarky.kitesurf.wasmGuest="$PWD/target/kitesurf-guest.wasm" test
```

The test starts its own loopback HTTP server. It uses Endive for the embedded
guest and needs a finite Larky evaluation deadline. The test temporarily sets a 16 MiB module cap for the current Kitesurf build.

## Embedding host

[The Java integration test](../../larky/src/test/java/com/verygood/security/larky/modules/KitesurfWasmModuleTest.java)
shows the setup. The host prepares a trusted guest binary with
`KitesurfModule.prepareHost`, supplies an exact-origin HTTP grant and a value
resolver, and wraps the normal Larky evaluation in
`try (var invocation = host.openInvocation())`. The invocation owns the guest
memory, the shared GET budget, and the deadline. The HTTP transport must honor
the supplied byte cap and deadline and return redirects to its caller.

The guest uses `vgs_browser_v1.http_get` alongside WASI for stream I/O. The usual
`@vgs//wasm` command loader rejects the browser import. Customer scripts use
`load("@vgs//kitesurf", "kitesurf")` to access the scoped guest.
