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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
final class GraalWasmProgram implements WasmRuntime.Program {

  /** How often the watchdog checks a run's deadline, output and calling thread. */
  private static final long POLL_MILLIS = 5;

  private static final ScheduledExecutorService WATCHDOG =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "larky-wasm-graal-watchdog");
            t.setDaemon(true);
            return t;
          });

  private final byte[] wasm;
  private final Source source;
  /** The module's WASI imports, every one of them served by {@link WasiHost}. */
  private final Set<String> imports;
  /** Sources with their memory capped, by cap in pages. */
  private final Map<Long, Source> cappedSources = new ConcurrentHashMap<>();

  GraalWasmProgram(byte[] wasm, Source source, Set<String> imports) {
    this.wasm = wasm;
    this.source = source;
    this.imports = imports;
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
  public WasmRuntime.Result run(byte[] stdin, WasmRuntime.Limits limits)
      throws WasmException, InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    if (limits.deadlineEpochMs() != 0 && System.currentTimeMillis() >= limits.deadlineEpochMs()) {
      throw new WasmException(WasmException.Kind.TIMEOUT, "deadline passed before the run");
    }
    Source capped = cappedSource(limits.maxMemoryPages());
    CappedOutputStream stdout = new CappedOutputStream("stdout", limits.maxOutputBytes());
    CappedOutputStream stderr = new CappedOutputStream("stderr", limits.maxOutputBytes());
    WasiHost wasi =
        new WasiHost(stdin, stdout, stderr, limits.randomSeed(), limits.deadlineEpochMs());
    Context context = GraalWasmRuntime.contextBuilder().build();
    Watch watch = new Watch(context, Thread.currentThread(), limits.deadlineEpochMs(), stdout, stderr);
    ScheduledFuture<?> poll =
        WATCHDOG.scheduleWithFixedDelay(watch, POLL_MILLIS, POLL_MILLIS, TimeUnit.MILLISECONDS);
    int exitCode = 0;
    PolyglotException failure = null;
    try {
      Value module = context.eval(capped);
      Value[] memory = new Value[1];
      Value instance = module.newInstance(ProxyObject.fromMap(hostImports(memory, wasi)));
      Value exports = instance.getMember("exports");
      memory[0] = exports.getMember("memory");
      exports.getMember("_start").executeVoid();
    } catch (PolyglotException e) {
      if (e.isHostException() && e.asHostException() instanceof WasiHost.ProcExit exit) {
        exitCode = exit.code;
      } else {
        failure = e;
      }
    } finally {
      poll.cancel(false);
      watch.finish();
      try {
        context.close();
      } catch (PolyglotException e) {
        // Closing a context that was cancelled reports that again.
      }
    }
    boolean stopped =
        failure != null
            && failure.isHostException()
            && failure.asHostException() instanceof WasiHost.Stop;
    if (watch.interrupted || (stopped && Thread.currentThread().isInterrupted())) {
      Thread.interrupted();
      throw new InterruptedException("WebAssembly run interrupted");
    }
    if (stdout.exceeded() || stderr.exceeded()) {
      throw new WasmException(
          WasmException.Kind.OUTPUT_LIMIT,
          "output exceeded " + limits.maxOutputBytes() + " bytes",
          failure);
    }
    if (watch.timedOut || stopped || (failure != null && failure.isCancelled())) {
      throw new WasmException(WasmException.Kind.TIMEOUT, "deadline exceeded", failure);
    }
    if (failure != null) {
      // compile() accepted the module and every import is a host function, so it links; a
      // failure while instantiating is the module's own (e.g. a data segment out of bounds), a
      // trap as on Endive.
      throw new WasmException(WasmException.Kind.TRAP, "trap: " + failure.getMessage(), failure);
    }
    if (limits.deadlineEpochMs() != 0 && System.currentTimeMillis() >= limits.deadlineEpochMs()) {
      // It finished after the deadline, before the watchdog's next check: still late.
      throw new WasmException(WasmException.Kind.TIMEOUT, "deadline exceeded");
    }
    return new WasmRuntime.Result(exitCode, stdout.toByteArray(), stderr.toByteArray());
  }

  /** The module's WASI imports as host functions, each served by {@code wasi}. */
  private Map<String, Object> hostImports(Value[] memory, WasiHost wasi) {
    WasiHost.GuestMemory guest =
        new WasiHost.GuestMemory() {
          @Override
          public long size() {
            return memory().getBufferSize();
          }

          @Override
          public void read(long address, byte[] into, int offset, int length) {
            memory().readBuffer(address, into, offset, length);
          }

          @Override
          public void write(long address, byte[] from, int offset, int length) {
            // Eight bytes per polyglot call where it can.
            Value m = memory();
            ByteBuffer words = ByteBuffer.wrap(from, offset, length).order(ByteOrder.LITTLE_ENDIAN);
            int i = 0;
            for (; i + 8 <= length; i += 8) {
              m.writeBufferLong(ByteOrder.LITTLE_ENDIAN, address + i, words.getLong(offset + i));
            }
            for (; i < length; i++) {
              m.writeBufferByte(address + i, from[offset + i]);
            }
          }

          private Value memory() {
            if (memory[0] == null) {
              throw new WasiHost.Trap("memory accessed before the module was instantiated");
            }
            return memory[0];
          }
        };
    Map<String, Object> functions = new HashMap<>();
    for (String name : imports) {
      functions.put(
          name,
          (ProxyExecutable)
              args -> {
                long[] values = new long[args.length];
                for (int i = 0; i < args.length; i++) {
                  values[i] = args[i].asLong();
                }
                return wasi.call(name, values, guest);
              });
    }
    return Map.of(GraalWasmRuntime.HOST_MODULE, ProxyObject.fromMap(functions));
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
      } else if (!stdout.exceeded() && !stderr.exceeded()) {
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
