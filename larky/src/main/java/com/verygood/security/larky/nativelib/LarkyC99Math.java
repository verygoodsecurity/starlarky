package com.verygood.security.larky.nativelib;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkValue;


@StarlarkBuiltin(
    name = "c99math",
    category = "BUILTIN",
    doc = "This module provides access to the mathematical functions defined by the C99 standard")
public class LarkyC99Math implements StarlarkValue {

  @StarlarkMethod(name="PI", doc ="a constant pi", structField = true)
  public StarlarkFloat PI_CONSTANT() {
    return StarlarkFloat.of(Math.PI);
  }


}
