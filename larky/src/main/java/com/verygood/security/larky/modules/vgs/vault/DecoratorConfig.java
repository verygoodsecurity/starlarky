package com.verygood.security.larky.modules.vgs.vault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;

@Getter
@Builder
@EqualsAndHashCode
public class DecoratorConfig {

  /**
   * The decorator config keys that describe a SYNTHETIC alias. They are kept, stringified, in
   * {@link #getOptions()} so a provider can forward them; any other unknown key is ignored.
   */
  public static final ImmutableSet<String> OPTION_KEYS =
      ImmutableSet.of("kind", "record", "country", "email_domain", "allow_unreserved_phone", "dob");

  /** A decorator option whose value is not a string, boolean or number. Never echoes the value. */
  public static class InvalidDecoratorOptionException extends InvalidDecoratorConfigException {

    public InvalidDecoratorOptionException(String message) {
      super(message);
    }
  }

  public static class InvalidDecoratorConfigException extends RuntimeException {

    public InvalidDecoratorConfigException(String message) {
      super(message);
    }
  }

  @Getter
  @AllArgsConstructor
  @Builder
  @EqualsAndHashCode
  public static class NonLuhnValidTransformPattern {

    private final String search;
    private final String replace;
  }

  @Getter
  @AllArgsConstructor
  @Builder
  @EqualsAndHashCode
  public static class NonLuhnValidPattern {

    private final String validatePattern;
    private final List<NonLuhnValidTransformPattern> transformPatterns;
  }

  private final String searchPattern;
  private final String replacePattern;
  private final NonLuhnValidPattern nonLuhnValidPattern;

  /**
   * The SYNTHETIC keys present in the config ({@link #OPTION_KEYS}), with scalar values as text:
   * booleans as "true" or "false". Immutable, never null. Values can be personal data (a date of
   * birth, a record), so {@link #toString()} prints the keys only.
   */
  private final Map<String, String> options;

  public DecoratorConfig(
      String searchPattern,
      String replacePattern,
      NonLuhnValidPattern nonLuhnValidPattern,
      Map<String, String> options) {
    this.searchPattern = searchPattern;
    this.replacePattern = replacePattern;
    this.nonLuhnValidPattern = nonLuhnValidPattern;
    this.options = options == null ? ImmutableMap.of() : ImmutableMap.copyOf(options);
  }

  /** Kept so code written before {@link #getOptions()} existed still compiles. */
  public DecoratorConfig(
      String searchPattern, String replacePattern, NonLuhnValidPattern nonLuhnValidPattern) {
    this(searchPattern, replacePattern, nonLuhnValidPattern, null);
  }


  public static DecoratorConfig fromObject(Object decoratorConfig) {
    if (!(decoratorConfig instanceof Map)) {
      return null;
    }
    Map map = (Map) decoratorConfig;

    DecoratorConfigBuilder decoratorConfigBuilder = DecoratorConfig.builder();

    decoratorConfigBuilder.searchPattern(getString(map, "searchPattern"));
    decoratorConfigBuilder.replacePattern(getString(map, "replacePattern"));

    Map nonLuhnValidPattern = getMap(map, "nonLuhnValidPattern");
    if (nonLuhnValidPattern != null) {
      NonLuhnValidPattern.NonLuhnValidPatternBuilder nonLuhnValidPatternBuilder = NonLuhnValidPattern.builder();
      nonLuhnValidPatternBuilder.validatePattern(getString(nonLuhnValidPattern, "validatePattern"));
      ImmutableList.Builder<NonLuhnValidTransformPattern> transformPatterns = ImmutableList.builder();
      for (Object transformPattern : getList(nonLuhnValidPattern, "transformPatterns")) {
        NonLuhnValidTransformPattern.NonLuhnValidTransformPatternBuilder transformPatternBuilder = NonLuhnValidTransformPattern.builder();
        Map transformPatternMap = toMap(transformPattern);
        transformPatternBuilder.search(getString(transformPatternMap, "search"));
        transformPatternBuilder.replace(getString(transformPatternMap, "replace"));
        transformPatterns.add(transformPatternBuilder.build());
      }
      nonLuhnValidPatternBuilder.transformPatterns(transformPatterns.build());
      decoratorConfigBuilder.nonLuhnValidPattern(nonLuhnValidPatternBuilder.build());
    }
    decoratorConfigBuilder.options(readOptions(map));
    return decoratorConfigBuilder.build();
  }

  /** Keys only: the values can be personal data. */
  @Override
  public String toString() {
    return "DecoratorConfig(searchPattern="
        + (searchPattern == null ? "unset" : "set")
        + ", replacePattern="
        + (replacePattern == null ? "unset" : "set")
        + ", nonLuhnValidPattern="
        + (nonLuhnValidPattern == null ? "unset" : "set")
        + ", options="
        + options.keySet()
        + ")";
  }

  private static Map<String, String> readOptions(Map map) {
    Map<String, String> options = new LinkedHashMap<>();
    for (String key : OPTION_KEYS) {
      if (!map.containsKey(key)) {
        continue;
      }
      Object value = map.get(key);
      if (value == null || value instanceof NoneType) {
        continue;
      }
      if (value instanceof String || value instanceof Boolean || value instanceof Number
          || value instanceof StarlarkInt || value instanceof StarlarkFloat) {
        options.put(key, value.toString());
      } else {
        // Do not include the value: it can be personal data.
        throw new InvalidDecoratorOptionException(
            String.format("'%s' field must be a string, boolean or number", key));
      }
    }
    return ImmutableMap.copyOf(options);
  }

  private static Map toMap(Object obj) {
    if (obj == null) {
      return null;
    }
    if (!(obj instanceof Map)) {
      throw new InvalidDecoratorConfigException(
          String.format("'%s' must be dict", obj)
      );
    }
    return (Map) obj;
  }

  private static String getString(Map map, String field) {
    if (!map.containsKey(field)) {
      return null;
    }
    Object value = map.get(field);
    if (!(value instanceof String)) {
      throw new InvalidDecoratorConfigException(
          String.format("'%s' field must be string", field)
      );
    }
    return (String) value;
  }

  private static Map getMap(Map map, String field) {
    if (!map.containsKey(field)) {
      return null;
    }
    Object value = map.get(field);
    if (!(value instanceof Map)) {
      throw new InvalidDecoratorConfigException(
          String.format("'%s' field must be dict", field)
      );
    }
    return (Map) value;
  }

  private static List getList(Map map, String field) {
    if (!map.containsKey(field)) {
      return Collections.emptyList();
    }
    Object value = map.get(field);
    if (!(value instanceof List)) {
      throw new InvalidDecoratorConfigException(
          String.format("'%s' field must be array", field)
      );
    }
    return (List) value;
  }
}
