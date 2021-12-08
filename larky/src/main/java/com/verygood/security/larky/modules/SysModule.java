package com.verygood.security.larky.modules;

import java.nio.ByteOrder;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

/**
 * A very simple module that just exposes basic information <b><i>JUST for portability
 * reasons.</i></b>
 *
 * This module will just expose things like <code>maxint</code> or <code>byteorder</code>,
 * just to make it easier to port certain things.
 *
 * There is *NO* intention to making this 100% compatible with Python's sys module.
 */
@StarlarkBuiltin(
    name = "sys",
    category = "BUILTIN",
    doc = "*Larky* system-specific parameters and functions")
public class SysModule implements StarlarkValue {

  public static final SysModule INSTANCE = new SysModule();
  private static final String LITTLE = "little";
  private static final String BIG = "big";

  private static final String NATIVE;
  static {
    NATIVE = (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) ? LITTLE : BIG;
  }

  /**
   * An indicator of the native byte order. This will have the value 'big' on
   * big-endian (most-significant byte first) platforms, and 'little' on little-endian
   * (least-significant byte first) platforms.
   *
   * @return 'big on big-endian (MSB) platforms or 'little' on little-endian (LSB) platforms
   */
  @StarlarkMethod(
    name = "byteorder",
    doc = "An indicator of the native byte order. This will have the value 'big' on " +
            "big-endian (most-significant byte first) platforms, and 'little' on " +
            "little-endian (least-significant byte first) platforms." +
            "\n",
    structField = true)
  public static String byteOrder() {
    return NATIVE;
  }
}
