package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.modules.vgs.ModuleProviders;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import com.verygood.security.larky.parser.LarkyEvaluator;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.Test;

/**
 * {@code vault.redact(alias=...)}, {@code fake.reveal} and {@code fake.delete} from a script, through a
 * per-run vault given in {@link ModuleProviders}. A script call goes through {@code VaultModule.redactForRun}
 * (and {@code revealForRun} / {@code deleteForRun}), which resolve the run's provider from the thread; {@link
 * VaultModuleAliasTest} drives only the Java entry point.
 */
public class VaultModuleAliasScriptTest {

    /** Records every call it receives, per instance, so each run has its own record. */
    private static final class CapturingVault implements LarkyVault {
        final List<Object[]> redacts = new ArrayList<>();
        final List<Object[]> reveals = new ArrayList<>();
        final List<Object[]> deletes = new ArrayList<>();

        @Override
        public Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) {
            redacts.add(new Object[] {value, storage, format, tags, decoratorConfig});
            return "alias-" + redacts.size();
        }

        @Override
        public Object reveal(Object value, Object storage) {
            return reveal(value, storage, Starlark.NONE, Starlark.NONE);
        }

        @Override
        public Object reveal(Object value, Object storage, Object entityKey, Object kind) {
            reveals.add(new Object[] {value, storage, entityKey, kind});
            return "revealed:" + value;
        }

        @Override
        public void delete(Object value, Object storage) {
            delete(value, storage, Starlark.NONE, Starlark.NONE);
        }

        @Override
        public void delete(Object value, Object storage, Object entityKey, Object kind) {
            deletes.add(new Object[] {value, storage, entityKey, kind});
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

    private static ParsedStarFile run(CapturingVault vault, String... lines) throws ScriptException {
        Bindings bindings = new SimpleBindings();
        bindings.put(LarkyEvaluator.MODULE_PROVIDERS, ModuleProviders.of().with(LarkyVault.class, vault));
        return (ParsedStarFile) new LarkyScriptEngine().eval(String.join("\n", lines) + "\n", bindings);
    }

    private static Map<?, ?> asMap(Object config) {
        return new HashMap<>((Map<?, ?>) config);
    }

    @Test
    public void redactWithAliasReachesTheRunVaultAsTheLongFormDoes() throws Exception {
        CapturingVault vault = new CapturingVault();
        ParsedStarFile result = run(vault,
                "load('@vgs//synthetic', 'Faker')",
                "load('@vgs//vault', 'vault')",
                "fake = Faker('en_GB', entity_key='entity-a', email_domain='example.org')",
                "spec = fake.email()",
                "short = vault.redact('x@example.net', storage='VOLATILE', tags=['t1'], alias=spec)",
                "long = vault.redact('x@example.net', storage='VOLATILE', tags=['t1'], format='SYNTHETIC',",
                "    decorator_config=dict(spec.decorator_config))");

        assertEquals("alias-1", result.getGlobalEnvironmentVariable("short", Object.class));
        assertEquals("alias-2", result.getGlobalEnvironmentVariable("long", Object.class));
        assertEquals(2, vault.redacts.size());
        Object[] viaAlias = vault.redacts.get(0);
        Object[] viaLongForm = vault.redacts.get(1);
        assertEquals("x@example.net", viaAlias[0]);
        assertEquals("volatile", viaAlias[1]);
        assertEquals("SYNTHETIC", viaAlias[2]);
        assertEquals(List.of("t1"), new ArrayList<>((List<?>) viaAlias[3]));
        assertEquals(ImmutableMap.of("kind", "email", "entity_key", "entity-a", "country", "GB",
                "scope_kind", false, "email_domain", "example.org"), asMap(viaAlias[4]));
        for (int i = 0; i < 3; i++) {
            assertEquals(viaLongForm[i], viaAlias[i], "argument " + i);
        }
        assertEquals(new ArrayList<>((List<?>) viaLongForm[3]), new ArrayList<>((List<?>) viaAlias[3]));
        assertEquals(asMap(viaLongForm[4]), asMap(viaAlias[4]));
    }

    @Test
    public void theScopeKindChoiceReachesTheRunVault() throws Exception {
        CapturingVault vault = new CapturingVault();
        run(vault,
                "load('@vgs//synthetic', 'Faker')",
                "load('@vgs//vault', 'vault')",
                "vault.redact('Leeds', alias=Faker('GB', entity_key='entity-a', scope_kind=True).city())");

        assertEquals(1, vault.redacts.size());
        assertEquals(ImmutableMap.of("kind", "city", "entity_key", "entity-a", "country", "GB", "scope_kind", true),
                asMap(vault.redacts.get(0)[4]));
    }

    @Test
    public void fakeRevealAndDeleteSendOnlyTheEntityKeyByDefault() throws Exception {
        CapturingVault vault = new CapturingVault();
        ParsedStarFile result = run(vault,
                "load('@vgs//synthetic', 'Faker')",
                "fake = Faker('en_GB', entity_key='entity-a')",
                "out = fake.reveal('alias-1')",
                "fake.delete('alias-1', storage='volatile')");

        assertEquals("revealed:alias-1", result.getGlobalEnvironmentVariable("out", Object.class));
        assertEquals(1, vault.reveals.size());
        assertEquals(List.of("alias-1", Starlark.NONE, "entity-a", Starlark.NONE), List.of(vault.reveals.get(0)));
        assertEquals(1, vault.deletes.size());
        assertEquals(List.of("alias-1", "volatile", "entity-a", Starlark.NONE), List.of(vault.deletes.get(0)));
    }

    @Test
    public void fakeRevealAndDeleteSendTheKindWhenItIsScope() throws Exception {
        CapturingVault vault = new CapturingVault();
        run(vault,
                "load('@vgs//synthetic', 'Faker')",
                "fake = Faker('en_GB', entity_key='entity-a', scope_kind=True)",
                "fake.reveal('alias-1', kind='email')",
                "fake.delete('alias-1', kind='email')");

        assertEquals(List.of("alias-1", Starlark.NONE, "entity-a", "email"), List.of(vault.reveals.get(0)));
        assertEquals(List.of("alias-1", Starlark.NONE, "entity-a", "email"), List.of(vault.deletes.get(0)));
    }

    @Test
    public void aliasCombinedWithFormatNeverReachesTheRunVault() {
        CapturingVault vault = new CapturingVault();
        ScriptException e = assertThrows(ScriptException.class, () -> run(vault,
                "load('@vgs//synthetic', 'Faker')",
                "load('@vgs//vault', 'vault')",
                "vault.redact('Leeds', format='SYNTHETIC', alias=Faker('GB', entity_key='entity-a').city())"));
        assertTrue(e.getMessage().contains("alias cannot be combined with format or decorator_config"),
                e.getMessage());
        assertTrue(vault.redacts.isEmpty());
    }

    @Test
    public void eachRunRedactsThroughItsOwnVault() throws Exception {
        String[] script = {
            "load('@vgs//synthetic', 'Faker')",
            "load('@vgs//vault', 'vault')",
            "out = vault.redact('Leeds', alias=Faker('GB', entity_key='entity-a').city())",
        };
        CapturingVault first = new CapturingVault();
        CapturingVault second = new CapturingVault();
        run(first, script);
        run(second, script);
        assertEquals(1, first.redacts.size());
        assertEquals(1, second.redacts.size());
    }
}
