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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
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
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

/**
 * Opt-in one-shot worker backend. The host-supplied launcher must place the child JVM under an
 * independent OS resource limit before it reads stdin, and terminate the entire sandbox when the
 * launcher is killed. This class never claims that {@link ProcessBuilder} alone is a sandbox.
 */
public final class WorkerWasmRuntime implements WasmRuntime {
  private static final String PREFIX = "larky.wasm.worker.";
  private static final ConcurrentHashMap<Settings, Semaphore> ADMISSION = new ConcurrentHashMap<>();

  private record Settings(Path launcher, String remoteHost, int remotePort, int backend,
      long maxRequestMillis, long maxMemoryBytes, int maxInputBytes, int maxOutputBytes,
      int maxDiagnosticBytes, int maxConcurrent) {}

  private final Settings settings;
  private final WasmHostLimits hostLimits;
  private final Semaphore slots;
  private final WorkerTlsConfig tls;

  public WorkerWasmRuntime() throws WasmException {
    try {
      this.settings = configuredSettings();
      this.hostLimits = WasmHostLimits.configured();
      this.tls = settings.remoteHost() == null ? null : WorkerTlsConfig.configured();
    } catch (IllegalArgumentException e) {
      throw new WasmException(WasmException.Kind.INVALID_MODULE, e.getMessage(), e);
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
      throw new WasmException(WasmException.Kind.INVALID_MODULE,
          "WebAssembly module exceeds host limit");
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
      return runOneShot(wasm, stdin, limits);
    }
  }

  // The remote server already received a RUN request. The child compiles before running, so
  // launching another child just to compile first adds no validation or isolation.
  Result runOneShot(byte[] wasm, byte[] stdin, Limits limits)
      throws WasmException, InterruptedException {
    if (wasm.length > hostLimits.maxModuleBytes()) {
      throw new WasmException(WasmException.Kind.INVALID_MODULE, "WebAssembly module exceeds host limit");
    }
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
      if (settings.remoteHost() != null) {
        return exchangeRemote(encoded, request.maxOutputBytes(), deadline);
      }
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

  private WorkerProtocol.Response exchangeRemote(byte[] request, int maxOutput, long deadline)
      throws WasmException, InterruptedException {
    AtomicReference<Socket> connection = new AtomicReference<>();
    AtomicBoolean abandoned = new AtomicBoolean();
    ExecutorService io = Executors.newVirtualThreadPerTaskExecutor();
    try {
      Future<WorkerProtocol.Response> exchange = io.submit(() -> {
        try (Socket raw = new Socket()) {
          connection.set(raw);
          if (abandoned.get()) throw new IOException("remote worker request expired");
          raw.connect(new InetSocketAddress(settings.remoteHost(), settings.remotePort()),
              timeoutMillis(deadline));
          if (abandoned.get()) throw new IOException("remote worker request expired");
          try (SSLSocket socket = (SSLSocket) tls.context().getSocketFactory()
              .createSocket(raw, settings.remoteHost(), settings.remotePort(), true)) {
            connection.set(socket);
            if (abandoned.get()) throw new IOException("remote worker request expired");
            socket.setSoTimeout(timeoutMillis(deadline));
            SSLParameters parameters = socket.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(parameters);
            socket.startHandshake();
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(request.length);
            out.write(request);
            out.flush();
            DataInputStream in = new DataInputStream(socket.getInputStream());
            long maxResponse = 32L + 2L * maxOutput + WorkerProtocol.MAX_MESSAGE_BYTES;
            int size = in.readInt();
            if (size < 0 || size > maxResponse) {
              throw new IOException("remote worker response exceeded IPC limit");
            }
            byte[] response = in.readNBytes(size);
            if (response.length != size) throw new IOException("truncated remote worker response");
            return WorkerProtocol.decodeResponse(response, maxOutput);
          }
        }
      });
      try {
        WorkerProtocol.Response response = exchange.get(remaining(deadline), TimeUnit.NANOSECONDS);
        if (response.error() != null) throw response.error();
        return response;
      } catch (TimeoutException e) {
        throw timeout();
      } catch (ExecutionException e) {
        throw unavailable("remote worker transport failed", e.getCause());
      }
    } finally {
      abandoned.set(true);
      Socket active = connection.get();
      if (active != null) try { active.close(); } catch (IOException ignored) { }
      io.shutdownNow();
    }
  }

  private static int timeoutMillis(long deadline) {
    return (int) Math.max(1, Math.min(Integer.MAX_VALUE,
        TimeUnit.NANOSECONDS.toMillis(remaining(deadline))));
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
      if (process.exitValue() != 0) {
        throw unavailable("worker exited without a valid result (exit code "
            + process.exitValue() + ")", null);
      }
      writer.get(remaining(deadline), TimeUnit.NANOSECONDS);
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
    String remoteHost = System.getProperty(PREFIX + "remote.host");
    boolean local = value != null && !value.isBlank();
    boolean remote = remoteHost != null && !remoteHost.isBlank();
    if (local == remote) {
      throw new WasmException(WasmException.Kind.INVALID_MODULE,
          "worker backend requires exactly one local launcher or remote host");
    }
    Path launcher = local ? Path.of(value) : null;
    if (local && (!launcher.isAbsolute() || !Files.isRegularFile(launcher)
        || !Files.isExecutable(launcher))) {
      throw new WasmException(WasmException.Kind.INVALID_MODULE,
          "worker launcher must be an absolute executable file");
    }
    int remotePort = 0;
    if (remote) {
      if (System.getProperty(PREFIX + "remote.port") == null) {
        throw new WasmException(WasmException.Kind.INVALID_MODULE,
            "remote worker port is required");
      }
      remotePort = positiveInt("remote.port", 0);
      if (remotePort > 65535) {
        throw new WasmException(WasmException.Kind.INVALID_MODULE,
            "remote worker port must be at most 65535");
      }
    }
    String backend = System.getProperty(PREFIX + "backend", "endive");
    int backendCode = switch (backend) {
      case "endive" -> WorkerProtocol.ENDIVE;
      case "graal-isolate" -> WorkerProtocol.GRAAL_ISOLATE;
      default -> throw new WasmException(WasmException.Kind.INVALID_MODULE,
          "worker backend must be endive or graal-isolate");
    };
    return new Settings(launcher, remoteHost, remotePort, backendCode,
        positiveLong("maxRequestMillis", 10_000),
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
    long parsed;
    try {
      parsed = Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(PREFIX + suffix + " must be an integer", e);
    }
    if (parsed <= 0) {
      throw new IllegalArgumentException(PREFIX + suffix + " must be positive");
    }
    if (suffix.equals("maxRequestMillis") && parsed > 86_400_000L) {
      throw new IllegalArgumentException(PREFIX + suffix + " must not exceed 24 hours");
    }
    return parsed;
  }
}
