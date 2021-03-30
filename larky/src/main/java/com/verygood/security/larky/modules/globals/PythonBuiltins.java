package com.verygood.security.larky.modules.globals;

import com.verygood.security.larky.annot.Library;
import com.verygood.security.larky.modules.codecs.TextUtil;
import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteArray;
import com.verygood.security.larky.modules.types.LarkyByteLike;
import com.verygood.security.larky.modules.types.LarkyObject;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkIterable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;


/**
 * A collection of global Larky API functions that mimic python's built-ins, to a certain extent.
 *
 * A work-in-progress to add methods as we need them.
 *
 * More here: https://docs.python.org/3/library/functions.html
 */
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
    if (Starlark.isNullOrNone(mod)) {
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
                  @ParamType(type = LarkyByte.class),
              }
          )
      },
      useStarlarkThread = true
  )
  public StarlarkInt ordinal(Object c, StarlarkThread thread) throws EvalException {
    int containerSize = 0;
    byte[] bytes = null;
    if (String.class.isAssignableFrom(c.getClass())) {
      containerSize = ((String) c).length();
      bytes = ((String) c).getBytes(StandardCharsets.UTF_8);
    } else if (LarkyByte.class.isAssignableFrom(c.getClass())) {
      containerSize = ((LarkyByte) c).size();
      bytes = ((LarkyByte) c).getBytes();
    }

    if (containerSize != 1 || bytes == null) {
      //"ord() expected a character, but string of length %d found", c.length()
      throw new EvalException(
          String.format("ord: %s has length %d, want 1", Starlark.type(c), containerSize)
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
      parameters = {
          @Param(name = "x", doc = "The object to check type of."),
          @Param(name = "bases", defaultValue = "None"),
          @Param(name = "dict", defaultValue = "None")
      },
      extraKeywords = @Param(name = "kwargs", defaultValue = "{}"),
      useStarlarkThread = true
  )
  public Object type(Object object, Object bases, Object dict, Dict<String, Object> kwargs, StarlarkThread thread) throws EvalException {
    if(Starlark.isNullOrNone(bases) && Starlark.isNullOrNone(dict) && kwargs.size() == 0) {
      // There is no 'type' type in Starlark, so we return a string with the type name.
      if (LarkyObject.class.isAssignableFrom(object.getClass())) {
        return ((LarkyObject) object).type();
      }
      return Starlark.type(object);
    }
    else if (kwargs.size() != 0) {
      throw Starlark.errorf("type() takes 1 or 3 arguments");
    }
    return Starlark.type(object); // TODO: fix.
      /*
           Collection<String> fieldNames =
          fields instanceof Sequence
              ? Sequence.cast(fields, String.class, "fields")
              : fields instanceof Dict
              ? Dict.cast(fields, String.class, String.class, "fields").keySet()
              : null;

      if(!Strings.isNullOrEmpty(name)) {
         return LarkyType.createExportedSchemaful(
             new LarkyType.Key("BUILTIN", name),
             fieldNames,
             thread.getCallerLocation()
         );
      }
      return LarkyType.createUnexportedSchemaful(fieldNames, thread.getCallerLocation());
       */
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
                  @ParamType(type = LarkyByte.class),
              }),
      })
  public int hash(Object value) throws EvalException {
    return value.hashCode();
  }

  @StarlarkMethod(
      name = "abs",
      doc = "Return the absolute value of a number. The argument may be an " +
          "integer, a floating point number, or an object " +
          "implementing __abs__(). If the argument is a complex number, " +
          "its magnitude is returned.",
      parameters = {
          @Param(
              name = "x",
              doc = "Return the absolute value of x."
          )
      }
  )
  public StarlarkValue abs(Object x) throws EvalException {
    String classType = Starlark.classType(x.getClass());
    try {
      switch (classType) {
        case "int":
          return StarlarkInt.of(((StarlarkInt) x).toBigInteger().abs());
        // fall through
        case "float":
          // fallthrough
          return StarlarkFloat.of(Math.abs(((StarlarkFloat) x).toDouble()));
        default:
          throw Starlark.errorf("bad operand type for abs(): '%s'", classType);
      }
    } catch (EvalException | ClassCastException ex) {
      throw Starlark.errorf("%s", ex.getMessage());
    }
  }

  @StarlarkMethod(
      name = "divmod",
      doc = "Take two (non complex) numbers as arguments and return a pair of numbers " +
          "consisting of their quotient and remainder when using integer division. " +
          "With mixed operand types, the rules for binary arithmetic operators apply. " +
          "For integers, the result is the same as (a // b, a % b). " +
          "For floating point numbers the result is (q, a % b), where q is usually " +
          "math.floor(a / b) but may be 1 less than that. " +
          "In any case q * b + a % b is very close to a, if a % b is non-zero " +
          "it has the same sign as b, and 0 <= abs(a % b) < abs(b).",
      parameters = {
          @Param(name = "a"),
          @Param(name = "b"),
      }
  )
  public Tuple divmod(StarlarkInt a, StarlarkInt b) throws EvalException {
    BigInteger bigA = a.toBigInteger();
    BigInteger bigB = b.toBigInteger();
    BigInteger[] dm = bigA.divideAndRemainder(bigB);
    return Tuple.of(StarlarkInt.of(dm[0]), StarlarkInt.of(dm[1]));
  }

  @StarlarkMethod(
      name = "bytes",
      doc = "Construct an immutable array of bytes from:\n" +
          "  - an iterable yielding integers in range(256)\n" +
          "  - a text string encoded using the specified encoding\n" +
          "  - any object implementing the buffer API.\n" +
          "  - an integer" +
          "\n" +
          "bytes() -> empty bytes object" +
          "\n" +
          "bytes(int) -> bytes object of size given by the parameter initialized with null bytes" +
          "\n" +
          "bytes(bytes_or_buffer) -> immutable copy of bytes_or_buffer" +
          "\n" +
          "bytes(iterable_of_ints) -> bytes" +
          "\n" +
          "bytes(string, encoding[, errors]) -> bytes",
      parameters = {
          @Param(name = "obj"),
          @Param(name = "encoding",
              named = true,
              allowedTypes = {
                  @ParamType(type = NoneType.class),
                  @ParamType(type = String.class),
          }, defaultValue = "None"),
          @Param(name = "errors",
              named = true,
              allowedTypes = {
                  @ParamType(type = NoneType.class),
                  @ParamType(type = String.class),
          }, defaultValue = "None")
      },
      useStarlarkThread = true
  )
  public LarkyByteLike asBytes(
      Object _obj,
      Object _encoding,
      Object _errors,
      StarlarkThread thread
  ) throws EvalException {
    if (!LarkyByte.class.isAssignableFrom(_obj.getClass())
        && !StarlarkIterable.class.isAssignableFrom(_obj.getClass())
        && !String.class.isAssignableFrom(_obj.getClass())
        && !NoneType.class.isAssignableFrom(_obj.getClass())) {
      throw Starlark.errorf("want string, bytes, or iterable of ints. got %s", Starlark.type(_obj));
    }

    //bytes() -> empty bytes object
    if (Starlark.isNullOrNone(_obj) || LarkyByte.class.isAssignableFrom(_obj.getClass())) {
      return StarlarkUtil.convertFromNoneable(
          _obj,
          LarkyByte.builder(thread)
              .setSequence(Sequence.cast(_obj, StarlarkInt.class, "nope"))
              .build()
      );
    }

    // handle case where string is passed in.
    // TODO: move this to LarkyBytes class
    if (String.class.isAssignableFrom(_obj.getClass())) {
      // _obj is a string
      String encoding = StarlarkUtil.convertOptionalString(_encoding);
      if (encoding == null) {
        // if encoding is null && _obj is a string, then we have to throw an error
        throw Starlark.errorf("string argument without an encoding");
      }
      Charset charset;
      try {
        charset = Charset.forName(encoding);
      } catch (UnsupportedCharsetException e) {
        throw Starlark.errorf("unknown encoding: %s", e.getMessage());
      }
      /*
       mimic the python behavior such that if string is null, then we convert it to empty string:

      >>> bytes('', 'utf-8')
      b''
      */

      /*
        errors
          The error handling scheme to use for encoding errors.
          The default is 'strict' meaning that encoding errors raise a
          UnicodeEncodeError.  Other possible values are 'ignore', 'replace' and
          'xmlcharrefreplace' as well as any other name registered with
          codecs.register_error that can handle UnicodeEncodeErrors.
       */

      CodingErrorAction errs = TextUtil.CodecHelper.convertCodingErrorAction(
          StarlarkUtil.convertFromNoneable(_errors, TextUtil.CodecHelper.STRICT)
      );

      CharsetDecoder decoder = charset.newDecoder();
      decoder.onMalformedInput(errs);
      decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
      decoder.replaceWith(String.valueOf(TextUtil.REPLACEMENT_CHAR));
      //bytes(string, encoding[, errors]) -> bytes
      return LarkyByte.builder(thread)
          .setSequence(decoder.charset()
              .encode(TextUtil.unescapeJavaString((String) _obj))
          ).build();
    }

    // here we are not null,
    try {
      // do we have an int?
      _obj = StarlarkUtil.valueToStarlark(_obj, thread.mutability());
    } catch (IllegalArgumentException x) {
      // obj is not a value we support, gtfo here
      throw Starlark.errorf("cannot convert '%s' to bytes", x.getMessage());
    }

    String classType = Starlark.classType(_obj.getClass());
    try {
      switch (classType) {
        case "bytes.elems":
        case "list":
          Sequence<StarlarkInt> seq = Sequence.cast(_obj, StarlarkInt.class, classType);
          return LarkyByte.builder(thread).setSequence(seq).build();
        case "int":
          // fallthrough
        default:
          throw Starlark.errorf("unable to convert '%s' to bytes", classType);
      }
    } catch (EvalException | ClassCastException ex) {
      throw Starlark.errorf("%s", ex.getMessage());
    }
  }

  @StarlarkMethod(
      name = "bytearray",
      doc = "Construct an mutable array of bytes from:\n" +
          "  - an iterable yielding integers in range(256)\n" +
          "  - a text string encoded using the specified encoding\n" +
          "  - any object implementing the buffer API.\n" +
          "  - an integer" +
          "\n" +
          "bytearray() -> empty bytearray object" +
          "\n" +
          "bytearray(int) -> bytearray object of size given by the parameter initialized with null bytes" +
          "\n" +
          "bytearray(bytes_or_buffer) -> mutable copy of bytes_or_buffer" +
          "\n" +
          "bytearray(iterable_of_ints) -> bytearray" +
          "\n" +
          "bytearray(string, encoding[, errors]) -> bytearray",
      parameters = {
          @Param(name = "obj"),
          @Param(name = "encoding",
              named = true,
              allowedTypes = {
                  @ParamType(type = NoneType.class),
                  @ParamType(type = String.class),
          }, defaultValue = "None"),
          @Param(name = "errors",
              named = true,
              allowedTypes = {
              @ParamType(type = NoneType.class),
              @ParamType(type = String.class),
          }, defaultValue = "None")
      },
      useStarlarkThread = true
  )
  public LarkyByteLike asByteArray(
       Object _obj,
       Object _encoding,
       Object _errors,
       StarlarkThread thread
   ) throws EvalException {
      if(!LarkyByteArray.class.isAssignableFrom(_obj.getClass())
          && !StarlarkIterable.class.isAssignableFrom(_obj.getClass())
          && !String.class.isAssignableFrom(_obj.getClass())
          && !NoneType.class.isAssignableFrom(_obj.getClass())) {
        throw Starlark.errorf("want string, bytes, or iterable of ints. got %s", Starlark.type(_obj));
      }

     //bytes() -> empty bytes object
     if (Starlark.isNullOrNone(_obj) || LarkyByteArray.class.isAssignableFrom(_obj.getClass())) {
       return StarlarkUtil.convertFromNoneable(
           _obj,
           LarkyByteArray.builder(thread)
               .setSequence(Sequence.cast(_obj, StarlarkInt.class, "nope"))
               .build()
       );
     }

     // handle case where string is passed in.
     // TODO: move this to LarkyBytes class
     if (String.class.isAssignableFrom(_obj.getClass())) {
       // _obj is a string
       String encoding = StarlarkUtil.convertOptionalString(_encoding);
       if (encoding == null) {
         // if encoding is null && _obj is a string, then we have to throw an error
         throw Starlark.errorf("string argument without an encoding");
       }
       Charset charset;
       try {
         charset = Charset.forName(encoding);
       } catch (UnsupportedCharsetException e) {
         throw Starlark.errorf("unknown encoding: %s", e.getMessage());
       }
       /*
        mimic the python behavior such that if string is null, then we convert it to empty string:

       >>> bytes('', 'utf-8')
       b''
       */

       /*
         errors
           The error handling scheme to use for encoding errors.
           The default is 'strict' meaning that encoding errors raise a
           UnicodeEncodeError.  Other possible values are 'ignore', 'replace' and
           'xmlcharrefreplace' as well as any other name registered with
           codecs.register_error that can handle UnicodeEncodeErrors.
        */

       CodingErrorAction errs = TextUtil.CodecHelper.convertCodingErrorAction(
           StarlarkUtil.convertFromNoneable(_errors, TextUtil.CodecHelper.STRICT)
       );

       CharsetDecoder decoder = charset.newDecoder();
       decoder.onMalformedInput(errs);
       decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
       decoder.replaceWith(String.valueOf(TextUtil.REPLACEMENT_CHAR));
       //bytes(string, encoding[, errors]) -> bytes
       return LarkyByteArray.builder(thread)
           .setSequence(decoder.charset()
               .encode(TextUtil.unescapeJavaString((String) _obj))
           ).build();
     }

     // here we are not null,
     try {
       // do we have an int?
       _obj = StarlarkUtil.valueToStarlark(_obj, thread.mutability());
     } catch (IllegalArgumentException x) {
       // obj is not a value we support, gtfo here
       throw Starlark.errorf("cannot convert '%s' to bytes", x.getMessage());
     }

     String classType = Starlark.classType(_obj.getClass());
     try {
       switch (classType) {
         case "bytes":
           _obj = ((LarkyByte) _obj).elems();
           // fall through
         case "bytes.elems":
         case "list":
           Sequence<StarlarkInt> seq = Sequence.cast(_obj, StarlarkInt.class, classType);
           return LarkyByteArray.builder(thread).setSequence(seq).build();
         case "int":
           // fallthrough
         default:
           throw Starlark.errorf("unable to convert '%s' to bytes", classType);
       }
     } catch (EvalException | ClassCastException ex) {
       throw Starlark.errorf("%s", ex.getMessage());
     }
   }
}
