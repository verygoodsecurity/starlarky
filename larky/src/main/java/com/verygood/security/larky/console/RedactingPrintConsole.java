package com.verygood.security.larky.console;

import net.starlark.java.eval.DiagnosticRedactor;

/** Masks print lines for one execution and borrows the host's console. */
public final class RedactingPrintConsole extends DelegateConsole {
  private final DiagnosticRedactor.Scope scope;

  private RedactingPrintConsole(Console delegate, DiagnosticRedactor.Scope scope) {
    super(delegate);
    this.scope = scope;
  }

  /** Returns the console for the scope. The host owns the supplied delegate. */
  public static Console whenTracking(Console delegate, DiagnosticRedactor.Scope scope) {
    return scope.isTracking() ? new RedactingPrintConsole(delegate, scope) : delegate;
  }

  @Override
  public void info(String message) {
    super.info(scope.sanitizePrint(message));
  }

  @Override
  public void verbose(String message) {
    super.verbose(scope.sanitizePrint(message));
  }

  @Override
  protected void handleMessage(Message.MessageType type, String message) {}
}
