package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A provider that parses the decorator config with DecoratorConfig sees the SYNTHETIC keys as
 * options, whether they come from alias= or from the long form.
 */
public class VaultModuleDecoratorOptionsTest {

    private static final String DOB = "1987-06-05";

    /** Parses what it is given the way a Knox-backed provider does. */
    public static class ParsingVault implements LarkyVault {
        public static final List<DecoratorConfig> PARSED = new ArrayList<>();
        public static final List<String> FORMATS = new ArrayList<>();

        @Override
        public Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) {
            FORMATS.add(String.valueOf(format));
            PARSED.add(DecoratorConfig.fromObject(decoratorConfig));
            return "alias";
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
    public static void setUp() throws Exception {
        savedProperty = System.getProperty(VaultModule.ENABLE_INMEMORY_PROPERTY);
        System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "true");
        assertNotNull(VaultModule.INSTANCE); // see VaultModuleAliasTest
        savedServices = new String(Files.readAllBytes(SERVICES), StandardCharsets.UTF_8);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        Files.write(SERVICES, savedServices.getBytes(StandardCharsets.UTF_8));
        if (savedProperty == null) {
            System.clearProperty(VaultModule.ENABLE_INMEMORY_PROPERTY);
        } else {
            System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, savedProperty);
        }
    }

    private static VaultModule parsingModule() throws Exception {
        Files.write(SERVICES, ParsingVault.class.getName().getBytes(StandardCharsets.UTF_8));
        System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "false");
        try {
            ParsingVault.PARSED.clear();
            ParsingVault.FORMATS.clear();
            VaultModule module = new VaultModule();
            assertEquals(ParsingVault.class, module.getVault().getClass());
            return module;
        } finally {
            Files.write(SERVICES, savedServices.getBytes(StandardCharsets.UTF_8));
            System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "true");
        }
    }

    private static Object redact(VaultModule m, Object format, Object config, Object alias) throws EvalException {
        return m.redactForStarlark("v", Starlark.NONE, format, Collections.emptyList(), config, alias);
    }

    @Test
    public void aliasAndTheLongFormGiveTheSameOptions() throws Exception {
        VaultModule module = parsingModule();
        AliasSpec spec = SyntheticModule.INSTANCE.aliasSpec("age", "record-a", "GB", Starlark.NONE, false, DOB);
        redact(module, Starlark.NONE, Starlark.NONE, spec);

        Map<String, Object> longForm = new LinkedHashMap<>();
        longForm.put("kind", "age");
        longForm.put("record", "record-a");
        longForm.put("country", "GB");
        longForm.put("dob", DOB);
        redact(module, "SYNTHETIC", Dict.immutableCopyOf(longForm), Starlark.NONE);

        assertEquals(2, ParsingVault.PARSED.size());
        Map<String, String> expected = ImmutableMap.of(
                "kind", "age", "record", "record-a", "country", "GB", "dob", DOB);
        assertEquals(expected, ParsingVault.PARSED.get(0).getOptions());
        assertEquals(expected, ParsingVault.PARSED.get(1).getOptions());
        assertEquals(ParsingVault.PARSED.get(0), ParsingVault.PARSED.get(1));
        assertEquals(List.of("SYNTHETIC", "SYNTHETIC"), ParsingVault.FORMATS);
    }

    @Test
    public void everyOptionReachesTheProvider() throws Exception {
        VaultModule module = parsingModule();
        redact(module, Starlark.NONE, Starlark.NONE,
                SyntheticModule.INSTANCE.aliasSpec("email", "r", "GB", "example.org", false, Starlark.NONE));
        redact(module, Starlark.NONE, Starlark.NONE,
                SyntheticModule.INSTANCE.aliasSpec("phone", "r", "GB", Starlark.NONE, true, Starlark.NONE));

        assertEquals(ImmutableMap.of("kind", "email", "record", "r", "country", "GB", "email_domain", "example.org"),
                ParsingVault.PARSED.get(0).getOptions());
        assertEquals(ImmutableMap.of("kind", "phone", "record", "r", "country", "GB", "allow_unreserved_phone", "true"),
                ParsingVault.PARSED.get(1).getOptions());
    }

    @Test
    public void aLongFormConfigWithoutSyntheticKeysHasNoOptions() throws Exception {
        VaultModule module = parsingModule();
        redact(module, Starlark.NONE, Dict.immutableCopyOf(ImmutableMap.of("searchPattern", "(a)", "replacePattern", "b")), Starlark.NONE);
        assertTrue(ParsingVault.PARSED.get(0).getOptions().isEmpty());
        assertEquals("(a)", ParsingVault.PARSED.get(0).getSearchPattern());
    }

    @Test
    public void aNonScalarOptionFailsWithoutEchoingAnyValue() {
        VaultModule module = new VaultModule();
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("kind", "age");
        config.put("record", "record-private-7");
        config.put("dob", StarlarkList.immutableOf(DOB));
        EvalException e = assertThrows(EvalException.class,
                () -> redact(module, "SYNTHETIC", Dict.immutableCopyOf(config), Starlark.NONE));
        assertEquals("Decorator config is invalid. 'dob' field must be a string, boolean or number", e.getMessage());
    }

    @Test
    public void anotherDecoratorErrorDoesNotEchoSyntheticValuesEither() {
        VaultModule module = new VaultModule();
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("record", "record-private-7");
        config.put("dob", DOB);
        config.put("searchPattern", StarlarkList.empty());
        EvalException e = assertThrows(EvalException.class,
                () -> redact(module, Starlark.NONE, Dict.immutableCopyOf(config), Starlark.NONE));
        assertEquals("Decorator config is invalid. 'searchPattern' field must be string", e.getMessage());
        assertFalse(e.getMessage().contains(DOB));
    }
}
