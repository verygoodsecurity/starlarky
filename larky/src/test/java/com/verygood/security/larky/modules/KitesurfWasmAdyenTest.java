/*
 * Copyright 2026 Very Good Security Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.verygood.security.larky.modules;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assume.assumeNotNull;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.parser.LarkyEvaluator;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.junit.Test;

/** Runs the official Adyen test checkout against the in-process Kitesurf Wasm guest. */
public class KitesurfWasmAdyenTest {
  private static final URI MERCHANT = URI.create("http://localhost:3000");
  private static final URI RECEIPT = MERCHANT.resolve("/api/payments");
  private static final Set<URI> ORIGINS = Set.of(MERCHANT,
      URI.create("https://checkoutshopper-test.adyen.com"),
      URI.create("https://checkoutshopper-test.cdn.adyen.com"),
      URI.create("https://checkoutanalytics-test.adyen.com"));

  @Test
  public void embeddedKitesurfLetsAdyenEncryptItsOwnHostedInputs() throws Exception {
    var guest = KitesurfTestGuest.load();
    String fixturePort = System.getProperty("larky.kitesurf.adyenFixturePort");
    assumeNotNull(fixturePort);
    URI fixture = URI.create("http://127.0.0.1:" + Integer.parseInt(fixturePort));
    var previous = new HashMap<String, String>();
    // The vendor bundles are substantially faster in Endive compiler mode.
    Map.of("larky.wasm.enabled", "true", "larky.wasm.endive.mode", "compiler",
        "larky.wasm.maxModuleBytes", "33554432",
        "larky.wasm.maxFunctions", "50000", "larky.wasm.maxTotalLocals", "150000",
        "larky.wasm.maxMemoryBytes", "134217728")
        .forEach((key, value) -> previous.put(key, System.setProperty(key, value)));

    var testCard = Map.of("card.number", "5555555555554444", "card.expiry", "0330",
        "card.security_code", "737");
    AtomicReference<String> receipt = new AtomicReference<>();
    var transportErrors = new ConcurrentLinkedQueue<String>();
    try (var client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY)
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NEVER).build();
         var source = new InputStreamReader(getClass().getResourceAsStream(
             "/kitesurf/embedded_adyen_checkout.star"), UTF_8)) {
      var host = KitesurfModule.prepareHost(guest.bytes(), guest.sha256(), ORIGINS, 48,
          (uri, deadline, cap) -> request(client, fixture, uri, null, deadline, cap, transportErrors),
          Set.of(RECEIPT), (uri, body, deadline, cap) -> {
            byte[] result = request(client, fixture, uri, body, deadline, cap, transportErrors);
            receipt.set(new String(result, UTF_8));
            return result;
          }, (reference, target) -> {
            String autocomplete = Map.of("card.number", "cc-number", "card.expiry", "cc-exp",
                "card.security_code", "cc-csc").get(reference);
            if (!MERCHANT.resolve("/").equals(target.checkoutUrl())
                || !"https://checkoutshopper-test.adyen.com".equals(target.origin())
                || !target.documentUrl().getPath().contains("/securedfields/")
                || autocomplete == null || !autocomplete.equals(target.autocomplete())) {
              return Optional.empty();
            }
            return Optional.of(testCard.get(reference));
          });

      var bindings = new SimpleBindings();
      bindings.put("start_url", MERCHANT + "/");
      bindings.put(LarkyEvaluator.EXPIRATION_MS, System.currentTimeMillis() + 180_000);
      try (var ignored = host.openInvocation()) {
        new LarkyScriptEngine().eval(source, bindings);
      } catch (ScriptException e) {
        if (transportErrors.isEmpty()) {
          throw e;
        }
        throw new AssertionError("Sandbox transport errors " + transportErrors, e);
      }
      assertThat(receipt.get()).isEqualTo("""
          {"resultCode":"EncryptedFieldsReceived","encryptedFields":["encryptedCardNumber","encryptedExpiryMonth","encryptedExpiryYear","encryptedSecurityCode"]}""");
    } finally {
      previous.forEach((key, value) -> {
        if (value == null) {
          System.clearProperty(key);
        } else {
          System.setProperty(key, value);
        }
      });
    }
  }

  private static byte[] request(HttpClient client, URI fixture, URI uri, byte[] body,
      long deadline, int cap, ConcurrentLinkedQueue<String> transportErrors)
      throws IOException, InterruptedException {
    boolean merchant = "localhost".equals(uri.getHost());
    // The guest sees the original origins. Surefire reaches the Adyen sandbox through the
    // fixture's loopback relay, which independently allows only the three test hosts.
    URI target = merchant
        ? URI.create(fixture + uri.getRawPath()
            + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()))
        : fixture.resolve("/__demo_egress");
    var request = HttpRequest.newBuilder(target)
        .timeout(Duration.ofMillis(Math.max(1, Math.min(15_000, deadline - System.currentTimeMillis()))));
    if (!merchant) {
      request.header("x-demo-upstream", uri.toASCIIString());
    }
    if (body != null) {
      request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofByteArray(body));
    }
    try {
      var response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
      try (var input = response.body()) {
        if (response.statusCode() != 200) {
          transportErrors.add(uri.getHost() + " HTTP " + response.statusCode());
          throw new IOException("sandbox checkout request failed");
        }
        return input.readNBytes(cap + 1);
      }
    } catch (IOException e) {
      transportErrors.add(uri.getHost() + " " + e.getClass().getSimpleName());
      throw e;
    }
  }
}
