package com.verygood.security.larky.modules.types;


import com.google.common.primitives.UnsignedBytes;

import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.StarlarkList;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractList;


public abstract class LarkyBytesLike<T> extends AbstractList<T> implements Comparable<LarkyBytesLike<T>>, Sequence<T> {

  private StarlarkList<T> delegate;

  LarkyBytesLike() {
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
  public int size() {
    return getSequenceStorage().size();
  }

  @Override
  public abstract boolean isImmutable();

  @Override
  public int compareTo(@NotNull LarkyBytesLike<T> o) {
    return UnsignedBytes
        .lexicographicalComparator()
        .compare(getBytes(), o.getBytes());
  }
}
