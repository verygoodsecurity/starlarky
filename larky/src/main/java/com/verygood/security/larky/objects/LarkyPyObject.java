package com.verygood.security.larky.objects;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import java.util.Map;

import com.verygood.security.larky.modules.types.LarkyCollection;
import com.verygood.security.larky.objects.type.BinaryOpHelper;
import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class LarkyPyObject implements
  PyObject,
    Comparable<LarkyPyObject>,
    LarkyCollection,
    HasBinary
{

  private final LarkyType.Origin origin;
  private final LarkyType __class__;
  private final Map<String, Object> __dict__;
  private final StarlarkThread thread;

  public LarkyPyObject(LarkyType klass, StarlarkThread instanceThread) {
    this.origin = LarkyType.Origin.LARKY;
    this.thread = instanceThread;
    this.__dict__ = Maps.newHashMap();
    this.__class__ = klass;
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
    Object result;
    final boolean lt;
    final boolean gt;

    try {
      // This code is a bit tricky. If we return null from operatorDispatch,
      // it most likely not a proper comparison operation.
      //
      // To make the IDE happy, we have to do the checks below.
      result = BinaryOpHelper.operatorDispatch(
        this,
        TokenKind.LESS,
        o,
        true,
        this.getCurrentThread()
      );
      if (result instanceof Boolean) {
        lt = (boolean) result;
        if (lt) {
          return -1;
        }
      }
      result = BinaryOpHelper.operatorDispatch(
        this,
        TokenKind.GREATER,
        o,
        true,
        this.getCurrentThread()
      );
      if (result instanceof Boolean) {
        gt = (boolean) result;
        if (gt) {
          return 1;
        }
      }
      // if result is null, let's throw an Error
      if (result == null) {
        throw Starlark.errorf(
          String.format(
          "unsupported binary operation: %s and %s",
          this, o
        ));
      }
    } catch (EvalException e) {
      throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(e, null);
    }
    return 0;
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
    SetAttribute.set(this, name, value, thread);
  }

  @Override
  public void __delattr__(String name, StarlarkThread thread) throws EvalException {
    DeleteAttribute.delete(this, name, thread);
  }

  @Nullable
  @Override
  public Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    // important to note this: https://docs.python.org/3/reference/datamodel.html#special-method-lookup
    // special methods automatically delegate to the underlying type
    // we should bypass the instance dictionary if it's a SpecialMethod
    return this.typeClass().binaryOp(op, that, thisLeft);
  }


}
