package com.verygood.security.larky.modules.types.structs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.Map;

import com.verygood.security.larky.modules.types.LarkyIterator;
import com.verygood.security.larky.modules.types.LarkyObject;
import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkIndexable;
import net.starlark.java.eval.StarlarkIterable;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import net.starlark.java.spelling.SpellChecker;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// A trivial struct-like class with Starlark fields defined by a map.
public class SimpleStruct implements LarkyObject, StarlarkCallable, StarlarkIterable<Object>, StarlarkIndexable, HasBinary {

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
   * Avoid un-necessary allocation if we need to override the immutability of the `__dict__` in a
   * subclass for the caller.
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

  /**
   * The below does not belong in LarkyObject because LarkyObject does not dictate what operations
   * should exist on an object. That is left to the interface implementer.
   *
   * However, for SimpleStruct and its hierarchy tree, in Larky, we can simply "tack-on" the
   * magic method (i.e. __len__ or __contains__, etc.) and we expect various operations to work
   * on that object, which is why we want to enable binaryOp on SimpleStruct.
   */
  @Nullable
  @Override
  public Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    //noinspection SwitchStatementWithTooFewBranches
    switch (op) {
      case IN:
        // is this (thisLeft = true) "is this in that?" or (thisLeft = false) "is that in this?"
        // first, check to see if __contains__ exists?
        final StarlarkCallable __contains__ =
          thisLeft
                ? (StarlarkCallable) ((SimpleStruct) that).getField(PyProtocols.__CONTAINS__)
                : (StarlarkCallable) getField(PyProtocols.__CONTAINS__);
        if (__contains__ != null) {
          return thisLeft
                   ? (boolean) ((SimpleStruct) that).invoke(__contains__, ImmutableList.of(this))
                   : (boolean) this.invoke(__contains__, ImmutableList.of(that));
        }
        // it does not. ok, is thisLeft = false & it is an iterator?
        if(!thisLeft) {
          try {
            final LarkyIterator iterator = (LarkyIterator) iterator();
            return iterator.binaryOp(op, that, false);
          } catch (RuntimeException ignored) {}
        }
        // *not in* case will be handled by EvalUtils
        // fallthrough
      default:
        // unsupported binary operation!
        return null;
    }
  }

  // supports object[key] retrieval if `__getitem__` is implemented
  @Override
  public Object getIndex(StarlarkSemantics semantics, Object key) throws EvalException {
    // The __getitem__ magic method is usually used for list indexing, dictionary lookup, or
    // accessing ranges of values.
    final StarlarkCallable __getitem__ = (StarlarkCallable) getField(PyProtocols.__GETITEM__);
    if(__getitem__ != null) {
      return this.invoke(__getitem__, ImmutableList.of(key));
    }
    throw Starlark.errorf("TypeError: '%s' object is not subscriptable", type());
  }

  // supports `in` operator if __contains__ is implemented
  @Override
  public boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {
    final Object result = binaryOp(TokenKind.IN, key, false);
    if(result == null) {
      throw Starlark.errorf(
              "unsupported binary operation: %s %s %s", Starlark.type(key), TokenKind.IN, type());
    }
    return (boolean) result;
  }

  @NotNull
  @Override
  public Iterator<Object> iterator() {
    try {
      return LarkyIterator.LarkyObjectIterator.of(this, getCurrentThread());
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) throws EvalException, InterruptedException {
    Object callable = getField(PyProtocols.__CALL__);
    if (callable instanceof StarlarkCallable) {
      // we have to pass the execution thread here b/c otherwise
      // we will pass the thread that was responsible for capturing the
      // the closure -- which is not what we want.
      return invoke(thread, callable, args, kwargs);
    }
    //StarlarkCallable.super.call(thread, args, kwargs);
    throw Starlark.errorf(
      "'%s' object is not callable (either def __call__(*args, **kwargs) is not " +
      "defined or __call__ is defined but is not callable)", getName());
  }

  @Override
  public String getName() {
    Object callable;
    try {
      callable = getField(PyProtocols.__CALL__);
    } catch (EvalException ex) {
      throw new RuntimeException(ex);
    }
    StringBuilder name = new StringBuilder(type());
    if (callable instanceof StarlarkCallable) {
      name.append(".").append(((StarlarkCallable)callable).getName());
    }
    else if(callable != null) {
      name.append(".")
        .append("__call__<type: ")
        .append(StarlarkUtil.richType(callable))
        .append(", value=")
        .append(Starlark.str(callable))
        .append(">");
    }
    return name.toString();
  }
}
