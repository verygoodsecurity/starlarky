package com.verygood.security.larky.modules.types;

import java.util.Iterator;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkIterable;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.NotNull;

public interface LarkyCollection extends LarkyIndexable, StarlarkIterable<Object> {
  @NotNull
  @Override
  default Iterator<Object> iterator() {
    try {
      return LarkyIterator.LarkyObjectIterator.of(this, getCurrentThread());
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  default boolean containsKey(StarlarkThread starlarkThread, StarlarkSemantics semantics, Object key) throws EvalException {
    return this.__contains__(this, TokenKind.IN, key, false, starlarkThread);
  }

  /**
   * The below does not belong in LarkyObject because LarkyObject does not dictate what operations should exist on an
   * object. That is left to the interface implementer.
   *
   * However, for LarkyCollections and its hierarchy tree, in Larky, we can simply "tack-on" the magic method (i.e. __len__
   * or __contains__, etc.) and we expect various operations to work on that object, which is why we want to enable
   * binaryOp on SimpleStruct.
   */
  @Override
  default boolean __contains__(LarkyIndexable lhs, TokenKind op, Object rhs, boolean thisLeft, StarlarkThread thread) throws EvalException {
    boolean result = false;
    try {
      result = LarkyIndexable.super.__contains__(lhs, op, rhs, thisLeft, thread);
    } catch (EvalException ignored) {
    }

    if(!result && !thisLeft) {
      // it does not. ok, is thisLeft = false & it is an iterator?
      LarkyCollection lhsCollection = (LarkyCollection) lhs;
      try {
        final LarkyIterator iterator = (LarkyIterator) lhsCollection.iterator();
        Object res = iterator.binaryOp(op, rhs, false);
        result = (res != null && (boolean) res);
      } catch (Throwable ignored) {
      }
    }
    return result;
  }

}
