package com.verygood.security.larky.modules.types;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
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
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;


@StarlarkBuiltin(
    name = "bytes",
    documented = false
)
public final class LarkyByte extends AbstractList<StarlarkInt> implements LarkyObject, HasBinary, Sequence<StarlarkInt>, Comparable<LarkyByte> {

  final private List<StarlarkInt> _string;
  final private Map<String, Object> fields = new HashMap<>();
  final StarlarkThread currentThread;

  /**
   * bytes() -> empty bytes object
   */
  public LarkyByte(StarlarkThread thread) throws EvalException {
    this(thread, "", true);
  }

  /**
   * bytes(int) -> bytes object of size given by the parameter initialized with null bytes
   *
   * @param sizeof ->  size given by the parameter initialized
   */
  public LarkyByte(StarlarkThread thread, int sizeof) {
    this(thread, ByteBuffer.allocate(sizeof));
  }

  /**
   * bytes(bytes_or_buffer) -> immutable copy of bytes_or_buffer
   */
  public LarkyByte(StarlarkThread thread, byte[] buf) {
    this(thread, buf, 0, buf.length);
  }

  public LarkyByte(StarlarkThread thread, ByteBuffer buf) {
    this(thread, buf.array(), 0, buf.limit());
  }

  /**
   * bytes(string, encoding[, errors]) -> bytes
   */
  public LarkyByte(StarlarkThread thread, @Nonnull CharSequence string) throws EvalException {
    this(thread, string, false);
  }

  public LarkyByte(StarlarkThread thread, @Nonnull LarkyByteElems elems) throws EvalException {
    this(thread, elems.getLarkyByteArr().toUnsignedBytes());
  }

  public LarkyByte(StarlarkThread thread, @Nonnull StarlarkList<?> list) throws EvalException {
    this(thread,      list
            .stream()
            .map(StarlarkInt.class::cast)
            .map(StarlarkInt::toIntUnchecked)
            .mapToInt(i -> i)
            .toArray());
  }

  public LarkyByte(StarlarkThread thread, byte[] buf, int off, int ending) {
    this.currentThread = thread;
    _string = Bytes.asList(buf)
        .stream()
        .skip(off)
        .limit(ending)
        .map(Byte::toUnsignedInt)
        .map(StarlarkInt::of)
        .collect(Collectors.toList());
    initFields();
  }

  /**
   * bytes(iterable_of_ints) -> bytes
   */
  public LarkyByte(StarlarkThread thread, int[] iterable_of_ints) throws EvalException {
    this.currentThread = thread;
    try {
        _string = IntStream.of(iterable_of_ints)
            .mapToObj(UnsignedBytes::checkedCast)
            .map(Byte::toUnsignedInt)
            .map(StarlarkInt::of)
            .collect(Collectors.toList());
    }catch (IllegalArgumentException e) {
      throw Starlark.errorf("%s, want value in unsigned 8-bit range", e.getMessage());
    }
    initFields();
  }

  /**
   * Local-use constructor in which the client is allowed to guarantee that the
   * <code>String</code> argument contains only characters in the byte range. We do not then
   * range-check the characters.
   *
   * @param string  a Java String to be wrapped (not null)
   * @param isBytes true if the client guarantees we are dealing with bytes
   */
  private LarkyByte(StarlarkThread thread, CharSequence string, boolean isBytes) throws EvalException {
    this.currentThread = thread;
    if (!isBytes && !TextUtil.isBytes(string)) {
      throw Starlark.errorf("Cannot create byte with non-byte value");
    }

    try {
      _string = string.chars()
          .map(UnsignedBytes::checkedCast)
          .mapToObj(StarlarkInt::of)
          .collect(Collectors.toList());
    }catch (IllegalArgumentException e) {
      throw Starlark.errorf("%s, want value in unsigned 8-bit range", e.getMessage());
    }
    initFields();
  }

  private void initFields() {
    fields.putAll(ImmutableMap.of(
        "elems", new StarlarkCallable() {
          @Override
          public Object fastcall(StarlarkThread thread, Object[] positional, Object[] named) throws EvalException, InterruptedException {
            return new LarkyByteElems(LarkyByte.this);
          }

          @Override
          public String getName() {
            return "bytes.elems";
          }
        }
    ));
  }


  /**
   * @return a byte array with one byte for each char in this object's underlying String. Each
   *         byte contains the low-order bits of its corresponding char.
   */
  public byte[] toBytes() {
    return Bytes.toArray(this._string.stream()
        .map(StarlarkInt::toNumber)
        .map(Number::byteValue)
        .collect(Collectors.toList()));
  }

  public int[] toUnsignedBytes() {
    return this._string.stream()
        .map(StarlarkInt::toIntUnchecked)
        .map(Integer::byteValue)
        .map(Byte::toUnsignedInt)
        .mapToInt(i->i)
        .toArray();
  }


  @Override
  public StarlarkThread getCurrentThread() {
    return this.currentThread;
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
  public String getString() {
    try {
      return TextUtil.decode(toBytes());
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
    ByteBuffer buf = ByteBuffer.wrap(toBytes());
    int lastpos = 0;
    int l = toBytes().length;
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
    String s = TextUtil.starlarkDecodeUtf8(this.toBytes());
    String s2 = String.format("b\"%s\"", s);
    System.out.println("passing: " + s2);
    printer.append(s2);
  }

  @Override
  public int size() {
    return this._string.size();
  }


  @Override
  public boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {

    if(key instanceof LarkyByte) {
         // https://stackoverflow.com/a/32865087/133514
         return -1 != Collections.indexOfSubList(
             this._string,
             ((LarkyByte)key)) ;
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
  public boolean isImmutable() {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    return LarkyByte.class.isAssignableFrom(obj.getClass())
        && (this.compareTo((LarkyByte) obj) == 0);
  }

  @Override
  public int hashCode() {
    return FnvHash.FnvHash32.hash(this.toBytes());
  }

  @Override
  public StarlarkInt get(int index) {
    return this._string.get(index);
  }

  @Override
  public int compareTo(@NotNull LarkyByte o) {
    return UnsignedBytes
        .lexicographicalComparator()
        .compare(toBytes(), o.toBytes());
  }

  @Override
  public Sequence<StarlarkInt> getSlice(Mutability mu, int start, int stop, int step) {
    StarlarkList<StarlarkInt> c = StarlarkList.copyOf(mu, new ArrayList<>(this._string));
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
              Collections.nCopies(copies, this._string)))
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
