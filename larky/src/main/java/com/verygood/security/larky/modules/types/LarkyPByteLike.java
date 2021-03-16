package com.verygood.security.larky.modules.types;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import com.google.common.primitives.Bytes;

import com.verygood.security.larky.modules.codecs.TextUtil;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.HasBinary;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

interface LarkyPByteLike extends LarkyObject, HasBinary, Sequence<StarlarkInt>, Comparable<LarkyByte> {

  List<StarlarkInt> getInternalSequence();

  @Override
  StarlarkInt get(int index);

  @Override
  int compareTo(@NotNull LarkyByte o);

  @Override
  Sequence<StarlarkInt> getSlice(Mutability mu, int start, int stop, int step);

  default String getString() {
    try {
      return TextUtil.decode(toBytes());
    } catch (CharacterCodingException e) {
      throw new RuntimeException(e.getMessage(), e.fillInStackTrace());
    }
  }

  /**
   * @return a byte array with one byte for each char in this object's underlying String. Each byte
   * contains the low-order bits of its corresponding char.
   */
  default byte[] toBytes() {
    return Bytes.toArray(this.getInternalSequence().stream()
        .map(StarlarkInt::toNumber)
        .map(Number::byteValue)
        .collect(Collectors.toList()));
  }

  default int[] toUnsignedBytes() {
    return this.getInternalSequence().stream()
        .map(StarlarkInt::toIntUnchecked)
        .map(Integer::byteValue)
        .map(Byte::toUnsignedInt)
        .mapToInt(i -> i)
        .toArray();
  }

  @Override
  default void str(Printer printer) {
    /*
    The starlark spec says that UTF-8 gets encoded to UTF-K,
    where K is the host language: Go, Rust is UTF-8 and Java is
    UTF-16.
     */
    StringBuffer sb = new StringBuffer();
    ByteBuffer buf = ByteBuffer.wrap(toBytes());
    int lastpos = 0;
    int l = toBytes().length;
    while (buf.hasRemaining()) {
      int r = 0;
      try {
        r = TextUtil.bytesToCodePoint(buf);
        if (r == -1) {
          break;
        }
        lastpos = buf.position();
      } catch (java.nio.BufferUnderflowException e) {
        buf.position(lastpos);
        for (int i = lastpos; i < l; i++) {
          sb.append("\\x");
          sb.append(Integer.toHexString(Byte.toUnsignedInt(buf.get(i))));
        }
        break;
      }
      if (Character.isLowSurrogate((char) r) || Character.isHighSurrogate((char) r)) {
        sb.append(TextUtil.REPLACEMENT_CHAR);
      } else {
        sb.append(TextUtil.runeToString(r));
      }

      //System.out.println(Integer.toHexString(r));
      //System.out.println("Chars: " + Arrays.toString(Character.toChars(r)));
    }
    printer.append(sb.toString());
  }

  @Override
  default void repr(Printer printer) {
    String s = TextUtil.starlarkDecodeUtf8(this.toBytes());
    String s2 = String.format("b\"%s\"", s);
    System.out.println("passing: " + s2);
    printer.append(s2);
  }

  @Override
  default boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {

    if (key instanceof LarkyByte) {
      // https://stackoverflow.com/a/32865087/133514
      return -1 != Collections.indexOfSubList(
          this.getInternalSequence(),
          ((LarkyByte) key));
    } else if (key instanceof StarlarkInt) {
      StarlarkInt _key = ((StarlarkInt) key);
      if (!Range
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

  /**
   * Returns {@code this op that}, if thisLeft, or {@code that op this} otherwise. May return null
   * to indicate that the operation is not supported, or may throw a specific exception.
   */
  @Nullable
  @Override
  default Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    switch (op) {
      case STAR:
        Object function = this.getField(PyProtocols.__MUL__);
        if (this.getField(PyProtocols.__MUL__) != null) {
          return invoke(function, ImmutableList.of(that));
        }
        if (that instanceof StarlarkInt) {
          int copies = ((StarlarkInt) that).toIntUnchecked();

          return new LarkyByte(
              this.getCurrentThread(),
              Bytes.toArray(
                  Streams.stream(
                      Iterables.concat(
                          Collections.nCopies(copies, this.getInternalSequence())))
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
}
