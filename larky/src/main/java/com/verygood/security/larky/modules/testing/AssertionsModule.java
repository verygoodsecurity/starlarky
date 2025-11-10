package com.verygood.security.larky.modules.testing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.FormatMethod;
import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import java.util.Objects;

import com.verygood.security.larky.modules.utils.Reporter;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;


@StarlarkBuiltin(
    name = "assertions",
    category = "BUILTIN",
    doc = "This module implements a ")
public class AssertionsModule implements StarlarkValue {

  public static final AssertionsModule INSTANCE = new AssertionsModule();

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
      Objects.requireNonNull(thread.getThreadLocal(Reporter.class)).reportError(thread, msg);
    }
    return Starlark.NONE;
  }

  @StarlarkMethod(
      name = "assert_fails",
      doc = "assert_fails asserts that evaluation of f() fails with the specified error",
      parameters = {
          @Param(name = "f", doc = "the Starlark function to call"),
          @Param(
              name = "wantError",
              doc = "a regular expression matching the expected error message"),
      },
      useStarlarkThread = true)
  public Object assertFails(StarlarkCallable f, String wantError, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Pattern pattern;
    try {
      pattern = Pattern.compile(wantError);
    } catch (PatternSyntaxException unused) {
      throw Starlark.errorf("invalid regexp: %s", wantError);
    }

    String errorMsg;
    try {
      Starlark.call(thread, f, ImmutableList.of(), ImmutableMap.of());
      errorMsg = String.format("evaluation succeeded unexpectedly (want error matching %s)", wantError);
    } catch (Starlark.UncheckedEvalException ex) {
      // Verify error matches UncheckedEvalException message.
      // Use getMessage() directly, which may be overridden to provide formatted output,
      // or fall back to getCause().getMessage() if available.
      String msg = ex.getMessage();
      if (msg == null && ex.getCause() != null) {
        msg = ex.getCause().getMessage();
      }
      if (pattern.matcher(msg).find()) {
        return Starlark.NONE;
      }
      errorMsg = String.format("regular expression (%s) did not match error (%s)", pattern, msg);
    } catch(EvalException ex) {
      // Verify error matches expectation.
      String msg = ex.getMessage();
      if (pattern.matcher(msg).find()) {
        return Starlark.NONE;
      }
      errorMsg = String.format("regular expression (%s) did not match error (%s)", pattern, msg);
    }
    reportErrorf(thread, "%s", errorMsg);
    throw Starlark.errorf("%s", errorMsg);

  }

  @FormatMethod
  private static void reportErrorf(StarlarkThread thread, String format, Object... args) {
    Objects.requireNonNull(thread.getThreadLocal(Reporter.class))
        .reportError(thread, String.format(format, args));
  }


}
