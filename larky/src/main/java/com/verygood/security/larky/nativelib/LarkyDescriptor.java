package com.verygood.security.larky.nativelib;

import net.starlark.java.annot.StarlarkAnnotations;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import javax.annotation.Nullable;
import lombok.Builder;

@Builder
public class LarkyDescriptor implements StarlarkValue {

  final private StarlarkCallable callable;
  final private StarlarkThread thread;

  @StarlarkMethod(
          name = "get",
          doc = "call",
          structField = true
  )
  public Object get() throws InterruptedException, EvalException {
    return Starlark.call(this.thread, this.callable, Tuple.empty(), Dict.empty());
  }

//  @StarlarkMethod(
//      name = "descriptor",
//      doc = "call",
//      parameters = {
//          @Param(
//              name = "callable"
//          )
//      },
//      useStarlarkThread = true,
//      selfCall = true
//  )
//  public LarkyDescriptor create(Object callable, StarlarkThread thread) throws EvalException {
//    return new LarkyDescriptorImpl((StarlarkCallable) callable, thread);
//  }

  public Object call() throws NoSuchMethodException, InterruptedException, EvalException {
    return call(null, null);
  }
  /**
   * Invokes this method using {@code obj} as a target and {@code args} as Java arguments.
   *
   * <p>Methods with {@code void} return type return {@code None} following Python convention.
   *
   * <p>The Mutability is used if it is necessary to allocate a Starlark copy of a Java result.
   */
  public Object call(Object[] args, @Nullable Mutability mu) throws NoSuchMethodException, EvalException, InterruptedException {
    Object result;
    Method method = this.getClass().getMethod("get");

    try {
      result = method.invoke(this);
    } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ex) {
      // "Can't happen": unexpected type mismatch.
      // Show details to aid debugging (see e.g. b/162444744).
      StringBuilder buf = new StringBuilder();
      buf.append(
          String.format(
              "IllegalArgumentException (%s) in Starlark call of %s, obj=%s (%s), args=[",
              ex.getMessage(), method, Starlark.repr(this), Starlark.type(this)));
      String sep = "";
      for (Object arg : args) {
        buf.append(String.format("%s%s (%s)", sep, Starlark.repr(arg), Starlark.type(arg)));
        sep = ", ";
      }
      buf.append(']');
      throw new IllegalArgumentException(buf.toString());

    }
    if (method.getReturnType().equals(Void.TYPE)) {
      return Starlark.NONE;
    }

    StarlarkMethod starlarkMethod = StarlarkAnnotations.getStarlarkMethod(method);
    assert starlarkMethod != null;

    if (result == null && !starlarkMethod.allowReturnNones()) {
      throw new IllegalStateException(
          "method invocation returned null: " +
              starlarkMethod.name() +
              Tuple.copyOf(Arrays.asList(args)));
    }
    return Starlark.fromJava(result, mu);
  }
}
