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

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Whether the module cache admits each of Larky's own modules (stdlib, vendor, vgs), and why not.
 *
 * <p>The decision is made at run time, from the values a module's top level creates. For the
 * modules Larky ships, those values do not depend on the host or the evaluation, so the decision is
 * the same on every run, in any load order and under concurrent loads. This test checks that, and
 * compares the decisions with {@code module-admission.tsv}: a module that stops being cached, or a
 * new reason, fails the build. Regenerate the file with {@code -Dlarky.admission.update=true}.
 */
public class ModuleAdmissionSnapshotTest {

  private static final Path RESOURCES = Path.of("src/main/resources");
  private static final Path SNAPSHOT = Path.of("src/test/resources/module-admission.tsv");

  private static List<String> labels;

  @BeforeAll
  static void listModules() throws IOException {
    try (Stream<Path> files = Files.walk(RESOURCES)) {
      labels =
          files
              .filter(p -> p.toString().endsWith(".star"))
              .map(p -> ResourceContentStarFile.labelOf(RESOURCES.relativize(p).toString()))
              .distinct()
              .sorted()
              .toList();
    }
  }

  /** Loads each label, in order, in its own evaluation sharing {@code cache}. */
  private static Map<String, String> admit(List<String> order, ModuleCache cache) {
    Map<String, String> status = new TreeMap<>();
    for (String label : order) {
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
        // a module that loads a test-only module (e.g. @vendor//asserts) loads only in tests
        status.put(label, testOnly(e) ? "TEST_ONLY" : "LOAD_FAILED");
        continue;
      }
      // a module a later evaluation takes from the cache reports nothing; keep the first verdict
      ev.lastMetadata().uncachedModules().forEach(status::putIfAbsent);
    }
    return status;
  }

  private static boolean testOnly(Throwable e) {
    for (Throwable c = e; c != null; c = c.getCause()) {
      if (String.valueOf(c.getMessage()).contains("is a test-only module")) {
        return true;
      }
    }
    return false;
  }

  /** Every label: ADMITTED, or the first reason any load gave. */
  private static Map<String, String> verdicts(List<String> order, ModuleCache cache) {
    Map<String, String> status = admit(order, cache);
    return complete(status, cache);
  }

  private static Map<String, String> complete(Map<String, String> status, ModuleCache cache) {
    Map<String, String> all = new TreeMap<>(status);
    for (String p : cache.cachedPaths()) {
      String label = ResourceContentStarFile.labelOf(p);
      String previous = all.put(label, "ADMITTED");
      assertWithMessage("%s was declined (%s) and also cached", label, previous)
          .that(previous == null || previous.equals("ADMITTED"))
          .isTrue();
    }
    for (String label : labels) {
      all.putIfAbsent(label, "NOT_LOADED");
    }
    return all;
  }

  private static String render(Map<String, String> verdicts) {
    return verdicts.entrySet().stream()
            .map(e -> e.getKey() + "\t" + e.getValue())
            .collect(Collectors.joining("\n"))
        + "\n";
  }

  @Test
  void theSameInAnyLoadOrder() {
    Map<String, String> sorted = verdicts(labels, new ModuleCache(true));

    List<String> reversed = new ArrayList<>(labels);
    Collections.reverse(reversed);
    assertWithMessage("reverse order").that(verdicts(reversed, new ModuleCache(true))).isEqualTo(sorted);

    for (long seed = 1; seed <= 3; seed++) {
      List<String> shuffled = new ArrayList<>(labels);
      Collections.shuffle(shuffled, new Random(seed));
      assertWithMessage("shuffled with seed %s", seed)
          .that(verdicts(shuffled, new ModuleCache(true)))
          .isEqualTo(sorted);
    }
  }

  @Test
  void theSameUnderConcurrentLoads() throws Exception {
    Map<String, String> expected = verdicts(labels, new ModuleCache(true));
    ModuleCache shared = new ModuleCache(true);
    int threads = 8;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      List<Future<Map<String, String>>> runs = new ArrayList<>();
      for (int t = 0; t < threads; t++) {
        List<String> order = new ArrayList<>(labels);
        Collections.shuffle(order, new Random(100 + t));
        runs.add(pool.submit(() -> admit(order, shared)));
      }
      for (Future<Map<String, String>> run : runs) {
        // what one thread saw declined must be declined for the same reason everywhere
        for (Map.Entry<String, String> e : run.get().entrySet()) {
          assertWithMessage("%s, concurrently", e.getKey())
              .that(e.getValue())
              .isEqualTo(expected.get(e.getKey()));
        }
      }
    } finally {
      pool.shutdownNow();
    }
    Map<String, String> admitted = new TreeMap<>();
    expected.forEach((k, v) -> admitted.put(k, v.equals("ADMITTED") ? v : "-"));
    Map<String, String> cachedConcurrently = new TreeMap<>();
    complete(Map.of(), shared)
        .forEach((k, v) -> cachedConcurrently.put(k, v.equals("ADMITTED") ? v : "-"));
    assertWithMessage("modules cached concurrently").that(cachedConcurrently).isEqualTo(admitted);
  }

  @Test
  void matchesTheCheckedInSnapshot() throws IOException {
    String actual = render(verdicts(labels, new ModuleCache(true)));
    if (Boolean.getBoolean("larky.admission.update")) {
      Files.writeString(SNAPSHOT, actual, StandardCharsets.UTF_8);
      return;
    }
    assertWithMessage(
            "module admission changed; if intended, rerun with -Dlarky.admission.update=true and"
                + " review the diff of %s",
            SNAPSHOT)
        .that(actual)
        .isEqualTo(Files.readString(SNAPSHOT, StandardCharsets.UTF_8));
  }
}
