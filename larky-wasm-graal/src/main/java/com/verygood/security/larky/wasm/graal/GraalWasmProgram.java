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

package com.verygood.security.larky.wasm.graal;

import com.verygood.security.larky.wasm.WasmBinaries;
import com.verygood.security.larky.wasm.WasmException;
import com.verygood.security.larky.wasm.WasmLimits;
import com.verygood.security.larky.wasm.WasmProgram;
import com.verygood.security.larky.wasm.WasmResult;
import java.io.ByteArrayInputStream;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

/** A module compiled by {@link GraalWasmRuntime}. */
final class GraalWasmProgram implements WasmProgram {

  /** How often the watchdog checks a run's deadline, output and calling thread. */
  private static final long POLL_MILLIS = 5;

  private static final int ERRNO_SUCCESS = 0;
  private static final int ERRNO_INVAL = 28;

  private static final ScheduledExecutorService WATCHDOG =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "larky-wasm-graal-watchdog");
            t.setDaemon(true);
            return t;
          });

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final byte[] wasm;
  private final Source source;
  /** Sources with their memory capped, by cap in pages. */
  private final Map<Long, Source> cappedSources = new ConcurrentHashMap<>();

  GraalWasmProgram(byte[] wasm, Source source) {
    this.wasm = wasm;
    this.source = source;
  }

  private Source cappedSource(long maxPages) throws WasmException {
    Source cached = cappedSources.get(maxPages);
    if (cached != null) {
      return cached;
    }
    byte[] capped = WasmBinaries.capMemory(wasm, maxPages);
    Source result = capped == wasm ? source : GraalWasmRuntime.source(capped);
    Source previous = cappedSources.putIfAbsent(maxPages, result);
    return previous != null ? previous : result;
  }

  @Override
  public WasmResult run(byte[] stdin, WasmLimits limits)
      throws WasmException, InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    Source capped = cappedSource(limits.maxMemoryPages());
    if (limits.deadlineEpochMs() != 0 && System.currentTimeMillis() >= limits.deadlineEpochMs()) {
      throw new WasmException(WasmException.Kind.TIMEOUT, "deadline passed before the run");
    }
    CappedOutputStream stdout = new CappedOutputStream(limits.maxOutputBytes());
    CappedOutputStream stderr = new CappedOutputStream(limits.maxOutputBytes());
    Random random = limits.randomSeed() != null ? new Random(limits.randomSeed()) : SECURE_RANDOM;
    Context context =
        GraalWasmRuntime.contextBuilder()
            .option("wasm.Builtins", GraalWasmRuntime.WASI_MODULE)
            .in(new ByteArrayInputStream(stdin))
            .out(stdout)
            .err(stderr)
            .arguments("wasm", new String[] {"module"})
            .build();
    Watch watch = new Watch(context, Thread.currentThread(), limits.deadlineEpochMs(), stdout, stderr);
    ScheduledFuture<?> poll =
        WATCHDOG.scheduleWithFixedDelay(watch, POLL_MILLIS, POLL_MILLIS, TimeUnit.MILLISECONDS);
    int exitCode = 0;
    PolyglotException failure = null;
    boolean instantiated = false;
    try {
      Value module = context.eval(capped);
      Value[] memory = new Value[1];
      Value instance = module.newInstance(ProxyObject.fromMap(hostImports(memory, random)));
      instantiated = true;
      Value exports = instance.getMember("exports");
      memory[0] = exports.getMember("memory");
      exports.getMember("_start").executeVoid();
    } catch (PolyglotException e) {
      if (e.isExit()) {
        exitCode = e.getExitStatus();
      } else {
        failure = e;
      }
    } finally {
      poll.cancel(false);
      watch.finish();
      try {
        context.close();
      } catch (PolyglotException e) {
        // Closing a context the guest exited or that was cancelled reports that again.
      }
    }
    if (watch.interrupted) {
      Thread.interrupted();
      throw new InterruptedException("WebAssembly run interrupted");
    }
    if (stdout.overflowed() || stderr.overflowed()) {
      throw new WasmException(
          WasmException.Kind.OUTPUT_LIMIT,
          "output exceeded " + limits.maxOutputBytes() + " bytes",
          failure);
    }
    if (watch.timedOut) {
      throw new WasmException(WasmException.Kind.TIMEOUT, "deadline exceeded", failure);
    }
    if (failure != null) {
      if (failure.isCancelled()) {
        throw new WasmException(WasmException.Kind.TIMEOUT, "run cancelled", failure);
      }
      if (!instantiated && !failure.isResourceExhausted()) {
        throw new WasmException(
            WasmException.Kind.INVALID_MODULE,
            "could not instantiate module: " + failure.getMessage(),
            failure);
      }
      throw new WasmException(WasmException.Kind.TRAP, "trap: " + failure.getMessage(), failure);
    }
    return new WasmResult(exitCode, stdout.toByteArray(), stderr.toByteArray());
  }

  /** The WASI functions whose GraalWasm versions read the host clock and random source. */
  private static Map<String, Object> hostImports(Value[] memory, Random random) {
    ProxyExecutable clockTimeGet =
        args -> {
          int clockId = args[0].asInt();
          int resultPtr = args[2].asInt();
          if (clockId < 0 || clockId > 3) {
            return ERRNO_INVAL;
          }
          for (int i = 0; i < 8; i++) {
            memory[0].writeBufferByte(Integer.toUnsignedLong(resultPtr) + i, (byte) 0);
          }
          return ERRNO_SUCCESS;
        };
    ProxyExecutable randomGet =
        args -> {
          long ptr = Integer.toUnsignedLong(args[0].asInt());
          int len = args[1].asInt();
          byte[] bytes = new byte[len];
          random.nextBytes(bytes);
          for (int i = 0; i < len; i++) {
            memory[0].writeBufferByte(ptr + i, bytes[i]);
          }
          return ERRNO_SUCCESS;
        };
    return Map.of(
        GraalWasmRuntime.HOST_MODULE,
        ProxyObject.fromMap(Map.of("clock_time_get", clockTimeGet, "random_get", randomGet)));
  }

  /** Cancels a run at its deadline, when it overflows its output, or when its caller is interrupted. */
  private static final class Watch implements Runnable {
    private final Context context;
    private final Thread caller;
    private final long deadline;
    private final CappedOutputStream stdout;
    private final CappedOutputStream stderr;
    private boolean finished;
    volatile boolean timedOut;
    volatile boolean interrupted;

    Watch(Context context, Thread caller, long deadline, CappedOutputStream stdout,
        CappedOutputStream stderr) {
      this.context = context;
      this.caller = caller;
      this.deadline = deadline;
      this.stdout = stdout;
      this.stderr = stderr;
    }

    @Override
    public synchronized void run() {
      if (finished) {
        return;
      }
      if (caller.isInterrupted()) {
        interrupted = true;
      } else if (deadline != 0 && System.currentTimeMillis() >= deadline) {
        timedOut = true;
      } else if (!stdout.overflowed() && !stderr.overflowed()) {
        return;
      }
      finished = true;
      try {
        context.close(true);
      } catch (RuntimeException e) {
        // Already closing or closed.
      }
    }

    synchronized void finish() {
      finished = true;
    }
  }
}
