package com.verygood.security.larky.modules.types;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

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
  private final String methodName;

  // hide lombok's builder method
  // this allows us to cache the method name
  private static class PartialBuilder {
  }

  public static Partial create(StarlarkFunction func,
                               Tuple args,
                               Dict<String, Object> kwargs) {
    return Partial.builder()
             .method(func)
             .methodName(generateMethodName(func, args, kwargs))
             .func_args(args)
             .func_kwargs(kwargs)
             .build();
  }

  private static String generateMethodName(StarlarkFunction func, Tuple args, Dict<String, Object> kwargs) {
    // This allows us to cache the method name string so we do not calculate it
    // in getName() everytime
    int arg_size = args.size();
    int kwarg_size = kwargs.values().size();
    // this helps us avoid a trailing _ on the partial method name if there are no args.
    if (arg_size == 0 && kwarg_size == 0) {
      return func.getName();
    }
    int i = 0;
    // max length is method name + _ (2+) each element gets a _ appended (size()*2)
    StringBuilder sb = new StringBuilder((arg_size + kwarg_size) * 2 + 2);
    sb.append(func.getName()).append("_");
    for (i = 0; i < arg_size; i++) {
      String str = Starlark.str(args.get(i));
      if (str.contains("built-in function")) {
        str = str
                .replace("<built-in function ", "")
                .replace(">", "");
      }
      sb.append(str).append('_');
    }
    i = 0;
    for (Object a : kwargs.values()) {
      String str = Starlark.str(a);
      if (str.contains("built-in function")) {
        str = str
                .replace("<built-in function ", "")
                .replace(">", "");
      }
      sb.append(str);
      if (++i < kwarg_size) { // ++i to avoid a trailing _ added to the method name
        sb.append('_');
      }
    }
    // remove any trailing _
    sb.trimToSize();
    if(sb.subSequence(sb.length()-1,sb.length()).equals("_")) {
      sb.deleteCharAt(sb.length()-1);
    }
    return sb.toString();
  }


  @Override
  @SuppressWarnings("unchecked")
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    final ImmutableList<String> intersection = ImmutableList.copyOf(Sets.intersection(
      ImmutableSet.copyOf(this.method.getParameterNames()),
      this.func_kwargs.keySet()));

    final Tuple joinedArgs;
    if(intersection.isEmpty()) {
      kwargs.update(Dict.empty(), this.func_kwargs, thread);
      joinedArgs = Tuple.concat(this.func_args, args);
    } else {
      joinedArgs = Tuple.concat(
        Tuple.of(this.func_kwargs.get(intersection.get(0))),
        args
      );
    }
    return Starlark.call(thread, this.method, joinedArgs, kwargs);
  }

  /**
   * Returns the form this callable value should take in a stack trace.
   */
  @Override
  public String getName() {
    /*functools.partial(<function foo at 0x10e0304c0>, 1)*/
    return methodName;
  }

  @Override
  public void repr(Printer printer) {
    printer
      .append("partial(<function ")
      .append(getName());
    if (this.func_args.size() > 0) {
      printer.append(", args=");
      this.func_args.repr(printer);
    }
    if (this.func_kwargs.size() > 0) {
      printer.append(", kwargs=");
      this.func_kwargs.repr(printer);
    }
    printer
      .append(">)");
  }

  @Override
  public void debugPrint(Printer printer) {
    printer
      .append("partial(<function ")
      .append(getName());
    if (this.func_args.size() > 0) {
      printer.append(", args=");
      this.func_args.debugPrint(printer);
    }
    if (this.func_kwargs.size() > 0) {
      printer.append(", kwargs=");
      this.func_kwargs.debugPrint(printer);
    }
    printer
      .append(">)");
  }

  @Override
  public String toString() {
    return func_args.toString();
  }
}
