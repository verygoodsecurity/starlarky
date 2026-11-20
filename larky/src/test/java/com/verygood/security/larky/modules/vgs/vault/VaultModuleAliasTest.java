package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.VaultModule;
import com.verygood.security.larky.modules.vgs.synthetic.AliasSpec;
import com.verygood.security.larky.modules.vgs.synthetic.SyntheticModule;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** VaultModule.redactForStarlark's alias argument: validation and the call the provider sees. */
public class VaultModuleAliasTest {

    /** Records what the module passes to the provider. */
    public static class CapturingVault implements LarkyVault {
        public static final List<Object[]> REDACTS = new ArrayList<>();

        @Override
        public Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) {
            REDACTS.add(new Object[] {value, storage, format, tags, decoratorConfig});
            return "alias-for-" + value;
        }

        @Override
        public Object reveal(Object value, Object storage) {
            return value;
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

    private static final Path SERVICES = Paths.get("target", "test-classes", "META-INF", "services",
            "com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault");
    private static String savedServices;
    private static String savedProperty;

    @BeforeAll
    public static void installCapturingVault() throws Exception {
        savedProperty = System.getProperty(VaultModule.ENABLE_INMEMORY_PROPERTY);
        System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "true");
        // Scripts use VaultModule.INSTANCE, built when the class first loads: load it with the
        // in-memory vault selected, before the capturing provider is installed.
        assertNotNull(VaultModule.INSTANCE);
        savedServices = new String(Files.readAllBytes(SERVICES), StandardCharsets.UTF_8);
    }

    @AfterAll
    public static void restore() throws Exception {
        Files.write(SERVICES, savedServices.getBytes(StandardCharsets.UTF_8));
        if (savedProperty == null) {
            System.clearProperty(VaultModule.ENABLE_INMEMORY_PROPERTY);
        } else {
            System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, savedProperty);
        }
    }

    private static VaultModule capturingModule() throws Exception {
        Files.write(SERVICES, CapturingVault.class.getName().getBytes(StandardCharsets.UTF_8));
        System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "false");
        try {
            CapturingVault.REDACTS.clear();
            VaultModule module = new VaultModule();
            assertEquals(CapturingVault.class, module.getVault().getClass());
            return module;
        } finally {
            Files.write(SERVICES, savedServices.getBytes(StandardCharsets.UTF_8));
            System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "true");
        }
    }

    private static AliasSpec spec(String kind) throws EvalException {
        return SyntheticModule.INSTANCE.aliasSpec(kind, "record-a", "GB", Starlark.NONE, false, Starlark.NONE);
    }

    private static Object[] onlyCall() {
        assertEquals(1, CapturingVault.REDACTS.size());
        return CapturingVault.REDACTS.get(0);
    }

    @Test
    public void aliasMakesTheSameProviderCallAsTheLongForm() throws Exception {
        VaultModule module = capturingModule();
        AliasSpec spec = SyntheticModule.INSTANCE.aliasSpec(
                "email", "record-a", "GB", "example.org", false, Starlark.NONE);

        Object viaAlias = module.redactForStarlark(
                "x@example.net", Starlark.NONE, Starlark.NONE, Collections.emptyList(), Starlark.NONE, spec);
        Object[] aliasCall = onlyCall();
        CapturingVault.REDACTS.clear();

        Dict<String, Object> longConfig = Dict.immutableCopyOf(new java.util.LinkedHashMap<>(ImmutableMap.of(
                "kind", "email", "record", "record-a", "country", "GB", "email_domain", "example.org")));
        Object viaLongForm = module.redactForStarlark(
                "x@example.net", Starlark.NONE, "SYNTHETIC", Collections.emptyList(), longConfig, Starlark.NONE);
        Object[] longCall = onlyCall();

        assertEquals(viaLongForm, viaAlias);
        assertEquals(longCall.length, aliasCall.length);
        for (int i = 0; i < 4; i++) {
            assertEquals(longCall[i], aliasCall[i], "argument " + i);
        }
        assertEquals("SYNTHETIC", aliasCall[2]);
        assertEquals(new HashMap<>((Map<?, ?>) longCall[4]), new HashMap<>((Map<?, ?>) aliasCall[4]));
        assertTrue(longCall[4] instanceof Dict && aliasCall[4] instanceof Dict);
    }

    @Test
    public void aliasRespectsStorageAndTags() throws Exception {
        VaultModule module = capturingModule();
        module.redactForStarlark("v", "VOLATILE", Starlark.NONE, List.of("t1"), Starlark.NONE, spec("city"));
        Object[] call = onlyCall();
        assertEquals("volatile", call[1]);
        assertEquals(List.of("t1"), call[3]);
    }

    @Test
    public void aliasCannotBeCombinedWithFormatOrDecoratorConfig() throws Exception {
        VaultModule module = capturingModule();
        AliasSpec spec = spec("city");
        for (Object[] c : new Object[][] {
            {"UUID", Starlark.NONE},
            {"SYNTHETIC", Starlark.NONE},
            {Starlark.NONE, Dict.immutableCopyOf(ImmutableMap.of())},
            {"SYNTHETIC", Dict.immutableCopyOf(ImmutableMap.of("kind", "city"))},
        }) {
            EvalException e = assertThrows(EvalException.class, () -> module.redactForStarlark(
                    "v", Starlark.NONE, c[0], Collections.emptyList(), c[1], spec));
            assertEquals("alias cannot be combined with format or decorator_config", e.getMessage());
        }
        assertTrue(CapturingVault.REDACTS.isEmpty());
    }

    @Test
    public void longFormIsUnchangedWithoutAlias() throws Exception {
        VaultModule module = capturingModule();
        module.redactForStarlark("v", Starlark.NONE, Starlark.NONE, Collections.emptyList(), Starlark.NONE, Starlark.NONE);
        Object[] call = onlyCall();
        assertEquals(Starlark.NONE, call[2]);
        assertEquals(Starlark.NONE, call[4]);
        // The five-argument SPI method is still there and still validates.
        assertThrows(EvalException.class, () -> module.redact("v", Starlark.NONE, "NOT_A_FORMAT", Collections.emptyList(), Starlark.NONE));
    }

    @Test
    public void defaultVaultAnswersItsUnsupportedFormatErrorThroughAlias() throws Exception {
        VaultModule module = new VaultModule(); // in-memory vault
        EvalException e = assertThrows(EvalException.class, () -> module.redactForStarlark(
                "v", Starlark.NONE, Starlark.NONE, Collections.emptyList(), Starlark.NONE, spec("city")));
        assertEquals("Format 'SYNTHETIC' is not supported", e.getMessage());
    }
}
