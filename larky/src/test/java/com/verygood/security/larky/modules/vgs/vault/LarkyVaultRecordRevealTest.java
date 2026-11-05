package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import java.util.List;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.Test;

/** Behaviour of the default {@code LarkyVault.reveal(value, storage, record)} method. */
public class LarkyVaultRecordRevealTest {

    /** An implementation written before the record overload existed: only the 2-arg reveal. */
    private static class LegacyVault implements LarkyVault {
        @Override
        public Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) {
            return value;
        }

        @Override
        public Object reveal(Object value, Object storage) {
            return "revealed:" + value;
        }

        @Override
        public void delete(Object value, Object storage) {}

        @Override
        public Object sign(String keyId, String message, String algorithm) {
            return message;
        }

        @Override
        public Object verify(String keyId, String message, String signature, String algorithm) {
            return false;
        }
    }

    @Test
    public void legacyImplementationCompilesAndDelegatesWhenRecordIsNone() throws Exception {
        LarkyVault vault = new LegacyVault();
        assertEquals("revealed:alias", vault.reveal("alias", Starlark.NONE, Starlark.NONE));
    }

    @Test
    public void legacyImplementationDelegatesWhenRecordIsJavaNull() throws Exception {
        LarkyVault vault = new LegacyVault();
        assertEquals("revealed:alias", vault.reveal("alias", Starlark.NONE, null));
    }

    @Test
    public void legacyImplementationRejectsARecord() {
        LarkyVault vault = new LegacyVault();
        EvalException e = assertThrows(EvalException.class,
                () -> vault.reveal("alias", Starlark.NONE, "record-1"));
        assertEquals("this vault does not support record-scoped reveal", e.getMessage());
    }
}
