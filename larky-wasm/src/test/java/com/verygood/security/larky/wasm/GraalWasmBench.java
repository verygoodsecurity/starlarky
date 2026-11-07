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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Compile and run latency. Runs only with {@code -Dlarky.wasm.bench=true}. */
@RunWith(JUnit4.class)
public final class GraalWasmBench {

  static final int WARMUP = 100;
  static final int RUNS = 300;

  /** {@code wasm} plus a custom section holding {@code n}, so each copy is a new Source. */
  static byte[] unique(byte[] wasm, int n) {
    byte[] payload = {1, 'n', (byte) n, (byte) (n >>> 8), (byte) (n >>> 16)};
    byte[] out = Arrays.copyOf(wasm, wasm.length + 2 + payload.length);
    out[wasm.length] = 0;
    out[wasm.length + 1] = (byte) payload.length;
    System.arraycopy(payload, 0, out, wasm.length + 2, payload.length);
    return out;
  }

  static String stats(long[] nanos) {
    long[] sorted = nanos.clone();
    Arrays.sort(sorted);
    return String.format(
        "n=%d median=%.3f ms p10=%.3f ms p90=%.3f ms",
        sorted.length,
        sorted[sorted.length / 2] / 1e6,
        sorted[sorted.length / 10] / 1e6,
        sorted[sorted.length * 9 / 10] / 1e6);
  }

  @Test
  public void bench() throws Exception {
    assumeTrue(Boolean.getBoolean("larky.wasm.bench"));
    GraalWasmRuntime runtime = new GraalWasmRuntime();
    System.out.println(
        "engine implementation: " + GraalWasmRuntime.engine().getImplementationName()
            + " java " + Runtime.version());
    byte[] echo = GraalWasmRuntimeTest.wat(GraalWasmRuntimeTest.ECHO);
    long[] compile = new long[RUNS];
    for (int i = 0; i < WARMUP + RUNS; i++) {
      byte[] wasm = unique(echo, i);
      long t0 = System.nanoTime();
      runtime.compile(wasm);
      long t = System.nanoTime() - t0;
      if (i >= WARMUP) {
        compile[i - WARMUP] = t;
      }
    }
    System.out.println("compile (new module each time): " + stats(compile));
    long[] recompile = new long[RUNS];
    for (int i = 0; i < RUNS; i++) {
      long t0 = System.nanoTime();
      runtime.compile(echo);
      recompile[i] = System.nanoTime() - t0;
    }
    System.out.println("compile (same bytes, engine cache): " + stats(recompile));
    WasmRuntime.Program program = runtime.compile(echo);
    byte[] input = "hello, wasm\n".getBytes(UTF_8);
    WasmRuntime.Limits limits = WasmRuntime.Limits.defaults();
    long[] run = new long[RUNS];
    for (int i = 0; i < WARMUP + RUNS; i++) {
      long t0 = System.nanoTime();
      program.run(input, limits);
      long t = System.nanoTime() - t0;
      if (i >= WARMUP) {
        run[i - WARMUP] = t;
      }
    }
    System.out.println("run echo (12-byte stdin): " + stats(run));
  }
}
