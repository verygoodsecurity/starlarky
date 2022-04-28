package com.verygood.security.larky.objects.type;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.verygood.security.larky.modules.types.LarkyCollection;
import com.verygood.security.larky.objects.DeleteAttribute;
import com.verygood.security.larky.objects.GetAttribute;
import com.verygood.security.larky.objects.PyObject;
import com.verygood.security.larky.objects.SetAttribute;
import com.verygood.security.larky.objects.descriptor.LarkyDataDescriptor;
import com.verygood.security.larky.objects.descriptor.LarkyNonDataDescriptor;
import com.verygood.security.larky.objects.mro.C3;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.NotNull;

import lombok.SneakyThrows;

public interface LarkyType extends PyObject, LarkyCollection, HasBinary {

  @SneakyThrows
  static void setupInheritanceHierarchy(@NotNull LarkyType cls, LarkyType[] parentClasses) {
    cls.setBaseClasses(parentClasses);
    final List<LarkyType> mro;
    mro = C3.calculateMRO(cls);
    cls.setMRO(mro);
    for (LarkyType superclass : mro) {
      superclass.getAllSubclasses().add(cls);
    }
    cls.getAllSubclasses().add(cls);
  }

  Set<LarkyType> getAllSubclasses();

  void setMRO(List<LarkyType> mro);

  void setBaseClasses(LarkyType[] parentClasses);

  @Override
  default String typeName() {
    return "type";
  }

  @Override
  default LarkyType __class__() {
    return LarkyTypeObject.getInstance();
  }

  @Override
  default void __init__(Tuple args, Dict<String, ?> keywords) throws EvalException {
  }

  @Override
  default void repr(Printer printer) {
    printer.append(this.__repr__());
  }

  @Override
  default String __repr__() {
    return String.format("<class '%s'>", typeName());
  }

  @Override
  default void str(Printer printer) {
    printer.append(this.__str__());
  }

  @Override
  default String __str__() {
    return this.__repr__();
  }

  /**
   * Get the {@code __base__} of this type. The {@code __base__} is a type from the MRO, but its choice is determined by
   * implementation details.
   * <p>
   * It is the type earliest on the MRO after the current type, whose implementation contains all the members necessary
   * to implement the current type.
   *
   * @return the type's base
   */
  @StarlarkMethod(name = "__base__", structField = true)
  Object getBase();

  /**
   * @return the bases as a tuple
   */
  @StarlarkMethod(name = "__bases__", structField = true)
  Tuple getBases();

  @StarlarkMethod(name = "__mro__", structField = true)
  Tuple getMRO();

  @StarlarkMethod(name = "__name__", structField = true)
  String __name__();

  /**
   * Look for a name, returning the entry directly from the first dictionary along the MRO containing key {@code name}.
   * This may be a descriptor, but no {@code __get__} takes place on it: the descriptor itself will be returned. This
   * method does not throw an exception if the name is not found, but returns {@code null} like a {@code Map.get}
   *
   * @param name to look up, must be exactly a {@code str}
   * @return dictionary entry or null
   */
  @SneakyThrows
  default Object lookup(String name) {

    // Look in dictionaries of types in MRO
    Sequence<LarkyType> mro = Sequence.cast(getMRO(), LarkyType.class, "LarkyType::lookup");
    // See https://docs.python.org/3/reference/datamodel.html#the-standard-type-hierarchy
    // under Custom Clases
    for (LarkyType base : mro) {
      switch (name) {
        case "__class__":
          return base.__class__();
        case "__dict__":
          return base.__dict__();
        case "__bases__":
          return base.getBases();
        case "__base__":
          return base.getBase();
        case "__mro__":
          return base.getMRO();
        default:
          Object res;
          if ((res = base.getInternalDictUnsafe().get(name)) != null) {
            return res;
          }
      }
    }
    return null;
  }


  /**
   * See <br/>
   *  <a href="https://github.com/python/cpython/blob/6969eaf4682beb01bc95eeb14f5ce6c01312e297/Objects/typeobject.c#L7314-L7704">python/cpython@Objects/typeobject.c#L7314-L7704</a><br/>
   *  and <br/>
   *  <a href="https://stackoverflow.com/a/44994572/133514">Martijn Pieters' answer on StackOverflow</a><br/>
   *
   * @param ref the first type to check before going through the MRO
   * @param name the method name to lookup
   * @return the method if found, or null otherwise
   */
  default PyObject lookupForSuper(LarkyType ref, String name) {
    PyObject result = null;
    //
    Tuple mro = this.getMRO();
    if (mro != null) {
      int i;
      // skip past the start type in the MRO
      for (i = 0; i < mro.size(); i++) {
        if (mro.get(i) == ref)
          break;
      }
      i++;
      // Search for the attribute on the remainder of the MRO
      for (; i < mro.size(); i++) {
        Map<String, Object> dict = ((PyObject) mro.get(i)).getInternalDictUnsafe();
        if (dict != null) {
          Object obj = dict.get(name);
          if (obj != null) {
            result = (PyObject) obj;
            break;
          }
        }
      }
    }
    return result;
  }

  default boolean isSubtypeOf(LarkyType other) {
    boolean result = false;
    if (other == this) {
      result = true;
    } else {
      for (Object superclass : this.getMRO()) {
        if (superclass == other) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  default boolean isInstance(PyObject object) {
    boolean result = false;
    if (object.typeClass() == this) {
      result = true;
    } else {
      for (LarkyType subclass : this.getAllSubclasses()) {
        if (subclass == object.typeClass()) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  /**
   * provides attribute read access on
   * this type object and its metatype. This is very like
   * {@code object.__getattribute__}
   * ({@link PyObject#__getattribute__(String, net.starlark.java.eval.StarlarkThread)}), but the
   * instance is replaced by a type object, and that object's type is
   * a meta-type (which is also a {@code type}).
   * <p>
   * The behavioural difference is that in looking for attributes on a
   * type:
   * <ul>
   * <li>we use {@link #lookup(String)} to search along along the
   * MRO, and</li>
   * <li>if we find a descriptor, we use it.
   * ({@code object.__getattribute__} does not check for descriptors
   * on the instance.)</li>
   * </ul>
   * <p>
   * The following order of precedence applies when looking for the
   * value of an attribute:
   * <ol>
   * <li>a data descriptor from the dictionary of the meta-type</li>
   * <li>a descriptor or value in the dictionary of {@code type}</li>
   * <li>a non-data descriptor or value from dictionary of the meta
   * type</li>
   * </ol>
   *
   * @param name of the attribute
   * @return attribute value
   * @throws EvalException if no such attribute
   */
  @Override
  @StarlarkMethod(
    name = "__getattribute__",
    doc = "" +
      "Slot.op_getattribute has signature Signature.GETATTR and provides attribute read " +
            "access on this type object and its metatype. " +
            "" +
            "This is very like object.__getattribute__ (PyBaseObject.__getattribute__(Object, String)), " +
            "but the instance is replaced by a type object, and that object's type is a " +
            "meta-type (which is also a type).\n" +
      "The behavioral difference is that in looking for attributes on a type:\n" +
      "we use lookup(String) to search along along the MRO, and\n" +
      "if we find a descriptor, we use it. (object.__getattribute__ does not check for descriptors " +
            "on the instance.)\n" +
      "The following order of precedence applies when looking for the value of an attribute:\n" +
      "- a data descriptor from the dictionary of the meta-type\n" +
      "- a descriptor or value in the dictionary of type\n" +
      "- a non-data descriptor or value from dictionary of the meta type\n",
    parameters = {
      @Param(name = "name", allowedTypes = {@ParamType(type = String.class)})
    },
    useStarlarkThread = true
  )
  default Object __getattribute__(String name, StarlarkThread thread)
    throws EvalException {
    return GetAttribute.getForType(this, name, thread);
  }

  @Override
  default void __setattr__(String name, Object value, StarlarkThread thread) throws EvalException {
    SetAttribute.set(this, name, value, thread);
  }

  @Override
  default void __delattr__(String name, StarlarkThread thread) throws EvalException {
    DeleteAttribute.delete(this, name, thread);
  }

  /**
   * If an object defines __set__() or __delete__(), it is considered a data descriptor.
   *
   * Descriptors that only define __get__() are called non-data descriptors
   * (they are often used for methods but other uses are possible).
   */
  default boolean isDataDescriptor() {
    return LarkyDataDescriptor.isDataDescriptor(this);
  }

  default boolean isNonDataDescriptor() {
    return LarkyNonDataDescriptor.isNonDataDescriptor(this);
  }

  /**
   * Will be used to determine if a type is eligible for a special
   * operation / method.
   * @return An immutable set of the type's {@link SpecialMethod}s.
   */
  ImmutableSet<SpecialMethod> getSpecialMethods();

  enum Origin {
    PLACEHOLDER,  // Dummy entry to resolve circular dependencies
    BUILTIN,      // A type provided as part of Starlarky itself.
    LARKY         // A type defined in Larky code
    ;

    public boolean isBuiltin() {
      return this == PLACEHOLDER || this == BUILTIN;
    }
  }

}
