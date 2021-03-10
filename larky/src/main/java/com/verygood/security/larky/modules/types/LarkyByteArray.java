package com.verygood.security.larky.modules.types;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;

import com.verygood.security.larky.modules.io.TextUtil;
import com.verygood.security.larky.modules.utils.FnvHash;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

class LarkyByte implements StarlarkValue {
  final private Byte b;
  public LarkyByte(Byte b) {
    this.b = b;
  }
  public Byte get() {return b;}
}

public class LarkyByteArray extends LarkyObject implements Sequence<Byte>, Comparable<LarkyByteArray> {

  private List<Byte> _string;

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

  public LarkyByteArray(StarlarkThread thread, byte[] buf, int off, int ending) {
    super(thread);
    StringBuilder v = new StringBuilder(buf.length);
    for (int i = off; i < ending; i++) {
      v.appendCodePoint(buf[i] & 0xFF);
    }
    _string = Bytes.asList(v.toString().getBytes(StandardCharsets.UTF_8));
  }

  /**
   * bytes(iterable_of_ints) -> bytes
   */
  public LarkyByteArray(StarlarkThread thread, int[] iterable_of_ints) throws EvalException {
    super(thread);
    StringBuilder v = new StringBuilder(iterable_of_ints.length);
    for (int i : iterable_of_ints) {
      try {
        UnsignedBytes.checkedCast(i);
      }catch (IllegalArgumentException e) {
        throw Starlark.errorf("%s, want value in unsigned 8-bit range", e.getMessage());
      }
      v.appendCodePoint(i);
    }
    _string = Bytes.asList(
            v.toString()
                .getBytes(StandardCharsets.UTF_8)
        );
  }

  /**
   * bytes(string, encoding[, errors]) -> bytes
   */
  public LarkyByteArray(StarlarkThread thread, @Nonnull CharSequence string) throws EvalException {
    this(thread, string, false);
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
    super(thread);
    if (!isBytes && !TextUtil.isBytes(string)) {
      throw Starlark.errorf("Cannot create byte with non-byte value");
    }

    _string = Bytes.asList(
            string.toString()
                .getBytes(StandardCharsets.UTF_8)
        );
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
    return Bytes.toArray(new ArrayList<>(this._string));
  }


//  @StarlarkMethod(
//      name = "elems",
//      doc =
//          "Returns an iterable value containing successive 1-element substrings of the string. "
//              + "Equivalent to <code>[s[i] for i in range(len(s))]</code>, except that the "
//              + "returned value might not be a list.",
//      parameters = {@Param(name = "self", doc = "This string.")})
//  public Sequence<String> elems(String self) {
//    // TODO(adonovan): opt: return a new type that is lazily iterable.
//    char[] chars = self.toCharArray();
//    Object[] strings = new Object[chars.length];
//    for (int i = 0; i < chars.length; i++) {
//      strings[i] = memoizedCharToString(chars[i]);
//    }
//    return StarlarkList.wrap(null, strings);
//  }

  final ImmutableMap<String, Object> of = ImmutableMap.of(
      "values_only_field",
      "fromValues",
      "values_only_method",
      returnFromValues,
      "collision_field",
      "fromValues",
      "collision_method",
      returnFromValues);

  // A function that returns "fromValues".
  private static final Object returnFromValues =
      new StarlarkCallable() {
        @Override
        public String getName() {
          return "returnFromValues";
        }

        @Override
        public Object fastcall(StarlarkThread thread, Object[] positional, Object[] named) {
          return "bar";
        }
  };

  @StarlarkMethod(name = "callable_only_field", documented = false, structField = true)
  public String getCallableOnlyField() {
    return "fromStarlarkMethod";
  }

  @StarlarkMethod(name = "callable_only_method", documented = false, structField = false)
  public String getCallableOnlyMethod() {
    return "fromStarlarkMethod";
  }

  @StarlarkMethod(name = "collision_field", documented = false, structField = true)
  public String getCollisionField() {
    return "fromStarlarkMethod";
  }

  @StarlarkMethod(name = "collision_method", documented = false, structField = false)
  public String getCollisionMethod() {
    return "fromStarlarkMethod";
  }

  @Nullable
  @Override
  public Object getValue(String name) throws EvalException {
    return null;
  }

  @Override
  public ImmutableCollection<String> getFieldNames() {
    return null;
  }

  @Nullable
  @Override
  public String getErrorMessageForUnknownField(String field) {
    return null;
  }

  @Override
  public void repr(Printer printer) {
    printer.append(String.format("b'%s'", this.getString()));
  }

  @Override
  public int size() {
    return this._string.size();
  }

  @Override
  public boolean isEmpty() {
    return this._string.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return this._string.contains(o);
  }

  @NotNull
  @Override
  public Iterator<Byte> iterator() {
    return this._string.iterator();
  }

  @NotNull
  @Override
  public Object[] toArray() {
    return this._string.toArray();
  }

  @NotNull
  @Override
  public <T> T[] toArray(@NotNull T[] a) {
    return this._string.toArray(a);
  }

  @Override
  public boolean add(Byte aByte) {
    return this._string.add(aByte);
  }

  @Override
  public boolean remove(Object o) {
    return this._string.remove(o);
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    return this._string.containsAll(c);
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends Byte> c) {
    return this._string.addAll(c);
  }

  @Override
  public boolean addAll(int index, @NotNull Collection<? extends Byte> c) {
    return this._string.addAll(index, c);
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> c) {
    return this._string.removeAll(c);
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    return this._string.retainAll(c);
  }

  @Override
  public void clear() {
    this._string.clear();;
  }

  @Override
  public boolean equals(Object obj) {
    return LarkyByteArray.class.isAssignableFrom(obj.getClass())
        && (this.compareTo((LarkyByteArray) obj) == 0);
  }

  @Override
  public int hashCode() {
    return FnvHash.getFNV1a(this.getString()).intValue();
  }

  @Override
  public Byte get(int index) {
    return this._string.get(index);
  }

  @Override
  public Byte set(int index, Byte element) {
    return this._string.set(index, element);
  }

  @Override
  public void add(int index, Byte element) {
    this._string.add(index, element);
  }

  @Override
  public Byte remove(int index) {
    return this._string.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return this._string.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return this._string.lastIndexOf(o);
  }

  @NotNull
  @Override
  public ListIterator<Byte> listIterator() {
    return this._string.listIterator();
  }

  @NotNull
  @Override
  public ListIterator<Byte> listIterator(int index) {
    return this._string.listIterator(index);
  }

  @NotNull
  @Override
  public List<Byte> subList(int fromIndex, int toIndex) {
    return this._string.subList(fromIndex, toIndex);
  }

  @Override
  public int compareTo(@NotNull LarkyByteArray o) {
    return UnsignedBytes
        .lexicographicalComparator()
        .compare(toBytes(), o.toBytes());
  }

  @Override
  public Sequence<StarlarkInt> getSlice(Mutability mu, int start, int stop, int step) {
    StarlarkList<StarlarkInt> c = StarlarkList.copyOf(mu,
        this._string.stream()
            .map(Byte::toUnsignedInt)
            .map(StarlarkInt::of)
            .collect(Collectors.toList()));
    return c.getSlice(mu, start, stop, step);
  }
}
