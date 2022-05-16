package com.verygood.security.larky.objects;

import com.verygood.security.larky.objects.type.ForwardingLarkyType;
import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.objects.type.LarkyTypeObject;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.Nullable;

/**
 * The staticmethod descriptor.
 */
@StarlarkBuiltin(name = "staticmethod")
public class LarkyStaticMethod implements ForwardingLarkyType, StarlarkCallable {

  public static final LarkyType TYPE = LarkyTypeObject.createBuiltinType("staticmethod");
  public static final LarkyStaticMethod INSTANCE = new LarkyStaticMethod();

  LarkyStaticMethod() {
  }

  public static LarkyStaticMethod getInstance() {
    return INSTANCE;
  }

  @Override
  public LarkyType delegate() {
    return TYPE;
  }

  @Override
  public PyObject __new__(Tuple args, Dict<String, Object> kwargs, StarlarkThread thread) {
    return new LarkyStaticMethodInstance((StarlarkFunction) args.get(0), thread);
  }

  @Override
  public String getName() {
    return TYPE.__name__();
  }

  @Override
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    if (args.size() != 1) {
      throw Starlark.errorf("TypeError: %s expected 1 argument, got %d", getName(), args.size());
    }
    if (!kwargs.isEmpty()) {
      throw Starlark.errorf("TypeError: %s takes no keyword arguments", getName());
    }
    // We divert from python here because we validate that the first argument is a callable up front
    if (!(args.get(0) instanceof StarlarkCallable)) {
      throw Starlark.errorf("TypeError: %s is not callable", args.get(0));
    }

    LarkyPyObject newInst = (LarkyPyObject) this.__new__(args, kwargs, thread);
    LarkyType instanceCls;
    instanceCls = newInst.typeClass();

    if (TYPE == instanceCls) {
      newInst.__init__(args, kwargs);
      return newInst;
    }
      /*
       If __new__() does not return an instance of cls, then the new
       instance’s __init__() method will not be invoked.
      */
    return newInst;
  }

  public static class LarkyStaticMethodInstance extends LarkyFunction {
    private LarkyStaticMethodInstance(StarlarkFunction function, StarlarkThread thread) {
      super(LarkyStaticMethod.TYPE, function, thread);
    }

    @StarlarkMethod(name = "__func__", structField = true)
    public StarlarkCallable wrapped() {
      return this.fget;
    }

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
      // Override this *on purpose* because we cannot depend on the fact that the base
      // class will have this same implementation.
      return Starlark.call(thread, this.fget, args, kwargs);
    }

    @Override
    public Object __get__(Object obj, LarkyType type, @Nullable StarlarkThread thread) {
      return this;
    }
  }
}


