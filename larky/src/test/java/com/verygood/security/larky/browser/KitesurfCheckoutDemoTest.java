package com.verygood.security.larky.browser;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeNotNull;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.modules.JsonModule;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import org.junit.Test;

/** Exercises the same browser actions on two checkout layouts with different field IDs and order. */
public class KitesurfCheckoutDemoTest {
  @Test
  public void discoversAndFillsBothLayouts() throws Exception {
    String port = System.getProperty("larky.kitesurf.checkoutDemoPort");
    assumeNotNull(port);
    URI endpoint = URI.create("ws://127.0.0.1:" + Integer.parseInt(port) + "/devtools/browser");
    for (String path : new String[] {"/", "/?layout=alternate"}) {
      try (var browser = new BrowserSession(endpoint)) {
        var engine = new LarkyScriptEngine();
        engine.put("browser", browser);
        engine.put("start_url", "http://localhost:3000" + path);
        var result = (ParsedStarFile) engine.eval("""
            def run():
                browser.open(start_url)
                started = False
                for action in browser.inspect()["actions"]:
                    if action["label"] == "Pay with Addi":
                        browser.click(action["handle"])
                        started = True
                        break
                if not started:
                    fail("checkout action unavailable")

                for _ in range(20):
                    fields = browser.inspect()["fields"]
                    if fields:
                        break

                filled = []
                for field in fields:
                    if field["autocomplete"] == "email":
                        value = "shopper@example.test"
                    elif field["autocomplete"] == "tel":
                        value = "0000000000"
                    elif "documento" in field["label"].lower():
                        value = "0000000000"
                    else:
                        continue
                    browser.fill(field["handle"], value)
                    filled.append(field["label"])
                if len([field for field in browser.inspect()["fields"] if field["filled"] and field["valid"]]) != 3:
                    fail("checkout did not retain three valid fields")
                return sorted(filled)

            filled_labels = run()
            """);
        assertThat(result.getGlobalEnvironmentVariable("filled_labels", Sequence.class))
            .containsExactly("Celular", "Correo electrónico", "Documento de identidad");
      }
    }
  }

  @StarlarkBuiltin(name = "browser_demo_session", documented = false)
  public static final class BrowserSession implements StarlarkValue, AutoCloseable {
    private static final String INSPECT = """
        (() => {
          const fields = [];
          const actions = [];
          const handles = new Map();
          for (const node of document.querySelectorAll("input, textarea, button, a[href]")) {
            const bounds = node.getBoundingClientRect();
            if (node.matches(":disabled") || !bounds.width || !bounds.height) continue;
            const type = node.type ?? "";
            const action = node.matches("button, a[href], input[type=submit], input[type=button]");
            const field = node.matches("textarea, input") && !node.readOnly
              && (type === "textarea" || ["text", "email", "tel", "search", "url", "password"].includes(type));
            if (!action && !field) continue;
            if (handles.size >= 100) break;
            const label = (node.getAttribute("aria-label")
              || Array.from(node.labels ?? []).map(elem => elem.textContent).join(" ")
              || (action ? node.textContent || node.getAttribute("value") : node.getAttribute("placeholder"))
              || "").trim().slice(0, 200);
            const handle = %d + ":" + handles.size;
            handles.set(handle, node);
            if (action) actions.push({handle, label});
            else fields.push({
              handle, label, type, required: node.required,
              filled: Boolean(node.value), valid: node.validity.valid,
              autocomplete: (node.getAttribute("autocomplete") || "").slice(0, 80)
            });
          }
          globalThis.__larkyDemoHandles = handles;
          return {fields, actions};
        })()
        """;
    private final URI endpoint;
    private final HttpClient client =
        HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();
    private KitesurfBrowser.CdpSession cdp;
    private StarlarkThread thread;
    private String sessionId;
    private int snapshotId;

    BrowserSession(URI endpoint) {
      this.endpoint = endpoint;
    }

    @StarlarkMethod(name = "open", doc = "Opens a page in the demo's private browser context.",
        parameters = {@Param(name = "url")}, useStarlarkThread = true)
    public void open(String url, StarlarkThread thread) throws EvalException, InterruptedException {
      URI page = URI.create(url);
      if (cdp != null || !"http".equals(page.getScheme()) || !"localhost:3000".equals(page.getRawAuthority())) {
        throw Starlark.errorf("demo browser can open one loopback checkout");
      }
      this.thread = thread;
      cdp = new KitesurfBrowser.CdpSession(thread, System.nanoTime() + Duration.ofSeconds(30).toNanos());
      try {
        cdp.connect(client, endpoint);
      } catch (ExecutionException | TimeoutException e) {
        throw Starlark.errorf("could not connect to the demo browser");
      }
      String contextId = (String) command("Target.createBrowserContext", Map.of(), null).get("browserContextId");
      String target = (String) command("Target.createTarget",
          Map.of("url", "about:blank", "browserContextId", contextId), null).get("targetId");
      sessionId = (String) command("Target.attachToTarget",
          Map.of("targetId", target, "flatten", true), null).get("sessionId");
      var navigation = command("Page.navigate", Map.of("url", url), sessionId);
      if (navigation.containsKey("errorText")) {
        throw Starlark.errorf("could not open the demo checkout");
      }
    }

    @StarlarkMethod(name = "inspect", doc = "Lists editable fields and actions with opaque handles.")
    public Object inspect() throws EvalException, InterruptedException {
      String expression = INSPECT.formatted(++snapshotId);
      for (int attempt = 0; ; attempt++) {
        try {
          return evaluate(expression);
        } catch (EvalException e) {
          // Kitesurf can reject evaluation while replacing the document after a click.
          if (attempt == 9 || !"browser.render: Kitesurf rejected Runtime.evaluate".equals(e.getMessage())) {
            throw e;
          }
          Thread.sleep(50);
        }
      }
    }

    @StarlarkMethod(name = "click", doc = "Clicks an action from the latest inspection.",
        parameters = {@Param(name = "handle")})
    public void click(String handle) throws EvalException, InterruptedException {
      Map<?, ?> point = asObject(evaluate("""
          (() => {
            const node = globalThis.__larkyDemoHandles?.get(%s);
            if (!node?.isConnected || !node.matches("a[href], button, input[type=submit], input[type=button]")
              || node.matches(":disabled")) return null;
            node.scrollIntoView();
            const bounds = node.getBoundingClientRect();
            const x = bounds.x + bounds.width / 2;
            const y = bounds.y + bounds.height / 2;
            const hit = document.elementFromPoint(x, y);
            if (!hit || !node.contains(hit)) return null;
            return {x, y};
          })()
          """.formatted(quote(handle))));
      for (String type : new String[] {"mousePressed", "mouseReleased"}) {
        command("Input.dispatchMouseEvent", Map.of(
            "type", type, "x", point.get("x"), "y", point.get("y"), "button", "left",
            "clickCount", StarlarkInt.of(1)), sessionId);
      }
    }

    @StarlarkMethod(name = "fill", doc = "Enters text into a field from the latest inspection.",
        parameters = {@Param(name = "handle"), @Param(name = "text")})
    public void fill(String handle, String text) throws EvalException, InterruptedException {
      String key = quote(handle);
      if (text.isEmpty() || text.length() > 512 || !Boolean.TRUE.equals(evaluate("""
          (() => {
            const node = globalThis.__larkyDemoHandles?.get(%s);
            if (!node?.isConnected || node.readOnly
              || node.matches(":disabled, button, input[type=submit], input[type=button], a")) {
              return false;
            }
            node.focus();
            node.select();
            return document.activeElement === node;
          })()
          """.formatted(key)))) {
        throw Starlark.errorf("browser field is unavailable");
      }
      command("Input.insertText", Map.of("text", text), sessionId);
      if (!Boolean.TRUE.equals(evaluate("""
          globalThis.__larkyDemoHandles?.get(%s)?.value === %s
          """.formatted(key, quote(text))))) {
        throw Starlark.errorf("browser input did not retain the requested text");
      }
    }

    private String quote(String value) throws EvalException, InterruptedException {
      return JsonModule.INSTANCE.encode(value, Starlark.NONE, thread);
    }

    private Object evaluate(String expression) throws EvalException, InterruptedException {
      if (sessionId == null) {
        throw Starlark.errorf("open the browser before inspecting or interacting");
      }
      Map<?, ?> result = command("Runtime.evaluate",
          Map.of("expression", expression, "returnByValue", true, "awaitPromise", true), sessionId);
      if (result.containsKey("exceptionDetails")) {
        throw Starlark.errorf("demo browser could not evaluate the document");
      }
      return asObject(result.get("result")).get("value");
    }

    private Map<?, ?> command(String method, Map<String, Object> params, String session)
        throws EvalException, InterruptedException {
      try {
        return cdp.command(method, params, session);
      } catch (ExecutionException | TimeoutException e) {
        throw Starlark.errorf("demo browser command failed: %s", method);
      }
    }

    private static Map<?, ?> asObject(Object value) throws EvalException {
      if (value instanceof Map<?, ?> map) {
        return map;
      }
      throw Starlark.errorf("demo browser response unavailable");
    }

    @Override
    public void close() {
      try {
        if (cdp != null) {
          // Disconnecting releases the connection-owned browser context.
          cdp.close();
        }
      } finally {
        client.close();
      }
    }
  }
}
