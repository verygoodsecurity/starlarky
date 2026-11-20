package com.verygood.security.larky.browser;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.verygood.security.larky.modules.JsonModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.ThreadExpiration;

/** Host-granted access to a local Kitesurf engine for trusted documents. */
@StarlarkBuiltin(name = "kitesurf_browser", doc = "Renders trusted HTML with Kitesurf.")
public final class KitesurfBrowser implements StarlarkValue, AutoCloseable {
  private static final Duration RENDER_TIMEOUT = Duration.ofSeconds(15);
  private static final int MAX_HTML_BYTES = 256 * 1024;
  private static final int MAX_MESSAGE_CHARS = 3 * 1024 * 1024;
  private static final int VIEWPORT_WIDTH = 800;
  private static final int VIEWPORT_HEIGHT = 600;

  private final URI endpoint;
  private final HttpClient client;

  public KitesurfBrowser(URI endpoint) {
    this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
    if (!"ws".equals(endpoint.getScheme()) || !"127.0.0.1".equals(endpoint.getHost())
        || endpoint.getPort() < 1 || endpoint.getPort() > 65535
        || !"/devtools/browser".equals(endpoint.getRawPath())
        || endpoint.getRawUserInfo() != null || endpoint.getRawQuery() != null
        || endpoint.getRawFragment() != null) {
      throw new IllegalArgumentException("Expected ws://127.0.0.1:<port>/devtools/browser");
    }
    client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();
  }

  @StarlarkMethod(
      name = "render",
      doc = "Runs an inline HTML document and returns its visible text and PNG screenshot.",
      parameters = {@Param(name = "html", allowedTypes = {@ParamType(type = String.class)})},
      useStarlarkThread = true)
  public Dict<String, Object> render(String html, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (html.length() > MAX_HTML_BYTES || html.getBytes(UTF_8).length > MAX_HTML_BYTES) {
      throw Starlark.errorf("browser.render: HTML exceeds %d bytes", MAX_HTML_BYTES);
    }
    ThreadExpiration.check(thread);
    long remainingMs = Math.min(
        RENDER_TIMEOUT.toMillis(), thread.getExpirationMs() - System.currentTimeMillis());
    if (remainingMs <= 0) {
      throw Starlark.errorf("browser.render: evaluation expired");
    }
    long deadline = System.nanoTime() + Duration.ofMillis(remainingMs).toNanos();
    try (var cdp = new CdpSession(thread, deadline)) {
      cdp.connect(client, endpoint);
      String context = string(cdp.command("Target.createBrowserContext", Map.of(), null),
          "browserContextId");
      String target = string(cdp.command("Target.createTarget",
          Map.of("url", "about:blank", "browserContextId", context), null), "targetId");
      String session = string(cdp.command("Target.attachToTarget",
          Map.of("targetId", target, "flatten", true), null), "sessionId");
      cdp.command("Emulation.setDeviceMetricsOverride", Map.of(
          "width", StarlarkInt.of(VIEWPORT_WIDTH), "height", StarlarkInt.of(VIEWPORT_HEIGHT),
          "deviceScaleFactor", StarlarkInt.of(1), "mobile", false), session);
      Map<?, ?> tree = object(cdp.command("Page.getFrameTree", Map.of(), session).get("frameTree"));
      String frame = string(object(tree.get("frame")), "id");
      cdp.command("Page.setDocumentContent", Map.of("frameId", frame, "html", html), session);
      Map<?, ?> evaluation = cdp.command("Runtime.evaluate", Map.of(
          "expression", "document.body.innerText", "returnByValue", true), session);
      if (evaluation.containsKey("exceptionDetails")) {
        throw Starlark.errorf("browser.render: Kitesurf could not read the document text");
      }
      String text = string(object(evaluation.get("result")), "value");
      String png = string(cdp.command("Page.captureScreenshot", Map.of("format", "png"), session),
          "data");
      cdp.command("Target.disposeBrowserContext", Map.of("browserContextId", context), null);
      return Dict.immutableCopyOf(
          Map.of("text", text, "png", StarlarkBytes.immutableOf(decodePng(png))));
    } catch (TimeoutException e) {
      ThreadExpiration.check(thread);
      throw Starlark.errorf("browser.render: Kitesurf timed out");
    } catch (ExecutionException | IllegalStateException e) {
      throw Starlark.errorf("browser.render: Kitesurf connection failed");
    }
  }

  private static byte[] decodePng(String encoded) throws EvalException {
    try {
      return Base64.getDecoder().decode(encoded);
    } catch (IllegalArgumentException e) {
      throw Starlark.errorf("browser.render: invalid Kitesurf screenshot");
    }
  }

  private static Map<?, ?> object(Object value) throws EvalException {
    if (value instanceof Map<?, ?> map) {
      return map;
    }
    throw Starlark.errorf("browser.render: invalid Kitesurf response");
  }

  private static String string(Map<?, ?> object, String key) throws EvalException {
    if (object.get(key) instanceof String value) {
      return value;
    }
    throw Starlark.errorf("browser.render: missing Kitesurf response field %s", key);
  }

  @Override
  public void close() {
    client.close();
  }

  static final class CdpSession implements WebSocket.Listener, AutoCloseable {
    private final StarlarkThread thread;
    private final long deadline;
    private final ArrayBlockingQueue<String> messages = new ArrayBlockingQueue<>(16);
    private final StringBuilder fragment = new StringBuilder();
    private WebSocket socket;
    private int nextId;

    CdpSession(StarlarkThread thread, long deadline) {
      this.thread = thread;
      this.deadline = deadline;
    }

    void connect(HttpClient client, URI endpoint)
        throws ExecutionException, InterruptedException, TimeoutException {
      var opening = client.newWebSocketBuilder()
          .connectTimeout(Duration.ofNanos(remainingNanos()))
          .buildAsync(endpoint, this);
      try {
        socket = await(opening);
      } catch (InterruptedException | TimeoutException e) {
        opening.thenAccept(WebSocket::abort);
        opening.cancel(true);
        throw e;
      }
    }

    Map<?, ?> command(String method, Map<String, Object> params, String session)
        throws EvalException, ExecutionException, InterruptedException, TimeoutException {
      StarlarkInt id = StarlarkInt.of(++nextId);
      Map<String, Object> request = new LinkedHashMap<>();
      request.put("id", id);
      request.put("method", method);
      request.put("params", Dict.immutableCopyOf(params));
      if (session != null) {
        request.put("sessionId", session);
      }
      String encoded = JsonModule.INSTANCE.encode(Dict.immutableCopyOf(request), Starlark.NONE, thread);
      await(socket.sendText(encoded, true));
      for (;;) {
        String message = messages.poll(remainingNanos(), NANOSECONDS);
        if (message == null) {
          throw new TimeoutException();
        }
        if (message.isEmpty()) {
          throw Starlark.errorf("browser.render: Kitesurf disconnected");
        }
        Map<?, ?> response;
        try {
          response = object(JsonModule.INSTANCE.decode(message, thread));
        } catch (EvalException e) {
          throw Starlark.errorf("browser.render: invalid Kitesurf response");
        }
        if (!id.equals(response.get("id"))) {
          continue; // CDP events may arrive between replies.
        }
        if (response.containsKey("error")) {
          throw Starlark.errorf("browser.render: Kitesurf rejected %s", method);
        }
        return object(response.get("result"));
      }
    }

    private long remainingNanos() throws TimeoutException {
      long remaining = deadline - System.nanoTime();
      if (remaining <= 0) {
        throw new TimeoutException();
      }
      return remaining;
    }

    private <T> T await(Future<T> future)
        throws ExecutionException, InterruptedException, TimeoutException {
      return future.get(remainingNanos(), NANOSECONDS);
    }

    @Override
    public void onOpen(WebSocket webSocket) {
      webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      if (data.length() > MAX_MESSAGE_CHARS - fragment.length()) {
        webSocket.abort();
        disconnected();
        return null;
      }
      fragment.append(data);
      if (last) {
        if (!messages.offer(fragment.toString())) {
          webSocket.abort();
          disconnected();
          return null;
        }
        fragment.setLength(0);
      }
      webSocket.request(1);
      return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      disconnected();
      return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
      disconnected();
    }

    private void disconnected() {
      messages.clear();
      messages.offer("");
    }

    @Override
    public void close() {
      if (socket != null) {
        socket.abort(); // Kitesurf releases connection-owned page isolates on disconnect.
      }
    }
  }
}
