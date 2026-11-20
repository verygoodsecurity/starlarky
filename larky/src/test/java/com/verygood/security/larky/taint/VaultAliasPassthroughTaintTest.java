package com.verygood.security.larky.taint;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.modules.vgs.ModuleProviders;
import com.verygood.security.larky.modules.vgs.vault.defaults.DefaultVault;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import com.verygood.security.larky.parser.LarkyEvaluator;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.util.List;
import javax.script.SimpleBindings;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.SecretTaint;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.TaintMode;
import net.starlark.java.eval.TaintObserver;
import org.junit.Test;

public class VaultAliasPassthroughTaintTest {

  private static final class BatchVault extends DefaultVault {
    @Override
    public Object reveal(Object value, Object storage) throws EvalException {
      if (value instanceof List<?> aliases) {
        return aliases.stream().map(BatchVault::revealAlias).toList();
      }
      return revealAlias(value);
    }

    private static Object revealAlias(Object alias) {
      return "known-alias".equals(alias) ? new String("synthetic reveal result") : alias;
    }
  }

  @Test
  public void defaultVaultMissLeavesRunInactive() throws Exception {
    SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, TaintObserver.NOOP);
    String alias = new String("missing-alias");
    ParsedStarFile file = run(new DefaultVault(), taint, alias, "revealed = vault.reveal(alias)");

    assertSame(alias, global(file, "revealed"));
    assertFalse(taint.isActive());
    assertFalse(taint.containsSecretValue(alias));
  }

  @Test
  public void unchangedBatchLeavesRunInactive() throws Exception {
    SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, TaintObserver.NOOP);
    String alias = new String("missing-alias");
    ParsedStarFile file = run(
        new BatchVault(), taint, alias, "revealed = vault.reveal([alias, 'another-alias'])");

    assertSame(alias, ((List<?>) global(file, "revealed")).get(0));
    assertFalse(taint.isActive());
    assertFalse(taint.containsSecretValue(alias));
  }

  @Test
  public void mixedBatchRegistersOnlyRevealedValues() throws Exception {
    SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, TaintObserver.NOOP);
    String alias = new String("missing-alias");
    ParsedStarFile file = run(
        new BatchVault(), taint, alias, "revealed = vault.reveal([alias, 'known-alias'])");
    List<?> revealed = (List<?>) global(file, "revealed");

    assertTrue(taint.isActive());
    assertSame(alias, revealed.get(0));
    assertFalse(taint.isTainted(revealed.get(0)));
    assertFalse(taint.containsSecretValue(alias));
    assertTrue(taint.isTainted(revealed.get(1)));
  }

  @Test
  public void equalButDistinctRevealIsTracked() throws Exception {
    LarkyVault vault = new DefaultVault() {
      @Override
      public Object reveal(Object value, Object storage) {
        return new String((String) value);
      }
    };
    SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, TaintObserver.NOOP);
    String alias = new String("synthetic-alias");
    ParsedStarFile file = run(vault, taint, alias, "revealed = vault.reveal(alias)");

    Object revealed = global(file, "revealed");
    assertNotSame(alias, revealed);
    assertTrue(taint.isTainted(revealed));
  }

  @Test
  public void passthroughPreservesAnExistingTag() throws Exception {
    SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, TaintObserver.NOOP);
    ParsedStarFile file = run(new BatchVault(), taint, "known-alias",
        "revealed = vault.reveal(alias)\nagain = vault.reveal(revealed)");

    assertSame(global(file, "revealed"), global(file, "again"));
    assertTrue(taint.isTainted(global(file, "again")));
  }

  @Test
  public void immutableSharedSourceValueMarksTheRunSecretDependent() throws Exception {
    LarkyVault vault = new DefaultVault() {
      @Override
      public Object reveal(Object value, Object storage) {
        return Boolean.TRUE;
      }
    };
    SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, TaintObserver.NOOP);
    run(vault, taint, "known-alias", "revealed = vault.reveal(alias)");

    assertTrue(taint.isActive());
    assertTrue(taint.isSecretDependent());
  }

  @Test
  public void inPlaceRevealTracksTheChangedSlot() throws Exception {
    LarkyVault vault = new DefaultVault() {
      @Override
      @SuppressWarnings("unchecked")
      public Object reveal(Object value, Object storage) throws EvalException {
        StarlarkList<Object> aliases = (StarlarkList<Object>) value;
        aliases.setElementAt(0, "synthetic reveal result");
        return aliases;
      }
    };
    SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, TaintObserver.NOOP);
    ParsedStarFile file = run(vault, taint, "known-alias",
        "revealed = vault.reveal([alias, 'missing-alias'])");
    List<?> revealed = (List<?>) global(file, "revealed");

    assertTrue(taint.isTainted(revealed.get(0)));
    assertFalse(taint.isTainted(revealed.get(1)));
  }

  private static ParsedStarFile run(LarkyVault vault, SecretTaint taint, String alias, String script)
      throws Exception {
    SimpleBindings bindings = new SimpleBindings();
    bindings.put(
        LarkyEvaluator.MODULE_PROVIDERS, ModuleProviders.of().with(LarkyVault.class, vault));
    bindings.put(SecretTaint.BINDING_NAME, taint);
    bindings.put("alias", alias);
    return (ParsedStarFile) new LarkyScriptEngine().eval("load('@vgs//vault', 'vault')\n" + script + "\n",
        bindings);
  }

  private static Object global(ParsedStarFile file, String name) {
    return file.getGlobalEnvironmentVariable(name, Object.class);
  }
}
