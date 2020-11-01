package com.verygood.security.larky.nativelib;

import com.verygood.security.larky.annot.Library;
import com.verygood.security.larky.annot.StarlarkConstructor;
import com.verygood.security.larky.core.CallableMutableStruct;
import com.verygood.security.larky.core.SimpleStruct;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.StarlarkFunction;
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
    return SimpleStruct.immutable(kwargs, thread.getSemantics());
  }

  @StarlarkMethod(
      name = "mutablestruct",
      doc = "Just like struct, but creates an mutable struct using the keyword arguments as attributes",
      extraKeywords =
          @Param(name = "kwargs", defaultValue = "{}", doc = "Dictionary of arguments."),
      useStarlarkThread = true
    )
  @StarlarkConstructor
  public SimpleStruct mutablestruct(Dict<String, Object> kwargs, StarlarkThread thread)  {
    return SimpleStruct.mutable(kwargs, thread.getSemantics());
  }

  @StarlarkMethod(
      name = "callablestruct",
      doc = "Just like struct, but creates an callable struct using a function and its keyword arguments as its attributes",
      parameters = {
          @Param(
              name = "function",
              doc = "The function to invoke when the struct is called"
          ),
      },
      extraKeywords =
          @Param(name = "kwargs", defaultValue = "{}", doc = "Dictionary of arguments."),
      useStarlarkThread = true
    )
  @StarlarkConstructor
  public SimpleStruct callablestruct(StarlarkFunction function, Dict<String, Object> kwargs, StarlarkThread thread)  {
    return CallableMutableStruct.create(thread, function, kwargs);
  }
}
