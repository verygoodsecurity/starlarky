package com.verygood.security.larky.nativelib;

import com.verygood.security.larky.annot.Library;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;


/**
 * A collection of global Larky API functions that mimic python's built-ins, to a certain extent.
 *
 * A work-in-progress to add methods as we need them.
 *
 * More here: https://docs.python.org/3/library/functions.html
 *
 * */
@Library
public final class PythonBuiltins {

  @StarlarkMethod(
      name = "pow",
      doc = "Return base to the power exp; if mod is present, return base to " +
          "the power exp, modulo mod (computed more efficiently than pow(base, exp) % mod). " +
          "" +
          "The two-argument form pow(base, exp) is equivalent to using the power operator: base**exp.",
      parameters = {
          @Param(
              name = "base",
              doc = "The function to invoke when the struct is called",
              named = true
          ),
          @Param(
              name = "exp",
              doc = "The function to invoke when the struct is called",
              named = true
          ),
          @Param(
              name = "mod",
              doc = "",
              named = true,
              allowedTypes = {
                  @ParamType(type = String.class),
                  @ParamType(type = NoneType.class),
              },
              defaultValue = "None"
          )
      }
    )
  public StarlarkInt pow(StarlarkInt base, StarlarkInt exp, Object mod) throws EvalException {
    if(Starlark.isNullOrNone(mod)) {
      return StarlarkInt.of(
          base.toBigInteger()
              .pow(exp.toInt("exp " + exp.toString() + " is too big."))
      );
    }
    return StarlarkInt.of(
      base.toBigInteger()
          .modPow(exp.toBigInteger(), ((StarlarkInt) mod).toBigInteger())
    );
  }
//
//  @StarlarkMethod(
//      name = "bytes",
//      doc = "immutable array of bytes",
//      parameters = {
//       @Param(
//         name = "sequence",
//         allowedTypes = {
//             @ParamType(type = String.class),
//         }
//       )
//     }
//  )
//  public StarlarkList<StarlarkInt> bytes(String sequence) {
//    byte[] bytes = sequence.getBytes(StandardCharsets.UTF_8);
//
//    return StarlarkList.immutableOf(Stream.of(bytes.).map((byte[] x) -> Byte.toUnsignedInt(x)).collect();
//  }
//
//  @StarlarkMethod(
//      name = "chr",
//      doc = "Return ascii ord",
//      parameters = {
//       @Param(
//         name = "ordinal",
//         allowedTypes = {
//             @ParamType(type = StarlarkInt.class),
//         }
//       )
//     }
//  )
//  public String chr(StarlarkInt ordinal) {
//    return String.valueOf((char) ordinal.toIntUnchecked());
//  }
}
