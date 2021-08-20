package com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.dto;

import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.modules.types.structs.SimpleStruct;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  public static TextPIIEntity immutableOf(String entityType, double score, int start, int end) {
    return TextPIIEntity.of(entityType, score, start, end, null);
  }

  public static TextPIIEntity of(String entityType, double score, int start, int end, StarlarkThread thread) {
    return new TextPIIEntity(entityType, StarlarkFloat.of(score), StarlarkInt.of(start), StarlarkInt.of(end), thread);
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
    return false;
  }

  private static Dict<String, Object> dictOf(String entityType, StarlarkFloat score, StarlarkInt start,
                                             StarlarkInt end) {
    Map<String, Object> mapRepresentation = new HashMap<>();
    mapRepresentation.put("end", end);
    mapRepresentation.put("entity_type", entityType);
    mapRepresentation.put("score", score);
    mapRepresentation.put("start", start);

    final Dict.Builder<String, Object> builder = Dict.builder();
    final List<String> keySet = new ArrayList<>(mapRepresentation.keySet());
    Collections.sort(keySet);
    for (String k: keySet) {
      builder.put(k, mapRepresentation.get(k));
    }

    return builder.buildImmutable();
  }
}
