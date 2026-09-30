package com.verygood.security.larky.modules.globals;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;

import com.verygood.security.larky.annot.Library;
import com.verygood.security.larky.modules.codecs.TextUtil;
import com.verygood.security.larky.modules.types.LarkyIterator;
import com.verygood.security.larky.modules.types.LarkyObject;
import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.modules.types.results.LarkyAttributeError;
import com.verygood.security.larky.modules.types.results.LarkyIndexError;
import com.verygood.security.larky.modules.types.results.LarkyStopIteration;
import com.verygood.security.larky.objects.PyObject;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.IntLimits;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.FormatSpec;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkBytes.StarlarkByteArray;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkIterable;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Structure;
import net.starlark.java.eval.Tuple;
import net.starlark.java.spelling.SpellChecker;


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
    name = "int",
    doc =
      "Returns x as an int value."
        + "<ul>"
        + "<li>If <code>x</code> is already an int, <code>int</code> returns it unchanged." //
        + "<li>If <code>x</code> is a bool, <code>int</code> returns 1 for True and 0 for"
        + " False." //
        + "<li>If <code>x</code> is a string, it must have the format "
        + "    <code>&lt;sign&gt;&lt;prefix&gt;&lt;digits&gt;</code>. "
        + "    <code>&lt;sign&gt;</code> is either <code>\"+\"</code>, <code>\"-\"</code>, "
        + "    or empty (interpreted as positive). <code>&lt;digits&gt;</code> are a "
        + "    sequence of digits from 0 up to <code>base</code> - 1, where the letters a-z "
        + "    (or equivalently, A-Z) are used as digits for 10-35. In the case where "
        + "    <code>base</code> is 2/8/16, <code>&lt;prefix&gt;</code> is optional and may "
        + "    be 0b/0o/0x (or equivalently, 0B/0O/0X) respectively; if the "
        + "    <code>base</code> is any other value besides these bases or the special value "
        + "    0, the prefix must be empty. In the case where <code>base</code> is 0, the "
        + "    string is interpreted as an integer literal, in the sense that one of the "
        + "    bases 2/8/10/16 is chosen depending on which prefix if any is used. If "
        + "    <code>base</code> is 0, no prefix is used, and there is more than one digit, "
        + "    the leading digit cannot be 0; this is to avoid confusion between octal and "
        + "    decimal. The magnitude of the number represented by the string must be within "
        + "    the allowed range for the int type." //
        + "<li>If <code>x</code> is a float, <code>int</code> returns the integer value of"
        + "    the float, rounding towards zero. It is an error if x is non-finite (NaN or"
        + "    infinity)."
        + "</ul>" //
        + "This function fails if <code>x</code> is any other type, or if the value is a "
        + "string not satisfying the above format. Unlike Python's <code>int</code> "
        + "function, this function does not allow zero arguments, and does "
        + "not allow extraneous whitespace for string arguments.<p>" //
        + "Examples:<pre class=\"language-python\">int(\"123\") == 123\n"
        + "int(\"-123\") == -123\n"
        + "int(\"+123\") == 123\n"
        + "int(\"FF\", 16) == 255\n"
        + "int(\"0xFF\", 16) == 255\n"
        + "int(\"10\", 0) == 10\n"
        + "int(\"-0x10\", 0) == -16\n"
        + "int(\"-0x10\", 0) == -16\n"
        + "int(\"123.456\") == 123\n"
        + "</pre>",
    parameters = {
      @Param(name = "x", doc = "The string to convert."),
      @Param(
        name = "base",
        defaultValue = "unbound",
        doc =
          "The base used to interpret a string value; defaults to 10. Must be between 2 "
            + "and 36 (inclusive), or 0 to detect the base as if <code>x</code> were an "
            + "integer literal. This parameter must not be supplied if the value is not a "
            + "string.",
        named = true)
    }, useStarlarkThread = true)
  public StarlarkInt intForStarlark(Object x, Object baseO, StarlarkThread thread) throws EvalException {
      /*
      Losslessly convert an object to an integer object.

      If obj is an instance of int, return it directly. Otherwise call __index__()
      and require it be a direct instance of int (raising TypeError if it isn't).
      */
    if (x instanceof String) {
      int base = baseO == Starlark.UNBOUND ? 10 : Starlark.toInt(baseO, "base");
      try {
        return StarlarkInt.parse((String) x, base);
      } catch (NumberFormatException ex) {
        throw Starlark.errorf("%s", ex.getMessage());
      }
    } else if (x instanceof LarkyObject) {
      return ((LarkyObject) x).coerceToInt(thread);
    }

    if (baseO != Starlark.UNBOUND) {
      throw Starlark.errorf("can't convert non-string with explicit base");
    }
    if (x instanceof Boolean) {
      return StarlarkInt.of((boolean) x ? 1 : 0);
    } else if (x instanceof StarlarkInt) {
      return (StarlarkInt) x;
    } else if (x instanceof StarlarkFloat) {
      try {
        return StarlarkEvalWrapper.ofFiniteDouble(((StarlarkFloat) x).toDouble());
      } catch (IllegalArgumentException unused) {
        throw Starlark.errorf("can't convert float %s to int", x);
      }
    }
    throw Starlark.errorf("got %s, want string, int, float, or bool", Starlark.type(x));
  }

  @StarlarkMethod(
    name = "pow",
    doc = "Return base to the power exp; if mod is present, return base to " +
            "the power exp, modulo mod (computed more efficiently than pow(base, exp) % mod). " +
            "The two-argument form pow(base, exp) is equivalent to Python's power operator " +
            "base**exp: two ints give an int, or a float when exp is negative; any float " +
            "operand gives a float. The three-argument form requires ints; a negative exp " +
            "then uses the modular inverse of base.",
    parameters = {
      @Param(
        name = "base",
        doc = "The base.",
        named = true,
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = StarlarkFloat.class),
        }
      ),
      @Param(
        name = "exp",
        doc = "The exponent.",
        named = true,
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = StarlarkFloat.class),
        }
      ),
      @Param(
        name = "mod",
        doc = "The modulus; requires int base and exp.",
        named = true,
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None"
      )
    }
  )
  public Object pow(Object baseO, Object expO, Object mod) throws EvalException {
    if (!Starlark.isNullOrNone(mod)) {
      if (!(baseO instanceof StarlarkInt) || !(expO instanceof StarlarkInt)) {
        throw Starlark.errorf(
          "TypeError: pow() 3rd argument not allowed unless all arguments are integers");
      }
      return intModPow((StarlarkInt) baseO, (StarlarkInt) expO, (StarlarkInt) mod);
    }
    if (baseO instanceof StarlarkInt base && expO instanceof StarlarkInt exp && exp.signum() >= 0) {
      int e;
      try {
        e = exp.toIntUnchecked();
      } catch (IllegalArgumentException ex) {
        throw Starlark.errorf("OverflowError: pow() exponent too large: %s", exp);
      }
      BigInteger b = base.toBigInteger();
      IntLimits.checkPow(b, e);
      BigInteger z;
      try {
        z = b.pow(e);
      } catch (ArithmeticException ex) { // result exceeds BigInteger's range
        throw Starlark.errorf("OverflowError: pow() result too large");
      }
      IntLimits.checkBits(z.bitLength(), "pow");
      return StarlarkInt.of(z);
    }
    // Python converts both operands to float for a float operand or a negative int exponent.
    return StarlarkFloat.of(floatPow(toDouble(baseO), toDouble(expO)));
  }

  private static double toDouble(Object x) throws EvalException {
    return x instanceof StarlarkInt i ? i.toFiniteDouble() : ((StarlarkFloat) x).toDouble();
  }

  private static boolean isOddInteger(double x) {
    return Math.abs(x) < 0x1p53 && x == Math.rint(x) && Math.abs(x % 2.0) == 1.0;
  }

  /** x ** y for floats, following CPython's float_pow (special cases before libm pow). */
  private static double floatPow(double x, double y) throws EvalException {
    if (y == 0.0) {
      return 1.0; // even for a NaN base
    }
    if (Double.isNaN(x)) {
      return x;
    }
    if (Double.isNaN(y)) {
      return x == 1.0 ? 1.0 : y;
    }
    if (Double.isInfinite(y)) {
      double ax = Math.abs(x);
      if (ax == 1.0) {
        return 1.0;
      }
      return (y > 0) == (ax > 1.0) ? Double.POSITIVE_INFINITY : 0.0;
    }
    if (Double.isInfinite(x)) {
      boolean odd = isOddInteger(y);
      if (y > 0) {
        return odd ? x : Math.abs(x);
      }
      return odd ? Math.copySign(0.0, x) : 0.0;
    }
    if (x == 0.0) {
      if (y < 0) {
        throw Starlark.errorf("ZeroDivisionError: 0.0 cannot be raised to a negative power");
      }
      return isOddInteger(y) ? x : 0.0;
    }
    boolean negate = false;
    if (x < 0) {
      if (y != Math.floor(y)) {
        // Python returns a complex number here; Starlark has none.
        throw Starlark.errorf(
          "ValueError: negative number cannot be raised to a fractional power");
      }
      x = -x;
      negate = isOddInteger(y);
    }
    if (x == 1.0) {
      return negate ? -1.0 : 1.0;
    }
    double z = Math.pow(x, y);
    if (Double.isInfinite(z)) {
      throw Starlark.errorf("OverflowError: pow() result too large");
    }
    return negate ? -z : z;
  }

  /** pow(base, exp, mod) for ints, as in Python 3.8+ (a negative exp inverts base). */
  private static StarlarkInt intModPow(StarlarkInt base, StarlarkInt exp, StarlarkInt mod)
      throws EvalException {
    BigInteger m = mod.toBigInteger();
    IntLimits.checkModPow(exp.toBigInteger(), m);
    if (m.signum() == 0) {
      throw Starlark.errorf("pow() 3rd argument cannot be 0");
    }
    BigInteger absMod = m.abs();
    BigInteger r;
    try {
      r = base.toBigInteger().modPow(exp.toBigInteger(), absMod);
    } catch (ArithmeticException e) {
      // absMod is positive, so this means exp < 0 and base has no inverse modulo absMod.
      throw Starlark.errorf("base is not invertible for the given modulus");
    }
    // r is in [0, |mod|); Python's result takes the sign of mod.
    if (m.signum() < 0 && r.signum() != 0) {
      r = r.subtract(absMod);
    }
    return StarlarkInt.of(r);
  }

  @StarlarkMethod(
    name = "bin",
    doc = "Convert an integer number to a binary string prefixed with '0b'. The result is a " +
            "valid Python expression. If x is not a Python int object, it has to define" +
            " an __index__() method that returns an integer.",
    parameters = {
      @Param(
        name = "x",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
        }
      )
    }
  )
  public String bin(StarlarkInt x) throws EvalException {
    String prefix = "0b";
    StringBuilder sb = new StringBuilder();
    BigInteger value = x.toBigInteger();
    if (x.signum() == -1) {
      sb.append('-');
    }
    sb.append(prefix);
    sb.append(value.abs().toString(2));
    return sb.toString();
  }

  @StarlarkMethod(name = "StopIteration")
  public LarkyStopIteration stopIteration() {//throws LarkyStopIteration {
    return LarkyStopIteration.getInstance();
  }

  @StarlarkMethod(name = "IndexError")
  public LarkyIndexError indexError() {//throws LarkyIndexError {
    return LarkyIndexError.getInstance();
  }

  @StarlarkMethod(name = "AttributeError")
  public LarkyAttributeError attributeError() {//throws LarkyAttributeError {
    return LarkyAttributeError.getInstance();
  }

  // iter(object[, sentinel])
  /*
  iter(iterable) -> iterator
  iter(callable, sentinel) -> iterator

   */
  @StarlarkMethod(name = "iter",
    doc = "Get an iterator from an object.  In the first form, the argument must\n" +
            "supply its own iterator, or be a sequence.\n" +
            "In the second form, the callable is called until it returns the sentinel.\n",
    parameters = {
      @Param(name = "iterable"),
      @Param(name = "sentinel", defaultValue = "unbound")
    }, useStarlarkThread = true)
  public LarkyIterator iter(Object iterableO, Object sentinelO, StarlarkThread thread)
    throws EvalException {

    if (StarlarkUtil.isNullOrNoneOrUnbound(sentinelO)) {
      return LarkyIterator.from(iterableO, thread);
    }

    if (!StarlarkUtil.isCallable(iterableO)) {
      throw Starlark.errorf("TypeError: iter(v, w): v must be callable");
    }

    return LarkyIterator.LarkyCallableIterator.of(
      StarlarkUtil.toCallable(iterableO), sentinelO, thread);
  }

  @StarlarkMethod(name = "next",
    doc = "next(iterator[, default])\n" +
            "\n" +
            "Return the next item from the iterator. If default is given and the iterator\n" +
            "is exhausted, it is returned instead of raising StopIteration.\n",
    parameters = {
      @Param(name = "iterator"),
      @Param(name = "default", defaultValue = "unbound")
    }, useStarlarkThread = true)
  public Object next(Object iteratorO, Object defaultO, StarlarkThread thread) throws EvalException {
    final LarkyIterator iterator;
    if (iteratorO instanceof LarkyIterator) {
      iterator = (LarkyIterator) iteratorO;
    }
    // there could be a delegated __next__
    else if (iteratorO instanceof LarkyObject && LarkyIterator.isIterator((LarkyObject) iteratorO)) {
      iterator = LarkyIterator.LarkyObjectIterator.of((LarkyObject) iteratorO, thread);
    } else {
      throw Starlark.errorf("TypeError: '%s' object is not an iterator",
        StarlarkUtil.richType(iteratorO));
    }

    iterator.setCurrentThread(thread);
    if (iterator.hasNext()) {
      return iterator.next();
    }
    // If default is given and the iterator is exhausted, it is returned
    if (defaultO != Starlark.UNBOUND) {
      return defaultO;
    }
    // else raise StopIteration
    throw LarkyStopIteration.getInstance();
  }

  @StarlarkMethod(
    name = "chr",
    doc = "Return the string representing a character whose Unicode code point is the " +
            "integer i. For example, chr(97) returns the string 'a', while chr(8364) returns " +
            "the string '€'. This is the inverse of ord().\n" +
            "\n" +
            "The valid range for the argument is from 0 through 1,114,111 " +
            "(0x10FFFF in base 16). ValueError will be raised if i is outside that range.",
    parameters = {
      @Param(
        name = "i",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
        }
      )
    },
    useStarlarkThread = true
  )
  public String chr(StarlarkInt c, StarlarkThread thread) throws EvalException {
    // A lone surrogate (0xD800-0xDFFF) is returned as a one-char string, as in Python.
    if (c.signum() < 0 || c.compareTo(StarlarkInt.of(0x10FFFF)) > 0) {
      throw Starlark.errorf("ValueError: chr() arg not in range(0x110000)");
    }
    return new String(new int[]{c.toIntUnchecked()}, 0, 1);
  }

  @StarlarkMethod(
      name = "format",
      doc =
          "Returns <code>value</code> formatted by <code>format_spec</code>, in Python's format "
              + "specification mini-language: "
              + "<code>[[fill]align][sign][#][0][width][grouping][.precision][type]</code>. "
              + "Strings, ints, bools and floats support it; an object with a "
              + "<code>__format__</code> method formats itself; any other value accepts only "
              + "the empty specification, which formats it as <code>str()</code> does."
              + "<pre class=\"language-python\">format(3.14159, \".2f\") == \"3.14\"\n"
              + "format(1234567, \",\") == \"1,234,567\"\n"
              + "format(\"x\", \"*^5\") == \"**x**\"</pre>",
      parameters = {
        @Param(name = "value"),
        @Param(
            name = "format_spec",
            allowedTypes = {@ParamType(type = String.class)},
            defaultValue = "''")
      },
      useStarlarkThread = true)
  public String format(Object value, String formatSpec, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (value instanceof LarkyObject obj) {
      Object dunder = obj.getField(PyProtocols.__FORMAT__, thread);
      if (dunder instanceof StarlarkCallable) {
        Object result = Starlark.call(thread, dunder, Tuple.of(formatSpec), Dict.empty());
        if (!(result instanceof String)) {
          throw Starlark.errorf(
              "TypeError: __format__ must return a str, not %s", Starlark.type(result));
        }
        return (String) result;
      }
      if (!formatSpec.isEmpty()) {
        throw Starlark.errorf(
            "unsupported format string passed to %s.__format__", obj.typeName());
      }
    }
    return FormatSpec.format(value, formatSpec, thread.getSemantics());
  }

  // A default no attribute lookup can return, for hasattr.
  private static final Object MISSING = new Object();

  //override built-in hasattr
  /** Returns true if the object has a field of the given name, otherwise false. */
  @StarlarkMethod(
      name = "hasattr",
      doc =
          "Returns True if the object <code>x</code> has an attribute or method of the given "
              + "<code>name</code>, otherwise False. Example:<br>"
              + "<pre class=\"language-python\">hasattr(ctx.attr, \"myattr\")</pre>",
      parameters = {
        @Param(name = "x", doc = "The object to check."),
        @Param(name = "name", doc = "The name of the attribute.")
      },
      useStarlarkThread = true)
  public boolean hasattr(Object obj, String name, StarlarkThread thread) throws EvalException {
    // Look up with a default, so that a missing attribute returns it instead of building an
    // error message (with spelling suggestions) that would only be discarded. (Starlark.UNBOUND
    // would not do: getattr treats it as "no default" and throws.)
    try {
      Object res = getattr(obj, name, MISSING, thread);
      return res != null && res != MISSING && res != LarkyAttributeError.getInstance();
    } catch(EvalException ex) {
      return false;
    }
  }

  //override built-in getattr

  @StarlarkMethod(
    name = "getattr",
    doc =
      "Returns the struct's field of the given name if it exists. If not, it either returns "
        + "<code>default</code> (if specified) or raises an error. "
        + "<code>getattr(x, \"foobar\")</code> is equivalent to <code>x.foobar</code>."
        + "<pre class=\"language-python\">getattr(ctx.attr, \"myattr\")\n"
        + "getattr(ctx.attr, \"myattr\", \"mydefault\")</pre>",
    parameters = {
      @Param(name = "x", doc = "The struct whose attribute is accessed."),
      @Param(name = "name", doc = "The name of the struct attribute."),
      @Param(
        name = "default",
        defaultValue = "unbound",
        doc =
          "The default value to return in case the struct "
            + "doesn't have an attribute of the given name.")
    },
    useStarlarkThread = true)
  public Object getattr(Object obj, String name, Object defaultValue, StarlarkThread thread)
    throws EvalException {
    Object result = defaultValue;
    if (obj instanceof LarkyObject) {
      Object res;
      try {
        if (obj instanceof PyObject) {
          res = ((PyObject) obj).getField(name, thread);
        } else {
          // TODO(mahmoudimus): delete all this code when merging LarkyObject into PyObject
          res = ((LarkyObject) obj).getField(name, thread);
          if (res == null) {
            // if there's an object with a __getattr__, it will be invoked..
            Object getAttrMethod = ((LarkyObject) obj).getField(PyProtocols.__GETATTR__);
            if (getAttrMethod != null) {
              res = Starlark.call(thread, getAttrMethod, Tuple.of(name), Dict.empty());
            }
          }
        }
      } catch (InterruptedException | Starlark.UncheckedEvalException | StarlarkEvalWrapper.Exc.RuntimeEvalException ex) {
        throw new EvalException(ex.getCause());
      }
      result = res != null ? res : defaultValue;
    }

    if (result == defaultValue) {
      try {
        result = Starlark.getattr(
          thread.mutability(),
          thread.getSemantics(),
          obj,
          name,
          defaultValue == Starlark.UNBOUND ? null : defaultValue);
      } catch (InterruptedException e) {
        throw new EvalException(e.getCause());
      }
    }

    if (result == Starlark.UNBOUND) {
      throw Starlark.errorf(
        "'%s' value has no field or method '%s'%s",
        StarlarkUtil.richType(obj),
        name,
        SpellChecker.didYouMean(
          name,
          Starlark.dir(thread.mutability(), thread.getSemantics(), name)));
    }
    return result;
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
          @ParamType(type = StarlarkBytes.class),
        }),
    })
  public int hash(Object value) throws EvalException {
    if (value instanceof StarlarkValue v) {
      v.checkHashable(); // bytearray is unhashable, as in Python
    }
    return value.hashCode();
  }

  @StarlarkMethod(
    name = "hex",
    doc = "Return the hexadecimal representation of an integer." +
            "\n" +
            ">>> hex(12648430)" +
            "\n" +
            "'0xc0ffee'",
    parameters = {
      @Param(
        name = "number",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
        }),
    })
  public String hex(StarlarkInt number) throws EvalException {
    String prefix = "0x";
    StringBuilder sb = new StringBuilder();
    BigInteger value = number.toBigInteger();
    if (value.signum() < 0) {
      sb.append('-');
    }
    sb.append(prefix);
    sb.append(value.abs().toString(16));
    return sb.toString();
  }


  @StarlarkMethod(
    name = "setattr",
    doc =
      "Sets the named attribute on the given object to the specified value.\n" +
        "\n" +
        "setattr(x, 'y', v) is equivalent to ``x.y = v''" +
        "\n" +
        "If not, it either returns " +
        "<code>default</code> (if specified) or raises an error. ",
    parameters = {
      @Param(name = "x", doc = "The struct whose attribute is accessed."),
      @Param(name = "name", doc = "The name of the struct attribute."),
      @Param(name = "value", doc = "the value to update the named field with  the Starlark statement")
    },
    useStarlarkThread = true)
  public void setattr(Object obj, String name, Object value, StarlarkThread thread)
    throws EvalException {
    if (!Structure.class.isAssignableFrom(obj.getClass())) {
      throw Starlark.errorf(
        "type(%s) does not support setattr. Must inherit from " +
          "Structure. See LarkyObject.", Starlark.type(obj));
    }
    ((Structure) obj).setField(name, value);
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
          throw Starlark.errorf("TypeError: bad operand type for abs(): '%s'", classType);
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
      @Param(
        name = "a",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = StarlarkFloat.class),
        }),
      @Param(
        name = "b",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = StarlarkFloat.class),
        }),
    }
  )
  public Tuple divmod(Object a, Object b) throws EvalException {
    if (a instanceof StarlarkInt x && b instanceof StarlarkInt y) {
      if (y.signum() == 0) {
        throw Starlark.errorf("integer division or modulo by zero");
      }
      // Floor division, as Python's // and %: the remainder takes the sign of the divisor.
      return Tuple.of(StarlarkInt.floordiv(x, y), StarlarkInt.mod(x, y));
    }
    return floatDivmod(toDouble(a), toDouble(b));
  }

  /** divmod for floats, following CPython's float_divmod. */
  private static Tuple floatDivmod(double vx, double wx) throws EvalException {
    if (wx == 0.0) {
      throw Starlark.errorf("floating-point division or modulo by zero");
    }
    double mod = vx % wx; // Java's % on doubles is C's fmod
    double div = (vx - mod) / wx;
    if (mod != 0.0) {
      if ((wx < 0) != (mod < 0)) {
        mod += wx;
        div -= 1.0;
      }
    } else {
      mod = Math.copySign(0.0, wx);
    }
    double floordiv;
    if (div != 0.0) {
      floordiv = Math.floor(div);
      if (div - floordiv > 0.5) {
        floordiv += 1.0;
      }
    } else {
      floordiv = Math.copySign(0.0, vx / wx);
    }
    return Tuple.of(StarlarkFloat.of(floordiv), StarlarkFloat.of(mod));
  }

  @StarlarkMethod(
    name = "len",
    doc =
      "Returns the length of a string, sequence (such as a list or tuple), dict, or other"
        + " iterable.",
    parameters = {@Param(name = "x", doc = "The value whose length to report.")}
  )
  public StarlarkInt len(Object x) throws EvalException {
    final String typeString;
    if (LarkyIterator.class.isAssignableFrom(x.getClass())) {
      LarkyIterator object = ((LarkyIterator) x);
      typeString = object.typeName();
      // IF LarkyObject has a `__length_hint__()` method, invoke it. Otherwise, ...
      if (object.hasLengthHintMethod()) {
        return (StarlarkInt) object.invoke(object.getLengthHintMethod());
      }
    } else if (LarkyObject.class.isAssignableFrom(x.getClass())) {
      LarkyObject object = ((LarkyObject) x);
      typeString = object.typeName();

      // IF LarkyObject has a `__len__()` method, invoke it. Otherwise, ...
      // TODO(mahmoudimus): This should be a sub type of LarkyObject(?) called
      //  Sizeable that hasLenMethod() and getLenMethod()
      if (object.hasLenField()) {
        return (StarlarkInt) object.invoke(object.getField(PyProtocols.__LEN__));
      }
    } else {
      typeString = Starlark.type(x);
    }
    int len = Starlark.len(x);
    if (len < 0) {
      throw Starlark.errorf("%s is not iterable", typeString);
    }
    return StarlarkInt.of(len);
  }

  @StarlarkMethod(
    name = "id",
    doc = "Return the 'identity' of an object. This is an integer which is " +
            "guaranteed to be unique and constant for this object during " +
            "its lifetime. Two objects with non-overlapping lifetimes may" +
            " have the same id() value.",
    parameters = {@Param(name = "object", doc = "The value whose identity to report.")}
  )
  public StarlarkInt id(Object x) throws EvalException {
    return StarlarkInt.of(System.identityHashCode(x));
  }


  @StarlarkMethod(
    name = "list",
    doc =
      "Returns a new list with the same elements as the given iterable value."
        + "<pre class=\"language-python\">list([1, 2]) == [1, 2]\n"
        + "list((2, 3, 2)) == [2, 3, 2]\n"
        + "list({5: \"a\", 2: \"b\", 4: \"c\"}) == [5, 2, 4]</pre>",
    parameters = {@Param(name = "x", defaultValue = "[]", doc = "The object to convert.")},
    useStarlarkThread = true)
  public StarlarkList<?> list(Object x, StarlarkThread thread) throws EvalException {
    final String errmsg = "Error in list: in call to list(), parameter 'x' got value of type '%s', want 'iterable'";
    final Object[] arr;

    // convert to array
    if (x instanceof StarlarkIterable) {
      arr = Starlark.toArray(x);
    } else {
      final String objType;
      if (x instanceof LarkyObject) {
        objType = ((LarkyObject) x).typeName();
        try {
          arr = Starlark.toArray(LarkyIterator.from(x, thread));
        } catch (EvalException ex) {
          throw Starlark.errorf(errmsg, objType);
        }
      } else {
        objType = Starlark.type(x);
        throw Starlark.errorf(errmsg, objType);
      }
    }
    return StarlarkEvalWrapper.zeroCopyList(thread.mutability(), arr);
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
            "bytes(bytes_or_buffer) -> immutable copy of bytes_or_buffer" +
            "\n" +
            "bytes(iterable_of_ints) -> bytes" +
            "\n" +
            "bytes(string, encoding[, errors]) -> bytes",
    parameters = {
      @Param(name = "obj", defaultValue = "None"),
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
  public StarlarkBytes asBytes(
    Object _obj,
    Object _encoding,
    Object _errors,
    StarlarkThread thread
  ) throws EvalException {
    if (!StarlarkBytes.class.isAssignableFrom(_obj.getClass())
          && !StarlarkIterable.class.isAssignableFrom(_obj.getClass())
          && !String.class.isAssignableFrom(_obj.getClass())
          && !NoneType.class.isAssignableFrom(_obj.getClass())) {
      throw Starlark.errorf("want string, bytes, or iterable of ints. got %s", Starlark.type(_obj));
    }
//    // if it's bytes, just return
//    if(Starlark.type(_obj).equals("bytes")) {
//      if (_obj instanceof StarlarkBytes) {
//        return (StarlarkBytes) _obj;
//      }
//    }

    //bytes() -> empty bytes object
    if (Starlark.isNullOrNone(_obj)) {
      return StarlarkUtil.convertFromNoneable(_obj, StarlarkBytes.empty());
    } else if (_obj instanceof StarlarkBytes) {
      return StarlarkBytes.immutableCopyOf(((StarlarkBytes) _obj).elems());
    }

    // handle case where string is passed in.
    // TODO: move this to StarlarkBytess class
    if (String.class.isAssignableFrom(_obj.getClass())) {
      // _obj is a string
      String encoding = StarlarkUtil.convertOptionalString(_encoding);
      if (encoding == null) {
        // if encoding is null && _obj is a string, then we have to throw an error
        throw Starlark.errorf("string argument without an encoding");
      }
      // Encoded by the same codecs as codecs.encode (CPython's names and error handlers). Unlike
      // Python, escapes written as text in the string (r"\x00") are interpreted first: Larky
      // scripts write arbitrary bytes this way (bytes(r"\x80", encoding="utf-8")).
      String errors = StarlarkUtil.convertFromNoneable(_errors, TextUtil.CodecHelper.STRICT);
      byte[] encoded =
          TextUtil.PyCodecs.encode(TextUtil.unescapeJavaString((String) _obj), encoding, errors);
      return StarlarkBytes.of(thread.mutability(), encoded);
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
        case "bytearray":
          _obj = ((StarlarkBytes) _obj).elems(); // "type safety" :D
          return StarlarkBytes.copyOf(thread.mutability(), byteValues(_obj, classType));
        case "int":
          throw Starlark.errorf("unable to convert '%s' to bytes", classType);
        default:
          return StarlarkBytes.copyOf(thread.mutability(), byteValues(_obj, classType));
      }
    } catch (ClassCastException ex) {
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
            "bytearray(bytes_or_buffer) -> mutable copy of bytes_or_buffer" +
            "\n" +
            "bytearray(iterable_of_ints) -> bytearray" +
            "\n" +
            "bytearray(string, encoding[, errors]) -> bytearray",
    parameters = {
      @Param(name = "obj", defaultValue = "None"),
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
  public StarlarkByteArray asByteArray(
    Object _obj,
    Object _encoding,
    Object _errors,
    StarlarkThread thread
  ) throws EvalException {
    if (!StarlarkBytes.class.isAssignableFrom(_obj.getClass())
          && !StarlarkIterable.class.isAssignableFrom(_obj.getClass())
          && !String.class.isAssignableFrom(_obj.getClass())
          && !NoneType.class.isAssignableFrom(_obj.getClass())) {
      throw Starlark.errorf("want string, bytes, or iterable of ints. got %s", Starlark.type(_obj));
    }

    //bytearray() -> empty bytearray object
    if (Starlark.isNullOrNone(_obj)) {
      return StarlarkByteArray.of(thread.mutability());
    }
    // bytearray(bytearray) is a new, independent copy, as in Python.
    if (_obj instanceof StarlarkByteArray) {
      return StarlarkByteArray.of(thread.mutability(), ((StarlarkByteArray) _obj).toByteArray());
    }

    // handle case where string is passed in.
    // TODO: move this to StarlarkBytess class
    if (String.class.isAssignableFrom(_obj.getClass())) {
      // _obj is a string
      String encoding = StarlarkUtil.convertOptionalString(_encoding);
      if (encoding == null) {
        // if encoding is null && _obj is a string, then we have to throw an error
        throw Starlark.errorf("string argument without an encoding");
      }
      // Encoded by the same codecs as codecs.encode (CPython's names and error handlers). Unlike
      // Python, escapes written as text in the string (r"\x00") are interpreted first: Larky
      // scripts write arbitrary bytes this way (bytes(r"\x80", encoding="utf-8")).
      String errors = StarlarkUtil.convertFromNoneable(_errors, TextUtil.CodecHelper.STRICT);
      byte[] encoded =
          TextUtil.PyCodecs.encode(TextUtil.unescapeJavaString((String) _obj), encoding, errors);
      return StarlarkByteArray.of(thread.mutability(), encoded);
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
          _obj = ((StarlarkBytes) _obj).elems();
          classType = Starlark.classType(_obj.getClass());
          return StarlarkByteArray.copyOf(thread.mutability(), byteValues(_obj, classType));
        case "int":
          throw Starlark.errorf("unable to convert '%s' to bytes", classType);
        default:
          return StarlarkByteArray.copyOf(thread.mutability(), byteValues(_obj, classType));
      }
    } catch (ClassCastException ex) {
      throw Starlark.errorf("%s", ex.getMessage());
    }
  }

  /**
   * The elements of {@code x}, an iterable of ints (a list, tuple, range, {@code bytes.elems},
   * ...), as bytes() and bytearray() take them.
   */
  private static Sequence<StarlarkInt> byteValues(Object x, String what) throws EvalException {
    if (x instanceof String || !(x instanceof StarlarkIterable)) {
      throw Starlark.errorf("unable to convert '%s' to bytes", what);
    }
    Object seq = x instanceof Sequence ? x : StarlarkList.immutableCopyOf(Starlark.toIterable(x));
    return Sequence.cast(seq, StarlarkInt.class, what);
  }
}
