package com.verygood.security.larky.objects.descriptor;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import com.verygood.security.larky.objects.LarkyBindable;
import com.verygood.security.larky.objects.LarkyPyObject;
import com.verygood.security.larky.objects.PyObject;
import com.verygood.security.larky.objects.type.LarkyType;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.Nullable;


// https://docs.python.org/3.10/reference/datamodel.html#invoking-descriptors
// Mimic the *_get, *_set, and *_delete descriptors in CPython's descrobject.c
public abstract class LarkyDescriptor extends LarkyPyObject implements LarkyBindable, LarkyNonDataDescriptor {

  /**
   * Name of the object described, e.g. "__add__" or "to_bytes". This is
   * exposed to via {@link #descName()}.
   */
  final protected String name;

  final protected StarlarkCallable fget;

  public LarkyDescriptor(String name, StarlarkCallable fget, LarkyType forType, StarlarkThread thread) {
    super(forType, thread);
    this.name = name;
    this.fget = fget;
  }

  /**
   * The {@code __get__} special method of the Python descriptor protocol,
   * implementing {@code obj.name} or possibly
   * {@code type.name}.
   *
   * Take the following example:
   *
   * <pre language="python">
   * import os
   *
   * class DirectorySize:
   *
   *     def __get__(self, obj, objtype=None):
   *         return len(os.listdir(obj.dirname))
   *
   * class Directory:
   *
   *     size = DirectorySize()              # Descriptor instance
   *
   *     def __init__(self, dirname):
   *         self.dirname = dirname          # Regular instance attribute
   * </pre>
   *
   * The self parameter is size, an instance of DirectorySize.
   * The obj parameter is either g or s, an instance of Directory.
   * It is the obj parameter that lets the __get__() method learn the target directory.
   *
   * The objtype parameter is the class Directory.
   *
   * @param obj  object on which the attribute is sought or {@code null}
   * @param type on which this descriptor was found (may be ignored)
   * @return attribute value, bound object or this attribute
   *
   */
  @StarlarkMethod(
    name = "__get__",
    doc = "call",
    useStarlarkThread = true
  )
  public Object __get__(Object obj, LarkyType type, @Nullable StarlarkThread thread) throws EvalException, InterruptedException {
    if (obj == null) {
      /*
       * obj==null indicates the descriptor was found on the
       * target object itself (or a base).
       */
      return this;
    } else {
      check(obj, thread);
      return Starlark.call(thread, this.fget, Tuple.of(obj), ImmutableMap.of("objtype", type));
    }

  }

  protected PyObject check(Object obj, StarlarkThread thread) throws EvalException {

    if (!(obj instanceof PyObject)) {
      throw Starlark.errorf("'%s' is not of type 'PyObject'", Starlark.repr(obj));
    }

    final LarkyType objType = ((PyObject) obj).typeClass();

    if (!objType.isSubtypeOf(getBoundOwner())) {
      throw Starlark.errorf(
        "descriptor '%s' for '%s' objects doesn't apply to a '%s' object",
        this.name,
        getBoundOwner(),
        Starlark.repr(objType)
      );
    }
    return (PyObject) obj;
  }


  @Override
  public ImmutableCollection<String> getFieldNames() {
    return ImmutableSet.of("__get__");
  }

  public String descName() {
    return name;
  }


  @Override
  public boolean readonly() {
    return true;
  }


  @Override
  public boolean optional() {
    return false;
  }

  static public abstract class LarkyDataDescriptor extends LarkyDescriptor implements com.verygood.security.larky.objects.descriptor.LarkyDataDescriptor {

    final private StarlarkCallable fset;
    final private StarlarkCallable fdel;

    public LarkyDataDescriptor(String name, StarlarkCallable fget, StarlarkCallable fset, StarlarkCallable fdel, LarkyType forType, StarlarkThread thread) {
      super(name, fget, forType, thread);
      this.fset = fset;
      this.fdel = fdel;
    }

    /**
     * The {@code __set__} special method of the Python descriptor protocol, implementing {@code obj.name = value}. In
     * general, {@code obj} must be of type {@link #getBoundOwner()}.
     *
     * @param obj   object on which the attribute is sought
     * @param value to assign (not {@code null})
     * @throws EvalException from the implementation of the deleter
     */
    @StarlarkMethod(
      name = "__set__",
      doc = "call",
      useStarlarkThread = true
    )
    public void __set__(Object obj, Object value, @Nullable StarlarkThread thread) throws EvalException, InterruptedException {
      if (readonly()) {
        throw new EvalException(
          String.format(
            "DataDescriptor (%1$s) is readonly! It does not define a setter for value '%2$s' !",
            obj, value));
      }
      ;

      if (value == null) {
        // This ought to be an error, but allow for CPython idiom.
        __delete__(obj, thread);
      } else {
        check(obj, thread);
        Starlark.call(thread, this.fset, Tuple.of(obj, value), Dict.empty());
      }
    }

    /**
     * The {@code __delete__} special method of the Python descriptor protocol, implementing {@code del obj.name}. In
     * general, {@code obj} must be of type {@link #getBoundOwner()}.
     *
     * @param obj object on which the attribute is sought
     * @throws EvalException from the implementation of the deleter
     * @throws InterruptedException from the implementation of the deleter
     */
    // Compare CPython *_set in descrobject.c with NULL
    @StarlarkMethod(
      name = "__delete__",
      doc = "call",
      useStarlarkThread = true
    )
    public void __delete__(Object obj, @Nullable StarlarkThread thread) throws EvalException, InterruptedException {
      if (optional()) {
        throw new EvalException(String.format(
          "DataDescriptor (%1$s) does not define delete!", obj));
      }
      check(obj, thread);
      Starlark.call(thread, this.fdel, Tuple.of(), Dict.empty());
    }

    @Override
    public boolean readonly() {
      return this.fset == null;
    }


    @Override
    public boolean optional() {
      return this.fdel == null;
    }

    @Override
    public ImmutableCollection<String> getFieldNames() {
      return ImmutableSet.of("__get__", "__set__", "__delete__");
    }
  }
}

