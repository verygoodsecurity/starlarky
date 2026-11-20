package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.jsr223.LarkyScriptEngine.GlobalScope;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.io.StringWriter;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.junit.Test;

/**
 * {@link LarkyScriptEngine.GlobalScope}: an engine uses a context's global scope as JSR-223 describes it, unless it is
 * built to refuse one, and then nothing in a global scope reaches a script.
 */
public class LarkyScriptEngineGlobalScopeTest {

  private static LarkyScriptEngine refusing() {
    return LarkyScriptEngine.builder().globalScope(GlobalScope.REFUSE).build();
  }

  /** A context whose global scope holds {@code shared}, as a ScriptEngineManager's would after a put. */
  private static ScriptContext withGlobal() {
    final ScriptContext context = new SimpleScriptContext();
    context.setWriter(new StringWriter());
    context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
    context.setBindings(new SimpleBindings(new java.util.HashMap<>(Map.of("shared", "global value"))),
        ScriptContext.GLOBAL_SCOPE);
    return context;
  }

  private static ScriptContext clean() {
    final ScriptContext context = new SimpleScriptContext();
    context.setWriter(new StringWriter());
    context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
    return context;
  }

  private static void assertRefused(ScriptException e) {
    assertTrue(e.getClass().getName(), e instanceof LarkyGlobalScopeRefusedException);
    assertTrue(e.getMessage(), e.getMessage().contains("[shared]"));
    assertTrue(e.getMessage(), e.getMessage().contains("engine scope"));
  }

  @Test
  public void anEngineUsesTheJsr223GlobalScopeUnlessBuiltOtherwise() {
    assertEquals(GlobalScope.JSR223, new LarkyScriptEngine().globalScope());
    assertEquals(GlobalScope.JSR223, LarkyScriptEngine.builder().build().globalScope());
    assertEquals(GlobalScope.REFUSE, refusing().globalScope());
    assertThrows(NullPointerException.class, () -> LarkyScriptEngine.builder().globalScope(null));
  }

  @Test
  public void theDefaultEngineStillGivesTheGlobalScopeToAScriptAndWritesItBack() throws Exception {
    final LarkyScriptEngine engine = new LarkyScriptEngine();
    final ScriptContext context = withGlobal();
    context.getBindings(ScriptContext.GLOBAL_SCOPE).put("both", "global");
    context.getBindings(ScriptContext.ENGINE_SCOPE).put("both", "engine");

    final ParsedStarFile result = (ParsedStarFile) engine.compile("out = shared + '/' + both\n").eval(context);
    assertEquals("global value/engine", result.getGlobalEnvironmentVariable("out", Object.class));

    // a script's top-level value of a name already in the global scope goes back there
    engine.compile("shared = 'written back'\n").eval(context);
    assertEquals("written back", context.getBindings(ScriptContext.GLOBAL_SCOPE).get("shared"));
  }

  /** The context given to eval governs the call, not the engine's default context. */
  @Test
  public void aRefusingEngineEvaluatesACleanContextWhateverItsDefaultContextHolds() throws Exception {
    final LarkyScriptEngine engine = refusing();
    engine.setContext(withGlobal());
    final ScriptContext context = clean();
    context.getBindings(ScriptContext.ENGINE_SCOPE).put("given", "engine value");

    final ParsedStarFile result = (ParsedStarFile) engine.eval("out = given\n", context);

    assertEquals("engine value", result.getGlobalEnvironmentVariable("out", Object.class));
    assertEquals("engine value", context.getBindings(ScriptContext.ENGINE_SCOPE).get("out"));
    assertFalse("the default context is not written", engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE)
        .containsKey("out"));
  }

  /** Its global scope, settings and writer all come from the context given to eval. */
  @Test
  public void evalWithAContextUsesThatContextsGlobalScopeSettingsAndWriter() throws Exception {
    final LarkyScriptEngine engine = new LarkyScriptEngine();
    final StringWriter defaultWriter = new StringWriter();
    engine.getContext().setWriter(defaultWriter);
    final ScriptContext context = withGlobal();
    context.setAttribute(LarkyScriptEngine.MODULES, Map.of("helper.star", "def greet():\n    return 'hi'\n"),
        ScriptContext.ENGINE_SCOPE);

    final ParsedStarFile result = (ParsedStarFile) engine.eval(
        "load('helper', 'greet')\nprint(greet())\nout = shared\n", context);

    assertEquals("global value", result.getGlobalEnvironmentVariable("out", Object.class));
    assertEquals("hi\n", context.getWriter().toString());
    assertEquals("", defaultWriter.toString());
  }

  /**
   * Existing handlers keep working: {@code eval(ScriptContext)} still declares only {@link LarkyEvaluationScriptException}
   * (this method compiles only if so), and a refusal is one.
   */
  @Test
  public void aRefusalReachesAHandlerOfEvaluationFailures() {
    final LarkyCompiledScript compiled;
    try {
      compiled = (LarkyCompiledScript) refusingWithCleanContext().compile("out = 1\n");
    } catch (ScriptException e) {
      throw new AssertionError(e);
    }
    LarkyEvaluationScriptException caught = null;
    try {
      compiled.eval(withGlobal());
    } catch (LarkyEvaluationScriptException e) {
      caught = e;
    }
    assertTrue(String.valueOf(caught), caught instanceof LarkyGlobalScopeRefusedException);
  }

  private static LarkyScriptEngine refusingWithCleanContext() {
    final LarkyScriptEngine engine = refusing();
    engine.setContext(clean());
    return engine;
  }

  @Test
  public void aRefusingEngineRefusesToCompileWithAGlobalScope() {
    final LarkyScriptEngine engine = refusing();
    engine.setContext(withGlobal());
    assertRefused(assertThrows(ScriptException.class, () -> engine.compile("out = 1\n")));
  }

  @Test
  public void aRefusingEngineRefusesToEvalWithAGlobalScope() {
    final LarkyScriptEngine engine = refusing();
    // the context passed to eval
    assertRefused(assertThrows(ScriptException.class, () -> engine.eval("out = shared\n", withGlobal())));

    // the engine's own context, whose global scope a ScriptEngineManager sets
    engine.setContext(withGlobal());
    final Bindings engineScope = new SimpleBindings();
    assertRefused(assertThrows(ScriptException.class, () -> engine.eval("out = shared\n", engineScope)));
    assertFalse("the refused script never ran", engineScope.containsKey("out"));
  }

  @Test
  public void aRefusingEngineRefusesToEvalACompiledScriptWithAGlobalScope() throws Exception {
    final LarkyScriptEngine engine = refusing();
    engine.setContext(clean());
    final LarkyCompiledScript compiled = (LarkyCompiledScript) engine.compile("out = shared\n");
    final ScriptContext context = withGlobal();

    assertRefused(assertThrows(ScriptException.class, () -> compiled.eval(context)));
    assertFalse("the refused script never ran", context.getBindings(ScriptContext.ENGINE_SCOPE).containsKey("out"));
  }

  @Test
  public void aRefusingEngineTakesAnEmptyGlobalScopeAndAScriptNeverSeesALaterOne() throws Exception {
    // a ScriptEngineManager gives every engine its global scope, empty until something is put in it
    final ScriptEngineManager manager = new ScriptEngineManager();
    final LarkyScriptEngine engine = refusing();
    engine.setContext(clean());
    engine.setBindings(manager.getBindings(), ScriptContext.GLOBAL_SCOPE);

    final ParsedStarFile result = (ParsedStarFile) engine.eval("out = 1\n", new SimpleBindings());
    assertEquals("1", String.valueOf(result.getGlobalEnvironmentVariable("out", Object.class)));

    // a name only the global scope would give is not defined for the script
    final ScriptException undefined =
        assertThrows(ScriptException.class, () -> engine.eval("out = shared\n", new SimpleBindings()));
    assertFalse(undefined instanceof LarkyGlobalScopeRefusedException);
    assertTrue(undefined.getMessage(), undefined.getMessage().contains("shared"));

    // once anything is put in the manager, the engine refuses rather than share it
    manager.put("shared", "global value");
    assertRefused(assertThrows(ScriptException.class, () -> engine.eval("out = shared\n", new SimpleBindings())));
  }

  @Test
  public void aRefusingEngineStillReadsItsSettingsFromTheEngineScope() throws Exception {
    final LarkyScriptEngine engine = refusing();
    final ScriptContext context = clean();
    context.setAttribute(LarkyScriptEngine.MODULES, Map.of("helper.star", "def one():\n    return 1\n"),
        ScriptContext.ENGINE_SCOPE);
    engine.setContext(context);

    final ParsedStarFile result = (ParsedStarFile) engine.eval(String.join("\n",
        "load('helper', 'one')",
        "out = one()",
        ""), new SimpleBindings());

    assertEquals("1", String.valueOf(result.getGlobalEnvironmentVariable("out", Object.class)));
  }
}
