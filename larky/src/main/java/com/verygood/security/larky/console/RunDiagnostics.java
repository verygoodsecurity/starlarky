package com.verygood.security.larky.console;

import com.verygood.security.larky.modules.vgs.vault.VaultAccess;
import javax.annotation.Nullable;
import net.starlark.java.eval.DiagnosticRedactor;
import net.starlark.java.eval.StarlarkThread;

/** Connects reveal observation and print redaction for one execution. */
public final class RunDiagnostics implements AutoCloseable {
  private final DiagnosticRedactor.Scope redaction;
  private final VaultAccess.Scope observation;

  public RunDiagnostics(StarlarkThread thread, @Nullable DiagnosticRedactor redactor) {
    redaction = DiagnosticRedactor.openScope(thread, redactor);
    if (redaction.isTracking()) {
      observation = VaultAccess.observeOutcomes(thread, (execution, outcome) ->
          redaction.registerOutcome(execution, outcome.value(), outcome.revealedValues()));
    } else {
      observation = VaultAccess.suspendObservation(thread);
    }
  }

  public Console printConsole(Console delegate) {
    return RedactingPrintConsole.whenTracking(delegate, redaction);
  }

  @Override
  public void close() {
    try {
      observation.close();
    } finally {
      redaction.close();
    }
  }
}
