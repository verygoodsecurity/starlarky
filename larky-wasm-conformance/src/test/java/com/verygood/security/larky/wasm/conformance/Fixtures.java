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

import com.verygood.security.larky.wasm.WasmException;
import com.verygood.security.larky.wasm.WasmLimits;
import com.verygood.security.larky.wasm.WasmResult;
import com.verygood.security.larky.wasm.WasmRuntime;
import com.verygood.security.larky.wasm.WasmRuntimes;
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
 * source of truth and is compiled here with Endive's wabt port; {@code js/sample_encrypt.wasm} is
 * built from {@code js/sample_encrypt.js} by {@code build-fixtures.sh} (Javy) and committed.
 */
final class Fixtures {

  /** The WAT fixtures, by file name without {@code .wat}. */
  static final List<String> WAT =
      List.of(
          "echo", "exit3", "trap", "spin", "grow", "bigmem", "flood", "random", "random_split",
          "clock", "env", "fresh", "nostart", "nomemory");

  /** The Javy-built JavaScript fixture. */
  static final String SAMPLE_ENCRYPT = "js/sample_encrypt";

  /**
   * Inputs to {@code sample_encrypt} and the stdout recorded for them (checked under Node's WASI
   * and against the same JavaScript run directly in V8).
   */
  static final Map<String, String> SAMPLE_ENCRYPT_CASES =
      Map.of(
          "{\"pan\": \"4111111111111111\", \"key\": \"k1\"}",
          "{\"encrypted\":\"5317929663681111\",\"keyId\":\"983d80c1\"}",
          "{\"pan\": \"5500-0000-0000-0004\", \"key\": \"vendor-key-2026\"}",
          "{\"encrypted\":\"6107-6120-6978-0004\",\"keyId\":\"b5c8e57e\"}",
          "{\"pan\": \"4111\", \"key\": \"k1\"}",
          "{\"error\":\"pan must have 12 to 19 digits; got 4\"}");

  private static final Map<String, byte[]> CACHE = new ConcurrentHashMap<>();

  private Fixtures() {}

  /** The bytes of fixture {@code name}: a compiled WAT file or a committed {@code .wasm}. */
  static byte[] wasm(String name) {
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
   * Every runtime {@link WasmRuntimes#all()} finds, as JUnit parameters {@code {name, runtime}}.
   *
   * @throws IllegalStateException if there is none, so a class path without runtimes fails loudly
   *     instead of passing with zero tests
   */
  static List<Object[]> runtimeParameters() {
    List<WasmRuntime> runtimes = WasmRuntimes.all();
    if (runtimes.isEmpty()) {
      throw new IllegalStateException(
          "no WasmRuntime is registered with ServiceLoader"
              + " (META-INF/services/com.verygood.security.larky.wasm.WasmRuntime);"
              + " larky-wasm-endive and larky-wasm-graal should each provide one");
    }
    List<Object[]> parameters = new ArrayList<>();
    for (WasmRuntime runtime : runtimes) {
      parameters.add(new Object[] {runtime.name(), runtime});
    }
    return parameters;
  }

  /** Limits with the defaults except for the deadline, {@code millis} from now. */
  static WasmLimits deadlineIn(long millis) {
    WasmLimits d = WasmLimits.defaults();
    return new WasmLimits(
        d.maxMemoryBytes(), System.currentTimeMillis() + millis, d.maxOutputBytes(), d.randomSeed());
  }

  /** Limits with the defaults except for the random seed. */
  static WasmLimits seeded(long seed) {
    WasmLimits d = WasmLimits.defaults();
    return new WasmLimits(d.maxMemoryBytes(), d.deadlineEpochMs(), d.maxOutputBytes(), seed);
  }

  /** Limits with the defaults except for the memory cap. */
  static WasmLimits memoryCap(long bytes) {
    WasmLimits d = WasmLimits.defaults();
    return new WasmLimits(bytes, d.deadlineEpochMs(), d.maxOutputBytes(), d.randomSeed());
  }

  /** Limits with the defaults except for the output cap. */
  static WasmLimits outputCap(int bytes) {
    WasmLimits d = WasmLimits.defaults();
    return new WasmLimits(d.maxMemoryBytes(), d.deadlineEpochMs(), bytes, d.randomSeed());
  }

  /** Compiles and runs fixture {@code name} on {@code runtime}. */
  static WasmResult run(WasmRuntime runtime, String name, byte[] stdin, WasmLimits limits)
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
