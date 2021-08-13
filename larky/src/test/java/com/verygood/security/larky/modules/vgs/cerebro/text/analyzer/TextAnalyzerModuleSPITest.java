package com.verygood.security.larky.modules.vgs.cerebro.text.analyzer;

import com.verygood.security.larky.modules.VaultModule;
import com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.dto.TextPIIEntity;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Tests that CerebroTextAnalyzerModule SPI functionality works as expected
public class TextAnalyzerModuleSPITest {

  // This is the path to CerebroTextAnalyzerModule ServiceLoader config in the test classpath
  // Do not reference src/test/resources/META-INF/services because it is not put in the classpath at runtime,
  // and thus not reference by ServiceLoader
  private static final Path TEXT_PII_ANALYZER_CONFIG_PATH = Paths.get(
      "target", "test-classes", "META-INF", "services",
      "com.verygood.security.larky.modules.vgs.cerebro.analyzer.text.spi.TextPIIAnalyzer"
  );
  private static String TEXT_PII_ANALYZER_SAVED_CONFIG;
  private TextAnalyzerModule textAnalyzerModule;

  @BeforeAll
  public static void setUp() throws Exception {
    TEXT_PII_ANALYZER_SAVED_CONFIG = getTextPIIAnalyzerImpl();
  }

  @AfterAll
  public static void tearDown() throws Exception {
    setTextPIIAnalyzerImpl(TEXT_PII_ANALYZER_SAVED_CONFIG);
  }

  @Test
  public void testNoopModule_exception() throws Exception {

    // Arrange
    // Setup Noop Vault
    setTextPIIAnalyzerImpl("");
    System.setProperty(TextAnalyzerModule.ENABLE_INMEMORY_PROPERTY, "false");
    textAnalyzerModule = new TextAnalyzerModule();

    // Act Assert
    // Assert Exceptions
    Assertions.assertThrows(EvalException.class,
        () -> {
          textAnalyzerModule.analyze("some text", "EN", StarlarkList.empty(), null);
        },
        "must be overridden"
    );
    Assertions.assertThrows(EvalException.class,
        () -> {
          textAnalyzerModule.supportedLanguages();
        },
        "must be overridden"
    );
    Assertions.assertThrows(EvalException.class,
        () -> {
          textAnalyzerModule.supportedEntities("EN");
        },
        "must be overridden"
    );
  }

  @Test
  public void testDefaultModule_ok() throws Exception {

    // Arrange
    // Setup Default Vault through system config
    setTextPIIAnalyzerImpl("");
    System.setProperty(TextAnalyzerModule.ENABLE_INMEMORY_PROPERTY, "true");
    textAnalyzerModule = new TextAnalyzerModule();
    List<String> cardNumbers = new ArrayList<>();
    cardNumbers.add("4095-2609-9393-4932");
    cardNumbers.add("4095260993934932");
    cardNumbers.add("4095 2609 9393 4932");

    // Act
    // Invoke Vault
    String testInput = "4095-2609-9393-4932,4095260993934932,4095 2609 9393 4932,12345";
    final List<TextPIIEntity> piiEntities = textAnalyzerModule.analyze(testInput,
        "EN", Collections.EMPTY_LIST, StarlarkFloat.of(0.0));

    // Assert
    Assertions.assertEquals(3, piiEntities.size());
    for (int i = 0; i < 3; ++i) {
      int pii_start = piiEntities.get(i).start().toIntUnchecked();
      int pii_end = piiEntities.get(i).end().toIntUnchecked();
      Assertions.assertEquals(cardNumbers.get(i), testInput.substring(pii_start, pii_end));
    }
  }

  @Test
  public void testDefaultModule_analyze_notSupportedLanguage_exception() throws Exception {

    // Arrange
    // Setup Default Vault through system config
    setTextPIIAnalyzerImpl("");
    System.setProperty(TextAnalyzerModule.ENABLE_INMEMORY_PROPERTY, "true");
    textAnalyzerModule = new TextAnalyzerModule();
    String language = "ES";

    // Act
    // Invoke Vault
    String testInput = "4095-2609-9393-4932,4095260993934932,4095 2609 9393 4932,12345";

    // Assert
    Assertions.assertThrows(EvalException.class,
        () -> {
          textAnalyzerModule.supportedEntities("ES");
        },
        String.format("Provided language: %s is not currently supported.\nList of supported languages: [EN]",
            language)
    );
  }

  @Test
  public void testDefaultModule_analyze_notSupportedEntities_exception() throws Exception {

    // Arrange
    // Setup Default Vault through system config
    setTextPIIAnalyzerImpl("");
    System.setProperty(TextAnalyzerModule.ENABLE_INMEMORY_PROPERTY, "true");
    textAnalyzerModule = new TextAnalyzerModule();
    List<String> entities = new ArrayList<>();
    entities.add("CARD_NUMBER");
    entities.add("BLA_BLA_BLA");

    // Act
    // Assert
    Assertions.assertThrows(EvalException.class,
        () -> {
          textAnalyzerModule.analyze("Sth, sth", "EN", entities, StarlarkFloat.of(0.0));
        },
        "Requested PII entities: [BLA_BLA_BLA] are not currently supported.\n" +
            "List of supported PII entities: [CARD_NUMBER]"
    );
  }

  @Test
  public void testDefaultModule_supporteEntities_notSupportedEntities_exception() throws Exception {

    // Arrange
    // Setup Default Vault through system config
    setTextPIIAnalyzerImpl("");
    System.setProperty(TextAnalyzerModule.ENABLE_INMEMORY_PROPERTY, "true");
    textAnalyzerModule = new TextAnalyzerModule();
    String language = "ES";

    // Act
    // Invoke Vault
    String testInput = "4095-2609-9393-4932,4095260993934932,4095 2609 9393 4932,12345";

    // Assert
    Assertions.assertThrows(EvalException.class,
        () -> {
          textAnalyzerModule.analyze(testInput,"ES", Collections.EMPTY_LIST, StarlarkFloat.of(0.0));
        },
        String.format("Provided language: %s is not currently supported.\nList of supported languages: [EN]",
            language)
    );
  }

  @Test
  public void testSPIModule_single_ok() throws Exception {
    // Setup Default Vault through SPI config
    setTextPIIAnalyzerImpl("com.verygood.security.larky.modules.vgs.cerebro.analyzer.text.analyzers.DefaultTextPIIAnalyzer");
    textAnalyzerModule = new TextAnalyzerModule();
    List<String> cardNumbers = new ArrayList<>();
    cardNumbers.add("4095-2609-9393-4932");
    cardNumbers.add("4095260993934932");
    cardNumbers.add("4095 2609 9393 4932");


    // Invoke Vault
    String testInput = "4095-2609-9393-4932,4095260993934932,4095 2609 9393 4932,12345";
    final List<TextPIIEntity> piiEntities = textAnalyzerModule.analyze(testInput,
        "EN", Collections.EMPTY_LIST, StarlarkFloat.of(0.0));

    // Assert OK
    Assertions.assertEquals(3, piiEntities.size());
    for (int i = 0; i < 3; ++i) {
      int pii_start = piiEntities.get(i).start().toIntUnchecked();
      int pii_end = piiEntities.get(i).end().toIntUnchecked();
      Assertions.assertEquals(cardNumbers.get(i), testInput.substring(pii_start, pii_end));
    }
  }

  @Test
  public void testSPIModule_multiple_exception() throws Exception {

    // Setup multiple vault SPI configs
    setTextPIIAnalyzerImpl("com.verygood.security.larky.modules.vgs.cerebro.analyzer.text.analyzers.DefaultTextPIIAnalyzer\n"
        + "com.verygood.security.larky.modules.vgs.cerebro.analyzer.text.analyzers.NoopTextPIIAnalyzer\n");
    System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "false");

    // Assert Exception
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> {
          textAnalyzerModule = new TextAnalyzerModule();
        },
        "Cerebro expecting only 1 text PII data analyzer provider of type TextPiiAnalyzer, found 2"
    );
  }

  private static void setTextPIIAnalyzerImpl(String implementationURI) throws Exception {
    Files.write(
        TEXT_PII_ANALYZER_CONFIG_PATH,
        implementationURI.getBytes(StandardCharsets.UTF_8)
    );
  }

  private static String getTextPIIAnalyzerImpl() throws Exception {
    return new String(Files.readAllBytes(TEXT_PII_ANALYZER_CONFIG_PATH));
  }
}
