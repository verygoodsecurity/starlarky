package com.verygood.security.larky.modules.vgs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.jsr223.LarkyCompiledScript;
import com.verygood.security.larky.jsr223.LarkyLoadedScript;
import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import com.verygood.security.larky.parser.LarkyEvaluator;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.junit.Test;

/**
 * A host gives a run its own {@code @vgs//vault}; without one the vault found with ServiceLoader answers, which in
 * these tests is the in-memory {@code DefaultVault}.
 */
public class ModuleProvidersTest {

  /** A vault whose every answer names it, so a test can tell which vault a call reached. */
  private record NamedVault(String name) implements LarkyVault {

    @Override
    public Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) {
      return name + "-alias";
    }

    @Override
    public Object reveal(Object value, Object storage) {
      return name + ":" + value + (storage instanceof String s ? "@" + s : "");
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

  private static ModuleProviders vault(String name) {
    return ModuleProviders.of().with(LarkyVault.class, new NamedVault(name));
  }

  private static Object eval(Map<String, Object> bindings, String... lines) throws ScriptException {
    final Bindings engineBindings = new SimpleBindings();
    engineBindings.putAll(bindings);
    final ParsedStarFile result = (ParsedStarFile) new LarkyScriptEngine()
        .eval(String.join("\n", lines) + "\n", engineBindings);
    return result.getGlobalEnvironmentVariable("out", Object.class);
  }

  private static LarkyLoadedScript loadOnce(String... lines) throws ScriptException {
    final ScriptContext context = new SimpleScriptContext();
    context.setWriter(new StringWriter());
    context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
    return ((LarkyCompiledScript) new LarkyScriptEngine().compile(String.join("\n", lines) + "\n"))
        .load(context, Set.of());
  }

  @Test
  public void aRunGivenAVaultRevealsThroughIt() throws Exception {
    assertEquals("given:tok", eval(Map.of(LarkyEvaluator.MODULE_PROVIDERS, vault("given")),
        "load('@vgs//vault', 'vault')",
        "out = vault.reveal('tok')"));
  }

  @Test
  public void aRunWithoutOneUsesTheServiceLoaderVault() throws Exception {
    assertEquals("4111111111111111", eval(Map.of(),
        "load('@vgs//vault', 'vault')",
        "out = vault.reveal(vault.redact('4111111111111111'))"));
  }

  @Test
  public void theModuleStillChecksTheArgumentsForAGivenVault() {
    final ScriptException e = assertThrows(ScriptException.class,
        () -> eval(Map.of(LarkyEvaluator.MODULE_PROVIDERS, vault("given")),
            "load('@vgs//vault', 'vault')",
            "out = vault.reveal('tok', storage='bogus')"));
    assertTrue(e.getMessage(), e.getMessage().contains("Storage 'bogus' not found"));
  }

  @Test
  public void theGivenVaultGetsTheStorageInLowercaseAsTheModuleAlwaysPassedIt() throws Exception {
    assertEquals("given:tok@volatile", eval(Map.of(LarkyEvaluator.MODULE_PROVIDERS, vault("given")),
        "load('@vgs//vault', 'vault')",
        "out = vault.reveal('tok', storage='VOLATILE')"));
  }

  @Test
  public void aScriptLoadedOnceRevealsThroughEachCallsOwnVault() throws Exception {
    final LarkyLoadedScript loaded = loadOnce(
        "load('@vgs//vault', 'vault')",
        "def process(alias):",
        "    return vault.reveal(alias)");

    assertEquals("first:t", loaded.invoke("process", Map.of(LarkyEvaluator.MODULE_PROVIDERS, vault("first")), "t"));
    assertEquals("second:t", loaded.invoke("process", Map.of(LarkyEvaluator.MODULE_PROVIDERS, vault("second")), "t"));
    // a call without providers gets the ServiceLoader vault, not the previous call's
    assertEquals("t", loaded.invoke("process", Map.of(), "t"));
  }

  @Test
  public void javaCallersOfTheModuleStillGetTheServiceLoaderVault() throws Exception {
    final com.verygood.security.larky.modules.VaultModule module = new com.verygood.security.larky.modules.VaultModule();
    assertEquals("plain", module.reveal("plain", net.starlark.java.eval.Starlark.NONE));
  }

  @Test
  public void onlyTheModulesProviderTypesAreTaken() {
    assertThrows(IllegalArgumentException.class, () -> ModuleProviders.of().with(String.class, "x"));
    assertThrows(IllegalArgumentException.class, () -> ModuleProviders.of().with(LarkyVault.class, null));
  }

  @Test
  public void aBindingThatIsNotProvidersIsRefused() throws Exception {
    final LarkyLoadedScript loaded = loadOnce("def process():", "    return 1");
    assertThrows(IllegalArgumentException.class,
        () -> loaded.invoke("process", Map.of(LarkyEvaluator.MODULE_PROVIDERS, "not providers")));
  }

  @Test
  public void givingATypeAgainReplacesIt() {
    final ModuleProviders providers = vault("first").with(LarkyVault.class, new NamedVault("second"));
    assertEquals("second", ((NamedVault) providers.get(LarkyVault.class).orElseThrow()).name());
  }
}
