package com.verygood.security.larky.jsr223;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.runtime.EvaluationSettings;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.junit.Test;

/**
 * The step limit is the evaluation's: the files a script loads run on their own threads, and the
 * steps of all of them count against one limit.
 */
public class EvaluationStepLimitTest {

  // about 3 steps per iteration
  private static String burn(String name, int iterations) {
    return String.join(
        "\n",
        "def burn():",
        "    t = 0",
        "    for i in range(" + iterations + "):",
        "        t += i",
        "    return t",
        name + " = burn()",
        "");
  }

  private static Object eval(long limit, Map<String, String> modules, String script)
      throws ScriptException {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    LarkyScriptContext context = new LarkyScriptContext();
    context.setSettings(EvaluationSettings.builder().stepLimit(limit).build());
    Map<String, Object> scope = new HashMap<>();
    scope.put(LarkyScriptEngine.MODULES, modules);
    context.setBindings(new SimpleBindings(scope), ScriptContext.ENGINE_SCOPE);
    engine.setContext(context);
    return engine.eval(script);
  }

  @Test
  public void loadedFilesShareTheScriptsStepLimit() {
    // each file alone is well under the limit; together they are over it
    Map<String, String> modules =
        Map.of("a.star", burn("va", 1000), "b.star", burn("vb", 1000), "c.star", burn("vc", 1000));
    String script = "load('a', 'va')\nload('b', 'vb')\nload('c', 'vc')\n" + burn("x", 1000);

    ScriptException e = assertThrows(ScriptException.class, () -> eval(10_000, modules, script));

    assertThat(e).hasMessageThat().contains("too many steps");
  }

  @Test
  public void aLoadedFileStopsWhenTheFilesBeforeItUsedTheLimit() {
    Map<String, String> modules = Map.of("a.star", burn("va", 2000), "b.star", burn("vb", 2000));

    ScriptException e =
        assertThrows(
            ScriptException.class,
            () -> eval(10_000, modules, "load('a', 'va')\nload('b', 'vb')\nout = 1\n"));

    assertThat(e).hasMessageThat().contains("too many steps in b.star");
  }

  @Test
  public void filesUnderTheLimitTogetherRun() throws Exception {
    Map<String, String> modules = Map.of("a.star", burn("va", 1000), "b.star", burn("vb", 1000));

    eval(100_000, modules, "load('a', 'va')\nload('b', 'vb')\n" + burn("x", 1000));
  }
}
