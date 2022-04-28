package com.verygood.security.larky.objects;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSortedSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
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
  @StarlarkMethod(
    name = "__getattribute__",
    doc = " <pre>__getattribute__</pre> provides attribute read access on the object and its type.                              " +
            "\n" +
            "The default instance {@code __getattribute__} slot implements dictionary look-up on the type and the instance. It" +
            "is the starting point for activating the descriptor protocol. The following order of precedence applies when     " +
            "looking for the value of an attribute:                                                                           " +
            "<ol>                                                                                                             " +
            "<li>a data descriptor from the dictionary of the type</li>                                                       " +
            "<li>a value in the instance dictionary of {@code obj}</li>                                                       " +
            "<li>a non-data descriptor from dictionary of the type</li>                                                       " +
            "<li>a value from the dictionary of the type</li>                                                                 " +
            "</ol>                                                                                                            " +
            "If a matching entry on the type is a data descriptor (case 1),                                                   " +
            "but throws AttributeError, the instance dictionary (if                                                           " +
            "any) will be consulted, and the subsequent cases (3 and 4)                                                       " +
            "skipped. A non-data descriptor that throws an                                                                    " +
            "AttributeError (case 3) causes case 4 to be skipped.                                                             ",
    parameters = {
      @Param(name = "name", allowedTypes = {@ParamType(type = String.class)})
    },
    useStarlarkThread = true
  )
  public Object __getattribute__(String name, StarlarkThread thread)
    throws EvalException {
    return GetAttribute.get(this, name, thread);
  }

  @Override
  public void __setattr__(String name, Object value, StarlarkThread thread) throws EvalException {
    this.__dict__.put(name, value);
  }

  @Override
  public void __delattr__(String name, StarlarkThread thread) throws EvalException {
    this.__dict__.remove(name);
  }
}
