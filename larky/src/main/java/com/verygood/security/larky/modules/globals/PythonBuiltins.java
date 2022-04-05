package com.verygood.security.larky.modules.globals;

import java.math.BigDecimal;
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
import com.verygood.security.larky.modules.types.results.LarkyIndexError;
import com.verygood.security.larky.modules.types.results.LarkyStopIteration;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkBytes.StarlarkByteArray;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkIterable;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Structure;
import net.starlark.java.eval.Tuple;


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
      final LarkyObject asLarkyObj = (LarkyObject) x;
      // first, detect __index__
      Object coerceToIntO = asLarkyObj.getField(PyProtocols.__INDEX__);
      // then if it doesn't exist, does it have __int__?
      if (coerceToIntO == null) {
        // deprecated since python 3.8
        coerceToIntO = asLarkyObj.getField(PyProtocols.__INT__);
      }
      if (!StarlarkUtil.isCallable(coerceToIntO)) {
        throw Starlark.errorf("%s object is not callable", StarlarkUtil.richType(coerceToIntO));
      }
      StarlarkCallable coerceToInt = (StarlarkCallable) coerceToIntO;
      if (coerceToInt != null) {
        Object res = asLarkyObj.invoke(thread, coerceToInt, Tuple.empty(), Dict.empty());
        if (!(res instanceof StarlarkInt)) {
          throw Starlark.errorf("%s returned non-int (type %s)", coerceToInt.getName(), StarlarkUtil.richType(res));
        }
        return (StarlarkInt) res;
      }
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
            "" +
            "The two-argument form pow(base, exp) is equivalent to using the power operator: base**exp.",
    parameters = {
      @Param(
        name = "base",
        doc = "The function to invoke when the struct is called",
        named = true,
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = StarlarkFloat.class),
        }
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
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None"
      )
    }
  )
  public Object pow(Object baseO, StarlarkInt exp, Object mod) throws EvalException {
    if (!(baseO instanceof StarlarkInt) && !(baseO instanceof StarlarkFloat)) {
      throw Starlark.errorf("Error in pow: in call to pow(), parameter 'base' got " +
                              "value of type '%s', want 'int' or 'float'", Starlark.type(baseO));
    }
    final BigDecimal bigLeft;
    if (baseO instanceof StarlarkInt) {
      bigLeft = new BigDecimal(((StarlarkInt) baseO).toBigInteger());
    } else {
      bigLeft = BigDecimal.valueOf(((StarlarkFloat) baseO).toDouble());
    }

    final BigInteger bigRight = exp.toBigInteger();

    if (Starlark.isNullOrNone(mod)) {
      if (bigRight.signum() < 0) {
        return StarlarkFloat.of(Math.pow(bigLeft.doubleValue(), bigRight.longValueExact()));
      }
      return StarlarkInt.of(bigLeft.toBigIntegerExact().pow((bigRight.intValueExact())));
    }
    final BigInteger bigMod = ((StarlarkInt) mod).toBigInteger();
    final BigInteger bigModPos = bigMod.signum() < 0 ? bigMod.abs() : bigMod;

    if (bigMod.signum() == 0) {
      throw Starlark.errorf("pow() 3rd argument cannot be 0");
    }

    try {
      BigInteger pow = bigLeft.toBigIntegerExact().modPow(bigRight, bigModPos);
      if (bigModPos.equals(bigMod) || BigInteger.ZERO.equals(pow)) {
        return StarlarkInt.of(pow);
      } else {
        return StarlarkInt.of(pow.subtract(bigModPos));
      }
    } catch (ArithmeticException e) {
      // a positive mod was used, so this exception must mean the exponent was
      // negative and the base is not relatively prime to the exponent
      throw Starlark.errorf("base is not invertible for the given modulus");
    }
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
    if (c.toIntUnchecked() > 0x10FFFF) {
      throw Starlark.errorf("ValueError: chr(%s) arg not in range(0x110000)", c.toIntUnchecked());
    }
    return new String(new int[]{c.toIntUnchecked()}, 0, 1);
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
    throws EvalException, InterruptedException {
    if (LarkyObject.class.isAssignableFrom(obj.getClass())) {
      // if there's an object with a __getattr__, it will be invoked..
      Object getAttrMethod = ((LarkyObject) obj).getField(PyProtocols.__GETATTR__);
      if (getAttrMethod != null) {
        Object res = Starlark.call(thread, getAttrMethod, Tuple.of(name), Dict.empty());
        return (res != null) ? res : defaultValue;
      }
    }
    return Starlark.getattr(
      thread.mutability(),
      thread.getSemantics(),
      obj,
      name,
      defaultValue == Starlark.UNBOUND ? null : defaultValue);
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
    if (Starlark.isNullOrNone(bases) && Starlark.isNullOrNone(dict) && kwargs.size() == 0) {
      // There is no 'type' type in Starlark, so we return a string with the type name.
      if (LarkyObject.class.isAssignableFrom(object.getClass())) {
        return ((LarkyObject) object).type();
      }
      return Starlark.type(object);
    } else if (kwargs.size() != 0) {
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
          @ParamType(type = StarlarkBytes.class),
        }),
    })
  public int hash(Object value) throws EvalException {
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
      typeString = object.type();
      // IF LarkyObject has a `__length_hint__()` method, invoke it. Otherwise, ...
      if (object.hasLengthHintMethod()) {
        return (StarlarkInt) object.invoke(object.getLengthHintMethod());
      }
    } else if (LarkyObject.class.isAssignableFrom(x.getClass())) {
      LarkyObject object = ((LarkyObject) x);
      typeString = object.type();

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
        objType = ((LarkyObject) x).type();
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
      return StarlarkBytes.copyOf(
        thread.mutability(),
        decoder
          .charset()
          .encode(TextUtil.unescapeJavaString((String) _obj)));
//      return StarlarkBytes.builder(thread)
//          .setSequence(decoder.charset()
//              .encode(TextUtil.unescapeJavaString((String) _obj))
//          ).build();
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
        case "bytes.elems":
        case "list":
          Sequence<StarlarkInt> seq = Sequence.cast(_obj, StarlarkInt.class, classType);
          return StarlarkBytes.copyOf(thread.mutability(), seq);
        //return StarlarkBytes.builder(thread).setSequence(seq).build();
        case "int":
          // fallthrough
        default:
          throw Starlark.errorf("unable to convert '%s' to bytes", classType);
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

    //bytes() -> empty bytes object
    if (Starlark.isNullOrNone(_obj)
          || StarlarkByteArray.class.isAssignableFrom(_obj.getClass())) {
//           || StarlarkBytes.class.isAssignableFrom(_obj.getClass())) {
      return StarlarkUtil.convertFromNoneable(
        _obj,
        StarlarkByteArray.of(thread.mutability())
//           StarlarkBytes.builder(thread)
//               .setSequence(new byte[]{})
//               .build()
      );
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
      return StarlarkByteArray.of(StarlarkBytes.copyOf(
        thread.mutability(),
        decoder.charset()
          .encode(TextUtil.unescapeJavaString((String) _obj))));
//       return StarlarkBytes.builder(thread)
//           .setSequence(decoder.charset()
//               .encode(TextUtil.unescapeJavaString((String) _obj))
//           ).build();
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
          // fall through
        case "bytes.elems":
        case "list":
          Sequence<StarlarkInt> seq = Sequence.cast(_obj, StarlarkInt.class, classType);
          return StarlarkByteArray.copyOf(thread.mutability(), seq);
        case "int":
          // fallthrough
        default:
          throw Starlark.errorf("unable to convert '%s' to bytes", classType);
      }
    } catch (ClassCastException ex) {
      throw Starlark.errorf("%s", ex.getMessage());
    }
  }
}
