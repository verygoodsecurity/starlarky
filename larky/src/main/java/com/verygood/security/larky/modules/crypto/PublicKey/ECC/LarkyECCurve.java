package com.verygood.security.larky.modules.crypto.PublicKey.ECC;

import java.math.BigInteger;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.math.ec.ECCurve;

public class LarkyECCurve implements StarlarkValue {
  final private ECCurve curve;

  public LarkyECCurve(ECCurve curve) {
    this.curve = curve;
  }

  @StarlarkMethod(name = "point", parameters = {
    @Param(name = "xb", allowedTypes = {@ParamType(type = StarlarkInt.class)}),
    @Param(name = "yb", allowedTypes = {@ParamType(type = StarlarkInt.class)}),
  })
  public LarkyECPoint point(StarlarkInt xb, StarlarkInt yb) throws EvalException {
    BigInteger x = xb.toBigInteger();
    BigInteger y = yb.toBigInteger();
    return LarkyECPoint.create(curve, x, y);

  }

  @StarlarkMethod(name = "infinity")
  public LarkyECPoint pointAtInfinity() {
    return LarkyECPoint.infinity(curve);
  }
}