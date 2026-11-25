package com.verygood.security.larky.modules.vgs.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.Test;

public class SyntheticModuleTest {

    private static final SyntheticModule MODULE = SyntheticModule.INSTANCE;

    private static AliasSpec spec(String kind) throws EvalException {
        return MODULE.aliasSpec(kind, "entity-a", "GB", Starlark.NONE, false, Starlark.NONE, false);
    }

    @Test
    public void parsesLocales() throws Exception {
        String[][] cases = {
            {"en_GB", "GB"}, {"en-GB", "GB"}, {"GB", "GB"}, {"en_gb", "GB"}, {"EN-gb", "GB"},
            {"EN_GB", "GB"}, {"fr_FR", "FR"}, {"DE", "DE"}, {"fr-fr", "FR"}
        };
        for (String[] c : cases) {
            assertEquals(c[1], SyntheticModule.countryOfLocale(c[0]), c[0]);
            assertEquals(c[1], MODULE.localeCountry(c[0]), c[0]);
        }
    }

    @Test
    public void rejectsABareLowerCaseValueAsALanguageCode() {
        for (String locale : new String[] {"cy", "en", "de", "gb", "Gb", "gB"}) {
            EvalException e = assertThrows(EvalException.class,
                    () -> SyntheticModule.countryOfLocale(locale), locale);
            assertTrue(e.getMessage().startsWith("Invalid locale '" + locale + "'"), e.getMessage());
            assertTrue(e.getMessage().contains("language code"), e.getMessage());
            assertTrue(e.getMessage().contains("'en_GB'") && e.getMessage().contains("'GB'"), e.getMessage());
        }
    }

    @Test
    public void rejectsOtherLocales() {
        String[] bad = {
            "", " ", "G", "GBR", "eng_GB", "en_GBR", "en__GB", "en_G", "en GB", "en.GB", "en_GB_x",
            "en-GB-x", "_GB", "en_", "1a_GB", "en_G1", " en_GB", "en_GB ", "e\u00e9_GB", "en_GB\n"
        };
        for (String locale : bad) {
            EvalException e = assertThrows(EvalException.class,
                    () -> SyntheticModule.countryOfLocale(locale), "'" + locale + "'");
            assertTrue(e.getMessage().startsWith("Invalid locale '" + locale + "'"), e.getMessage());
        }
    }

    @Test
    public void everyKindProducesASyntheticSpecWithTheBaseConfig() throws Exception {
        assertEquals(12, SyntheticModule.KINDS.size());
        for (String kind : SyntheticModule.KINDS) {
            AliasSpec spec = spec(kind);
            assertEquals("SYNTHETIC", spec.getFormat());
            assertEquals(ImmutableMap.of("kind", kind, "entity_key", "entity-a", "country", "GB", "scope_kind", false),
                    spec.getDecoratorConfigMap());
            assertEquals(kind, spec.kind());
            assertEquals("entity-a", spec.entityKey());
            assertEquals(false, spec.scopeKind());
            assertEquals("GB", spec.country());
            assertEquals("SYNTHETIC", spec.format());
        }
    }

    @Test
    public void checkKindAcceptsOnlyTheStoredKinds() throws Exception {
        for (String kind : SyntheticModule.KINDS) {
            assertEquals(kind, MODULE.checkKind(kind));
        }
        for (String name : List.of("name", "state", "administrative_unit", "street_address", "phone_number",
                "date_of_birth", "", " ", "FULL_NAME")) {
            EvalException e = assertThrows(EvalException.class, () -> MODULE.checkKind(name), name);
            assertEquals("Unknown kind '" + name + "': expected one of " + SyntheticModule.KINDS, e.getMessage());
        }
    }

    @Test
    public void scopeKindIsCarriedInTheSpec() throws Exception {
        AliasSpec scoped = MODULE.aliasSpec("city", "e", "GB", Starlark.NONE, false, Starlark.NONE, true);
        assertEquals(ImmutableMap.of("kind", "city", "entity_key", "e", "country", "GB", "scope_kind", true),
                scoped.getDecoratorConfigMap());
        assertTrue(scoped.scopeKind());
        AliasSpec unscoped = MODULE.aliasSpec("city", "e", "GB", Starlark.NONE, false, Starlark.NONE, false);
        assertTrue(!scoped.equals(unscoped));
    }

    @Test
    public void optionsAreCarriedWhenSetForTheirKind() throws Exception {
        assertEquals(
                ImmutableMap.of("kind", "email", "entity_key", "e", "country", "GB", "scope_kind", false,
                        "email_domain", "example.org"),
                MODULE.aliasSpec("email", "e", "GB", "example.org", false, Starlark.NONE, false).getDecoratorConfigMap());
        assertEquals(
                ImmutableMap.of("kind", "phone", "entity_key", "e", "country", "GB", "scope_kind", false,
                        "allow_unreserved_phone", true),
                MODULE.aliasSpec("phone", "e", "GB", Starlark.NONE, true, Starlark.NONE, false).getDecoratorConfigMap());
        assertEquals(
                ImmutableMap.of("kind", "age", "entity_key", "e", "country", "GB", "scope_kind", false,
                        "dob", "1990-05-17"),
                MODULE.aliasSpec("age", "e", "GB", Starlark.NONE, false, "1990-05-17", false).getDecoratorConfigMap());
    }

    @Test
    public void optionsAreRejectedForOtherKinds() {
        assertThrows(EvalException.class,
                () -> MODULE.aliasSpec("city", "e", "GB", "example.org", false, Starlark.NONE, false));
        assertThrows(EvalException.class,
                () -> MODULE.aliasSpec("email", "e", "GB", Starlark.NONE, true, Starlark.NONE, false));
        assertThrows(EvalException.class,
                () -> MODULE.aliasSpec("dob", "e", "GB", Starlark.NONE, false, "1990-05-17", false));
    }

    @Test
    public void invalidFieldsAreRejected() {
        assertThrows(EvalException.class, () -> spec("nickname"));
        for (String entityKey : List.of("", " ", "\t\n")) {
            EvalException e = assertThrows(EvalException.class,
                    () -> MODULE.aliasSpec("city", entityKey, "GB", Starlark.NONE, false, Starlark.NONE, false));
            assertEquals("entity_key must not be empty or blank", e.getMessage());
        }
        for (String country : List.of("", "G", "GBR", "gb", "G1")) {
            assertThrows(EvalException.class,
                    () -> MODULE.aliasSpec("city", "e", country, Starlark.NONE, false, Starlark.NONE, false));
        }
        for (String domain : List.of("", " ", "a@b.org", "a b.org")) {
            assertThrows(EvalException.class,
                    () -> MODULE.aliasSpec("email", "e", "GB", domain, false, Starlark.NONE, false));
        }
        assertThrows(EvalException.class,
                () -> MODULE.aliasSpec("age", "e", "GB", Starlark.NONE, false, " ", false));
    }

    @Test
    public void textFormsNeverContainTheDateOfBirth() throws Exception {
        String dob = "1987-06-05";
        AliasSpec spec = MODULE.aliasSpec("age", "entity-a", "GB", Starlark.NONE, false, dob, false);
        String[] forms = {
            spec.toString(),
            Starlark.repr(spec, net.starlark.java.eval.StarlarkSemantics.DEFAULT),
            Starlark.str(spec, net.starlark.java.eval.StarlarkSemantics.DEFAULT),
            String.format("%s", spec),
            "" + spec,
        };
        for (String form : forms) {
            assertTrue(!form.contains(dob) && !form.contains("1987"), form);
            assertTrue(form.contains("dob=<set>"), form);
            assertTrue(form.contains("entity-a"), form);
        }
        // The value itself is still available to the vault.
        assertEquals(dob, spec.getDecoratorConfigMap().get("dob"));
        // Other kinds print their config in full.
        assertTrue(spec("city").toString().contains("kind=city"));
    }

    @Test
    public void specsAreImmutableValuesWithEquality() throws Exception {
        AliasSpec a = spec("city");
        assertTrue(a.isImmutable());
        assertEquals(a, spec("city"));
        assertEquals(a.hashCode(), spec("city").hashCode());
        assertTrue(!a.equals(spec("region")));
        assertTrue(a.decoratorConfig().isImmutable());
    }
}
