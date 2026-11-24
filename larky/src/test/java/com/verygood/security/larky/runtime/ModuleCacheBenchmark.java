// Copyright 2026 Very Good Security Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.verygood.security.larky.runtime;

import static org.junit.Assume.assumeTrue;

import com.verygood.security.larky.parser.EvaluationMetadata;
import com.verygood.security.larky.parser.EvaluationSettings;
import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyScript;
import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

/**
 * Measures what {@link ModuleCache} saves per evaluation: wall time and library-init steps with no
 * module cache and with a warm one. Runs only with {@code -Dlarky.bench=true}; writes one CSV row
 * per evaluation to {@code -Dlarky.bench.out} (default {@code target/module-cache-bench.csv}).
 * {@code -Dlarky.bench.onFirst=true} measures the cached mode first, to expose JIT-order bias.
 */
public class ModuleCacheBenchmark {

  private static final List<String> WORKLOADS =
      List.of(
          "json_roundtrip",
          "hmac_sign",
          "regex_mask",
          "jwt_hs384",
          "aes_gcm",
          "http_request",
          "lxml_c14n",
          "iso8583_encode");

  private static final int WARMUP = Integer.getInteger("larky.bench.warmup", 30);
  private static final int RUNS = Integer.getInteger("larky.bench.runs", 50);

  private static String source(String name) throws Exception {
    try (var in = ModuleCacheBenchmark.class.getResourceAsStream("/bench/" + name + ".star")) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static long[] run(String name, String src, ModuleCache cache) throws Exception {
    LarkyScript script =
        new CachedTestScript(
                ModuleSupplier.CORE_MODULES,
                LarkyScript.StarlarkMode.STRICT,
                ImmutableMap.of(),
                EvaluationSettings.NONE)
            .withModuleCache(cache);
    long t0 = System.nanoTime();
    EvaluationMetadata m =
        script
            .evaluate(InMemMapBackedStarFile.createStarFile(name + ".star", src), new TestingConsole())
            .metadata();
    long t1 = System.nanoTime();
    return new long[] {t1 - t0, m.executedSteps(), m.libraryInitSteps()};
  }

  @Test
  public void measure() throws Exception {
    assumeTrue(Boolean.getBoolean("larky.bench"));
    Path out = Path.of(System.getProperty("larky.bench.out", "target/module-cache-bench.csv"));
    try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out))) {
      w.println("workload,mode,phase,iter,nanos,executed_steps,library_init_steps");
      for (String name : WORKLOADS) {
        String src = source(name);
        if (Boolean.getBoolean("larky.bench.onFirst")) {
        // module cache on: the first evaluation misses, later ones hit
        ModuleCache on = new ModuleCache(true);
        for (int i = 0; i < WARMUP + RUNS; i++) {
          long[] r = run(name, src, on);
          String phase = i == 0 ? "miss" : i < WARMUP ? "warmup" : "measured";
          w.printf("%s,on,%s,%d,%d,%d,%d%n", name, phase, i, r[0], r[1], r[2]);
        }
        // no module cache: every evaluation executes every loaded module (ProgramCache still on)
        ModuleCache off = new ModuleCache(false);
        for (int i = 0; i < WARMUP + RUNS; i++) {
          long[] r = run(name, src, off);
          w.printf("%s,off,%s,%d,%d,%d,%d%n", name, i < WARMUP ? "warmup" : "measured", i, r[0], r[1], r[2]);
        }
        } else {
        // no module cache: every evaluation executes every loaded module (ProgramCache still on)
        ModuleCache off = new ModuleCache(false);
        for (int i = 0; i < WARMUP + RUNS; i++) {
          long[] r = run(name, src, off);
          w.printf("%s,off,%s,%d,%d,%d,%d%n", name, i < WARMUP ? "warmup" : "measured", i, r[0], r[1], r[2]);
        }
        // module cache on: the first evaluation misses, later ones hit
        ModuleCache on = new ModuleCache(true);
        for (int i = 0; i < WARMUP + RUNS; i++) {
          long[] r = run(name, src, on);
          String phase = i == 0 ? "miss" : i < WARMUP ? "warmup" : "measured";
          w.printf("%s,on,%s,%d,%d,%d,%d%n", name, phase, i, r[0], r[1], r[2]);
        }
        }
      }
    }
  }
}
