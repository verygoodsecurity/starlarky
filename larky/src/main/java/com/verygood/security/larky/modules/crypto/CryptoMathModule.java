package com.verygood.security.larky.modules.crypto;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;

public class CryptoMathModule implements StarlarkValue {

  public static final CryptoMathModule INSTANCE = new CryptoMathModule();

  @StarlarkMethod(name = "bit_length",
      doc = "If x is nonzero, then x.bit_length() is the unique positive " +
          "integer k such that 2**(k-1) <= abs(x) < 2**k. " +
          "" +
          "Equivalently, when abs(x) is small enough to have a correctly " +
          "rounded logarithm, then k = 1 + int(log(abs(x), 2)). " +
          "" +
          "If x is zero, then x.bit_length() returns 0.",
      parameters = {
       @Param(name = "n")
   })
   public StarlarkInt bitLength(StarlarkInt n) {
    return StarlarkInt.of(n.toBigInteger().bitLength());
   }

}
