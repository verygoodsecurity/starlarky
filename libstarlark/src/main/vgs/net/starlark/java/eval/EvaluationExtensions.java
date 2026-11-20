package net.starlark.java.eval;

import javax.annotation.Nullable;
import net.starlark.java.syntax.TokenKind;

/** Optional runtime extensions installed by a trusted host for one Starlark execution. */
public final class EvaluationExtensions {
  /** The interpreter's original implementation for the current operation. */
  @FunctionalInterface
  public interface BinaryOperation {
    Object apply(TokenKind op, Object left, Object right, StarlarkThread thread)
        throws EvalException;
  }

  /**
   * Replaces an eager binary operation executed at runtime. The compiler can fold literal
   * expressions before evaluation. Delegate at most once, as augmented assignment can mutate its
   * left operand. Return a valid Starlark value of the type expected by the execution.
   */
  @FunctionalInterface
  public interface BinaryOperationOverride {
    Object apply(
        BinaryOperation upstream, TokenKind op, Object left, Object right, StarlarkThread thread,
        boolean augmented) throws EvalException;
  }

  @Nullable private final EvaluationHooks hooks;
  @Nullable private final BinaryOperationOverride binaryOverride;

  public EvaluationExtensions(EvaluationHooks hooks) {
    this(hooks, null);
  }

  public EvaluationExtensions(
      @Nullable EvaluationHooks hooks, @Nullable BinaryOperationOverride binaryOverride) {
    this.hooks = hooks;
    this.binaryOverride = binaryOverride;
  }

  /**
   * Installs these extensions until the returned scope closes. Close nested scopes in reverse order
   * on the same execution thread. If the host caches evaluated modules, isolate their results by
   * run when extensions can change results or retain run-specific state.
   */
  public Scope installOn(StarlarkThread thread) {
    Scope scope = new Scope(thread, thread.evaluationExtensions);
    thread.evaluationExtensions = this;
    return scope;
  }

  public static final class Scope implements AutoCloseable {
    @Nullable private StarlarkThread thread;
    @Nullable private EvaluationExtensions previous;

    private Scope(StarlarkThread thread, @Nullable EvaluationExtensions previous) {
      this.thread = thread;
      this.previous = previous;
    }

    @Override
    public void close() {
      if (thread != null) {
        thread.evaluationExtensions = previous;
        thread = null;
        previous = null;
      }
    }
  }

  @Nullable
  EvaluationHooks hooks() {
    return hooks;
  }

  /** Reports a successfully resolved field from an interpreter or host attribute implementation. */
  public static Object afterFieldRead(
      @Nullable StarlarkThread thread, Object target, String name, Object result) {
    EvaluationHooks hooks = thread == null ? null : thread.evaluationHooks();
    return hooks == null ? result : hooks.afterFieldRead(thread, target, name, result);
  }

  Object binary(
      BinaryOperation upstream, TokenKind op, Object left, Object right, StarlarkThread thread,
      boolean augmented) throws EvalException {
    Object result;
    try {
      result = binaryOverride == null
          ? upstream.apply(op, left, right, thread)
          : binaryOverride.apply(upstream, op, left, right, thread, augmented);
    } catch (EvalException error) {
      if (hooks != null) {
        hooks.onBinaryError(thread, op, left, right, augmented, error);
      }
      throw error;
    }
    return hooks == null ? result : hooks.afterBinary(thread, op, left, right, result, augmented);
  }
}
