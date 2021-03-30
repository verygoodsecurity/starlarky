package com.verygood.security.larky.modules.crypto.Util;

import com.google.common.primitives.UnsignedBytes;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.modes.gcm.GCMUtil;

import java.util.Arrays;

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
  public LarkyByteLike strxor(LarkyByteLike term1, LarkyByteLike term2, StarlarkThread thread) throws EvalException {
    byte[] xorResult = Arrays.copyOf(term1.getBytes(), term1.getBytes().length);
    GCMUtil.xor(xorResult, term2.getBytes());
    LarkyByteLike build = LarkyByte.builder(thread)
        .setSequence(xorResult)
        .build();
    return build;

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
  public LarkyByteLike strxor_c(LarkyByteLike term, StarlarkInt c, StarlarkThread thread) throws EvalException {
    byte c_ = UnsignedBytes.checkedCast(c.toIntUnchecked());
    byte[] strbytes = Arrays.copyOf(term.getBytes(), term.getBytes().length);
    byte[] xorable = new byte[strbytes.length];
    Arrays.fill(xorable, c_);
    GCMUtil.xor(strbytes, xorable);
    LarkyByteLike build = LarkyByte.builder(thread)
            .setSequence(strbytes)
            .build();
    return build;
  }
}
