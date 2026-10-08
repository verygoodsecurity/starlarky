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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Runs WebAssembly modules for Larky's {@code @vgs//wasm}. {@link #configured()} returns the
 * runtime to use: Endive ({@link EndiveWasmRuntime}) unless {@code -Dlarky.wasm.runtime} names
 * another.
 *
 * <p>Every runtime must behave identically (the conformance tests compare them): a module is a
 * WASI preview 1 command (it exports {@code _start} and its memory). A run gets a fresh instance
 * with the input as stdin; its stdout is the result. It has no files (no preopens), no
 * environment, argv {@code ["module"]}, no network. With the default host policy,
 * {@code clock_time_get} always returns 0, and
 * {@code random_get} reads {@link Limits#randomSeed()} if set, else a {@code SecureRandom}.
 */
public interface WasmRuntime {

  /** System property naming the runtime, e.g. {@code -Dlarky.wasm.runtime=graal}. */
  String PROPERTY = "larky.wasm.runtime";

  /** The runtime used when {@link #PROPERTY} is unset. */
  String DEFAULT = "endive";

  /**
   * The largest module {@link #compile} accepts, in bytes. Preparing a module takes time in
   * proportion to it, before any run's deadline applies.
   */
  int MAX_MODULE_BYTES = 8 << 20;

  /** The most functions a module {@link #compile} accepts may define. */
  int MAX_FUNCTIONS = 20_000;

  /** The name {@link #PROPERTY} selects it by, e.g. "endive" or "graal". */
  String name();

  /**
   * Validates and prepares {@code wasm} for running. Callers cache the result by content.
   *
   * @throws WasmException of kind {@link WasmException.Kind#INVALID_MODULE} if the bytes are not
   *     a valid module, it lacks the {@code _start} or {@code memory} export, has a start
   *     section, imports anything but WASI preview 1 functions with their standard signatures, or
   *     is larger than {@link #MAX_MODULE_BYTES} or {@link #MAX_FUNCTIONS}
   */
  Program compile(byte[] wasm) throws WasmException;

  /** A compiled module. Thread-safe: each {@link #run} instantiates it afresh. */
  interface Program {

    /**
     * Instantiates the module and runs {@code _start} with {@code stdin}, on a thread of its own
     * with an 8 MiB stack: the deadline stops that thread, never the caller's, and how deeply the
     * module can nest calls does not depend on the caller's stack.
     *
     * <p>A module that returns from {@code _start} exits with 0; {@code proc_exit(n)} exits with
     * n. Both are results, not exceptions.
     *
     * @throws WasmException of kind {@link WasmException.Kind#TRAP} if execution traps, {@link
     *     WasmException.Kind#MEMORY_LIMIT} if the module's initial memory exceeds {@link
     *     Limits#maxMemoryBytes()} (growing past it makes {@code memory.grow} return -1, as the
     *     specification says), {@link WasmException.Kind#TIMEOUT} at the deadline, and {@link
     *     WasmException.Kind#OUTPUT_LIMIT} when stdout or stderr exceeds the output limit
     * @throws InterruptedException if the calling thread is interrupted (not by the deadline)
     */
    Result run(byte[] stdin, Limits limits) throws WasmException, InterruptedException;
  }

  /**
   * Host-selected WASI functions for a run. The allowlist is immutable and defaults to the
   * existing sandbox behavior. Newly annotated functions require an explicit grant.
   * Disabled errno-returning functions return NOSYS without side effects; disabled proc_exit traps.
   *
   * @param enabledFunctions names of implemented WASI preview 1 functions; unknown names and
   *     functions without a host implementation are rejected
   * @throws IllegalArgumentException if a requested function has no implementation
   */
  record WasiHostPolicy(Set<String> enabledFunctions) {
    private static final WasiHostPolicy DEFAULT = new WasiHostPolicy(Set.of(
        "args_get", "args_sizes_get", "clock_time_get", "environ_get", "environ_sizes_get",
        "fd_close", "fd_fdstat_get", "fd_prestat_dir_name", "fd_prestat_get", "fd_read",
        "fd_seek", "fd_write", "proc_exit", "random_get", "sched_yield"));

    public WasiHostPolicy {
      enabledFunctions = Set.copyOf(enabledFunctions);
      for (String name : enabledFunctions) {
        if (!WasiHost.RUNTIME_FUNCTIONS.contains(name)) {
          throw new IllegalArgumentException("no WASI host implementation: " + name);
        }
      }
    }

    public static WasiHostPolicy defaults() {
      return DEFAULT;
    }

    public static WasiHostPolicy none() {
      return new WasiHostPolicy(Set.of());
    }
  }

  /**
   * Limits for one run.
   *
   * @param maxMemoryBytes the most linear memory the instance may have (rounded down to 64 KiB
   *     pages)
   * @param deadlineEpochMs wall-clock deadline in {@link System#currentTimeMillis()} terms; 0 for
   *     none
   * @param maxOutputBytes the most bytes stdout, and separately stderr, may receive
   * @param randomSeed if not null, {@code random_get} returns a deterministic stream from this
   *     seed (for tests); otherwise it reads a {@code SecureRandom}
   * @param wasiHostPolicy functions the embedding host permits for this run
   * @throws IllegalArgumentException if a limit or the deadline is negative
   */
  record Limits(long maxMemoryBytes, long deadlineEpochMs, int maxOutputBytes, Long randomSeed,
      WasiHostPolicy wasiHostPolicy) {

    /** Existing callers retain the default WASI host policy. */
    public Limits(long maxMemoryBytes, long deadlineEpochMs, int maxOutputBytes, Long randomSeed) {
      this(maxMemoryBytes, deadlineEpochMs, maxOutputBytes, randomSeed, WasiHostPolicy.defaults());
    }

    public Limits {
      Objects.requireNonNull(wasiHostPolicy, "wasiHostPolicy");
      if (maxMemoryBytes < 0 || deadlineEpochMs < 0 || maxOutputBytes < 0) {
        throw new IllegalArgumentException(
            String.format(
                "WebAssembly limits must not be negative: maxMemoryBytes=%d, deadlineEpochMs=%d,"
                    + " maxOutputBytes=%d",
                maxMemoryBytes, deadlineEpochMs, maxOutputBytes));
      }
    }

    public static final long DEFAULT_MAX_MEMORY_BYTES = 64L << 20;
    public static final int DEFAULT_MAX_OUTPUT_BYTES = 1 << 20;

    public static Limits defaults() {
      return new Limits(DEFAULT_MAX_MEMORY_BYTES, 0, DEFAULT_MAX_OUTPUT_BYTES, null);
    }

    public long maxMemoryPages() {
      return maxMemoryBytes / 65536;
    }
  }

  /** What a run produced: its exit code, stdout and stderr. */
  record Result(int exitCode, byte[] stdout, byte[] stderr) {}

  /**
   * A failed compile or run. Runtimes word their messages differently; callers (and the
   * conformance tests) rely only on {@link #kind()}.
   */
  final class WasmException extends Exception {

    public enum Kind {
      INVALID_MODULE,
      TRAP,
      MEMORY_LIMIT,
      TIMEOUT,
      OUTPUT_LIMIT,
    }

    private final Kind kind;

    public WasmException(Kind kind, String message) {
      super(message);
      this.kind = kind;
    }

    public WasmException(Kind kind, String message, Throwable cause) {
      super(message, cause);
      this.kind = kind;
    }

    public Kind kind() {
      return kind;
    }
  }

  /** The runtime {@link #PROPERTY} names, or {@value #DEFAULT} if it is unset. */
  static WasmRuntime configured() throws WasmException {
    String name = System.getProperty(PROPERTY);
    return byName(name == null || name.isEmpty() ? DEFAULT : name);
  }

  /**
   * The runtime named {@code name}: "endive", "graal" (when GraalWasm is on the class path), or
   * one registered with {@link ServiceLoader}.
   */
  static WasmRuntime byName(String name) throws WasmException {
    switch (name) {
      case "endive":
        return new EndiveWasmRuntime();
      case "graal":
        return graal();
      default:
        for (WasmRuntime runtime : ServiceLoader.load(WasmRuntime.class, WasmRuntime.class.getClassLoader())) {
          if (runtime.name().equals(name)) {
            return runtime;
          }
        }
        throw new WasmException(
            WasmException.Kind.INVALID_MODULE, "no WebAssembly runtime named '" + name + "'");
    }
  }

  /** Endive, GraalWasm if it is on the class path, and any registered with ServiceLoader. */
  static List<WasmRuntime> all() {
    List<WasmRuntime> runtimes = new ArrayList<>();
    runtimes.add(new EndiveWasmRuntime());
    try {
      runtimes.add(graal());
    } catch (WasmException e) {
      // GraalWasm's dependencies are optional.
    }
    ServiceLoader.load(WasmRuntime.class, WasmRuntime.class.getClassLoader()).forEach(runtimes::add);
    return runtimes;
  }

  /**
   * {@link GraalWasmRuntime}, loaded by name: its GraalVM dependencies are optional, so it is
   * only linked when asked for.
   */
  private static WasmRuntime graal() throws WasmException {
    try {
      return (WasmRuntime)
          Class.forName("com.verygood.security.larky.wasm.GraalWasmRuntime")
              .getDeclaredConstructor()
              .newInstance();
    } catch (ReflectiveOperationException | LinkageError e) {
      throw new WasmException(
          WasmException.Kind.INVALID_MODULE,
          "the 'graal' WebAssembly runtime needs org.graalvm.polyglot:polyglot and"
              + " org.graalvm.polyglot:wasm-community on the class path",
          e);
    }
  }
}
