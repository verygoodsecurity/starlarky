package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import java.util.ArrayList;
import java.util.List;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.Test;

/** Behaviour of the default lookup-context overloads of {@code LarkyVault.reveal} and {@code delete}. */
public class LarkyVaultLookupContextTest {

    /** An implementation written before the lookup-context overloads existed: only the 2-arg methods. */
    private static class LegacyVault implements LarkyVault {
        @Override
        public Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) {
            return value;
        }

        @Override
        public Object reveal(Object value, Object storage) {
            return "revealed:" + value;
        }

        private final List<String> deleted = new ArrayList<>();

        @Override
        public void delete(Object value, Object storage) {
            deleted.add(String.valueOf(value));
        }

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
    public void legacyImplementationCompilesAndDelegatesWhenContextIsNone() throws Exception {
        LarkyVault vault = new LegacyVault();
        assertEquals("revealed:alias", vault.reveal("alias", Starlark.NONE, Starlark.NONE, Starlark.NONE));
    }

    @Test
    public void legacyImplementationDelegatesWhenContextIsJavaNull() throws Exception {
        LarkyVault vault = new LegacyVault();
        assertEquals("revealed:alias", vault.reveal("alias", Starlark.NONE, null, null));
    }

    @Test
    public void legacyImplementationRejectsAnEntityKeyOrAKind() {
        LarkyVault vault = new LegacyVault();
        for (Object[] context : new Object[][] {
                {"entity-1", Starlark.NONE}, {Starlark.NONE, "example"}, {"entity-1", "example"}}) {
            EvalException e = assertThrows(EvalException.class,
                    () -> vault.reveal("alias", Starlark.NONE, context[0], context[1]));
            assertEquals("this vault does not support reveal with entity_key or kind", e.getMessage());
        }
    }

    @Test
    public void legacyDeleteDelegatesWhenContextIsNone() throws Exception {
        LegacyVault vault = new LegacyVault();
        vault.delete("alias", Starlark.NONE, Starlark.NONE, Starlark.NONE);
        vault.delete("alias-2", Starlark.NONE, null, null);
        assertEquals(List.of("alias", "alias-2"), vault.deleted);
    }

    @Test
    public void legacyDeleteRejectsAnEntityKeyOrAKind() {
        LegacyVault vault = new LegacyVault();
        for (Object[] context : new Object[][] {
                {"entity-1", Starlark.NONE}, {Starlark.NONE, "example"}, {"entity-1", "example"}}) {
            EvalException e = assertThrows(EvalException.class,
                    () -> vault.delete("alias", Starlark.NONE, context[0], context[1]));
            assertEquals("this vault does not support delete with entity_key or kind", e.getMessage());
        }
        assertEquals(List.of(), vault.deleted);
    }
}
