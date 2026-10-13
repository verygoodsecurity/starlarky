/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.wasm;

import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Opt-in one-shot worker backend. The host-supplied launcher must place the child JVM under an
 * independent OS resource limit before it reads stdin, and terminate the entire sandbox when the
 * launcher is killed. This class never claims that {@link ProcessBuilder} alone is a sandbox.
 */
public final class WorkerWasmRuntime implements WasmRuntime {
  private static final String PREFIX = "larky.wasm.worker.";
  private static final ConcurrentHashMap<Settings, Semaphore> ADMISSION = new ConcurrentHashMap<>();

  private record Settings(Path launcher, int backend, long maxRequestMillis, long maxMemoryBytes,
      int maxInputBytes, int maxOutputBytes, int maxDiagnosticBytes, int maxConcurrent) {}

  private final Settings settings;
  private final WasmHostLimits hostLimits;
  private final Semaphore slots;

  public WorkerWasmRuntime() throws WasmException {
    try {
      this.settings = configuredSettings();
      this.hostLimits = WasmHostLimits.configured();
    } catch (IllegalArgumentException e) {
      throw new WasmException(WasmException.Kind.INVALID_MODULE,
          "invalid WASM worker host configuration", e);
    }
    this.slots = ADMISSION.computeIfAbsent(settings, s -> new Semaphore(s.maxConcurrent()));
  }

  @Override public String name() { return "worker"; }

  @Override public boolean cacheProgramsInHost() { return false; }

  @Override public Program compile(byte[] wasm) throws WasmException {
    return compile(wasm, 0);
  }

  @Override public Program compile(byte[] wasm, long deadlineEpochMs) throws WasmException {
    if (wasm.length > hostLimits.maxModuleBytes()) {
      throw new WasmException(WasmException.Kind.INVALID_MODULE, "WebAssembly module exceeds host limit");
    }
    byte[] copy = wasm.clone();
    var request = new WorkerProtocol.Request(WorkerProtocol.COMPILE, settings.backend(), copy,
        new byte[0], 0, 1, 0, null, WasiHostPolicy.none());
    try {
      invoke(request, deadlineEpochMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new WasmException(WasmException.Kind.TIMEOUT, "worker compilation interrupted", e);
    }
    return new WorkerProgram(copy);
  }

  private final class WorkerProgram implements Program {
    private final byte[] wasm;

    WorkerProgram(byte[] wasm) { this.wasm = wasm; }

    @Override
    public Result run(byte[] stdin, Limits limits) throws WasmException, InterruptedException {
      if (limits.maxMemoryBytes() > settings.maxMemoryBytes()) {
        throw new WasmException(WasmException.Kind.MEMORY_LIMIT,
            "WASM memory exceeds worker host limit");
      }
      if (stdin.length > settings.maxInputBytes()) {
        throw new WasmException(WasmException.Kind.MEMORY_LIMIT, "WASM stdin exceeds worker limit");
      }
      if (limits.maxOutputBytes() > settings.maxOutputBytes()) {
        throw new WasmException(WasmException.Kind.OUTPUT_LIMIT,
            "WASM output exceeds worker host limit");
      }
      var request = new WorkerProtocol.Request(WorkerProtocol.RUN, settings.backend(), wasm,
          stdin.clone(), limits.maxMemoryBytes(), 1, limits.maxOutputBytes(), limits.randomSeed(),
          limits.wasiHostPolicy());
      return invoke(request, limits.deadlineEpochMs()).result();
    }
  }

  private WorkerProtocol.Response invoke(WorkerProtocol.Request request, long wallDeadlineMs)
      throws WasmException, InterruptedException {
    long wallRemaining = wallDeadlineMs == 0 ? settings.maxRequestMillis()
        : wallDeadlineMs - System.currentTimeMillis();
    long budgetMillis = Math.min(settings.maxRequestMillis(), wallRemaining);
    if (budgetMillis <= 0) throw timeout();
    // Keep all monotonic deadline arithmetic within Long.MAX_VALUE/2.
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(budgetMillis);
    if (!slots.tryAcquire(remaining(deadline), TimeUnit.NANOSECONDS)) throw timeout();
    try {
      long remainingMs = Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining(deadline)));
      byte[] encoded;
      try {
        encoded = WorkerProtocol.encodeRequest(new WorkerProtocol.Request(request.operation(),
            request.backend(), request.wasm(), request.stdin(), request.maxMemoryBytes(),
            remainingMs, request.maxOutputBytes(), request.randomSeed(), request.policy()));
      } catch (IOException e) {
        throw unavailable("could not encode worker request", e);
      }
      if (remaining(deadline) == 0) throw timeout();
      Process process = startLauncher(deadline);
      try {
        return exchange(process, encoded, request.maxOutputBytes(), deadline);
      } finally {
        terminate(process);
      }
    } finally {
      slots.release();
    }
  }

  private Process startLauncher(long deadline) throws WasmException, InterruptedException {
    ProcessBuilder builder = new ProcessBuilder(settings.launcher().toString());
    builder.environment().clear(); // Atlas Horizon's parent JVM carries credentials.
    AtomicBoolean abandoned = new AtomicBoolean();
    AtomicReference<Process> started = new AtomicReference<>();
    ExecutorService starter = Executors.newVirtualThreadPerTaskExecutor();
    try {
      Future<Process> future = starter.submit(() -> {
        Process process = builder.start();
        started.set(process);
        if (abandoned.get()) terminate(process);
        return process;
      });
      try {
        return future.get(remaining(deadline), TimeUnit.NANOSECONDS);
      } catch (TimeoutException | InterruptedException e) {
        abandoned.set(true);
        Process late = started.get();
        if (late != null) terminate(late);
        if (e instanceof InterruptedException interrupted) throw interrupted;
        throw timeout();
      } catch (ExecutionException e) {
        throw unavailable("could not start worker launcher", e.getCause());
      }
    } finally {
      starter.shutdownNow();
    }
  }

  private WorkerProtocol.Response exchange(Process process, byte[] request, int maxOutput,
      long deadline) throws WasmException, InterruptedException {
    long maxResponse = Math.min(Integer.MAX_VALUE,
        32L + 2L * maxOutput + WorkerProtocol.MAX_MESSAGE_BYTES);
    ExecutorService io = Executors.newVirtualThreadPerTaskExecutor();
    try {
      Future<byte[]> stdout = io.submit(() -> readBounded(process.getInputStream(), maxResponse));
      Future<byte[]> stderr = io.submit(
          () -> readBounded(process.getErrorStream(), settings.maxDiagnosticBytes()));
      Future<?> writer = io.submit(() -> {
        try (var pipe = process.getOutputStream()) { pipe.write(request); }
        return null;
      });
      if (!process.waitFor(remaining(deadline), TimeUnit.NANOSECONDS)) {
        terminate(process);
        throw timeout();
      }
      byte[] reply = stdout.get(remaining(deadline), TimeUnit.NANOSECONDS);
      stderr.get(remaining(deadline), TimeUnit.NANOSECONDS);
      writer.get(remaining(deadline), TimeUnit.NANOSECONDS);
      if (process.exitValue() != 0) throw unavailable("worker exited without a valid result", null);
      WorkerProtocol.Response result = WorkerProtocol.decodeResponse(reply, maxOutput);
      if (result.error() != null) throw result.error();
      return result;
    } catch (TimeoutException e) {
      terminate(process);
      throw timeout();
    } catch (ExecutionException | IOException e) {
      terminate(process);
      throw unavailable("worker protocol failed", e);
    } finally {
      terminate(process);
      try { process.getOutputStream().close(); } catch (IOException ignored) { }
      try { process.getInputStream().close(); } catch (IOException ignored) { }
      try { process.getErrorStream().close(); } catch (IOException ignored) { }
      io.shutdownNow();
    }
  }

  private static byte[] readBounded(InputStream in, long limit) throws IOException {
    try (in; ByteArrayOutputStream result = new ByteArrayOutputStream()) {
      byte[] chunk = new byte[4096];
      int n;
      while ((n = in.read(chunk)) != -1) {
        if (n > limit - result.size()) throw new IOException("worker output exceeded IPC limit");
        result.write(chunk, 0, n);
      }
      return result.toByteArray();
    }
  }

  private static long remaining(long deadline) {
    return Math.max(0, deadline - System.nanoTime());
  }

  private static void terminate(Process process) {
    if (!process.isAlive()) return;
    // Let a trusted launcher tear down its cgroup/container before forcibly ending the process.
    // A detached worker still needs out-of-band cleanup in the launcher contract.
    process.destroy();
    try {
      if (process.waitFor(200, TimeUnit.MILLISECONDS)) return;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
  }

  private static WasmException timeout() {
    return new WasmException(WasmException.Kind.TIMEOUT, "WASM worker deadline expired");
  }

  private static WasmException unavailable(String message, Throwable cause) {
    return new WasmException(WasmException.Kind.TRAP, message, cause);
  }

  private static Settings configuredSettings() throws WasmException {
    String value = System.getProperty(PREFIX + "launcher");
    if (value == null || value.isBlank()) {
      throw new WasmException(WasmException.Kind.INVALID_MODULE,
          "worker backend requires an explicit OS-limited launcher");
    }
    Path launcher = Path.of(value);
    if (!launcher.isAbsolute() || !Files.isRegularFile(launcher) || !Files.isExecutable(launcher)) {
      throw new WasmException(WasmException.Kind.INVALID_MODULE,
          "worker launcher must be an absolute executable file");
    }
    String backend = System.getProperty(PREFIX + "backend", "endive");
    int backendCode = switch (backend) {
      case "endive" -> WorkerProtocol.ENDIVE;
      case "graal-isolate" -> WorkerProtocol.GRAAL_ISOLATE;
      default -> throw new WasmException(WasmException.Kind.INVALID_MODULE,
          "worker backend must be endive or graal-isolate");
    };
    return new Settings(launcher, backendCode, positiveLong("maxRequestMillis", 10_000),
        positiveLong("maxMemoryBytes", Limits.DEFAULT_MAX_MEMORY_BYTES),
        positiveInt("maxInputBytes", 1 << 20),
        positiveInt("maxOutputBytes", Limits.DEFAULT_MAX_OUTPUT_BYTES),
        positiveInt("maxDiagnosticBytes", 4096), positiveInt("maxConcurrent", 4));
  }

  static int configuredMaxInputBytes() {
    return positiveInt("maxInputBytes", 1 << 20);
  }

  private static int positiveInt(String suffix, int fallback) {
    long value = positiveLong(suffix, fallback);
    if (value > Integer.MAX_VALUE) throw new IllegalArgumentException(PREFIX + suffix + " too large");
    return (int) value;
  }

  private static long positiveLong(String suffix, long fallback) {
    String value = System.getProperty(PREFIX + suffix);
    if (value == null) return fallback;
    try {
      long parsed = Long.parseLong(value);
      if (parsed > 0 && (!suffix.equals("maxRequestMillis") || parsed <= 86_400_000L)) {
        return parsed;
      }
    } catch (NumberFormatException ignored) {
      // Report the same configuration error below.
    }
    throw new IllegalArgumentException(PREFIX + suffix + " must be positive");
  }
}
