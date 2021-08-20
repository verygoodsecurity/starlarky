package com.verygood.security.larky.modules.vgs.cerebro.text.analyzer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.flogger.FluentLogger;
import com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.analyzers.DefaultTextPIIAnalyzer;
import com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.analyzers.NotImplementedTextPIIAnalyzer;
import com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.dto.TextPIIEntity;
import com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.spi.TextPIIAnalyzer;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkFloat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;

@StarlarkBuiltin(
    name = "TextPIIAnalyzer",
    category = "BUILTIN",
    doc = "Overridable Text PII Analysis API in Larky")
public class TextAnalyzerModule implements TextPIIAnalyzer {

  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  //ISO 639 language codes
  private static final Set<String> ISO_LANGUAGES = new HashSet<>
      (Arrays.asList(Locale.getISOLanguages()));

  public static final TextAnalyzerModule INSTANCE = new TextAnalyzerModule();
  public static final String ENABLE_INMEMORY_PROPERTY =
      "larky.modules.vgs.cerebro.text.analyzer.spi.enableInMemoryPIIAnalyzer";

  private final TextPIIAnalyzer textPiiAnalyzer;

  public TextAnalyzerModule() {

    ServiceLoader<TextPIIAnalyzer> loader = ServiceLoader.load(TextPIIAnalyzer.class);
    List<TextPIIAnalyzer> textAnalyzerProviders = ImmutableList.copyOf(loader.iterator());

    if (Boolean.getBoolean(ENABLE_INMEMORY_PROPERTY)) {
      if (!textAnalyzerProviders.isEmpty()) {
        log.at(Level.WARNING).log("Property %s is set to true," +
            " but TextPIIAnalyzer services implementations detected.\nProceeding with default implementation." +
            " To use custom TextPIIAnalyzer service implementation, set property to 'false' or unset it.");
      }
      textPiiAnalyzer = new DefaultTextPIIAnalyzer();
    } else if (textAnalyzerProviders.isEmpty()) {
      textPiiAnalyzer = new NotImplementedTextPIIAnalyzer();
    } else {
      if (textAnalyzerProviders.size() != 1) {
        throw new IllegalArgumentException(String.format(
            "Cerebro expecting only 1 text PII data analyzer provider of type TextPIIAnalyzer, found %d",
            textAnalyzerProviders.size()
        ));
      }

      textPiiAnalyzer = textAnalyzerProviders.get(0);
    }

  }

  @StarlarkMethod(
      name = "analyze",
      doc = "performs PII entities search, given supported language." +
          "@return a list[TextPIIEntity], representing the found entities in given text" +
          "Example:\n" +
          "> message = 'My credit card number is 4095-2609-9393-4932 and my crypto wallet id is 16Yeky6GMjeNkAiNcBY7ZhrLoMSgg1BoyZ.'\n" +
          "> result = pii_analyzer.analyze(message)\n" +
          "> print([e.to_dict() for e in result])\n" +
          "[\n" +
          "    {\n" +
          "        'entity_type': 'CREDIT_CARD',\n" +
          "        'start': 25,\n" +
          "        'end': 44,\n" +
          "        'score': 1.0\n" +
          "    },\n" +
          "    {\n" +
          "        'entity_type': 'CRYPTO',\n" +
          "        'start': 72,\n" +
          "        'end': 106,\n" +
          "        'score': 1.0\n" +
          "    } \n" +
          "]",
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
              defaultValue = "'en'",
              allowedTypes = {
                  @ParamType(type = String.class),
              }),
          @Param(
              name = "entities",
              doc = "list of PII entities that should be looked for in the text.\n" +
                  "If List is empty, analyzer will look for all supported entities for given language.\n" +
                  "Example: ['CREDIT_CARD', 'CRYPTO']",
              named = true,
              defaultValue = "[]",
              allowedTypes = {
                  @ParamType(type = List.class),
              }),
          @Param(
              name = "score_threshold",
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
      doc = "performs PII entities search, given supported language.\n" +
          "@return a list[str] of names of supported PII entities\n" +
          "Example:" +
          "> entities = pii_analyzer.supported_entities()\n" +
          "> print(entities)" +
          "  [\n" +
          "  'PHONE_NUMBER',\n" +
          "  'US_DRIVER_LICENSE',\n" +
          "  'US_PASSPORT',\n" +
          "  'LOCATION',\n" +
          "  'CREDIT_CARD',\n" +
          "  'CRYPTO',\n" +
          "  'UK_NHS',\n" +
          "  'US_SSN',\n" +
          "  'US_BANK_NUMBER',\n" +
          "  'EMAIL_ADDRESS',\n" +
          "  'DATE_TIME',\n" +
          "  'IP_ADDRESS',\n" +
          "  'PERSON',\n" +
          "  'IBAN_CODE',\n" +
          "  'NRP',\n" +
          "  'US_ITIN',\n" +
          "  'DOMAIN_NAME',\n" +
          "  'MEDICAL_LICENS'\n" +
          "]",
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
      doc = "@return list[str] of supported languages in ISO_639-1 format." +
          "Example:\n" +
          "> languages = pii_analyzer.supported_languages()\n" +
          "> print(languages)\n" +
          "['EN']\n")
  @Override
  public List<String> supportedLanguages() throws EvalException {
    return this.textPiiAnalyzer.supportedLanguages();
  }

  private void validateLanguage(String language) throws EvalException {
    if (!ISO_LANGUAGES.contains(language)) {
      throw Starlark.errorf("Provided language: %s is not valid. Language must be ISO_639-1 format.", language);
    }

    if (!this.supportedLanguages().contains(language)) {
      throw Starlark.errorf("Provided language: %s is not currently supported.\nList of supported languages: %s",
          language, this.supportedLanguages());
    }
  }

  private void validateEntities(String language, List<String> entities) throws EvalException {
    Set<String> deduplicatedEntities = Sets.newHashSet(entities);
    deduplicatedEntities.removeAll(this.supportedEntities(language));
    if (!deduplicatedEntities.isEmpty()) {
      throw Starlark.errorf("Requested PII entities: %s are not currently supported. List of supported PII entities: %s",
          deduplicatedEntities, this.supportedEntities(language));
    }
  }
}
