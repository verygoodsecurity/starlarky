package com.verygood.security.larky.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
    Console console;
    try (var scope = DiagnosticRedactor.openScope(redactor)) {
      console = RedactingPrintConsole.whenTracking(delegate, scope);
      console.info("info fixture value");
      console.verbose("verbose fixture value");
    }
    console.info("after fixture value");
    assertEquals(
        List.of("info ***REDACTED***", "verbose ***REDACTED***", "after fixture value"),
        output.toString().lines().toList());
  }

  @Test
  void returnsTheDelegateWhenTrackingIsDisabled() {
    Console delegate = new StreamWriterConsole(new StringWriter());
    try (var unconfigured = DiagnosticRedactor.openScope(null)) {
      assertSame(delegate, RedactingPrintConsole.whenTracking(delegate, unconfigured));
    }
    try (var off = DiagnosticRedactor.openScope(new DiagnosticRedactor(DiagnosticRedactor.Mode.OFF))) {
      assertSame(delegate, RedactingPrintConsole.whenTracking(delegate, off));
    }
  }
}
