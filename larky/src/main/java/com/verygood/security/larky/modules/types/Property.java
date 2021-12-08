package com.verygood.security.larky.modules.types;

import com.verygood.security.larky.parser.StarlarkUtil;

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
public class Property implements StarlarkValue {

  final private StarlarkCallable fget;
  final private StarlarkCallable fset;
  final private StarlarkThread thread;

  @StarlarkMethod(
    name = "get",
    doc = "call"
  )
  public Object get() throws InterruptedException, EvalException {
    return Starlark.call(this.thread, this.fget, Tuple.empty(), Dict.empty());
  }

  @StarlarkMethod(
    name = "set",
    doc = "call"
  )
  public Object set(Object val, String fieldName) throws InterruptedException, EvalException {
    if(this.fset == null) {
      throw new EvalException(String.format(
          "Property (%1$s) does not define a setter!", fieldName));
    }
    return Starlark.call(this.thread, this.fset, Tuple.of(val), Dict.empty());
  }

  public Object call() throws NoSuchMethodException, EvalException {
    return call(null, null);
  }
  /**
   * Invokes this method using {@code obj} as a target and {@code args} as Java arguments.
   *
   * <p>Methods with {@code void} return type return {@code None} following Python convention.
   *
   * <p>The Mutability is used if it is necessary to allocate a Starlark copy of a Java result.
   */
  public Object call(Object[] args, @Nullable Mutability mu) throws NoSuchMethodException, EvalException {
    Object result = null;
    Method method = null;

    try {
      if(args != null && args.length == 2) {
        method = this.getClass().getMethod("set", Object.class, String.class);
        result = method.invoke(this, args[0], String.valueOf(args[1]));
      }
      else {
        method = this.getClass().getMethod("get");
        result = method.invoke(this);
      }
    } catch (IllegalArgumentException | IllegalAccessException ex) {
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

    } catch (InvocationTargetException e) {
      throw new EvalException(e.getCause());
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
    return StarlarkUtil.valueToStarlark(result);
  }

  @Override
  public String toString() {
    return fget.toString();
  }
}
