package com.verygood.security.larky.modules.vgs.cerebro.analyzer.text.dto;

import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class TextPIIEntityTest {

  private static final String ENTITY_TYPE_KEY = "entity_type";
  private static final String END_KEY = "end";
  private static final String START_KEY = "start";
  private static final String SCORE_KEY = "score";

  @Test
  void testTextPIIEntity_toDict_ok() {
    // Arrange
    final String entityType = "CARD_NUMBER";
    final int start = 0;
    final int end = 44;
    final double score = 0.0;

    // Act
    final TextPIIEntity piiEntity = createTextPiiEntity(entityType, start, end, score);

    final Map<String, Object> piiDict = piiEntity.toDict();

    // Assert
    Assertions.assertEquals(4, piiDict.size());
    Assertions.assertEquals(entityType, piiDict.get(ENTITY_TYPE_KEY));
    Assertions.assertEquals(StarlarkInt.of(start), piiDict.get(START_KEY));
    Assertions.assertEquals(StarlarkInt.of(end), piiDict.get(END_KEY));
    Assertions.assertEquals(StarlarkFloat.of(score), piiDict.get(SCORE_KEY));
  }

  private TextPIIEntity createTextPiiEntity(String entityType, int start, int end, double score) {
    return TextPIIEntity.builder()
        .entityType(entityType)
        .start(StarlarkInt.of(start))
        .end(StarlarkInt.of(end))
        .score(StarlarkFloat.of(score))
        .build();
  }
}
