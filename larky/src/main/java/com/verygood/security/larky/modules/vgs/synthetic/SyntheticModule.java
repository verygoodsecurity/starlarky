package com.verygood.security.larky.modules.vgs.synthetic;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkValue;

/**
 * The native half of {@code @vgs//synthetic}: validates and builds {@link AliasSpec}s. The {@code
 * Faker} API in {@code synthetic.star} is written on top of it.
 */
@StarlarkBuiltin(
    name = "native_synthetic",
    category = "BUILTIN",
    doc = "Builds the specifications of SYNTHETIC aliases for @vgs//synthetic.")
public class SyntheticModule implements StarlarkValue {

  public static final SyntheticModule INSTANCE = new SyntheticModule();

  /** The kinds of value a SYNTHETIC alias can generate. */
  public static final ImmutableSet<String> KINDS =
      ImmutableSet.of(
          "full_name",
          "first_name",
          "last_name",
          "email",
          "street",
          "city",
          "postcode",
          "region",
          "country",
          "dob",
          "phone",
          "age");

  private static final Pattern COUNTRY = Pattern.compile("[A-Z]{2}");
  private static final Pattern LOCALE = Pattern.compile("(?:[A-Za-z]{2}[_-])?([A-Za-z]{2})");

  /**
   * The country of a locale: {@code xx_CC} or {@code xx-CC} gives {@code CC}, and a bare two-letter
   * country {@code CC} gives itself, all upper-cased.
   */
  public static String countryOfLocale(String locale) throws EvalException {
    java.util.regex.Matcher m = LOCALE.matcher(locale);
    if (!m.matches()) {
      throw Starlark.errorf(
          "Invalid locale '%s': expected 'xx_CC', 'xx-CC' or a two-letter country code such as 'GB'",
          locale);
    }
    return m.group(1).toUpperCase(java.util.Locale.ROOT);
  }

  @StarlarkMethod(
      name = "locale_country",
      doc = "The upper-case country code of a locale such as 'en_GB', 'en-GB' or 'GB'.",
      parameters = {@Param(name = "locale", doc = "the locale", allowedTypes = {@ParamType(type = String.class)})})
  public String localeCountry(String locale) throws EvalException {
    return countryOfLocale(locale);
  }

  @StarlarkMethod(
      name = "alias_spec",
      doc = "Validates and builds the specification of a SYNTHETIC alias.",
      parameters = {
        @Param(name = "kind", doc = "kind of value to generate", allowedTypes = {@ParamType(type = String.class)}),
        @Param(name = "record", doc = "record the alias belongs to", allowedTypes = {@ParamType(type = String.class)}),
        @Param(name = "country", doc = "two-letter upper-case country code", allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "email_domain",
            doc = "domain of generated email addresses; only for kind 'email'",
            named = true,
            positional = false,
            defaultValue = "None",
            allowedTypes = {@ParamType(type = NoneType.class), @ParamType(type = String.class)}),
        @Param(
            name = "allow_unreserved_phone",
            doc = "allow phone numbers outside the reserved fictional ranges; only for kind 'phone'",
            named = true,
            positional = false,
            defaultValue = "False",
            allowedTypes = {@ParamType(type = Boolean.class)}),
        @Param(
            name = "dob",
            doc = "date of birth the age is derived from; only for kind 'age'",
            named = true,
            positional = false,
            defaultValue = "None",
            allowedTypes = {@ParamType(type = NoneType.class), @ParamType(type = String.class)}),
      })
  public AliasSpec aliasSpec(
      String kind,
      String record,
      String country,
      Object emailDomain,
      boolean allowUnreservedPhone,
      Object dob)
      throws EvalException {
    if (!KINDS.contains(kind)) {
      throw Starlark.errorf("Unknown kind '%s': expected one of %s", kind, KINDS);
    }
    if (record.isBlank()) {
      throw Starlark.errorf("record must not be empty or blank");
    }
    if (!COUNTRY.matcher(country).matches()) {
      throw Starlark.errorf(
          "Invalid country '%s': expected a two-letter upper-case code such as 'GB'", country);
    }

    Map<String, Object> config = new LinkedHashMap<>();
    config.put("kind", kind);
    config.put("record", record);
    config.put("country", country);

    if (!(emailDomain instanceof NoneType)) {
      String domain = (String) emailDomain;
      if (!kind.equals("email")) {
        throw Starlark.errorf("email_domain only applies to kind 'email', not '%s'", kind);
      }
      if (domain.isBlank() || domain.contains("@") || domain.chars().anyMatch(Character::isWhitespace)) {
        throw Starlark.errorf(
            "Invalid email_domain '%s': expected a domain name such as 'example.org'", domain);
      }
      config.put("email_domain", domain);
    }
    if (allowUnreservedPhone) {
      if (!kind.equals("phone")) {
        throw Starlark.errorf("allow_unreserved_phone only applies to kind 'phone', not '%s'", kind);
      }
      config.put("allow_unreserved_phone", Boolean.TRUE);
    }
    if (!(dob instanceof NoneType)) {
      String d = (String) dob;
      if (!kind.equals("age")) {
        throw Starlark.errorf("dob only applies to kind 'age', not '%s'", kind);
      }
      if (d.isBlank()) {
        throw Starlark.errorf("dob must not be empty or blank");
      }
      config.put("dob", d);
    }
    return new AliasSpec(ImmutableMap.copyOf(config));
  }
}
