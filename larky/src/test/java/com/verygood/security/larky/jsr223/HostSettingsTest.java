package com.verygood.security.larky.jsr223;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.parser.EvaluationMetadata;
import com.verygood.security.larky.parser.EvaluationSettings;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.junit.Test;

/**
 * A host gives a run its settings on a {@link LarkyScriptContext}, never as bindings: a script
 * cannot read or change them, and bindings that use the retired setting names ({@code
 * LARKY_EVAL_*}, {@code _EXECUTION_STEPS_}) are refused.
 */
public class HostSettingsTest {

  // Far more steps, and far longer, than any limit or deadline below allows.
  private static final String LOOP =
      String.join(
          "\n",
          "def process(i, c):",
          "    n = 0",
          "    for x in range(1000000000):",
          "        n += x",
          "    return str(n)",
          "");

  private static final String[] RETIRED = {
    "LARKY_EVAL_STEP_LIMIT",
    "LARKY_EVAL_EXPIRATION_MS",
    "LARKY_EVAL_MODULE_PROVIDERS",
    "LARKY_EVAL_SETTINGS",
    "_EXECUTION_STEPS_",
  };

  // The first evaluation in a JVM is slow (class loading, stdlib); deadlines are set after it.
  @org.junit.BeforeClass
  public static void warmUp() throws Exception {
    new LarkyScriptEngine().eval("def f():\n    return 1\nout = f()\n");
  }

  private static LarkyScriptContext contextWith(
      EvaluationSettings settings, Map<String, Object> engineScope) {
    LarkyScriptContext context = new LarkyScriptContext();
    context.setSettings(settings);
    context.setBindings(new SimpleBindings(new HashMap<>(engineScope)), ScriptContext.ENGINE_SCOPE);
    return context;
  }

  private static LarkyScriptEngine engineWith(ScriptContext context) {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    engine.setContext(context);
    return engine;
  }

  @Test
  public void aBindingWithARetiredSettingNameIsRefused() {
    for (String name : RETIRED) {
      LarkyScriptEngine engine =
          engineWith(contextWith(EvaluationSettings.NONE, Map.of(name, 1000L)));
      IllegalArgumentException e =
          assertThrows(name, IllegalArgumentException.class, () -> engine.eval("out = 1\n"));
      assertThat(e).hasMessageThat().contains(name + " is no longer read from bindings");
      assertThat(e).hasMessageThat().contains(EvaluationSettings.class.getName());
    }
  }

  @Test
  public void aRetiredSettingNameInTheGlobalScopeIsRefused() {
    ScriptContext context = new SimpleScriptContext();
    context.setBindings(
        new SimpleBindings(new HashMap<>(Map.of("LARKY_EVAL_EXPIRATION_MS", 1L))),
        ScriptContext.GLOBAL_SCOPE);
    LarkyScriptEngine engine = engineWith(context);
    assertThrows(IllegalArgumentException.class, () -> engine.eval("out = 1\n"));
  }

  @Test
  public void aScriptsVariableWithARetiredNameIsNotCopiedBack() throws Exception {
    LarkyScriptContext context = contextWith(EvaluationSettings.NONE, Map.of());
    LarkyScriptEngine engine = engineWith(context);

    engine.eval("LARKY_EVAL_STEP_LIMIT = 10000000000\n_EXECUTION_STEPS_ = 1\nout = 1\n");

    Bindings scope = context.getBindings(ScriptContext.ENGINE_SCOPE);
    assertThat(scope).containsKey("out");
    assertThat(scope).doesNotContainKey("LARKY_EVAL_STEP_LIMIT");
    assertThat(scope).doesNotContainKey("_EXECUTION_STEPS_");
    // so the next eval on these bindings runs
    engine.eval("out = 2\n");
  }

  @Test
  public void theContextsStepLimitApplies() {
    LarkyScriptEngine engine =
        engineWith(contextWith(EvaluationSettings.builder().stepLimit(1000).build(), Map.of()));
    ScriptException e =
        assertThrows(ScriptException.class, () -> engine.eval(LOOP + "out = process(1, 2)\n"));
    assertThat(e).hasMessageThat().contains("too many steps");
  }

  @Test(timeout = 30_000)
  public void theContextsDeadlineAppliesToEachEvalOfTheScriptThenTheCall() throws Exception {
    // A host that evaluates the script, then the call, on one context with one deadline.
    long deadline = System.currentTimeMillis() + 1000;
    LarkyScriptContext context =
        contextWith(
            EvaluationSettings.builder().expirationMs(deadline).build(),
            Map.of("input", "x", "ctx", "y"));
    LarkyScriptEngine engine = engineWith(context);

    engine.eval(LOOP);
    assertThat(context.metadata().outcome()).isEqualTo(EvaluationMetadata.Outcome.OK);

    ScriptException e =
        assertThrows(ScriptException.class, () -> engine.eval("output = process(input, ctx)"));
    assertThat(e).hasMessageThat().contains("past expiration date");
    assertThat(context.metadata().outcome()).isEqualTo(EvaluationMetadata.Outcome.DEADLINE);
    assertThat(context.getSettings().expirationMs().getAsLong()).isEqualTo(deadline);
  }

  @Test
  public void metadataIsOnTheContextAfterSuccessAndFailure() throws Exception {
    LarkyScriptContext context =
        contextWith(EvaluationSettings.builder().stepLimit(1000).build(), Map.of());
    LarkyScriptEngine engine = engineWith(context);
    assertThat(context.metadata()).isNull();

    ParsedStarFile ok = (ParsedStarFile) engine.eval("out = [i for i in range(10)]\n");
    assertThat(context.metadata()).isSameInstanceAs(ok.metadata());
    assertThat(context.metadata().outcome()).isEqualTo(EvaluationMetadata.Outcome.OK);
    assertThat(context.metadata().executedSteps()).isGreaterThan(0L);
    assertThat(ok.getGlobals()).doesNotContainKey("_EXECUTION_STEPS_");

    LarkyEvaluationScriptException e =
        assertThrows(
            LarkyEvaluationScriptException.class,
            () -> engine.eval(LOOP + "out = process(1, 2)\n"));
    assertThat(context.metadata()).isSameInstanceAs(e.metadata());
    assertThat(context.metadata().outcome()).isEqualTo(EvaluationMetadata.Outcome.STEP_LIMIT);
  }

  @Test
  public void theEnginesContextSettingsApplyToEvalWithBindings() throws Exception {
    // A host that sets its context on the engine, then evaluates with bindings of its own.
    LarkyScriptContext context =
        contextWith(EvaluationSettings.builder().stepLimit(1000).build(), Map.of());
    LarkyScriptEngine engine = engineWith(context);

    Bindings bindings = new SimpleBindings();
    bindings.put("n", 3);
    ParsedStarFile ok = (ParsedStarFile) engine.eval("out = n + 1\n", bindings);
    assertThat(context.metadata()).isSameInstanceAs(ok.metadata());
    assertThat(bindings).containsKey("out");

    ScriptException e =
        assertThrows(
            ScriptException.class,
            () -> engine.eval(LOOP + "out = process(1, 2)\n", new SimpleBindings()));
    assertThat(e).hasMessageThat().contains("too many steps");
    assertThat(context.metadata().outcome()).isEqualTo(EvaluationMetadata.Outcome.STEP_LIMIT);
  }

  @Test
  public void aPlainContextRunsWithNoSettings() throws Exception {
    ScriptContext context = new SimpleScriptContext();
    LarkyScriptEngine engine = engineWith(context);
    ParsedStarFile ok = (ParsedStarFile) engine.eval("out = 1\n");
    assertThat(ok.metadata().settings()).isEqualTo(EvaluationSettings.NONE);
  }
}
