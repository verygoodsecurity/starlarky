package net.starlark.java.eval;

import java.util.Objects;
import javax.annotation.Nullable;
import net.starlark.java.syntax.TokenKind;

/** Optional runtime observation for trusted Java embedders. */
public final class EvaluationExtensions {
  private EvaluationExtensions() {}

  /**
   * Called after a successful eager binary operation. The observer may return an equivalent
   * Starlark value with a different identity to attach run-specific metadata. It must preserve
   * evaluation semantics and must not throw. Preserve the result's identity when it is visible
   * to scripts, such as the existing list returned by an in-place addition.
   */
  @FunctionalInterface
  public interface BinaryObserver {
    Object afterBinary(
        StarlarkThread thread, TokenKind op, Object left, Object right, Object result,
        boolean augmented);
  }

  /**
   * Installs the observer until the returned scope closes. Close nested scopes in reverse order on
   * the same execution thread. Isolate evaluated-module caches by run when results may hold
   * run-specific metadata.
   */
  public static Scope observeBinary(StarlarkThread thread, BinaryObserver observer) {
    Objects.requireNonNull(observer);
    Scope scope = new Scope(thread, thread.binaryObserver);
    thread.binaryObserver = observer;
    return scope;
  }

  public static final class Scope implements AutoCloseable {
    @Nullable private StarlarkThread thread;
    @Nullable private BinaryObserver previous;

    private Scope(StarlarkThread thread, @Nullable BinaryObserver previous) {
      this.thread = thread;
      this.previous = previous;
    }

    @Override
    public void close() {
      if (thread != null) {
        thread.binaryObserver = previous;
        thread = null;
        previous = null;
      }
    }
  }
}
