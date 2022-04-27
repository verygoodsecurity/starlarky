package com.verygood.security.larky.objects.type;

import java.util.List;
import java.util.Set;

import com.verygood.security.larky.objects.PyObject;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.NotNull;

import lombok.SneakyThrows;

public interface LarkyType extends PyObject {


  static void setupInheritanceHierarchy(@NotNull LarkyType cls, LarkyType[] parentClasses) {
    cls.setBaseClasses(parentClasses);
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
    return this;
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
