package com.verygood.security.run;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.util.Timeout;

/** A single run's host-granted capability to make direct invocation requests. */
public final class FunctionsCaller implements StarlarkValue, AutoCloseable {

  private static final int MAX_BODY_BYTES = 1_048_576;
  private static final int MAX_TOKEN_BYTES = 16_384;
  private static final long TIMEOUT_MILLIS = 15_000;
  private static final Pattern FUNCTION_ID = Pattern.compile("FN[1-9A-HJ-NP-Za-km-z]{22}");
  private static final Pattern INVOCATION_ID = Pattern.compile("FI[1-9A-HJ-NP-Za-km-z]{22}");
  private static final Pattern BEARER_TOKEN = Pattern.compile("[A-Za-z0-9._~+/\\-]+=*");
  private static final Set<String> OUTCOMES = Set.of("succeeded", "failed", "rejected", "unknown");
  private static final List<String> RESPONSE_HEADERS = List.of("content-type", "content-language");

  private final URI origin;
  private final String authorization;
  private final CloseableHttpClient client;
  private final ScheduledExecutorService deadlines = Executors.newSingleThreadScheduledExecutor();

  FunctionsCaller(URI origin, Path tokenFile) throws IOException {
    if (!"https".equalsIgnoreCase(origin.getScheme()) || origin.getHost() == null
        || origin.getRawUserInfo() != null || origin.getRawQuery() != null || origin.getRawFragment() != null
        || !("".equals(origin.getRawPath()) || "/".equals(origin.getRawPath()))) {
      throw new IOException("Functions origin must be an HTTPS origin without a path, credentials, query, or fragment");
    }
    this.origin = origin;
    this.authorization = "Bearer " + readToken(tokenFile);
    final var tls = ClientTlsStrategyBuilder.create().setSslContext(defaultTls()).buildClassic();
    this.client = HttpClients.custom()
        .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create().setTlsSocketStrategy(tls).build())
        .disableAutomaticRetries()
        .disableRedirectHandling()
        .disableCookieManagement()
        .disableContentCompression()
        .build();
  }

  private static SSLContext defaultTls() throws IOException {
    try {
      return SSLContext.getDefault();
    } catch (java.security.NoSuchAlgorithmException failure) {
      throw new IOException("Functions TLS context is unavailable", failure);
    }
  }

  private static String readToken(Path tokenFile) throws IOException {
    try (var input = Files.newInputStream(tokenFile)) {
      final var bytes = input.readNBytes(MAX_TOKEN_BYTES + 1);
      final var token = new String(bytes, StandardCharsets.US_ASCII).strip();
      if (bytes.length > MAX_TOKEN_BYTES || !BEARER_TOKEN.matcher(token).matches()) {
        throw new IOException("Functions token file must contain one bearer token of at most 16 KiB");
      }
      return token;
    }
  }

  @StarlarkMethod(name = "invoke", doc = "Invokes an immutable function version in the host's vault.",
      parameters = {@Param(name = "version_id"), @Param(name = "body"),
          @Param(name = "content_type", defaultValue = "\"application/octet-stream\"")},
      useStarlarkThread = true)
  public Dict<String, Object> invoke(String versionId, StarlarkBytes body, String contentType, StarlarkThread thread)
      throws EvalException {
    if (!FUNCTION_ID.matcher(versionId).matches()) {
      throw Starlark.errorf("Invalid function version ID");
    }
    if (body.size() > MAX_BODY_BYTES) {
      throw Starlark.errorf("Functions request body exceeds 1 MiB");
    }
    final ContentType mediaType;
    try {
      if (contentType.length() > 256 || contentType.chars().anyMatch(c -> c < 32 || c >= 127)) {
        throw new IllegalArgumentException();
      }
      mediaType = ContentType.parse(contentType);
    } catch (IllegalArgumentException failure) {
      throw Starlark.errorf("Invalid Functions content type");
    }
    final long budget = Math.min(TIMEOUT_MILLIS, thread.getExpirationMs() - System.currentTimeMillis());
    if (budget <= 0) {
      throw Starlark.errorf("Functions invocation deadline expired before dispatch");
    }
    final var request = new HttpPost(origin.resolve("/v1/functions/" + versionId + "/invocations"));
    request.setConfig(RequestConfig.custom().setResponseTimeout(Timeout.ofMilliseconds(budget))
        .setHardCancellationEnabled(true).setAuthenticationEnabled(false).build());
    request.setHeader("Authorization", authorization);
    request.setEntity(new ByteArrayEntity(body.toByteArray(), mediaType));
    final var timeout = deadlines.schedule(request::cancel, budget, TimeUnit.MILLISECONDS);
    try (var response = client.executeOpen(null, request, new BasicHttpContext())) {
      return result(response, request);
    } catch (IOException failure) {
      return result(Starlark.NONE, Starlark.NONE, "unknown", Map.of(), new byte[0]);
    } finally {
      timeout.cancel(false);
    }
  }

  private static Dict<String, Object> result(ClassicHttpResponse response, HttpPost request) {
    final var status = StarlarkInt.of(response.getCode());
    final String id = singleHeader(response, "Function-Invocation-Id");
    final Object invocationId = id != null && INVOCATION_ID.matcher(id).matches() ? id : Starlark.NONE;
    final String suppliedOutcome = singleHeader(response, "Function-Invocation-Status");
    final String outcome = suppliedOutcome != null && OUTCOMES.contains(suppliedOutcome) ? suppliedOutcome : "unknown";
    final Map<String, String> headers = new LinkedHashMap<>();
    for (String name : RESPONSE_HEADERS) {
      final var value = singleHeader(response, name);
      if (value != null) {
        headers.put(name, value);
      }
    }
    final var entity = response.getEntity();
    if (entity == null) {
      return result(status, invocationId, outcome, headers, new byte[0]);
    }
    try {
      final var bytes = entity.getContent().readNBytes(MAX_BODY_BYTES + 1);
      if (bytes.length <= MAX_BODY_BYTES) {
        return result(status, invocationId, outcome, headers, bytes);
      }
    } catch (IOException ignored) {
      // The HTTP status or ID may have arrived before the body was lost.
    }
    request.cancel();
    return result(status, invocationId, "unknown", headers, new byte[0]);
  }

  private static String singleHeader(ClassicHttpResponse response, String name) {
    final var headers = response.getHeaders(name);
    return headers.length == 1 ? headers[0].getValue() : null;
  }

  private static Dict<String, Object> result(Object status, Object invocationId, String outcome,
      Map<String, String> headers, byte[] body) {
    return Dict.immutableCopyOf(Map.of("status_code", status, "invocation_id", invocationId, "outcome", outcome,
        "headers", Dict.immutableCopyOf(headers), "body", StarlarkBytes.of(null, body)));
  }

  @Override
  public void close() throws IOException {
    deadlines.shutdownNow();
    client.close();
  }
}
