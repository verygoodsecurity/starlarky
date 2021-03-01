package com.verygood.security.larky.modules.types.structs;

import com.google.common.collect.ImmutableMap;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;

// SimpleStructWithMethods augments SimpleStruct's fields with annotated Java methods.
final class SimpleStructWithMethods extends SimpleStruct {

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
