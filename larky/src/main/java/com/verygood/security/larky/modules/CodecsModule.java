package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.codecs.TextUtil;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;


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
    CharsetEncoder encoder = Charset.forName(encoding)
        .newEncoder()
        .onMalformedInput(TextUtil.CodecHelper.convertCodingErrorAction(errors))
        .onUnmappableCharacter(TextUtil.CodecHelper.convertCodingErrorAction(errors));
    try {
      String unescapedString = additionalUnescape ? TextUtil.unescapeJavaString(strToEncode) : strToEncode;
      ByteBuffer encoded = encoder.encode(CharBuffer.wrap(unescapedString));
      return StarlarkBytes.copyOf(thread.mutability(), encoded);
//      return (StarlarkBytes) StarlarkBytes.builder(thread).setSequence(encoded).build();
    } catch (CharacterCodingException e) {
      throw Starlark.errorf(e.getMessage());
    }
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
    CharsetDecoder decoder = Charset.forName(encoding)
        .newDecoder()
        .onMalformedInput(TextUtil.CodecHelper.convertCodingErrorAction(errors))
        .onUnmappableCharacter(TextUtil.CodecHelper.convertCodingErrorAction(errors));
    CharBuffer decoded;
    try {
      decoded = decoder.decode(ByteBuffer.wrap(bytesToDecode.toByteArray()));
    } catch (CharacterCodingException e) {
      throw Starlark.errorf(e.getMessage());
    }
    return decoded.toString();
  }
}
