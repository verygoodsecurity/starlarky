package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.Test;

/** NoopVault rejects every reveal and delete the same way, whatever lookup context is given. */
public class NoopVaultLookupContextTest {

    private static final String MESSAGE = "vault.reveal operation must be overridden";

    private static final Object[][] CONTEXTS = {
        {Starlark.NONE, Starlark.NONE}, {"entity-a", Starlark.NONE}, {Starlark.NONE, "example"}, {"entity-a", "example"}
    };

    @Test
    public void revealWithoutContextMustBeOverridden() {
        EvalException e = assertThrows(EvalException.class,
                () -> new NoopVault().reveal("alias", Starlark.NONE, Starlark.NONE, Starlark.NONE));
        assertEquals(MESSAGE, e.getMessage());
    }

    @Test
    public void revealWithContextMustBeOverridden() {
        for (Object[] context : CONTEXTS) {
            EvalException e = assertThrows(EvalException.class,
                    () -> new NoopVault().reveal("alias", Starlark.NONE, context[0], context[1]));
            assertEquals(MESSAGE, e.getMessage());
        }
    }

    @Test
    public void deleteMustBeOverriddenWithOrWithoutContext() {
        for (Object[] context : CONTEXTS) {
            EvalException e = assertThrows(EvalException.class,
                    () -> new NoopVault().delete("alias", Starlark.NONE, context[0], context[1]));
            assertEquals("vault.delete operation must be overridden", e.getMessage());
        }
    }
}
