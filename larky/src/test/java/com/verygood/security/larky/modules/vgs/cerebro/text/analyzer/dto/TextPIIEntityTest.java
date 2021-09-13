package com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.dto;

import net.starlark.java.eval.EvalException;
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
  void testTextPIIEntity_toDict_ok() throws EvalException {
    // Arrange
    final String entityType = "CARD_NUMBER";
    final int start = 0;
    final int end = 44;
    final double score = 0.0;

    // Act
    final TextPIIEntity piiEntity = TextPIIEntity.immutableOf(entityType, score, start, end);

    final Map<String, Object> piiDict = piiEntity.dunderDict();

    // Assert
    Assertions.assertEquals(4, piiDict.size());
    Assertions.assertEquals(entityType, piiDict.get(ENTITY_TYPE_KEY));
    Assertions.assertEquals(StarlarkInt.of(start), piiDict.get(START_KEY));
    Assertions.assertEquals(StarlarkInt.of(end), piiDict.get(END_KEY));
    Assertions.assertEquals(StarlarkFloat.of(score), piiDict.get(SCORE_KEY));
  }
}
