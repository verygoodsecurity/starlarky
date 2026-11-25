package com.verygood.security.run;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import net.starlark.java.eval.StarlarkEvalWrapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FunctionsCallerTest {

  private static final String FN = "FN" + "1".repeat(22);
  private static final String FI = "FI" + "2".repeat(22);

  @Rule public final TemporaryFolder files = new TemporaryFolder();

  @Test
  public void runsOneAuthenticatedInvocationAndPreservesTheFunctionResponse() throws Exception {
    final var request = new AtomicReference<Received>();
    final var calls = new AtomicInteger();
    serve(exchange -> {
      calls.incrementAndGet();
      request.set(new Received(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(),
          exchange.getRequestHeaders().getFirst("Authorization"),
          exchange.getRequestHeaders().getFirst("Content-Type"), exchange.getRequestBody().readAllBytes()));
      exchange.getResponseHeaders().set("Function-Invocation-Id", FI);
      exchange.getResponseHeaders().set("Function-Invocation-Status", "succeeded");
      exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
      exchange.getResponseHeaders().set("Content-Language", "en");
      exchange.getResponseHeaders().set("Set-Cookie", "sensitive=fixture");
      respond(exchange, 422, new byte[] {-1, 0});
    }, origin -> {
      final var result = run(origin, """
          result = functions.invoke("%s", b"\\x00\\xff")
          checks = [
              result["status_code"] == 422,
              result["invocation_id"] == "%s",
              result["outcome"] == "succeeded",
              result["headers"] == {"content-type": "application/octet-stream", "content-language": "en"},
              result["body"] == b"\\xff\\x00",
          ]
          "ok" if all(checks) else repr(result)
          """.formatted(FN, FI));
      assertEquals("ok", result);
    });
    assertEquals(1, calls.get());
    final var received = request.get();
    assertEquals("POST", received.method());
    assertEquals("/v1/functions/" + FN + "/invocations", received.path());
    assertEquals("Bearer synthetic.token", received.authorization());
    assertEquals("application/octet-stream", received.contentType());
    assertArrayEquals(new byte[] {0, -1}, received.body());
  }

  @Test
  public void acceptsNamedAndPositionalContentType() throws Exception {
    final var contentTypes = new CopyOnWriteArrayList<String>();
    serve(exchange -> {
      contentTypes.add(exchange.getRequestHeaders().getFirst("Content-Type"));
      respond(exchange, 204, new byte[0]);
    }, origin -> assertEquals("ok", run(origin, """
        functions.invoke("%s", b"", content_type="application/json")
        functions.invoke("%s", b"", "text/plain")
        "ok"
        """.formatted(FN, FN))));
    assertEquals(List.of("application/json", "text/plain"), contentTypes);
  }

  @Test
  public void preservesUncertainOutcomeWithoutRetryOrRedirect() throws Exception {
    final var calls = new AtomicInteger();
    serve(exchange -> {
      final int call = calls.incrementAndGet();
      if (call == 1) {
        exchange.getRequestBody().readAllBytes();
        exchange.close();
        return;
      }
      exchange.getResponseHeaders().set("Location", "/elsewhere");
      respond(exchange, 307, new byte[0]);
    }, origin -> assertEquals("ok", run(origin, """
        lost = functions.invoke("%s", b"")
        redirected = functions.invoke("%s", b"")
        checks = [
            lost["outcome"] == "unknown",
            lost["status_code"] == None,
            redirected["status_code"] == 307,
            redirected["outcome"] == "unknown",
        ]
        "ok" if all(checks) else repr([lost, redirected])
        """.formatted(FN, FN))));
    assertEquals(2, calls.get());
  }

  @Test
  public void rejectsAPathAsAFunctionIdBeforeSendingARequest() throws Exception {
    final var calls = new AtomicInteger();
    serve(exchange -> {
      calls.incrementAndGet();
      respond(exchange, 200, new byte[0]);
    }, origin -> assertTrue(assertThrows(StarlarkEvalWrapper.Exc.RuntimeEvalException.class,
        () -> run(origin, "functions.invoke(\"../another/path\", b\"\")\n"))
        .getMessage().contains("Invalid function version ID")));
    assertEquals(0, calls.get());
  }

  private String run(String origin, String source) throws Exception {
    final var script = Files.writeString(files.newFile().toPath(), source);
    final var token = Files.writeString(files.newFile().toPath(), "synthetic.token\n");
    final var output = files.newFile().toPath();
    final var command = new picocli.CommandLine(new LarkyEntrypoint());
    final var result = command.parseArgs("-s", script.toString(), "-o", output.toString(),
        "--functions-origin", origin, "--functions-token-file", token.toString());
    ((LarkyEntrypoint) result.commandSpec().userObject()).call();
    return Files.readString(output);
  }

  private void serve(com.sun.net.httpserver.HttpHandler handler, WithOrigin test) throws Exception {
    final var store = Path.of(files.getRoot().getPath(), "fixture.p12");
    final var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
        "-genkeypair", "-storetype", "PKCS12", "-keystore", store.toString(), "-storepass", "synthetic",
        "-alias", "https", "-keyalg", "EC", "-dname", "CN=localhost", "-ext", "SAN=dns:localhost",
        "-validity", "1", "-noprompt").redirectErrorStream(true).start();
    final var message = new ByteArrayOutputStream();
    process.getInputStream().transferTo(message);
    assertTrue(message.toString(), process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0);
    final var keys = KeyStore.getInstance(store.toFile(), "synthetic".toCharArray());
    final var keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagers.init(keys, "synthetic".toCharArray());
    final var trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagers.init(keys);
    final var ssl = SSLContext.getInstance("TLS");
    ssl.init(keyManagers.getKeyManagers(), trustManagers.getTrustManagers(), null);

    final var previous = SSLContext.getDefault();
    final var server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
    server.setHttpsConfigurator(new HttpsConfigurator(ssl));
    server.createContext("/", handler);
    server.start();
    SSLContext.setDefault(ssl);
    try {
      test.run("https://localhost:" + server.getAddress().getPort());
    } finally {
      SSLContext.setDefault(previous);
      server.stop(0);
    }
  }

  private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
    try (exchange) {
      exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
      exchange.getResponseBody().write(body);
    }
  }

  private record Received(String method, String path, String authorization, String contentType, byte[] body) {}

  @FunctionalInterface private interface WithOrigin {
    void run(String origin) throws Exception;
  }
}
