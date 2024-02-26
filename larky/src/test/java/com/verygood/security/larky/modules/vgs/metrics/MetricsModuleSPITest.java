package com.verygood.security.larky.modules.vgs.metrics;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MetricsModuleSPITest {

  private static final Path METRICS_CONFIG_PATH = Paths.get(
    "target", "test-classes", "META-INF", "services",
    "com.verygood.security.larky.modules.vgs.metrics.spi.LarkyMetrics"
  );

  private static String METRICS_SAVED_CONFIG;
  private MetricsModule metrics;

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
    setMetricsImpl("");
    System.setProperty(MetricsModule.ENABLE_DEFAULT_PROPERTY, "false");
    metrics = new MetricsModule();
    // Assert Exceptions
    assertThrows(EvalException.class,
      () -> {
        metrics.track(Dict.empty());
      },
      "metrics.track operation must be overridden"
    );
  }

  @Test
  public void testDefaultModule_ok() throws Exception {
    setMetricsImpl("");
    System.setProperty(MetricsModule.ENABLE_DEFAULT_PROPERTY, "true");
    metrics = new MetricsModule();

    PrintStream originalSystemOut = System.out;
    ByteArrayOutputStream systemOutContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(systemOutContent));

    Map<String, String> map = Map.of("a", "b");
    Dict<String, String> dict = new Dict.Builder<String, String>()
      .putAll(map)
      .buildImmutable();
    metrics.track(dict);

    assertEquals(systemOutContent.toString().trim(), dict.toString().trim());

    System.setOut(originalSystemOut);

  }
}
