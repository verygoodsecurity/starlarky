package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.types.results.Error;
import com.verygood.security.larky.modules.types.results.Result;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;


@StarlarkBuiltin(
    name = "jresult",
    category = "BUILTIN",
    doc =
      "Given that Starlark does not support exceptions, there needs to be a way to map python" +
      "exception handling and control flow to Starlark. One of the more popular ways to " +
      "do this is to take inspiration from some of the functional programming constructs." +
      "" +
      "Taking a page out of Railway Oriented Programming and the Result type of the Rust" +
      " programming language, we can emulate a Result type that will contain utilities " +
      "that enable functional style programming in Java (which maps well to Starlark) with" +
      " under-the-hood error handling without having to define try-catch-blocks or" +
      " conditionals."
)
public class ResultModule implements StarlarkValue {

  public static final ResultModule INSTANCE = new ResultModule();

  @StarlarkMethod(name = "Error", parameters = {@Param(name = "error")}, useStarlarkThread = true)
  public static Result error(Object error, StarlarkThread thread) {
    // TODO: capture stack frame
    return Result.error(error, thread);
  }

  @StarlarkMethod(name = "Ok", parameters = {@Param(name = "value")})
  public static Result ok(Object value) {
    return Result.ok(value);
  }

  @StarlarkMethod(name = "of", parameters = {@Param(name = "o")}, useStarlarkThread = true)
  public static Result of(Object o, StarlarkThread thread) {
    return Result.of(o, thread);
  }

  @StarlarkMethod(name = "safe",
    parameters = {@Param(name = "func")},
    extraPositionals = @Param(name = "args"),
    extraKeywords = @Param(name = "kwargs", defaultValue = "{}"),
    useStarlarkThread = true
  )
  public static Result safe(StarlarkCallable func, Tuple args, Dict<String, Object> kwargs, StarlarkThread thread) {
    try {
      return ok(Starlark.call(thread, func, args, kwargs));
    } catch(Error e) {
      return e;
    } catch (EvalException e) {
      return Error.of(e); // for the stack trace.
    } catch (InterruptedException | RuntimeException e) {
      return error(new EvalException(e.getMessage(), e.getCause()), thread);
    }
  }


}
