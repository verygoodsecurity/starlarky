package com.verygood.security.larky.modules.vgs.cerebro.analyzer.text.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;

import java.util.HashMap;
import java.util.Map;

@Builder
@AllArgsConstructor
@StarlarkBuiltin(
    name = "text_pii_entity",
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

  @StarlarkMethod(
      name = "entity_type",
      doc = "PII entity entity type")
  public String entityType() {
    return entityType;
  }

  @StarlarkMethod(
      name = "score",
      doc = "The PII detection score")
  public StarlarkFloat score() {
    return score;
  }

  @StarlarkMethod(
      name = "start",
      doc = "Where the PII starts")
  public StarlarkInt start() {
    return start;
  }

  @StarlarkMethod(
      name = "end",
      doc = "Where the PII ends")
  public StarlarkInt end() {
    return end;
  }

  @StarlarkMethod(
      name = "to_dict",
      doc = "Dictionary PII representation")
  public Map<String, Object> toDict() {
    Map<String, Object> dictRepresentation = new HashMap<>();
    dictRepresentation.put("entity_type", this.entityType);
    dictRepresentation.put("score", this.score);
    dictRepresentation.put("start", start);
    dictRepresentation.put("end", end);

    return dictRepresentation;
  }

}
