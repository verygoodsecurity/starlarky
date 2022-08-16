package com.verygood.security.larky.modules;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class DecoratorConfig {

  public static class InvalidDecoratorConfigException extends RuntimeException {

    public InvalidDecoratorConfigException(String message) {
      super(message);
    }
  }

  @Getter
  @AllArgsConstructor
  @Builder
  public static class NonLuhnValidTransformPattern {

    private final String search;
    private final String replace;
  }

  @Getter
  @AllArgsConstructor
  @Builder
  public static class NonLuhnValidPattern {

    private final String validatePattern;
    private final List<NonLuhnValidTransformPattern> transformPatterns;
  }

  private final String searchPattern;
  private final String replacePattern;
  private final NonLuhnValidPattern nonLuhnValidPattern;


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
    return decoratorConfigBuilder.build();
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
