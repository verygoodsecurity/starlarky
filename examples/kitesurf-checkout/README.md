# Checkout form discovery with Kitesurf

The demo starts on a clothing store checkout and follows the "Pay with Addi"
link. The same Larky script runs against two checkout layouts with different
field IDs, order, and label markup. The browser finds the controls in the live
DOM and returns field labels, input types, autocomplete hints, and opaque
handles. The script chooses which values to enter from that metadata. Kitesurf
clicks the link and types through CDP input events. It checks that all three
fields contain valid values and stops at the filled form.

The example uses a loopback fixture modeled on Addi's checkout redirect. The
test binding operates on the top-level document and supports text fields and
actions. Its transport accepts only GET to the fixture. The example uses
synthetic values and needs neither Addi credentials nor a merchant account.

The [public Addi sandbox](https://dummy-sandbox.addi.com/) was also probed through
Kitesurf. The browser found and clicked the checkout button. The sandbox returned
an authentication error before the hosted shopper form. The local fixture makes
the discovery and fill test repeatable with either layout. See
[Addi's checkout redirect documentation](https://support.addi.com/custom-integrations).

## Run locally

Build [Kitesurf at revision a58b15d](https://github.com/cloudflare/kitesurf/commit/a58b15dfabc0a238a4ab37c9ba9c1813c865bf21)
with `pnpm install --frozen-lockfile` and `pnpm run build`. Copy any generated
files that are symlinks into the checkout before mounting it in Docker.

From the Starlarky root, start the fixture and local workerd engine.

```sh
KITESURF_SOURCE=/absolute/path/to/kitesurf examples/kitesurf-checkout/run.sh
```

In another terminal, run the demo with JDK 21.

```sh
mvn -B -ntp -pl larky -am -Dtest=KitesurfCheckoutDemoTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Dlarky.kitesurf.checkoutDemoPort=18788 test
```

If you set `KITESURF_DEMO_PORT`, use the same port in the Maven command.
