package com.verygood.security.larky.parser;

import static com.google.common.truth.Truth.assertThat;

import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.modules.types.structs.SimpleStruct;
import com.verygood.security.larky.modules.vgs.ModuleProviders;
import com.verygood.security.larky.modules.vgs.vault.defaults.DefaultVault;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import java.util.List;
import java.util.Map;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.SecretTaint;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.TaintMode;
import org.junit.Test;

public class SecretTaintLifetimeTest {

  private static final String VALUE = "synthetic-value";

  private static LarkyEvaluator evaluator(ModuleCache cache, Map<String, Object> bindings) {
    LarkyScript script =
        new LarkyScript(ModuleSupplier.CORE_MODULES, LarkyScript.StarlarkMode.STRICT, bindings);
    return new LarkyEvaluator(script.withModuleCache(cache), new TestingConsole());
  }

  @Test
  public void evenAnOffBindingIsHostOnlyAndPreservesModuleSharing() throws Exception {
    ModuleCache cache = new ModuleCache(true);
    StarFile library = ResourceContentStarFile.buildStarFile("@stdlib//sets");
    SecretTaint off = new SecretTaint(TaintMode.OFF, null);

    Module first =
        evaluator(cache, Map.of(SecretTaint.BINDING_NAME, off)).eval(library).module();
    assertThat(first.getPredeclaredBindings()).doesNotContainKey(SecretTaint.BINDING_NAME);
    assertThat(evaluator(cache, Map.of()).eval(library).module()).isSameInstanceAs(first);
  }

  @Test
  public void trackingKeepsEvaluatedModulesLocalEvenWhenTheSharedCacheIsEnabled() throws Exception {
    ModuleCache cache = new ModuleCache(true);
    StarFile library = ResourceContentStarFile.buildStarFile("@stdlib//sets");
    SecretTaint observe = new SecretTaint(TaintMode.OBSERVE, null);
    LarkyEvaluator run = evaluator(cache, Map.of(SecretTaint.BINDING_NAME, observe));

    Module observed = run.eval(library).module();
    assertThat(run.eval(library).module()).isSameInstanceAs(observed);
    assertThat(cache.size()).isEqualTo(0);

    Module ordinary = evaluator(cache, Map.of()).eval(library).module();
    SecretTaint enforce = new SecretTaint(TaintMode.ENFORCE, null);
    assertThat(evaluator(cache, Map.of(SecretTaint.BINDING_NAME, enforce)).eval(library).module())
        .isNotSameInstanceAs(ordinary);
    assertThat(evaluator(cache, Map.of()).eval(library).module()).isSameInstanceAs(ordinary);
  }

  @Test
  public void aRetainedEvaluationValueDoesNotKeepTheTaintStateThroughItsThread() throws Exception {
    DefaultVault vault = new DefaultVault();
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, null);
    LarkyEvaluator run =
        evaluator(
            new ModuleCache(false),
            Map.of(
                SecretTaint.BINDING_NAME, taint,
                LarkyEvaluator.MODULE_PROVIDERS, providers(vault),
                "alias", aliasForValue(vault)));
    Module result =
        run.eval(
                InMemMapBackedStarFile.createStarFile(
                    "main.star",
                    "load('@vgs//vault', 'vault')\nholder = _struct(value=vault.reveal(alias))\n"))
            .module();

    assertThat(result.getPredeclaredBindings()).doesNotContainKey(SecretTaint.BINDING_NAME);
    assertDetached(result.getGlobal("holder"));
    assertThat(taint.containsSecretValue(VALUE)).isTrue();
  }

  @Test
  public void aRetainedInvokedValueDoesNotKeepTheTaintStateThroughItsThread() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    engine.eval(
        "load('@vgs//vault', 'vault')\n"
            + "def get_holder(alias):\n"
            + "    return _struct(value=vault.reveal(alias))\n");
    DefaultVault vault = new DefaultVault();
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, null);
    engine.put(LarkyEvaluator.MODULE_PROVIDERS, providers(vault));
    engine.put(SecretTaint.BINDING_NAME, taint);
    Object result = engine.invokeFunction("get_holder", aliasForValue(vault));

    assertDetached(result);
    assertThat(taint.containsSecretValue(VALUE)).isTrue();
  }

  private static ModuleProviders providers(LarkyVault vault) {
    return ModuleProviders.of().with(LarkyVault.class, vault);
  }

  private static Object aliasForValue(DefaultVault vault) throws Exception {
    return vault.redact(VALUE, Starlark.NONE, Starlark.NONE, List.of(), Starlark.NONE);
  }

  private static void assertDetached(Object holder) {
    StarlarkThread thread = ((SimpleStruct) holder).getCurrentThread();
    assertThat(thread.getSecretTaint()).isNull();
    assertThat(thread.getThreadLocal(SecretTaint.class)).isNull();
  }
}
