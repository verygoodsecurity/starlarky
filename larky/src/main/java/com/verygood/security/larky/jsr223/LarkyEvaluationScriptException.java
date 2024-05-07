package com.verygood.security.larky.jsr223;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.syntax.Location;

import jakarta.annotation.Nonnull;

import javax.script.ScriptException;

public class LarkyEvaluationScriptException extends ScriptException {

  private final @Nonnull String excToString;

  private LarkyEvaluationScriptException(Exception e) {
    super(e); // do not construct this exception directly.
    excToString = super.getMessage();
  }

  private LarkyEvaluationScriptException(EvalException evalException) {
    this(evalException, null, -1, -1); // do not construct this exception directly.
  }

  private LarkyEvaluationScriptException(@Nonnull EvalException message, String fileName, int lineNumber, int columnNumber) {
    super(message.getMessage(), fileName, lineNumber, columnNumber); // do not construct this exception directly.
    excToString = super.getMessage() + System.lineSeparator() + message.getMessageWithStack();
  }

  public static @Nonnull LarkyEvaluationScriptException of(@Nonnull Exception e) {
    if (e instanceof StarlarkEvalWrapper.Exc.RuntimeEvalException
          || e instanceof Starlark.UncheckedEvalException) {
      return onUnchecked(e);
    } else if (e instanceof EvalException) {
      return onEvalException((EvalException) e);
    } else {
      return new LarkyEvaluationScriptException(e);
    }
  }

  /**
   * Both {@link Starlark.UncheckedEvalException} and {@link StarlarkEvalWrapper.Exc.RuntimeEvalException} may not have
   * stacktraces as they are derivative of {@link RuntimeException}.
   *
   * As a result, {@link net.starlark.java.eval.StarlarkThread}'s stacktrace might be buried as the second frame instead
   * of the first one.
   */
  private static @Nonnull LarkyEvaluationScriptException onUnchecked(final @Nonnull Exception e) {
    if ((e.getCause() instanceof EvalException)) {
      final EvalException cause = (EvalException) e.getCause();
      final LarkyEvaluationScriptException scriptException = onEvalException(cause);
//      scriptException.fillInLarkyStackTrace(cause);
      return scriptException;
    }
    return new LarkyEvaluationScriptException(e);
  }

  private static @Nonnull LarkyEvaluationScriptException onEvalException(final @Nonnull EvalException larkyException) {
    final LarkyEvaluationScriptException exception;
    final Location errorLoc = StarlarkEvalWrapper.Exc.getErrorLocation(larkyException);
    if (errorLoc != null) {
      exception = new LarkyEvaluationScriptException(
        larkyException,
        errorLoc.file(),
        errorLoc.line(),
        errorLoc.column()
      );
    } else {
      exception = new LarkyEvaluationScriptException(larkyException);
    }
    return exception;
  }

  @Override
  public String toString() {
    return excToString;
  }

  /**
   * Helper method that helps fill in the stack trace from an external {@link EvalException}. This will just invoke
   * {@link StarlarkEvalWrapper.Exc#fillInLarkyStackTrace(EvalException, Throwable)} with the current exception.
   *
   * @param larkyException - The {@link EvalException} that contains the Larky stacktrace
   */
  public void fillInLarkyStackTrace(@Nonnull EvalException larkyException) {
    StarlarkEvalWrapper.Exc.fillInLarkyStackTrace(larkyException, this);
  }

}
