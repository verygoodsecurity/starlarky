package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.Test;

/** NoopVault rejects every reveal and delete the same way, whether or not a record is given. */
public class NoopVaultRecordRevealTest {

    private static final String MESSAGE = "vault.reveal operation must be overridden";

    @Test
    public void revealWithoutRecordMustBeOverridden() {
        EvalException e = assertThrows(EvalException.class,
                () -> new NoopVault().reveal("alias", Starlark.NONE, Starlark.NONE));
        assertEquals(MESSAGE, e.getMessage());
    }

    @Test
    public void revealWithRecordMustBeOverridden() {
        EvalException e = assertThrows(EvalException.class,
                () -> new NoopVault().reveal("alias", Starlark.NONE, "record-a"));
        assertEquals(MESSAGE, e.getMessage());
    }

    @Test
    public void deleteMustBeOverriddenWithOrWithoutRecord() {
        for (Object record : new Object[] {Starlark.NONE, "record-a"}) {
            EvalException e = assertThrows(EvalException.class,
                    () -> new NoopVault().delete("alias", Starlark.NONE, record));
            assertEquals("vault.delete operation must be overridden", e.getMessage());
        }
    }
}
