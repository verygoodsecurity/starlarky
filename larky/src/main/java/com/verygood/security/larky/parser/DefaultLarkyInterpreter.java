package com.verygood.security.larky.parser;

import com.verygood.security.larky.console.Console;
import java.io.IOException;
import java.io.Writer;
import javax.script.Bindings;
import net.starlark.java.eval.EvalException;

/** @deprecated Use {@link com.verygood.security.larky.runtime.DefaultLarkyInterpreter}. */
@Deprecated
public class DefaultLarkyInterpreter {

  public static final String VERBOSE =
      com.verygood.security.larky.runtime.DefaultLarkyInterpreter.VERBOSE;

  private final com.verygood.security.larky.runtime.DefaultLarkyInterpreter delegate;

  public DefaultLarkyInterpreter(LarkyScript.StarlarkMode mode, Bindings... bindings) {
    try {
      delegate = new com.verygood.security.larky.runtime.DefaultLarkyInterpreter(mode, bindings);
    } catch (EvalException e) {
      // The published constructor has no checked exceptions.
      throw new IllegalArgumentException("could not convert Starlark bindings", e);
    }
  }

  public ParsedStarFile evaluate(StarFile script, Writer writer) throws IOException, EvalException {
    return delegate.evaluate(script, writer);
  }

  public ParsedStarFile evaluate(StarFile script, Console console) throws IOException, EvalException {
    return delegate.evaluate(script, console);
  }
}
