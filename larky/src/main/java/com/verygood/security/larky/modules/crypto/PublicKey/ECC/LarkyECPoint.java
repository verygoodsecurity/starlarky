package com.verygood.security.larky.modules.crypto.PublicKey.ECC;

import com.verygood.security.larky.modules.types.NativeMutability;
import java.math.BigInteger;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

/**
 * An immutable elliptic-curve point. Every operation returns a new point; the receiver is never
 * changed, so a point can be shared between evaluations.
 */
public class LarkyECPoint implements Mutability.Freezable, StarlarkValue {
  // The evaluation that created this point. Nothing changes a point, so this only tells the module
  // cache's admission walk that a point in a frozen module is frozen.
  private final Mutability mutability = NativeMutability.current();
  private final ECPoint point;

  @Override
  public Mutability mutability() {
    return mutability;
  }

  private LarkyECPoint(ECPoint point) {
    this.point = point;
  }

  public static LarkyECPoint create(ECCurve curve, BigInteger x, BigInteger y) throws EvalException {
    ECPoint point = curve.createPoint(x, y);
    if (!point.isValid()) {
      throw Starlark.errorf("ValueError: The EC point does not belong to the curve");
    }
    return new LarkyECPoint(point);
  }

  public static LarkyECPoint infinity(ECCurve curve) {
    return new LarkyECPoint(curve.getInfinity());
  }

  @StarlarkMethod(name = "negate")
  public LarkyECPoint negate() {
    return new LarkyECPoint(this.point.normalize().negate());
  }

  @StarlarkMethod(name = "is_infinity")
  public boolean isInfinity() {
    return this.point.isInfinity();
  }

  @StarlarkMethod(name = "as_tuple")
  public Tuple asTuple() {
    /*
     The way PyCryptodome represents infinity is (0, 0), so we must
     match this behavior.

     Really, this should probably be in the ECCPoint class itself instead
     of here.
    */
    if (this.point.isInfinity()) {
      return Tuple.of(StarlarkInt.of(0), StarlarkInt.of(0));
    }
    return Tuple.of(
      StarlarkInt.of(this.point.getXCoord().toBigInteger()),
      StarlarkInt.of(this.point.getYCoord().toBigInteger())
    );
  }

  @StarlarkMethod(name = "twice")
  public LarkyECPoint twice() {
    return new LarkyECPoint(this.point.twice().normalize());
  }

  @StarlarkMethod(name = "add", parameters = {@Param(name = "point", allowedTypes = {@ParamType(type = LarkyECPoint.class)})})
  public LarkyECPoint add(LarkyECPoint other) throws EvalException {
    if (!(this.point.getCurve().equals(other.point.getCurve()))) {
      throw Starlark.errorf("ValueError: EC points are not on the same curve");
    }
    return new LarkyECPoint(this.point.add(other.point).normalize());
  }

  @StarlarkMethod(name = "multiply", parameters = {@Param(name = "point", allowedTypes = {@ParamType(type = StarlarkInt.class)})})
  public LarkyECPoint multiply(StarlarkInt scale) {
    return new LarkyECPoint(this.point.multiply(scale.toBigInteger()).normalize());
  }

  @Override
  public boolean equals(final Object that) {
    return that instanceof LarkyECPoint
             && this.point.equals(((LarkyECPoint) that).point);
  }

  @Override
  public int hashCode() {
    return this.point.normalize().hashCode();
  }
}
