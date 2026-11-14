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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import org.junit.Test;

/** {@link CurrentEvaluation} and {@link HostValues}. */
public class HostValuesTest {

  static final String SCRIPT =
      String.join(
          "\n",
          "def spin():",
          "    for i in range(10000000):",
          "        pass",
          "    return 1",
          "def make():",
          "    return [1, 2]",
          "def add(a, b = 10):",
          "    return a + b",
          "");

  static ParsedStarFile evaluate(String script, Object stepLimit) throws Exception {
    ImmutableMap<String, Object> globals =
        stepLimit == null
            ? ImmutableMap.of()
            : ImmutableMap.of(LarkyEvaluator.STEP_LIMIT, stepLimit);
    return new LarkyScript(
            ModuleSupplier.CORE_MODULES, LarkyScript.StarlarkMode.STRICT, globals)
        .evaluate(
            InMemMapBackedStarFile.createStarFile(
                "main.star", script),
            new TestingConsole());
  }

  @Test
  public void currentEvaluationNestsAndRestores() throws Exception {
    assertThat(CurrentEvaluation.currentOrNull()).isNull();
    IllegalStateException e =
        assertThrows(IllegalStateException.class, CurrentEvaluation::requireThread);
    assertThat(e).hasMessageThat().contains("no Starlark evaluation is active");

    try (net.starlark.java.eval.Mutability mu = net.starlark.java.eval.Mutability.create("t")) {
      StarlarkThread outer = StarlarkThread.createTransient(mu, LarkySemantics.LARKY_SEMANTICS);
      StarlarkThread inner = StarlarkThread.createTransient(mu, LarkySemantics.LARKY_SEMANTICS);
      try (CurrentEvaluation a = CurrentEvaluation.enter(outer)) {
        assertThat(CurrentEvaluation.requireThread()).isSameInstanceAs(outer);
        try (CurrentEvaluation b = CurrentEvaluation.enter(inner)) {
          assertThat(CurrentEvaluation.requireThread()).isSameInstanceAs(inner);
        }
        assertThat(CurrentEvaluation.requireThread()).isSameInstanceAs(outer);
      }
      assertThat(CurrentEvaluation.currentOrNull()).isNull();
    }
  }

  @Test
  public void currentEvaluationIsPerJavaThread() throws Exception {
    try (net.starlark.java.eval.Mutability mu = net.starlark.java.eval.Mutability.create("t")) {
      StarlarkThread t = StarlarkThread.createTransient(mu, LarkySemantics.LARKY_SEMANTICS);
      try (CurrentEvaluation a = CurrentEvaluation.enter(t)) {
        StarlarkThread[] seen = new StarlarkThread[] {t};
        Thread other = new Thread(() -> seen[0] = CurrentEvaluation.currentOrNull());
        other.start();
        other.join();
        assertThat(seen[0]).isNull();
      }
    }
  }

  /** A callable that records the thread it runs on and the current evaluation. */
  private static final class Probe implements StarlarkCallable {
    final List<StarlarkThread> threads = new ArrayList<>();
    final List<StarlarkThread> current = new ArrayList<>();

    @Override
    public String getName() {
      return "probe";
    }

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) {
      threads.add(thread);
      current.add(CurrentEvaluation.currentOrNull());
      return Starlark.NONE;
    }
  }

  @Test
  public void everyHostCallRunsOnAFreshThreadThatIsTheCurrentEvaluation() throws Exception {
    HostValues host = HostValues.create();
    Probe probe = new Probe();
    host.call(probe);
    host.call(probe);
    assertThat(probe.threads).hasSize(2);
    assertThat(probe.threads.get(0)).isNotSameInstanceAs(probe.threads.get(1));
    assertThat(probe.current).isEqualTo(probe.threads);
    assertThat(CurrentEvaluation.currentOrNull()).isNull();
  }

  @Test
  public void hostCallsDoNotShareMutability() throws Exception {
    ParsedStarFile parsed = evaluate(SCRIPT, null);
    HostValues host = parsed.hostValues();
    Object make = parsed.getGlobalEnvironmentVariable("make", Object.class);
    Object first = host.call(make);
    Object second = host.call(make);
    assertThat(first).isInstanceOf(StarlarkList.class);
    assertThat(first).isNotSameInstanceAs(second);
    // the mutability of a call is closed when the call returns
    assertThat(((StarlarkList<?>) first).mutability().isFrozen()).isTrue();
    assertThat(((StarlarkList<?>) second).mutability().isFrozen()).isTrue();
    assertThat(((StarlarkList<?>) first).mutability())
        .isNotSameInstanceAs(((StarlarkList<?>) second).mutability());
    // and so a list from one call cannot be changed by the host or a later call
    assertThrows(
        EvalException.class,
        () -> host.callMethod(first, "append", net.starlark.java.eval.StarlarkInt.of(3)));
  }

  @Test
  public void helperCallsFunctionsWithArgumentsAndKeywords() throws Exception {
    ParsedStarFile parsed = evaluate(SCRIPT, null);
    HostValues host = parsed.hostValues();
    Object add = parsed.getGlobalEnvironmentVariable("add", Object.class);
    assertThat(host.call(add, net.starlark.java.eval.StarlarkInt.of(1)))
        .isEqualTo(net.starlark.java.eval.StarlarkInt.of(11));
    assertThat(host.str(StarlarkList.immutableOf(net.starlark.java.eval.StarlarkInt.of(1), "a"))).isEqualTo("[1, \"a\"]");
    assertThat(host.repr("a")).isEqualTo("\"a\"");
  }

  @Test
  public void helperInheritsSemanticsAndStepLimitOfTheEvaluation() throws Exception {
    ParsedStarFile parsed = evaluate(SCRIPT, 2_000_000L);
    HostValues host = parsed.hostValues();
    assertThat(host.semantics()).isSameInstanceAs(LarkySemantics.LARKY_SEMANTICS);
    assertThat(host.maxSteps()).isEqualTo(2_000_000L);
    Object spin = parsed.getGlobalEnvironmentVariable("spin", Object.class);
    EvalException e = assertThrows(EvalException.class, () -> host.call(spin));
    assertThat(e).hasMessageThat().contains("too many steps");
    // the limit applies to each call, not cumulatively: a cheap call still works afterwards
    Object add = parsed.getGlobalEnvironmentVariable("add", Object.class);
    assertThat(host.call(add, net.starlark.java.eval.StarlarkInt.of(1)))
        .isEqualTo(net.starlark.java.eval.StarlarkInt.of(11));
  }

  @Test
  public void withoutAStepLimitTheHelperHasNone() throws Exception {
    assertThat(evaluate(SCRIPT, null).hostValues().maxSteps()).isNull();
  }

  @Test
  public void printOfCallbacksIsDiscardedUnlessAHandlerIsGiven() throws Exception {
    ParsedStarFile parsed =
        evaluate("def say():\n    print('hi')\n    return 1\n", null);
    Object say = parsed.getGlobalEnvironmentVariable("say", Object.class);
    List<String> printed = new ArrayList<>();
    parsed.hostValues().call(say);
    parsed.hostValues().withPrintHandler((t, m) -> printed.add(m)).call(say);
    assertThat(printed).containsExactly("hi");
  }
}
