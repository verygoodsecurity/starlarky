package com.verygood.security.larky.modules;

import com.google.common.math.DoubleMath;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;

import java.math.BigDecimal;
import java.math.RoundingMode;


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
      doc = "Return the ceiling of x, the smallest integer greater than or equal to x. " +
          "If x is not a float, delegates to x.__ceil__(), which should return an Integral value.",
      parameters = {
          @Param(
              name = "x"
          ),
          @Param(
              name = "base",
              named = true,
              defaultValue = "None"
          )
      }
  )
  public Object log(Object x, StarlarkValue base) throws EvalException {
    BigDecimal decimal;
    if(x instanceof StarlarkFloat) {
      decimal = BigDecimal.valueOf(Math.log(
          ((StarlarkFloat) x).toDouble()
      ));
    } else {
      int nx = Starlark.toInt(x, "to int in Math.log()");
      decimal = BigDecimal.valueOf(Math.log(nx));
    }
    if (base == Starlark.NONE) {
      return StarlarkFloat.of(decimal.doubleValue());
    }
    BigDecimal b = BigDecimal.valueOf(Double.parseDouble(String.valueOf(base)));
    return StarlarkFloat.of(decimal.divide(b, RoundingMode.HALF_EVEN).doubleValue());
  }

}
