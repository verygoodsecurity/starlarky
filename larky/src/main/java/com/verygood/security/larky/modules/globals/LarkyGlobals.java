package com.verygood.security.larky.modules.globals;

import com.verygood.security.larky.annot.Library;
import com.verygood.security.larky.annot.StarlarkConstructor;
import com.verygood.security.larky.modules.types.LarkyCounter;
import com.verygood.security.larky.modules.types.Partial;
import com.verygood.security.larky.modules.types.Property;
import com.verygood.security.larky.modules.types.structs.SimpleStruct;
import com.verygood.security.larky.objects.LarkyTypeObject;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;


/**
 * A library of Larky values (keyed by name) that are not part of core Starlark but are common to
 * all Larky star scripts. Examples: struct, json, etc..
 *
 * Namespaced by _ and should only be accessible via @stdlib//larky:
 *
 * load("@stdlib//larky", "larky")
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


  static class Sentinel implements StarlarkValue {
    private static Sentinel SINGLETON = null;
//    public static final Set<Integer> SINGLETON = Collections.singleton(1);

    public static synchronized Sentinel getInstance() {
      if (SINGLETON == null) {
        SINGLETON = new Sentinel();
      }
      return SINGLETON;
    }

    @Override
    public void repr(Printer printer) {
      printer.append("<sentinel>");
    }
  }

  @StarlarkMethod(
      name="_sentinel",
      doc="creates a sentinel object"
  )
  public StarlarkValue sentinel() {
    return Sentinel.getInstance();
  }


    @StarlarkMethod(
        name="_Counter",
        doc="creates a simple counter"
    )
    public LarkyCounter counter() {
      return LarkyCounter.newCounter();
    }

  @StarlarkMethod(
      name="_ThreadsafeCounter",
      doc="creates a thread-safe counter"
  )
  public LarkyCounter threadsafeCounter() {
    return LarkyCounter.threadSafeCounter();
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

  @StarlarkMethod(
        name = "_func_name",
        parameters = {@Param(name="obj")},
        useStarlarkThread = true)
  public String funcName(Object obj, StarlarkThread thread) {
    if(obj instanceof StarlarkCallable) {
      return ((StarlarkCallable) obj).getName();
    }
    return Starlark.type(obj);
  }

  @StarlarkMethod(
        name = "_type_class",
        parameters = {@Param(name="obj")},
        useStarlarkThread = true)
  public Object typeClass(Object obj, StarlarkThread thread) throws EvalException {
    return LarkyTypeObject.getInstance().type(obj, Tuple.empty(), Dict.empty(), Dict.empty(), thread);
  }

}
