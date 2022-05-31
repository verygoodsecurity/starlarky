package com.verygood.security.larky.modules.crypto.PublicKey;

import com.verygood.security.larky.modules.crypto.PublicKey.ECC.LarkyECCurve;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import org.bouncycastle.math.ec.custom.sec.SecP256R1Curve;
import org.bouncycastle.math.ec.custom.sec.SecP384R1Curve;
import org.bouncycastle.math.ec.custom.sec.SecP521R1Curve;

public class LarkyEllipticCurveCryptography implements StarlarkValue {
  public static LarkyEllipticCurveCryptography INSTANCE = new LarkyEllipticCurveCryptography();

  @StarlarkMethod(name = "P256R1Curve")
  public LarkyECCurve P256R1Curve() {
    return new LarkyECCurve(new SecP256R1Curve());
  }

  @StarlarkMethod(name = "P256K1Curve")
  public LarkyECCurve P256K1Curve() {
    return new LarkyECCurve(new SecP256K1Curve());
  }

  @StarlarkMethod(name = "P384R1Curve")
  public LarkyECCurve P384R1Curve() {
    return new LarkyECCurve(new SecP384R1Curve());
  }

  @StarlarkMethod(name = "P521R1Curve")
  public LarkyECCurve P521R1Curve() {
    return new LarkyECCurve(new SecP521R1Curve());
  }

}
