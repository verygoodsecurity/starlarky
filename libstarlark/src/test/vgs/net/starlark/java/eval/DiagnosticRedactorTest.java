package net.starlark.java.eval;

import static com.google.common.truth.Truth.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import net.starlark.java.syntax.TokenKind;
import org.junit.Test;

public class DiagnosticRedactorTest {
  @Test
  public void binaryObservationCoexistsWithAHostObserverAndRestoresNestedScopes() throws Exception {
    DiagnosticRedactor redactor = new DiagnosticRedactor(DiagnosticRedactor.Mode.ENFORCE);
    List<Object> outputs = new ArrayList<>();
    try (Mutability mu = Mutability.create("nested diagnostic scopes")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      try (var host = EvaluationExtensions.observeBinary(thread,
          (execution, op, left, right, result, augmented) -> {
            outputs.add(result);
            return result;
          })) {
        try (var diagnostics = DiagnosticRedactor.openScope(thread, redactor)) {
          String source = "fixture";
          Object hit = diagnostics.registerOutcome(thread, source, List.of(source));
          assertThat(hit).isEqualTo(source);
          assertThat(hit).isNotSameInstanceAs(source);
          assertThat(EvalUtils.binaryOp(TokenKind.PLUS, hit, "-tracked", thread))
              .isEqualTo("fixture-tracked");
          try (var off = DiagnosticRedactor.openScope(thread, null)) {
            assertThat(off.printer()).isNull();
            assertThat(EvalUtils.binaryOp(TokenKind.PLUS, hit, "-off", thread))
                .isEqualTo("fixture-off");
          }
          assertThat(thread.getThreadLocal(DiagnosticRedactor.Scope.class))
              .isSameInstanceAs(diagnostics);
        }
        assertThat(thread.getThreadLocal(DiagnosticRedactor.Scope.class)).isNull();
        EvalUtils.binaryOp(TokenKind.PLUS, "public", "-after", thread);
      }
    }
    assertThat(outputs).containsExactly("fixture-tracked", "fixture-off", "public-after").inOrder();
  }

  @Test
  public void boundIncludesTheSizeOfRetainedDerivedValues() throws Exception {
    var limits = DiagnosticRedactor.Limits.builder().maxRegistrationInput(96).build();
    var redactor = new DiagnosticRedactor(DiagnosticRedactor.Mode.ENFORCE, limits);
    try (Mutability mu = Mutability.create("retained size budget")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      try (var diagnostics = DiagnosticRedactor.openScope(thread, redactor)) {
        String source = "fixture";
        Object hit = diagnostics.registerOutcome(thread, source, List.of(source));
        assertThat(EvalUtils.binaryOp(TokenKind.PLUS, "x".repeat(80), hit, thread))
            .isEqualTo("x".repeat(80) + source);
        assertThat(diagnostics.sanitizePrint("over budget")).isEqualTo(DiagnosticRedactor.MASK);
      }
    }
  }

  @Test
  public void closingTheScopeDetachesExecutionCallbacks() {
    DiagnosticRedactor redactor = new DiagnosticRedactor(DiagnosticRedactor.Mode.ENFORCE);
    var scope = DiagnosticRedactor.openScope(redactor);
    try (scope) {
      scope.registerRevealed(List.of("active fixture"));
      assertThat(scope.sanitizePrint("active fixture")).isEqualTo("***REDACTED***");
    }
    scope.registerRevealed(List.of("later fixture"));
    assertThat(scope.isTracking()).isFalse();
    assertThat(scope.sanitizePrint("active fixture")).isEqualTo("active fixture");
    assertThat(redactor.sanitizeError("active fixture / later fixture"))
        .isEqualTo("***REDACTED*** / later fixture");
  }

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

  @Test
  public void hostCanOverrideMinimumLengthForOneRun() {
    DiagnosticRedactor custom = new DiagnosticRedactor(
        DiagnosticRedactor.Mode.ENFORCE, DiagnosticRedactor.Limits.builder().minLength(2).build());
    DiagnosticRedactor defaults = new DiagnosticRedactor(DiagnosticRedactor.Mode.ENFORCE);

    custom.registerRevealed("xy");
    defaults.registerRevealed("xy");

    assertThat(custom.sanitizeError("value=xy")).isEqualTo("value=***REDACTED***");
    assertThat(defaults.sanitizeError("value=xy")).isEqualTo("value=xy");
  }

  @Test
  public void hostConfiguredBudgetsTriggerTheExistingFallback() {
    List<DiagnosticRedactor.Limits> limits = List.of(
        DiagnosticRedactor.Limits.builder().maxSourceLength(1).build(),
        DiagnosticRedactor.Limits.builder().maxForms(1).build(),
        DiagnosticRedactor.Limits.builder().maxFormChars(1).build(),
        DiagnosticRedactor.Limits.builder().maxRegistrationInput(1).build(),
        DiagnosticRedactor.Limits.builder().maxTextLength(1).build(),
        DiagnosticRedactor.Limits.builder().maxScanWork(1).build());
    for (DiagnosticRedactor.Limits limit : limits) {
      DiagnosticRedactor redactor = new DiagnosticRedactor(DiagnosticRedactor.Mode.ENFORCE, limit);
      redactor.registerRevealed("fixture value");
      assertThat(redactor.sanitizeError("ordinary diagnostic")).isEqualTo(DiagnosticRedactor.MASK);
    }
  }
}
