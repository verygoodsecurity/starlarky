package com.verygood.security.larky.nativelib;

import com.verygood.security.larky.annot.Library;
import com.verygood.security.larky.annot.StarlarkConstructor;
import com.verygood.security.larky.core.SimpleStruct;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.StarlarkThread;


/** A collection of global Larky API functions that mimic python's built-ins, to a certain extent.
 *
 *  A library of Starlark values (keyed by name) that are not part of core Starlark but are common
 *  to all Larky star scripts. Examples: struct, json, etc..
 * */
@Library
public final class LarkyGlobals {

  @StarlarkMethod(
      name = "struct",
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
  public SimpleStruct struct(Dict<String, Object> kwargs, StarlarkThread thread)  {
    return SimpleStruct.create(kwargs, thread.getSemantics());
  }

}
