package net.starlark.java.eval;

import static com.google.common.base.Strings.isNullOrEmpty;

public class StarlarkEvalWrapper {

  private StarlarkEvalWrapper() {
  } // uninstantiable

  /**
   * Publicly exposes the {@link StarlarkList#wrap} method to allow list creation via array ownership transfer to allow
   * "zero-copy" StarlarkList creation. Takes ownership of the supplied array.
   *
   * Same limitations of the package-private {@link StarlarkList#wrap} method apply. The caller
   * <b>MUST NOT</b> subsequently modify the array.n
   *
   * @param mu  The {@link Mutability} of the list.
   * @param arr The array to take ownership
   * @param <T> The type of elements in the array
   * @return A {@link StarlarkList} which takes ownership of the supplied {@code arr}
   */
  public static <T> StarlarkList<?> zeroCopyList(Mutability mu, T[] arr) {
    return StarlarkList.wrap(mu, arr);
  }

  /**
   * Defines the strict weak ordering of Starlark values used for sorting and the comparison operators. Throws
   * ClassCastException on failure.
   */
  public static int compareUnchecked(Object x, Object y) {
    return Starlark.compareUnchecked(x, y);
  }

  public static StarlarkInt ofFiniteDouble(double x) {
    return StarlarkFloat.finiteDoubleToIntExact(x);
  }

  public interface Exc {
    static String createUncheckedEvalMessage(Throwable cause, StarlarkThread thread) {
      String msg = cause.getClass().getSimpleName() + " thrown during Starlark evaluation";
      String context = thread.getContextForUncheckedException();
      return isNullOrEmpty(context) ? msg : msg + " (" + context + ")";
    }

    /**
     * Decorates a {@link RuntimeException} with its Starlark stack, to help maintainers locate problematic source
     * expressions.
     *
     * <p>The original exception can be retrieved using {@link #getCause}.
     */
    final class RuntimeEvalException extends RuntimeException {
      public RuntimeEvalException(Throwable cause, StarlarkThread thread) {
        super(createUncheckedEvalMessage(cause, thread), cause);
        thread.fillInStackTrace(this);
      }
    }
  }
}
