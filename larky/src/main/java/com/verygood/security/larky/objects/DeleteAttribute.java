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

public abstract class DeleteAttribute {

  private DeleteAttribute() {} // do not inherit from this.

  private static boolean onDataDescriptor(String attr, PyObject obj, StarlarkThread thread) {
    // Look up the name in the type (null if not found).
    Object typeAttr = obj.typeClass().lookup(attr);
    if (typeAttr == null) {
      return false;
    }
    boolean result = false;
    // Found in the type, it might be a descriptor
    try {
      // is typeAttr a data descriptor? if so, call its __delete__.
      if (typeAttr instanceof LarkyDataDescriptor) {
        final LarkyDataDescriptor typeAttrAsDesc = (LarkyDataDescriptor) typeAttr;
        if(typeAttrAsDesc.readonly()) {
          throw Starlark.errorf("%s is readonly.", typeAttrAsDesc);
        }
          typeAttrAsDesc.__delete__(obj, thread);
          result = true;

      } else if (LarkyDataDescriptor.isDataDescriptor(typeAttr)) {
        // ok it's not a data descriptor instance, but it does contain some fields
        // that make it a data descriptor, so, let's see if it also has a __DELETE__
        Object __delete__ = ((PyObject) typeAttr).getField(PyProtocols.__DELETE__, thread);
        Starlark.call(thread, __delete__, Tuple.of(obj, attr), Dict.empty());
        result = true;
      }
    } catch (InterruptedException | EvalException cause) {
      /*
       * If we fail on __set__ invocation, it is an irrecoverable error and
       * this is an invalid data descriptor.
       */
      throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(
        "data descriptor does not define " + PyProtocols.__DELETE__, cause, thread
      );
    }
    return result;
  }

  public static void delete(PyObject obj, String attr, StarlarkThread thread) throws EvalException {
    // check to see if it is a data descriptor so call its __set__.
    if (onDataDescriptor(attr, obj, thread)) {
      return;
    }
    /*
     * If we are here, then there was no data descriptor.
     * Remove the name in the object instance dictionary.
     */
      Object previous = obj.getInternalDictUnsafe().remove(attr);
      if (previous == null) {
        throw Starlark.errorf(obj.getErrorMessageForUnknownField(attr));
      }
  }

  /**
   * Like {@code object.__setattr__(self, name, value)} but for attribute deletion
   * instead of assignment. This should only be implemented if {@code del obj.name} is
   * meaningful for the object.
   *
   * Very similar to {@link SetAttribute#dunderSetAttr(PyObject, String, Object, StarlarkThread, boolean)}.
   *
   * @param obj the {@link PyObject} or derivative to introspect
   * @param name the attribute name to delete
   * @param thread the starlark runtime execution thread
   * @param throwExc if true, throws a {@link StarlarkEvalWrapper.Exc.RuntimeEvalException} exception
   *                 if the attribute was unable to be set. The caller must handle the exception if
   *                 this value is set to true.
   */
  public static void dunderDelAttr(@NotNull PyObject obj, String name, StarlarkThread thread, boolean throwExc) {
    final LarkyType type = obj.typeClass();
    // TODO(mahmoudimus): This needs to also support if there's a Java class for `__DELATTR__`
    final Object __delattr__ = type.getInternalDictUnsafe().getOrDefault(PyProtocols.__DELATTR__, null);
    if (!StarlarkUtil.isNullOrNoneOrUnbound(__delattr__)) {
      try {
        Starlark.call(thread, __delattr__, Tuple.of(obj, name), Dict.empty());
      } catch (EvalException | InterruptedException exc) {
        if (throwExc) {
          throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(exc, thread);
        }
      }
    }
  }

}
