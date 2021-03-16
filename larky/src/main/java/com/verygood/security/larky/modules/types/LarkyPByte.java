package com.verygood.security.larky.modules.types;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.common.primitives.Bytes;

import com.verygood.security.larky.modules.utils.FnvHash;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@StarlarkBuiltin(
    name = "bytes",
    documented = false
)
public final class LarkyPByte extends LarkyBytesLike<StarlarkInt> implements LarkyObject, HasBinary {

  private final StarlarkThread currentThread;
  private final Map<String, Object> fields = new HashMap<>();

  public LarkyPByte(StarlarkThread thread) {
    currentThread = thread;
  }

  private void initFields() {
    fields.putAll(ImmutableMap.of(
        "elems", new StarlarkCallable() {
          @Override
          public Object fastcall(StarlarkThread thread, Object[] positional, Object[] named) throws EvalException, InterruptedException {
            return new LarkyByteElems(LarkyPByte.this);
          }

          @Override
          public String getName() {
            return "bytes.elems";
          }
        }
    ));
  }

  @Override
  public byte[] getBytes() {
    return Bytes.toArray(this.getSequenceStorage().stream()
            .map(StarlarkInt::toNumber)
            .map(Number::byteValue)
            .collect(Collectors.toList()));
  }

  @Override
  public int[] getUnsignedBytes() {
    return Bytes.asList(getBytes())
        .stream()
        .map(Byte::toUnsignedInt)
        .mapToInt(i->i)
        .toArray();
  }

  @Override
  public boolean isImmutable() {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    return LarkyPByte.class.isAssignableFrom(obj.getClass())
        && (this.compareTo((LarkyPByte) obj) == 0);
  }

  @Override
  public int hashCode() {
    return FnvHash.FnvHash32.hash(this.getBytes());
  }

  @Override
  public StarlarkInt get(int index) {
    return this.getSequenceStorage().get(index);
  }

  /**
  * Returns {@code this op that}, if thisLeft, or {@code that op this} otherwise. May return null
  * to indicate that the operation is not supported, or may throw a specific exception.
  */
  @Nullable
  @Override
  public Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    switch(op) {
      case STAR:
        Object function = this.getField(PyProtocols.__MUL__);
        if(this.getField(PyProtocols.__MUL__) != null) {
          return invoke(function, ImmutableList.of(that));
        }
        if(that instanceof StarlarkInt) {
          int copies = ((StarlarkInt)that).toIntUnchecked();

          return new LarkyByte(
              this.currentThread, Bytes.toArray(Streams.stream(Iterables.concat(
              Collections.nCopies(copies, this.getSequenceStorage())))
                  .map(StarlarkInt::toNumber)
                  .collect(Collectors.toList())
          ));
        }
      default:
        // unsupported binary operation!
        throw Starlark.errorf(
                "unsupported binary operation: %s %s %s", Starlark.type(this), op, Starlark.type(that));
    }
  }

  @Override
  public Sequence<StarlarkInt> getSlice(Mutability mu, int start, int stop, int step) {
    StarlarkList<StarlarkInt> c = StarlarkList.copyOf(mu, new ArrayList<>(this.getSequenceStorage()));
    try {
      return new LarkyByte(
          this.currentThread,
          c.getSlice(mu, start, stop, step).stream()
              .map(StarlarkInt::toIntUnchecked)
              .map(Integer::byteValue)
              .map(Byte::toUnsignedInt)
              .mapToInt(i->i)
              .toArray());
    } catch (EvalException e) {
      throw new RuntimeException(e.getMessage(), e.fillInStackTrace());
    }
  }

  @Nullable
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
  public ImmutableCollection<String> getFieldNames() {
    return ImmutableSet.copyOf(fields.keySet());
  }

  @Override
  public StarlarkThread getCurrentThread() {
    return this.currentThread;
  }
}
