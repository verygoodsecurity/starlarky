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

package com.verygood.security.larky.wasm.conformance;

import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import com.verygood.security.larky.wasm.WasmRuntime;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import run.endive.wabt.Wat2Wasm;

/**
 * The modules the conformance tests run. Each WAT fixture in {@code src/test/resources/wasm} is the
 * source of truth and is compiled here with Endive's wabt port.
 */
public final class Fixtures {

  /** The WAT fixtures, by file name without {@code .wat}. */
  static final List<String> WAT =
      List.of(
          "echo", "exit3", "trap", "spin", "grow", "bigmem", "flood", "random", "random_split",
          "random_at", "random_fill", "clock", "env", "fresh", "nostart", "nomemory", "nosys",
          "fds", "startsection", "badsig");

  private static final Map<String, byte[]> CACHE = new ConcurrentHashMap<>();

  private Fixtures() {}

  /** The bytes of fixture {@code name}: a compiled WAT file or a committed {@code .wasm}. */
  public static byte[] wasm(String name) {
    return CACHE.computeIfAbsent(name, Fixtures::load).clone();
  }

  private static byte[] load(String name) {
    byte[] wasm = resource("/wasm/" + name + ".wasm");
    if (wasm != null) {
      return wasm;
    }
    byte[] wat = resource("/wasm/" + name + ".wat");
    if (wat == null) {
      throw new IllegalArgumentException("no fixture wasm/" + name + ".{wasm,wat}");
    }
    return Wat2Wasm.parse(new String(wat, StandardCharsets.UTF_8));
  }

  private static byte[] resource(String path) {
    try (InputStream in = Fixtures.class.getResourceAsStream(path)) {
      return in == null ? null : in.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Every runtime {@link WasmRuntime#all()} finds, as JUnit parameters {@code {name, runtime}}.
   *
   * @throws IllegalStateException if GraalWasm is missing, so a class path without it fails
   *     loudly instead of testing only Endive
   */
  static List<Object[]> runtimeParameters() {
    List<WasmRuntime> runtimes = WasmRuntime.all();
    if (runtimes.size() < 2) {
      throw new IllegalStateException(
          "expected Endive and GraalWasm; found " + runtimes.size()
              + " runtime(s). GraalWasm's optional dependencies must be on the test class path");
    }
    List<Object[]> parameters = new ArrayList<>();
    for (WasmRuntime runtime : runtimes) {
      parameters.add(new Object[] {runtime.name(), runtime});
    }
    return parameters;
  }

  /** Limits with the defaults except for the deadline, {@code millis} from now. */
  static WasmRuntime.Limits deadlineIn(long millis) {
    WasmRuntime.Limits d = WasmRuntime.Limits.defaults();
    return new WasmRuntime.Limits(
        d.maxMemoryBytes(), System.currentTimeMillis() + millis, d.maxOutputBytes(), d.randomSeed());
  }

  /** Limits with the defaults except for the random seed. */
  static WasmRuntime.Limits seeded(long seed) {
    WasmRuntime.Limits d = WasmRuntime.Limits.defaults();
    return new WasmRuntime.Limits(d.maxMemoryBytes(), d.deadlineEpochMs(), d.maxOutputBytes(), seed);
  }

  /** Limits with the defaults except for the memory cap. */
  static WasmRuntime.Limits memoryCap(long bytes) {
    WasmRuntime.Limits d = WasmRuntime.Limits.defaults();
    return new WasmRuntime.Limits(bytes, d.deadlineEpochMs(), d.maxOutputBytes(), d.randomSeed());
  }

  /** Limits with the defaults except for the output cap. */
  static WasmRuntime.Limits outputCap(int bytes) {
    WasmRuntime.Limits d = WasmRuntime.Limits.defaults();
    return new WasmRuntime.Limits(d.maxMemoryBytes(), d.deadlineEpochMs(), bytes, d.randomSeed());
  }

  /** Compiles and runs fixture {@code name} on {@code runtime}. */
  static WasmRuntime.Result run(WasmRuntime runtime, String name, byte[] stdin, WasmRuntime.Limits limits)
      throws WasmException, InterruptedException {
    return runtime.compile(wasm(name)).run(stdin, limits);
  }

  static byte[] utf8(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }

  static String utf8(byte[] b) {
    return new String(b, StandardCharsets.UTF_8);
  }
}
