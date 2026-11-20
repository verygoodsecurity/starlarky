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
import com.verygood.security.larky.wasm.WasmRuntime.WasmException.Kind;
import java.util.ArrayList;
import java.util.List;
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
import run.endive.runtime.TableInstance;
import run.endive.runtime.WasmInterruptedException;
import run.endive.wasm.WasmModule;
import run.endive.wasm.types.FunctionImport;
import run.endive.wasm.types.FunctionType;
import run.endive.wasm.types.MemoryLimits;
import run.endive.wasm.types.Table;
import run.endive.wasm.types.TableLimits;

/** A parsed (and, in compiler mode, compiled) module; each run instantiates it afresh. */
final class EndiveWasmProgram implements WasmRuntime.Program {

  private static final long PAGE_SIZE = 65536;

  /** Interrupts runs at their deadlines. One daemon thread serves every run. */
  private static final ScheduledThreadPoolExecutor WATCHDOG = newWatchdog();

  private final WasmModule module;
  /** The compiled machine factory, or null to interpret. */
  private final Function<Instance, Machine> machineFactory;

  private final MemoryLimits declaredMemory;
  private final long[] tableMaxima;

  EndiveWasmProgram(
      WasmModule module, Function<Instance, Machine> machineFactory, MemoryLimits declaredMemory,
      long[] tableMaxima) {
    this.module = module;
    this.machineFactory = machineFactory;
    this.declaredMemory = declaredMemory;
    this.tableMaxima = tableMaxima.clone();
  }

  @Override
  public WasmRuntime.Result run(byte[] stdin, WasmRuntime.Limits limits)
      throws WasmException, InterruptedException {
    return GuestThreads.run(() -> {
      var io = newIo(stdin, limits);
      var builder = instanceBuilder(memoryLimits(limits), imports(io.wasi()::bind, null, 0));
      return execute(io, limits, () -> builder.build().export("_start").apply());
    }, limits.deadlineEpochMs());
  }

  BrowserWasmRuntime.Reactor openBrowser(WasmRuntime.Limits limits,
      BrowserWasmRuntime.Session browser) throws WasmException {
    return new BrowserReactor(limits, browser, memoryLimits(limits));
  }

  private record GuestIo(WasiHost wasi, CappedOutputStream stdout, CappedOutputStream stderr) {}

  private static GuestIo newIo(byte[] stdin, WasmRuntime.Limits limits) {
    var stdout = new CappedOutputStream("stdout", limits.maxOutputBytes());
    var stderr = new CappedOutputStream("stderr", limits.maxOutputBytes());
    var wasi = new WasiHost(stdin, stdout, stderr, limits.randomSeed(), limits.deadlineEpochMs(),
        limits.wasiHostPolicy());
    return new GuestIo(wasi, stdout, stderr);
  }

  private Instance.Builder instanceBuilder(MemoryLimits memory, ImportValues imports) {
    int[] tableIndex = {0};
    var builder = Instance.builder(module)
        .withImportValues(imports)
        .withTableFactory((table, initial) ->
            freshTable(table, initial, tableMaxima[tableIndex[0]++]))
        .withMemoryLimits(memory)
        .withStart(false);
    if (machineFactory != null) {
      builder.withMachineFactory(machineFactory);
    }
    return builder;
  }

  private static WasmRuntime.Result execute(GuestIo io, WasmRuntime.Limits limits, Runnable action)
      throws WasmException, InterruptedException {
    Deadline deadline = Deadline.start(Thread.currentThread(), limits.deadlineEpochMs());
    int exitCode;
    try {
      action.run();
      exitCode = 0;
    } catch (WasiHost.ProcExit e) {
      exitCode = e.code;
    } catch (RuntimeException | StackOverflowError e) {
      // Stop the watchdog first, so an interrupt seen from here on is not the deadline's.
      deadline.finish();
      throw failure(e, deadline, io.stdout(), io.stderr());
    } finally {
      deadline.finish();
    }
    return result(exitCode, deadline, io.stdout(), io.stderr(), limits.maxOutputBytes());
  }

  /** Keeps memory and globals while attaching fresh WASI streams to each call. */
  private final class BrowserReactor implements BrowserWasmRuntime.Reactor {
    private final WasmRuntime.Limits limits;
    private final BrowserWasmRuntime.Session browser;
    private final MemoryLimits memory;
    private Instance instance;
    private boolean closed;
    // Only accessed on the guest thread while a step is active.
    private WasiHost activeWasi;

    BrowserReactor(WasmRuntime.Limits limits, BrowserWasmRuntime.Session browser,
        MemoryLimits memory) {
      this.limits = limits;
      this.browser = browser;
      this.memory = memory;
    }

    @Override
    public synchronized WasmRuntime.Result step(byte[] input)
        throws WasmException, InterruptedException {
      if (closed) {
        throw new IllegalStateException("browser guest is closed");
      }
      Instance previous = instance;
      Instance[] completed = {null};
      try {
        WasmRuntime.Result output = GuestThreads.run(
            () -> stepOnGuestThread(previous, input, completed), limits.deadlineEpochMs());
        instance = completed[0];
        if (instance == null) {
          close(); // A guest that called proc_exit is done, including exit code 0.
        }
        return output;
      } catch (WasmException | InterruptedException | RuntimeException failure) {
        close();
        throw failure;
      }
    }

    private WasmRuntime.Result stepOnGuestThread(Instance previous, byte[] input,
        Instance[] completed) throws WasmException, InterruptedException {
      var io = newIo(input, limits);
      activeWasi = io.wasi();
      try {
        return execute(io, limits, () -> {
          Instance current = previous;
          if (current == null) {
            var imports = imports(name -> (args, guest) -> activeWasi.bind(name).call(args, guest),
                browser, limits.deadlineEpochMs());
            current = instanceBuilder(memory, imports).build();
            current.export(BrowserWasmRuntime.INITIALIZE).apply();
          }
          current.export(BrowserWasmRuntime.STEP).apply();
          completed[0] = current;
        });
      } finally {
        activeWasi = null;
      }
    }

    @Override
    public synchronized void close() {
      closed = true;
      instance = null;
    }
  }

  /**
   * The result of a run that returned or exited, once its {@link Deadline} is finished: a run
   * that overran its output limit or deadline still fails, and an interrupt that arrived after the
   * guest's last interrupt check is still an {@link InterruptedException}.
   */
  static WasmRuntime.Result result(
      int exitCode,
      Deadline deadline,
      CappedOutputStream stdout,
      CappedOutputStream stderr,
      int maxOutputBytes)
      throws WasmException, InterruptedException {
    if (stdout.exceeded() || stderr.exceeded()) {
      // In case a WASI call swallowed OutputLimitExceeded instead of letting it unwind.
      throw new WasmException(Kind.OUTPUT_LIMIT, "output exceeded " + maxOutputBytes);
    }
    if (deadline.fired() || deadline.passed()) {
      // A late finish is still late, even if the watchdog had not fired yet.
      throw new WasmException(Kind.TIMEOUT, "WebAssembly run exceeded its deadline");
    }
    if (Thread.interrupted()) {
      throw new InterruptedException("WebAssembly run interrupted");
    }
    return new WasmRuntime.Result(exitCode, stdout.toByteArray(), stderr.toByteArray());
  }

  /** An instance with empty stdin and no room for output, for tests to inspect. */
  Instance newInstanceForTest() {
    WasiHost wasi =
        new WasiHost(
            new byte[0],
            new CappedOutputStream("stdout", 0),
            new CappedOutputStream("stderr", 0),
            0L,
            0,
            WasmRuntime.WasiHostPolicy.defaults());
    int[] tableIndex = {0};
    Instance.Builder builder =
        Instance.builder(module)
            .withImportValues(imports(wasi::bind, null, 0))
            .withTableFactory((table, initial) ->
                freshTable(table, initial, tableMaxima[tableIndex[0]++]))
            .withStart(false);
    if (machineFactory != null) {
      builder.withMachineFactory(machineFactory);
    }
    return builder.build();
  }

  /** Endive mutates TableLimits on growth; never share those limits with the cached module. */
  private static TableInstance freshTable(Table table, int initialValue, long maximum) {
    TableLimits limits = table.limits();
    Table copy = new Table(table.elementType(),
        new TableLimits(limits.min(), maximum, limits.shared()), table.initialize());
    return new TableInstance(copy, initialValue);
  }

  /** The module's own memory limits, with the maximum lowered to the run's cap. */
  private MemoryLimits memoryLimits(WasmRuntime.Limits limits) throws WasmException {
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

  /** Classifies a run that threw, once its {@link Deadline} is finished. */
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
    if (has(e, WasiHost.Stop.class) && !Thread.currentThread().isInterrupted()) {
      // A WASI call saw the deadline pass before the watchdog interrupted the thread.
      return new WasmException(Kind.TIMEOUT, "WebAssembly run exceeded its deadline", e);
    }
    // Endive raises WasmInterruptedException when it sees the interrupt flag.
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

  private ImportValues imports(Function<String, WasiHost.Function> wasi, BrowserWasmRuntime.Session browser,
      long deadlineEpochMs) {
    List<ImportFunction> functions = new ArrayList<>();
    var imports = module.importSection();
    for (int i = 0; i < imports.importCount(); i++) {
      // EndiveWasmRuntime has verified the import's module, name, and signature.
      FunctionImport imp = (FunctionImport) imports.getImport(i);
      String name = imp.name();
      FunctionType type = module.typeSection().getType(imp.typeIndex());
      boolean returnsErrno = !type.returns().isEmpty();
      WasiHost.Function function = imp.module().equals(WasiHost.MODULE)
          ? wasi.apply(name)
          : browser.bind(name, deadlineEpochMs);
      functions.add(
          new HostFunction(
              imp.module(),
              name,
              type,
              (instance, args) -> {
                int errno = function.call(args, memory(instance));
                return returnsErrno ? new long[] {errno} : new long[0];
              }));
    }
    return ImportValues.builder().withFunctions(functions).build();
  }

  /** {@code instance}'s memory as {@link WasiHost} reads and writes it. */
  private static WasiHost.GuestMemory memory(Instance instance) {
    Memory memory = instance.memory();
    return new WasiHost.GuestMemory() {
      @Override
      public long size() {
        return memory.pages() * PAGE_SIZE;
      }

      @Override
      public void read(long address, byte[] into, int offset, int length) {
        System.arraycopy(memory.readBytes((int) address, length), 0, into, offset, length);
      }

      @Override
      public void write(long address, byte[] from, int offset, int length) {
        memory.write((int) address, from, offset, length);
      }
    };
  }

  /**
   * Interrupts the running thread (a {@link GuestThreads} thread) at the deadline. {@link #finish}
   * makes sure an interrupt meant for this run never outlives it.
   */
  static final class Deadline {
    private static final int RUNNING = 0;
    private static final int FINISHED = 1;
    private static final int FIRED = 2;
    static final Deadline NONE = new Deadline(null, 0);

    private final Thread thread;
    private final long epochMs;
    private final AtomicInteger state = new AtomicInteger(RUNNING);
    private volatile ScheduledFuture<?> task;
    /** Set once the watchdog has called interrupt(). */
    private volatile boolean delivered;
    /** Set by the running thread's first {@link #finish}. */
    private boolean finished;

    /** A deadline that has not been scheduled; {@link #start} schedules one. */
    Deadline(Thread thread, long epochMs) {
      this.thread = thread;
      this.epochMs = epochMs;
    }

    static Deadline start(Thread thread, long deadlineEpochMs) {
      if (deadlineEpochMs == 0) {
        return NONE;
      }
      Deadline deadline = new Deadline(thread, deadlineEpochMs);
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

    /** Whether the watchdog interrupted the thread. */
    boolean fired() {
      return thread != null && state.get() == FIRED;
    }

    /** Whether the deadline has passed, whether or not the watchdog has fired yet. */
    boolean passed() {
      return epochMs != 0 && System.currentTimeMillis() >= epochMs;
    }

    /**
     * Stops the watchdog; if it fired, waits for its interrupt and clears it. Called by the
     * running thread; later calls do nothing.
     */
    void finish() {
      if (thread == null || finished) {
        return;
      }
      finished = true;
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
