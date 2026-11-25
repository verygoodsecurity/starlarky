package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.vgs.vault.defaults.DefaultVault;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * DefaultVault keys its in-memory store on (entity_key, kind, alias). Each context field is
 * optional, an omitted one adds no scope, and a lookup matches only exactly the redact's context.
 */
public class DefaultVaultLookupContextTest {

    private static final String SECRET = "example-secret";
    private static final Object NONE = Starlark.NONE;

    private DefaultVault vault;

    @BeforeEach
    public void setUp() {
        vault = new DefaultVault();
    }

    /** A decorator config with kind as a generation input only (no scope_kind). */
    private static Map<String, Object> config(Object entityKey) {
        return ImmutableMap.of("kind", "example", "entity_key", entityKey);
    }

    /** A decorator config whose kind is lookup scope; null values are left out. */
    private static Map<String, Object> scoped(Object entityKey, Object kind) {
        Map<String, Object> config = new HashMap<>();
        if (entityKey != null) {
            config.put("entity_key", entityKey);
        }
        if (kind != null) {
            config.put("kind", kind);
        }
        config.put("scope_kind", true);
        return config;
    }

    private String redact(Object storage, Object decoratorConfig) throws EvalException {
        return (String) vault.redact(
                SECRET, storage, NONE, Collections.emptyList(), decoratorConfig);
    }

    @Test
    public void revealWithTheEntityKeyReturnsTheSecret() throws Exception {
        String alias = redact(NONE, config("entity-a"));
        assertEquals(SECRET, vault.reveal(alias, NONE, "entity-a", NONE));
    }

    @Test
    public void revealWithoutTheEntityKeyIsNotFound() throws Exception {
        String alias = redact(NONE, config("entity-a"));
        assertEquals(alias, vault.reveal(alias, NONE, NONE, NONE));
        assertEquals(alias, vault.reveal(alias, NONE));
    }

    @Test
    public void revealWithAnotherEntityKeyIsNotFound() throws Exception {
        String alias = redact(NONE, config("entity-a"));
        assertEquals(alias, vault.reveal(alias, NONE, "entity-b", NONE));
    }

    @Test
    public void revealWithAnEntityKeyIgnoresAliasesStoredWithoutOne() throws Exception {
        String alias = redact(NONE, NONE);
        assertEquals(SECRET, vault.reveal(alias, NONE));
        assertEquals(alias, vault.reveal(alias, NONE, "entity-a", NONE));
    }

    @Test
    public void kindIsNotScopeUnlessScopeKindIsTrue() throws Exception {
        String alias = redact(NONE, config("entity-a"));
        // kind was only a generation input, so a reveal that names it asks for another scope.
        assertEquals(alias, vault.reveal(alias, NONE, "entity-a", "example"));
        assertEquals(SECRET, vault.reveal(alias, NONE, "entity-a", NONE));

        String explicit = redact(NONE, ImmutableMap.of("kind", "example", "scope_kind", false));
        assertEquals(SECRET, vault.reveal(explicit, NONE));
    }

    @Test
    public void entityKeyAndKindTogetherMustBothBeGiven() throws Exception {
        String alias = redact(NONE, scoped("entity-a", "example"));
        assertEquals(SECRET, vault.reveal(alias, NONE, "entity-a", "example"));
        assertEquals(alias, vault.reveal(alias, NONE, "entity-a", NONE));
        assertEquals(alias, vault.reveal(alias, NONE, NONE, "example"));
        assertEquals(alias, vault.reveal(alias, NONE, NONE, NONE));
        assertEquals(alias, vault.reveal(alias, NONE, "entity-a", "other"));
    }

    @Test
    public void kindAloneIsAScope() throws Exception {
        String alias = redact(NONE, scoped(null, "example"));
        assertEquals(SECRET, vault.reveal(alias, NONE, NONE, "example"));
        assertEquals(alias, vault.reveal(alias, NONE));
        assertEquals(alias, vault.reveal(alias, NONE, "example", NONE));
    }

    @Test
    public void entityKeyOnlyAndKindOnlyAreDistinctScopes() throws Exception {
        String entityOnly = redact(NONE, ImmutableMap.of("entity_key", "same"));
        String kindOnly = redact(NONE, scoped(null, "same"));
        assertEquals(SECRET, vault.reveal(entityOnly, NONE, "same", NONE));
        assertEquals(entityOnly, vault.reveal(entityOnly, NONE, NONE, "same"));
        assertEquals(SECRET, vault.reveal(kindOnly, NONE, NONE, "same"));
        assertEquals(kindOnly, vault.reveal(kindOnly, NONE, "same", NONE));
    }

    @Test
    public void scopeKindWithoutAKindAddsNoKindScope() throws Exception {
        String alias = redact(NONE, scoped("entity-a", null));
        assertEquals(SECRET, vault.reveal(alias, NONE, "entity-a", NONE));
    }

    @Test
    public void nonBooleanScopeKindIsRejected() {
        EvalException e = assertThrows(EvalException.class,
                () -> redact(NONE, ImmutableMap.of("kind", "example", "scope_kind", "yes")));
        assertEquals("scope_kind must be a bool, not string", e.getMessage());
    }

    @Test
    public void storageIsHonouredForScopedAliases() throws Exception {
        String alias = redact("volatile", config("entity-a"));
        assertEquals(SECRET, vault.reveal(alias, "volatile", "entity-a", NONE));
        assertEquals(alias, vault.reveal(alias, "persistent", "entity-a", NONE));
    }

    @Test
    public void aTwoArgumentRevealIsUnchanged() throws Exception {
        String alias = redact(NONE, ImmutableMap.of("kind", "example"));
        assertEquals(SECRET, vault.reveal(alias, NONE));
        assertEquals(SECRET, vault.reveal(alias, NONE, NONE, NONE));
    }

    @Test
    public void deleteRemovesAnAliasStoredWithoutContext() throws Exception {
        String alias = redact(NONE, NONE);
        vault.delete(alias, NONE);
        assertEquals(alias, vault.reveal(alias, NONE));
        assertNotEquals(SECRET, vault.reveal(alias, NONE));
    }

    @Test
    public void emptyOrBlankContextIsRejectedAtRedactRevealAndDelete() {
        for (String blank : new String[] {"", " ", "\t", " \n "}) {
            assertThrows(EvalException.class, () -> redact(NONE, config(blank)));
            assertThrows(EvalException.class, () -> redact(NONE, scoped(null, blank)));
            assertThrows(EvalException.class, () -> vault.reveal("alias", NONE, blank, NONE));
            assertThrows(EvalException.class, () -> vault.reveal("alias", NONE, NONE, blank));
            assertThrows(EvalException.class, () -> vault.delete("alias", NONE, blank, NONE));
            assertThrows(EvalException.class, () -> vault.delete("alias", NONE, NONE, blank));
        }
    }

    @Test
    public void nonStringContextIsRejectedAtRedactAndReveal() {
        assertThrows(EvalException.class, () -> redact(NONE, config(1)));
        assertThrows(EvalException.class, () -> redact(NONE, scoped(null, 1)));
        assertThrows(EvalException.class, () -> vault.reveal("alias", NONE, 1, NONE));
        assertThrows(EvalException.class, () -> vault.reveal("alias", NONE, NONE, 1));
    }

    @Test
    public void deleteWithTheEntityKeyRemovesOnlyThatEntry() throws Exception {
        String alias = redact(NONE, config("entity-a"));
        vault.delete(alias, NONE, "entity-b", NONE);
        assertEquals(SECRET, vault.reveal(alias, NONE, "entity-a", NONE));
        vault.delete(alias, NONE);
        assertEquals(SECRET, vault.reveal(alias, NONE, "entity-a", NONE));
        vault.delete(alias, NONE, "entity-a", "example");
        assertEquals(SECRET, vault.reveal(alias, NONE, "entity-a", NONE));
        vault.delete(alias, NONE, "entity-a", NONE);
        assertEquals(alias, vault.reveal(alias, NONE, "entity-a", NONE));
    }

    @Test
    public void deleteWithMissingKindDoesNotRemoveAKindScopedEntry() throws Exception {
        String alias = redact(NONE, scoped("entity-a", "example"));
        vault.delete(alias, NONE, "entity-a", NONE);
        assertEquals(SECRET, vault.reveal(alias, NONE, "entity-a", "example"));
        vault.delete(alias, NONE, "entity-a", "example");
        assertEquals(alias, vault.reveal(alias, NONE, "entity-a", "example"));
    }

    @Test
    public void deleteWithContextLeavesUnscopedEntries() throws Exception {
        String alias = redact(NONE, NONE);
        vault.delete(alias, NONE, "entity-a", NONE);
        vault.delete(alias, NONE, NONE, "example");
        assertEquals(SECRET, vault.reveal(alias, NONE));
    }

    @Test
    public void deleteWithStorageIsScoped() throws Exception {
        String alias = redact("volatile", config("entity-a"));
        vault.delete(alias, "persistent", "entity-a", NONE);
        assertEquals(SECRET, vault.reveal(alias, "volatile", "entity-a", NONE));
        vault.delete(alias, "volatile", "entity-a", NONE);
        assertEquals(alias, vault.reveal(alias, "volatile", "entity-a", NONE));
    }

    @Test
    public void deleteRejectsEmptyAndNonStringContext() {
        assertThrows(EvalException.class, () -> vault.delete("alias", NONE, "", NONE));
        assertThrows(EvalException.class, () -> vault.delete("alias", NONE, 1, NONE));
        assertThrows(EvalException.class, () -> vault.delete("alias", NONE, NONE, ""));
        assertThrows(EvalException.class, () -> vault.delete("alias", NONE, NONE, 1));
    }
}
