package com.verygood.security.larky.modules.types.structs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

import com.verygood.security.larky.modules.types.LarkyObject;
import com.verygood.security.larky.modules.types.PyProtocols;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.spelling.SpellChecker;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.Nullable;

// A trivial struct-like class with Starlark fields defined by a map.
public class SimpleStruct implements LarkyObject, HasBinary {

  final Map<String, Object> fields;
  final StarlarkThread currentThread;

  public static SimpleStruct create(Map<String, Object> kwargs) {
    return new SimpleStruct(kwargs, null);
  }

  public static SimpleStruct immutable(Dict<String, Object> kwargs, StarlarkThread thread) {
    return new ImmutableStruct(ImmutableMap.copyOf(kwargs), thread);
  }

  public static SimpleStruct mutable(Dict<String, Object> kwargs, StarlarkThread thread) {
    return new MutableStruct(kwargs, thread);
  }

  protected SimpleStruct(Map<String, Object> fields, StarlarkThread currentThread) {
    this.currentThread = currentThread;
    this.fields = fields;
  }

  @StarlarkMethod(name = PyProtocols.__DICT__, structField = true)
  public Dict<String, Object> dunderDict() throws EvalException {
    return composeAndFillDunderDictBuilder().build(Mutability.IMMUTABLE);
  }

  @Override
  public StarlarkThread getCurrentThread() {
    return currentThread;
  }

  @Override
  public ImmutableList<String> getFieldNames() {
    return ImmutableList.copyOf(fields.keySet());
  }

  @Override
  public Object getValue(String name) throws EvalException {
    if(name == null
        || !fields.containsKey(name)
        || fields.getOrDefault(name, null) == null) {
      return null;
    }

    return fields.get(name);
  }

  @Override
  public String getErrorMessageForUnknownField(String name) {
    String starlarkType = Starlark.type(this);
    String larkyType = LarkyObject.super.type();
    if(!larkyType.equals(starlarkType)) {
      starlarkType += String.format(" of class '%s'",larkyType);
    }

    return String.format(
      "%s has no field or method '%s'%s",
      starlarkType,
      name,
      SpellChecker.didYouMean(name,
        Starlark.dir(
          getCurrentThread().mutability(),
          getCurrentThread().getSemantics(), name)));
  }

  @Override
  public void repr(Printer p) {
    try {
      if (hasReprField()) {
        final StarlarkCallable reprCallable = (StarlarkCallable) getField(PyProtocols.__REPR__);
        if (reprCallable != null) {
          p.append((String)invoke(reprCallable));
          return;
        }
      }
    } catch (EvalException ex) {
      //TODO(mahmoudimus): Should this throw a RuntimeException?
      throw new RuntimeException(ex);
    }
    p.append("<class '").append(type()).append("'>");
  }

  @Override
  public void debugPrint(Printer p) {
    // This repr function prints only the fields.
    // Any methods are still accessible through dir/getattr/hasattr.
    p.append(type());
    p.append("(");
    String sep = "";
    for (Map.Entry<String, Object> e : fields.entrySet()) {
      p.append(sep).append(e.getKey()).append(" = ").debugPrint(e.getValue());
      sep = ", ";
    }
    p.append(")");
  }

  /**
   * Avoid un-necessary allocation if we need to override the immutability of the `__dict__` in a subclass for the caller.
   * */
  protected Dict.Builder<String, Object> composeAndFillDunderDictBuilder() throws EvalException {
    StarlarkThread thread = getCurrentThread();
    StarlarkList<String> keys = Starlark.dir(thread.mutability(), thread.getSemantics(), this);
    Dict.Builder<String, Object> builder = Dict.builder();
    for(String k : keys) {
      // obviously, ignore the actual __dict__ key since we're in this method already
      if(k.equals(PyProtocols.__DICT__)) {
        continue;
      }
      Object value = getValue(k);
      builder.put(k,  value != null ? value : Starlark.NONE);
    }

    return builder;
  }

  @Override
  public boolean equals(Object obj) {
    StarlarkCallable equals;
    StarlarkCallable notEquals;

    if (!(obj instanceof SimpleStruct)) {
      return false;
    }
    try {
      equals = (StarlarkCallable) getField(PyProtocols.__EQ__);
      if (equals != null) {
        return (boolean) invoke(equals, ImmutableList.of(obj));
      }
      notEquals = (StarlarkCallable) getField(PyProtocols.__NE__);
      if (notEquals != null) {
        return !(boolean) invoke(notEquals, ImmutableList.of(obj));
      }
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
    return this == obj;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  //TODO(mahmoudimus): evaluate if we should have LarkyObject extend StarlarkIndexable
  // and dispatch overridden behavior in `containsKey()` or not?
  //TODO(mahmoudimus): should this belong in LarkyObject?
  @Nullable
  @Override
  public Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    //noinspection SwitchStatementWithTooFewBranches
    switch (op) {
      case IN:
        // is this (thisLeft = true) "is this in that?" or (thisLeft = false) "is that in this?"
        final StarlarkCallable __contains__ =
          thisLeft
                ? (StarlarkCallable) ((SimpleStruct) that).getField(PyProtocols.__CONTAINS__)
                : (StarlarkCallable) getField(PyProtocols.__CONTAINS__);
        if (__contains__ != null) {
          return thisLeft
                   ? (boolean) ((SimpleStruct) that).invoke(__contains__, ImmutableList.of(this))
                   : (boolean) this.invoke(__contains__, ImmutableList.of(that));
        }
        // *not in* case will be handled by EvalUtils
        // fallthrough
      default:
        // unsupported binary operation!
        return null;
    }
  }
}
