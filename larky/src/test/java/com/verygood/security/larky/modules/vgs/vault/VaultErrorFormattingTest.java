package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.verygood.security.larky.modules.VaultModule;
import com.verygood.security.larky.modules.vgs.vault.defaults.DefaultVault;
import java.util.Collections;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.Test;

/**
 * Error messages that quote caller input are formatted once: a {@code %} in the input is text, not a
 * format specifier, so the caller gets the usual EvalException instead of a formatter exception.
 */
public class VaultErrorFormattingTest {

    @Test
    public void vaultModuleQuotesAStorageWithAPercentSign() {
        VaultModule module = new VaultModule();
        for (String storage : new String[] {"50%", "%d", "%s%n", "100%'"}) {
            EvalException e = assertThrows(EvalException.class, () -> module.redact(
                    "v", storage, Starlark.NONE, Collections.emptyList(), Starlark.NONE));
            assertEquals("Storage '" + storage + "' not found in supported storage types: [persistent, volatile]",
                    e.getMessage());
            assertThrows(EvalException.class, () -> module.reveal("v", storage));
            assertThrows(EvalException.class, () -> module.delete("v", storage));
        }
    }

    @Test
    public void vaultModuleQuotesAFormatWithAPercentSign() {
        VaultModule module = new VaultModule();
        EvalException e = assertThrows(EvalException.class, () -> module.redact(
                "v", Starlark.NONE, "50%d", Collections.emptyList(), Starlark.NONE));
        assertEquals(true, e.getMessage().startsWith("Format '50%d' not found in supported format types: "),
                e.getMessage());
    }

    @Test
    public void vaultModuleQuotesAnAlgorithmWithAPercentSign() {
        VaultModule module = new VaultModule();
        EvalException e = assertThrows(EvalException.class, () -> module.sign("key", "message", "%s"));
        assertEquals(true, e.getMessage().startsWith("Algorithm '%s' not found in supported algorithms: "),
                e.getMessage());
    }

    @Test
    public void defaultVaultQuotesAStorageAndAFormatWithAPercentSign() {
        DefaultVault vault = new DefaultVault();
        EvalException s = assertThrows(EvalException.class, () -> vault.reveal("v", "50%"));
        assertEquals("Storage '50%' not found in supported storage types: [persistent, volatile]", s.getMessage());
        EvalException f = assertThrows(EvalException.class, () -> vault.redact(
                "v", Starlark.NONE, "%d", Collections.emptyList(), Starlark.NONE));
        assertEquals(true, f.getMessage().startsWith("Format '%d' not found in supported format types: "),
                f.getMessage());
    }
}
