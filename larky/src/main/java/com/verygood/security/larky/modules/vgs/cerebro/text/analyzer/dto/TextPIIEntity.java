package com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.dto;

import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.modules.types.structs.SimpleStruct;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;

@StarlarkBuiltin(
    name = "TextPIIEntity",
    category = "BUILTIN",
    doc = "The type represents TextPIIAnalyzer text analysis result, and describes an instance of PII entity.")
public class TextPIIEntity extends SimpleStruct {

  // Type of the PII entity. e.g.: CARD_NUMBER, CRYPTO, etc.
  private final String entityType;

  // The PII detection score
  private final StarlarkFloat score;

  // Where the PII starts
  private final StarlarkInt start;

  // Where the PII ends
  private final StarlarkInt end;


  TextPIIEntity(String entityType, StarlarkFloat score, StarlarkInt start, StarlarkInt end, StarlarkThread thread) {
    super(dictOf(entityType, score, start, end), thread);
    this.entityType = entityType;
    this.score = score;
    this.start = start;
    this.end = end;
  }

  public static TextPIIEntity of(String entityType, double score, int start, int end) {
    return new TextPIIEntity(entityType, StarlarkFloat.of(score), StarlarkInt.of(start), StarlarkInt.of(end), null);
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

  @Override
  @StarlarkMethod(name = PyProtocols.__DICT__, structField = true)
  public Dict<String, Object> dunderDict() {
    return TextPIIEntity.dictOf(this.entityType, this.score, this.start, this.end);
  }

  @Override
  public boolean isImmutable() {
    return true;
  }

  private static Dict<String, Object> dictOf(String entityType, StarlarkFloat score, StarlarkInt start,
                                             StarlarkInt end) {
    return Dict.<String, Object>builder()
        .put("entity_type", entityType)
        .put("score", score)
        .put("start", start)
        .put("end", end)
        .buildImmutable();
  }
}
