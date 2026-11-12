package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.parser.LarkyEvaluator;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.junit.Test;

/**
 * A loaded script is shared by every call, so nothing a call can reach may change: not a value its top level
 * defines, nor anything held by one, by a function's defaults or closure, or by a module it loads.
 */
public class LarkyLoadedScriptIsolationTest {

  private static final String JCRYPTO = "load('@stdlib//jcrypto', 'jcrypto')";

  private static LarkyCompiledScript compile(Map<String, String> modules, String... lines) throws ScriptException {
    return compile(null, modules, lines);
  }

  private static LarkyCompiledScript compile(String cacheNamespace, Map<String, String> modules, String... lines)
      throws ScriptException {
    final LarkyScriptEngine engine = new LarkyScriptEngine();
    if (!modules.isEmpty()) {
      engine.getContext().setAttribute(LarkyScriptEngine.MODULES, modules, ScriptContext.ENGINE_SCOPE);
    }
    if (cacheNamespace != null) {
      engine.getContext().setAttribute(LarkyScriptEngine.CACHE_NAMESPACE, cacheNamespace, ScriptContext.ENGINE_SCOPE);
    }
    return engine.compile(String.join("\n", lines) + "\n", "larky.star");
  }

  private static ScriptContext context(Map<String, Object> bindings) {
    final ScriptContext context = new SimpleScriptContext();
    context.setWriter(new StringWriter());
    context.setBindings(new SimpleBindings(new java.util.HashMap<>(bindings)), ScriptContext.ENGINE_SCOPE);
    return context;
  }

  private static LarkyLoadedScript load(Map<String, String> modules, String... lines) throws ScriptException {
    return compile(modules, lines).load(context(Map.of()), Set.of());
  }

  private static void assertNotLoadable(String reaches, Map<String, String> modules, String... lines) {
    final LarkyScriptNotLoadableException e =
        assertThrows(LarkyScriptNotLoadableException.class, () -> load(modules, lines));
    assertTrue(e.getMessage(), e.getMessage().contains(reaches));
  }

  @Test
  public void aMutableValueInAFrozenListIsRefused() {
    assertNotLoadable("STATE[0]", Map.of(),
        JCRYPTO,
        "STATE = [jcrypto.Hash.SHA256()]",
        "def process(value):",
        "    STATE[0].update(value)",
        "    return STATE[0].hexdigest()");
  }

  @Test
  public void aMutableValueInAFrozenDictIsRefused() {
    assertNotLoadable("STATE[\"h\"]", Map.of(),
        JCRYPTO,
        "STATE = {'h': jcrypto.Hash.SHA256()}",
        "def process(value):",
        "    STATE['h'].update(value)",
        "    return STATE['h'].hexdigest()");
  }

  @Test
  public void aMutableDefaultValueIsRefused() {
    assertNotLoadable("process", Map.of(),
        JCRYPTO,
        "def process(value, digest=jcrypto.Hash.SHA256()):",
        "    digest.update(value)",
        "    return digest.hexdigest()");
  }

  @Test
  public void aMutableValueAFunctionClosesOverIsRefused() {
    assertNotLoadable("process", Map.of(),
        JCRYPTO,
        "def make():",
        "    digest = jcrypto.Hash.SHA256()",
        "    def process(value):",
        "        digest.update(value)",
        "        return digest.hexdigest()",
        "    return process",
        "process = make()");
  }

  @Test
  public void aMethodBoundToAMutableValueIsRefused() {
    assertNotLoadable("update", Map.of(),
        JCRYPTO,
        "update = jcrypto.Hash.SHA256().update",
        "def process(value):",
        "    update(value)");
  }

  @Test
  public void aMutableValueOfAModuleTheScriptLoadsIsRefused() {
    final Map<String, String> modules = Map.of("helper.star", String.join("\n",
        JCRYPTO,
        "_STATE = [jcrypto.Hash.SHA256()]",
        "def digest(value):",
        "    _STATE[0].update(value)",
        "    return _STATE[0].hexdigest()",
        ""));
    assertNotLoadable("_STATE[0]", modules,
        "load('helper', 'digest')",
        "def process(value):",
        "    return digest(value)");
  }

  /** A host's cache namespace makes its modules cacheable, but not one whose values can change. */
  @Test
  public void aMutableValueOfAModuleTheScriptLoadsIsRefusedInACacheNamespace() throws Exception {
    final Map<String, String> modules = Map.of("helper.star", String.join("\n",
        JCRYPTO,
        "_STATE = [jcrypto.Hash.SHA256()]",
        "def digest(value):",
        "    _STATE[0].update(value)",
        "    return _STATE[0].hexdigest()",
        ""));
    for (int attempt = 0; attempt < 2; attempt++) {
      final LarkyCompiledScript script = compile("isolation-test", modules,
          "load('helper', 'digest')",
          "def process(value):",
          "    return digest(value)");
      final LarkyScriptNotLoadableException e = assertThrows(LarkyScriptNotLoadableException.class,
          () -> script.load(context(Map.of()), Set.of()));
      assertTrue(e.getMessage(), e.getMessage().contains("_STATE[0]"));
    }
  }

  /** The reviewer's reproduction: had it loaded, the second call would have hashed b'ab'. */
  @Test
  public void noCallSeesWhatAnEarlierCallDidThroughTheTopLevel() {
    assertThrows(LarkyScriptNotLoadableException.class, () -> load(Map.of(),
        JCRYPTO,
        "STATE = [jcrypto.Hash.SHA256()]",
        "def process(value):",
        "    STATE[0].update(value)",
        "    return STATE[0].hexdigest()"));
  }

  @Test
  public void immutableTopLevelsStillLoad() throws Exception {
    final LarkyLoadedScript loaded = load(Map.of("helper.star", "PREFIX = ('a', 'b')\ndef join(x):\n    return '-'.join(PREFIX) + x\n"),
        "load('@stdlib//json', 'json')",
        "load('@stdlib//larky', 'larky')",
        JCRYPTO,
        "load('helper', 'join')",
        "NAMES = ['x', 'y']",
        "TABLE = {'k': ('v', 1, 2.5, None, True)}",
        "CONFIG = larky.struct(names=NAMES, sep=',')",
        "def process(value, sep=CONFIG.sep):",
        "    digest = jcrypto.Hash.SHA256()",
        "    digest.update(bytes(value, 'utf-8'))",
        "    return sep.join(NAMES) + join(json.encode(TABLE)) + digest.hexdigest()[:4]");

    final Object first = loaded.invoke("process", Map.of(), "ab");
    assertEquals(first, loaded.invoke("process", Map.of(), "ab"));
    assertTrue(String.valueOf(first), String.valueOf(first).startsWith("x,ya-b{"));
  }

  @Test
  public void theLoadObservesItsExpiration() {
    final LarkyCompiledScript script;
    try {
      script = compile(Map.of(),
          "load('@stdlib//larky', 'larky')",
          "def spin(n):",
          "    total = 0",
          "    for i in range(n):",
          "        total += i",
          "    return total",
          "TOTAL = spin(100000)",
          "def process():",
          "    return TOTAL");
    } catch (ScriptException e) {
      throw new AssertionError(e);
    }

    final ScriptException e = assertThrows(ScriptException.class,
        () -> script.load(context(Map.of(LarkyEvaluator.EXPIRATION_MS, 1L)), Set.of()));
    assertTrue(String.valueOf(e.getMessage()), !(e instanceof LarkyScriptNotLoadableException));
  }

  @Test
  public void anExpirationAtLoadDoesNotLimitLaterCalls() throws Exception {
    final LarkyLoadedScript loaded = compile(Map.of(),
        "def spin(n):",
        "    total = 0",
        "    for i in range(n):",
        "        total += i",
        "    return total")
        .load(context(Map.of(LarkyEvaluator.EXPIRATION_MS, System.currentTimeMillis() + 60_000)), Set.of());

    assertEquals(net.starlark.java.eval.StarlarkInt.of(4950), loaded.invoke("spin", Map.of(), 100));
  }
}
