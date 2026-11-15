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
import com.verygood.security.larky.console.Message.MessageType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkInt;
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
    return evaluate(script, stepLimit, new TestingConsole());
  }

  static ParsedStarFile evaluate(String script, Object stepLimit, TestingConsole console)
      throws Exception {
    ImmutableMap<String, Object> globals =
        stepLimit == null
            ? ImmutableMap.of()
            : ImmutableMap.of(LarkyEvaluator.STEP_LIMIT, stepLimit);
    return new LarkyScript(
            ModuleSupplier.CORE_MODULES, LarkyScript.StarlarkMode.STRICT, globals)
        .evaluate(
            InMemMapBackedStarFile.createStarFile(
                "main.star", script),
            console);
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
    for (int i = 0; i < 2; i++) {
      assertThat(probe.current.get(i)).isSameInstanceAs(probe.threads.get(i));
    }
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
  public void helperContinuesTheEvaluationsStepBudget() throws Exception {
    ParsedStarFile parsed = evaluate(SCRIPT, 2_000_000L);
    HostValues host = parsed.hostValues();
    assertThat(host.semantics()).isSameInstanceAs(LarkySemantics.LARKY_SEMANTICS);
    // what the evaluation left of its limit
    long used = parsed.getGlobalEnvironmentVariable(LarkyEvaluator.EXECUTION_STEPS, Long.class);
    assertThat(used).isGreaterThan(0L);
    assertThat(host.remainingSteps()).isEqualTo(2_000_000L - used);
    Object spin = parsed.getGlobalEnvironmentVariable("spin", Object.class);
    EvalException e = assertThrows(EvalException.class, () -> host.call(spin));
    assertThat(e).hasMessageThat().contains("too many steps");
    // the budget is shared by all calls: once spent, a cheap call fails too, also through a
    // derived instance
    Object add = parsed.getGlobalEnvironmentVariable("add", Object.class);
    EvalException after =
        assertThrows(
            EvalException.class,
            () -> host.withPrintHandler((t, m) -> {}).call(add, StarlarkInt.of(1)));
    assertThat(after).hasMessageThat().contains("too many steps");
  }

  @Test
  public void callsDrawOnOneBudget() throws Exception {
    // a getter that takes ~3,000 steps, under a budget of 10,000 for all calls
    String script = "def work():\n    for i in range(1000):\n        pass\n    return 1\n";
    ParsedStarFile parsed = evaluate(script, 10_000L);
    HostValues host = parsed.hostValues();
    Object work = parsed.getGlobalEnvironmentVariable("work", Object.class);
    int ok = 0;
    EvalException e = null;
    for (int i = 0; i < 100 && e == null; i++) {
      try {
        host.call(work);
        ok++;
      } catch (EvalException x) {
        e = x;
      }
    }
    assertThat(e).isNotNull();
    assertThat(e).hasMessageThat().contains("too many steps");
    assertThat(ok).isAtLeast(1);
    assertThat(ok).isLessThan(10);
  }

  @Test
  public void withoutAStepLimitTheHelperHasNone() throws Exception {
    assertThat(evaluate(SCRIPT, null).hostValues().remainingSteps()).isNull();
  }

  @Test
  public void helperStopsAtTheEvaluationsExpirationDate() throws Exception {
    long expiration = System.currentTimeMillis() + 60_000;
    ParsedStarFile parsed =
        new LarkyScript(
                ModuleSupplier.CORE_MODULES,
                LarkyScript.StarlarkMode.STRICT,
                ImmutableMap.of(LarkyEvaluator.EXPIRATION_MS, expiration))
            .evaluate(InMemMapBackedStarFile.createStarFile("main.star", SCRIPT), new TestingConsole());
    assertThat(parsed.hostValues().expirationMs()).isEqualTo(expiration);

    HostValues past =
        HostValues.of(LarkySemantics.LARKY_SEMANTICS, null, System.currentTimeMillis() - 1, null);
    Object add = parsed.getGlobalEnvironmentVariable("add", Object.class);
    EvalException e = assertThrows(EvalException.class, () -> past.call(add, StarlarkInt.of(1)));
    assertThat(e).hasMessageThat().contains("past expiration date");

    // a call runs on a thread with that date, so a callback that runs past it stops there
    Probe probe = new Probe();
    parsed.hostValues().call(probe);
    assertThat(probe.threads.get(0).getExpirationMs()).isEqualTo(expiration);
  }

  /** A callable whose Java code fails with an unchecked exception. */
  private static final class Broken implements StarlarkCallable {
    @Override
    public String getName() {
      return "broken";
    }

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) {
      throw new IllegalArgumentException("java detail");
    }
  }

  @Test
  public void uncheckedExceptionsOfCallbacksAreEvalExceptions() throws Exception {
    EvalException e =
        assertThrows(EvalException.class, () -> HostValues.create().call(new Broken()));
    assertThat(e).hasMessageThat().contains("internal error in HostValues.call");
    assertThat(e).hasMessageThat().contains("IllegalArgumentException: java detail");
  }

  @Test
  public void javaStringRunsAStructsReprOnAHelperThread() throws Exception {
    String script =
        String.join(
            "\n",
            "load('@stdlib//larky', 'larky')",
            "def _repr():",
            "    return 'R!'",
            "R = larky.struct(__repr__ = _repr)",
            "[R]",
            "");
    String out =
        new LarkyScript(LarkyScript.StarlarkMode.STRICT)
            .executeSkylarkWithOutputAsString(
                InMemMapBackedStarFile.createStarFile("main.star", script),
                new ModuleSupplier().create(),
                new TestingConsole());
    assertThat(out).isEqualTo("[R!]");
  }

  @Test
  public void printOfCallbacksGoesToTheEvaluationsConsoleUnlessAHandlerIsGiven()
      throws Exception {
    TestingConsole console = new TestingConsole();
    ParsedStarFile parsed =
        evaluate("def say(word):\n    print(word)\n    return 1\n", null, console);
    Object say = parsed.getGlobalEnvironmentVariable("say", Object.class);
    parsed.hostValues().call(say, "to-console");
    List<String> printed = new ArrayList<>();
    parsed.hostValues().withPrintHandler((t, m) -> printed.add(m)).call(say, "to-handler");
    assertThat(printed).containsExactly("to-handler");
    console.assertThat().onceInLog(MessageType.VERBOSE, ".*to-console.*");
    console.assertThat().timesInLog(0, MessageType.VERBOSE, ".*to-handler.*");
  }

  @Test
  public void starlarkDefaultPrintHandlerWritesToStandardErrorWithTheLocation() throws Exception {
    TestingConsole console = new TestingConsole();
    ParsedStarFile parsed =
        evaluate("def say(word):\n    print(word)\n    return 1\n", null, console);
    Object say = parsed.getGlobalEnvironmentVariable("say", Object.class);
    java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();
    java.io.PrintStream saved = System.err;
    System.setErr(new java.io.PrintStream(err, true, StandardCharsets.UTF_8));
    try {
      parsed.hostValues().withStarlarkDefaultPrintHandler().call(say, "to-stderr");
    } finally {
      System.setErr(saved);
    }
    assertThat(err.toString(StandardCharsets.UTF_8))
        .isEqualTo("main.star:2:10: to-stderr" + System.lineSeparator());
    console.assertThat().timesInLog(0, MessageType.VERBOSE, ".*to-stderr.*");
  }
}
