package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.iso8583.ISO8583ParseModule;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

@StarlarkBuiltin(
    name = "jiso8583",
    category = "BUILTIN",
    doc = ""
)
public class ISO8583Module implements StarlarkValue {

  public static final ISO8583Module INSTANCE = new ISO8583Module();

  @StarlarkMethod(name = "Parser", structField = true)
  public ISO8583ParseModule Parser() {
    return ISO8583ParseModule.INSTANCE;
  }

}
