package com.verygood.security.larky.stdtypes.structs;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import lombok.Builder;

@Builder
public class Partial implements StarlarkCallable {

  private final StarlarkFunction method;
  private final Tuple func_args;
  private final Dict<String, Object> func_kwargs;

  public static Partial create(StarlarkFunction func,
                               Tuple args,
                               Dict<String, Object> kwargs) {

    return Partial.builder().method(func).func_args(args).func_kwargs(kwargs).build();
  }


  @Override
  @SuppressWarnings("unchecked")
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    kwargs.update(Dict.empty(), this.func_kwargs, thread);
    Tuple joinedArgs = Tuple.concat(this.func_args, args);
    return Starlark.call(thread, this.method, joinedArgs, kwargs);
  }

  /**
   * Returns the form this callable value should take in a stack trace.
   */
  @Override
  public String getName() {
    return /*functools.partial(<function foo at 0x10e0304c0>, 1)*/method.getName();
  }

  @Override
  public void repr(Printer printer) {
    printer
        .append("partial(<function ")
        .append(getName());
    if(this.func_args.size() > 0) {
      printer.append(", args=").append(String.valueOf(this.func_args));
    }
    if(this.func_kwargs.size() > 0) {
      printer.append(", kwargs=").append(String.valueOf(this.func_kwargs));
    }
    printer
        .append(">)");
  }

  @Override
  public String toString() {
    return func_args.toString();
  }
}
