package com.verygood.security.larky.modules.crypto.Util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedBytes;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

public class Strxor implements StarlarkValue {

  @StarlarkMethod(name = "strxor",
      doc = "XOR two byte strings.\n" +
          "  \n"  +
          "  Args:\n"  +
          "    term1 (bytes/bytearray/memoryview):\n"  +
          "       The first term of the XOR operation.\n"  +
          "    term2 (bytes/bytearray/memoryview):\n"  +
          "       The second term of the XOR operation.\n"  +
          "    output (bytearray/memoryview):\n"  +
          "       The location where the result must be written to.\n"  +
          "       If ``None``, the result is returned.\n"  +
          "  :Return:\n"  +
          "       If ``output`` is ``None``, a new ``bytes`` string with the result.\n"  +
          "       Otherwise ``None``.",
      parameters = {
          @Param(name = "term1"),
          @Param(name = "term2")
  }, useStarlarkThread = true)
  public StarlarkBytes strxor(StarlarkBytes term1, StarlarkBytes term2, StarlarkThread thread) throws EvalException {
    if(term1.size() != term2.size()) {
      throw Starlark.errorf("term1.size() and term2.size() should be equal");
    }
    final int[] in1 = term1.getUnsignedBytes();
    final int[] in2 = term2.getUnsignedBytes();
    int[] out = new int[in1.length];
    _strxor(in1, in2, out, in1.length);
    return StarlarkBytes.of(thread.mutability(), out);
  }

  @StarlarkMethod(name = "strxor_c",
      doc = "XOR a byte string with a repeated sequence of characters.\n" +
          "\n" +
          "Args:\n" +
          "    term(bytes/bytearray/memoryview):\n" +
          "        The first term of the XOR operation.\n" +
          "    c (bytes):\n" +
          "        The byte that makes up the second term of the XOR operation.\n" +
          "    output (None or bytearray/memoryview):\n" +
          "        If not ``None``, the location where the result is stored into.\n" +
          "\n" +
          "Return:\n" +
          "    If ``output`` is ``None``, a new ``bytes`` string with the result.\n" +
          "    Otherwise ``None``.\n" +
          "",
      parameters = {
          @Param(name = "term"),
          @Param(name = "c"),
  },useStarlarkThread = true)
  public StarlarkBytes strxor_c(StarlarkBytes term, StarlarkInt c, StarlarkThread thread) {
    byte c_ = UnsignedBytes.checkedCast(c.toIntUnchecked());
    int[] in = term.getUnsignedBytes();
    int[] out = new int[in.length];
    _strxor_c(in, Byte.toUnsignedInt(c_), out, in.length);
    return StarlarkBytes.of(thread.mutability(), out);
  }

  @VisibleForTesting
  public static void _strxor(final int[] in1, final int[] in2, int[] out, int len) {
    /*
       for (; len>0; len--)
         *out++ = *in1++ ^ *in2++;
     */
    for(int i = 0; len > 0; i++, len--) {
      out[i] = (in1[i] ^ in2[i]);
    }
  }

  @VisibleForTesting
  public static void _strxor_c(final int[] in, int c, int[] out, int len) {
    /*
          for (; len>0; len--)
              *out++ = *in++ ^ c;
     */
    for(int i = 0; len > 0; i++, len--) {
      out[i] = in[i] ^ c;
    }
  }
}
