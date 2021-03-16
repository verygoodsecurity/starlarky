package com.verygood.security.larky.modules.types;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;

import com.verygood.security.larky.modules.codecs.TextUtil;
import com.verygood.security.larky.modules.utils.FnvHash;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;


@StarlarkBuiltin(
    name = "bytes",
    documented = false
)
public final class LarkyPByte extends LarkyByteLike<StarlarkInt> implements LarkyObject, HasBinary {

  private final StarlarkThread currentThread;
  private final Map<String, Object> fields = new HashMap<>();

  public static Builder builder(StarlarkThread thread) {
    return new Builder(thread);
  }

  public static class Builder {
    // required parameters
    private StarlarkThread currentThread;
    private Sequence<StarlarkInt> sequence;

    public Builder(StarlarkThread currentThread) {
         this.currentThread = currentThread;
     }

    public Builder setSequence(byte[] buf) throws EvalException {
        return setSequence(ByteBuffer.wrap(buf));
    }

    public Builder setSequence(byte[] buf, int off, int ending) throws EvalException {
       return setSequence(ByteBuffer.wrap(buf, off, ending));
    }

    public Builder setSequence(ByteBuffer buf) throws EvalException {
      int[] arr = new int[buf.remaining()];
      for(int i = 0; i < arr.length; i++){
          arr[i] = Byte.toUnsignedInt(buf.get(i));
      }
      return setSequence(arr);
    }

    public Builder setSequence(@Nonnull CharSequence string) throws EvalException {
      return setSequence(string.chars().toArray());
    }

    public Builder setSequence(int[] iterable_of_ints) throws EvalException {
      setSequence(
          StarlarkList.immutableCopyOf(
              IntStream.of(iterable_of_ints)
                  .mapToObj(StarlarkInt::of)
                  .collect(Collectors.toList())
          ));
      return this;
    }

    public Builder setSequence(@Nonnull Sequence<?> seq) throws EvalException {
      try {
        sequence = StarlarkList.immutableCopyOf(
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


    public LarkyPByte build() throws EvalException {
        return new LarkyPByte(this);
    }

  }

  private LarkyPByte(Builder builder) throws EvalException {
    currentThread = builder.currentThread;
    setSequenceStorage(builder.sequence);
    initFields();
  }

  private void initFields() {
    fields.putAll(ImmutableMap.of(
        "elems", new StarlarkCallable() {
          @Override
          public Object fastcall(StarlarkThread thread, Object[] positional, Object[] named) {
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

  public String getString() {
    try {
      return TextUtil.decode(getBytes());
    } catch (CharacterCodingException e) {
      throw new RuntimeException(e.getMessage(), e.fillInStackTrace());
    }
  }

  @Override
  public void str(Printer printer) {
    /*
    The starlark spec says that UTF-8 gets encoded to UTF-K,
    where K is the host language: Go, Rust is UTF-8 and Java is
    UTF-16.
     */
    StringBuffer sb = new StringBuffer();
    ByteBuffer buf = ByteBuffer.wrap(getBytes());
    int lastpos = 0;
    int l = getBytes().length;
    while(buf.hasRemaining()) {
      int r = 0;
      try {
        r = TextUtil.bytesToCodePoint(buf);
        if(r == -1) {
          break;
        }
        lastpos = buf.position();
      }catch(java.nio.BufferUnderflowException e) {
        buf.position(lastpos);
        for(int i = lastpos; i < l; i++) {
          sb.append("\\x");
          sb.append(Integer.toHexString(Byte.toUnsignedInt(buf.get(i))));
        }
        break;
      }
      if(Character.isLowSurrogate((char) r) || Character.isHighSurrogate((char) r)) {
        sb.append(TextUtil.REPLACEMENT_CHAR);
      }
      else {
        sb.append(TextUtil.runeToString(r));
      }

      //System.out.println(Integer.toHexString(r));
      //System.out.println("Chars: " + Arrays.toString(Character.toChars(r)));
    }
    printer.append(sb.toString());
  }

  @Override
  public void repr(Printer printer) {
    String s = TextUtil.starlarkDecodeUtf8(getBytes());
    String s2 = String.format("b\"%s\"", s);
    System.out.println("passing: " + s2);
    printer.append(s2);
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
          return LarkyPByte.builder(getCurrentThread())
              .setSequence(
                  Bytes.toArray(
                      Streams.stream(
                          Iterables.concat(
                              Collections.nCopies(copies, this.getSequenceStorage())
                          ))
                          .map(StarlarkInt::toNumber)
                          .collect(Collectors.toList())
                  ))
              .build();
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
      return LarkyPByte.builder(getCurrentThread())
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
