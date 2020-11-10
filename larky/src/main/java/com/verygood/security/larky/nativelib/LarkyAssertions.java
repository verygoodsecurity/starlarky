package com.verygood.security.larky.nativelib;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import java.util.List;
import java.util.Objects;


@StarlarkBuiltin(
    name = "asserts",
    category = "BUILTIN",
    doc = "This module implements a ")
public class LarkyAssertions implements StarlarkValue {

  public interface Reporter {
    /**
     * Should be called by an assertion method when the test encounters an unexpected evaluation. It
     * does not stop the program; multiple failures may be reported in a single run.
     */
    default void reportError(StarlarkThread thread, String message) {
      System.err.print("Traceback (most recent call last):\n");
      List<StarlarkThread.CallStackEntry> stack = thread.getCallStack();
      stack = stack.subList(0, stack.size() - 1); // pop the built-in function
      for (StarlarkThread.CallStackEntry fr : stack) {
        System.err.printf("%s: called from %s\n", fr.location, fr.name);
      }
      System.err.println("Error: " + message);
    }
  }

  @StarlarkMethod(
      name = "assert_",
      documented = false,
      parameters = {
        @Param(name = "cond"),
        @Param(name = "msg", defaultValue = "'assertion failed'"),
      },
      useStarlarkThread = true)
  public Object assertStarlark(Object cond, String msg, StarlarkThread thread)
      throws EvalException {
    if (!Starlark.truth(cond)) {
      Objects.requireNonNull(thread.getThreadLocal(Reporter.class))
          .reportError(thread, "assert_: " + msg);
    }
    return Starlark.NONE;
  }

  @StarlarkMethod(
      name = "assert_eq",
      documented = false,
      parameters = {
        @Param(name = "x"),
        @Param(name = "y"),
      },
      useStarlarkThread = true)
  public Object assertEq(Object x, Object y, StarlarkThread thread) throws EvalException {
    if (!x.equals(y)) {
      String msg = String.format("assert_eq: %s != %s", Starlark.repr(x), Starlark.repr(y));
      thread.getThreadLocal(Reporter.class).reportError(thread, msg);
    }
    return Starlark.NONE;
  }

}
