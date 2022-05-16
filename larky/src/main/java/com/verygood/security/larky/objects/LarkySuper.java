package com.verygood.security.larky.objects;

import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.objects.descriptor.LarkyNonDataDescriptor;
import com.verygood.security.larky.objects.type.ForwardingLarkyType;
import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.objects.type.LarkyTypeObject;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.Nullable;


@StarlarkBuiltin(name = "super")
public class LarkySuper implements LarkyNonDataDescriptor, ForwardingLarkyType, StarlarkCallable {

  public static final LarkyType TYPE = LarkyTypeObject.createBuiltinType("super");

  public static final LarkySuper INSTANCE = new LarkySuper();

  public static LarkySuper getInstance() {
    return INSTANCE;
  }

  private StarlarkThread thread;
  LarkyType objType;
  PyObject obj;
  LarkyType superType;

  LarkySuper() {
  }

  @Override
  public LarkyType delegate() {
    return TYPE;
  }

  @Override
  public String getName() {
    return "super";
  }

  @StarlarkMethod(name = "__thisclass__", structField = true)
  public LarkyType superType() {
    return superType;
  }

  @StarlarkMethod(name = "__self__", structField = true)
  public PyObject obj() {
    return obj;
  }

  @StarlarkMethod(name = "__self_class__", structField = true)
  public LarkyType objType() {
    return objType;
  }

  /**
    * The attribute may not be set or deleted.
    *
    * @return true if the attribute may not be set or deleted
    */
   @Override
   public boolean readonly() {
     return true;
   }

   /**
    * The attribute may be deleted.
    *
    * @return true if the attribute may be deleted.
    */
   @Override
   public boolean optional() {
     return false;
   }
  @Override
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    LarkyType type;
    Object obj = null;
    LarkyType objType = null;
    final int arg_size = args.size();
    if (!kwargs.isEmpty() || arg_size != 2) {
      throw Starlark.errorf("%s\n%s\n%s",
        "super: expecting two arguments.",
        "super(type, obj) -> bound super object; requires isinstance(obj, type)",
        "super(type, type2) -> bound super object; requires issubclass(type2, type)"
      );
    }

    if (!(args.get(0) instanceof LarkyType)) {
      throw Starlark.errorf("super: argument 1 must be type");
    }
    type = (LarkyType) args.get(0);

    if (args.get(1) != Starlark.NONE) {
      obj = args.get(1);
    }

    if (obj != null) {
      if (obj instanceof PyObject) {
        objType = supercheck(type, (PyObject) obj);
      } else {
        throw Starlark.errorf("was expecting obj to of type PyObject, but got %s", Starlark.type(obj));
      }
    }

    LarkySuper newsuper = new LarkySuper();
    newsuper.superType = type;
    newsuper.obj = (obj != null) ? (PyObject) obj : null;
    newsuper.objType = objType;
    newsuper.thread = thread;
    return newsuper;
  }

  /**
   * Port of: https://github.com/limweb/flex-pypy/blob/master/flex-backend/pypy/module/__builtin__/app_descriptor.py
   * @param name  field name
   * @param thread starlark thread
   */
  @Override
  public Object __getattribute__(String name, StarlarkThread thread) throws EvalException {
    if (objType != null && !name.equals(PyProtocols.__CLASS__)) {
      // __class__ should always come from this object, not
      // the represented MRO.
      Object descr = objType.lookupForSuper(superType, name);
      // if it is a descriptor object, bind it
      if (LarkyNonDataDescriptor.isNonDataDescriptor(descr)) {
        try {
          return ((LarkyNonDataDescriptor) descr).__get__(objType == obj ? null : obj, objType, getCurrentThread());
        } catch (InterruptedException e) {
          throw new EvalException(e);
        }
      }
    }
    return ForwardingLarkyType.super.__getattribute__(name, thread);
  }

  /**
   * Check that a super() call makes sense.  Return a type object.
   *
   * obj can be a new-style class, or an instance of one:
   *
   * - If it is a class, it must be a subclass of 'type'.  This case is used for class methods; the return value is
   * obj.
   *
   * - If it is an instance, it must be an instance of 'type'.  This is the normal case; the return value is
   * obj.__class__.
   *
   * But... when obj is an instance, we want to allow for the case where objType is not a subclass of type, but
   * obj.__class__ is!  This will allow using super() with a proxy for obj.
   *
   * @param type the LarkyType superType associated with the super
   * @param obj  the PyObject obj associated with the super
   * @return a LarkyType superType
   */
  private LarkyType supercheck(LarkyType type, PyObject obj) throws EvalException {
    // Check for first bullet above (special case)
    if (obj instanceof LarkyType && ((LarkyType) obj).isSubtypeOf(type)) {
      return (LarkyType) obj;
    }

    // Normal case
    LarkyType objType = obj.typeClass();
    if (objType.isSubtypeOf(type)) {
      return objType;
    } else {
      // Try the slow way
      Object classAttr = obj.getField("__class__");
      if (classAttr instanceof LarkyType) {
        if (((LarkyType) classAttr).isSubtypeOf(type)) {
          return (LarkyType) classAttr;
        }
      }
    }
    throw new EvalException("super(type, obj): obj must be an instance or subtype of type");
  }

  @Override
  public Object __get__(Object obj, LarkyType type, @Nullable StarlarkThread thread) throws EvalException, InterruptedException {
    if (obj == null || obj == Starlark.NONE || this.obj != null) {
      return this;
    }
    if (typeClass() != TYPE && typeClass() != null) {
      // If an instance of a (strict) subclass of super, call its type
      StarlarkThread newThread = thread == null ? ((PyObject) obj).getCurrentThread() : thread;
      return ((StarlarkCallable) typeClass()).call(newThread, Tuple.of(type, obj), Dict.empty());
    } else {
      // Inline the common case
      LarkyType objType = supercheck(this.superType, (PyObject) obj);
      LarkySuper newsuper = new LarkySuper();
      newsuper.superType = this.superType;
      newsuper.obj = (PyObject) obj;
      newsuper.objType = objType;
      newsuper.thread = thread;
      return newsuper;
    }

  }

  @Override
  public StarlarkThread getCurrentThread() {
    return thread;
  }
}