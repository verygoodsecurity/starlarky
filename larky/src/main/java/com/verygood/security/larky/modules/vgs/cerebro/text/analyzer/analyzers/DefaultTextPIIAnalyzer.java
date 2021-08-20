package com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.analyzers;

import com.google.common.collect.Sets;
import com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.dto.TextPIIEntity;
import com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.spi.TextPIIAnalyzer;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultTextPIIAnalyzer implements TextPIIAnalyzer {

  private static final Set<String> SUPPORTED_LANGUAGES = Sets.newHashSet("en");

  private static final Set<String> SUPPORTED_PII_ENTITIES = Sets.newHashSet("CREDIT_CARD");

  private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");

  @Override
  public List<TextPIIEntity> analyze(String text, String language, List<String> entities,
                                     StarlarkFloat scoreThreshold) {
    return findCreditCards(text);
  }

  private static List<TextPIIEntity> findCreditCards(String text) {
    Matcher matcher = CREDIT_CARD_PATTERN.matcher(text);
    List<TextPIIEntity> foundEntities = new ArrayList<>();
    int start = 0;
    while (matcher.find(start)) {
      TextPIIEntity entity = TextPIIEntity.builder()
          .entityType("CREDIT_CARD")
          .start(StarlarkInt.of(matcher.start()))
          .end(StarlarkInt.of(matcher.end()))
          .score(StarlarkFloat.of(1.0))
          .build();
      foundEntities.add(entity);
      start = matcher.end();
    }

    return foundEntities;
  }

  @Override
  public List<String> supportedEntities(String language) {
    return new ArrayList<>(SUPPORTED_PII_ENTITIES);
  }

  @Override
  public List<String> supportedLanguages() {
    return new ArrayList<>(SUPPORTED_LANGUAGES);
  }
}
