package com.verygood.security.larky.modules.vgs.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.vgs.vault.DecoratorConfig.InvalidDecoratorConfigException;
import com.verygood.security.larky.modules.vgs.vault.DecoratorConfig.InvalidDecoratorOptionException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import org.junit.jupiter.api.Test;

public class DecoratorConfigOptionsTest {

    private static final String DOB = "1987-06-05";
    private static final String RECORD = "record-private-7";

    private static Map<String, Object> allOptions() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", "age");
        m.put("record", RECORD);
        m.put("country", "GB");
        m.put("email_domain", "example.org");
        m.put("allow_unreserved_phone", true);
        m.put("dob", DOB);
        return m;
    }

    @Test
    public void keepsExactlyTheSixKeysAsText() {
        Map<String, Object> config = allOptions();
        config.put("searchPattern", "(a)");
        config.put("replacePattern", "b${1}");
        config.put("somethingElse", "ignored");
        DecoratorConfig parsed = DecoratorConfig.fromObject(config);

        assertEquals(ImmutableMap.of(
                "kind", "age", "record", RECORD, "country", "GB",
                "email_domain", "example.org", "allow_unreserved_phone", "true", "dob", DOB),
                parsed.getOptions());
        assertEquals("(a)", parsed.getSearchPattern());
        assertEquals("b${1}", parsed.getReplacePattern());
    }

    @Test
    public void booleansAreTrueOrFalseAndNumbersAreText() {
        DecoratorConfig parsed = DecoratorConfig.fromObject(ImmutableMap.of(
                "allow_unreserved_phone", false, "country", 44, "kind", StarlarkInt.of(7)));
        assertEquals(ImmutableMap.of("allow_unreserved_phone", "false", "country", "44", "kind", "7"),
                parsed.getOptions());
    }

    @Test
    public void unknownKeysAreIgnoredAsBefore() {
        DecoratorConfig parsed = DecoratorConfig.fromObject(ImmutableMap.of("unknown", "x", "Kind", "y", "dob_", "z"));
        assertTrue(parsed.getOptions().isEmpty());
        assertNull(parsed.getSearchPattern());
        assertNull(parsed.getReplacePattern());
        assertNull(parsed.getNonLuhnValidPattern());
    }

    @Test
    public void emptyConfigAndNullValuesGiveEmptyOptions() {
        assertTrue(DecoratorConfig.fromObject(ImmutableMap.of()).getOptions().isEmpty());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", null);
        m.put("record", Starlark.NONE);
        assertTrue(DecoratorConfig.fromObject(m).getOptions().isEmpty());
        assertNull(DecoratorConfig.fromObject("not a map"));
        assertNull(DecoratorConfig.fromObject(Starlark.NONE));
    }

    @Test
    public void optionsAreImmutable() {
        DecoratorConfig parsed = DecoratorConfig.fromObject(ImmutableMap.of("kind", "city"));
        assertThrows(UnsupportedOperationException.class, () -> parsed.getOptions().put("dob", "x"));
    }

    @Test
    public void nonScalarValuesAreRejectedWithoutEchoingThem() {
        for (String key : DecoratorConfig.OPTION_KEYS) {
            for (Object bad : new Object[] {
                ImmutableList.of(DOB), ImmutableMap.of("secret", DOB), Dict.immutableCopyOf(ImmutableMap.of("k", DOB)),
                List.of(RECORD), new Object[] {DOB}}) {
                InvalidDecoratorConfigException e = assertThrows(InvalidDecoratorOptionException.class,
                        () -> DecoratorConfig.fromObject(ImmutableMap.of(key, bad)));
                assertEquals("'" + key + "' field must be a string, boolean or number", e.getMessage());
                assertFalse(e.getMessage().contains(DOB) || e.getMessage().contains(RECORD));
            }
        }
    }

    @Test
    public void toStringPrintsKeysOnly() {
        DecoratorConfig parsed = DecoratorConfig.fromObject(allOptions());
        String text = parsed.toString();
        for (String secret : new String[] {DOB, "1987", RECORD, "example.org", "GB", "age"}) {
            assertFalse(text.contains(secret), text);
        }
        assertTrue(text.contains("options=[kind, record, country, email_domain, allow_unreserved_phone, dob]"), text);
        assertEquals(text, String.format("%s", parsed));
    }

    @Test
    public void equalityAndHashCodeCoverOptions() {
        DecoratorConfig a = DecoratorConfig.fromObject(ImmutableMap.of("kind", "city", "record", "r1"));
        DecoratorConfig b = DecoratorConfig.fromObject(ImmutableMap.of("kind", "city", "record", "r1"));
        DecoratorConfig c = DecoratorConfig.fromObject(ImmutableMap.of("kind", "city", "record", "r2"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(c));
    }

    @Test
    public void theBuilderAndTheOldConstructorStillWork() {
        assertTrue(DecoratorConfig.builder().searchPattern("a").build().getOptions().isEmpty());
        DecoratorConfig old = new DecoratorConfig("a", "b", null);
        assertEquals("a", old.getSearchPattern());
        assertTrue(old.getOptions().isEmpty());
        assertTrue(new DecoratorConfig("a", "b", null, null).getOptions().isEmpty());
    }
}
