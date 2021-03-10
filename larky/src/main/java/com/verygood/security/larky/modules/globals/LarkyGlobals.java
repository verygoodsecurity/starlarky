package com.verygood.security.larky.modules.globals;

import com.verygood.security.larky.annot.Library;
import com.verygood.security.larky.annot.StarlarkConstructor;
import com.verygood.security.larky.modules.io.TextUtil;
import com.verygood.security.larky.modules.types.LarkyByteArray;
import com.verygood.security.larky.modules.types.Partial;
import com.verygood.security.larky.modules.types.Property;
import com.verygood.security.larky.modules.types.structs.SimpleStruct;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;


/**
 * A library of Larky values (keyed by name) that are not part of core Starlark but are common to
 * all Larky star scripts. Examples: struct, json, etc..
 *
 * Namespaced by _ and should only be accessable via @stdlib/larky:
 *
 * load("@stdlib/larky", "larky")
 */
@Library
public final class LarkyGlobals {

  @StarlarkMethod(
      name = "_struct",
      doc =
          "Creates an immutable struct using the keyword arguments as attributes. It is used to "
              + "group multiple values together. Example:<br>"
              + "<pre class=\"language-python\">s = struct(x = 2, y = 3)\n"
              + "return s.x + getattr(s, \"y\")  # returns 5</pre>",
      extraKeywords =
      @Param(name = "kwargs", defaultValue = "{}", doc = "Dictionary of arguments."),
      useStarlarkThread = true
  )
  @StarlarkConstructor
  public SimpleStruct struct(Dict<String, Object> kwargs, StarlarkThread thread) {
    return SimpleStruct.immutable(kwargs, thread);
  }

  @StarlarkMethod(
      name = "_mutablestruct",
      doc = "Just like struct, but creates an mutable struct using the keyword arguments as attributes",
      extraKeywords =
      @Param(name = "kwargs", defaultValue = "{}", doc = "Dictionary of arguments."),
      useStarlarkThread = true
  )
  @StarlarkConstructor
  public SimpleStruct mutablestruct(Dict<String, Object> kwargs, StarlarkThread thread) {
    return SimpleStruct.mutable(kwargs, thread);
  }

  @StarlarkMethod(
      name = "_partial",
      doc = "Just like struct, but creates an callable struct using a function and its keyword arguments as its attributes",
      parameters = {
          @Param(
              name = "function",
              doc = "The function to invoke when the struct is called"
          )
      },
      extraPositionals = @Param(name = "args"),
      extraKeywords =
      @Param(name = "kwargs", defaultValue = "{}", doc = "Dictionary of arguments.")
  )
  public Partial partial(StarlarkFunction function, Tuple args, Dict<String, Object> kwargs) {
    return Partial.create(function, args, kwargs);
  }

  //b=struct(c=property(callablestruct(_get_data, self)))
  //b.c == _get_data(self)
  @StarlarkMethod(
      name = "_property",
      doc = "Creates an property-like struct using a function and " +
          "its keyword arguments as its attributes. \n" +
          "You can invoke a property using the . instead of (). " +
          "For example: \n" +
          "\n" +
          "  def get_data():\n" +
          "      return {'foo': 1}\n" +
          "  c = struct(data=property(get_data))\n" +
          "  assert c.data == get_data()"
      ,
      parameters = {
          @Param(
              name = "getter",
              doc = "The function to invoke when the struct is called"
          ),
          @Param(
              name = "setter",
              doc = "The function to invoke when the struct is called",
              allowedTypes = {
                  @ParamType(type = StarlarkCallable.class),
                  @ParamType(type = NoneType.class),
              },
              defaultValue = "None"
          )
      },
      extraPositionals = @Param(name = "args"),
      extraKeywords =
      @Param(name = "kwargs", defaultValue = "{}", doc = "Dictionary of arguments."),
      useStarlarkThread = true
  )
  public Property property(StarlarkCallable getter, Object setter, Tuple args, Dict<String, Object> kwargs, StarlarkThread thread) {
    return Property.builder()
        .thread(thread)
        .fget(getter)
        .fset(setter != Starlark.NONE ? (StarlarkCallable) setter : null)
        .build();
  }

  // todo: move this out of here
  static class CodecHelper {
    public static final String STRICT = "strict";
    public static final String IGNORE = "ignore";
    public static final String REPLACE = "replace";
    public static final String BACKSLASHREPLACE = "backslashreplace";
    public static final String NAMEREPLACE = "namereplace";
    public static final String XMLCHARREFREPLACE = "xmlcharrefreplace";
    public static final String SURROGATEESCAPE = "surrogateescape";
    public static final String SURROGATEPASS = "surrogatepass";

    static CodingErrorAction convertCodingErrorAction(String errors) {
      CodingErrorAction errorAction;
      switch (errors) {
        case IGNORE:
          errorAction = CodingErrorAction.IGNORE;
          break;
        case REPLACE:
        case NAMEREPLACE:
          errorAction = CodingErrorAction.REPLACE;
          break;
        case STRICT:
        case BACKSLASHREPLACE:
        case SURROGATEPASS:
        case SURROGATEESCAPE:
        case XMLCHARREFREPLACE:
        default:
          errorAction = CodingErrorAction.REPORT;
          break;
      }
      return errorAction;
    }

  }

  @StarlarkMethod(
      name = "_as_bytearray",
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
          @Param(name = "obj", allowedTypes = {
              @ParamType(type = NoneType.class),
              @ParamType(type = String.class),
              @ParamType(type = LarkyByteArray.class),
              @ParamType(type = LarkyByteArray.Elems.class),
              @ParamType(type = StarlarkInt.class),
              @ParamType(type = StarlarkList.class),
          }, defaultValue = "None"),
          @Param(name = "encoding", allowedTypes = {
              @ParamType(type = NoneType.class),
              @ParamType(type = String.class),
          }, defaultValue = "None"),
          @Param(name = "errors", allowedTypes = {
              @ParamType(type = NoneType.class),
              @ParamType(type = String.class),
          }, defaultValue = "None")
      },
      useStarlarkThread = true
  )
  public LarkyByteArray asByteArray(
      Object _obj,
      Object _encoding,
      Object _errors,
      StarlarkThread thread
  ) throws EvalException {
    //bytes() -> empty bytes object
    if (Starlark.isNullOrNone(_obj) || LarkyByteArray.class.isAssignableFrom(_obj.getClass())) {
      //TODO(mahmoudimus): potential copy constructor bug if class really is larkybyte..test this!
      return StarlarkUtil.convertFromNoneable(_obj, new LarkyByteArray(thread));
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

      CodingErrorAction errs = CodecHelper.convertCodingErrorAction(
          StarlarkUtil.convertFromNoneable(_errors, CodecHelper.STRICT)
      );

      CharsetDecoder decoder = charset.newDecoder();
      decoder.onMalformedInput(errs);
      decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
      decoder.replaceWith(String.valueOf(TextUtil.REPLACEMENT_CHAR));
      //bytes(string, encoding[, errors]) -> bytes
      return new LarkyByteArray(
          thread,
          decoder.charset()
              .encode(TextUtil.unescapeJavaString((String) _obj))
      );
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
        case "int":
          return new LarkyByteArray(thread, ((StarlarkInt) _obj).toIntUnchecked());
        case "bytes.elems":
          return new LarkyByteArray(thread, (LarkyByteArray.Elems) _obj);
        case "list":
          return new LarkyByteArray(thread, (StarlarkList<?>) _obj);
        default:
          throw Starlark.errorf("unable to convert '%s' to bytes", classType);
      }
    } catch (ClassCastException ex) {
      throw Starlark.errorf("%s", ex.getMessage());
    }
  }

}
