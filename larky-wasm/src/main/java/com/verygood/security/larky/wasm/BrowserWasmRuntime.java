/*
 * Copyright 2026 Very Good Security Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.verygood.security.larky.wasm;

import com.verygood.security.larky.wasm.WasmRuntime.Limits;
import com.verygood.security.larky.wasm.WasmRuntime.Result;
import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** Experimental Endive entry point for a host-approved browser guest. */
public final class BrowserWasmRuntime {
  static final String MODULE = "vgs_browser_v1";
  private static final String HTTP_GET = "http_get";
  private static final String HTTP_POST = "http_post";
  private static final String WALL_CLOCK_MS = "wall_clock_ms";
  static final String INITIALIZE = "_initialize";
  static final String STEP = "vgs_browser_step";
  static final Map<String, String> SIGNATURES = Map.of(
      HTTP_GET, "iiii:i",
      HTTP_POST, "iiiiii:i",
      WALL_CLOCK_MS, "i:i",
      BrowserWebCrypto.AES_GCM_ENCRYPT, "iiiiiiiiiii:i",
      BrowserWebCrypto.RSA_OAEP_256_ENCRYPT, "iiiiiiii:i");
  private static final int MAX_URL_BYTES = 4 * 1024;
  private static final int MAX_BODY_BYTES = 1024 * 1024;
  private static final int DENIED = -1;
  private static final int IO_ERROR = -2;
  private static final int BODY_TOO_LARGE = -3;

  private BrowserWasmRuntime() {}

  /** Only trusted hosts can authorize compilation with the browser import namespace. */
  public static Program compile(byte[] wasm) throws WasmException {
    EndiveWasmProgram guest = new EndiveWasmRuntime().compile(wasm, true);
    return (limits, session) -> {
      if (limits.deadlineEpochMs() == 0) {
        throw new IllegalArgumentException("A browser guest requires a finite deadline");
      }
      return guest.openBrowser(limits, Objects.requireNonNull(session));
    };
  }

  @FunctionalInterface
  public interface Program {
    Reactor open(Limits limits, Session session) throws WasmException;
  }

  /** A guest instance retained only until its owning evaluation closes it. */
  public interface Reactor extends AutoCloseable {
    Result step(byte[] input) throws WasmException, InterruptedException;

    @Override
    void close();
  }

  /**
   * A host-owned GET transport. Each request must honor the deadline and response byte cap.
   * Following a redirect here would bypass the guest session's destination checks.
   */
  @FunctionalInterface
  public interface Fetcher {
    byte[] get(URI url, long deadlineEpochMs, int maxBytes) throws IOException, InterruptedException;
  }

  /**
   * A host-owned POST transport. Honor the deadline and response cap without following redirects.
   * The request body is borrowed for this call and must not be retained.
   */
  @FunctionalInterface
  public interface Poster {
    byte[] post(URI url, byte[] body, long deadlineEpochMs, int maxBytes)
        throws IOException, InterruptedException;
  }

  /** An invocation grant and request budget shared by that invocation's guest calls. */
  public static final class Session {
    private final Set<URI> origins;
    private final Set<URI> exactPostUrls;
    private final AtomicInteger remaining;
    private final Fetcher fetcher;
    private final Poster poster;
    private final BrowserWebCrypto webCrypto;

    public Session(Set<URI> origins, int maxRequests, Fetcher fetcher) {
      this(origins, maxRequests, fetcher, Set.of(), (url, body, deadline, maxBytes) -> {
        throw new AssertionError("POST transport called without a grant");
      });
    }

    public Session(Set<URI> origins, int maxRequests, Fetcher fetcher,
        Set<URI> exactPostUrls, Poster poster) {
      this(origins, maxRequests, fetcher, exactPostUrls, poster, null);
    }

    /**
     * Uses the host's JCA provider for browser encryption. A null provider selects the JVM defaults.
     * An explicit provider must supply the requested primitives, including the RSA key factory.
     */
    public Session(Set<URI> origins, int maxRequests, Fetcher fetcher,
        Set<URI> exactPostUrls, Poster poster, Provider cryptoProvider) {
      if (maxRequests <= 0) {
        throw new IllegalArgumentException("A positive browser request budget is required");
      }
      this.origins = origins.stream().map(Session::validateOrigin).collect(Collectors.toUnmodifiableSet());
      this.exactPostUrls = Set.copyOf(exactPostUrls);
      for (URI url : this.exactPostUrls) {
        if (originOf(url) == null || url.getRawFragment() != null) {
          throw new IllegalArgumentException("Browser POST grants must contain exact HTTP URLs");
        }
      }
      this.remaining = new AtomicInteger(maxRequests);
      this.fetcher = Objects.requireNonNull(fetcher);
      this.poster = Objects.requireNonNull(poster);
      this.webCrypto = new BrowserWebCrypto(cryptoProvider);
    }

    WasiHost.Function bind(String name, long deadlineEpochMs) {
      if (HTTP_GET.equals(name)) {
        return (args, memory) -> request(args, memory, deadlineEpochMs, false);
      }
      if (HTTP_POST.equals(name)) {
        return (args, memory) -> request(args, memory, deadlineEpochMs, true);
      }
      WasiHost.Function function = switch (name) {
        case WALL_CLOCK_MS -> BrowserWasmRuntime::wallClock;
        case BrowserWebCrypto.AES_GCM_ENCRYPT -> webCrypto::aesGcmEncrypt;
        case BrowserWebCrypto.RSA_OAEP_256_ENCRYPT -> webCrypto::rsaOaep256Encrypt;
        default -> throw new IllegalArgumentException("Unknown browser host function: " + name);
      };
      return (args, memory) -> {
        checkDeadline(deadlineEpochMs);
        return function.call(args, memory);
      };
    }

    private int request(long[] args, WasiHost.GuestMemory memory, long deadlineEpochMs,
        boolean post) {
      long urlAt = u32(args[0]);
      long urlSize = u32(args[1]);
      int outputIndex = post ? 4 : 2;
      long outAt = u32(args[outputIndex]);
      long outSize = u32(args[outputIndex + 1]);
      if (urlSize == 0 || urlSize > MAX_URL_BYTES || outSize == 0 || outSize > MAX_BODY_BYTES
          || !validRange(memory, urlAt, urlSize) || !validRange(memory, outAt, outSize)) {
        return DENIED;
      }
      long bodyAt = post ? u32(args[2]) : 0;
      long bodySize = post ? u32(args[3]) : 0;
      if (post && (bodySize > MAX_BODY_BYTES || !validRange(memory, bodyAt, bodySize))) {
        return DENIED;
      }
      byte[] urlBytes = new byte[(int) urlSize];
      memory.read(urlAt, urlBytes, 0, urlBytes.length);

      URI destination;
      try {
        String address = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(urlBytes)).toString();
        destination = new URI(address);
      } catch (IOException | URISyntaxException invalid) {
        return DENIED;
      }
      URI origin = originOf(destination);
      if (origin == null || destination.getRawFragment() != null
          || (post ? !exactPostUrls.contains(destination) : !origins.contains(origin))) {
        return DENIED;
      }
      checkDeadline(deadlineEpochMs);
      if (remaining.getAndUpdate(count -> count > 0 ? count - 1 : 0) <= 0) {
        return DENIED;
      }

      byte[] response;
      try {
        if (post) {
          byte[] body = new byte[(int) bodySize];
          if (body.length != 0) {
            memory.read(bodyAt, body, 0, body.length);
          }
          try {
            response = poster.post(destination, body, deadlineEpochMs, (int) outSize);
          } finally {
            Arrays.fill(body, (byte) 0);
          }
        } else {
          response = fetcher.get(destination, deadlineEpochMs, (int) outSize);
        }
      } catch (IOException unreachable) {
        return IO_ERROR;
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        throw new WasiHost.Stop();
      }
      if (response.length > outSize) {
        return BODY_TOO_LARGE;
      }
      memory.write(outAt, response, 0, response.length);
      return response.length;
    }

    private static URI validateOrigin(URI uri) {
      URI origin = originOf(uri);
      if (origin == null || uri.getRawQuery() != null || uri.getRawFragment() != null
          || (!uri.getRawPath().isEmpty() && !"/".equals(uri.getRawPath()))) {
        throw new IllegalArgumentException("Browser grants must contain HTTP origins");
      }
      return origin;
    }

    private static URI originOf(URI uri) {
      String scheme = uri.getScheme();
      String host = uri.getHost();
      int port = uri.getPort();
      if (scheme == null || host == null || uri.getRawUserInfo() != null
          || port == 0 || port > 65535) {
        return null;
      }
      scheme = scheme.toLowerCase(Locale.ROOT);
      host = host.toLowerCase(Locale.ROOT);
      boolean https = scheme.equals("https");
      if (!https && !(scheme.equals("http") && (host.equals("127.0.0.1") || host.equals("localhost")))) {
        return null;
      }
      if (port == (https ? 443 : 80)) {
        port = -1;
      }
      return URI.create(scheme + "://" + host + (port == -1 ? "" : ":" + port));
    }
  }

  private static int wallClock(long[] args, WasiHost.GuestMemory memory) {
    long output = u32(args[0]);
    if (!validRange(memory, output, Long.BYTES)) {
      return DENIED;
    }
    byte[] epochMillis = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN)
        .putLong(System.currentTimeMillis()).array();
    memory.write(output, epochMillis, 0, Long.BYTES);
    return 0;
  }

  private static void checkDeadline(long deadlineEpochMs) {
    if (Thread.currentThread().isInterrupted() || System.currentTimeMillis() >= deadlineEpochMs) {
      throw new WasiHost.Stop();
    }
  }

  static boolean validRange(WasiHost.GuestMemory memory, long start, long size) {
    return start <= memory.size() && size <= memory.size() - start;
  }

  static long u32(long value) {
    return Integer.toUnsignedLong((int) value);
  }
}
