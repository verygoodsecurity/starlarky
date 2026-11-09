package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.vgs.vault.defaults.DefaultVault;
import java.util.Collections;
import java.util.Map;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** DefaultVault keys its in-memory store on (record, alias). */
public class DefaultVaultRecordTest {

    private static final String SECRET = "example-secret";

    private DefaultVault vault;

    @BeforeEach
    public void setUp() {
        vault = new DefaultVault();
    }

    private static Map<String, Object> config(Object record) {
        return ImmutableMap.of("kind", "example", "record", record);
    }

    private String redact(Object storage, Object decoratorConfig) throws EvalException {
        return (String) vault.redact(
                SECRET, storage, Starlark.NONE, Collections.emptyList(), decoratorConfig);
    }

    @Test
    public void revealWithTheRecordReturnsTheSecret() throws Exception {
        String alias = redact(Starlark.NONE, config("record-a"));
        assertEquals(SECRET, vault.reveal(alias, Starlark.NONE, "record-a"));
    }

    @Test
    public void revealWithoutTheRecordIsNotFound() throws Exception {
        String alias = redact(Starlark.NONE, config("record-a"));
        assertEquals(alias, vault.reveal(alias, Starlark.NONE, Starlark.NONE));
        assertEquals(alias, vault.reveal(alias, Starlark.NONE));
    }

    @Test
    public void revealWithAnotherRecordIsNotFound() throws Exception {
        String alias = redact(Starlark.NONE, config("record-a"));
        assertEquals(alias, vault.reveal(alias, Starlark.NONE, "record-b"));
    }

    @Test
    public void revealWithARecordIgnoresAliasesStoredWithoutOne() throws Exception {
        String alias = redact(Starlark.NONE, Starlark.NONE);
        assertEquals(SECRET, vault.reveal(alias, Starlark.NONE));
        assertEquals(alias, vault.reveal(alias, Starlark.NONE, "record-a"));
    }

    @Test
    public void storageIsHonouredForRecordScopedAliases() throws Exception {
        String alias = redact("volatile", config("record-a"));
        assertEquals(SECRET, vault.reveal(alias, "volatile", "record-a"));
        assertEquals(alias, vault.reveal(alias, "persistent", "record-a"));
    }

    @Test
    public void aTwoArgumentRevealIsUnchanged() throws Exception {
        String alias = redact(Starlark.NONE, ImmutableMap.of("kind", "example"));
        assertEquals(SECRET, vault.reveal(alias, Starlark.NONE));
        assertEquals(SECRET, vault.reveal(alias, Starlark.NONE, Starlark.NONE));
    }

    @Test
    public void deleteRemovesAnAliasStoredWithoutARecord() throws Exception {
        String alias = redact(Starlark.NONE, Starlark.NONE);
        vault.delete(alias, Starlark.NONE);
        assertEquals(alias, vault.reveal(alias, Starlark.NONE));
        assertNotEquals(SECRET, vault.reveal(alias, Starlark.NONE));
    }

    @Test
    public void emptyOrBlankRecordIsRejectedAtRedactAndReveal() {
        for (String blank : new String[] {"", " ", "\t", " \n "}) {
            assertThrows(EvalException.class, () -> redact(Starlark.NONE, config(blank)));
            assertThrows(EvalException.class, () -> vault.reveal("alias", Starlark.NONE, blank));
            assertThrows(EvalException.class, () -> vault.delete("alias", Starlark.NONE, blank));
        }
    }

    @Test
    public void nonStringRecordIsRejectedAtRedactAndReveal() {
        assertThrows(EvalException.class, () -> redact(Starlark.NONE, config(1)));
        assertThrows(EvalException.class, () -> vault.reveal("alias", Starlark.NONE, 1));
    }

    @Test
    public void deleteWithTheRecordRemovesOnlyThatEntry() throws Exception {
        String alias = redact(Starlark.NONE, config("record-a"));
        vault.delete(alias, Starlark.NONE, "record-b");
        assertEquals(SECRET, vault.reveal(alias, Starlark.NONE, "record-a"));
        vault.delete(alias, Starlark.NONE);
        assertEquals(SECRET, vault.reveal(alias, Starlark.NONE, "record-a"));
        vault.delete(alias, Starlark.NONE, "record-a");
        assertEquals(alias, vault.reveal(alias, Starlark.NONE, "record-a"));
    }

    @Test
    public void deleteWithARecordLeavesUnscopedEntries() throws Exception {
        String alias = redact(Starlark.NONE, Starlark.NONE);
        vault.delete(alias, Starlark.NONE, "record-a");
        assertEquals(SECRET, vault.reveal(alias, Starlark.NONE));
    }

    @Test
    public void deleteWithStorageIsScoped() throws Exception {
        String alias = redact("volatile", config("record-a"));
        vault.delete(alias, "persistent", "record-a");
        assertEquals(SECRET, vault.reveal(alias, "volatile", "record-a"));
        vault.delete(alias, "volatile", "record-a");
        assertEquals(alias, vault.reveal(alias, "volatile", "record-a"));
    }

    @Test
    public void deleteRejectsEmptyAndNonStringRecords() {
        assertThrows(EvalException.class, () -> vault.delete("alias", Starlark.NONE, ""));
        assertThrows(EvalException.class, () -> vault.delete("alias", Starlark.NONE, 1));
    }
}
