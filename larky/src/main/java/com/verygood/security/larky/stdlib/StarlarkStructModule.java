package com.verygood.security.larky.stdlib;

import com.verygood.security.larky.annot.StarlarkConstructor;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import java.security.NoSuchAlgorithmException;


/** A Starlark structure to deliver information about the system we are running on. */
@StarlarkBuiltin(
    name = "struct",
    category = "BUILTIN",
    doc = "A built-in .")
public class StarlarkStructModule implements StarlarkValue {

  @StarlarkMethod(
      name = "struct",
      doc =
          "Creates an immutable struct using the keyword arguments as attributes. It is used to "
              + "group multiple values together. Example:<br>"
              + "<pre class=\"language-python\">s = struct(x = 2, y = 3)\n"
              + "return s.x + getattr(s, \"y\")  # returns 5</pre>",
      extraKeywords =
          @Param(name = "kwargs", defaultValue = "{}", doc = "Dictionary of arguments."),
      useStarlarkThread = true,
      selfCall = true)
  @StarlarkConstructor
  public String create(Dict<String, Object> kwargs, StarlarkThread thread) throws EvalException, NoSuchAlgorithmException {
    return null;
//    MessageDigest md = MessageDigest.getInstance("MD5");
//    md.update(toHash.getBytes());
//    byte[] digest = md.digest();
//
//    String myHash = DatatypeConverter
//        .printHexBinary(digest)
//        .toUpperCase();
//    return myHash;
  }
}
