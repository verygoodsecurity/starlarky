package com.verygood.security.larky.jsr223;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.syntax.Location;

import com.verygood.security.larky.runtime.EvaluationMetadata;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class LarkyEvaluationScriptException extends ScriptException {

  private final @NotNull String excToString;

  // what happened in the evaluation that failed, when an evaluation threw it
  private @Nullable EvaluationMetadata metadata;

  /** For a failure with only a message, found before the script ran (see {@link LarkyGlobalScopeRefusedException}). */
  protected LarkyEvaluationScriptException(@NotNull String message) {
    super(message);
    excToString = super.getMessage();
  }

  private LarkyEvaluationScriptException(Exception e) {
    super(e); // do not construct this exception directly.
    excToString = super.getMessage();
  }

  private LarkyEvaluationScriptException(EvalException evalException) {
    this(evalException, null, -1, -1); // do not construct this exception directly.
  }

  private LarkyEvaluationScriptException(@NotNull EvalException message, String fileName, int lineNumber, int columnNumber) {
    super(message.getMessage(), fileName, lineNumber, columnNumber); // do not construct this exception directly.
    excToString = super.getMessage() + System.lineSeparator() + message.getMessageWithStack();
  }

  @Contract("_ -> new")
  public static @NotNull LarkyEvaluationScriptException of(@NotNull Exception e) {
    final LarkyEvaluationScriptException exception;
    if (e instanceof StarlarkEvalWrapper.Exc.RuntimeEvalException
          || e instanceof Starlark.UncheckedEvalException) {
      exception = onUnchecked(e);
    } else if (e instanceof EvalException) {
      exception = onEvalException((EvalException) e);
    } else {
      exception = new LarkyEvaluationScriptException(e);
    }
    if (e instanceof StarlarkEvalWrapper.Exc.RuntimeEvalException failure) {
      exception.metadata = failure.metadata();
    }
    return exception;
  }

  /**
   * What happened in the evaluation that failed: how it ended ({@link
   * EvaluationMetadata.Outcome#STEP_LIMIT}, {@link EvaluationMetadata.Outcome#DEADLINE} or {@link
   * EvaluationMetadata.Outcome#SCRIPT_ERROR}), its steps and its time. Null for a failure outside
   * an evaluation (converting the bindings, say).
   */
  public @Nullable EvaluationMetadata metadata() {
    return metadata;
  }

  // the evaluation's metadata, for an exception that did not carry it (an EvalException)
  LarkyEvaluationScriptException withMetadataIfAbsent(@Nullable EvaluationMetadata metadata) {
    if (this.metadata == null) {
      this.metadata = metadata;
    }
    return this;
  }

  /**
   * Both {@link Starlark.UncheckedEvalException} and {@link StarlarkEvalWrapper.Exc.RuntimeEvalException} may not have
   * stacktraces as they are derivative of {@link RuntimeException}.
   *
   * As a result, {@link net.starlark.java.eval.StarlarkThread}'s stacktrace might be buried as the second frame instead
   * of the first one.
   */
  @Contract("_ -> new")
  private static @NotNull LarkyEvaluationScriptException onUnchecked(final @NotNull Exception e) {
    if ((e.getCause() instanceof EvalException)) {
      final EvalException cause = (EvalException) e.getCause();
      final LarkyEvaluationScriptException scriptException = onEvalException(cause);
//      scriptException.fillInLarkyStackTrace(cause);
      return scriptException;
    }
    return new LarkyEvaluationScriptException(e);
  }

  @Contract("_ -> new")
  private static @NotNull LarkyEvaluationScriptException onEvalException(final @NotNull EvalException larkyException) {
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
  public void fillInLarkyStackTrace(@NotNull EvalException larkyException) {
    StarlarkEvalWrapper.Exc.fillInLarkyStackTrace(larkyException, this);
  }

}
