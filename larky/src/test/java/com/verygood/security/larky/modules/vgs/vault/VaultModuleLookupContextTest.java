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

/** VaultModule reveal/delete lookup context (entity_key, kind): validation and pass-through to the vault SPI. */
public class VaultModuleLookupContextTest {

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

    private static final Object NONE = Starlark.NONE;

    @Test
    public void emptyEntityKeyIsRejected() {
        VaultModule module = new VaultModule();
        EvalException e = assertThrows(EvalException.class,
                () -> module.reveal("alias", NONE, "", NONE));
        assertEquals("entity_key must not be empty or blank", e.getMessage());
    }

    @Test
    public void blankContextIsRejectedOnRevealAndDelete() {
        VaultModule module = new VaultModule();
        for (String blank : new String[] {"", " ", "   ", "\t", "\n", " \t\n "}) {
            EvalException r = assertThrows(EvalException.class,
                    () -> module.reveal("alias", NONE, blank, NONE));
            assertEquals("entity_key must not be empty or blank", r.getMessage());
            EvalException d = assertThrows(EvalException.class,
                    () -> module.delete("alias", NONE, blank, NONE));
            assertEquals("entity_key must not be empty or blank", d.getMessage());
            EvalException rk = assertThrows(EvalException.class,
                    () -> module.reveal("alias", NONE, NONE, blank));
            assertEquals("kind must not be empty or blank", rk.getMessage());
            EvalException dk = assertThrows(EvalException.class,
                    () -> module.delete("alias", NONE, NONE, blank));
            assertEquals("kind must not be empty or blank", dk.getMessage());
        }
    }

    @Test
    public void nonStringContextFromJavaCallersIsRejected() {
        VaultModule module = new VaultModule();
        assertThrows(EvalException.class, () -> module.reveal("alias", NONE, 1, NONE));
        assertThrows(EvalException.class, () -> module.delete("alias", NONE, 1, NONE));
        assertThrows(EvalException.class, () -> module.reveal("alias", NONE, List.of("a"), NONE));
        assertThrows(EvalException.class, () -> module.reveal("alias", NONE, NONE, 1));
        assertThrows(EvalException.class, () -> module.delete("alias", NONE, NONE, 1));
    }

    @Test
    public void anEntityKeyWithSurroundingSpacesButContentIsAccepted() throws Exception {
        VaultModule module = new VaultModule();
        String alias = (String) module.redact("example-secret", NONE, NONE,
                Collections.emptyList(), ImmutableMap.of("entity_key", " entity-a "));
        assertEquals("example-secret", module.reveal(alias, NONE, " entity-a ", NONE));
        assertEquals(alias, module.reveal(alias, NONE, "entity-a", NONE));
    }

    @Test
    public void entityKeyRoundTripThroughTheModule() throws Exception {
        VaultModule module = new VaultModule();
        String alias = (String) module.redact("example-secret", NONE, NONE,
                Collections.emptyList(), ImmutableMap.of("kind", "example", "entity_key", "entity-a"));

        assertEquals("example-secret", module.reveal(alias, NONE, "entity-a", NONE));
        assertEquals(alias, module.reveal(alias, NONE, NONE, NONE));
        assertEquals(alias, module.reveal(alias, NONE));
        assertNotEquals("example-secret", module.reveal(alias, NONE, "entity-b", NONE));
        // kind was a generation input only: naming it asks for a scope the redact did not use.
        assertEquals(alias, module.reveal(alias, NONE, "entity-a", "example"));
    }

    @Test
    public void scopedKindRoundTripThroughTheModule() throws Exception {
        VaultModule module = new VaultModule();
        String alias = (String) module.redact("example-secret", NONE, NONE, Collections.emptyList(),
                ImmutableMap.of("kind", "example", "entity_key", "entity-a", "scope_kind", true));

        assertEquals("example-secret", module.reveal(alias, NONE, "entity-a", "example"));
        assertEquals(alias, module.reveal(alias, NONE, "entity-a", NONE));
        assertEquals(alias, module.reveal(alias, NONE, NONE, "example"));
        module.delete(alias, NONE, "entity-a", NONE);
        assertEquals("example-secret", module.reveal(alias, NONE, "entity-a", "example"));
        module.delete(alias, NONE, "entity-a", "example");
        assertEquals(alias, module.reveal(alias, NONE, "entity-a", "example"));
    }

    @Test
    public void storageIsValidatedBeforeTheVaultIsCalled() {
        VaultModule module = new VaultModule();
        assertThrows(EvalException.class, () -> module.reveal("alias", "unknown", "entity-a", NONE));
    }

    /** A provider written against the two-argument SPI, before the lookup-context overloads existed. */
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
    public void aLegacyProviderServesPlainCallsAndRejectsContextThroughTheModule() throws Exception {
        Path config = Paths.get("target", "test-classes", "META-INF", "services",
                "com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault");
        String saved = new String(Files.readAllBytes(config), StandardCharsets.UTF_8);
        try {
            Files.write(config, LegacyProvider.class.getName().getBytes(StandardCharsets.UTF_8));
            System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "false");
            VaultModule module = new VaultModule();
            assertEquals(LegacyProvider.class, module.getVault().getClass());

            assertEquals("plain:a", module.reveal("a", NONE));
            assertEquals("plain:a", module.reveal("a", NONE, NONE, NONE));
            module.delete("a", NONE);
            module.delete("b", NONE, NONE, NONE);
            assertEquals(List.of("a", "b"), LegacyProvider.DELETED);

            EvalException r = assertThrows(EvalException.class,
                    () -> module.reveal("a", NONE, "entity-a", NONE));
            assertEquals("this vault does not support reveal with entity_key or kind", r.getMessage());
            EvalException rk = assertThrows(EvalException.class,
                    () -> module.reveal("a", NONE, NONE, "example"));
            assertEquals("this vault does not support reveal with entity_key or kind", rk.getMessage());
            EvalException d = assertThrows(EvalException.class,
                    () -> module.delete("a", NONE, "entity-a", NONE));
            assertEquals("this vault does not support delete with entity_key or kind", d.getMessage());
            assertEquals(List.of("a", "b"), LegacyProvider.DELETED);
        } finally {
            Files.write(config, saved.getBytes(StandardCharsets.UTF_8));
            System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "true");
            LegacyProvider.DELETED.clear();
        }
    }

    @Test
    public void deleteWithEntityKeyRoundTripThroughTheModule() throws Exception {
        VaultModule module = new VaultModule();
        String alias = (String) module.redact("example-secret", NONE, NONE,
                Collections.emptyList(), ImmutableMap.of("entity_key", "entity-a"));

        module.delete(alias, NONE, "entity-b", NONE);
        assertEquals("example-secret", module.reveal(alias, NONE, "entity-a", NONE));
        module.delete(alias, NONE);
        assertEquals("example-secret", module.reveal(alias, NONE, "entity-a", NONE));
        module.delete(alias, NONE, "entity-a", NONE);
        assertEquals(alias, module.reveal(alias, NONE, "entity-a", NONE));
    }

    @Test
    public void deleteRejectsAnEmptyEntityKeyAndAnUnknownStorage() {
        VaultModule module = new VaultModule();
        EvalException e = assertThrows(EvalException.class,
                () -> module.delete("alias", NONE, "", NONE));
        assertEquals("entity_key must not be empty or blank", e.getMessage());
        assertThrows(EvalException.class, () -> module.delete("alias", "unknown", "entity-a", NONE));
    }
}
