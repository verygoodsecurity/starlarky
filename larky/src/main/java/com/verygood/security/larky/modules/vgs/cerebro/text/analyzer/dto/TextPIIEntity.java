package com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.dto;

import lombok.Builder;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;


@Builder
@StarlarkBuiltin(
    name = "TextPIIEntity",
    category = "BUILTIN",
    doc = "The type represents TextPIIAnalyzer text analysis result, and describes an instance of PII entity.")
public class TextPIIEntity implements StarlarkValue {

  // Type of the PII entity. e.g.: CARD_NUMBER, CRYPTO, etc.
  private final String entityType;

  // The PII detection score
  private final StarlarkFloat score;

  // Where the PII starts
  private final StarlarkInt start;

  // Where the PII ends
  private final StarlarkInt end;

  public static TextPIIEntity of(String entityType, double score, int start, int end) {
    return TextPIIEntity.builder()
        .entityType(entityType)
        .score(StarlarkFloat.of(score))
        .start(StarlarkInt.of(start))
        .end(StarlarkInt.of(end))
        .build();
  }

  @StarlarkMethod(
      name = "entity_type",
      structField = true,
      doc = "PII entity entity type")
  public String entityType() {
    return entityType;
  }

  @StarlarkMethod(
      name = "score",
      structField = true,
      doc = "The PII detection score")
  public StarlarkFloat score() {
    return score;
  }

  @StarlarkMethod(
      name = "start",
      structField = true,
      doc = "Where the PII starts")
  public StarlarkInt start() {
    return start;
  }

  @StarlarkMethod(
      name = "end",
      structField = true,
      doc = "Where the PII ends")
  public StarlarkInt end() {
    return end;
  }

  @StarlarkMethod(
      name = "to_dict",
      doc = "Dictionary PII representation")
  public Dict<String, Object> toDict() {
    return Dict.<String, Object>builder()
        .put("entity_type", this.entityType)
        .put("score", this.score)
        .put("start", this.start)
        .put("end", this.end)
        .buildImmutable();
  }

}