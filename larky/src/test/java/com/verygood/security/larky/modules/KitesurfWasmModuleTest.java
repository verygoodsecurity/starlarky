/*
 * Copyright 2026 Very Good Security Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.verygood.security.larky.modules;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeNotNull;

import com.sun.net.httpserver.HttpServer;
import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.parser.LarkyEvaluator;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.StarlarkInt;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KitesurfWasmModuleTest {
  private String previousWasmEnabled;

  @Before
  public void enableWasm() {
    previousWasmEnabled = System.setProperty("larky.wasm.enabled", "true");
  }

  @After
  public void restoreWasmSetting() {
    if (previousWasmEnabled == null) {
      System.clearProperty("larky.wasm.enabled");
    } else {
      System.setProperty("larky.wasm.enabled", previousWasmEnabled);
    }
  }

  @Test
  public void aScriptCanLoadKitesurfButNeedsAnInvocationGrant() {
    ScriptException denied = assertThrows(ScriptException.class,
        () -> evaluate("https://checkout.example/"));
    assertThat(denied).hasMessageThat().contains("no Kitesurf host grant for this invocation");
  }

  @Test
  public void customerScriptLoadsAndFillsAFormInTheEmbeddedWasmGuest() throws Exception {
    String guestPath = System.getProperty("larky.kitesurf.wasmGuest");
    assumeNotNull(guestPath);

    byte[] html = """
        <!DOCTYPE html>
        <title>Checkout &amp; payment</title>
        <script type="application/json">{}</script>
        <iframe title="Blank frame"></iframe>
        <label for="email">Email address</label>
        <input id="email" type="email" autocomplete="email">
        <input type="hidden" value="ignored">
        """.getBytes(StandardCharsets.UTF_8);
    var requests = new AtomicInteger();
    var fixture = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    fixture.createContext("/checkout", exchange -> {
      requests.incrementAndGet();
      exchange.sendResponseHeaders(200, html.length);
      try (var out = exchange.getResponseBody()) {
        out.write(html);
      }
    });
    fixture.start();
    String previousModuleLimit = System.setProperty("larky.wasm.maxModuleBytes", "33554432");
    String previousFunctionLimit = System.setProperty("larky.wasm.maxFunctions", "50000");
    String previousLocalLimit = System.setProperty("larky.wasm.maxTotalLocals", "150000");
    String origin = "http://127.0.0.1:" + fixture.getAddress().getPort();
    try (var client = HttpClient.newBuilder()
        .proxy(HttpClient.Builder.NO_PROXY)
        .followRedirects(HttpClient.Redirect.NEVER).build()) {
      var host = KitesurfModule.prepareHost(Files.readAllBytes(Path.of(guestPath)),
          Set.of(URI.create(origin)), 1, (uri, deadline, maxBytes) -> {
            var request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(Math.max(1, deadline - System.currentTimeMillis())))
                .GET()
                .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var body = response.body()) {
              if (response.statusCode() != 200) {
                throw new IOException("fixture returned a non-200 response");
              }
              return body.readNBytes(maxBytes + 1);
            }
          }, reference -> "checkout.email".equals(reference)
              ? Optional.of("synthetic@example.test") : Optional.empty());

      try (var ignored = host.openInvocation()) {
        ParsedStarFile result = evaluate(origin + "/checkout");
        Dict<?, ?> summary = result.getGlobalEnvironmentVariable("summary", Dict.class);
        assertThat(result.getGlobalEnvironmentVariable("same_document", Boolean.class)).isTrue();
        assertThat(result.getGlobalEnvironmentVariable("initially_filled", Boolean.class)).isFalse();
        assertThat(result.getGlobalEnvironmentVariable("filled", Boolean.class)).isTrue();
        assertThat(summary.toString()).doesNotContain("synthetic@example.test");
        var fields = (List<?>) summary.get("fields");
        assertThat(fields).hasSize(1);
        assertThat(summary.get("title")).isEqualTo("Checkout & payment");
        assertThat(summary.get("script_count")).isEqualTo(StarlarkInt.of(1));
        assertThat(((Dict<?, ?>) fields.getFirst()).get("label")).isEqualTo("Email address");
        assertThat(summary.get("iframe_count")).isEqualTo(StarlarkInt.of(1));
        assertThat(requests.get()).isEqualTo(1);
      }
      assertThrows(ScriptException.class, () -> evaluate(origin + "/checkout"));
      assertThat(requests.get()).isEqualTo(1);
    } finally {
      if (previousModuleLimit == null) {
        System.clearProperty("larky.wasm.maxModuleBytes");
      } else {
        System.setProperty("larky.wasm.maxModuleBytes", previousModuleLimit);
      }
      if (previousFunctionLimit == null) {
        System.clearProperty("larky.wasm.maxFunctions");
      } else {
        System.setProperty("larky.wasm.maxFunctions", previousFunctionLimit);
      }
      if (previousLocalLimit == null) {
        System.clearProperty("larky.wasm.maxTotalLocals");
      } else {
        System.setProperty("larky.wasm.maxTotalLocals", previousLocalLimit);
      }
      fixture.stop(0);
    }
  }

  private static ParsedStarFile evaluate(String checkout) throws IOException, ScriptException {
    var engine = new LarkyScriptEngine();
    var bindings = new SimpleBindings();
    bindings.put("checkout", checkout);
    bindings.put(LarkyEvaluator.EXPIRATION_MS, System.currentTimeMillis() + 10_000);
    try (var source = new InputStreamReader(
        KitesurfWasmModuleTest.class.getResourceAsStream("/kitesurf/wasm_checkout.star"),
        StandardCharsets.UTF_8)) {
      return (ParsedStarFile) engine.eval(source, bindings);
    }
  }
}
