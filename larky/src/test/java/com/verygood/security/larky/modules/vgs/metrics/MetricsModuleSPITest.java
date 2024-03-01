package com.verygood.security.larky.modules.vgs.metrics;

import com.verygood.security.larky.modules.vgs.metrics.constants.Currency;
import com.verygood.security.larky.modules.vgs.metrics.constants.PSP;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionResult;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionType;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.verygood.security.larky.modules.vgs.metrics.DefaultMetrics.OUTPUT_STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MetricsModuleSPITest {

  private static final Path METRICS_CONFIG_PATH = Paths.get(
    "target", "test-classes", "META-INF", "services",
    "com.verygood.security.larky.modules.vgs.metrics.spi.LarkyMetrics"
  );

  private static String METRICS_SAVED_CONFIG;
  private MetricsModule metrics;
  private int amount;
  private int bin;

  @BeforeAll
  public static void setUp() throws Exception {
    METRICS_SAVED_CONFIG = getMetricsImpl();
  }

  @AfterAll
  public static void tearDown() throws Exception {
    setMetricsImpl(METRICS_SAVED_CONFIG);
  }

  private static void setMetricsImpl(String implementationURI) throws Exception {
    Files.writeString(
      METRICS_CONFIG_PATH,
      implementationURI
    );
  }

  private static String getMetricsImpl() throws Exception {
    return new String(Files.readAllBytes(METRICS_CONFIG_PATH));
  }

  @Test
  public void testNoopModule_exception() throws Exception {
    setMetricsImpl(NoopMetrics.class.getName());
    metrics = new MetricsModule();
    // Assert Exceptions
    assertThrows(EvalException.class,
      () -> metrics.track(
        0,
        0,
        Currency.USD,
        PSP.ADYEN,
        TransactionResult.SUCCESS,
        TransactionType.AUTHORIZATION),
      "metrics.track operation must be overridden"
    );
  }

  @Test
  public void testDefaultModule_ok() throws Exception {
    metrics = new MetricsModule();
    PrintStream originalSystemOut = System.out;
    ByteArrayOutputStream systemOutContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(systemOutContent));

    Map<String, String> map = Map.of("a", "b");
    Dict<String, Object> dict = new Dict.Builder<String, Object>()
      .putAll(map)
      .buildImmutable();
    amount = 1234;
    bin = 123456;
    Currency usd = Currency.USD;
    PSP adyen = PSP.ADYEN;
    TransactionResult success = TransactionResult.SUCCESS;
    TransactionType authorization = TransactionType.AUTHORIZATION;
    metrics.track(
      amount,
      bin,
      usd,
      adyen,
      success,
      authorization);

    assertEquals(
      systemOutContent.toString(),
      String.format(OUTPUT_STRING, amount, bin, usd, adyen, success, authorization));

    System.setOut(originalSystemOut);

  }
}
