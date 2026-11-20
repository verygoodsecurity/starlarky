# Embedded Kitesurf Wasm experiment

[The checkout script](../../larky/src/test/resources/kitesurf/wasm_checkout.star)
loads `@vgs//browsers`, opens a loopback checkout, discovers its email field, and
fills it using a value reference. The Java host supplies a synthetic value for
that reference. The guest retains the Kitesurf DOM between calls, and the script
receives only field metadata and whether the field is filled. The test checks
that inspection uses the same document and that the checkout was fetched once.

```python
load("@vgs//browsers", "kitesurf")

kitesurf.open(checkout)
form = kitesurf.inspect()
kitesurf.fill(form["fields"][0]["handle"], "checkout.email")
filled = kitesurf.inspect()
```

The guest runs page JavaScript in a separate context per frame. Filling a field
dispatches browser input events, and clicking dispatches a browser click. The
[cross-origin test](../../larky/src/test/java/com/verygood/security/larky/modules/KitesurfWasmJavaScriptTest.java)
verifies that a child frame encrypts a synthetic value with WebCrypto, sends
the result to its parent using `postMessage`, and the parent submits the
ciphertext to a local receipt.

## Build and run

Use JDK 21 and Maven for Starlarky. The guest build uses Git, curl, Node.js,
patch, and Rust 1.99.0 with the `wasm32-wasip1` target. The script fetches the pinned
[Kitesurf source](https://github.com/cloudflare/kitesurf/tree/a58b15dfabc0a238a4ab37c9ba9c1813c865bf21)
and builds the guest against Kitesurf's patched Rust dependencies.

```sh
rustup toolchain install 1.99.0 --profile minimal --target wasm32-wasip1
RUSTUP_TOOLCHAIN=1.99.0 examples/kitesurf-wasm/build.sh
mvn -B -ntp -pl larky -am \
  -Dtest=KitesurfWasmModuleTest,KitesurfWasmJavaScriptTest,BrowserWasmRuntimeTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dlarky.kitesurf.wasmGuest="$PWD/target/kitesurf-guest.wasm" test
```

The tests start their own loopback HTTP servers. They use Endive for the
embedded guest and set a finite Larky evaluation deadline and guest-specific
Wasm limits.

## Adyen sandbox checkout

[The checkout script](../../larky/src/test/resources/kitesurf/embedded_adyen_checkout.star)
discovers the test card fields in Adyen's hosted frames using their labels,
autocomplete hints, and origin. It fills them from host-owned references,
waits for the merchant page to report that the card fields are encrypted,
and clicks Pay. The local receipt checks for all four compact JWE fields.

Set `ADYEN_CLIENT_KEY` to a sandbox client key that permits the merchant origin
`http://localhost:3000`. Start the merchant fixture on an unused loopback port.
The Java test maps the merchant origin to that port and grants the guest GET
access to the Adyen test origins through the fixture relay and POST access
to the local receipt.

```sh
cd examples/kitesurf-adyen
npm ci
npm run build
ADYEN_FIXTURE_PORT=18801 node fixture.mjs
```

Run the opt-in test from the repository root:

```sh
mvn -B -ntp -pl larky -am -Dtest=KitesurfWasmAdyenTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dlarky.kitesurf.wasmGuest="$PWD/target/kitesurf-guest.wasm" \
  -Dlarky.kitesurf.adyenFixturePort=18801 test
```

## Embedding host

[The Java integration test](../../larky/src/test/java/com/verygood/security/larky/modules/KitesurfWasmModuleTest.java)
shows the setup. The host prepares a trusted guest binary with
`KitesurfModule.prepareHost`, supplies an exact-origin HTTP grant and a value
resolver, and wraps the normal Larky evaluation in
`try (var invocation = host.openInvocation())`. The invocation owns the guest
memory, the shared HTTP budget, and the deadline. A host can additionally
authorize POST to exact URLs. The HTTP transport must honor the supplied byte
cap and deadline and return redirects to its caller.

The guest uses `vgs_browser_v1` for HTTP, WebCrypto primitives, and the clock,
alongside WASI for stream I/O and random bytes. Customer scripts use
`load("@vgs//browsers", "kitesurf")` to access the scoped guest.
