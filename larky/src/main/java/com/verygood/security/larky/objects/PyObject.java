package com.verygood.security.larky.objects;

import java.util.Map;

import com.verygood.security.larky.modules.types.LarkyObject;
import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.Nullable;

import lombok.SneakyThrows;

/**
 * An attempt to faithfully implement https://docs.python.org/3/reference/datamodel.html
 */
public interface PyObject extends LarkyObject {

  @SneakyThrows
  @StarlarkMethod(name = "__dict__", structField = true)
  default Dict<?, ?> __dict__() {
    return Dict.cast(
      StarlarkUtil.valueToStarlark(this.getInternalDictUnsafe()),
      Object.class,
      Object.class,
      "this.__dict__()"
    );
  }

  /**
   * @return The internal dictionary of this object
   */
  Map<String, Object> getInternalDictUnsafe();

  LarkyType.Origin getOrigin();

  /**
   * Return the Python type for this object.
   */
  LarkyType typeClass();

  @StarlarkMethod(name = "__class__", structField = true)
  LarkyType __class__();

  /**
   * Return the Python type name for this object.
   */
  @Override
  String typeName();

  /*
    Python interface compatibility

    Section 3.3.1 - Basic customization
   */

  /**
   * Called to create a new instance of class cls. __new__() is a static
   * method (special-cased so you need not declare it as such) that takes
   * the class of which an instance was requested as its first argument.
   * The remaining arguments are those passed to the object constructor
   * expression (the call to the class). The return value of __new__()
   * should be the new object instance (usually an instance of cls).
   *
   * Typical implementations create a new instance of the class by
   * invoking the superclass’s __new__() method using
   * super().__new__(cls[, ...]) with appropriate arguments and
   * then modifying the newly-created instance as necessary before returning it.
   *
   * If __new__() is invoked during object construction and it
   * returns an instance of cls, then the new instance’s __init__()
   * method will be invoked like __init__(self[, ...]), where self
   * is the new instance and the remaining arguments are the same as
   * were passed to the object constructor.
   *
   * If __new__() does not return an instance of cls, then the new
   * instance’s __init__() method will not be invoked.
   *
   * __new__() is intended mainly to allow subclasses of immutable
   * types (like int, str, or tuple) to customize instance creation.
   *
   * It is also commonly overridden in custom metaclasses in order to
   * customize class creation.
   * @param args
   * @param kwargs
   * @param thread
   * @return PyObject representing a new instance
   */
  PyObject __new__(Tuple args, Dict<String, Object> kwargs, StarlarkThread thread);

  void __init__(Tuple args, Dict<String, ?> keywords) throws EvalException;

  String __repr__();

  String __str__();

  default boolean isBuiltin() {
    return getOrigin().isBuiltin();
  }

  /**
   * 3.3.2. Customizing attribute access
   */

  @Nullable
  @Override
  default Object getField(String name, @Nullable StarlarkThread thread) {
    Object getattr_;
    try {
      getattr_ = this.__getattribute__(name, thread);
    } catch (EvalException ex) {
      getattr_ = null;
    }
    return getattr_;
  }


  @StarlarkMethod(
    name = "__getattribute__",
    doc = "" +
          "Called unconditionally to implement attribute accesses for" +
          " instances of the class. If the class also defines " +
          "<pre>__getattr__()</pre>, the latter will not be called unless" +
          " <pre>__getattribute__()</pre> either calls it explicitly or raises " +
          "an <pre>AttributeError</pre>. " +
          "" +
          "This method should return the (computed) attribute value or" +
          " raise an <pre>AttributeError exception</pre>. In order to avoid " +
          "infinite recursion in this method, its implementation " +
          "should always call the base class method with the same name" +
          " to access any attributes it needs, for example, " +
          "<pre>object.__getattribute__(self, name).</pre>",
    parameters = {
      @Param(name = "name", allowedTypes = {@ParamType(type = String.class)})
    },
    useStarlarkThread = true
  )
  Object __getattribute__(String name, StarlarkThread thread)
    throws EvalException;

  @Override
  default void setField(String field, Object value) throws EvalException {
    __setattr__(field, value, getCurrentThread());
  }

  default void setField(String field, Object value, StarlarkThread thread) throws EvalException {
    __setattr__(field, value, thread);
  }

  @StarlarkMethod(
    name = "__setattr__",
    doc = "Called when an attribute assignment is attempted. This is" +
            " called instead of the normal mechanism (i.e. store the" +
            " value in the instance dictionary). name is the " +
            "attribute name, value is the value to be assigned to it." +
            "\n" +
            "If __setattr__() wants to assign to an instance " +
            "attribute, it should call the base class method with the " +
            "same name, for example, " +
            "object.__setattr__(self, name, value).",
    parameters = {
      @Param(name = "name", allowedTypes = {@ParamType(type = String.class)}),
      @Param(name = "value")
    },
    useStarlarkThread = true
  )
  void __setattr__(String name, Object value, StarlarkThread thread) throws EvalException;

  @StarlarkMethod(
    name = "__delattr__",
    doc = "" +
            "Like __setattr__() but for attribute deletion instead" +
            " of assignment. This should only be implemented if del" +
            " obj.name is meaningful for the object.",
    parameters = {
      @Param(name = "name", allowedTypes = {@ParamType(type = String.class)})
    },
    useStarlarkThread = true
  )
  void __delattr__(String name, StarlarkThread thread) throws EvalException;

}
