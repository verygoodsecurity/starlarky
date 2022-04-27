package com.verygood.security.larky.objects;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSortedSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.NotNull;


public class LarkyPyObject implements PyObject, Comparable<LarkyPyObject> {

  private final LarkyType.Origin origin;
  private final LarkyType __class__;
  private final Map<String, Object> __dict__;
  private final StarlarkThread thread;

  public LarkyPyObject(LarkyType klass, StarlarkThread instanceThread) {
    this.origin = LarkyType.Origin.LARKY;
    this.thread = instanceThread;
    this.__dict__ = new HashMap<>();
    this.__class__ = klass;
  }

  public static LarkyPyObject getInstance() {
    return LarkyPyObjectBuiltin.LarkyPyObjectBuiltinSingleton.INSTANCE.get();
  }

  @Override
  public void __init__(Tuple args, Dict<String, ?> keywords) throws EvalException {
    Object init = this.getField("__init__", thread);
    if (init != null && StarlarkUtil.isCallable(init)) {
      try {
        Starlark.call(
          thread,
          StarlarkUtil.toCallable(init),
          args,
          Dict.cast(keywords, String.class, Object.class, this + ".__init__()")
        );
      } catch (InterruptedException e) {
        throw new EvalException(e);
      }
    }
  }

  @Override
  public LarkyType.Origin getOrigin() {
    return this.origin;
  }


  @Override
  public String typeName() {
    return typeClass().__name__();
  }

  @Override
  public LarkyType typeClass() {
    return this.__class__;
  }

  @Override
  public LarkyType __class__() {
    return this.__class__;
  }

  @Override
  public PyObject __new__(Tuple args, Dict<String, Object> kwargs, StarlarkThread thread) {
    throw new UnsupportedOperationException(); // TODO: implement me
  }

  @Override
  public void debugPrint(Printer p) {
    // This repr function prints only the fields.
    // Any methods are still accessible through dir/getattr/hasattr.
    p.append(typeName()).append("(");
    String sep = "";
    for (Map.Entry<String, Object> e : __dict__.entrySet()) {
      p.append(sep).append(e.getKey()).append(" = ").repr(e.getValue());
      sep = ", ";
    }
    p.append(")");
  }

  @Override
  public String toString() {
    return __repr__();
  }

  @Override
  public void repr(Printer printer) {
    printer.append(this.__repr__());
  }

  @Override
  public void str(Printer printer) {
    printer.append(this.__str__());
  }

  @Override
  public String __str__() {
    return this.__repr__();
  }

  @Override
  public String __repr__() {
    return String.format("<'%s' object>",
      typeName()
    );
  }


  @Override
  public StarlarkThread getCurrentThread() {
    return thread;
  }

   @Override
  public Map<String, Object> getInternalDictUnsafe() {
    return this.__dict__;
  }

  @Override
  public int compareTo(@NotNull LarkyPyObject o) {
    return Objects.equals(this, o) ? 0 : -1;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LarkyPyObject)) {
      return false;
    }
    return this == obj;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public ImmutableCollection<String> getFieldNames() {
    return ImmutableSortedSet.copyOf(this.__dict__.keySet());
  }

  @Override
  public Object __getattribute__(String name, StarlarkThread thread) throws EvalException {
    return null;
  }

  @Override
  public void __setattr__(String name, Object value, StarlarkThread thread) throws EvalException {
  }

  @Override
  public void __delattr__(String name, StarlarkThread thread) throws EvalException {
  }
}
