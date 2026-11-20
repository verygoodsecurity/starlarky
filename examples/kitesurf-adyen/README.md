# Adyen Secured Fields in Kitesurf

Larky inspects an Adyen test checkout, provides the host with a field-to-value
reference map, and submits the form in one Kitesurf browser context. The host
resolves the references to synthetic card data. Adyen Web runs its own encryption
in the hosted test frames. The loopback fixture checks for four compact JWE
values and returns just the field names.

The test uses Adyen Web 6.48.0 and the Boa evaluation path in local `workerd`.
The runner adds an egress hook restricted to Adyen test hosts, raises the local
engine's worker budget for the hosted frames, and adapts WebCrypto arguments and
binary results across the Boa/workerd boundary. It applies those changes to a
temporary copy of Kitesurf. The Larky binding and relay are demo code. This
checkout uses synthetic values and a loopback receipt. It does not authorize a
payment or exercise the gateway's token reveal path.

## Run locally

1. Build [Kitesurf at revision a58b15d](https://github.com/cloudflare/kitesurf/commit/a58b15dfabc0a238a4ab37c9ba9c1813c865bf21)
   with `pnpm install --frozen-lockfile` and `pnpm run build`. Copy any generated
   files that are symlinks into the checkout before mounting it in Docker.
2. Export the absolute Kitesurf checkout path as `KITESURF_SOURCE`. Export an
   Adyen [test client key](https://docs.adyen.com/development-resources/client-side-authentication/)
   as `ADYEN_CLIENT_KEY`, with `http://localhost:3000` as an allowed origin. The
   fixture reaches only Adyen's test environment and listens inside Docker.
3. From the Starlarky repository root, start the demo engine.

   ```sh
   examples/kitesurf-adyen/run.sh
   ```

4. In a second terminal, run the Larky checkout test with JDK 21.

   ```sh
   mvn -B -ntp -pl larky -am -Dtest=AdyenSandboxCheckoutDemoTest \
     -Dsurefire.failIfNoSpecifiedTests=false -Dlarky.kitesurf.adyenDemoPort=18788 test
   ```

Set `KITESURF_DEMO_PORT` and the Maven property to the same port if necessary.
For a screenshot before filling, add
`-Dlarky.kitesurf.adyenDemoScreenshot=/tmp/adyen-test-checkout.png` to the test command.
The PNG shows the checkout layout. At this revision Kitesurf paints fallback text
for Adyen's cross-origin frames even when CDP can read and fill their live DOM.
Stop the demo engine with Ctrl-C.

The expected receipt has `resultCode` set to `EncryptedFieldsReceived` and contains
`encryptedCardNumber`, `encryptedExpiryMonth`, `encryptedExpiryYear`, and
`encryptedSecurityCode`.
