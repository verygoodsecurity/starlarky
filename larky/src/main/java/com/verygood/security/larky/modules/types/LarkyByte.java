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

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
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
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;


@StarlarkBuiltin(
    name = "bytes",
    documented = false
)
public final class LarkyByte extends LarkyByteLike implements LarkyObject, HasBinary {

  private final StarlarkThread currentThread;
  private final Map<String, Object> fields = new HashMap<>();

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

   @Override
   public LarkyByte build() throws EvalException {
        return new LarkyByte(this);
    }

  }

  private LarkyByte(Builder builder) throws EvalException {
    currentThread = builder.currentThread;
    setSequenceStorage(builder.sequence);
    initFields();
  }

  private void initFields() {
    fields.putAll(ImmutableMap.of(
        "elems", new StarlarkCallable() {
          @Override
          public Object fastcall(StarlarkThread thread, Object[] positional, Object[] named) {
            return elems();
          }

          @Override
          public String getName() {
            return "bytes.elems";
          }
        }
    ));
  }

  public LarkyByteElems elems() {
    return new LarkyByteElems(this);
  }

  @Override
  protected ByteLikeBuilder builder() {
    return builder(this.getCurrentThread());
  }

  @Override
  public byte[] getBytes() {
    return Bytes.toArray(this.getSequenceStorage().stream()
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
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LarkyByte)) {
        return false;
    }
    if (this == obj) {
        return true;
    }
    return this.compareTo((LarkyByte) obj) == 0;
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
          return LarkyByte.builder(getCurrentThread())
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
        return null;
//        throw Starlark.errorf(
//                "unsupported binary operation: %s %s %s", Starlark.type(this), op, Starlark.type(that));
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


  @StarlarkMethod(name = "hex")
  public String hex() {
    return hexlify(getBytes());
  }

  @StarlarkMethod(
      name = "decode",
      parameters = {
          @Param(name = "encoding", defaultValue = "'utf-8'"),
          @Param(name ="errors", defaultValue = "'strict'")
      })
  public String decode(String encoding, String errors) throws EvalException {
    CharsetDecoder decoder = Charset.forName(encoding)
        .newDecoder()
        .onMalformedInput(TextUtil.CodecHelper.convertCodingErrorAction(errors))
        .onUnmappableCharacter(TextUtil.CodecHelper.convertCodingErrorAction(errors));
    try {
      return new String(
          decoder.decode(
              ByteBuffer.wrap(getBytes())
          ).array());
    } catch (CharacterCodingException e) {
      throw Starlark.errorf(e.getMessage());
    }
  }

//
//  @StarlarkMethod(
//      name = "fromhex",
//      parameters = {@Param(name = "hexstr")},
//      useStarlarkThread = true)
//  public static LarkyByte unhexlify(String hexstr, StarlarkThread thread) throws EvalException {
//    int length = hexstr.length();
//    byte[] result = new byte[length / 2];
//    for (int i = 0; i < length; i += 2) {
//      result[i / 2] = (byte) (
//          (Character.digit(hexstr.charAt(i), 16) << 4)
//          + Character.digit(hexstr.charAt(i + 1), 16)
//      );
//    }
//    return (LarkyByte) builder(thread).setSequence(result).build();
//  }

}
