package com.verygood.security.larky.modules.globals;

import com.google.common.primitives.UnsignedBytes;

import com.verygood.security.larky.annot.Library;
import com.verygood.security.larky.annot.StarlarkConstructor;
import com.verygood.security.larky.modules.types.Partial;
import com.verygood.security.larky.modules.types.Property;
import com.verygood.security.larky.modules.types.structs.SimpleStruct;

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

import java.util.stream.Collectors;


/**
 *  A library of Larky values (keyed by name) that are not part of core Starlark but are common
 *  to all Larky star scripts. Examples: struct, json, etc..
 *
 *  Namespaced by _ and should only be accessable via @stdlib/larky:
 *
 *    load("@stdlib/larky", "larky")
 * */
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
  public SimpleStruct struct(Dict<String, Object> kwargs, StarlarkThread thread)  {
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
  public SimpleStruct mutablestruct(Dict<String, Object> kwargs, StarlarkThread thread)  {
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
  public Partial partial(StarlarkFunction function, Tuple args, Dict<String, Object> kwargs)  {
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
          "\n"+
          "  def get_data():\n" +
          "      return {'foo': 1}\n"+
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
  public Property property(StarlarkCallable getter, Object setter, Tuple args, Dict<String, Object> kwargs, StarlarkThread thread)  {
      return Property.builder()
        .thread(thread)
        .fget(getter)
        .fset(setter != Starlark.NONE ? (StarlarkCallable) setter : null)
        .build();
  }

  @StarlarkMethod(name = "_as_bytearray", parameters = {
      @Param(name="str", allowedTypes = {
          @ParamType(type = StarlarkList.class),

      })
  })
  public StarlarkList<StarlarkInt> asByteArray(StarlarkList<StarlarkInt> unsignedByteArray)
      throws EvalException {
    try {
      for (StarlarkInt b1 : unsignedByteArray) {
        // between 0 < x < 255
        UnsignedBytes.checkedCast(b1.toNumber().longValue());
      }
    }
    catch(IllegalArgumentException e) {
      throw new EvalException(String.format("%s, want value in unsigned 8-bit range", e.getMessage()));
    }
    return StarlarkList.immutableCopyOf(unsignedByteArray
        .stream()
        .map(StarlarkInt::toNumber)
        .map(Number::byteValue)
        .map(Byte::toUnsignedInt)
        .map(StarlarkInt::of)
        .collect(Collectors.toList()));
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
       parameters = {@Param(name = "x", doc = "The object to check type of.")})
   public String type(Object object) {
     if (SimpleStruct.class.isAssignableFrom(object.getClass())) {
       return ((SimpleStruct) object).type();
     }
     return Starlark.type(object);
   }

}
