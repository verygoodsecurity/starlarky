package com.verygood.security.larky.objects;

import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.objects.descriptor.LarkyDataDescriptor;
import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.NotNull;

public abstract class SetAttribute {

  private SetAttribute() {} // do not inherit from this.

  private static boolean onDataDescriptor(String attr, PyObject obj, Object value, StarlarkThread thread) {
    // Look up the name in the type (null if not found).
    Object typeAttr = obj.typeClass().lookup(attr);
    if (typeAttr == null) {
      return false;
    }
    boolean result = false;
    // Found in the type, it might be a descriptor
    try {
      // is typeAttr a data descriptor? if so, call its __set__.
      if (typeAttr instanceof LarkyDataDescriptor) {
        final LarkyDataDescriptor typeAttrAsDesc = (LarkyDataDescriptor) typeAttr;
        if(typeAttrAsDesc.readonly()) {
          throw Starlark.errorf("%s is readonly.", typeAttrAsDesc);
        }
          typeAttrAsDesc.__set__(obj, value, thread);
          result = true;

      } else if (LarkyDataDescriptor.isDataDescriptor(typeAttr)) {
        // ok it's not a data descriptor instance, but it does contain some fields
        // that make it a data descriptor, so, let's see if it also has a __set__
        Object __set__ = ((PyObject) typeAttr).getField(PyProtocols.__SET__, thread);
        Starlark.call(thread, __set__, Tuple.of(obj, value), Dict.empty());
        result = true;
      }
    } catch (InterruptedException | EvalException cause) {
      /*
       * If we fail on __set__ invocation, it is an irrecoverable error and
       * this is an invalid data descriptor.
       */
      throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(
        "data descriptor does not define " + PyProtocols.__SET__, cause, thread
      );
    }
    return result;
  }

  public static void set(PyObject obj, String attr, Object value, StarlarkThread thread) throws EvalException {
    if (value == null) {
      // In CPython, x.foo = NULL means delete, but this will not actually be invoked
      // Do this to help python semantics if we want to port C modules.
      DeleteAttribute.delete(obj, attr, thread);
    }

    // check to see if it is a data descriptor so call its __set__.
    if (onDataDescriptor(attr, obj, value, thread)) {
      return;
    }
    /*
     * If we are here, then there was no data descriptor.
     * Put the value in the object internal dictionary.
     */
    obj.getInternalDictUnsafe().put(attr, value);
  }

  /**
   * {@code object.__setattr__(self, name, value)} is called when an
   * attribute assignment is attempted. This is called instead of the
   * normal mechanism (i.e. store the value in the instance dictionary).
   *
   * If __setattr__() wants to assign to an instance attribute, it should
   * call the base class method with the same name, for example,
   * object.__setattr__(self, name, value).
   *
   * @param obj the {@link PyObject} or derivative to introspect
   * @param name the attribute name
   * @param value the value to be assigned to it.
   * @param thread the starlark runtime execution thread
   * @param throwExc if true, throws a {@link StarlarkEvalWrapper.Exc.RuntimeEvalException} exception
   *                 if the attribute was unable to be set. The caller must handle the exception if
   *                 this value is set to true.
   */
  public static void dunderSetAttr(@NotNull PyObject obj, String name, Object value, StarlarkThread thread, boolean throwExc) {
    final LarkyType type = obj.typeClass();
    // TODO(mahmoudimus): This needs to also support if there's a Java class for `__SETATTR__`
    final Object setattr_ = type.getInternalDictUnsafe().getOrDefault(PyProtocols.__SETATTR__, null);
    if (!StarlarkUtil.isNullOrNoneOrUnbound(setattr_)) {
      try {
        Starlark.call(thread, setattr_, Tuple.of(obj, name, value), Dict.empty());
      } catch (EvalException | InterruptedException exc) {
        if (throwExc) {
          throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(exc, thread);
        }
      }
    }
  }

}
