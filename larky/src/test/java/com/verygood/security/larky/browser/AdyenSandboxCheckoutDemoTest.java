package com.verygood.security.larky.browser;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeNotNull;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.modules.JsonModule;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import org.junit.Test;

/** Browser and gateway capability proof against the loopback, Adyen-test-only checkout fixture. */
public class AdyenSandboxCheckoutDemoTest {
  @Test
  public void larkyFillsHostedFieldsAndReceivesAnEncryptedReceipt() throws Exception {
    String port = System.getProperty("larky.kitesurf.adyenDemoPort");
    assumeNotNull(port);
    URI endpoint = URI.create("ws://127.0.0.1:" + Integer.parseInt(port) + "/devtools/browser");
    try (var browser = new Checkout(endpoint)) {
      var engine = new LarkyScriptEngine();
      engine.put("checkout", browser);
      var result = (ParsedStarFile) engine.eval("""
          form = checkout.inspect()
          plan = {
              "encryptedCardNumber": "payment.card.number",
              "encryptedExpiryDate": "payment.card.expiry",
              "encryptedSecurityCode": "payment.card.cvc",
          }
          form_matches = sorted(form.keys()) == sorted(plan.keys())
          checkout.fill(plan)
          receipt = checkout.submit()
          """);
      assertThat(result.getGlobalEnvironmentVariable("form_matches", Boolean.class)).isTrue();
      Dict<?, ?> receipt = result.getGlobalEnvironmentVariable("receipt", Dict.class);
      assertThat(receipt.get("resultCode")).isEqualTo("EncryptedFieldsReceived");
      assertThat((Iterable<?>) receipt.get("encryptedFields")).containsExactly(
          "encryptedCardNumber", "encryptedExpiryMonth", "encryptedExpiryYear", "encryptedSecurityCode");
    }
  }

  @StarlarkBuiltin(name = "adyen_sandbox_checkout", documented = false)
  public static final class Checkout implements StarlarkValue, AutoCloseable {
    private record Field(String label, String reference, String testValue) {}

    private static final Map<String, Field> FIELDS = Map.of(
        "encryptedCardNumber", new Field("Card number", "payment.card.number", "4111111111111111"),
        "encryptedExpiryDate", new Field("Expiry date", "payment.card.expiry", "03/30"),
        "encryptedSecurityCode", new Field("Security code", "payment.card.cvc", "737"));

    private final URI endpoint;
    private final HttpClient http = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();
    private final Map<String, StarlarkInt> contexts = new HashMap<>();
    private final Map<String, StarlarkInt> fields = new HashMap<>();
    private StarlarkThread thread;
    private KitesurfBrowser.CdpSession cdp;
    private String browserContextId;
    private String session;

    Checkout(URI endpoint) {
      this.endpoint = endpoint;
    }

    @StarlarkMethod(name = "inspect", doc = "Reads the names and labels of the test form.",
        useStarlarkThread = true)
    public Dict<String, String> inspect(StarlarkThread thread)
        throws EvalException, InterruptedException {
      this.thread = thread;
      long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
      cdp = new KitesurfBrowser.CdpSession(thread, deadline, 128, this::onEvent);
      try {
        cdp.connect(http, endpoint);
      } catch (ExecutionException | TimeoutException e) {
        throw Starlark.errorf("could not connect to the local demo browser");
      }
      browserContextId = (String) command("Target.createBrowserContext", Map.of(), null)
          .get("browserContextId");
      String target = (String) command("Target.createTarget",
          Map.of("url", "about:blank", "browserContextId", browserContextId), null).get("targetId");
      session = (String) command("Target.attachToTarget",
          Map.of("targetId", target, "flatten", true), null).get("sessionId");
      command("Page.enable", Map.of(), session);
      command("Runtime.enable", Map.of(), session);
      command("Page.navigate", Map.of("url", "http://localhost:3000/"), session);
      for (int attempt = 0; attempt < 32 && fields.size() < FIELDS.size(); attempt++) {
        Thread.sleep(1000);
        inspectFrames();
      }
      if (!fields.keySet().equals(FIELDS.keySet())) {
        throw Starlark.errorf("Adyen sandbox inputs were unavailable");
      }
      saveScreenshot();
      var labels = Dict.<String, String>builder();
      FIELDS.forEach((name, field) -> labels.put(name, field.label()));
      return labels.buildImmutable();
    }

    private void saveScreenshot() throws EvalException, InterruptedException {
      String filename = System.getProperty("larky.kitesurf.adyenDemoScreenshot");
      if (filename == null) {
        return;
      }
      Map<?, ?> screenshot = command("Page.captureScreenshot", Map.of("format", "png"), session);
      if (!(screenshot.get("data") instanceof String encoded)) {
        throw Starlark.errorf("test browser did not return a screenshot");
      }
      try {
        Files.write(Path.of(filename), Base64.getDecoder().decode(encoded));
      } catch (IOException | IllegalArgumentException e) {
        throw Starlark.errorf("could not save the test checkout screenshot");
      }
    }

    private void onEvent(Map<?, ?> message) {
      if ("Runtime.executionContextCreated".equals(message.get("method"))
          && message.get("params") instanceof Map<?, ?> params
          && params.get("context") instanceof Map<?, ?> context
          && context.get("auxData") instanceof Map<?, ?> aux
          && Boolean.TRUE.equals(aux.get("isDefault"))
          && aux.get("frameId") instanceof String frameId
          && context.get("id") instanceof StarlarkInt id) {
        contexts.put(frameId, id);
      }
    }

    private void inspectFrames() throws EvalException, InterruptedException {
      Map<?, ?> tree = asObject(command("Page.getFrameTree", Map.of(), session).get("frameTree"));
      if (!(tree.get("childFrames") instanceof List<?> childFrames)) {
        return;
      }
      for (Object child : childFrames) {
        Map<?, ?> frame = asObject(asObject(child).get("frame"));
        if (!(frame.get("url") instanceof String url)
            || !"checkoutshopper-test.adyen.com".equals(URI.create(url).getHost())) {
          continue;
        }
        StarlarkInt context = contexts.get(frame.get("id"));
        if (context != null) {
          Object name = evaluate("""
              document.querySelector("input[id^=adyen-checkout-encrypted]:not([disabled])")
                ?.id?.split("-")[2] ?? ""
              """, context, true);
          if (name instanceof String field && FIELDS.containsKey(field)) {
            fields.put(field, context);
          }
        }
      }
    }

    @StarlarkMethod(name = "fill", doc = "Fills the form using host-owned synthetic values.",
        parameters = {@Param(name = "plan")})
    public void fill(Dict<?, ?> plan) throws EvalException, InterruptedException {
      if (!plan.keySet().equals(fields.keySet()) || !fields.keySet().equals(FIELDS.keySet())) {
        throw Starlark.errorf("form plan must name every inspected test field");
      }
      for (String name : List.of("encryptedCardNumber", "encryptedExpiryDate", "encryptedSecurityCode")) {
        Field field = FIELDS.get(name);
        if (!field.reference().equals(plan.get(name))) {
          throw Starlark.errorf("unrecognized test value reference");
        }
        Object filled = evaluate("""
            (() => {
              // The demo's workerd adapter returns bytes as a cross-realm list.
              const encryptBytes = crypto.subtle.__adyenDemoEncryptBytes;
              crypto.subtle.encrypt = async function (...args) {
                return Uint8Array.from(await encryptBytes(...args)).buffer;
              };
              const enc = TextEncoder.prototype.encode;
              if (!(new TextEncoder().encode("x") instanceof Uint8Array)) {
                TextEncoder.prototype.encode = function (...args) {
                  return Uint8Array.from(enc.apply(this, args));
                };
              }
              const input = document.querySelector(%s);
              if (!input || input.disabled) return false;
              input.focus();
              input.value = %s;
              input.dispatchEvent(new InputEvent("input", { bubbles: true, inputType: "insertText" }));
              input.dispatchEvent(new Event("change", { bubbles: true }));
              input.blur();
              return true;
            })()
            """.formatted(quote("input[id^='adyen-checkout-" + name + "-']"),
                quote(field.testValue())), fields.get(name), false);
        if (!Boolean.TRUE.equals(filled)) {
          throw Starlark.errorf("Adyen test input was unavailable");
        }
      }
    }

    @StarlarkMethod(name = "submit", doc = "Submits to the local encrypted receipt fixture.")
    public Object submit() throws EvalException, InterruptedException {
      for (int attempt = 0; attempt < 25; attempt++) {
        Object ready = evaluate("document.querySelector('#result').dataset.ready", null, false);
        if ("true".equals(ready)) {
          return submitValidForm();
        }
        Thread.sleep(200);
      }
      throw Starlark.errorf("Adyen test checkout did not validate");
    }

    private Object submitValidForm() throws EvalException, InterruptedException {
      Object clicked = evaluate("""
          (() => {
            const button = document.querySelector("#card-container button.adyen-checkout__button--pay");
            if (!button || button.disabled) return false;
            button.click();
            return true;
          })()
          """, null, false);
      if (!Boolean.TRUE.equals(clicked)) {
        throw Starlark.errorf("Adyen test submit button was unavailable");
      }
      for (int attempt = 0; attempt < 20; attempt++) {
        Thread.sleep(500);
        Object status = evaluate("document.querySelector('#result').textContent", null, false);
        if (status instanceof String receipt && !receipt.isEmpty()) {
          return JsonModule.INSTANCE.decode(receipt, thread);
        }
      }
      throw Starlark.errorf("Adyen did not produce a local encrypted receipt");
    }

    private String quote(String text) throws EvalException, InterruptedException {
      return JsonModule.INSTANCE.encode(text, Starlark.NONE, thread);
    }

    private Object evaluate(String expression, StarlarkInt context, boolean loading)
        throws EvalException, InterruptedException {
      var params = new HashMap<String, Object>(Map.of(
          "expression", expression, "awaitPromise", true, "returnByValue", true));
      if (context != null) {
        params.put("contextId", context);
      }
      Map<?, ?> result;
      try {
        result = command("Runtime.evaluate", params, session);
      } catch (EvalException unavailableContext) {
        if (loading) {
          return null;
        }
        throw unavailableContext;
      }
      if (result.containsKey("exceptionDetails")) {
        if (loading) {
          return null;
        }
        throw Starlark.errorf("Adyen test browser evaluation failed");
      }
      return asObject(result.get("result")).get("value");
    }

    private Map<?, ?> command(String method, Map<String, Object> params, String session)
        throws EvalException, InterruptedException {
      try {
        return cdp.command(method, params, session);
      } catch (ExecutionException | TimeoutException e) {
        throw Starlark.errorf("test browser command failed: %s (%s)", method, e.getClass().getSimpleName());
      }
    }

    private static Map<?, ?> asObject(Object value) throws EvalException {
      if (value instanceof Map<?, ?> map) {
        return map;
      }
      throw Starlark.errorf("invalid test browser response");
    }

    @Override
    public void close() {
      try {
        if (browserContextId != null) {
          command("Target.disposeBrowserContext", Map.of("browserContextId", browserContextId), null);
        }
      } catch (EvalException ignored) {
        // A closed or expired connection already lost its browser context.
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
      } finally {
        if (cdp != null) {
          cdp.close();
        }
        http.close();
      }
    }
  }
}
