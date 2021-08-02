package com.verygood.security.larky.modules;

import java.nio.ByteOrder;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;


@StarlarkBuiltin(
    name = "sys",
    category = "BUILTIN",
    doc = "Larky system-specific parameters and functions")
public class SysModule implements StarlarkValue {

  public static final SysModule INSTANCE = new SysModule();
  private static final String LITTLE = "little";
  private static final String BIG = "big";

  private static final String NATIVE;
  static {
    NATIVE = (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) ? LITTLE : BIG;
  }

  @StarlarkMethod(name = "byteorder", doc = "the system byte order", structField = true)
  public static String byteOrder() {
    return NATIVE;
  }
}
