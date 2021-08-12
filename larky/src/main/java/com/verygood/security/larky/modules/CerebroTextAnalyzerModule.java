package com.verygood.security.larky.modules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.verygood.security.larky.modules.vgs.cerebro.analyzer.text.DefaultTextPIIAnalyzer;
import com.verygood.security.larky.modules.vgs.cerebro.analyzer.text.NoopTextPIIAnalyzer;
import com.verygood.security.larky.modules.vgs.cerebro.analyzer.text.dto.TextPIIEntity;
import com.verygood.security.larky.modules.vgs.cerebro.analyzer.text.spi.TextPIIAnalyzer;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkFloat;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

@StarlarkBuiltin(
    name = "text_pii_analyzer",
    category = "BUILTIN",
    doc = "Overridable PII Analysis API in Larky")
public class CerebroTextAnalyzerModule implements TextPIIAnalyzer {

  public static final CerebroTextAnalyzerModule INSTANCE = new CerebroTextAnalyzerModule();
  public static final String ENABLE_INMEMORY_PROPERTY = "larky.modules.vgs.cerebro.piiAnalyzer.text.enableInMemory";

  private TextPIIAnalyzer textPiiAnalyzer;

  public CerebroTextAnalyzerModule() {

    ServiceLoader<TextPIIAnalyzer> loader = ServiceLoader.load(TextPIIAnalyzer.class);
    List<TextPIIAnalyzer> textAnalyzerProviders = ImmutableList.copyOf(loader.iterator());

    if (Boolean.getBoolean(ENABLE_INMEMORY_PROPERTY)) {
      textPiiAnalyzer = new DefaultTextPIIAnalyzer();
    } else if (textAnalyzerProviders.isEmpty()) {
      textPiiAnalyzer = new NoopTextPIIAnalyzer();
    } else {
      if (textAnalyzerProviders.size() != 1) {
        throw new IllegalArgumentException(String.format(
            "Cerebro expecting only 1 text PII data analyzer provider of type TextPiiAnalyzer, found %d",
            textAnalyzerProviders.size()
        ));
      }

      textPiiAnalyzer = textAnalyzerProviders.get(0);
    }

  }

  @StarlarkMethod(
      name = "analyze",
      doc = "performs PII entities search, given supported language.",
      parameters = {
          @Param(
              name = "text",
              doc = "the text to analyze",
              allowedTypes = {
                  @ParamType(type = String.class),
              }),
          @Param(
              name = "language",
              doc = "two characters for the desired language in ISO_639-1 format.",
              named = true,
              defaultValue = "EN",
              allowedTypes = {
                  @ParamType(type = NoneType.class),
                  @ParamType(type = String.class),
              }),
          @Param(
              name = "entities",
              doc = "list of PII entities that should be looked for in the text.\n" +
                  "If List is empty, analyzer will look for all supported entities for given language.",
              named = true,
              defaultValue = "[]",
              allowedTypes = {
                  @ParamType(type = List.class),
              }),
          @Param(
              name = "scoreThreshold",
              doc = "a minimum value for which to return an identified entity, defaults to 0.0",
              named = true,
              defaultValue = "0.0",
              allowedTypes = {
                  @ParamType(type = StarlarkFloat.class)
              }),
      })
  @Override
  public List<TextPIIEntity> analyze(String text, String language, List<String> entities, StarlarkFloat scoreThreshold)
      throws EvalException {
    validateLanguage(language);
    if (Objects.isNull(entities) || entities.isEmpty()) {
      entities = this.supportedEntities(language);
    }
    validateEntities(language, entities);
    return this.textPiiAnalyzer.analyze(text, language, entities, scoreThreshold);
  }

  @StarlarkMethod(
      name = "supported_entities",
      doc = "performs PII entities search, given supported language.",
      parameters = {
          @Param(
              name = "language",
              doc = "Two characters for the desired language in ISO_639-1 format.",
              allowedTypes = {
                  @ParamType(type = String.class),
              })})
  @Override
  public List<String> supportedEntities(String language) throws EvalException {
    validateLanguage(language);
    return this.textPiiAnalyzer.supportedEntities(language);
  }

  @StarlarkMethod(
      name = "supported_languages",
      doc = "list of supported languages in ISO_639-1 format.")
  @Override
  public List<String> supportedLanguages() throws EvalException {
    return this.textPiiAnalyzer.supportedLanguages();
  }

  private void validateLanguage(String language) throws EvalException {
    if (!this.supportedLanguages().contains(language)) {
      throw Starlark.errorf(String.format("Provided language: %s is not currently supported.\n" +
          "List of supported languages: %s", language, this.supportedLanguages()));
    }
  }

  private void validateEntities(String language, List<String> entities) throws EvalException {
    Set<String> deduplicatedEntities = Sets.newHashSet(entities);
    deduplicatedEntities.removeAll(this.supportedEntities(language));
    if (!deduplicatedEntities.isEmpty()) {
      throw Starlark.errorf(String.format("Requested PII entities: %s are not currently supported." +
          " List of supported PII entities: %s", deduplicatedEntities, this.supportedEntities(language)));
    }
  }
}
