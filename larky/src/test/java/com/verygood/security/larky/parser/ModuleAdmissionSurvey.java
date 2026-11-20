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

package com.verygood.security.larky.parser;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Opt-in survey (-Dlarky.adm.survey=OUT.tsv): loads every stdlib, vendor and vgs module through
 * one module cache and writes, per module, whether the cache admitted it and why not.
 */
public class ModuleAdmissionSurvey {

  @Test
  public void survey() throws Exception {
    String out = System.getProperty("larky.adm.survey");
    assumeTrue(out != null && !out.isEmpty());
    Path root = Path.of("src/main/resources");
    List<String> labels;
    try (Stream<Path> files = Files.walk(root)) {
      labels =
          files
              .filter(p -> p.toString().endsWith(".star"))
              .map(p -> ResourceContentStarFile.labelOf(root.relativize(p).toString()))
              .sorted()
              .toList();
    }
    ModuleCache cache = new ModuleCache(true);
    Map<String, String> status = new TreeMap<>();
    for (String label : labels) {
      if (status.containsKey(label)) {
        continue;
      }
      LarkyEvaluator ev =
          new LarkyEvaluator(
              new LarkyScript(
                      ModuleSupplier.CORE_MODULES,
                      LarkyScript.StarlarkMode.STRICT,
                      ImmutableMap.of(),
                      EvaluationSettings.NONE)
                  .withModuleCache(cache),
              new TestingConsole());
      try {
        new LarkyEvaluator.LarkyLoader(
                new InMemMapBackedStarFile(ImmutableMap.of(), "main.star"), ev)
            .load(label);
      } catch (RuntimeException e) {
        status.putIfAbsent(label, "LOAD_FAILED");
        continue;
      }
      ev.lastMetadata().uncachedModules().forEach(status::putIfAbsent);
    }
    for (String p : cache.cachedPaths()) {
      status.putIfAbsent(ResourceContentStarFile.labelOf(p), "ADMITTED");
    }
    for (String label : labels) {
      status.putIfAbsent(label, "NOT_LOADED");
    }
    try (PrintWriter w =
        new PrintWriter(Files.newBufferedWriter(Path.of(out), StandardCharsets.UTF_8))) {
      status.forEach((k, v) -> w.println(k + "\t" + v));
    }
  }
}
