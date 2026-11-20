package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.VaultModule;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
        // VaultModule.INSTANCE, which scripts use, is built when the class first loads. Load it now,
        // with the in-memory vault selected, so no test below decides which vault scripts get.
        assertNotNull(VaultModule.INSTANCE);
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
        assertEquals("record must not be empty or blank", e.getMessage());
    }

    @Test
    public void blankRecordsAreRejectedOnRevealAndDelete() {
        VaultModule module = new VaultModule();
        for (String blank : new String[] {"", " ", "   ", "\t", "\n", " \t\n "}) {
            EvalException r = assertThrows(EvalException.class,
                    () -> module.reveal("alias", Starlark.NONE, blank));
            assertEquals("record must not be empty or blank", r.getMessage());
            EvalException d = assertThrows(EvalException.class,
                    () -> module.delete("alias", Starlark.NONE, blank));
            assertEquals("record must not be empty or blank", d.getMessage());
        }
    }

    @Test
    public void nonStringRecordsFromJavaCallersAreRejected() {
        VaultModule module = new VaultModule();
        assertThrows(EvalException.class, () -> module.reveal("alias", Starlark.NONE, 1));
        assertThrows(EvalException.class, () -> module.delete("alias", Starlark.NONE, 1));
        assertThrows(EvalException.class, () -> module.reveal("alias", Starlark.NONE, List.of("a")));
    }

    @Test
    public void aRecordWithSurroundingSpacesButContentIsAccepted() throws Exception {
        VaultModule module = new VaultModule();
        String alias = (String) module.redact("example-secret", Starlark.NONE, Starlark.NONE,
                Collections.emptyList(), ImmutableMap.of("record", " record-a "));
        assertEquals("example-secret", module.reveal(alias, Starlark.NONE, " record-a "));
        assertEquals(alias, module.reveal(alias, Starlark.NONE, "record-a"));
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

    /** A provider written against the two-argument SPI, before the record overloads existed. */
    public static class LegacyProvider implements LarkyVault {
        public static final List<String> DELETED = new ArrayList<>();

        @Override
        public Object redact(Object v, Object s, Object f, List<Object> t, Object d) {
            return v;
        }

        @Override
        public Object reveal(Object value, Object storage) {
            return "plain:" + value;
        }

        @Override
        public void delete(Object value, Object storage) {
            DELETED.add(String.valueOf(value));
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
    public void aLegacyProviderServesPlainCallsAndRejectsARecordThroughTheModule() throws Exception {
        Path config = Paths.get("target", "test-classes", "META-INF", "services",
                "com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault");
        String saved = new String(Files.readAllBytes(config), StandardCharsets.UTF_8);
        try {
            Files.write(config, LegacyProvider.class.getName().getBytes(StandardCharsets.UTF_8));
            System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "false");
            VaultModule module = new VaultModule();
            assertEquals(LegacyProvider.class, module.getVault().getClass());

            assertEquals("plain:a", module.reveal("a", Starlark.NONE));
            assertEquals("plain:a", module.reveal("a", Starlark.NONE, Starlark.NONE));
            module.delete("a", Starlark.NONE);
            module.delete("b", Starlark.NONE, Starlark.NONE);
            assertEquals(List.of("a", "b"), LegacyProvider.DELETED);

            EvalException r = assertThrows(EvalException.class,
                    () -> module.reveal("a", Starlark.NONE, "record-a"));
            assertEquals("this vault does not support record-scoped reveal", r.getMessage());
            EvalException d = assertThrows(EvalException.class,
                    () -> module.delete("a", Starlark.NONE, "record-a"));
            assertEquals("this vault does not support record-scoped delete", d.getMessage());
            assertEquals(List.of("a", "b"), LegacyProvider.DELETED);
        } finally {
            Files.write(config, saved.getBytes(StandardCharsets.UTF_8));
            System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "true");
            LegacyProvider.DELETED.clear();
        }
    }

    @Test
    public void deleteWithRecordRoundTripThroughTheModule() throws Exception {
        VaultModule module = new VaultModule();
        String alias = (String) module.redact("example-secret", Starlark.NONE, Starlark.NONE,
                Collections.emptyList(), ImmutableMap.of("record", "record-a"));

        module.delete(alias, Starlark.NONE, "record-b");
        assertEquals("example-secret", module.reveal(alias, Starlark.NONE, "record-a"));
        module.delete(alias, Starlark.NONE);
        assertEquals("example-secret", module.reveal(alias, Starlark.NONE, "record-a"));
        module.delete(alias, Starlark.NONE, "record-a");
        assertEquals(alias, module.reveal(alias, Starlark.NONE, "record-a"));
    }

    @Test
    public void deleteRejectsAnEmptyRecordAndAnUnknownStorage() {
        VaultModule module = new VaultModule();
        EvalException e = assertThrows(EvalException.class,
                () -> module.delete("alias", Starlark.NONE, ""));
        assertEquals("record must not be empty or blank", e.getMessage());
        assertThrows(EvalException.class, () -> module.delete("alias", "unknown", "record-a"));
    }
}
