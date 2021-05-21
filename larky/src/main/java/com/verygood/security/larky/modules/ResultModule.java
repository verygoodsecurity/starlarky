package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.types.results.Result;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkValue;


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

  @StarlarkMethod(name = "Result", doc = "result", structField = true)
  public Result result() {
    return Result.of(Starlark.NONE);
  }
}
