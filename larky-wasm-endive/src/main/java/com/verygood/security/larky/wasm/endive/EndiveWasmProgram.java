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

package com.verygood.security.larky.wasm.endive;

import com.verygood.security.larky.wasm.WasmException;
import com.verygood.security.larky.wasm.WasmException.Kind;
import com.verygood.security.larky.wasm.WasmLimits;
import com.verygood.security.larky.wasm.WasmProgram;
import com.verygood.security.larky.wasm.WasmResult;
import java.io.ByteArrayInputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import run.endive.runtime.HostFunction;
import run.endive.runtime.ImportFunction;
import run.endive.runtime.ImportValues;
import run.endive.runtime.Instance;
import run.endive.runtime.Machine;
import run.endive.runtime.Memory;
import run.endive.runtime.WasmInterruptedException;
import run.endive.wasi.WasiExitException;
import run.endive.wasi.WasiOptions;
import run.endive.wasi.WasiPreview1;
import run.endive.wasm.WasmModule;
import run.endive.wasm.types.MemoryLimits;

/** A parsed (and, in compiler mode, compiled) module; each run instantiates it afresh. */
final class EndiveWasmProgram implements WasmProgram {

  private static final List<String> ARGV = List.of("module");

  /** WASI errno values (Endive's WasiErrno is package-private). */
  private static final long ESUCCESS = 0;

  private static final long EINVAL = 28;

  /** Interrupts runs at their deadlines. One daemon thread serves every run. */
  private static final ScheduledThreadPoolExecutor WATCHDOG = newWatchdog();

  private final WasmModule module;
  /** The compiled machine factory, or null to interpret. */
  private final Function<Instance, Machine> machineFactory;

  private final MemoryLimits declaredMemory;

  EndiveWasmProgram(
      WasmModule module, Function<Instance, Machine> machineFactory, MemoryLimits declaredMemory) {
    this.module = module;
    this.machineFactory = machineFactory;
    this.declaredMemory = declaredMemory;
  }

  @Override
  public WasmResult run(byte[] stdin, WasmLimits limits)
      throws WasmException, InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    MemoryLimits memoryLimits = memoryLimits(limits);
    CappedOutputStream stdout = new CappedOutputStream("stdout", limits.maxOutputBytes());
    CappedOutputStream stderr = new CappedOutputStream("stderr", limits.maxOutputBytes());
    WasiOptions options =
        WasiOptions.builder()
            .withStdin(new ByteArrayInputStream(stdin), false)
            .withStdout(stdout, false)
            .withStderr(stderr, false)
            .withArguments(ARGV)
            .withThrowOnExit0(true)
            .build();

    Deadline deadline = Deadline.start(Thread.currentThread(), limits.deadlineEpochMs());
    try (WasiPreview1 wasi =
        WasiPreview1.builder().withLogger(QuietLogger.INSTANCE).withOptions(options).build()) {
      Instance.Builder builder =
          Instance.builder(module)
              .withImportValues(imports(wasi, limits.randomSeed()))
              .withMemoryLimits(memoryLimits)
              .withStart(false);
      if (machineFactory != null) {
        builder.withMachineFactory(machineFactory);
      }
      int exitCode;
      try {
        Instance instance = builder.build();
        instance.export("_start").apply();
        exitCode = 0;
      } catch (WasiExitException e) {
        exitCode = e.exitCode();
      } catch (RuntimeException | StackOverflowError e) {
        throw failure(e, deadline, stdout, stderr);
      }
      if (stdout.exceeded() || stderr.exceeded()) {
        // In case a WASI call swallowed OutputLimitExceeded instead of letting it unwind.
        throw new WasmException(Kind.OUTPUT_LIMIT, "output exceeded " + limits.maxOutputBytes());
      }
      return new WasmResult(exitCode, stdout.toByteArray(), stderr.toByteArray());
    } finally {
      deadline.finish();
    }
  }

  /** An instance with no WASI state, for tests to inspect. */
  Instance newInstanceForTest() {
    try (WasiPreview1 wasi =
        WasiPreview1.builder()
            .withLogger(QuietLogger.INSTANCE)
            .withOptions(WasiOptions.builder().build())
            .build()) {
      Instance.Builder builder =
          Instance.builder(module).withImportValues(imports(wasi, 0L)).withStart(false);
      if (machineFactory != null) {
        builder.withMachineFactory(machineFactory);
      }
      return builder.build();
    }
  }

  /** The module's own memory limits, with the maximum lowered to the run's cap. */
  private MemoryLimits memoryLimits(WasmLimits limits) throws WasmException {
    long cap = Math.min(limits.maxMemoryPages(), MemoryLimits.MAX_PAGES);
    if (declaredMemory.initialPages() > cap) {
      throw new WasmException(
          Kind.MEMORY_LIMIT,
          "module needs "
              + declaredMemory.initialPages()
              + " pages of memory; the limit is "
              + cap);
    }
    int maximum = (int) Math.min(declaredMemory.maximumPages(), cap);
    return new MemoryLimits(declaredMemory.initialPages(), maximum, declaredMemory.shared());
  }

  private static WasmException failure(
      Throwable e, Deadline deadline, CappedOutputStream stdout, CappedOutputStream stderr)
      throws InterruptedException {
    if (deadline.fired()) {
      return new WasmException(Kind.TIMEOUT, "WebAssembly run exceeded its deadline", e);
    }
    if (stdout.exceeded()
        || stderr.exceeded()
        || has(e, CappedOutputStream.OutputLimitExceeded.class)) {
      return new WasmException(Kind.OUTPUT_LIMIT, String.valueOf(e.getMessage()), e);
    }
    // Endive raises WasmInterruptedException when it sees the interrupt flag; a WASI call that
    // sleeps (poll_oneoff) wraps the InterruptedException instead, which clears the flag.
    if (Thread.interrupted()
        || e instanceof WasmInterruptedException
        || has(e, InterruptedException.class)) {
      InterruptedException interrupted = new InterruptedException("WebAssembly run interrupted");
      interrupted.initCause(e);
      throw interrupted;
    }
    String message = e instanceof StackOverflowError ? "call stack exhausted" : e.getMessage();
    return new WasmException(Kind.TRAP, "WebAssembly trap: " + message, e);
  }

  private static boolean has(Throwable e, Class<? extends Throwable> type) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (type.isInstance(t)) {
        return true;
      }
      if (t.getCause() == t) {
        break;
      }
    }
    return false;
  }

  /**
   * Endive's WASI functions, with {@code clock_time_get} returning 0 and {@code random_get}
   * reading {@link DeterministicBytes} or a {@code SecureRandom}.
   */
  private static ImportValues imports(WasiPreview1 wasi, Long seed) {
    List<ImportFunction> functions = new ArrayList<>();
    RandomBytes random = seed != null ? new DeterministicBytes(seed) : new SecureBytes();
    for (HostFunction f : wasi.toHostFunctions()) {
      switch (f.name()) {
        case "clock_time_get" ->
            functions.add(
                new HostFunction(
                    f.module(),
                    f.name(),
                    f.functionType(),
                    (instance, args) -> {
                      instance.memory().writeLong((int) args[2], 0L);
                      return new long[] {ESUCCESS};
                    }));
        case "random_get" ->
            functions.add(
                new HostFunction(
                    f.module(),
                    f.name(),
                    f.functionType(),
                    (instance, args) -> {
                      int len = (int) args[1];
                      if (len < 0) {
                        return new long[] {EINVAL};
                      }
                      Memory memory = instance.memory();
                      byte[] bytes = new byte[len];
                      random.fill(bytes);
                      memory.write((int) args[0], bytes);
                      return new long[] {ESUCCESS};
                    }));
        default -> functions.add(f);
      }
    }
    return ImportValues.builder().withFunctions(functions).build();
  }

  interface RandomBytes {
    void fill(byte[] bytes);
  }

  /**
   * {@code random_get} with a seed: the bytes of {@code new SplittableRandom(seed).nextLong()},
   * least significant byte first, read as one continuous stream across calls (so two calls of 4
   * bytes return the same 8 bytes as one call of 8).
   */
  static final class DeterministicBytes implements RandomBytes {
    private final SplittableRandom random;
    private long current;
    private int left;

    DeterministicBytes(long seed) {
      this.random = new SplittableRandom(seed);
    }

    @Override
    public void fill(byte[] bytes) {
      for (int i = 0; i < bytes.length; i++) {
        if (left == 0) {
          current = random.nextLong();
          left = 8;
        }
        bytes[i] = (byte) current;
        current >>>= 8;
        left--;
      }
    }
  }

  static final class SecureBytes implements RandomBytes {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void fill(byte[] bytes) {
      RANDOM.nextBytes(bytes);
    }
  }

  /**
   * Interrupts the running thread at the deadline. {@link #finish} makes sure an interrupt meant
   * for this run never outlives it.
   */
  static final class Deadline {
    private static final int RUNNING = 0;
    private static final int FINISHED = 1;
    private static final int FIRED = 2;
    private static final Deadline NONE = new Deadline(null);

    private final Thread thread;
    private final AtomicInteger state = new AtomicInteger(RUNNING);
    private volatile ScheduledFuture<?> task;
    /** Set once the watchdog has called interrupt(). */
    private volatile boolean delivered;

    private Deadline(Thread thread) {
      this.thread = thread;
    }

    static Deadline start(Thread thread, long deadlineEpochMs) {
      if (deadlineEpochMs == 0) {
        return NONE;
      }
      Deadline deadline = new Deadline(thread);
      long delay = deadlineEpochMs - System.currentTimeMillis();
      if (delay <= 0) {
        deadline.fire();
      } else {
        deadline.task = WATCHDOG.schedule(deadline::fire, delay, TimeUnit.MILLISECONDS);
      }
      return deadline;
    }

    private void fire() {
      if (state.compareAndSet(RUNNING, FIRED)) {
        thread.interrupt();
        delivered = true;
      }
    }

    boolean fired() {
      return thread != null && state.get() == FIRED;
    }

    /** Stops the watchdog; if it fired, waits for its interrupt and clears it. */
    void finish() {
      if (thread == null) {
        return;
      }
      if (state.compareAndSet(RUNNING, FINISHED)) {
        ScheduledFuture<?> t = task;
        if (t != null) {
          t.cancel(false);
        }
        return;
      }
      while (!delivered) {
        Thread.onSpinWait();
      }
      Thread.interrupted();
    }
  }

  private static ScheduledThreadPoolExecutor newWatchdog() {
    ScheduledThreadPoolExecutor executor =
        new ScheduledThreadPoolExecutor(
            1,
            r -> {
              Thread t = new Thread(r, "larky-wasm-endive-deadline");
              t.setDaemon(true);
              return t;
            });
    executor.setRemoveOnCancelPolicy(true);
    return executor;
  }
}
