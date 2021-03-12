package com.verygood.security.larky.modules;

import com.google.common.primitives.Bytes;

import com.verygood.security.larky.modules.codecs.TextUtil;
import com.verygood.security.larky.modules.types.LarkyByteArray;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkValue;

import java.util.stream.Collectors;


@StarlarkBuiltin(
    name = "codecs",
    category = "BUILTIN",
    doc = "This module provides codecs")
public class CodecsModule implements StarlarkValue {

  public static final CodecsModule INSTANCE = new CodecsModule();

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
          )
      }
  )
  public StarlarkList<StarlarkInt> encode(String strToEncode, String encoding, String errors) throws EvalException {
    TextUtil textUtil = new TextUtil(TextUtil.unescapeJavaString(strToEncode));
    return StarlarkList.immutableCopyOf(
        Bytes.asList(textUtil.copyBytes()).stream()
            .map(Byte::toUnsignedInt)
            .map(StarlarkInt::of)
            .collect(Collectors.toList()));
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
                  @ParamType(type = LarkyByteArray.class),
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
  public String decode(LarkyByteArray bytesToDecode, String encoding, String errors) {
      return TextUtil.starlarkDecodeUtf8(bytesToDecode.toBytes());
  }
}
