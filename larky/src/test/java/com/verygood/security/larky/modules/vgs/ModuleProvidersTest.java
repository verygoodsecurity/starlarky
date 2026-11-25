package com.verygood.security.larky.modules.vgs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.jsr223.LarkyScriptContext;
import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import com.verygood.security.larky.runtime.EvaluationSettings;
import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.runtime.LarkyEvaluator;
import java.util.List;
import java.util.Map;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
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

  // As a JSR-223 host does: the providers in the settings of the engine's LarkyScriptContext.
  private static Object eval(ModuleProviders providers, String... lines) throws ScriptException {
    final LarkyScriptEngine engine = new LarkyScriptEngine();
    final LarkyScriptContext context = new LarkyScriptContext();
    context.setSettings(EvaluationSettings.builder().moduleProviders(providers).build());
    engine.setContext(context);
    final ParsedStarFile result = (ParsedStarFile) engine.eval(String.join("\n", lines) + "\n", new SimpleBindings());
    return result.getGlobalEnvironmentVariable("out", Object.class);
  }

  @Test
  public void aRunGivenAVaultRevealsThroughIt() throws Exception {
    assertEquals("given:tok", eval(vault("given"),
        "load('@vgs//vault', 'vault')",
        "out = vault.reveal('tok')"));
  }

  @Test
  public void aRunGivenAVaultThroughLarkyScriptRevealsThroughIt() throws Exception {
    final ParsedStarFile result = new LarkyScript(
        ModuleSupplier.CORE_MODULES,
        LarkyScript.StarlarkMode.STRICT,
        Map.of(),
        EvaluationSettings.builder().moduleProviders(vault("given")).build())
        .evaluate(InMemMapBackedStarFile.createStarFile("main.star",
            "load('@vgs//vault', 'vault')\nout = vault.reveal('tok')\n"), new TestingConsole());
    assertEquals("given:tok", result.getGlobalEnvironmentVariable("out", Object.class));
  }

  @Test
  public void aRunWithoutOneUsesTheServiceLoaderVault() throws Exception {
    assertEquals("4111111111111111", eval(ModuleProviders.NONE,
        "load('@vgs//vault', 'vault')",
        "out = vault.reveal(vault.redact('4111111111111111'))"));
  }

  @Test
  public void theModuleStillChecksTheArgumentsForAGivenVault() {
    final ScriptException e = assertThrows(ScriptException.class,
        () -> eval(vault("given"),
            "load('@vgs//vault', 'vault')",
            "out = vault.reveal('tok', storage='bogus')"));
    assertTrue(e.getMessage(), e.getMessage().contains("Storage 'bogus' not found"));
  }

  @Test
  public void theGivenVaultGetsTheStorageInLowercaseAsTheModuleAlwaysPassedIt() throws Exception {
    assertEquals("given:tok@volatile", eval(vault("given"),
        "load('@vgs//vault', 'vault')",
        "out = vault.reveal('tok', storage='VOLATILE')"));
  }

  @Test
  public void eachEvaluationRevealsThroughItsOwnVault() throws Exception {
    final String[] script = {"load('@vgs//vault', 'vault')", "out = vault.reveal('t')"};

    assertEquals("first:t", eval(vault("first"), script));
    assertEquals("second:t", eval(vault("second"), script));
    // an evaluation without providers gets the ServiceLoader vault, not the previous evaluation's
    assertEquals("t", eval(ModuleProviders.NONE, script));
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
  public void givingATypeAgainReplacesIt() {
    final ModuleProviders providers = vault("first").with(LarkyVault.class, new NamedVault("second"));
    assertEquals("second", ((NamedVault) providers.get(LarkyVault.class).orElseThrow()).name());
  }
}
