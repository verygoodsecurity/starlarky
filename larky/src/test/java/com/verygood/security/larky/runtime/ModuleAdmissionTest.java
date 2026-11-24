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

package com.verygood.security.larky.runtime;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyScript;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import org.junit.jupiter.api.Test;

/** What the module cache shares, what it declines and why (ModuleAdmission, ModuleCache). */
class ModuleAdmissionTest {

  /**
   * Counts how often a fixture module's top level runs. A host binding with no instance fields
   * (the count is static), so the admission walk accepts it.
   */
  @StarlarkBuiltin(name = "init_counter")
  public static final class InitCounter implements StarlarkValue {
    static final InitCounter INSTANCE = new InitCounter();
    private static final AtomicInteger COUNT = new AtomicInteger();

    @StarlarkMethod(name = "count")
    public Object count() {
      COUNT.incrementAndGet();
      return Starlark.NONE;
    }

    static int get() {
      return COUNT.get();
    }
  }

  private static final String FIXTURES = "vendor/_admission_fixtures/";

  private static final String SAFE_SCRIPT =
      String.join(
          "\n",
          "load(\"@vendor//_admission_fixtures/safe\", \"digest\", \"lookup\")",
          "result = digest(b\"abc\") + \":\" + str(lookup(\"a\"))",
          "");

  private static final String DECLINED_SCRIPT =
      String.join(
          "\n",
          "load(\"@vendor//_admission_fixtures/depends\", \"call\")",
          "load(\"@vendor//_admission_fixtures/loads_host_service_too\", \"again\")",
          "result = call() + again()",
          "");

  private static LarkyEvaluator evaluator(ModuleCache cache) {
    return CachedTestScript.evaluator(
        new CachedTestScript(
                ModuleSupplier.CORE_MODULES,
                LarkyScript.StarlarkMode.STRICT,
                Map.of("init_counter", InitCounter.INSTANCE))
            .withModuleCache(cache),
        new TestingConsole());
  }

  private record Run(Module module, Object result, Map<String, String> uncached) {}

  private static Run run(ModuleCache cache, String script, String fixture) throws Exception {
    LarkyEvaluator ev = evaluator(cache);
    Module main = ev.eval(InMemMapBackedStarFile.createStarFile("main.star", script)).module();
    return new Run(
        ev.loadedModule(FIXTURES + fixture + ".star"),
        main.getGlobal("result"),
        ev.lastMetadata().uncachedModules());
  }

  @Test
  void aSafeModuleIsSharedAndItsTopLevelRunsOnce() throws Exception {
    Object expected = run(new ModuleCache(false), SAFE_SCRIPT, "safe").result();
    ModuleCache cache = new ModuleCache(true);
    int before = InitCounter.get();

    Run first = run(cache, SAFE_SCRIPT, "safe");
    assertThat(InitCounter.get()).isEqualTo(before + 1);
    assertThat(first.uncached()).doesNotContainKey("@vendor//_admission_fixtures/safe");
    assertThat(cache.admitted(first.module())).isTrue();

    for (int i = 0; i < 4; i++) {
      Run again = run(cache, SAFE_SCRIPT, "safe");
      assertThat(again.module()).isSameInstanceAs(first.module());
      assertThat(again.result()).isEqualTo(expected);
    }
    assertWithMessage("runs of the module's top level").that(InitCounter.get()).isEqualTo(before + 1);
  }

  @Test
  void aSharedModuleIsUsedByConcurrentEvaluations() throws Exception {
    Object expected = run(new ModuleCache(false), SAFE_SCRIPT, "safe").result();
    ModuleCache cache = new ModuleCache(true);
    Module shared = run(cache, SAFE_SCRIPT, "safe").module();
    int before = InitCounter.get();

    int threads = 16;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Run>> runs = new ArrayList<>();
      for (int i = 0; i < threads * 4; i++) {
        Callable<Run> c =
            () -> {
              start.await();
              return run(cache, SAFE_SCRIPT, "safe");
            };
        runs.add(pool.submit(c));
      }
      start.countDown();
      for (Future<Run> f : runs) {
        Run r = f.get(60, TimeUnit.SECONDS);
        assertThat(r.module()).isSameInstanceAs(shared);
        assertThat(r.result()).isEqualTo(expected);
      }
    } finally {
      pool.shutdownNow();
    }
    assertWithMessage("runs of the module's top level").that(InitCounter.get()).isEqualTo(before);
  }

  @Test
  void aDeclinedModuleRunsOncePerEvaluationAndSaysWhy() throws Exception {
    ModuleCache cache = new ModuleCache(true);
    int before = InitCounter.get();

    Run first = run(cache, DECLINED_SCRIPT, "host_service");
    // two modules load it, but it runs once in the evaluation
    assertThat(InitCounter.get()).isEqualTo(before + 1);
    assertThat(first.result()).isEqualTo("pongpong");
    assertThat(first.uncached())
        .containsEntry(
            "@vendor//_admission_fixtures/host_service",
            "HOST_SERVICE com.verygood.security.larky.modules.vgs.metrics.MetricsModule"
                + " at native_metrics");
    assertThat(first.uncached())
        .containsEntry(
            "@vendor//_admission_fixtures/depends",
            "DEPENDS_ON @vendor//_admission_fixtures/host_service");
    assertThat(first.uncached())
        .containsEntry(
            "@vendor//_admission_fixtures/loads_host_service_too",
            "DEPENDS_ON @vendor//_admission_fixtures/host_service");
    assertThat(cache.admitted(first.module())).isFalse();

    // the next evaluation runs its own copy, once
    Run second = run(cache, DECLINED_SCRIPT, "host_service");
    assertThat(InitCounter.get()).isEqualTo(before + 2);
    assertThat(second.module()).isNotSameInstanceAs(first.module());
    assertThat(second.uncached()).isEqualTo(first.uncached());
  }

  @Test
  void withTheCacheOffNothingIsReported() throws Exception {
    assertThat(run(new ModuleCache(false), DECLINED_SCRIPT, "host_service").uncached()).isEmpty();
  }

  // ------------------------------------------------------------------- the walk, rule by rule

  private static final ModuleAdmission.Modules NO_MODULES =
      new ModuleAdmission.Modules() {
        @Override
        public boolean admitted(Module m) {
          return false;
        }

        @Override
        public boolean nativeWrapper(Module m) {
          return false;
        }

        @Override
        public String label(Module m) {
          return "@test//other";
        }
      };

  private static ModuleAdmission.Decline check(Map<String, Object> globals) {
    return check(globals, ImmutableMap.of(), List.of());
  }

  private static ModuleAdmission.Decline check(
      Map<String, Object> globals, Map<String, Object> predeclared, List<Object> shared) {
    Module m = Module.create();
    globals.forEach(m::setGlobal);
    return ModuleAdmission.check(m, predeclared, shared, NO_MODULES);
  }

  private static Dict<String, Object> frozenDict(Map<String, Object> entries) {
    Mutability mu = Mutability.create("test");
    Dict.Builder<String, Object> b = Dict.builder();
    entries.forEach(b::put);
    Dict<String, Object> d = b.build(mu);
    mu.freeze();
    return d;
  }

  @Test
  void frozenStarlarkDataIsAccepted() {
    assertThat(
            check(
                ImmutableMap.of(
                    "TABLE",
                    frozenDict(ImmutableMap.of("a", "x", "b", Starlark.NONE)),
                    "N",
                    net.starlark.java.eval.StarlarkInt.of(3))))
        .isNull();
  }

  @Test
  void aJavaCollectionOutsideAFrozenValueIsUnknown() {
    ModuleAdmission.Decline d = check(ImmutableMap.of("x", new ArrayList<>()));
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.UNKNOWN_CLASS);
    assertThat(d.subject()).isEqualTo("java.util.ArrayList");
    assertThat(d.path()).isEqualTo("x");
  }

  @Test
  void anUnfrozenValueIsOwnedMutable() {
    ModuleAdmission.Decline d =
        check(ImmutableMap.of("x", StarlarkList.newList(Mutability.create("open"))));
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.OWNED_MUTABLE);
    assertThat(d.path()).isEqualTo("x");
  }

  @Test
  void aThreadInsideACollectionIsFoundWithItsPath() {
    StarlarkThread thread =
        StarlarkThread.createTransient(Mutability.create("t"), StarlarkSemantics.DEFAULT);
    ModuleAdmission.Decline d =
        check(ImmutableMap.of("x", ImmutableMap.of("t", ImmutableList.of(thread))));
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.THREAD);
    assertThat(d.path()).isEqualTo("x{0}[0]");
  }

  /** A Larky-package object with a field that can be reassigned. */
  static final class Reassignable {
    @SuppressWarnings("unused")
    private String value = "v";
  }

  @Test
  void aNonFinalFieldOfAnUnfrozenObjectIsMutable() {
    ModuleAdmission.Decline d = check(ImmutableMap.of("x", new Reassignable()));
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.MUTABLE_FIELD);
    assertThat(d.subject()).isEqualTo(Reassignable.class.getName() + "#value");
    assertThat(d.path()).isEqualTo("x");
  }

  @Test
  void aModuleThatIsNotSharedIsADependency() {
    ModuleAdmission.Decline d = check(ImmutableMap.of("dep", Module.create()));
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.DEPENDS_ON);
    assertThat(d.subject()).isEqualTo("@test//other");
    assertThat(d.path()).isEqualTo("dep");
  }

  @Test
  void aHostBindingTheModuleUsesIsChecked() {
    ModuleAdmission.Decline d =
        check(ImmutableMap.of(), ImmutableMap.of("ctx", new HashMap<>()), List.of());
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.UNKNOWN_CLASS);
    assertThat(d.path()).isEqualTo("<predeclared ctx>");
  }

  @Test
  void aSharedBuiltinIsNotWalked() {
    ArrayList<Object> builtin = new ArrayList<>();
    assertThat(check(ImmutableMap.of("x", builtin), ImmutableMap.of(), List.of(builtin))).isNull();
  }

  @Test
  void collectionPathsUseEntryPositionsInsteadOfKeyText() {
    String key = "syntheticKeyMarker";
    List<Object> mappings =
        List.of(
            ImmutableMap.of("other", "safe", key, new ArrayList<>()),
            ImmutableListMultimap.of(key, "safe", key, new ArrayList<>()));
    for (Object mapping : mappings) {
      ModuleAdmission.Decline d = check(ImmutableMap.of("x", mapping));
      assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.UNKNOWN_CLASS);
      assertThat(d.path()).isEqualTo("x{1}");
      assertThat(d.toString()).doesNotContain(key);
    }
  }

  @Test
  void tooManyObjectsIsDeclined() {
    List<Object> many = new ArrayList<>();
    for (int i = 0; i <= 50; i++) {
      many.add(new Object());
    }
    Module m = Module.create();
    m.setGlobal("x", ImmutableList.copyOf(many));
    ModuleAdmission.Decline d =
        ModuleAdmission.check(m, ImmutableMap.of(), Set.of(), NO_MODULES, 50);
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.TOO_LARGE);
  }

  @Test
  void repeatedReferencesConsumeTheWalkBudget() {
    Module m = Module.create();
    m.setGlobal("x", ImmutableList.copyOf(Collections.nCopies(49, "safe")));
    assertThat(ModuleAdmission.check(m, ImmutableMap.of(), Set.of(), NO_MODULES, 50)).isNull();

    m.setGlobal("x", ImmutableList.copyOf(Collections.nCopies(50, "safe")));
    ModuleAdmission.Decline d =
        ModuleAdmission.check(m, ImmutableMap.of(), Set.of(), NO_MODULES, 50);
    assertThat(d).isNotNull();
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.TOO_LARGE);
    assertThat(d.path()).isEqualTo("x[49]");
  }

  @Test
  void unknownPredeclaredNamesAreNotAnalyzed() {
    ModuleAdmission.Decline d =
        new ModuleCache(true)
            .put(
                "a.star",
                net.starlark.java.syntax.FileOptions.DEFAULT,
                StarlarkSemantics.DEFAULT,
                ModuleCache.Registry.DEFAULT,
                Module.create(),
                null,
                Set.of(),
                ImmutableMap.of(),
                ImmutableMap.of(),
                ImmutableMap.of());
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.NOT_ANALYZED);
  }
}
