package com.verygood.security.larky.nativelib;

import com.verygood.security.larky.annot.Library;
import com.verygood.security.larky.annot.StarlarkConstructor;
import com.verygood.security.larky.stdtypes.structs.CallableMutableStruct;
import com.verygood.security.larky.stdtypes.structs.SimpleStruct;

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
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;


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
          )
      },
      extraPositionals = @Param(name = "args"),
      extraKeywords =
          @Param(name = "kwargs", defaultValue = "{}", doc = "Dictionary of arguments."),
      useStarlarkThread = true
    )
  public SimpleStruct callablestruct(StarlarkFunction function, Tuple<Object> args, Dict<String, Object> kwargs, StarlarkThread thread)  {
    return CallableMutableStruct.create(thread, function, args, kwargs);
  }

  //b=struct(c=descriptor(callablestruct(_get_data, self)))
  //b.c == _get_data(self)
  @StarlarkMethod(
      name = "descriptor",
      doc = "Just like struct, but creates an descriptor-like struct using a function and " +
          "its keyword arguments as its attributes. \n" +
          "You can invoke a descriptor using the . instead of (). " +
          "For example: \n" +
          "\n"+
          "  def get_data():\n" +
          "      return {'foo': 1}\n"+
          "  c = struct(data=descriptor(get_data))\n" +
          "  assert c.data == get_data()"
      ,
      parameters = {
          @Param(
              name = "function",
              doc = "The function to invoke when the struct is called"
          )
      },
      extraPositionals = @Param(name = "args"),
      extraKeywords =
          @Param(name = "kwargs", defaultValue = "{}", doc = "Dictionary of arguments."),
      useStarlarkThread = true
    )
  public LarkyDescriptor descriptor(StarlarkCallable function, Tuple<Object> args, Dict<String, Object> kwargs, StarlarkThread thread)  {
    return LarkyDescriptor.builder()
        .callable(function)
        .thread(thread).build();
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
              named = true
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
                  @ParamType(type = String.class),
                  @ParamType(type = NoneType.class),
              },
              defaultValue = "None"
          )
      }
    )
  public StarlarkInt pow(StarlarkInt base, StarlarkInt exp, Object mod) throws EvalException {
    if(Starlark.isNullOrNone(mod)) {
      return StarlarkInt.of(
          base.toBigInteger()
              .pow(exp.toInt("exp " + exp.toString() + " is too big."))
      );
    }
    return StarlarkInt.of(
      base.toBigInteger()
          .modPow(exp.toBigInteger(), ((StarlarkInt) mod).toBigInteger())
    );
  }
}
