package com.verygood.security.larky.console;

import javax.annotation.Nullable;
import net.starlark.java.eval.DiagnosticRedactor;

/** Masks print lines for one execution and borrows the host's console. */
public final class RedactingPrintConsole extends DelegateConsole {
  @Nullable private DiagnosticRedactor redactor;

  private RedactingPrintConsole(Console delegate, DiagnosticRedactor redactor) {
    super(delegate);
    this.redactor = redactor;
  }

  /** Creates a wrapper only for a run that tracks diagnostics. */
  @Nullable
  public static RedactingPrintConsole whenTracking(
      Console delegate, @Nullable DiagnosticRedactor redactor) {
    return redactor != null && redactor.isTracking()
        ? new RedactingPrintConsole(delegate, redactor)
        : null;
  }

  @Override
  public void info(String message) {
    super.info(sanitize(message));
  }

  @Override
  public void verbose(String message) {
    super.verbose(sanitize(message));
  }

  private String sanitize(String message) {
    return redactor == null ? message : redactor.sanitizePrint(message);
  }

  @Override
  public void close() {
    // A reporter can outlive execution. Release the state and leave the host console open.
    redactor = null;
  }

  @Override
  protected void handleMessage(Message.MessageType type, String message) {}
}
