/*
 * Copyright 2026 Very Good Security Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.verygood.security.larky.modules;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assume.assumeNotNull;

import com.google.common.hash.Hashing;
import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.parser.LarkyEvaluator;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
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
  private static final List<URI> CHECKOUTS = List.of(MERCHANT.resolve("/"), MERCHANT.resolve("/drop-in"));
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
    boolean requireAuthorised = Boolean.getBoolean("larky.kitesurf.adyenRequireAuthorised");
    String demoToken = requireAuthorised ? System.getenv("ADYEN_DEMO_TOKEN") : null;
    if (requireAuthorised && (demoToken == null || !demoToken.matches("[a-f0-9]{64}"))) {
      throw new IllegalStateException("Sandbox authorization requires ADYEN_DEMO_TOKEN");
    }
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
         var source = getClass().getResourceAsStream("/kitesurf/embedded_adyen_checkout.star")) {
      byte[] scriptBytes = source.readAllBytes();
      String script = new String(scriptBytes, UTF_8);
      String scriptDigest = Hashing.sha256().hashBytes(scriptBytes).toString();
      var host = KitesurfModule.prepareHost(guest.bytes(), guest.sha256(), ORIGINS, 48,
          (uri, deadline, cap) -> request(client, fixture, uri, null, null, deadline, cap, transportErrors),
          Set.of(RECEIPT), (uri, body, deadline, cap) -> {
            byte[] result = request(client, fixture, uri, body, demoToken, deadline, cap, transportErrors);
            receipt.set(new String(result, UTF_8));
            return result;
          }, (reference, target) -> {
            String autocomplete = Map.of("card.number", "cc-number", "card.expiry", "cc-exp",
                "card.security_code", "cc-csc").get(reference);
            if (!CHECKOUTS.contains(target.checkoutUrl())
                || !"https://checkoutshopper-test.adyen.com".equals(target.origin())
                || !target.documentUrl().getPath().contains("/securedfields/")
                || autocomplete == null || !autocomplete.equals(target.autocomplete())) {
              return Optional.empty();
            }
            return Optional.of(testCard.get(reference));
          });

      var engine = new LarkyScriptEngine();
      var compiled = engine.compile(script);
      String expected = requireAuthorised
          ? "{\"resultCode\":\"Authorised\",\"sandboxAuthorised\":true,"
          : "{\"resultCode\":\"EncryptedFieldsReceived\",";
      expected += "\"encryptedFields\":[\"encryptedCardNumber\",\"encryptedExpiryMonth\","
          + "\"encryptedExpiryYear\",\"encryptedSecurityCode\"]}";
      for (URI checkout : CHECKOUTS) {
        receipt.set(null);
        transportErrors.clear();
        var bindings = new SimpleBindings();
        bindings.put("start_url", checkout.toString());
        bindings.put(LarkyEvaluator.EXPIRATION_MS, System.currentTimeMillis() + 180_000);
        ParsedStarFile result;
        try (var ignored = host.openInvocation()) {
          result = (ParsedStarFile) compiled.eval(bindings);
        } catch (ScriptException e) {
          throw new AssertionError("Checkout " + checkout.getPath()
              + " failed. Sandbox transport errors " + transportErrors, e);
        }
        assertWithMessage("Checkout %s expected %s", checkout.getPath(), expected)
            .that(expected.equals(receipt.get())).isTrue();
        var fields = result.getGlobalEnvironmentVariable("discovered_fields", List.class);
        assertThat(fields).hasSize(3);
        System.out.printf("Checkout %s | script SHA-256 %s | %s%n",
            checkout.getPath(), scriptDigest, expected);
        System.out.printf("Discovered reference, field label, and frame origin %s%n", fields);
      }
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

  private static byte[] request(HttpClient client, URI fixture, URI uri, byte[] body, String demoToken,
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
      if (RECEIPT.equals(uri) && demoToken != null) {
        request.header("X-Demo-Token", demoToken);
      }
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
