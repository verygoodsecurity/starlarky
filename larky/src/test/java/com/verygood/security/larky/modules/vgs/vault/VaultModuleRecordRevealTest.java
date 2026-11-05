package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.VaultModule;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import java.util.Collections;
import java.util.List;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** VaultModule.reveal's record argument: validation and pass-through to the vault SPI. */
public class VaultModuleRecordRevealTest {

    private static String savedProperty;

    @BeforeAll
    public static void useInMemoryVault() {
        savedProperty = System.getProperty(VaultModule.ENABLE_INMEMORY_PROPERTY);
        System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "true");
    }

    @AfterAll
    public static void restoreProperty() {
        if (savedProperty == null) {
            System.clearProperty(VaultModule.ENABLE_INMEMORY_PROPERTY);
        } else {
            System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, savedProperty);
        }
    }

    @Test
    public void emptyRecordIsRejected() {
        VaultModule module = new VaultModule();
        EvalException e = assertThrows(EvalException.class,
                () -> module.reveal("alias", Starlark.NONE, ""));
        assertEquals("record must not be empty", e.getMessage());
    }

    @Test
    public void recordRoundTripThroughTheModule() throws Exception {
        VaultModule module = new VaultModule();
        String alias = (String) module.redact("example-secret", Starlark.NONE, Starlark.NONE,
                Collections.emptyList(), ImmutableMap.of("kind", "example", "record", "record-a"));

        assertEquals("example-secret", module.reveal(alias, Starlark.NONE, "record-a"));
        assertEquals(alias, module.reveal(alias, Starlark.NONE, Starlark.NONE));
        assertEquals(alias, module.reveal(alias, Starlark.NONE));
        assertNotEquals("example-secret", module.reveal(alias, Starlark.NONE, "record-b"));
    }

    @Test
    public void storageIsValidatedBeforeTheVaultIsCalled() {
        VaultModule module = new VaultModule();
        assertThrows(EvalException.class, () -> module.reveal("alias", "unknown", "record-a"));
    }

    @Test
    public void aVaultWithoutRecordSupportRejectsARecordButServesPlainReveals() throws Exception {
        // A LarkyVault written against the 2-arg SPI, reached through the same default method
        // VaultModule calls on whatever provider is installed.
        LarkyVault legacy = new LarkyVault() {
            @Override
            public Object redact(Object v, Object s, Object f, List<Object> t, Object d) {
                return v;
            }

            @Override
            public Object reveal(Object value, Object storage) {
                return "plain:" + value;
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
        };

        assertEquals("plain:a", legacy.reveal("a", Starlark.NONE, Starlark.NONE));
        EvalException e = assertThrows(EvalException.class,
                () -> legacy.reveal("a", Starlark.NONE, "record-a"));
        assertEquals("this vault does not support record-scoped reveal", e.getMessage());
    }
}
