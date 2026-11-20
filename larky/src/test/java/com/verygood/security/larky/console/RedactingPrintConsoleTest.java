package com.verygood.security.larky.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringWriter;
import java.util.List;
import net.starlark.java.eval.DiagnosticRedactor;
import org.junit.jupiter.api.Test;

class RedactingPrintConsoleTest {
  @Test
  void masksPrintLinesWhileTheScopeIsActive() {
    StringWriter output = new StringWriter();
    Console delegate = new StreamWriterConsole(output, true);
    DiagnosticRedactor redactor = new DiagnosticRedactor(DiagnosticRedactor.Mode.ENFORCE);
    redactor.registerRevealed("fixture value");
    var console = RedactingPrintConsole.whenTracking(delegate, redactor);
    assertNotNull(console);
    try (console) {
      console.info("info fixture value");
      console.verbose("verbose fixture value");
    }
    console.info("after fixture value");
    assertEquals(
        List.of("info ***REDACTED***", "verbose ***REDACTED***", "after fixture value"),
        output.toString().lines().toList());
  }

  @Test
  void createsNoWrapperWhenTrackingIsDisabled() {
    Console delegate = new StreamWriterConsole(new StringWriter());
    assertNull(RedactingPrintConsole.whenTracking(delegate, null));
    assertNull(RedactingPrintConsole.whenTracking(
        delegate, new DiagnosticRedactor(DiagnosticRedactor.Mode.OFF)));
  }
}
