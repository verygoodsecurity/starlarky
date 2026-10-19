package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.types.results.Error;
import com.verygood.security.larky.modules.types.results.Result;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkSemantics;
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
  public static Result safe(StarlarkCallable func, Tuple args, Dict<String, Object> kwargs, StarlarkThread thread)
      throws InterruptedException {
    int stackSize = StarlarkEvalWrapper.callStackSize(thread);
    try {
      return ok(Starlark.call(thread, func, args, kwargs));
    } catch(Error e) {
      StarlarkEvalWrapper.unwindCallStack(thread, stackSize);
      return e;
    } catch (EvalException e) {
      StarlarkEvalWrapper.unwindCallStack(thread, stackSize);
      return Error.of(e); // for the stack trace.
    } catch (RuntimeException e) {
      StarlarkEvalWrapper.unwindCallStack(thread, stackSize);
      return error(new EvalException(e.getMessage(), e.getCause()), thread);
    }
    // InterruptedException propagates: an interrupted evaluation must stop.
  }

  @StarlarkMethod(
      name = "safe_wrap",
      doc = "Returns a callable that calls <code>func</code> through <code>safe</code>.",
      parameters = {@Param(name = "func")})
  public static SafeFunction safeWrap(StarlarkCallable func) {
    return new SafeFunction(func);
  }

  /**
   * What {@code safe(func)} returns. It is a Java callable rather than a Starlark closure: the
   * recursion check compares a Starlark function's code, so every closure made by one {@code def
   * decorator} counted as the same function, and {@code safe(f)()} failed with "function
   * 'decorator' called recursively" as soon as {@code f} called any other {@code safe(g)()}. A
   * Java callable is not checked itself; the functions it calls still are, so real recursion
   * (e.g. {@code f} calling {@code safe(f)()}) is still refused. Its type is {@code function}
   * and its repr is {@code <function safe(f)>}.
   */
  @StarlarkBuiltin(name = "function", documented = false)
  public static final class SafeFunction implements StarlarkCallable {
    private final StarlarkCallable func;

    SafeFunction(StarlarkCallable func) {
      this.func = func;
    }

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs)
        throws EvalException, InterruptedException {
      return safe(func, args, kwargs, thread);
    }

    /** {@code safe(<name of the wrapped function>)}, e.g. {@code safe(parse_body)}. */
    @Override
    public String getName() {
      return "safe(" + func.getName() + ")";
    }

    @Override
    public boolean isImmutable() {
      return func.isImmutable();
    }

    @Override
    public void repr(Printer printer, StarlarkSemantics semantics) {
      printer.append("<function ").append(getName()).append(">");
    }
  }


}
