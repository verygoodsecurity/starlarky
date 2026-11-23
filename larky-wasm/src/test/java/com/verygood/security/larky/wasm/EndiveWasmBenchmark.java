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

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import org.junit.Test;
import run.endive.wabt.Wat2Wasm;

/**
 * Prints compile and run latencies for each mode. Runs only with
 * {@code -Dlarky.wasm.endive.bench=true}.
 */
public class EndiveWasmBenchmark {

  private static final int WARMUP = 300;
  private static final int RUNS = 500;

  @Test
  public void measure() throws Exception {
    assumeTrue(Boolean.getBoolean("larky.wasm.endive.bench"));
    byte[] echo = Wat2Wasm.parse(EndiveWasmRuntimeTest.ECHO);
    byte[] input = "{\"card\":\"4111111111111111\",\"cvv\":\"123\"}".getBytes(UTF_8);
    for (EndiveWasmRuntime.Mode mode : EndiveWasmRuntime.Mode.values()) {
      EndiveWasmRuntime runtime = new EndiveWasmRuntime(mode);
      long[] compile = new long[RUNS];
      for (int i = -WARMUP; i < RUNS; i++) {
        long t0 = System.nanoTime();
        runtime.compile(echo);
        long t1 = System.nanoTime();
        if (i >= 0) {
          compile[i] = t1 - t0;
        }
      }
      WasmRuntime.Program program = runtime.compile(echo);
      WasmRuntime.Limits limits = WasmRuntime.Limits.defaults();
      long[] run = new long[RUNS];
      long[] runDeadline = new long[RUNS];
      for (int i = -WARMUP; i < RUNS; i++) {
        long t0 = System.nanoTime();
        byte[] out = program.run(input, limits).stdout();
        long t1 = System.nanoTime();
        program.run(input, new WasmRuntime.Limits(64L << 20, System.currentTimeMillis() + 10_000, 1 << 20, null));
        long t2 = System.nanoTime();
        assertThat(out).isEqualTo(input);
        if (i >= 0) {
          run[i] = t1 - t0;
          runDeadline[i] = t2 - t1;
        }
      }
      System.out.printf(
          "BENCH mode=%s module=%dB runs=%d warmup=%d compile[%s] run[%s] run+deadline[%s]%n",
          mode, echo.length, RUNS, WARMUP, stats(compile), stats(run), stats(runDeadline));
    }
  }

  private static String stats(long[] nanos) {
    long[] s = nanos.clone();
    Arrays.sort(s);
    return String.format(
        "median=%dus p90=%dus min=%dus",
        s[s.length / 2] / 1000, s[(int) (s.length * 0.9)] / 1000, s[0] / 1000);
  }
}
