package net.starlark.java.eval;

import java.lang.reflect.Method;
import javax.annotation.Nullable;
import net.starlark.java.syntax.TokenKind;

/**
 * Synchronous callbacks for trusted Java embedders. Result callbacks may return an equivalent
 * Starlark value with different identity, for example to attach run-specific metadata.
 * Callbacks must preserve evaluation semantics and must not throw.
 */
public interface EvaluationHooks {
  default Object afterBinary(
      StarlarkThread thread, TokenKind op, Object left, Object right, Object result,
      boolean augmented) {
    return result;
  }

  default void onBinaryError(
      StarlarkThread thread, TokenKind op, Object left, Object right, boolean augmented,
      EvalException error) {}

  default Object afterUnary(StarlarkThread thread, TokenKind op, Object value, Object result) {
    return result;
  }

  default void onTruthTest(StarlarkThread thread, Object value) {}

  default Object afterIndex(StarlarkThread thread, Object target, Object key, Object result) {
    return result;
  }

  default void onIndexError(StarlarkThread thread, Object target, Object key, EvalException error) {}

  default Object afterSlice(
      StarlarkThread thread, Object target, Object start, Object stop, Object step, Object result) {
    return result;
  }

  /** Reports a resolved attribute, including annotated struct fields. */
  default Object afterFieldRead(StarlarkThread thread, Object target, String name, Object result) {
    return result;
  }

  default void afterFieldWrite(StarlarkThread thread, Object target, String name, Object value) {}

  default Object afterIterationElement(StarlarkThread thread, Object iterable, Object element) {
    return element;
  }

  default void onDuplicateKey(StarlarkThread thread, Object key, EvalException error) {}

  /** Called after a successful call, including one initiated by the embedding host. */
  default Object afterCall(StarlarkThread thread, StarlarkCallable callable, Object result) {
    return result;
  }

  /**
   * Called before invoking an annotated Java method. Annotated struct fields use the field-read
   * callback. The bound arguments are borrowed for this
   * call. Treat the array as read-only and copy anything that must survive the callback.
   * The returned state is passed to the corresponding return or failure callback, including
   * interruption. Virtual machine errors are rethrown immediately.
   */
  @Nullable
  default Object beforeBuiltin(
      StarlarkThread thread, Method method, Object receiver, Object[] arguments) {
    return null;
  }

  default Object afterBuiltin(
      StarlarkThread thread, Method method, Object receiver, Object[] arguments,
      @Nullable Object state, Object result) {
    return result;
  }

  default void onBuiltinError(
      StarlarkThread thread, Method method, Object receiver, Object[] arguments,
      @Nullable Object state, Throwable error) {}
}
