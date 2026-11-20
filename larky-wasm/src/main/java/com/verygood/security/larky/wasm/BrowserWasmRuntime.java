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
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** Experimental Endive entry point for a host-approved browser guest. */
public final class BrowserWasmRuntime {
  static final String MODULE = "vgs_browser_v1";
  static final String HTTP_GET = "http_get";
  static final String INITIALIZE = "_initialize";
  static final String STEP = "vgs_browser_step";
  static final String HTTP_GET_SIGNATURE = "iiii:i";
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

  /** An invocation grant and request budget shared by that invocation's guest calls. */
  public static final class Session {
    private final Set<URI> origins;
    private final AtomicInteger remaining;
    private final Fetcher fetcher;

    public Session(Set<URI> origins, int maxRequests, Fetcher fetcher) {
      if (maxRequests <= 0) {
        throw new IllegalArgumentException("A positive browser request budget is required");
      }
      this.origins = origins.stream().map(Session::validateOrigin).collect(Collectors.toUnmodifiableSet());
      this.remaining = new AtomicInteger(maxRequests);
      this.fetcher = Objects.requireNonNull(fetcher);
    }

    int get(long[] args, WasiHost.GuestMemory memory, long deadlineEpochMs) {
      long urlAt = u32(args[0]);
      long urlSize = u32(args[1]);
      long outAt = u32(args[2]);
      long outSize = u32(args[3]);
      if (urlSize == 0 || urlSize > MAX_URL_BYTES || outSize == 0 || outSize > MAX_BODY_BYTES
          || !validRange(memory, urlAt, urlSize) || !validRange(memory, outAt, outSize)) {
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
      if (origin == null || !origins.contains(origin) || destination.getRawFragment() != null) {
        return DENIED;
      }
      if (Thread.currentThread().isInterrupted()
          || System.currentTimeMillis() >= deadlineEpochMs) {
        throw new WasiHost.Stop();
      }
      if (remaining.getAndUpdate(count -> count > 0 ? count - 1 : 0) <= 0) {
        return DENIED;
      }

      byte[] response;
      try {
        response = fetcher.get(destination, deadlineEpochMs, (int) outSize);
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
      if (!https && !(scheme.equals("http") && host.equals("127.0.0.1"))) {
        return null;
      }
      if (port == (https ? 443 : 80)) {
        port = -1;
      }
      return URI.create(scheme + "://" + host + (port == -1 ? "" : ":" + port));
    }

    private static boolean validRange(WasiHost.GuestMemory memory, long start, long size) {
      return start <= memory.size() && size <= memory.size() - start;
    }

    private static long u32(long value) {
      return Integer.toUnsignedLong((int) value);
    }
  }
}
