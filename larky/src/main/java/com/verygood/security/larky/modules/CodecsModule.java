package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.codecs.TextUtil;
import java.nio.charset.StandardCharsets;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;


@StarlarkBuiltin(
    name = "codecs",
    category = "BUILTIN",
    doc = "This module provides codecs")
public class CodecsModule implements StarlarkValue {

  public static final CodecsModule INSTANCE = new CodecsModule();
  private static final String UTF8 = StandardCharsets.UTF_8.toString().toLowerCase();

  @StarlarkMethod(
      name = "encode",
      doc = "Encodes obj using the codec registered for encoding.\n" +
          "\n" +
          "The default encoding is 'utf-8'.  errors may be given to set a\n" +
          "different error handling scheme.  Default is 'strict' meaning that encoding\n" +
          "errors raise a ValueError.  Other possible values are 'ignore', 'replace'\n" +
          "and 'backslashreplace' as well as any other name registered with\n" +
          "codecs.register_error that can handle ValueErrors.\n",
      parameters = {
          @Param(
              name = "obj",
              allowedTypes = {
                  @ParamType(type = String.class),
              }
          ),
          @Param(
              name = "encoding",
              allowedTypes = {
                  @ParamType(type = String.class),
              },
              defaultValue = "'utf-8'",
              named = true
          ),
          @Param(
              name = "errors",
              allowedTypes = {
                  @ParamType(type = String.class),
              },
              defaultValue = "'strict'",
              named = true
          ),
          @Param(
              name = "unescape",
              allowedTypes = {
                  @ParamType(type = Boolean.class),
              },
              defaultValue = "True",
              named = true
          )
      },
      useStarlarkThread = true
  )
  public StarlarkBytes encode(String strToEncode, String encoding, String errors,
      Boolean additionalUnescape, StarlarkThread thread) throws EvalException {
    String unescapedString = additionalUnescape ? TextUtil.unescapeJavaString(strToEncode) : strToEncode;
    try {
      return StarlarkBytes.immutableOf(TextUtil.PyCodecs.encode(unescapedString, encoding, errors));
    } catch (EvalException e) {
      throw wrapLookupError(e, "encoding", encoding);
    }
  }

  /** As CPython's codecs.encode/decode, which wrap a LookupError raised by the codec. */
  private static EvalException wrapLookupError(EvalException e, String op, String encoding) {
    if (e.getMessage().startsWith("unknown error handler name ")) {
      return Starlark.errorf(
          "%s with '%s' codec failed (LookupError: %s)", op, encoding, e.getMessage());
    }
    return e;
  }

  @StarlarkMethod(
      name = "decode",
      doc = "decode obj using the codec registered for encoding.\n" +
          "\n" +
          "The default encoding is 'utf-8'.  errors may be given to set a\n" +
          "different error handling scheme.  Default is 'strict' meaning that encoding\n" +
          "errors raise a ValueError.  Other possible values are 'ignore', 'replace'\n" +
          "and 'backslashreplace' as well as any other name registered with\n" +
          "codecs.register_error that can handle ValueErrors.\n",
      parameters = {
          @Param(
              name = "obj",
              allowedTypes = {
                  @ParamType(type = StarlarkBytes.class),
              }
          ),
          @Param(
              name = "encoding",
              allowedTypes = {
                  @ParamType(type = String.class),
              },
              defaultValue = "'utf-8'",
              named = true
          ),
          @Param(
              name = "errors",
              allowedTypes = {
                  @ParamType(type = String.class),
              },
              defaultValue = "'strict'",
              named = true
          )
      }
  )
  public String decode(StarlarkBytes bytesToDecode, String encoding, String errors) throws EvalException {
    if (CodecsModule.UTF8.equals(encoding.toLowerCase())) { // TODO: fix this to be a normal decoder
      return TextUtil.starlarkDecodeUtf8(bytesToDecode.toByteArray());
    }
    try {
      return TextUtil.PyCodecs.decode(bytesToDecode.toByteArray(), encoding, errors);
    } catch (EvalException e) {
      throw wrapLookupError(e, "decoding", encoding);
    }
  }

  @StarlarkMethod(
      name = "utf_8_decode",
      doc = "Decodes UTF-8 bytes as CPython's codecs.utf_8_decode(data, errors, final) does, "
          + "returning a tuple (str, number of bytes consumed). Unless final is true, an "
          + "incomplete sequence at the end of data is left unconsumed rather than being an error.",
      parameters = {
          @Param(name = "data", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
          @Param(
              name = "errors",
              allowedTypes = {@ParamType(type = String.class), @ParamType(type = NoneType.class)},
              defaultValue = "None"),
          @Param(name = "final", allowedTypes = {@ParamType(type = Boolean.class)},
              defaultValue = "False")
      })
  public Tuple utf8Decode(StarlarkBytes data, Object errors, Boolean last) throws EvalException {
    TextUtil.PyCodecs.Decoded d = TextUtil.PyCodecs.utf8Decode(
        data.toByteArray(),
        errors == Starlark.NONE ? TextUtil.CodecHelper.STRICT : (String) errors,
        last);
    return Tuple.pair(d.text, StarlarkInt.of(d.consumed));
  }
}
