package com.verygood.security.larky.objects;

import java.util.function.Function;
import java.util.stream.Stream;

import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.objects.descriptor.LarkyDataDescriptor;
import com.verygood.security.larky.objects.descriptor.LarkyNonDataDescriptor;
import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

public abstract class GetAttribute {

  private GetAttribute() {} // do not inherit from this.

  private static Object onDataDescriptor(String attr, PyObject obj, LarkyType objType, StarlarkThread thread) {
    Object typeAttr = objType.lookup(attr);
    Object result = null;
    if (typeAttr != null) {
      // Found in the type, it might be a descriptor
      try {
        // is typeAttr a data descriptor? if so, call its __get__.
        if (typeAttr instanceof LarkyDataDescriptor) {
          result = ((LarkyDataDescriptor) typeAttr).__get__(obj, objType, thread);
        } else if (LarkyDataDescriptor.isDataDescriptor(typeAttr)) {
          // ok it's not a data descriptor instance, but it does contain some fields
          // that make it a data descriptor, so, let's see if it also has a __get__
          Object __get__ = ((PyObject) typeAttr).getField(PyProtocols.__GET__, thread);
          result = Starlark.call(thread, __get__, Tuple.of(obj, objType), Dict.empty());
        }
      } catch (InterruptedException | EvalException cause) {
        /*
         * If we fail on __get__ invocation, it is an irrecoverable error and
         * this is an invalid data descriptor.
         */
        throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(
          "data descriptor does not define " + PyProtocols.__GET__, cause, thread
        );
      }
    }
    return result;
  }

  private static Object onNonDataDescriptor(String attr, PyObject obj, LarkyType objType, StarlarkThread thread, boolean throwExc) {
    Object typeAttr = objType.lookup(attr);
    Object result = null;
    if (typeAttr != null) {
      // Found the attr in the type, but it might be a descriptor.
      try {
        // typeAttr may be a non-data descriptor: call __get__ if it exists.
        if (typeAttr instanceof LarkyNonDataDescriptor) {
          result = ((LarkyNonDataDescriptor) typeAttr).__get__(obj, objType, thread);
        } else if (LarkyNonDataDescriptor.isNonDataDescriptor(typeAttr)) {
          // ok it's not a non data descriptor instance, but it does contain some fields
          // that make it a non data descriptor, so, let's see if it also has a __get__
          Object __get__ = ((PyObject) typeAttr).getField(PyProtocols.__GET__, thread);
          result = Starlark.call(thread, __get__, Tuple.of(obj, objType), Dict.empty());
        }
      } catch (InterruptedException | EvalException cause) {
        // technically, we should continue on if the result of a non descriptor is None, but
        // we will deviate from Python here to bubble up this exception.
        if(throwExc) {
          throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(
            "non-data descriptor does not define " + PyProtocols.__GET__, cause, thread
          );
        }
        result = typeAttr;
      }
    }
    return result;
  }

  /**
   * {@code __getattribute___} provides attribute read access on the object
   * and its type.
   *
   * The default instance {@code __getattribute__} implements dictionary
   * look-up on the type and the instance.
   *
   * It is the starting point for activating the descriptor protocol.
   *
   * The following order of precedence applies when looking for the value
   * of an attribute:
   * <ol>
   * <li>a data descriptor from the dictionary of the type</li>
   * <li>a value in the instance dictionary of {@code obj}</li>
   * <li>a non-data descriptor from dictionary of the type</li>
   * <li>a value from the dictionary of the type</li>
   * </ol>
   *
   * @param attr the target of the get
   * @param thread starlark thread
   * @return attribute value
   * @throws EvalException if no such attribute
   */
  public static Object get(PyObject obj, String attr, StarlarkThread thread) throws EvalException {
    // important to note this: https://docs.python.org/3/reference/datamodel.html#special-method-lookup
    // we should bypass the instance dictionary if it's a SpecialMethod
    Object value;
    if ((value = StarlarkEvalWrapper.getAttrFromMethodAnnotations(thread, obj, attr)) != null) {
      return value;
    }
    LarkyType objType = obj.typeClass();
    // Look up the name in the type (null if not found).
    Object typeAttr = objType.lookup(attr);
    if ((value = onDataDescriptor(attr, obj, objType, thread)) != null) {
      return value;
    }
    /*
     * If we are here, then typeAttr is either:
     *  - the value from the type, or
     *  - a non-data descriptor, or
     *  - null if the attribute was not found.
     *
     * Is {@code attr} in the current object instance dictionary?
     */
    if ((value = obj.getInternalDictUnsafe().getOrDefault(attr, null)) != null) {
      return value;
    }
    /*
     * Not in the object instance dictionary.
     * What's left now is to check to see
     * - typeAttr is not null
     * - it is a non-data descriptor
     * - lookup on type finds the value
     */
    if ((value = onNonDataDescriptor(attr, obj, objType, thread, true)) != null) {
      return value;
    }
    /*
     * The attribute obtained from the type is:
     * - not null
     * - not a descriptor
     * Therefore, it is the return value!
     */
    if (typeAttr != null) {
      return typeAttr;
    }
    // The chain of checks failed, no attribute exists.
    throw Starlark.errorf("AttributeError: %s has no attribute '%s'", objType, attr);
  }

  public static <Super, Sub extends Super> Function<Super, Stream<Sub>> filterType(Class<Sub> clz) {
    return obj -> clz.isInstance(obj) ? Stream.of(clz.cast(obj)) : Stream.empty();
  }

  public static <T, R> Function<T, Stream<R>> select(Class<R> clazz) {
    return e -> clazz.isInstance(e) ? Stream.of(clazz.cast(e)) : Stream.empty();
  }

  public static Object getForType(LarkyType obj, String attr, StarlarkThread thread) throws EvalException {
    Object value;
    if ((value = StarlarkEvalWrapper.getAttrFromMethodAnnotations(thread, obj, attr)) != null) {
     return value;
    }
    LarkyType metaClsType = obj.typeClass();
    // Look up the name in the type (null if not found).
    if ((value = onDataDescriptor(attr, obj, metaClsType, thread)) != null) {
     return value;
    }
    /*
    * If we are here, then attr is either:
    *  - the value from the type, or
    *  - a non-data descriptor, or
    *  - null if the attribute was not found.
    *
    * Is {@code attr} in the current type's instance dictionary *AND* is it a descriptor?
    */
    if ((value = onNonDataDescriptor(attr, null, obj, thread, false)) != null) {
     return value;
    }
    /*
    * Ok, attr is *NOT* a descriptor, but could still be:
    *  - the value from the type,
    *  - null if the attribute was not found.
    *
    * Is {@code attr} in the current type's instance dictionary?
    */
    if((value = obj.lookup(attr)) != null) {
      // we found it! return
      return value;
    }
    /*
    * Not in the current type's instance dictionary.
    * What's left now is to check to see if attr on the metatype:
    * - is not null
    * - it is a non-data descriptor
    * - lookup on type finds the value
    */
    if ((value = onNonDataDescriptor(attr, obj, metaClsType, thread, true)) != null) {
     return value;
    }
    /*
    * The attribute obtained from the meta-type is:
    * - not null
    * - not a descriptor
    * Therefore, it is the return value!
    */
    if ((value = metaClsType.lookup(attr)) != null) {
     return value;
    }
    // The chain of checks failed, no attribute exists.
    throw Starlark.errorf("AttributeError: %s has no attribute '%s'", metaClsType, attr);
  }


  /**
   * {@code object.__getattr__(self, name)} is an object method that is called if the object’s properties are not found.
   *
   * This method should return the property value or throw AttributeError.
   *
   * Note that if the object property can be found through the normal mechanism, it will not be called.__getattr__method.
   * @param obj the {@link PyObject} or derivative to introspect
   * @param name the target of the get
   * @param thread the starlark runtime execution thread
   * @param throwExc if true, throws a {@link StarlarkEvalWrapper.Exc.RuntimeEvalException} exception
   *                 if the attribute was not found. The caller must handle the exception if this value is
   *                 set to true.
   *
   *                 This should really be <code>false</code> since {@link PyObject}
   *                 extends {@link net.starlark.java.eval.Structure}, which contains machinery such that if
   *                 {@link net.starlark.java.eval.Structure#getValue(String)} returns <code>null</code>, it will
   *                 automatically invoke {@link net.starlark.java.eval.Structure#getErrorMessageForUnknownField(String)}
   *                 which will properly throw the exception.
   *
   * @return If {@link PyProtocols#__GETATTR__} exists and is callable, returns the result of its invocation.
   *         Otherwise, returns null.
   */
  public static Object dunderGetAttr(PyObject obj, String name, StarlarkThread thread, boolean throwExc) {
    Object result = null;
    final LarkyType type = obj.typeClass();
    // TODO(mahmoudimus): This needs to also support if there's a Java class for `__GETATTR__`
    final Object getattr_ = type.getInternalDictUnsafe().getOrDefault(PyProtocols.__GETATTR__, null);
    if (!StarlarkUtil.isNullOrNoneOrUnbound(getattr_)) {
      try {
        result = Starlark.call(thread, getattr_, Tuple.of(obj, name), Dict.empty());
      } catch (EvalException | InterruptedException exc) {
        if (throwExc) {
          throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(exc, thread);
        }
      }
    }

    return result;
  }

}
