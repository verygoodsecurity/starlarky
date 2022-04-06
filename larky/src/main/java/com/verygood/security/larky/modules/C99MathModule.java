package com.verygood.security.larky.modules;

import com.google.common.math.DoubleMath;
import java.math.BigDecimal;
import java.math.BigInteger;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;


@StarlarkBuiltin(
  name = "c99math",
  category = "BUILTIN",
  doc = "This module provides access to the mathematical functions defined by the C99 standard")
public class C99MathModule implements StarlarkValue {

  public static final C99MathModule INSTANCE = new C99MathModule();

  @StarlarkMethod(name = "PI", doc = "a constant pi", structField = true)
  public StarlarkFloat PI_CONSTANT() {
    return StarlarkFloat.of(Math.PI);
  }

  @StarlarkMethod(name = "E", doc = "a constant e", structField = true)
  public StarlarkFloat E_CONSTANT() {
    return StarlarkFloat.of(Math.E);
  }

  @StarlarkMethod(
    name = "sqrt",
    doc = "Returns the correctly rounded positive square root of a double value. " +
            "" +
            "Special cases:\n" +
            "- If the argument is NaN or less than zero, then the result is NaN.\n" +
            "- If the argument is positive infinity, then the result is positive infinity.\n" +
            "- If the argument is positive zero or negative zero, then the result is the same as the argument.\n" +
            "" +
            "Otherwise, the result is the double value closest to the true mathematical square root of the argument value.",
    parameters = {
      @Param(
        name = "a",
        doc = "a value"
      )
    }
  )
  public StarlarkFloat sqrt(Object a) {
    double x = Double.parseDouble(String.valueOf(a));
    return StarlarkFloat.of(Math.sqrt(x));
  }

  @StarlarkMethod(
    name = "pow",
    doc = "Return x raised to the power y. Exceptional cases follow Annex ‘F’ of the C99 standard" +
            " as far as possible. " +
            "" +
            "In particular, pow(1.0, x) and pow(x, 0.0) always return 1.0, even when x is a zero or a NaN. " +
            "" +
            "If both x and y are finite, x is negative, and y is not an integer then pow(x, y) " +
            "is undefined, and throws an EvalException." +
            "" +
            "Unlike the built-in pow() function, math.pow() converts both its arguments to type float." +
            "Use the built-in pow() function for computing exact integer powers.",
    parameters = {
      @Param(
        name = "x",
        doc = "The base to raise to the power of y"
      ),
      @Param(
        name = "y",
        doc = "The power to raise x to"
      )
    }
  )
  public StarlarkFloat pow(Object x, Object y) throws EvalException {
    double base = Double.parseDouble(String.valueOf(x));
    double exp = Double.parseDouble(String.valueOf(y));
    if (
      /* base and exp are finite */
      !Double.isInfinite(base) && !Double.isInfinite(exp)
        // base is negative
        && base < 0
        // and exp is not an integer
        && !DoubleMath.isMathematicalInteger(exp)) {
      throw new EvalException(String.format("math domain error: " +
                                              "If both x (%1s) and y (%2s) are finite, x is negative, and y is not an integer then math.pow(x, y)" +
                                              " is undefined",
        base, exp));
    }
    return StarlarkFloat.of(Math.pow(base, exp));
  }

  @StarlarkMethod(
    name = "fabs",
    doc = "Return the absolute value of x",
    parameters = {
      @Param(
        name = "x",
        doc = "Return the absolute value of x."
      )
    }
  )
  public StarlarkFloat fabs(Object x) {
    double base = Double.parseDouble(String.valueOf(x));
    return StarlarkFloat.of(Math.abs(base));
  }

  @StarlarkMethod(
    name = "ceil",
    doc = "Return the ceiling of x, the smallest integer greater than or equal to x. " +
            "If x is not a float, delegates to x.__ceil__(), which should return an Integral value.",
    parameters = {
      @Param(
        name = "x"
      )
    }
  )
  public StarlarkInt ceil(Object x) {
    double base = Double.parseDouble(String.valueOf(x));
    return StarlarkInt.of((int) Math.ceil(base));
  }

  @StarlarkMethod(
    name = "log",
    doc = "" +
            "With one argument, return the natural logarithm of x (to base e)." +
            "With two arguments, return the logarithm of x to the given base, " +
            "calculated as log(x)/log(base).",
    parameters = {
      @Param(
        name = "x",
        allowedTypes = {
          @ParamType(type=NoneType.class),
          @ParamType(type=StarlarkInt.class),
          @ParamType(type=StarlarkFloat.class)
        }
      ),
      @Param(
        name = "base",
        named = true,
        allowedTypes = {
          @ParamType(type=NoneType.class),
          @ParamType(type=StarlarkInt.class),
          @ParamType(type=StarlarkFloat.class)
        },
        defaultValue = "None"
      )
    }
  )
  public Object log(Object x, StarlarkValue base) throws EvalException {

    // passed down a base?
    final double log;

    if (x instanceof StarlarkFloat) {
      log = MathUtil.getBaseCount(((StarlarkFloat) x).toDouble());
    } else {
      log = MathUtil.getBaseCount(((StarlarkInt)x).toBigInteger());
    }

    if (base == Starlark.NONE) {
      return StarlarkFloat.of(log);
    }

    if(x instanceof StarlarkFloat && base instanceof StarlarkFloat) {
      return StarlarkFloat.of(
        MathUtil.logDD(
          ((StarlarkFloat)x).toDouble(),
          ((StarlarkFloat)base).toDouble())
      );
    }
    else if(x instanceof StarlarkFloat && base instanceof StarlarkInt) {
      return StarlarkFloat.of(
        MathUtil.logDD(
          ((StarlarkFloat)x).toDouble(),
          ((StarlarkInt)base).toBigInteger()
      ));
    }
    else if(x instanceof StarlarkInt && base instanceof StarlarkFloat) {
      return StarlarkFloat.of(
        MathUtil.logDD(
          ((StarlarkInt)x).toBigInteger(),
          ((StarlarkFloat)base).toDouble()
      ));
    }
    else if(x instanceof StarlarkInt && base instanceof StarlarkInt) {
      return StarlarkFloat.of(
              MathUtil.logDD(
                ((StarlarkInt)x).toBigInteger(),
                ((StarlarkInt)base).toBigInteger()
            ));
    }
    throw Starlark.errorf("Unable to get log(%s, %s)!", log, base);
  }

  @StarlarkMethod(
    name = "floor",
    doc = "Return the floor of x, the largest integer less than or equal to x. " +
            "If x is not a float, delegates to x.__floor__(), which should " +
            "return an Integral value.",
    parameters = {
      @Param(
        name = "x"
      )
    }
  )
  public StarlarkInt floor(Object x) {
    double base = Double.parseDouble(String.valueOf(x));
    return StarlarkInt.of((int) Math.floor(base));
  }

  static class MathUtil {
    // numbers greater than 10^MAX_DIGITS_10 or e^MAX_DIGITS_EXP are considered unsafe ('too big') for floating point operations
    protected static final int MAX_DIGITS_EXP = 677;
    protected static final int MAX_DIGITS_10 = 294; // ~ MAX_DIGITS_EXP/LN(10)
    protected static final int MAX_DIGITS_2 = 977; // ~ MAX_DIGITS_EXP/LN(2)
    private static final double LOG2 = Math.log(2.0);
    private static final double LOG10 = Math.log(10.0);

    // Advanced log2 of biginteger https://stackoverflow.com/questions/6827516/logarithm-for-biginteger
    public static double logBigInteger(BigInteger val) {

      double result;
      if (val.signum() < 1) {
        result = val.signum() < 0 ? Double.NaN : Double.NEGATIVE_INFINITY;
      } else {
        int blex = val.bitLength() - MAX_DIGITS_2; // any value in 60..1023 works ok here
        BigInteger value = val;
        if (blex > 0) {
          value = value.shiftRight(blex);
        }
        double res = Math.log(value.doubleValue());
        result = blex > 0 ? res + blex * LOG2 : res;
      }

      return result;
    }

    public static double logBigDecimal(BigDecimal val) {
      double result;
      if (val.signum() < 1) {
        result = val.signum() < 0 ? Double.NaN : Double.NEGATIVE_INFINITY;
      } else {
        int digits = val.precision() - val.scale();
        if (digits < MAX_DIGITS_10 && digits > -MAX_DIGITS_10) {
          result = Math.log(val.doubleValue());
        } else {
          result = logBigInteger(val.unscaledValue()) - val.scale() * LOG10;
        }
      }
      return result;
    }

    private static double getBaseCount(double base) {
      return Math.log(base);
    }

    private static double getBaseCount(BigInteger base) {
      return logBigInteger(base);
    }

    public static double logDD(BigInteger value, long base) {
      return logBigInteger(value) / getBaseCount(base);
    }

    public static double logDD(BigInteger value, double base) {
      return logBigInteger(value) / getBaseCount(base);
    }

    public static double logDD(BigInteger value, BigInteger base) {
      return logBigInteger(value) / getBaseCount(base);
    }

    public static double logLD(long value, double base) {
      return logDD(value, base);
    }

    public static double logDL(double value, long base) {
      return logDD(value, base);
    }

    public static double logLL(long value, long base) {
      return Math.log(value) / getBaseCount(base);
    }

    public static double logDD(double value, BigInteger base) {
      return Math.log(value) / getBaseCount(base);
    }

    public static double logDD(long value, BigInteger base) {
      return Math.log(value) / getBaseCount(base);
    }

    public static double logDD(double value, double base) {
      return Math.log(value) / getBaseCount(base);
    }

    protected static double logBigInteger2(BigInteger val) {
      int blex = val.bitLength() - 1022; // any value in 60..1023 is ok
      BigInteger value = blex > 0 ? val.shiftRight(blex) : val;
      double res = Math.log(value.doubleValue());
      return blex > 0 ? res + blex * LOG2 : res;
    }
  }
}
