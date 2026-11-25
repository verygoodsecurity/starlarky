package com.verygood.security.larky.jsr223;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.modules.vgs.ModuleProviders;
import com.verygood.security.larky.runtime.EvaluationMetadata;
import com.verygood.security.larky.runtime.EvaluationMetadata.Outcome;
import com.verygood.security.larky.runtime.EvaluationSettings;
import com.verygood.security.larky.runtime.LarkyEvaluator;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import org.junit.Test;

/**
 * A host gives a run its settings as one {@link EvaluationSettings} on a {@link
 * LarkyScriptContext}, and reads what happened in the run from {@link EvaluationMetadata}: in the
 * result when it succeeded, in the exception when it failed, and on the context either way.
 */
public class EvaluationSettingsTest {

  private static final String LOOP =
      String.join(
          "\n",
          "def process():",
          "    n = 0",
          "    for x in range(1000000000):",
          "        n += x",
          "    return n",
          "");

  // The first evaluation in a JVM is slow (class loading, stdlib); deadlines are set after it.
  @org.junit.BeforeClass
  public static void warmUp() throws Exception {
    new LarkyScriptEngine().eval("def f():\n    return 1\nout = f()\n");
  }

  private static LarkyScriptEngine engineWith(
      EvaluationSettings settings, Map<String, Object> engineScope) {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    LarkyScriptContext context = new LarkyScriptContext();
    context.setSettings(settings);
    context.setBindings(new SimpleBindings(new HashMap<>(engineScope)), ScriptContext.ENGINE_SCOPE);
    engine.setContext(context);
    return engine;
  }

  private static LarkyEvaluationScriptException failure(
      EvaluationSettings settings, String script) {
    LarkyScriptEngine engine = engineWith(settings, Map.of());
    return assertThrows(LarkyEvaluationScriptException.class, () -> engine.eval(script));
  }

  @Test
  public void theStepLimitStopsTheRunAndIsReportedAsItsOutcome() {
    EvaluationSettings settings = EvaluationSettings.builder().stepLimit(1000).build();

    LarkyEvaluationScriptException e = failure(settings, LOOP + "out = process()\n");

    assertThat(e).hasMessageThat().contains("too many steps");
    EvaluationMetadata m = e.metadata();
    assertThat(m).isNotNull();
    assertThat(m.outcome()).isEqualTo(Outcome.STEP_LIMIT);
    assertThat(m.executedSteps()).isAtLeast(1000L);
    assertThat(m.settings()).isSameInstanceAs(settings);
  }

  @Test(timeout = 30_000)
  public void theExpirationDateStopsTheRunAndIsReportedAsItsOutcome() {
    EvaluationSettings settings =
        EvaluationSettings.builder().expiration(Instant.now().plusMillis(200)).build();

    LarkyEvaluationScriptException e = failure(settings, LOOP + "out = process()\n");

    assertThat(e).hasMessageThat().contains("past expiration date");
    assertThat(e.metadata().outcome()).isEqualTo(Outcome.DEADLINE);
    assertThat(e.metadata().elapsedNanos()).isGreaterThan(0L);
  }

  @Test(timeout = 30_000)
  public void aLimitHitInAFileTheScriptLoadsIsReportedAsItsOutcome() {
    EvaluationSettings settings = EvaluationSettings.builder().stepLimit(1000).build();
    LarkyScriptEngine engine =
        engineWith(
            settings, Map.of(LarkyScriptEngine.MODULES, Map.of("m.star", LOOP + "v = process()\n")));

    LarkyEvaluationScriptException e =
        assertThrows(LarkyEvaluationScriptException.class, () -> engine.eval("load('m', 'v')\n"));

    assertThat(e).hasMessageThat().contains("too many steps");
    assertThat(e.metadata()).isNotNull();
    assertThat(e.metadata().outcome()).isEqualTo(Outcome.STEP_LIMIT);
    assertThat(e.metadata().executedSteps()).isAtLeast(1000L);
  }

  @Test
  public void aScriptsOwnErrorWithTheCancellationTextIsAScriptError() {
    for (String text :
        new String[] {
          "Starlark computation cancelled: too many steps",
          "Starlark computation cancelled: past expiration date"
        }) {
      EvaluationSettings settings =
          EvaluationSettings.builder()
              .stepLimit(1_000_000)
              .expiration(Instant.now().plusSeconds(60))
              .build();
      LarkyEvaluationScriptException e = failure(settings, "fail('" + text + "')\n");
      assertThat(e.metadata().outcome()).isEqualTo(Outcome.SCRIPT_ERROR);
    }
  }

  @Test(timeout = 30_000)
  public void aScriptErrorRaisedAfterTheDeadlinePassedIsStillAScriptError() {
    // a long builtin call crosses the deadline; the clock is read only every few checks, so the
    // script's own error can be the one that surfaces
    EvaluationSettings settings =
        EvaluationSettings.builder().expiration(Instant.now().plusMillis(40)).build();

    LarkyEvaluationScriptException e =
        failure(settings, "x = len(sorted(list(range(3000000)), reverse=True))\nfail('mine')\n");

    // whichever error surfaced, the outcome agrees with it
    if (e.getMessage().contains("mine")) {
      assertThat(e.metadata().outcome()).isEqualTo(Outcome.SCRIPT_ERROR);
    } else {
      assertThat(e).hasMessageThat().contains("past expiration date");
      assertThat(e.metadata().outcome()).isEqualTo(Outcome.DEADLINE);
    }
  }

  @Test
  public void aScriptErrorIsReportedAsItsOutcome() {
    LarkyEvaluationScriptException e =
        failure(EvaluationSettings.builder().stepLimit(1_000_000).build(), "fail('no')\n");

    assertThat(e.metadata().outcome()).isEqualTo(Outcome.SCRIPT_ERROR);
  }

  @Test
  public void aSuccessfulRunReportsItsStepsAndTime() throws Exception {
    EvaluationSettings settings = EvaluationSettings.builder().stepLimit(1_000_000).build();
    LarkyScriptEngine engine = engineWith(settings, Map.of());

    ParsedStarFile result = (ParsedStarFile) engine.eval("out = [i for i in range(100)]\n");

    EvaluationMetadata m = result.metadata();
    assertThat(m.outcome()).isEqualTo(Outcome.OK);
    assertThat(m.settings()).isSameInstanceAs(settings);
    assertThat(m.elapsedNanos()).isGreaterThan(0L);
    assertThat(m.executedSteps()).isGreaterThan(0L);
    assertThat(result.getGlobals()).doesNotContainKey(LarkyEvaluator.RETIRED_EXECUTION_STEPS);
    assertThat(((LarkyScriptContext) engine.getContext()).metadata()).isSameInstanceAs(m);
    // host reads continue from what the whole evaluation left of the limit
    assertThat(result.interop().remainingSteps()).isEqualTo(1_000_000L - m.executedSteps());
  }

  @Test
  public void aScriptCannotReadOrChangeTheSettings() throws Exception {
    EvaluationSettings settings = EvaluationSettings.builder().stepLimit(1000).build();
    LarkyScriptEngine engine = engineWith(settings, Map.of());

    LarkyEvaluationScriptException e =
        assertThrows(LarkyEvaluationScriptException.class, () -> engine.eval("out = settings"));
    assertThat(e).hasMessageThat().contains("is not defined");
    engine.eval("LARKY_EVAL_STEP_LIMIT = 10000000000\nout = 1\n");

    assertThat(((LarkyScriptContext) engine.getContext()).getSettings()).isSameInstanceAs(settings);
    LarkyEvaluationScriptException limit =
        assertThrows(
            LarkyEvaluationScriptException.class, () -> engine.eval(LOOP + "out = process()\n"));
    assertThat(limit.metadata().outcome()).isEqualTo(Outcome.STEP_LIMIT);
  }

  @Test
  public void theBuilderRefusesANonPositiveStepLimit() {
    assertThrows(IllegalArgumentException.class, () -> EvaluationSettings.builder().stepLimit(0));
    assertThat(EvaluationSettings.NONE.stepLimit().isPresent()).isFalse();
    assertThat(EvaluationSettings.NONE.expirationMs().isPresent()).isFalse();
    assertThat(EvaluationSettings.NONE.moduleProviders()).isSameInstanceAs(ModuleProviders.NONE);
  }
}
