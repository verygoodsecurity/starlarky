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
        return MODULE.aliasSpec(kind, "record-a", "GB", Starlark.NONE, false, Starlark.NONE);
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
            assertEquals(ImmutableMap.of("kind", kind, "record", "record-a", "country", "GB"),
                    spec.getDecoratorConfigMap());
            assertEquals(kind, spec.kind());
            assertEquals("record-a", spec.record());
            assertEquals("GB", spec.country());
            assertEquals("SYNTHETIC", spec.format());
        }
    }

    @Test
    public void optionsAreCarriedWhenSetForTheirKind() throws Exception {
        assertEquals(
                ImmutableMap.of("kind", "email", "record", "r", "country", "GB", "email_domain", "example.org"),
                MODULE.aliasSpec("email", "r", "GB", "example.org", false, Starlark.NONE).getDecoratorConfigMap());
        assertEquals(
                ImmutableMap.of("kind", "phone", "record", "r", "country", "GB", "allow_unreserved_phone", true),
                MODULE.aliasSpec("phone", "r", "GB", Starlark.NONE, true, Starlark.NONE).getDecoratorConfigMap());
        assertEquals(
                ImmutableMap.of("kind", "age", "record", "r", "country", "GB", "dob", "1990-05-17"),
                MODULE.aliasSpec("age", "r", "GB", Starlark.NONE, false, "1990-05-17").getDecoratorConfigMap());
    }

    @Test
    public void optionsAreRejectedForOtherKinds() {
        assertThrows(EvalException.class,
                () -> MODULE.aliasSpec("city", "r", "GB", "example.org", false, Starlark.NONE));
        assertThrows(EvalException.class,
                () -> MODULE.aliasSpec("email", "r", "GB", Starlark.NONE, true, Starlark.NONE));
        assertThrows(EvalException.class,
                () -> MODULE.aliasSpec("dob", "r", "GB", Starlark.NONE, false, "1990-05-17"));
    }

    @Test
    public void invalidFieldsAreRejected() {
        assertThrows(EvalException.class, () -> spec("nickname"));
        for (String record : List.of("", " ", "\t\n")) {
            EvalException e = assertThrows(EvalException.class,
                    () -> MODULE.aliasSpec("city", record, "GB", Starlark.NONE, false, Starlark.NONE));
            assertEquals("record must not be empty or blank", e.getMessage());
        }
        for (String country : List.of("", "G", "GBR", "gb", "G1")) {
            assertThrows(EvalException.class,
                    () -> MODULE.aliasSpec("city", "r", country, Starlark.NONE, false, Starlark.NONE));
        }
        for (String domain : List.of("", " ", "a@b.org", "a b.org")) {
            assertThrows(EvalException.class,
                    () -> MODULE.aliasSpec("email", "r", "GB", domain, false, Starlark.NONE));
        }
        assertThrows(EvalException.class,
                () -> MODULE.aliasSpec("age", "r", "GB", Starlark.NONE, false, " "));
    }

    @Test
    public void textFormsNeverContainTheDateOfBirth() throws Exception {
        String dob = "1987-06-05";
        AliasSpec spec = MODULE.aliasSpec("age", "record-a", "GB", Starlark.NONE, false, dob);
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
            assertTrue(form.contains("record-a"), form);
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
