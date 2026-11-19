package net.starlark.java.eval;

import static com.google.common.truth.Truth.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import org.junit.Test;

public class DiagnosticRedactorTest {
  @Test
  public void masksKnownEncodingsAndPreservesSurroundingText() {
    DiagnosticRedactor redactor = new DiagnosticRedactor(DiagnosticRedactor.Mode.ENFORCE);
    String value = "item /?";
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    redactor.registerRevealed(value);

    assertThat(redactor.sanitizeError("raw=" + value
        + " url=item+%2F%3F"
        + " b64=" + Base64.getEncoder().encodeToString(bytes)
        + " b64url=" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        + " hex=" + HexFormat.of().withUpperCase().formatHex(bytes)))
        .isEqualTo("raw=***REDACTED*** url=***REDACTED*** b64=***REDACTED***"
            + " b64url=***REDACTED*** hex=***REDACTED***");
  }

  @Test
  public void mergesOverlapsAndLeavesInsertedMasksIntact() {
    DiagnosticRedactor redactor = new DiagnosticRedactor(DiagnosticRedactor.Mode.ENFORCE);
    redactor.registerRevealed(List.of("abcdef", "defxyz", "RED"));

    assertThat(redactor.sanitizeError("value=abcdefxyz and RED"))
        .isEqualTo("value=***REDACTED*** and ***REDACTED***");
  }

  @Test
  public void recognizesOriginalBytesInBinaryEncodings() {
    DiagnosticRedactor redactor = new DiagnosticRedactor(DiagnosticRedactor.Mode.ENFORCE);
    redactor.registerRevealed(StarlarkBytes.of(Mutability.IMMUTABLE, (byte) 0xff, (byte) 0xfe,
        (byte) 0, (byte) 1));

    assertThat(redactor.sanitizeError("hex=FFFE0001 b64=//4AAQ=="))
        .isEqualTo("hex=***REDACTED*** b64=***REDACTED***");
  }

  @Test
  public void recognizesEscapedContainerTextAndJavaBytes() {
    DiagnosticRedactor redactor = new DiagnosticRedactor(DiagnosticRedactor.Mode.ENFORCE);
    redactor.registerRevealed("some \"quoted\" text");
    byte[] bytes = {(byte) 0xff, (byte) 0x2f, (byte) 0x20, (byte) 0x7e};
    redactor.registerRevealed(bytes);

    assertThat(redactor.sanitizeError(
        "[\"some \\\"quoted\\\" text\", " + StarlarkBytes.quoteBytes(bytes) + "] %FF/%20~"))
        .isEqualTo("[\"***REDACTED***\", b\"***REDACTED***\"] ***REDACTED***");
  }

  @Test
  public void repeatedRegistrationConsumesTheRunBudget() {
    DiagnosticRedactor redactor = new DiagnosticRedactor(DiagnosticRedactor.Mode.ENFORCE);
    redactor.registerRevealed(Collections.nCopies(500, "fixture".repeat(100)));

    assertThat(redactor.sanitizeError("diagnostic")).isEqualTo(DiagnosticRedactor.MASK);
  }

  @Test
  public void resourceLimitReportsAndMasksTheWholeDiagnosticInEnforce() {
    List<DiagnosticRedactor.Reason> reasons = new ArrayList<>();
    DiagnosticRedactor redactor = new DiagnosticRedactor(
        DiagnosticRedactor.Mode.ENFORCE, (sink, reason) -> reasons.add(reason));
    redactor.registerRevealed("x".repeat(16_385));

    assertThat(redactor.sanitizeError("failed")).isEqualTo(DiagnosticRedactor.MASK);
    assertThat(reasons).containsExactly(DiagnosticRedactor.Reason.LIMIT);
  }
}
