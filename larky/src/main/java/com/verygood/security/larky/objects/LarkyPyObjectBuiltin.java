package com.verygood.security.larky.objects;

import java.util.HashSet;
import java.util.Set;

import com.verygood.security.larky.objects.type.ForwardingLarkyType;
import com.verygood.security.larky.objects.type.LarkyType;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;


class LarkyPyObjectBuiltin extends LarkyPyObject implements ForwardingLarkyType, StarlarkCallable {

  private final Set<LarkyType> allSubclasses = new HashSet<>();

  private LarkyPyObjectBuiltin(LarkyTypeObject klass, StarlarkThread thread) {
    super(klass, thread);
  }

  @Override
  public String __repr__() {
    return
      String.format("<class '%s'>",
        __name__()
      );
  }

  @Override
  public LarkyType delegate() {
    return this;
  }

  @Override
  public LarkyType __class__() {
    return LarkyTypeObject.getInstance();
  }

  @Override
  @SuppressWarnings("scwjava_CollectionsDonotexposeinternalSets")
  public Set<LarkyType> getAllSubclasses() {
    return allSubclasses;
  }


  @Override
  public String __name__() {
    return "object";
  }

  @Override
  public Object getBase() {
    return Starlark.NONE;
  }

  @Override
  public Tuple getBases() {
    return Tuple.empty();
  }

  @Override
  public Tuple getMRO() {
    return Tuple.of(this);
  }

  @Override
  public String typeName() {
    return "object";
  }

  @Override
  public String getName() {
    return this.__repr__();
  }

  @Override
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    if (!args.isEmpty() || !kwargs.isEmpty()) {
      throw Starlark.errorf("object() takes no arguments");
    }
    final LarkyPyObject newInstance = new LarkyPyObject(this.typeClass(), thread);
    newInstance.__init__(args, kwargs);
    return newInstance;
  }

  enum LarkyPyObjectBuiltinSingleton {
    INSTANCE;

    private final LarkyPyObject inst;

    LarkyPyObjectBuiltinSingleton() {
      LarkyTypeObject type = new LarkyTypeObject(LarkyType.Origin.BUILTIN, "object", Dict.empty());
      LarkyType.setupInheritanceHierarchy(type, new LarkyType[0]);
      inst = new LarkyPyObjectBuiltin(type, null);
    }

    public LarkyPyObject get() {
      return inst;
    }
  }
}
