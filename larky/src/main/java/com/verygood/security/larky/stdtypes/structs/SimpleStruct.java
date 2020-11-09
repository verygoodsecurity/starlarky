package com.verygood.security.larky.stdtypes.structs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.ClassObject;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;

import java.util.Map;

// A trivial struct-like class with Starlark fields defined by a map.
public class SimpleStruct implements ClassObject {

  final Map<String, Object> fields;

  @SuppressWarnings("CdiInjectionPointsInspection")
  SimpleStruct(Map<String, Object> fields) {
    this.fields = fields;
  }

  public static SimpleStruct create(Map<String, Object> kwargs) {
    return new SimpleStruct(kwargs);
  }

  public static SimpleStruct immutable(Dict<String, Object> kwargs, StarlarkSemantics semantics) {
    return new ImmutableStruct(ImmutableMap.copyOf(kwargs));
  }

  public static SimpleStruct mutable(Dict<String, Object> kwargs, StarlarkSemantics semantics) {
    return new MutableStruct(kwargs);
  }

  @Override
  public ImmutableList<String> getFieldNames() {
    return ImmutableList.copyOf(fields.keySet());
  }

  @Override
  public Object getValue(String name) {
    return fields.get(name);
  }

  @Override
  public String getErrorMessageForUnknownField(String name) {
    return null;
  }

  @Override
  public void repr(Printer p) {
    // This repr function prints only the fields.
    // Any methods are still accessible through dir/getattr/hasattr.
    p.append(Starlark.type(this));
    p.append("(");
    String sep = "";
    for (Map.Entry<String, Object> e : fields.entrySet()) {
      p.append(sep).append(e.getKey()).append(" = ").repr(e.getValue());
      sep = ", ";
    }
    p.append(")");
  }

  // SimpleStructWithMethods augments SimpleStruct's fields with annotated Java methods.
  private static final class SimpleStructWithMethods extends SimpleStruct {

    // A function that returns "fromValues".
    private static final Object returnFromValues =
        new StarlarkCallable() {
          @Override
          public String getName() {
            return "returnFromValues";
          }

          @Override
          public Object fastcall(StarlarkThread thread, Object[] positional, Object[] named) {
            return "bar";
          }
        };

    SimpleStructWithMethods() {
      super(
          ImmutableMap.of(
              "values_only_field",
              "fromValues",
              "values_only_method",
              returnFromValues,
              "collision_field",
              "fromValues",
              "collision_method",
              returnFromValues));
    }

    @StarlarkMethod(name = "callable_only_field", documented = false, structField = true)
    public String getCallableOnlyField() {
      return "fromStarlarkMethod";
    }

    @StarlarkMethod(name = "callable_only_method", documented = false, structField = false)
    public String getCallableOnlyMethod() {
      return "fromStarlarkMethod";
    }

    @StarlarkMethod(name = "collision_field", documented = false, structField = true)
    public String getCollisionField() {
      return "fromStarlarkMethod";
    }

    @StarlarkMethod(name = "collision_method", documented = false, structField = false)
    public String getCollisionMethod() {
      return "fromStarlarkMethod";
    }
  }

}