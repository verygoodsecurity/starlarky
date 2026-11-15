package com.verygood.security.larky.modules.types;

import com.verygood.security.larky.objects.descriptor.LarkyDataDescriptor;
import com.verygood.security.larky.parser.CurrentEvaluation;
import com.verygood.security.larky.objects.type.LarkyType;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import javax.annotation.Nullable;
import lombok.Builder;

@Builder
public class Property implements StarlarkValue, LarkyDataDescriptor {

  final private StarlarkCallable fget;
  final private StarlarkCallable fset;

  @Override
  public Object __get__(Object obj, LarkyType type, @Nullable StarlarkThread thread)
      throws InterruptedException, EvalException {
    if (obj == null || obj == Starlark.NONE) {
      return this;
    }
    if (this.fget == null) {
      throw new EvalException("AttributeError: unreadable attribute");
    }
    return Starlark.call(CurrentEvaluation.orRequire(thread), this.fget, Tuple.of(obj), Dict.empty());
  }

  @Override
  public void __set__(Object obj, Object value, @Nullable StarlarkThread thread)
      throws InterruptedException, EvalException {
    if (this.fset == null) {
      throw new EvalException("AttributeError: can't set attribute");
    }
    Starlark.call(CurrentEvaluation.orRequire(thread), this.fset, Tuple.of(obj, value), Dict.empty());
  }

  @Override
  public void __delete__(Object obj, @Nullable StarlarkThread thread) throws EvalException {
    throw new EvalException("AttributeError: can't delete attribute");
  }

  @Override
  public boolean readonly() {
    // Dispatch to __set__ even without fset so it supplies the property-specific error.
    return false;
  }

  @Override
  public boolean optional() {
    return false;
  }

  /**
   * Calls the getter with no arguments on {@code thread}, the caller's thread. A property does
   * not keep the thread that created it: it is shared by everything that holds it (a struct in a
   * cached module, ...), and an evaluation's thread is finished when that evaluation is.
   *
   * @param thread the calling thread; the {@link CurrentEvaluation} when null
   */
  @StarlarkMethod(
    name = "get",
    doc = "call",
    useStarlarkThread = true
  )
  public Object get(StarlarkThread thread) throws InterruptedException, EvalException {
    if (this.fget == null) {
      // a Java host can build a property without a getter; as __get__ does
      throw new EvalException("AttributeError: unreadable attribute");
    }
    return Starlark.call(
        CurrentEvaluation.orRequire(thread), this.fget, Tuple.empty(), Dict.empty());
  }

  /** As {@link #get}: calls the setter with {@code val} on the caller's thread. */
  // The parameters are declared so that Starlark checks the call. Without them every call failed
  // with a Java reflection error that showed this method's Java signature to the script.
  @StarlarkMethod(
    name = "set",
    doc = "Calls the setter with val. field_name names the field in the error if there is none.",
    parameters = {
      @Param(name = "val"),
      @Param(name = "field_name", defaultValue = "''", named = true)
    },
    useStarlarkThread = true
  )
  public Object set(Object val, String fieldName, StarlarkThread thread)
      throws InterruptedException, EvalException {
    if(this.fset == null) {
      throw new EvalException(String.format(
          "Property (%1$s) does not define a setter!", fieldName));
    }
    return Starlark.call(
        CurrentEvaluation.orRequire(thread), this.fset, Tuple.of(val), Dict.empty());
  }

  @Override
  public String toString() {
    return String.valueOf(fget);
  }
}
