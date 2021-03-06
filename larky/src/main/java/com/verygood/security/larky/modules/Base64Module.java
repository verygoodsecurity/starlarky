package com.verygood.security.larky.modules;

import java.util.Base64;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;


@StarlarkBuiltin(
    name = "base64j",
    category = "BUILTIN",
    doc = "This module implements a common interface to many different secure hash and message digest algorithms.")
public class Base64Module implements StarlarkValue {

  public static final Base64Module INSTANCE = new Base64Module();

  @StarlarkMethod(
      name = "b64encode",
      doc = "base64 encode",
      parameters = {
        @Param(name = "s", doc = "String to base64 encode"),
        @Param(name = "altchars", doc = "Optional altchars should be a byte string of length 2 which specifies an alternative alphabet for the '+' and '/' characters.  This allows an application to e.g. generate url or filesystem safe Base64 strings.")
      },
      useStarlarkThread = true)
  public String b64encode(String s, Object altchars, StarlarkThread thread) {
    //noinspection UnstableApiUsage
    return Base64.getEncoder().encodeToString(s.getBytes());
  }


  @StarlarkMethod(
      name = "b64decode",
      doc = "base64 encode",
      parameters = {
        @Param(name = "s", doc = "Decode the Base64 encoded bytes-like object or ASCII string s."),
        @Param(name = "altchars", doc = "Optional altchars should be a byte string of length 2 which specifies an alternative alphabet for the '+' and '/' characters.  This allows an application to e.g. generate url or filesystem safe Base64 strings."),
        @Param(name = "validate", doc = "Optional altchars should be a byte string of length 2 which specifies an alternative alphabet for the '+' and '/' characters.  This allows an application to e.g. generate url or filesystem safe Base64 strings.")
      },
      useStarlarkThread = true)
  public String b64decode(String s, Object altchars, boolean validate, StarlarkThread thread) {
    //noinspection UnstableApiUsage
    // System.out.println("got " + s);
    byte[] decodedBytes = Base64.getDecoder().decode(s);
    String decodedString = new String(decodedBytes);
    // System.out.println("made " + decodedString);
    return decodedString;
  }

}
