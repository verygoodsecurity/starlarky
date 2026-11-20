/*
 * Copyright 2026 Very Good Security Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.verygood.security.larky.modules;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.parser.LarkyEvaluator;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.script.SimpleBindings;
import javax.script.ScriptException;
import org.junit.Test;

public class KitesurfWasmJavaScriptTest {
  @Test
  public void aHostedFieldRunsJavaScriptAndEncryptsAValueInsideTheWasmBrowser() throws Exception {
    var guest = KitesurfTestGuest.load();
    var properties = Map.of("larky.wasm.enabled", "true",
        "larky.wasm.maxModuleBytes", "33554432", "larky.wasm.maxFunctions", "50000",
        "larky.wasm.maxTotalLocals", "150000");
    var previous = new java.util.HashMap<String, String>();
    properties.forEach((key, value) -> previous.put(key, System.setProperty(key, value)));

    var merchant = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var hosted = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    String merchantOrigin = "http://127.0.0.1:" + merchant.getAddress().getPort();
    String frameOrigin = "http://127.0.0.1:" + hosted.getAddress().getPort();
    AtomicReference<String> receipt = new AtomicReference<>();
    var resolutionAttempts = new AtomicInteger();

    merchant.createContext("/checkout", exchange -> respond(exchange, """
        <!doctype html><title>Waiting for hosted fields</title>
        <script src="/checkout.js"></script>
        <input id="merchant-field" aria-label="Merchant field" autocomplete="cc-number">
        <iframe id="field" src="%s/field" sandbox="allow-scripts allow-same-origin"></iframe>
        <button id="pay" disabled>Pay</button>
        """.formatted(frameOrigin)));
    merchant.createContext("/checkout.js", exchange -> respond(exchange, """
        let encrypted = null;
        window.addEventListener("message", event => {
          const frame = document.getElementById("field");
          if (event.origin !== "%s" || event.source !== frame.contentWindow) return;
          if (event.data.kind === "wrong-target") {
            document.body.append("Wrong target received");
            return;
          }
          if (event.data.kind !== "encrypted") return;
          encrypted = event.data.value;
          setTimeout(() => {
            document.getElementById("pay").removeAttribute("disabled");
            document.title = "Ready for payment";
          }, 100);
        });
        document.getElementById("pay").addEventListener("click", async () => {
          const reply = await fetch("/receipt", { method: "POST", body: encrypted });
          document.title = await reply.text();
        });
        """.formatted(frameOrigin)));
    merchant.createContext("/opaque", exchange -> respond(exchange, """
        <!doctype html><iframe src="%s/outer" sandbox="allow-scripts"></iframe>
        """.formatted(frameOrigin)));
    hosted.createContext("/outer", exchange -> respond(exchange, """
        <!doctype html><iframe src="/field" sandbox="allow-scripts allow-same-origin"></iframe>
        """));
    hosted.createContext("/field", exchange -> respond(exchange, """
        <!doctype html><title>Hosted input</title>
        <script>
          if (parent.document !== undefined) throw new Error("frame boundary broken");
          const field = document.createElement("input");
          field.id = "hosted-card";
          field.setAttribute("aria-label", "Synthetic card");
          field.setAttribute("autocomplete", "cc-number");
          document.body.appendChild(field);
          field.addEventListener("change", async () => {
            // Fixed test-vector key/IV. The live component generates its own key and IV.
            const key = await crypto.subtle.importKey("raw", new Uint8Array(16), "AES-GCM", false, ["encrypt"]);
            const clear = new TextEncoder().encode(field.value);
            const ciphertext = await crypto.subtle.encrypt({ name: "AES-GCM", iv: new Uint8Array(12) }, key, clear);
            const value = Array.from(new Uint8Array(ciphertext), b => b.toString(16).padStart(2, "0")).join("");
            parent.postMessage({ kind: "wrong-target" }, "https://wrong.example");
            parent.postMessage({ kind: "encrypted", value }, "%s");
          });
        </script>
        """.formatted(merchantOrigin)));
    merchant.createContext("/receipt", exchange -> {
      if (!"POST".equals(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(405, -1);
        exchange.close();
        return;
      }
      try (var input = exchange.getRequestBody()) {
        receipt.set(new String(input.readNBytes(256), UTF_8));
      }
      respond(exchange, "Encrypted receipt");
    });
    merchant.start();
    hosted.start();
    try (var client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY)
        .followRedirects(HttpClient.Redirect.NEVER).build()) {
      var host = KitesurfModule.prepareHost(guest.bytes(), guest.sha256(),
          Set.of(URI.create(merchantOrigin), URI.create(frameOrigin)), 4,
          (uri, deadline, cap) -> request(client, uri, null, deadline, cap),
          Set.of(URI.create(merchantOrigin + "/receipt")),
          (uri, body, deadline, cap) -> request(client, uri, body, deadline, cap),
          (reference, target) -> {
            resolutionAttempts.incrementAndGet();
            return "test.card".equals(reference)
                && URI.create(merchantOrigin + "/checkout").equals(target.checkoutUrl())
                && URI.create(frameOrigin + "/field").equals(target.documentUrl())
                && "hosted-card".equals(target.fieldId()) ? Optional.of("424242") : Optional.empty();
          });
      var engine = new LarkyScriptEngine();
      var bindings = new SimpleBindings();
      bindings.put("checkout", merchantOrigin + "/checkout");
      bindings.put(LarkyEvaluator.EXPIRATION_MS, System.currentTimeMillis() + 45_000);
      ParsedStarFile result;
      try (var ignored = host.openInvocation()) {
        var denied = assertThrows(ScriptException.class, () -> engine.eval("""
            load("@vgs//browsers", "kitesurf")
            kitesurf.open(checkout)
            fields = kitesurf.inspect()["fields"]
            kitesurf.fill(fields[0]["handle"], "test.card")
            """, bindings));
        assertThat(denied).hasMessageThat().contains("Kitesurf value reference is unavailable");
      }
      bindings.put(LarkyEvaluator.EXPIRATION_MS, System.currentTimeMillis() + 45_000);
      try (var ignored = host.openInvocation()) {
        result = (ParsedStarFile) engine.eval("""
            load("@vgs//browsers", "kitesurf")
            kitesurf.open(checkout)
            timed_out = not kitesurf.wait_for_text("title", "Ready for payment", timeout_ms = 0)
            fields = kitesurf.inspect()["fields"]
            kitesurf.fill(fields[1]["handle"], "test.card")
            ready = kitesurf.wait_for_text("title", "Ready for payment")
            wrong_target_received = kitesurf.wait_for_text("body", "Wrong target received", timeout_ms = 0)
            payment = kitesurf.inspect()
            kitesurf.click(payment["actions"][0]["handle"])
            title = kitesurf.inspect()["title"]
            """, bindings);
      }
      assertThat(result.getGlobalEnvironmentVariable("timed_out", Boolean.class)).isTrue();
      assertThat(result.getGlobalEnvironmentVariable("ready", Boolean.class)).isTrue();
      assertThat(result.getGlobalEnvironmentVariable("wrong_target_received", Boolean.class)).isFalse();
      assertThat(result.getGlobalEnvironmentVariable("title", String.class))
          .isEqualTo("Encrypted receipt");
      String encrypted = receipt.get();
      assertThat(encrypted).isNotNull();
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(new byte[16], "AES"),
          new GCMParameterSpec(128, new byte[12]));
      assertThat(new String(cipher.doFinal(HexFormat.of().parseHex(encrypted)), UTF_8))
          .isEqualTo("424242");
      assertThat(resolutionAttempts.get()).isEqualTo(2);

      bindings.put("checkout", merchantOrigin + "/opaque");
      bindings.put(LarkyEvaluator.EXPIRATION_MS, System.currentTimeMillis() + 45_000);
      try (var ignored = host.openInvocation()) {
        var denied = assertThrows(ScriptException.class, () -> engine.eval("""
            load("@vgs//browsers", "kitesurf")
            kitesurf.open(checkout)
            fields = kitesurf.inspect()["fields"]
            kitesurf.fill(fields[0]["handle"], "test.card")
            """, bindings));
        assertThat(denied).hasMessageThat().contains("Kitesurf could not complete the browser command");
      }
      assertThat(resolutionAttempts.get()).isEqualTo(2);
    } finally {
      hosted.stop(0);
      merchant.stop(0);
      previous.forEach((key, value) -> {
        if (value == null) {
          System.clearProperty(key);
        } else {
          System.setProperty(key, value);
        }
      });
    }
  }

  private static byte[] request(HttpClient client, URI uri, byte[] body, long deadline, int cap)
      throws IOException, InterruptedException {
    var builder = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofMillis(Math.max(1, deadline - System.currentTimeMillis())));
    if (body != null) {
      builder.POST(HttpRequest.BodyPublishers.ofByteArray(body));
    }
    var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
    try (var data = response.body()) {
      if (response.statusCode() != 200) {
        throw new IOException("fixture request failed");
      }
      return data.readNBytes(cap + 1);
    }
  }

  private static void respond(HttpExchange exchange, String text) throws IOException {
    byte[] body = text.getBytes(UTF_8);
    exchange.sendResponseHeaders(200, body.length);
    try (var output = exchange.getResponseBody()) {
      output.write(body);
    }
  }
}
