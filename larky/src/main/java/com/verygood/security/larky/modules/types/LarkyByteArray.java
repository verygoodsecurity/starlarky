package com.verygood.security.larky.modules.types;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;

import com.verygood.security.larky.modules.io.TextUtil;
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
public final class LarkyByteArray extends AbstractList<StarlarkInt> implements LarkyObject, HasBinary, Sequence<StarlarkInt>, Comparable<LarkyByteArray> {

  final private List<StarlarkInt> _string;
  final private Map<String, Object> fields = new HashMap<>();
  final StarlarkThread currentThread;

  /**
   * bytes() -> empty bytes object
   */
  public LarkyByteArray(StarlarkThread thread) throws EvalException {
    this(thread, "", true);
  }

  /**
   * bytes(int) -> bytes object of size given by the parameter initialized with null bytes
   *
   * @param sizeof ->  size given by the parameter initialized
   */
  public LarkyByteArray(StarlarkThread thread, int sizeof) {
    this(thread, ByteBuffer.allocate(sizeof));
  }

  /**
   * bytes(bytes_or_buffer) -> immutable copy of bytes_or_buffer
   */
  public LarkyByteArray(StarlarkThread thread, byte[] buf) {
    this(thread, buf, 0, buf.length);
  }

  public LarkyByteArray(StarlarkThread thread, ByteBuffer buf) {
    this(thread, buf.array(), 0, buf.limit());
  }

  /**
   * bytes(string, encoding[, errors]) -> bytes
   */
  public LarkyByteArray(StarlarkThread thread, @Nonnull CharSequence string) throws EvalException {
    this(thread, string, false);
  }

  public LarkyByteArray(StarlarkThread thread, @Nonnull LarkyByteArrIterable elems) throws EvalException {
    this(thread, elems.getLarkyByteArr().toUnsignedBytes());
  }

  public LarkyByteArray(StarlarkThread thread, @Nonnull StarlarkList<?> list) throws EvalException {
    this(thread,      list
            .stream()
            .map(StarlarkInt.class::cast)
            .map(StarlarkInt::toIntUnchecked)
            .mapToInt(i -> i)
            .toArray());
  }

  public LarkyByteArray(StarlarkThread thread, byte[] buf, int off, int ending) {
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
  public LarkyByteArray(StarlarkThread thread, int[] iterable_of_ints) throws EvalException {
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
  private LarkyByteArray(StarlarkThread thread, CharSequence string, boolean isBytes) throws EvalException {
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
            return new LarkyByteArrIterable(LarkyByteArray.this);
          }

          @Override
          public String getName() {
            return "bytes.elems";
          }
        }
    ));
  }

  public String getString() {
    try {
      return TextUtil.decode(toBytes());
    } catch (CharacterCodingException e) {
      throw new RuntimeException(e.getMessage(), e.fillInStackTrace());
    }
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

  @Override
  public void repr(Printer printer) {
    // TODO(mahmoudimus): repr should just give escaped strings
    printer.append(String.format("b'%s'", TextUtil.decodeUTF8(this.toBytes(), this.toBytes().length)));
  }

  @Override
  public int size() {
    return this._string.size();
  }


  @Override
  public boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {
    if(key instanceof LarkyByteArray) {
         // https://stackoverflow.com/a/32865087/133514
         return -1 != Collections.indexOfSubList(
             this._string,
             ((LarkyByteArray)key)) ;
       }
     else if(key instanceof StarlarkInt) {
      return contains(key);
     }
     //"requires bytes or int as left operand, not string"
    throw new EvalException(
        String.format("requires bytes or int as left operand, not %s", Starlark.type(key))
    );
  }


  @Override
  public boolean equals(Object obj) {
    return LarkyByteArray.class.isAssignableFrom(obj.getClass())
        && (this.compareTo((LarkyByteArray) obj) == 0);
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
  public int compareTo(@NotNull LarkyByteArray o) {
    return UnsignedBytes
        .lexicographicalComparator()
        .compare(toBytes(), o.toBytes());
  }

  @Override
  public Sequence<StarlarkInt> getSlice(Mutability mu, int start, int stop, int step) {
    StarlarkList<StarlarkInt> c = StarlarkList.copyOf(mu, new ArrayList<>(this._string));
    try {
      return new LarkyByteArray(
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

          return new LarkyByteArray(
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
