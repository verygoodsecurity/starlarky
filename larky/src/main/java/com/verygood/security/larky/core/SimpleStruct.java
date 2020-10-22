package com.verygood.security.larky.core;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.ClassObject;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;

// A trivial struct-like class with Starlark fields defined by a map.
public class SimpleStruct implements ClassObject {
    final ImmutableMap<String, Object> fields;

    public SimpleStruct(ImmutableMap<String, Object> fields) {
      this.fields = fields;
    }

    @Override
    public ImmutableCollection<String> getFieldNames() {
      return fields.keySet();
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
      p.append("simplestruct(");
      String sep = "";
      for (var e : fields.entrySet()) {
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
            return "fromValues";
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