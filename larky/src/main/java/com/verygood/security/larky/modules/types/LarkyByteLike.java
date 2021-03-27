package com.verygood.security.larky.modules.types;


import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;

import com.verygood.security.larky.modules.utils.FnvHash;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;


public abstract class LarkyByteLike extends AbstractList<StarlarkInt> implements Comparable<LarkyByteLike>, Sequence<StarlarkInt> {

  private StarlarkList<StarlarkInt> delegate;

  Sequence<StarlarkInt> getSequenceStorage() {
    return this.delegate;
  }

  void setSequenceStorage(Sequence<StarlarkInt> store) {
    delegate = StarlarkList.immutableCopyOf(store.getImmutableList());
  }

   public abstract byte[] getBytes();

   public int[] getUnsignedBytes() {
     return Bytes.asList(getBytes())
         .stream()
         .map(Byte::toUnsignedInt)
         .mapToInt(i->i)
         .toArray();
   }

  @Override
  public int hashCode() {
    return FnvHash.FnvHash32.hash(this.getBytes());
  }

  @Override
  public boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {
    if(key instanceof LarkyByteLike) {
      // https://stackoverflow.com/a/32865087/133514
      //noinspection unchecked
      return -1 != Collections.indexOfSubList(getSequenceStorage(), (LarkyByteLike) key) ;
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
  public int compareTo(@NotNull LarkyByteLike o) {
    return UnsignedBytes
        .lexicographicalComparator()
        .compare(getBytes(), o.getBytes());
  }

  protected abstract ByteLikeBuilder builder();

  @Override
  public Sequence<StarlarkInt> getSlice(Mutability mu, int start, int stop, int step) {
    StarlarkList<StarlarkInt> c = StarlarkList.copyOf(mu, new ArrayList<>(this.getSequenceStorage()));
    try {
      return this.builder()
          .setSequence(c.getSlice(mu, start, stop, step).stream()
              .map(StarlarkInt::toIntUnchecked)
              .map(Integer::byteValue)
              .map(Byte::toUnsignedInt)
              .mapToInt(i->i)
              .toArray()
          ).build();
    } catch (EvalException e) {
      throw new RuntimeException(e.getMessage(), e.fillInStackTrace());
    }
  }

  public interface ByteLikeBuilder {

    default ByteLikeBuilder setSequence(byte[] buf) throws EvalException {
      return setSequence(ByteBuffer.wrap(buf));
    }
    default ByteLikeBuilder setSequence(byte[] buf, int off, int ending) throws EvalException {
      return setSequence(ByteBuffer.wrap(buf, off, ending));
    }
    default ByteLikeBuilder setSequence(@Nonnull CharSequence string) throws EvalException {
      return setSequence(string.chars().toArray());
    }
    default ByteLikeBuilder setSequence(ByteBuffer buf) throws EvalException {
      int[] arr = new int[buf.remaining()];
      for(int i = 0; i < arr.length; i++){
          arr[i] = Byte.toUnsignedInt(buf.get(i));
      }
      return setSequence(arr);
    }

    default ByteLikeBuilder setSequence(int[] iterable_of_ints) throws EvalException {
      StarlarkList<StarlarkInt> collect = StarlarkList.immutableCopyOf(
          IntStream.of(iterable_of_ints)
          .mapToObj(StarlarkInt::of)
          .collect(Collectors.toList()));
      setSequence(collect);
      return this;
    }

    ByteLikeBuilder setSequence(@Nonnull Sequence<?> seq) throws EvalException;
    LarkyByteLike build() throws EvalException;
  }

  static class BinaryOperations {

    /**
     * Attempts to multiply a LarkyByteLike type by an integer. The caller is responsible for casting
     * to the appropriate sub-type.
     *
     * @throws EvalException
     */
    static public LarkyByteLike multiply(LarkyByteLike target, Integer num) throws EvalException {
      List<Sequence<StarlarkInt>> copies = Collections.nCopies(num, target.getSequenceStorage());
      Iterable<StarlarkInt> joined = Iterables.concat(copies);
      byte[] bytes = Bytes.toArray(
          Streams.stream(joined)
              .map(StarlarkInt::toNumber)
              .collect(Collectors.toList())
      );
      return target.builder().setSequence(bytes).build();
    }

    /**
     * Add right to left (i.e. [1] + [2] = [1, 2])
     * @throws EvalException
     * @return
     */
    static public StarlarkList<StarlarkInt> add(LarkyByteLike left, LarkyByteLike right) throws EvalException {
      StarlarkList<StarlarkInt> seq;
      seq = StarlarkList.concat(
          StarlarkList.immutableCopyOf(left.getSequenceStorage().getImmutableList()),
          StarlarkList.immutableCopyOf(right.getSequenceStorage().getImmutableList()),
          null);
      return seq;
    }
  }
}
