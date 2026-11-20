/*
 * Copyright 2026 Very Good Security Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.verygood.security.larky.modules;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.verygood.security.larky.wasm.BrowserWasmRuntime;
import com.verygood.security.larky.wasm.WasmRuntime;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.ThreadExpiration;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/** Experimental, host-granted Kitesurf DOM execution inside a Larky Wasm invocation. */
@StarlarkBuiltin(name = "kitesurf", doc = "Experimental in-process Kitesurf DOM session.")
public final class KitesurfModule implements StarlarkValue {
  public static final KitesurfModule INSTANCE = new KitesurfModule();
  private static final ThreadLocal<Invocation> CURRENT = new ThreadLocal<>();
  private static final int MAX_SCAN_OUTPUT_BYTES = 128 * 1024;

  private KitesurfModule() {}

  @StarlarkMethod(
      name = "open",
      doc = "Loads an approved document into the current Wasm guest.",
      parameters = @Param(name = "url"),
      useStarlarkThread = true)
  public void open(String url, StarlarkThread thread) throws EvalException, InterruptedException {
    Invocation invocation = currentInvocation(thread);
    if (url.isEmpty() || url.length() > 4096 || url.getBytes(UTF_8).length > 4096) {
      throw Starlark.errorf("invalid Kitesurf document URL");
    }
    invocation.documentOpen = false;
    step(Dict.immutableCopyOf(Map.of("op", "open", "url", url)), thread);
    invocation.documentOpen = true;
  }

  @StarlarkMethod(
      name = "inspect",
      doc = "Returns metadata from the document held by the current Wasm guest.",
      useStarlarkThread = true)
  public Dict<?, ?> inspect(StarlarkThread thread) throws EvalException, InterruptedException {
    if (!currentInvocation(thread).documentOpen) {
      throw Starlark.errorf("open a Kitesurf document before inspecting it");
    }
    return step(Dict.immutableCopyOf(Map.of("op", "inspect")), thread);
  }

  @StarlarkMethod(
      name = "wait_for_text",
      doc = "Pumps browser events until text appears in an element matched in the main document.",
      parameters = {@Param(name = "selector"), @Param(name = "text"),
          @Param(name = "timeout_ms", named = true, defaultValue = "5000")},
      useStarlarkThread = true)
  public boolean waitForText(String selector, String text, StarlarkInt timeoutMs,
      StarlarkThread thread) throws EvalException, InterruptedException {
    if (!currentInvocation(thread).documentOpen) {
      throw Starlark.errorf("open a Kitesurf document before waiting for text");
    }
    if (selector.isBlank() || text.isEmpty()
        || selector.getBytes(UTF_8).length > 512 || text.getBytes(UTF_8).length > 512) {
      throw Starlark.errorf("invalid Kitesurf text query");
    }
    int timeout = timeoutMs.toInt("timeout_ms");
    if (timeout < 0 || timeout > 30_000) {
      throw Starlark.errorf("Kitesurf timeout_ms must be between 0 and 30000");
    }
    var command = Dict.immutableCopyOf(Map.of("op", "match_text", "selector", selector, "text", text));
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout);
    while (true) {
      if (Boolean.TRUE.equals(step(command, thread).get("matched"))) {
        return true;
      }
      long remaining = deadline - System.nanoTime();
      if (remaining <= 0) {
        return false;
      }
      TimeUnit.NANOSECONDS.sleep(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(50)));
    }
  }

  @StarlarkMethod(
      name = "fill",
      doc = "Enters a host-resolved value into a field in the current Wasm guest.",
      parameters = {@Param(name = "handle"), @Param(name = "reference")},
      useStarlarkThread = true)
  public void fill(StarlarkInt handle, String reference, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Invocation invocation = currentInvocation(thread);
    if (!invocation.documentOpen) {
      throw Starlark.errorf("open a Kitesurf document before filling a field");
    }
    if (handle.signum() <= 0 || handle.toBigInteger().bitLength() > 64) {
      throw Starlark.errorf("invalid Kitesurf field handle");
    }
    if (reference.isEmpty() || reference.length() > 256) {
      throw Starlark.errorf("invalid Kitesurf value reference");
    }
    String value = invocation.host.resolveValue.apply(reference)
        .filter(text -> text.length() <= 1024 && text.getBytes(UTF_8).length <= 1024)
        .orElseThrow(() -> Starlark.errorf("Kitesurf value reference is unavailable"));
    step(Dict.immutableCopyOf(Map.of("op", "fill", "handle", handle, "value", value)), thread);
  }

  @StarlarkMethod(
      name = "click",
      doc = "Dispatches a click to a control in the current Wasm guest.",
      parameters = @Param(name = "handle"),
      useStarlarkThread = true)
  public void click(StarlarkInt handle, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (!currentInvocation(thread).documentOpen) {
      throw Starlark.errorf("open a Kitesurf document before clicking a control");
    }
    if (handle.signum() <= 0 || handle.toBigInteger().bitLength() > 64) {
      throw Starlark.errorf("invalid Kitesurf action handle");
    }
    step(Dict.immutableCopyOf(Map.of("op", "click", "handle", handle)), thread);
  }

  private static Invocation currentInvocation(StarlarkThread thread) throws EvalException {
    WasmModule.checkExecutionAllowed(thread);
    Invocation invocation = CURRENT.get();
    if (invocation == null) {
      throw Starlark.errorf("no Kitesurf host grant for this invocation");
    }
    if (invocation.failed) {
      throw Starlark.errorf("Kitesurf guest session is closed");
    }
    return invocation;
  }

  private static Dict<?, ?> step(Dict<?, ?> command, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Invocation invocation = currentInvocation(thread);
    byte[] input = JsonModule.INSTANCE.encode(command, Starlark.NONE, thread).getBytes(UTF_8);
    WasmRuntime.Result result;
    try {
      result = invocation.reactor(thread).step(input);
    } catch (WasmRuntime.WasmException e) {
      invocation.failed = true;
      ThreadExpiration.check(thread);
      throw Starlark.errorf("Kitesurf guest failed (%s)", e.kind());
    } catch (InterruptedException e) {
      invocation.failed = true;
      throw e;
    }
    ThreadExpiration.check(thread);
    if (result.exitCode() != 0) {
      invocation.failed = true;
      throw Starlark.errorf("Kitesurf guest exited unexpectedly");
    }
    Object decoded = JsonModule.INSTANCE.decode(new String(result.stdout(), UTF_8), thread);
    if (!(decoded instanceof Dict<?, ?> response) || response.containsKey("error")) {
      throw Starlark.errorf("Kitesurf could not complete the browser command");
    }
    return response;
  }

  /**
   * Prepares a trusted guest from host configuration. The host selects the guest and grants
   * origins, and a script can access it only while the host has opened an invocation.
   */
  public static Host prepareHost(byte[] guest, Set<URI> origins, int maxRequests,
      BrowserWasmRuntime.Fetcher fetcher, Function<String, Optional<String>> resolveValue)
      throws WasmRuntime.WasmException {
    return prepareHost(guest, origins, maxRequests, fetcher, Set.of(),
        (url, body, deadline, limit) -> { throw new IOException("browser POST unavailable"); },
        resolveValue);
  }

  /** Gives this trusted guest a separate, exact URL allowlist for POST requests. */
  public static Host prepareHost(byte[] guest, Set<URI> origins, int maxRequests,
      BrowserWasmRuntime.Fetcher fetcher, Set<URI> postUrls, BrowserWasmRuntime.Poster poster,
      Function<String, Optional<String>> resolveValue) throws WasmRuntime.WasmException {
    return new Host(BrowserWasmRuntime.compile(guest), origins, maxRequests, fetcher, postUrls,
        poster, resolveValue);
  }

  public static final class Host {
    private static final BouncyCastleProvider CRYPTO_PROVIDER = new BouncyCastleProvider();

    private final BrowserWasmRuntime.Program program;
    private final Set<URI> origins;
    private final int maxRequests;
    private final BrowserWasmRuntime.Fetcher fetcher;
    private final Set<URI> postUrls;
    private final BrowserWasmRuntime.Poster poster;
    private final Function<String, Optional<String>> resolveValue;

    private Host(BrowserWasmRuntime.Program program, Set<URI> origins, int maxRequests,
        BrowserWasmRuntime.Fetcher fetcher, Set<URI> postUrls, BrowserWasmRuntime.Poster poster,
        Function<String, Optional<String>> resolveValue) {
      this.program = program;
      this.origins = Set.copyOf(origins);
      this.maxRequests = maxRequests;
      this.fetcher = Objects.requireNonNull(fetcher);
      this.postUrls = Set.copyOf(postUrls);
      this.poster = Objects.requireNonNull(poster);
      this.resolveValue = Objects.requireNonNull(resolveValue);
    }

    /** Opens a fresh network grant on the thread that evaluates the Larky script. */
    public Invocation openInvocation() {
      if (CURRENT.get() != null) {
        throw new IllegalStateException("A Kitesurf invocation is already open on this thread");
      }
      var invocation =
          new Invocation(this,
              new BrowserWasmRuntime.Session(origins, maxRequests, fetcher, postUrls, poster,
                  CRYPTO_PROVIDER));
      CURRENT.set(invocation);
      return invocation;
    }
  }

  public static final class Invocation implements AutoCloseable {
    private final Host host;
    private final BrowserWasmRuntime.Session session;
    private final Thread owner = Thread.currentThread();
    private BrowserWasmRuntime.Reactor reactor;
    private boolean closed;
    private boolean failed;
    private boolean documentOpen;

    private Invocation(Host host, BrowserWasmRuntime.Session session) {
      this.host = host;
      this.session = session;
    }

    private BrowserWasmRuntime.Reactor reactor(StarlarkThread thread)
        throws WasmRuntime.WasmException {
      if (reactor == null) {
        var limits = new WasmRuntime.Limits(
            WasmModule.property(WasmModule.MAX_MEMORY_PROPERTY, WasmRuntime.Limits.DEFAULT_MAX_MEMORY_BYTES),
            Math.max(1, thread.getExpirationMs() + 1), MAX_SCAN_OUTPUT_BYTES, null);
        reactor = host.program.open(limits, session);
      }
      return reactor;
    }

    @Override
    public void close() {
      if (Thread.currentThread() != owner) {
        throw new IllegalStateException("Close the Kitesurf invocation on its evaluation thread");
      }
      if (!closed) {
        CURRENT.remove();
        closed = true;
        if (reactor != null) {
          reactor.close();
          reactor = null;
        }
      }
    }
  }
}
