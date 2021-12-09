package com.verygood.security.larky.modules.types;

import com.google.common.collect.ImmutableList;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkIndexable;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.TokenKind;


public interface LarkyIndexable extends LarkyObject, StarlarkIndexable.Threaded {

  default Object get__getitem__() {
    try {
      return getField(PyProtocols.__GETITEM__);
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
  }


  default Object get__contains__() {
    try {
      return getField(PyProtocols.__CONTAINS__);
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  default Object getIndex(StarlarkThread starlarkThread, StarlarkSemantics semantics, Object key) throws EvalException {
    // The __getitem__ magic method is usually used for list indexing, dictionary lookup, or
    // accessing ranges of values.
    final StarlarkCallable __getitem__ = (StarlarkCallable) get__getitem__();
    if(__getitem__ != null) {
      return this.invoke(starlarkThread, __getitem__, ImmutableList.of(key), EMPTY_KWARGS);
    }
    throw Starlark.errorf("TypeError: '%s' object is not subscriptable", type());
  }

  @Override
  default boolean containsKey(StarlarkThread starlarkThread, StarlarkSemantics semantics, Object key) throws EvalException {
    return __contains__(this, TokenKind.IN, key, false, starlarkThread);
  }

  /**
   * The below does not belong in LarkyObject because LarkyObject does not dictate what operations should exist on an
   * object. That is left to the interface implementer.
   *
   * However, in Larky, we can simply "tack-on" the magic method (i.e. __len__
   * or __contains__, etc.) and we expect various operations to work on that object.
   *
   * This is why we want to make it easy to have Indexable so we can operator overload [], etc.
   */
  default boolean __contains__(LarkyIndexable lhs, TokenKind op, Object rhs, boolean thisLeft, StarlarkThread thread) throws EvalException {
    // is this (thisLeft = true) "is this in that?" or (thisLeft = false) "is that in this?"
    // first, check to see if __contains__ exists?
    final StarlarkCallable __contains__ =
      thisLeft
        ? (StarlarkCallable) ((LarkyIndexable) rhs).get__contains__()
        : (StarlarkCallable) lhs.get__contains__();
    if (__contains__ == null) {
      throw Starlark.errorf(
        "unsupported binary operation: %s %s %s", Starlark.type(rhs), TokenKind.IN, type());
    }
    return thisLeft
             ? (boolean) ((LarkyIndexable) rhs).invoke(thread, __contains__, ImmutableList.of(lhs), EMPTY_KWARGS)
             : (boolean) lhs.invoke(thread, __contains__, ImmutableList.of(rhs), EMPTY_KWARGS);
  }

}
