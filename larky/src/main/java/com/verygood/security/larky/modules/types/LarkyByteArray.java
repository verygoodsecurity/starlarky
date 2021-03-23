package com.verygood.security.larky.modules.types;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;

import com.verygood.security.larky.modules.codecs.TextUtil;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;


@StarlarkBuiltin(
    name = "bytearray",
    documented = false
)
public final class LarkyByteArray extends LarkyByteLike {

  private final StarlarkThread currentThread;

  public static Builder builder(StarlarkThread thread) {
    return new Builder(thread);
  }

  public static class Builder implements ByteLikeBuilder {
    // required parameters
    private final StarlarkThread currentThread;
    private Sequence<StarlarkInt> sequence;

    private Builder(StarlarkThread currentThread) {
         this.currentThread = currentThread;
     }

    @Override
    public ByteLikeBuilder setSequence(@NotNull Sequence<?> seq) throws EvalException {
      try {
        sequence = StarlarkList.copyOf(
            currentThread.mutability(),
            Sequence.cast(seq, StarlarkInt.class, "could not cast!")
            .stream()
            .mapToInt(StarlarkInt::toIntUnchecked)
            .map(UnsignedBytes::checkedCast)
            .mapToObj(Number.class::cast)
            .map(Number::byteValue)
            .map(Byte::toUnsignedInt)
            .map(StarlarkInt::of)
            .collect(Collectors.toList()));
      }catch(IllegalArgumentException e) {
        throw Starlark.errorf("%s, want value in unsigned 8-bit range", e.getMessage());
      }
       return this;
   }

   @Override
    public LarkyByteArray build() throws EvalException {
        return new LarkyByteArray(this);
    }

  }

  private LarkyByteArray(Builder builder) {
    currentThread = builder.currentThread;
    setSequenceStorage(builder.sequence);
  }


  @Override
  protected ByteLikeBuilder builder() {
    return builder(this.currentThread);
  }

  @Override
  public void setSequenceStorage(Sequence<StarlarkInt> store) {
    super.setSequenceStorage(store);
  }

  @Override
  public byte[] getBytes() {
     return Bytes.toArray(
         this.getSequenceStorage().stream()
         .map(StarlarkInt::toNumber)
         .map(Number::byteValue)
         .collect(Collectors.toList()));
  }

  @Override
  public void str(Printer printer) {
    printer.append(TextUtil.starlarkStringTranscoding(getBytes()));
  }

  @Override
  public void repr(Printer printer) {
    String s = TextUtil.starlarkDecodeUtf8(getBytes());
    String s2 = String.format("b\"%s\"", s);
    printer.append(s2);
  }


  @Override
  public boolean isImmutable() {
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LarkyByteArray)) {
        return false;
    }
    if (this == obj) {
        return true;
    }
    return this.compareTo((LarkyByteArray) obj) == 0;
  }

  @Override
  public StarlarkInt get(int index) {
    return this.getSequenceStorage().get(index);
  }

}
