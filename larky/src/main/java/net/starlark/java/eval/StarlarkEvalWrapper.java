package net.starlark.java.eval;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.collect.ImmutableList;

import net.starlark.java.eval.Starlark.UncheckedEvalException;
import net.starlark.java.eval.StarlarkThread.CallStackEntry;
import net.starlark.java.syntax.Location;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  // StarlarkMethod-annotated field or method?
  public static Object getAttrFromMethodAnnotations(
    @Nullable StarlarkThread thread,
    Object x,
    String name
  )  {
    Object result = null;
    if (thread != null) {
      MethodDescriptor method = CallUtils.getAnnotatedMethods(thread.getSemantics(), x.getClass()).get(name);
      if (method != null) {
        if (method.isStructField()) {
          try {
            result = method.callField(x, thread.getSemantics(), thread.mutability());
          } catch (EvalException | InterruptedException e) {
            throw new Exc.RuntimeEvalException(e, thread);
          }
        } else {
          result = new BuiltinFunction(x, name, method);
        }
      }
    }
    return result;
  }

  public interface Exc {

    /**
     * Given a {@link EvalException}, will return, if applicable, the metadata surrounding the location of the exception
     * for the evaluated script.
     *
     * @param larkyException - The {@link EvalException} that contains the Larky stacktrace
     * @return a {@link Location} detailing the filename, line, and row where the error occurred.
     */
    static @Nullable Location getErrorLocation(@NotNull final EvalException larkyException) {

      final ImmutableList<StarlarkThread.CallStackEntry> callStack = larkyException.getCallStack();
      final int n = callStack.size();
      if (callStack.isEmpty()) {
        return null;
      }
      final StarlarkThread.CallStackEntry leaf = callStack.get(n - 1);
      return leaf.location;
    }

    static void fillInStackTraceFromCallStack(
        @NotNull final Throwable throwable,
        @NotNull final ImmutableList<StarlarkThread.CallStackEntry> callStack) {
      final int callStackSize = callStack.size();
      StackTraceElement[] trace = new StackTraceElement[callStackSize];
      for (int i = 0; i < callStackSize; i++) {
        StarlarkThread.CallStackEntry frame = callStack.get(i);
        trace[trace.length - i - 1] = new StackTraceElement(
          /*declaringClass=*/ "<larky>",
          frame.name,
          frame.location.file(),
          frame.location.line());
      }
      throwable.setStackTrace(trace);
    }

    /**
     * Sets the given {@link Throwable}'s stack trace to a Java-style version of {@link StarlarkThread#getCallStack}.
     * This is useful to expose the underlying larky callstack to the caller for a simpler way to identify Larky
     * errors.
     *
     * @param larkyException - The {@link EvalException} that contains the Larky stacktrace
     * @param throwable      - The {@link Throwable} class to hoist the Larky stacktrace above the Java exception
     *                       callstack.
     */
    static void fillInLarkyStackTrace(@NotNull final EvalException larkyException, @NotNull final Throwable throwable) {
      final ImmutableList<StarlarkThread.CallStackEntry> larkyCallStack = larkyException.getCallStack();
      fillInStackTraceFromCallStack(throwable, larkyCallStack);
    }

    static String createUncheckedEvalMessage(Throwable cause, @Nullable StarlarkThread thread) {
      String msg = cause.getClass().getSimpleName() + " thrown during Starlark evaluation";
      String context = null;
      if (thread != null) {
        context = thread.getContextForUncheckedException();
      }
      if (isNullOrEmpty(context)) {
        context = cause.getMessage();
      }
      return msg + " (" + context + ")";
    }

    /**
     * Decorates a {@link RuntimeException} with its Starlark stack, to help maintainers locate problematic source
     * expressions.
     *
     * <p>The original exception can be retrieved using {@link #getCause}.
     */
    final class RuntimeEvalException extends RuntimeException {
      public RuntimeEvalException(Throwable cause, @Nullable StarlarkThread thread) {
        super(createUncheckedEvalMessage(cause, thread), cause);
        if (thread != null) {
          thread.fillInStackTrace(this);
        }
      }

      public RuntimeEvalException(String message, Throwable cause, @Nullable StarlarkThread thread) {
        super(message, cause);
        if (thread != null) {
          thread.fillInStackTrace(this);
        }
      }
    }
    
    final class WrappedUncheckedEvalException extends UncheckedEvalException {
      private WrappedUncheckedEvalException(
          @NotNull final String cause,
          @NotNull final StarlarkThread thread,
          @NotNull final ImmutableList<StarlarkThread.CallStackEntry> callStack) {
        super(new RuntimeException(cause), thread);
        StarlarkEvalWrapper.Exc.fillInStackTraceFromCallStack(this, callStack);
      }
      
      public static WrappedUncheckedEvalException of(
          @NotNull final String cause,
          @NotNull final StarlarkThread thread,
          @NotNull final ImmutableList<StarlarkThread.CallStackEntry> callStack) {
        thread.setUncheckedExceptionContext(
            () -> "in " + callStack.get(callStack.size() - 1).name);
        return new WrappedUncheckedEvalException(cause, thread, callStack);
      }
    }
  }

  public interface CallStack {

    /**
     * Returns the stack frame at the specified depth. 0 means top of stack, 1 is its caller, etc.
     */
    @NotNull
    static Debug.Frame frame(@NotNull StarlarkThread thread, int depth) throws EvalException {
      final int callstackSize = thread.getCallStackSize();
      if(depth > callstackSize) {
        throw Starlark.errorf("depth %d exceeds maximum call stack size", depth);
      }
      return thread.frame(depth);
    }

    /**
     * Reports the current call stack depth.
     *
     * @return <code>0</code> - if an idle thread <br/>
     *         <code>1</code> - if currently evaluating a function for the top-level statements of a file <br/>
     *         <code>2+</code> - which means a function call is in progress and this is the depth of functions
     */
    static int depth(@NotNull StarlarkThread thread) {
      return thread.getCallStackSize();
    }
  }
}
