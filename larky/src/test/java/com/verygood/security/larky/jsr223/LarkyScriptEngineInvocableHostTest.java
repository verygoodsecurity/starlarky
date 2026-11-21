package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.jsr223.LarkyScriptEngine.GlobalScope;
import com.verygood.security.larky.modules.vgs.ModuleProviders;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import com.verygood.security.larky.parser.EvaluationSettings;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.junit.Test;

/**
 * What a host gives an evaluation applies to {@link javax.script.Invocable} calls too: the module providers in the
 * bindings, and the engine's refusal of a context's global scope.
 */
public class LarkyScriptEngineInvocableHostTest {

  /** A vault whose every answer names it, so a test can tell which vault a call reached. */
  private record NamedVault(String name) implements LarkyVault {

    @Override
    public Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) {
      return name + "-alias";
    }

    @Override
    public Object reveal(Object value, Object storage) {
      return name + ":" + value;
    }

    @Override
    public void delete(Object value, Object storage) {
    }

    @Override
    public Object sign(String keyId, String message, String algorithm) {
      return name;
    }

    @Override
    public Object verify(String keyId, String message, String signature, String algorithm) {
      return name;
    }
  }

  /** A host interface the script implements. */
  public interface Revealer {
    String reveal(String alias);
  }

  private static final String SCRIPT = String.join("\n",
      "load('@vgs//vault', 'vault')",
      "load('@stdlib//larky', 'larky')",
      "def reveal(alias):",
      "    return vault.reveal(alias)",
      "holder = larky.struct(reveal = reveal)",
      "");

  private static ModuleProviders vault(String name) {
    return ModuleProviders.of().with(LarkyVault.class, new NamedVault(name));
  }

  private static LarkyScriptContext clean() {
    final LarkyScriptContext context = new LarkyScriptContext();
    context.setWriter(new StringWriter());
    context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
    return context;
  }

  /** The three ways a host calls a script, each revealing {@code alias}. */
  private static List<String> reveals(LarkyScriptEngine engine, String alias) throws Exception {
    final Object holder = engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE).get("holder");
    return List.of(
        String.valueOf(engine.invokeFunction("reveal", alias)),
        String.valueOf(engine.invokeMethod(holder, "reveal", alias)),
        engine.getInterface(Revealer.class).reveal(alias));
  }

  @Test
  public void callsGetTheProvidersFromTheContextAtTheTimeOfTheCall() throws Exception {
    final LarkyScriptEngine engine = LarkyScriptEngine.builder().build();
    engine.setContext(clean());
    engine.eval(SCRIPT);
    final LarkyScriptContext context = (LarkyScriptContext) engine.getContext();

    context.setSettings(EvaluationSettings.builder().moduleProviders(vault("A")).build());
    assertEquals(List.of("A:tok", "A:tok", "A:tok"), reveals(engine, "tok"));

    context.setSettings(EvaluationSettings.builder().moduleProviders(vault("B")).build());
    assertEquals(List.of("B:tok", "B:tok", "B:tok"), reveals(engine, "tok"));

    // without providers, the vault found with ServiceLoader answers (in these tests, the in-memory one)
    context.setSettings(EvaluationSettings.NONE);
    assertEquals(List.of("tok", "tok", "tok"), reveals(engine, "tok"));
  }

  @Test
  public void retiredProviderBindingsAreRefusedAsEvalRefusesThem() throws Exception {
    final LarkyScriptEngine engine = LarkyScriptEngine.builder().build();
    engine.setContext(clean());
    engine.eval(SCRIPT);
    engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE).put("LARKY_EVAL_MODULE_PROVIDERS", vault("A"));

    assertThrows(IllegalArgumentException.class, () -> engine.invokeFunction("reveal", "tok"));
  }

  @Test
  public void aRefusedGlobalScopeStopsACallBeforeItRuns() throws Exception {
    final LarkyScriptEngine engine = LarkyScriptEngine.builder().globalScope(GlobalScope.REFUSE).build();
    final ScriptContext context = clean();
    final StringWriter out = new StringWriter();
    context.setWriter(out);
    engine.setContext(context);
    engine.eval("def run():\n    print('ran')\n    return 1\nholder = 1\n");

    // a global binding added after the eval: eval would refuse this context, so a call must too
    context.setBindings(new SimpleBindings(new java.util.HashMap<>(Map.of("shared", "global value"))),
        ScriptContext.GLOBAL_SCOPE);

    final ScriptException byName = assertThrows(ScriptException.class, () -> engine.invokeFunction("run"));
    assertTrue(byName.getClass().getName(), byName instanceof LarkyGlobalScopeRefusedException);
    final ScriptException byMethod =
        assertThrows(ScriptException.class, () -> engine.invokeMethod("larky", "upper"));
    assertTrue(byMethod.getClass().getName(), byMethod instanceof LarkyGlobalScopeRefusedException);
    final Runnable proxy = engine.getInterface(Runnable.class);
    final UndeclaredThrowableException viaProxy = assertThrows(UndeclaredThrowableException.class, proxy::run);
    assertTrue(String.valueOf(viaProxy.getCause()), viaProxy.getCause() instanceof LarkyGlobalScopeRefusedException);
    assertEquals("", out.toString());
  }
}
