package com.verygood.security.larky.modules.globals;

import com.verygood.security.larky.annot.Library;
import com.verygood.security.larky.modules.types.LarkyByteArray;
import com.verygood.security.larky.modules.types.LarkyObject;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;


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

  @StarlarkMethod(
        name = "ord",
        doc = "Given a string representing one Unicode character, return an integer representing" +
            " the Unicode code point of that character. For example, ord('a') returns the " +
            "integer 97 and ord('€') (Euro sign) returns 8364. This is the inverse of chr().",
        parameters = {
         @Param(
           name = "c",
           allowedTypes = {
               @ParamType(type = String.class),
               @ParamType(type = LarkyByteArray.class),
           }
         )
       }
    )
    public StarlarkInt ordinal(Object c) throws EvalException {
      int containerSize = 0;
      byte[] bytes = null;
      if(String.class.isAssignableFrom(c.getClass())) {
        containerSize = ((String) c).length();
        bytes = ((String) c).getBytes(StandardCharsets.UTF_8);
      }
      else if(LarkyByteArray.class.isAssignableFrom(c.getClass())) {
        containerSize = ((LarkyByteArray) c).size();
        bytes = ((LarkyByteArray) c).toBytes();
      }

      if(containerSize != 1 || bytes == null) {
        //"ord() expected a character, but string of length %d found", c.length()
        throw new EvalException(
            String.format("ord: %s has length %d, want 1", type(c), containerSize)
        );
      }
      return StarlarkInt.of(new BigInteger(bytes).intValueExact());
    }

  //override built-in type
  @StarlarkMethod(
      name = "type",
      doc =
          "Returns the type name of its argument. This is useful for debugging and "
              + "type-checking. Examples:"
              + "<pre class=\"language-python\">"
              + "type(2) == \"int\"\n"
              + "type([1]) == \"list\"\n"
              + "type(struct(a = 2)) == \"struct\""
              + "</pre>"
              + "This function might change in the future. To write Python-compatible code and "
              + "be future-proof, use it only to compare return values: "
              + "<pre class=\"language-python\">"
              + "if type(x) == type([]):  # if x is a list"
              + "</pre>" +
              "\n" +
              "Type can overridden on any LarkyObject by implementing a __type__ special method." +
              "Otherwise, the type will default to the default Starlark::type() method invocation",
      parameters = {@Param(name = "x", doc = "The object to check type of.")})
  public String type(Object object) {
    if (LarkyObject.class.isAssignableFrom(object.getClass())) {
      return ((LarkyObject) object).type();
    }
    return Starlark.type(object);
  }

  @StarlarkMethod(
       name = "hash",
       doc =
           "Return a hash value for a string. This is computed deterministically using the same "
               + "algorithm as Java's <code>String.hashCode()</code>, namely: "
               + "<pre class=\"language-python\">s[0] * (31^(n-1)) + s[1] * (31^(n-2)) + ... + "
               + "s[n-1]</pre> Hashing of values besides strings is not currently supported.",
       // Deterministic hashing is important for the consistency of builds, hence why we
       // promise a specific algorithm. This is in contrast to Java (Object.hashCode()) and
       // Python, which promise stable hashing only within a given execution of the program.
       parameters = {
         @Param(
             name = "value",
             doc = "String or byte value to hash.",
             allowedTypes = {
                 @ParamType(type = String.class),
                 @ParamType(type = LarkyByteArray.class),
             }),
       })
   public int hash(Object value) throws EvalException {
    return value.hashCode();
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
