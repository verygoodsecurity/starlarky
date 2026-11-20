package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.jsr223.LarkyScriptEngine.GlobalScope;
import com.verygood.security.larky.modules.vgs.ModuleProviders;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import com.verygood.security.larky.parser.LarkyEvaluator;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import net.starlark.java.eval.SecretTaint;
import net.starlark.java.eval.TaintMode;
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
      "def diagnose(alias):",
      "    print(reveal(alias))",
      "holder = larky.struct(reveal = reveal)",
      "");

  private static ModuleProviders vault(String name) {
    return ModuleProviders.of().with(LarkyVault.class, new NamedVault(name));
  }

  private static ScriptContext clean() {
    final ScriptContext context = new SimpleScriptContext();
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
  public void callsGetTheProvidersInTheBindingsAtTheTimeOfTheCall() throws Exception {
    final LarkyScriptEngine engine = LarkyScriptEngine.builder().build();
    engine.setContext(clean());
    engine.eval(SCRIPT);
    final Bindings bindings = engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE);

    bindings.put(LarkyEvaluator.MODULE_PROVIDERS, vault("A"));
    assertEquals(List.of("A:tok", "A:tok", "A:tok"), reveals(engine, "tok"));

    bindings.put(LarkyEvaluator.MODULE_PROVIDERS, vault("B"));
    assertEquals(List.of("B:tok", "B:tok", "B:tok"), reveals(engine, "tok"));

    // without providers, the vault found with ServiceLoader answers (in these tests, the in-memory one)
    bindings.remove(LarkyEvaluator.MODULE_PROVIDERS);
    assertEquals(List.of("tok", "tok", "tok"), reveals(engine, "tok"));
  }

  @Test
  public void functionCallsObserveTaintOnlyWhileTheHostBindsIt() throws Exception {
    final LarkyScriptEngine engine = LarkyScriptEngine.builder().build();
    final ScriptContext context = clean();
    engine.setContext(context);
    engine.eval(SCRIPT);
    final Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
    bindings.put(LarkyEvaluator.MODULE_PROVIDERS, vault("fixture"));

    final List<SecretTaint.Sink> hits = new ArrayList<>();
    final SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, (sink, reason) -> hits.add(sink));
    bindings.put(SecretTaint.BINDING_NAME, taint);
    engine.invokeFunction("diagnose", "sample");
    assertEquals(List.of(SecretTaint.Sink.PRINT), hits);
    assertEquals("fixture:sample", context.getWriter().toString().strip());

    bindings.remove(SecretTaint.BINDING_NAME);
    engine.invokeFunction("diagnose", "sample");
    assertEquals(List.of(SecretTaint.Sink.PRINT), hits);
  }

  @Test
  public void methodCallsTrackValuesFromTheCurrentRun() throws Exception {
    final LarkyScriptEngine engine = LarkyScriptEngine.builder().build();
    final ScriptContext context = clean();
    engine.setContext(context);
    engine.eval(SCRIPT);
    final Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
    bindings.put(LarkyEvaluator.MODULE_PROVIDERS, vault("fixture"));
    final SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, null);
    bindings.put(SecretTaint.BINDING_NAME, taint);

    final Object revealed = engine.invokeMethod(bindings.get("holder"), "reveal", "sample");
    assertEquals("fixture:sample", revealed);
    assertTrue(taint.isTainted(revealed));
  }

  @Test
  public void moduleGlobalsLeaveTheEngineTaintBindingInPlace() throws Exception {
    final LarkyScriptEngine engine = LarkyScriptEngine.builder().build();
    final ScriptContext context = clean();
    engine.setContext(context);
    final Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
    final SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, null);
    bindings.put(SecretTaint.BINDING_NAME, taint);
    bindings.put(LarkyEvaluator.MODULE_PROVIDERS, vault("fixture"));

    engine.eval(SCRIPT + SecretTaint.BINDING_NAME + " = 'module value'\n");

    assertSame(taint, bindings.get(SecretTaint.BINDING_NAME));
    final Object revealed = engine.invokeFunction("reveal", "sample");
    assertEquals("fixture:sample", revealed);
    assertTrue(taint.isTainted(revealed));
  }

  @Test
  public void moduleGlobalsPreserveBothHostScopesAndTheirTaintPrecedence() throws Exception {
    final LarkyScriptEngine engine = LarkyScriptEngine.builder().build();
    final ScriptContext context = clean();
    final Bindings globalBindings = new SimpleBindings();
    context.setBindings(globalBindings, ScriptContext.GLOBAL_SCOPE);
    engine.setContext(context);
    final Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
    final SecretTaint globalTaint = new SecretTaint(TaintMode.OBSERVE, null);
    final SecretTaint engineTaint = new SecretTaint(TaintMode.OBSERVE, null);
    globalBindings.put(SecretTaint.BINDING_NAME, globalTaint);
    engineBindings.put(SecretTaint.BINDING_NAME, engineTaint);
    engineBindings.put(LarkyEvaluator.MODULE_PROVIDERS, vault("fixture"));

    engine.eval(SCRIPT + SecretTaint.BINDING_NAME + " = 'module value'\n");

    assertSame(globalTaint, globalBindings.get(SecretTaint.BINDING_NAME));
    assertSame(engineTaint, engineBindings.get(SecretTaint.BINDING_NAME));
    assertTrue(engineTaint.isTainted(engine.invokeFunction("reveal", "engine scope")));
    engineBindings.remove(SecretTaint.BINDING_NAME);
    assertTrue(globalTaint.isTainted(engine.invokeFunction("reveal", "global scope")));
  }

  @Test
  public void bindingsThatAreNotProvidersAreRefusedAsEvalRefusesThem() throws Exception {
    final LarkyScriptEngine engine = LarkyScriptEngine.builder().build();
    engine.setContext(clean());
    engine.eval(SCRIPT);
    engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE).put(LarkyEvaluator.MODULE_PROVIDERS, "not providers");

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
