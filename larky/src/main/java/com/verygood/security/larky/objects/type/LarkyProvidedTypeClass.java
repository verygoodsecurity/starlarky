package com.verygood.security.larky.objects.type;

import java.util.Map;

import com.verygood.security.larky.objects.LarkyBindable;
import com.verygood.security.larky.objects.LarkyFunction;
import com.verygood.security.larky.objects.LarkyPyObject;
import com.verygood.security.larky.objects.LarkyTypeObject;
import com.verygood.security.larky.objects.PyObject;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

final public class LarkyProvidedTypeClass implements ForwardingLarkyType, StarlarkCallable {
  private final StarlarkThread thread;
  private final LarkyTypeObject type;

  public LarkyProvidedTypeClass(StarlarkThread thread, LarkyTypeObject type) {
    this.thread = thread;
    this.type = type;
    init(thread);
  }

  private void init(StarlarkThread thread) {
    final Map<String, Object> clsDict = this.getInternalDictUnsafe();
    for(Map.Entry<String, Object> entry : clsDict.entrySet()) {
      Object value = entry.getValue();
      if(value instanceof StarlarkFunction) {
        // We are decorating a StarlarkFunction with a LarkyFunction so
        // that we can enable python descriptor support.
        value = LarkyFunction.create((StarlarkFunction) value, thread);
      }

      if (value instanceof LarkyBindable) {
        ((LarkyBindable) value).bindToOwnerIfNotBound(this);
      }

      clsDict.put(entry.getKey(), value);
    }
  }

  @Override
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    /*
      If __new__() is invoked during object construction, and it
      returns an instance of cls, then the new instance’s __init__() method
      will be invoked like __init__(self[, ...]), where self is the new
      instance and the remaining arguments are the same as were passed
      to the object constructor.
     */
    LarkyPyObject newInst = (LarkyPyObject)this.__new__(Tuple.concat(Tuple.of(this), args), kwargs, thread);
    LarkyType instanceCls;
    instanceCls = newInst.typeClass();

    if (this == instanceCls) {
      newInst.__init__(args, kwargs);
      return newInst;
    }
    /*
      If __new__() does not return an instance of cls, then the new
      instance’s __init__() method will not be invoked.
     */
    return newInst;

  }

  @Override
  public PyObject __new__(Tuple args, Dict<String, Object> kwargs, StarlarkThread thread) {
    final LarkyType cls = (LarkyType) args.get(0);
    final LarkyPyObject newInst = new LarkyPyObject(cls, thread);
    return newInst;
  }


  @Override
  public String __repr__() {
    return
      String.format("<class '%s'>",
        __name__()
      )
      ;
  }

  @Override
  public String toString() {
    return __name__();
  }

  @Override
  public String getName() {
    return __repr__();
  }

  @Override
  public LarkyType delegate() {
    return type;
  }

  @Override
  public LarkyType typeClass() {
    return LarkyTypeObject.getInstance();
  }

  @Override
  public LarkyType __class__() {
    return LarkyTypeObject.getInstance();
  }

  @Override
  public StarlarkThread getCurrentThread() {
    return thread;
  }

}
