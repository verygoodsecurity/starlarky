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

import com.verygood.security.larky.wasm.WasmRuntime.WasiHostPolicy;
import java.util.Map;
import java.util.Objects;

/**
 * Binds Larky's WASI preview 1 functions for one run. Runtime adapters supply guest memory
 * and bind each import once; the annotated {@link WasiHostModule} owns the run's state.
 *
 * <p>The module sees stdin (fd 0), stdout (1) and stderr (2), argv {@code ["module"]}, an empty
 * environment, no preopened directories, a clock that reads 0, and random bytes from a seed or
 * {@code SecureRandom}. {@link #RUNTIME_FUNCTIONS} lists the implementations. The per-run policy
 * selects which may execute; other errno-returning WASI functions return {@link #ERRNO_NOSYS},
 * while a disabled {@code proc_exit} traps.
 *
 * <p>Every guest pointer and length is a u32 checked against the guest's memory before it is
 * used; a bad one traps ({@link Trap}). Nothing is allocated in proportion to a guest-chosen
 * length, and a call that loops over guest data stops at the run's deadline or an interrupt
 * ({@link Stop}).
 */
final class WasiHost {

  static final String MODULE = "wasi_snapshot_preview1";

  static final int ERRNO_SUCCESS = 0;
  static final int ERRNO_BADF = 8;
  static final int ERRNO_INVAL = 28;
  static final int ERRNO_NOSYS = 52;
  static final int ERRNO_SPIPE = 70;

  private static final Map<String, WasiHostRegistry.Descriptor> FUNCTIONS =
      WasiHostRegistry.discover(WasiHostModule.class);

  /** Complete ABI catalog, derived solely from the module's annotated declarations. */
  static final Map<String, String> SIGNATURES = FUNCTIONS.entrySet().stream()
      .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,
          entry -> entry.getValue().signature()));

  /** Implemented declarations only; discovery does not authorize their execution. */
  static final java.util.Set<String> RUNTIME_FUNCTIONS = FUNCTIONS.entrySet().stream()
      .filter(entry -> entry.getValue().implemented())
      .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toUnmodifiableSet());

  /** A guest's linear memory, as each runtime exposes it. Addresses are within {@link #size}. */
  interface GuestMemory {
    long size();

    void read(long address, byte[] into, int offset, int length);

    void write(long address, byte[] from, int offset, int length);
  }

  /** Thrown by {@code proc_exit} to unwind the guest; the run returns {@link #code}. */
  static final class ProcExit extends RuntimeException {
    final int code;

    ProcExit(int code) {
      super(null, null, false, false);
      this.code = code;
    }
  }

  /** A pointer or length outside the guest's memory: the run traps, as a guest access would. */
  static final class Trap extends RuntimeException {
    Trap(String message) {
      super(message, null, false, false);
    }
  }

  /** Thrown when the run's deadline passes or its caller is interrupted inside a call. */
  static final class Stop extends RuntimeException {
    Stop() {
      super(null, null, false, false);
    }
  }

  @FunctionalInterface
  interface Function {
    int call(long[] args, GuestMemory memory);
  }

  private final Map<String, Function> functions;

  WasiHost(byte[] stdin, CappedOutputStream stdout, CappedOutputStream stderr,
      Long randomSeed, long deadlineEpochMs, WasiHostPolicy policy) {
    Objects.requireNonNull(policy, "policy");
    WasiHostModule module = new WasiHostModule(stdin, stdout, stderr, randomSeed, deadlineEpochMs);
    Map<String, Function> bound = new java.util.HashMap<>();
    FUNCTIONS.forEach((name, descriptor) -> {
      if (policy.enabledFunctions().contains(name)) {
        bound.put(name, descriptor.bind(module));
      }
    });
    functions = Map.copyOf(bound);
  }

  /** Known but unsupported or disabled imports bind to a stub, never runtime-provided WASI. */
  Function bind(String name) {
    String signature = SIGNATURES.get(name);
    if (signature == null) {
      throw new IllegalArgumentException("not a WASI preview 1 function: " + name);
    }
    Function enabled = functions.get(name);
    if (enabled != null) {
      return enabled;
    }
    if (signature.endsWith(":")) {
      // proc_exit has no errno result; returning would violate its non-returning contract.
      return (args, memory) -> {
        throw new Trap("WASI function disabled: " + name);
      };
    }
    return (args, memory) -> ERRNO_NOSYS;
  }

  /** Convenience for callers that have not prebound their imports. */
  int call(String name, long[] args, GuestMemory memory) {
    return bind(name).call(args, memory);
  }
}
