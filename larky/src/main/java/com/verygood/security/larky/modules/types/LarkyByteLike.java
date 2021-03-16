package com.verygood.security.larky.modules.types;


import com.google.common.collect.Range;
import com.google.common.primitives.UnsignedBytes;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractList;
import java.util.Collections;


public abstract class LarkyByteLike<T> extends AbstractList<T> implements Comparable<LarkyByteLike<T>>, Sequence<T> {

  private StarlarkList<T> delegate;

  LarkyByteLike() {
  }

  Sequence<T> getSequenceStorage() {
    return this.delegate;
  }

  void setSequenceStorage(Sequence<T> store) {
    delegate = StarlarkList.immutableCopyOf(store.getImmutableList());
  }

  public abstract byte[] getBytes();

  public abstract int[] getUnsignedBytes();

  @Override
  public boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {
    if(key instanceof LarkyByteLike) {
      // https://stackoverflow.com/a/32865087/133514
      //noinspection unchecked
      return -1 != Collections.indexOfSubList(getSequenceStorage(), (LarkyByteLike<T>) key) ;
    }
    else if(key instanceof StarlarkInt) {
      StarlarkInt _key = ((StarlarkInt) key);
      if(!Range
          .closed(0, 255)
          .contains(_key.toIntUnchecked())) {
        throw Starlark.errorf("int in bytes: %s out of range", _key);
      }
      return contains(_key);
    }
    //"requires bytes or int as left operand, not string"
    throw new EvalException(
        String.format("requires bytes or int as left operand, not %s", Starlark.type(key))
    );
  }

  @Override
  public int size() {
    return getSequenceStorage().size();
  }

  @Override
  public abstract boolean isImmutable();

  @Override
  public int compareTo(@NotNull LarkyByteLike<T> o) {
    return UnsignedBytes
        .lexicographicalComparator()
        .compare(getBytes(), o.getBytes());
  }
}
