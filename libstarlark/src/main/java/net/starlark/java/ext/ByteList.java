package net.starlark.java.ext;

import com.google.common.base.Ascii;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import org.jetbrains.annotations.NotNull;

public class ByteList implements CharSequence, RandomAccess, Iterable<Byte>, Comparable<ByteList> {

  public static class PY_ISSPACE implements IntPredicate {
    public static final PY_ISSPACE INSTANCE = new PY_ISSPACE();

    // stringlib whitespace =>
    //   - https://github.com/python/cpython/blob/main/Objects/bytesobject.c#L1720
    //   - https://github.com/python/cpython/blob/main/Objects/stringlib/stringdefs.h#L16
    //   - https://github.com/python/cpython/blob/main/Include/cpython/pyctype.h#L27
    //   - https://github.com/python/cpython/blob/main/Python/pyctype.c#L15-L19 + L38
    public static final byte[] PY_CTF_SPACE = (
      "\u0009" + (char) 0x0A + "\u000B" + "\u000C" + (char) 0x0D + "\u0020"
    ).getBytes(StandardCharsets.US_ASCII);

    @Override
    public boolean test(int ch) {
      if (ch > 0x0020) return false;
      long isspace = 1;
      for (byte b : PY_CTF_SPACE) {
        isspace |= (1L << b);
      }
      return ((isspace >> ch) & 1L) != 0;
    }
  }

  public static class IsLowerCase implements IntPredicate {
    public static final IsLowerCase INSTANCE = new IsLowerCase();

    @Override
    public boolean test(int value) {
      return Ascii.isLowerCase((char) value);
    }
  }

  public static class IsUpperCase implements IntPredicate {
    public static final IsUpperCase INSTANCE = new IsUpperCase();

    @Override
    public boolean test(int value) {
      return Ascii.isUpperCase((char) value);
    }
  }

  public static class SubByteFinder implements IntPredicate {

    private final ByteList bytes;

    public SubByteFinder(ByteList bytes) {
      this.bytes = bytes;
    }

    @Override
    public boolean test(int value) {
      return bytes.find((byte) value) != -1;
    }
  }

  SubByteFinder FINDER_PREDICATE = new SubByteFinder(this);

  protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  /**
   * Empty {@code ByteList}.
   */
  public static final ByteList EMPTY = new ByteList(EMPTY_BYTE_ARRAY);

  public static ByteList empty() {
    return EMPTY;
  }

  protected ByteList() {
    this(EMPTY_BYTE_ARRAY, 0, 0);
  }

  protected ByteList(int initialCapacity) {
    this(new byte[initialCapacity], 0, 0);
    ensureCapacity(initialCapacity);
  }

  protected ByteList(byte[] array) {
    this(array, 0, array.length);
  }

  /**
   * Construct an empty list with the given initial capacity.
   *
   * @throws IllegalArgumentException when <i>initialCapacity</i> is negative
   */
  protected ByteList(byte[] array, int offset, int length) {
    if (length < 0) {
      throw new IllegalArgumentException(String.format("capacity < 0 (%s)", length));
    }
    setArrayUnsafe(array);
    offset(offset);
    size(length);
  }

  /* Constants */

  private static final byte[] HEX_CHAR_LOOKUP_TABLE = "0123456789abcdef".getBytes();

  /**
   * The largest possible table capacity.  This value must be
   * exactly 1<<30 to stay within Java array allocation and indexing
   * bounds for power of two table sizes, and is further required
   * because the top two bits of 32bit hash fields are used for
   * control purposes.
   */
  private static final int MAXIMUM_CAPACITY = 1 << 30;

  /* Fields */

  private int offset;

  protected int offset() {
    return offset;
  }

  protected void offset(final int offset) {
    this.offset = offset;
  }

  /**
   * The current size of the list.
   *
   * The size of the list distinct from the length of the array. Think of it as the number of
   * elements set in the list and as a result, represents the number of actual elements in the
   * collection.
   */
  private int size;

  public int size() {
    return size;
  }

  public void size(final int size) {
    ensureCapacity(size);
    this.size = size;
  }

  protected byte[] array;

  /**
   * Get the array container directly. The array can then be passed directly to the target for use.
   *
   * Caller beware: once you use this method, it is recommended not to use this ByteList instance.
   *
   * @return the array container directly
   */
  public byte[] getArrayUnsafe() {
    return array;
  }

  public void setArrayUnsafe(final byte[] array) {
    if (array.length < size()) {
      throw new IllegalArgumentException("Array too small");
    }
    this.array = array;
  }

  /**
   * Returns an array containing all of the elements in this deque in proper sequence (from first to
   * last element).
   *
   * <p>The returned array will be "safe" in that no references to it are
   * maintained by this ByteList instance.  (In other words, this method must allocate a new array).
   * The caller is thus free to modify the returned array.
   *
   * <p>This method acts as bridge between array-based and collection-based
   * APIs.
   *
   * @return an array containing all of the elements in this ByteList instance
   */
  public byte[] toArray() {
    return toArray(null);
  }

  public byte[] toArray(byte[] a) {
    if (a == null || a.length < size()) {
      a = new byte[size()];
    }
    OfByte.unwrap(iterator(), a, 0, a.length);
    return a;
  }

  public byte[] toArray(byte[] dest, int sourcePos, int destPos, int len) {
    if (len == 0) {
      return dest;             // nothing to copy
    }
    OfByte it = iterator();
    if (sourcePos != 0) {
      if (sourcePos != it.advance(sourcePos)) {
        return dest; // cannot copy..
      }
    }
    OfByte.unwrap(it, dest, destPos, len);
    return dest;
  }

  public byte byteAt(int index) {
    // ignoring the potential offset because we need to do range-checking in the
    // substring case anyway.
    return array[index];
  }


  // instantiate / copy interface

  /**
   * Create a {@link ByteList} from the supplied array of byte[] and returns a new ByteList instance
   * that initially wraps the array. The caller *MUST NOT* subsequently modify the array, but the
   * ByteList instance may do so.
   *
   * @param b the source for new ByteList
   * @return the new ByteList
   */
  public static ByteList of(byte... b) {
    if (b.length == 0) {
      return empty();
    }
    return new ByteList(b);
  }

  /**
   * Create a {@link ByteList} from the provided CharSequence with a default ISO_8859_1 encoding.
   *
   * @param s the source for new ByteList
   * @return the new ByteList
   */
  public static ByteList copy(CharSequence s) {
    return copy(CharBuffer.wrap(s).array());
  }

  /**
   * Create a {@link ByteList} from the provided String with a default ISO_8859_1 encoding.
   *
   * @param s the source for new ByteList
   * @return the new ByteList
   */
  public static ByteList copy(String s) {
    return copy(StandardCharsets.ISO_8859_1.encode(CharBuffer.wrap(s)).array());
  }

  /**
   * Create a {@link ByteList} from the provided character array with a default ISO_8859_1
   * encoding.
   *
   * @param s the source for new ByteList
   * @return the new ByteList
   */
  public static ByteList copy(char[] s) {
    byte[] bytes = new byte[s.length];
    for (int i = 0; i < s.length; i++) {
      bytes[i] = (byte) s[i];
    }
    return copy(bytes);
  }

  /**
   * Create a {@link ByteList} from the provided ByteList.
   *
   * @param bytes the source for new ByteList
   * @return the new ByteList
   */
  public static ByteList copy(ByteList bytes) {
    return copy(bytes.copy());
  }

  public static ByteList copy(byte[] bytes) {
    return copy(bytes, 0, bytes.length);
  }

  public static ByteList copy(byte[] bytes, int offset, int size) {
    byte[] copy = new byte[size];
    System.arraycopy(bytes, offset, copy, 0, size);
    return new SubByteList(copy, offset, size);
  }

  public byte[] copy() {
    return copy(size());
  }

  private byte[] copy(final int newLength) {
    final byte[] copy = new byte[newLength];
    final byte[] oldArray = getArrayUnsafe();
    if (oldArray != null) System.arraycopy(oldArray, offset(), copy, 0, size());
    return copy;
  }

  /**
   * Gets the current capacity of the backing array.
   */
  public int capacity() {
    final byte[] array = getArrayUnsafe();
    return array == null ? 0 : array.length;
  }

  public void ensureCapacity(final int minCapacity) {
    final int oldCapacity = capacity();
    if (minCapacity <= oldCapacity) return; // no need to grow

    // grow the array by up to 50% (plus a small constant)
    final int growth = Math.min(oldCapacity / 2 + 16, MAXIMUM_CAPACITY);
    final int newCapacity;
    if (growth > Integer.MAX_VALUE - oldCapacity) {
      // growth would push array over the maximum array size
      newCapacity = Integer.MAX_VALUE;
    } else newCapacity = oldCapacity + growth;
    // ensure the array grows by at least the requested minimum capacity
    final int newLength = Math.max(minCapacity, newCapacity);

    // copy the data into a new array
    setArrayUnsafe(copy(newLength));
  }

  /**
   * Shifts the array to insert space at a specified index.
   *
   * @param index the index where the space should be inserted
   * @param count the number of values to insert
   */
  public void insert(final int index, final int count) {
    int oldSize = size();
    if (index < 0 || index > oldSize) {
      throw new ArrayIndexOutOfBoundsException("Invalid index value");
    }
    if (count > Integer.MAX_VALUE - oldSize) {
      // insertion would push array over the maximum size
      throw new IllegalArgumentException("Too many elements");
    }
    if (count <= 0) {
      throw new IllegalArgumentException("Count must be positive");
    }
    size(oldSize + count);
    if (index < oldSize) {
      final byte[] array = getArrayUnsafe();
      System.arraycopy(array, offset() + index, array, index + count, oldSize - index);
    }
  }

  /**
   * Shifts the array to delete space starting at a specified index.
   *
   * @param index the index where the space should be deleted
   * @param count the number of values to delete
   */
  public void delete(final int index, final int count) {
    int oldSize = size();
    if (index < 0 || index > oldSize) {
      throw new ArrayIndexOutOfBoundsException("Invalid index value");
    }
    if (index + count > oldSize) {
      throw new IllegalArgumentException("Invalid range: index=" + index +
                                           ", count=" + count + ", size=" + oldSize);
    }
    size(oldSize - count);
    if (index + count < oldSize) {
      final byte[] array = getArrayUnsafe();
      System.arraycopy(array, offset() + index + count, array, index, oldSize - index - count);
    }
  }

  // collections

  // Overridden for performance to prevent a ton of boxing
  public boolean contains(final Object o) {
    if (!(o instanceof Byte)) return false;
    final byte value = (Byte) o;
    return contains(value);
  }

  public boolean contains(final byte value) {
    return indexOf(value) >= 0;
  }

  public boolean contains(int value) {
    return contains((byte) value);
  }

  // Overridden for performance to prevent a ton of boxing
  public boolean remove(final Object o) {
    if (!(o instanceof Byte)) return false;
    final byte value = (Byte) o;
    return removeValue(value);
  }

  public boolean remove(final byte value) {
    return removeValue(value);
  }

  /**
   * Removes and returns the item at the specified index. Note that this is equivalent to {@link
   * java.util.List#remove(int)}, but can't have that name because we also have {@link
   * #remove(byte)} that removes a value, rather than an index.
   *
   * @param index the index of the item to remove and return
   * @return the removed item
   */
  public byte removeAt(final int index) {
    final byte removed = get(index);
    delete(index, 1);
    return removed;
  }

  // Overridden for performance to prevent a ton of boxing
  public boolean containsAll(final Collection<?> c) {
    for (final Object o : c) {
      if (!(o instanceof Byte)) return false;
      final byte value = (Byte) o;
      if (indexOf(value) < 0) return false;
    }
    return true;
  }

  // Overridden for performance to prevent a ton of boxing
  public boolean addAll(final int index, final Collection<? extends Byte> c) {
    if (c.size() == 0) return false;
    insert(index, c.size());
    int i = index;
    for (final byte e : c) {
      setValue(i++, e);
    }
    return true;
  }

  /**
   * @param index - where to insert the bytelist
   * @param b     - the bytelist to insert
   * @return return the new size (i.e. the index) of the underlying data structure. this can be used
   * to consecutively add to the list via index
   */
  public int addAll(final int index, final ByteList b) {
    if (b.size() == 0) return size();
    insert(index, b.size());
    int i = index;
    for (final byte e : b) {
      setValue(i++, e);
    }
    return i;
  }

  // Overridden for performance to prevent a ton of boxing
  public boolean removeAll(final Collection<?> c) {
    boolean changed = false;
    for (final Object o : c) {
      if (!(o instanceof Byte)) continue;
      final byte value = (Byte) o;
      final boolean result = removeValue(value);
      if (result) changed = true;
    }
    return changed;
  }

  public boolean removeAll(final ByteList c) {
    boolean changed = false;
    for (final byte value : c) {
      final boolean result = removeValue(value);
      if (result) changed = true;
    }
    return changed;
  }

  public void clear() {
    size(0);
  }

  // -- ByteArray methods --

  public void addValue(final byte value) {
    addValue(size(), value);
  }

  public void addValue(final int index, final byte value) {
    insert(index, 1);
    array[index] = value;
  }

  public boolean removeValue(final byte value) {
    final int index = indexOf(offset() + value);
    if (index < 0) return false;
    delete(index, 1);
    return true;
  }

  /**
   * Private remove method that skips bounds checking and does not return the value removed.
   */
  private void fastRemove(final int index) {
    final int newSize;
    final byte[] array = getArrayUnsafe();

    if ((newSize = size - 1) > index) {
      System.arraycopy(array, offset() + index + 1, array, index, newSize - index);
    }
    size(newSize);
  }

  public byte getValue(final int index) {
    // delegate offset checking because we do range-checking in the substring case anyway.
    return this.byteAt(index);
  }

  public byte setValue(final int index, final byte value) {
    checkBounds(offset() + index);
    final byte oldValue = getValue(index);
    array[offset() + index] = value;
    return oldValue;
  }

  public int indexOf(final byte value) {
    for (int i = 0; i < size(); i++) {
      if (get(i) == value) return i;
    }
    return -1;
  }

  public int indexOf(final int value) {
    return indexOf((byte) value);
  }

  /**
   * Returns the start position of the first occurrence of the specified {@code target} within this
   * ByteList, or {@code -1} if there is no such occurrence.
   *
   * @param target the array to search for as a sub-sequence
   */
  public int indexOf(byte[] target) {
    if (target.length == 0) {
      return 0;
    }

    outer:
    for (int i = 0; i < size() - target.length + 1; i++) {
      for (int j = 0; j < target.length; j++) {
        if (get(i + j) != target[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }

  public int lastIndexOf(final byte value) {
    for (int i = size() - 1; i >= 0; i--) {
      if (get(i) == value) return i;
    }
    return -1;
  }

  public int lastIndexOf(byte[] sub, int start, int end) {
    return lastIndexOf(ByteList.of(sub), start, end);
  }

  public int lastIndexOf(ByteList sub, int start, int end) {
    int subl = sub.size();
    if (subl == 0) {
      return end;
    }

    outer:
    for (int i = end - 1, blen = size(); i >= start; i--) {
      for (int j = 0; j < subl; j++) {
        if (i + j >= blen) {
          continue outer;
        }
        if (get(i + j) != sub.get(j)) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }

  // list
  public byte get(final int index) {
    return getValue(index);
  }

  public byte set(final int index, final Byte element) {
    return setValue(index, element == null ? defaultValue() : element);
  }

  public void add(final int index, final Byte element) {
    addValue(index, element);
  }

  public void add(final Byte element) {
    add(size(), element);
  }

  public void extend(final ByteList list) {
    addAll(size(), list);
  }

  /**
   * Creates a <b>new</b> ByteList and appends the passed list to it.
   *
   * @param list - The list to append to the new copy of the list
   * @return A new list consisting of a copy of this {@link ByteList} appended with list.
   */
  public ByteList mergedCopy(final ByteList list) {
    final ByteList bytes = new ByteList(size() + list.size());
    int idx = bytes.addAll(0, this);
    idx = bytes.addAll(idx, list);
    assert idx == (size() + list.size());
    return bytes;
  }

  public byte defaultValue() {
    return 0;
  }

  // collection
  public boolean isEmpty() {
    return size() == 0;
  }

  /// internal

  /**
   * Checks that the index is less than the size of the array.
   */
  protected void checkBounds(final int index) {
    checkBounds(index, size());
  }

  /**
   * Checks that the given index falls within the specified array size.
   *
   * @param index the index position to be tested
   * @param size  the length of the array
   * @throws IndexOutOfBoundsException if the index does not fall within the array.
   */
  protected void checkBounds(int index, int size) {
    if ((index | (size - (index + 1))) < 0) {
      if (index < 0) {
        throw new ArrayIndexOutOfBoundsException("Index < 0: " + index);
      }
      throw new ArrayIndexOutOfBoundsException("Index > length: " + index + ", " + size);
    }
  }

  /**
   * Checks that the given range falls within the bounds of an array
   *
   * @param startIndex the start index of the range (inclusive)
   * @param endIndex   the end index of the range (exclusive)
   * @param size       the size of the array.
   * @return the length of the range.
   * @throws IndexOutOfBoundsException some or all of the range falls outside of the array.
   */
  protected int checkRange(int startIndex, int endIndex, int size) {
    final int length = endIndex - startIndex;
    if ((startIndex | endIndex | length | (size - endIndex)) < 0) {
      if (startIndex < 0) {
        throw new IndexOutOfBoundsException("Beginning index: " + startIndex + " < 0");
      }
      if (endIndex < startIndex) {
        throw new IndexOutOfBoundsException(
          "Beginning index larger than ending index: " + startIndex + ", " + endIndex);
      }
      // endIndex >= size
      throw new IndexOutOfBoundsException("End index: " + endIndex + " >= " + size);
    }
    return length;
  }

  // CharSequence

  @Override
  public int length() {
    return size();
  }

  @Override
  public char charAt(int index) {
    return (char) byteAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return substring(start, end);
  }

  @Override
  public String toString() {
    return Arrays.toString(Arrays.copyOfRange(array, offset, offset + size));
  }

  // comparable
  @Override
  public int compareTo(@NotNull ByteList o) {
    final int thisSize = size();
    final int oSize = o.size();
    final int minLength = Math.min(thisSize, oSize);
    for (int i = 0; i < minLength; i++) {
      int result = Byte.toUnsignedInt(get(i)) - Byte.toUnsignedInt(o.get(i));
      if (result != 0) {
        return result;
      }
    }
    return thisSize - oSize;
  }

  // hashCode()
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ByteList)) {
      if (o instanceof Iterable) {
        return iterator().itemsEqual(((Iterable<?>) o).iterator());
      }
      return false;
    }
    if (this == o) {
      return true;
    }
    return this.compareTo((ByteList) o) == 0;
  }

// ByteList python buffer-like methods

  /**
   * Common implementation for find, rfind, index, rindex.
   *
   * @param forward true if we want to return the first matching index.
   */
  int find(boolean forward, byte[] sub, int start, int end) {
    if (sub.length == 0 && start > end) {
      return -1;
    }
    ByteList subRange = substring(start, end);
    int subpos = forward
                   ? subRange.indexOf(sub)
                   : subRange.lastIndexOf(sub, 0, subRange.size());

    return subpos < 0 ? subpos : subpos + start;
  }


  public String hex() {
    return hex((byte) -1);
  }

  public String hex(byte separator) {
    return hex(separator, -1);
  }

  public String hex(ByteList separator) {
    return hex(separator, -1);
  }

  public String hex(ByteList separator, int bytesPerSep) {
    if(separator == null || separator.isEmpty()) {
      throw new IllegalArgumentException("separator cannot be empty");
    }
    if(separator.size() != 1) {
      throw new IllegalArgumentException("separator must be of size 1");
    }
    return hex(separator.get(0), bytesPerSep);
  }

  public String hex(byte separator, int bytesPerSep) {
    final int srcLength = size();
    if (srcLength == 0) { // early-exit
      return "";
    }

    int absBytesPerSep = Math.abs(bytesPerSep);
    int resultlen = 0;
    if (bytesPerSep != 0) {
      resultlen = (srcLength - 1) / absBytesPerSep;
    }

    resultlen += srcLength * 2;
    if (absBytesPerSep >= srcLength) {
      bytesPerSep = absBytesPerSep = 0;
    }
    byte[] retbuf = new byte[resultlen];
    int i,j;
    for (i=j=0; i < srcLength; ++i) {
      int c = get(i) & 0xFF;
      retbuf[j++] = HEX_CHAR_LOOKUP_TABLE[c >>> 4];
      retbuf[j++] = HEX_CHAR_LOOKUP_TABLE[c & 0x0f];
      if ((bytesPerSep != 0) && (i < (srcLength - 1))) {
          int anchor;
          anchor = (bytesPerSep > 0) ? (srcLength - 1 - i) : (i + 1);
          if (anchor % absBytesPerSep == 0 && separator >= 0) {
              retbuf[j++] = separator;
          }
      }
    }
    return new String(retbuf, 0, j, StandardCharsets.US_ASCII);
  }

  public int count(byte[] bytes) {
    return count(bytes, 0);
  }

  public int count(byte[] bytes, int i) {
    return count(bytes, i, size());
  }

  public int count(byte i) {
    return count(i, 0);
  }

  public int count(byte i, int start) {
    return count(i, start, size());
  }

  public int count(byte i, int start, int end) {
    return count(new byte[]{i}, start, end);
  }

  public int count(byte[] sub, int start, int end) {
    if (sub == null) {
      throw new IllegalArgumentException("argument should be integer or bytes-like object, not 'null'");
    }
    int length = size();
    int sublength = sub.length;

    //If the sub string is longer than the value string a match cannot exist
    if (length < sublength) {
      return 0;
    }
    //Clamp value to negative positive range of indices
    int istart = Math.max(-length, Math.min(length, start));
    int iend = Math.max(-length, Math.min(length, end));
    //Compute wrapped index for negative values(Python modulo operation)
    if (istart < 0) {
      istart = ((istart % length) + length) % length;
    }
    if (iend < 0) {
      iend = ((iend % length) + length) % length;
    }

    int count = 0;
    boolean found_match;
    //iend-sub.length+1 accounts for the inner loop comparison to
    //  end comparisons at (i+j)==iend
    for (int i = istart; i < ((iend - sublength) + 1); i++) {
      found_match = true;
      for (int j = 0; j < sublength; j++) {
        if (get(i + j) != sub[j]) {
          found_match = false;
          break;
        }
      }
      if (found_match) {
        count++;
        //skip ahead by the length of the sub_array (-1 to account for i++ in outer loop)
        //this consumes the match from the value array
        i += sublength - 1;
      }
    }
    return count;
  }

  public int find(byte[] bytes) {
    return find(bytes, 0, size());
  }

  public int find(byte[] bytes, int start) {
    return find(bytes, start, size());
  }

  public int find(byte[] bytes, int start, int end) {
    return find(true, bytes, start, end);
  }

  public int find(byte i) {
    return find(i, 0, size());
  }

  public int find(byte i, int start) {
    return find(i, start, size());
  }

  public int find(byte i, int start, int end) {
    int subpos = substring(start, end).indexOf(i);
    return subpos < 0 ? subpos : subpos + start;
  }

  /**
   * Joins a set of byte arrays into a larger array. The {@code interlude} is placed between each of
   * the elements, but not at the beginning or end. In the case that the list is empty or {@code
   * null}, a zero-length byte array will be returned.
   *
   * @param parts the pieces to be joined. May be {@code null}, but does not allow for elements in
   *              the list to be {@code null}.
   * @return a newly created concatenation of the input
   */
  public ByteList join(byte[][] parts) {
    final int partsLength;
    if (parts == null || (partsLength = parts.length) == 0) {
      return empty();
    }

    if (getArrayUnsafe() == null) {
      setArrayUnsafe(EMPTY_BYTE_ARRAY);
    }

    int elementTotals = 0;
    for (byte[] e : parts) {
      elementTotals += e.length;
    }

    int interludeSize = size();
    byte[] dest = new byte[(interludeSize * (partsLength - 1)) + elementTotals];

    int startByte = 0;
    int index = 0;
    for (byte[] part : parts) {
      final int length = part.length;
      if (length > 0) {
        System.arraycopy(part, 0, dest, startByte, length);
        startByte += length;
      }
      if (index < partsLength - 1 && interludeSize > 0) {
        // If this is not the last element, append the interlude
        System.arraycopy(getArrayUnsafe(), 0, dest, startByte, interludeSize);
        startByte += interludeSize;
      }
      index++;
    }
    return ByteList.of(dest);
  }

  public ByteList join(ByteList... parts) {
    final int partsLength;
    if (parts == null || (partsLength = parts.length) == 0) {
      return empty();
    }
    final byte[][] bytes = new byte[partsLength][];
    for (int i = 0; i < partsLength; i++) {
      bytes[i] = parts[i].getArrayUnsafe();
    }
    return join(bytes);
  }

  public ByteList[] partition(byte[] separator) {
    int i = find(true, separator, 0, size());
    if (i == -1) {
      return new ByteList[]{this, empty(), empty()};
    }
    return new ByteList[]{
      substring(0, i),
      ByteList.of(separator),
      substring(i + separator.length, size())
    };
  }

  public ByteList[] rpartition(byte[] separator) {
    int i = find(false, separator, 0, size());
    if (i == -1) {
      return new ByteList[]{empty(), empty(), this};
    }
    return new ByteList[]{
      substring(0, i),
      ByteList.of(separator),
      substring(i + separator.length, size())
    };
  }

  public ByteList replace(byte[] oldBytes, byte[] newBytes) {
    return replace(oldBytes, newBytes, Integer.MAX_VALUE);
  }

  public ByteList replace(byte[] oldBytes, byte[] newBytes, int count) {
    int i, j, pos, maxcount = count, subLen = oldBytes.length, repLen = newBytes.length;
    final ByteList replacement = new ByteList(size() + (repLen * size()));
    int resultLen = 0;
    i = 0;
    while (maxcount-- > 0) {
      pos = find(true, oldBytes, i, size());
      if (pos < 0) {
        break;
      }
      j = pos;
      resultLen = replacement.addAll(resultLen, substring(i, j));
      resultLen = replacement.addAll(resultLen, ByteList.of(newBytes));
      i = j + subLen;
    }

    if (i == 0) {
      return ByteList.copy(substring(0, size()).toArray());
    }

    resultLen = replacement.addAll(resultLen, substring(i, size()));
    assert resultLen == replacement.size();
    return replacement;
  }

  public boolean startsWith(byte[] prefix) {
    if (prefix.length > size()) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (get(i) != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  static boolean isdigit(byte ch) {
    return ch >= '0' && ch <= '9';
  }

  public boolean isdigit() {
    if (isEmpty()) {
      return false;
    }
    for (byte ch : this) {
      if (!isdigit(ch)) {
        return false;
      }
    }
    return true;
  }


  public boolean isascii() {
    if (isEmpty()) {
      return true;
    }
    for (byte b : this) {
      if (b < 0) {  // remember, bytes are -127 to 127 and 0x7F is max ascii
        return false;
      }
    }
    return true;
  }

  static boolean isalpha(byte ch) {
    return Ascii.isUpperCase((char) ch) || Ascii.isLowerCase((char) ch);
  }

  public boolean isalpha() {
    if (isEmpty()) {
      return false;
    }
    for (byte ch : this) {
      if (!isalpha(ch)) {
        return false;
      }
    }
    return true;
  }

  static boolean isalnum(byte ch) {
    return isalpha(ch) || isdigit(ch);
  }

  public boolean isalnum() {
    if (isEmpty()) {
      return false;
    }
    for (byte ch : this) {
      if (!isalnum(ch)) {
        return false;
      }
    }
    return true;
  }

  public ByteList swapcase() {
    byte[] bytes = toArray();
    for (int idx = 0; idx < size(); ++idx) {
      char lc = (char) bytes[idx];
      if (Ascii.isUpperCase(lc)) {
        bytes[idx] = (byte) Ascii.toLowerCase(lc);
      } else {
        bytes[idx] = (byte) Ascii.toUpperCase(lc);
      }
    }
    return ByteList.of(bytes);
  }

  public boolean istitle() {
    final int inputLength = size();
    /* Special case for empty strings */
    if (inputLength == 0) {
      return false;
    }
    boolean isTitleCase = false;
    boolean previousIsCased = false;
    for (byte value : this) {
      char b = (char) value;
      if (Ascii.isUpperCase(b)) {
        if (previousIsCased) {
          return false;
        }
        previousIsCased = true;
        isTitleCase = true;
      } else if (Ascii.isLowerCase(b)) {
        if (!previousIsCased) {
          return false;
        }
        previousIsCased = true;
        isTitleCase = true;
      } else {
        previousIsCased = false;
      }
    }
    return isTitleCase;
  }

  public ByteList zfill(int width) {
    final int len = size();
    if (len >= width) {
      return this;
    }

    final int fill = width - len;
    ByteList p = pad(fill, 0, (byte) '0');

    if (len == 0) {
      return p;
    }

    if (p.get(fill) == '+' || p.get(fill) == '-') {
      /* move sign to beginning of string */
      p.setValue(0, p.get(fill));
      p.setValue(fill, (byte) '0');
    }
    return p;
  }

  private ByteList pad(int left, int right, byte padChar) {
    final int len = size();

    left = Math.max(left, 0);
    right = Math.max(right, 0);
    if (left == 0 && right == 0) {
      return this;
    }

    byte[] u = new byte[left + len + right];

    if (left > 0) {
      Arrays.fill(u, 0, left, padChar);
    }

    for (int i = left, j = 0; i < (left + len); j++, i++) {
      u[i] = get(j);
    }

    if (right > 0) {
      Arrays.fill(u, left + len, u.length, padChar);
    }
    return ByteList.of(u);
  }

  public ByteList title() {
    final int len = size();
    boolean capitalizeNext = true;
    final ByteList titleCased = ByteList.of(copy());
    for (int idx = 0; idx < len; ++idx) {
      byte lc = get(idx);
      if (!isalpha(lc)) {
        titleCased.setValue(idx, lc);
        capitalizeNext = true;
      } else if (capitalizeNext) {
        titleCased.setValue(idx, (byte) Ascii.toUpperCase((char) lc));
        capitalizeNext = false;
      } else {
        titleCased.setValue(idx, (byte) Ascii.toLowerCase((char) lc));
      }
    }
    return titleCased;
  }

  public ByteList lower() {
    final ByteList lowerCased = ByteList.copy(this);
    final int length = lowerCased.size();
    for (int i = 0; i < length; i++) {
      byte b = (byte) Ascii.toLowerCase((char) lowerCased.get(i));
      lowerCased.setValue(i, b);
    }
    return lowerCased;
  }

  public ByteList upper() {
    final ByteList upperCased = ByteList.copy(this);
    final int length = upperCased.size();
    for (int i = 0; i < length; i++) {
      byte b = (byte) Ascii.toUpperCase((char) upperCased.get(i));
      upperCased.setValue(i, b);
    }
    return upperCased;
  }

  public ByteList capitalize() {
    final ByteList capitalized = ByteList.copy(this);
    final int length = capitalized.size();
    for (int i = 0; i < length; i++) {
      byte b = capitalized.get(i);
      if (b < 127 && b > 32) {
        char c = (char) b;
        if (i == 0) {
          c = Ascii.toUpperCase(c);
        } else {
          c = Ascii.toLowerCase(c);
        }
        capitalized.setValue(i, (byte) c);
      } else {
        capitalized.setValue(i, b);
      }
    }
    return capitalized;
  }

  private static final byte[] SPACE = new byte[]{(byte) ' '};

  public ByteList center(int width) {
    return center(width, SPACE);
  }

  public ByteList center(int width, byte[] fillChar) {
    return center(width, ByteList.of(fillChar));
  }

  public ByteList center(int width, ByteList fillChar) {
    int marg = width - size();
    int left = marg / 2 + (marg & width & 1);
    return pad(left, marg - left, fillChar.get(0));
  }

  public ByteList rjust(int width) {
    return rjust(width, SPACE);
  }

  public ByteList rjust(int width, byte[] fillChar) {
    return rjust(width, ByteList.of(fillChar));
  }

  public ByteList rjust(int width, ByteList fillChar) {
    if (fillChar.isEmpty() || (width - size() <= 0)) {
      return this;
    }
    int l = width - size();
    int resLen = l + size();
    byte[] res = new byte[resLen];
    Arrays.fill(res, 0, l, fillChar.byteAt(0));
    for (int i = l, j = 0; i < (size() + l); j++, i++) {
      res[i] = this.byteAt(j);
    }
    return ByteList.of(res);
  }

  public ByteList ljust(int width) {
    return ljust(width, SPACE);
  }

  public ByteList ljust(int width, byte[] fillChar) {
    return ljust(width, ByteList.of(fillChar));
  }

  public ByteList ljust(int width, ByteList fillChar) {
    if (fillChar.isEmpty() || (width - size() <= 0)) {
      return this;
    }
    int l = width - size();
    int resLen = l + size();
    byte[] res = new byte[resLen];
    System.arraycopy(array, 0, res, 0, size());
    Arrays.fill(res, size(), resLen, fillChar.byteAt(0));
    return ByteList.of(res);
  }

  public ByteList[] splitlines() {
    return splitlines(false);
  }

  public ByteList[] splitlines(boolean keepEnds) {
    final int length = size();
    int start = 0;
    // Escape analysis should only allocate this on the heap
    List<ByteList> bl = new ArrayList<>();
    for (int i = 0; i < length; i++) {
      if (get(i) == '\n' || get(i) == '\r') {
        int end = i;
        if (get(i) == '\r' && i + 1 != length && get(i + 1) == '\n') {
          i++;
        }
        if (keepEnds) {
          end = i + 1;
        }
        bl.add(substring(start, start + (end - start)));
        start = i + 1;
      }
    }
    if (start == length) {
      return bl.toArray(new ByteList[0]);
    }
    // We have remaining parts, so let's process it.
    bl.add(substring(start, start + (length - start)));
    return bl.toArray(new ByteList[0]);
  }

  public ByteList translate(byte[] table) {
    return translate(table, empty());
  }

  public ByteList translate(byte[] table, byte[] delete) {
    return translate(table, ByteList.of(delete));
  }

  public ByteList translate(byte[] table, ByteList delete) {
    int dellen = delete.size();
    boolean changed = false;

    if (table != null) {
      if (table.length != 256) {
        throw new IllegalArgumentException(
          String.format(
            "translation table must be 256 characters long. length of table was %d",
            table.length));
      }
    }

    final int totalSize = size();

    if (dellen == 0 && table != null) {
      byte[] result = new byte[totalSize];
      /* If no deletions are required, use faster code */
      for (int i = 0; i < totalSize; i++) {
        byte c = byteAt(i);
        byte v = table[c];
        if (!changed && c != v) {
          changed = true;
        }
        result[i] = v;
      }
      if (!changed) {
        return this;
      }
      return ByteList.of(result);
    }

    boolean[] toDelete = new boolean[256];
    for (int i = 0; i < 256; i++) {
      toDelete[i] = false;
    }
    for (byte b : delete) {
      toDelete[b] = true;
    }

    int resultLen = 0;
    byte[] result = new byte[totalSize];

    for (int i = 0; i < totalSize; i++) {
      byte c = byteAt(i);
      if (!toDelete[c]) {
        byte v = table == null ? c : table[c];
        if (!changed && c != v) {
          changed = true;
        }
        result[resultLen] = v;
        resultLen++;
      }
    }
    if (!changed && resultLen == totalSize) {
      return this;
    }
    // optimize for pre-allocated if resultLen = 0
    if (resultLen == 0) {
      return empty();
    }
    return ByteList.of(result).substring(0, resultLen);
  }

  public ByteList expandtabs() {
    return expandtabs(8);
  }

  public ByteList expandtabs(int tabsize) {
    if (size() == 0) {
      return empty();
    }
    final int length = size();
    final int max = Integer.MAX_VALUE;
    int i = 0, j = 0;
    for (int i1 = 0; i1 < length; i1++) {
      byte p = get(i1);
      if (p == (byte) '\t') {
        if (tabsize > 0) {
          int incr = tabsize - (j % tabsize);
          if (j > max - incr) {
            throw new ArrayIndexOutOfBoundsException("result too long");
          }
          j += incr;
        }
      } else {
        if (j > max - 1) {
          throw new ArrayIndexOutOfBoundsException("result too long");
        }
        j++;
        if (p == (byte) '\n' || p == (byte) '\r') {
          if (i > max - j) {
            throw new ArrayIndexOutOfBoundsException("result too long");
          }
          i += j;
          j = 0;
        }
      }
    }
    if (i > max - j) {
      throw new ArrayIndexOutOfBoundsException("result too long");
    }

    byte[] q = new byte[i + j];
    j = 0;
    int idx = 0;
    for (int i1 = 0; i1 < length; i1++) {
      byte p = get(i1);
      if (p == (byte) '\t') {
        if (tabsize > 0) {
          i = tabsize - (j % tabsize);
          j += i;
          while (i-- > 0) {
            q[idx++] = (byte) ' ';
          }
        }
      } else {
        j++;
        q[idx++] = p;
        if (p == (byte) '\n' || p == (byte) '\r') {
          j = 0;
        }
      }

    }
    return ByteList.of(q);
  }

  public ByteList[] split() {
    return split(empty());
  }

  public ByteList[] split(ByteList sep) {
    return split(sep, Integer.MAX_VALUE);
  }

  public ByteList[] split(ByteList sep, int maxSplit) {
    if (maxSplit == -1) {
      maxSplit = Integer.MAX_VALUE;
    }
    List<ByteList> split;
    if (sep == null || sep.isEmpty()) {
      // on whitespace
      split = splitOnWhitespace(maxSplit);
    } else {
      split = splitOnSep(sep, maxSplit);
    }
    return split.toArray(new ByteList[0]);
  }

  public ByteList[] rsplit() {
    return rsplit(empty());
  }

  public ByteList[] rsplit(ByteList sep) {
    return rsplit(sep, Integer.MAX_VALUE);
  }

  public ByteList[] rsplit(ByteList sep, int maxSplit) {
    if (maxSplit == -1) {
      maxSplit = Integer.MAX_VALUE;
    }
    List<ByteList> rightSplit;
    if (sep == null || sep.isEmpty()) {
      // on whitespace
      rightSplit = rightSplitOnWhitespace(maxSplit);
    } else {
      rightSplit = rightSplitOnSep(sep, maxSplit);
    }
    return rightSplit.toArray(new ByteList[0]);
  }

  List<ByteList> rightSplitOnWhitespace(int maxCount) {
    List<ByteList> list = new ArrayList<>();
    final int srcLength = size();
    int i, j;
    i = srcLength - 1;
    while (maxCount-- > 0) {
      while (i >= 0 && PY_ISSPACE.INSTANCE.test(charAt(i)))
        i--;
      if (i < 0) break;
      j = i;
      i--;
      while (i >= 0 && !PY_ISSPACE.INSTANCE.test(charAt(i)))
        i--;
      if (j == srcLength - i && i < 0) {
        /* No whitespace in str_obj, so just use it as list[0] */
        list.add(substring(0));
        break;
      }
      list.add(substring(i + 1, j + 1));
    }
    if (i >= 0) {
      /* Only occurs when maxCount was reached */
      /* Skip any remaining whitespace and copy to end of string */
      while (i >= 0 && PY_ISSPACE.INSTANCE.test(charAt(i)))
        i--;
      if (i >= 0) {
        list.add(substring(0, i + 1));
      }
    }
    Collections.reverse(list);
    return list;
  }

  List<ByteList> rightSplitOnSep(ByteList sep, int maxCount) {
    if (sep.isEmpty()) {
      throw new UnsupportedOperationException("empty separator not supported");
    }
    int pos, end, sepLen = sep.size();
    final int srcLength = size();
    List<ByteList> list = new ArrayList<>();
    if (sepLen == 1) {
      pos = end = srcLength - 1;
      while ((pos >= 0) && (maxCount-- > 0)) {
        for (; pos >= 0; pos--) {
          if (get(pos) == sep.get(0)) {
            list.add(substring(pos + 1, end + 1));
            end = pos = pos - 1;
            break;
          }
        }
      }
      if (end >= -1) {
        list.add(substring(0, end + 1));
      }
      Collections.reverse(list);
      return list;
    }
    end = srcLength;
    while (true) {
      pos = lastIndexOf(sep, 0, end - 1);
      if (pos < 0 || maxCount-- == 0) {
        list.add(substring(0, end));
        break;
      }
      // if we're going to copy beyond the end of the array, copy to the end
      // and just return
      if (pos + sepLen > end) {
        list.add(substring(pos, end));
        break;
      }
      list.add(substring(pos + sepLen, end));
      end = pos;
    }
    Collections.reverse(list);
    return list;
  }

  List<ByteList> splitOnWhitespace(int maxCount) {
    List<ByteList> list = new ArrayList<>();
    final int srcLength = size();
    int i = 0, j;
    while ((maxCount--) > 0) {
      while (i < srcLength && PY_ISSPACE.INSTANCE.test(charAt(i)))
        i++;
      if (i == srcLength) break;
      j = i;
      i++;
      while (i < srcLength && !PY_ISSPACE.INSTANCE.test(charAt(i)))
        i++;
      if (j == 0 && i == srcLength) {
        /* No whitespace in str_obj, so just use it as list[0] */
        list.add(substring(0));
        break;
      }
      list.add(substring(j, i));
    }
    if (i < srcLength) {
      /* Only occurs when maxCount was reached */
      /* Skip any remaining whitespace and copy to end of string */
      while (i < srcLength && PY_ISSPACE.INSTANCE.test(charAt(i)))
        i++;
      if (i != srcLength) {
        list.add(substring(i, srcLength));
      }
    }
    return list;
  }

  List<ByteList> splitOnSep(ByteList sep, int maxsplit) {
    int i, j, pos, maxCount = maxsplit, sepLen = sep.size();
    final byte[] sepBytes = sep.toArray();
    List<ByteList> list = new ArrayList<>();
    i = 0;
    while ((maxCount--) > 0) {
      pos = find(true, sepBytes, i, size());
      if (pos < 0) {
        break;
      }
      j = pos;
      list.add(substring(i, j));
      i = j + sepLen;
    }
    list.add(substring(i));
    return list;
  }

  public ByteList strip() {
    return strip(null);
  }

  enum StripDirection {
    LEFTSTRIP, RIGHTSTRIP, BOTHSTRIP
  }

  ByteList strip(StripDirection striptype, IntPredicate sepobj) {
    if (isEmpty()) {
      return empty();
    }
    int len = size();
    int i = 0, j;
    if (striptype != StripDirection.RIGHTSTRIP) {
      while (i < len && sepobj.test(get(i))) {
        i++;
      }
    }

    j = len;
    if (striptype != StripDirection.LEFTSTRIP) {
      do {
        j--;
      } while (j >= i && sepobj.test(get(j)));
      j++;
    }

    if (i == 0 && j == len) {
      return this;
    }
    return substring(i, i + (j - i));
  }

  public ByteList strip(ByteList chars) {
    IntPredicate finder = PY_ISSPACE.INSTANCE;
    if (chars != null && !chars.isEmpty()) {
      finder = chars.FINDER_PREDICATE;
    }
    return strip(StripDirection.BOTHSTRIP, finder);
  }

  public ByteList lstrip() {
    return lstrip(null);
  }

  public ByteList lstrip(ByteList chars) {
    IntPredicate finder = PY_ISSPACE.INSTANCE;
    if (chars != null && !chars.isEmpty()) {
      finder = chars.FINDER_PREDICATE;
    }
    return strip(StripDirection.LEFTSTRIP, finder);
  }

  public ByteList rstrip() {
    return rstrip(null);
  }

  public ByteList rstrip(ByteList chars) {
    IntPredicate finder = PY_ISSPACE.INSTANCE;
    if (chars != null && !chars.isEmpty()) {
      finder = chars.FINDER_PREDICATE;
    }
    return strip(StripDirection.RIGHTSTRIP, finder);
  }

  public int rfind(ByteList bytes) {
    return rfind(bytes, 0, size());
  }

  public int rfind(ByteList bytes, int start) {
    return rfind(bytes, start, size());
  }

  public int rfind(ByteList bytes, int start, int end) {
    // TODO(mahmoudimus): should we check ranges here?
    //  if end == -1, basically no restriction right?
    if (end == -1) {
      end = size();
    }
    return find(false, bytes.getArrayUnsafe(), start, end);
  }

  public int rfind(byte i) {
    return rfind(i, 0, size());
  }

  public int rfind(byte i, int start) {
    return rfind(i, start, size());
  }

  public int rfind(byte i, int start, int end) {
    int subpos = substring(start, end).lastIndexOf(i);
    return subpos < 0 ? subpos : subpos + start;
  }

  public ByteList removeprefix(ByteList prefix) {
    final int self_len = size();
    final int prefix_len = prefix.size();
    if (self_len >= prefix_len && prefix_len > 0 && startsWith(prefix)) {
      return ByteList.of(substring(prefix_len, prefix_len + (self_len - prefix_len)).toArray());
    }
    return this;
  }

  public ByteList removesuffix(ByteList suffix) {
    final int self_len = size();
    final int suffix_len = suffix.size();
    if (self_len >= suffix_len && suffix_len > 0 && endsWith(suffix)) {
      return ByteList.of(substring(0, self_len - suffix_len).toArray());
    }
    return this;
  }

  private boolean sequenceSatisfies(IntPredicate falsePredicate, IntPredicate truePredicate) {
    final int inputLength = size();
    if (inputLength == 0) {
      return false;
    }
    boolean result = false;

    for (byte ch : this) {
      if (falsePredicate.test(ch)) {
        return false;
      } else if (!result && truePredicate.test(ch)) {
        result = true;
      }
    }
    return result;
  }

  public boolean isupper() {
    return sequenceSatisfies(IsLowerCase.INSTANCE, IsUpperCase.INSTANCE);
  }

  public boolean islower() {
    return sequenceSatisfies(IsUpperCase.INSTANCE, IsLowerCase.INSTANCE);
  }

  public boolean isspace() {
    return isspace(PY_ISSPACE.INSTANCE);
  }

  public boolean isspace(IntPredicate checker) {
    final int inputLength = size();
    /* Shortcut for single character strings */
    if (inputLength == 1 && checker.test(get(0))) {
      return true;
    }

    /* Special case for empty strings */
    if (inputLength == 0) {
      return false;
    }

    for (byte b : this) {
      if (!checker.test(b)) {
        return false;
      }
    }
    return true;
  }

  // iterable

  /**
   * Represents an operation that accepts a single {@code byte}-valued argument and returns no
   * result. This is the primitive type specialization of {@code Consumer} for {@code byte}.
   *
   * Unlike most other functional interfaces, {@code ByteConsumer} is expected to operate via
   * side-effects. This is a functional interface whose functional method is {@link #accept(byte)}.
   */
  @FunctionalInterface
  public interface ByteConsumer {
    /**
     * Performs this operation on the given argument.
     *
     * @param value the input argument
     */
    void accept(final byte value);

    /**
     * Returns a composed {@code ByteConsumer} that performs, in sequence, this operation followed
     * by the {@code after} operation.
     * <p>
     * If {@code after} is {@code null}, a {@code NullPointerException} will be thrown.
     * <p>
     * If performing either operation throws an exception, it is relayed to the caller of the
     * composed operation.
     * <p>
     * If performing this operation throws an exception, the {@code after} operation will not be
     * performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code ByteConsumer} that performs, in sequence, this operation followed
     * by the {@code after} operation
     * @throws NullPointerException thrown if, and only if, {@code after} is {@code null}
     */
    default ByteConsumer andThen(final ByteConsumer after) {
      Objects.requireNonNull(after, "after == null");

      return (byte t) -> {
        accept(t);

        after.accept(t);
      };
    }
  }

  /**
   * An {@code Iterator} specialized for {@code byte} values.
   *
   * This interface extends {@code PrimitiveIterator<T, T_CONS>}, so that we can return an unboxed
   * {@code byte}.
   */
  public interface OfByte extends PrimitiveIterator<Byte, ByteConsumer> {

    /**
     * Returns the next {@code byte} element in the iteration.
     *
     * If the iteration has no more elements, a {@code NoSuchElementException} will be thrown.
     *
     * @return the next {@code byte} element in the iteration
     * @throws NoSuchElementException thrown if, and only if, the iteration has no more elements
     */
    byte nextByte();

    /**
     * {@inheritDoc}
     */
    @Override
    default Byte next() {
      // Boxing calls Byte.valueOf(byte), which does not instantiate.
      return nextByte();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default void forEachRemaining(final Consumer<? super Byte> action) {
      if (action instanceof ByteConsumer) {
        forEachRemaining((ByteConsumer) (action));
      } else {
        Objects.requireNonNull(action);
        forEachRemaining((ByteConsumer) (action::accept));
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default void forEachRemaining(final ByteConsumer action) {
      Objects.requireNonNull(action);

      while (hasNext()) {
        action.accept(nextByte());
      }
    }

    /**
     * Unwraps an iterator into an array starting at a given offset for a given number of elements.
     *
     * <p>This method iterates over the given {@link ByteList.OfByte} iterator and stores the
     * elements
     * returned, up to a maximum of {@code length}, in the given array starting at {@code offset}.
     * The number of actually unwrapped elements is returned (it may be less than {@code max} if the
     * iterator emits less than {@code max} elements).
     *
     * @param i      a {@link ByteList.OfByte} iterator iterator.
     * @param array  an array to contain the output of the iterator.
     * @param offset the first element of the array to be returned.
     * @param max    the maximum number of elements to unwrap.
     * @return the number of elements unwrapped.
     */
    static int unwrap(final OfByte i, final byte[] array, int offset, final int max) {
      if (max < 0)
        throw new IllegalArgumentException("The maximum number of elements (" + max + ") is negative");
      if (offset < 0 || offset + max > array.length) throw new IllegalArgumentException();
      int j = max;
      while (j-- != 0 && i.hasNext()) array[offset++] = i.nextByte();
      return max - j - 1;
    }

    /**
     * Determines whether this iterator is equal to another iterator's elements in the same order.
     * More specifically, this method returns {@code true} if this iterator and {@code o} contain
     * the same number of elements and every element of this iterator is equal to the corresponding
     * element of {@code o}.
     *
     * <p>Note that this will modify the supplied iterators, since they will have been advanced
     * some
     * number of elements forward.
     */
    default boolean itemsEqual(@NotNull Iterator<?> o) {
      return itemsEqual(this, o);
    }

    /**
     * Determines whether iterator1 is equal to iterator2's elements in the same order. More
     * specifically, this method returns {@code true} if {@code iterator1} and {@code iterator2}
     * contain the same number of elements and every element of {@code iterator1} is equal to the
     * corresponding element of {@code iterator2}.
     *
     * <p>Note that this will modify the supplied iterators, since they will have been advanced
     * some
     * number of elements forward.
     */
    static boolean itemsEqual(@NotNull Iterator<?> iterator1, @NotNull Iterator<?> iterator2) {
      while (iterator1.hasNext()) {
        if (!iterator2.hasNext()) {
          return false;
        }
        Object o1 = iterator1.next();
        Object o2 = iterator2.next();
        if (!Objects.equals(o1, o2)) {
          return false;
        }
      }
      return !iterator2.hasNext();
    }

    /**
     * Calls {@code next()} on this iterator, either {@code numberToAdvance} times or until {@code
     * hasNext()} returns {@code false}, whichever comes first.
     */
    default int advance(int numberToAdvance) {
      if (numberToAdvance < 0)
        throw new IllegalArgumentException("numberToAdvance (" + numberToAdvance + ") is negative!");
      int i = 0;
      while (i < numberToAdvance && hasNext()) {
        next();
        i++;
      }
      return i;
    }

  }

  /**
   * Return a {@link OfByte} over the bytes in the ByteString. To avoid auto-boxing, you may get the
   * iterator manually and call {@link OfByte#nextByte()}.
   *
   * @return the iterator
   */
  @Override
  public OfByte iterator() {
    return new OfByte() {
      private int position = 0;
      private final int limit = size();

      @Override
      public byte nextByte() {
        int currentPos = position;
        if (currentPos >= limit) {
          throw new NoSuchElementException();
        }
        position = currentPos + 1;
        return byteAt(currentPos);
      }

      @Override
      public boolean hasNext() {
        return position < limit;
      }
    };
  }

  /**
   * This class is used to represent the substring of a {@link ByteList} over a single byte array.
   * In terms of the public API of {@link ByteList}, you end up here by calling {@link
   * ByteList#copy(byte[])} followed by {@link ByteList#substring(int, int)}.
   *
   * <p>This class contains most of the overhead involved in creating a substring from a {@link
   * ByteList}. The overhead involves some range-checking and two extra fields.
   */
  private static final class SubByteList extends ByteList {
    /**
     * Creates a {@code BoundedByteString} backed by the sub-range of given array, without copying.
     *
     * @param bytes  array to wrap
     * @param offset index to first byte to use in bytes
     * @param length number of bytes to use from bytes
     * @throws IllegalArgumentException if {@code offset < 0}, {@code length < 0}, or if {@code
     *                                  offset + length > bytes.length}.
     */
    SubByteList(byte[] bytes, int offset, int length) {
      super(bytes);
      checkRange(offset, offset + length, bytes.length);
      this.offset(offset);
      this.size(length);
    }

    /**
     * Gets the byte at the given index. Throws {@link ArrayIndexOutOfBoundsException} for
     * backwards-compatibility reasons although it would more properly be {@link
     * IndexOutOfBoundsException}.
     *
     * @param index index of byte
     * @return the value
     * @throws ArrayIndexOutOfBoundsException {@code index} is < 0 or >= size
     */
    @Override
    public byte byteAt(int index) {
      // We must check the index ourselves as we cannot rely on Java array index
      // checking for substrings.
      checkBounds(index, size());
      return array[offset() + index];
    }
  }

  // ByteList java.lang.String method equivalents

  /**
   * @see String#contentEquals(CharSequence)
   */
  public boolean contentEquals(ByteList that) {
    if (size() != that.size()) {
      return false;
    }
    OfByte thisBytes = iterator();
    OfByte thatBytes = that.iterator();
    while (thisBytes.hasNext()) {
      if (!thatBytes.hasNext()) {
        return false;
      } else if (thisBytes.nextByte() != thatBytes.nextByte()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Tests for the presence of a specific sequence of bytes in a larger array at a specific
   * location.<br/> If {@code this} is {@code null} there is a case for a match. First, if {@code
   * start} is non-zero, an {@code IllegalArgumentException} will be thrown. If {@code start} is
   * {@code 0}, will evaluate to {@code true} if {@code pattern} is {@code null}; {@code false}
   * otherwise.<br/> In all other cases, a {@code null} pattern will never match.
   *
   * @param start   the index at which to look for a match. The length of {@code pattern} added to
   *                this index must not pass the end of {@code src.}
   * @param pattern the series of {@code byte}s to match. If {@code null}, will only match a {@code
   *                null} {@code src} at position {@code 0}.
   * @return {@code true} if {@code pattern} is found in {@code src} at {@code start}.
   */
  public boolean constantTimeEquals(int start, byte[] pattern) {
    if (isEmpty()) {
      if (start == 0) {
        return pattern == null;
      }
      throw new IllegalArgumentException("start index after end of src");
    }
    if (pattern == null) {
      return false;
    }
    final int srcLength = size();
    // At this point neither src or pattern are null...
    if (start >= srcLength) {
      throw new IllegalArgumentException("start index after end of src");
    }
    if (srcLength < start + pattern.length) {
      return false;
    }
    int result = 0;
    // time-constant comparison
    for (int i = 0; i < pattern.length; i++) {
      result |= pattern[i] ^ get(start + i);
    }
    return result == 0;
  }

  /**
   * Given an integer, will return a new list with n consecutive repeats of the underlying value.
   *
   * @param value The number of times to repeat the entire list
   * @return a new ByteList with n consecutive repeats of the underlying value.
   */
  public ByteList repeat(int value) {
    int newSize = value * size();
    if (newSize > MAXIMUM_CAPACITY) {
      // TODO: avoid allocation
      throw new IllegalArgumentException(String.format("excessive repeat (%d * %d elements)", size(), value));
    }
    byte[] res = new byte[newSize];
    for (int i = 0; i < value; i++) {
      System.arraycopy(array, 0, res, i * size(), size());
    }
    return of(res);
  }

  /**
   * Tests if this bytestring starts with the specified prefix. Similar to {@link
   * String#startsWith(String)}
   *
   * @param prefix the prefix.
   * @return <code>true</code> if the byte sequence represented by the argument is a prefix of the
   * byte sequence represented by this string; <code>false</code> otherwise.
   */
  public final boolean startsWith(ByteList prefix) {
    return size() >= prefix.size() && substring(0, prefix.size()).equals(prefix);
  }

  /**
   * Tests if this bytestring ends with the specified suffix. Similar to {@link
   * String#endsWith(String)}
   *
   * @param suffix the suffix.
   * @return <code>true</code> if the byte sequence represented by the argument is a suffix of the
   * byte sequence represented by this string; <code>false</code> otherwise.
   */
  public final boolean endsWith(ByteList suffix) {
    return size() >= suffix.size() && substring(size() - suffix.size()).equals(suffix);
  }

  public final boolean endsWith(byte[] suffix) {
    return endsWith(ByteList.of(suffix));
  }

  /**
   * Return the substring from {@code beginIndex}, inclusive, to the end of the string.
   *
   * @param beginIndex start at this index
   * @return substring sharing underlying data
   * @throws IndexOutOfBoundsException if {@code beginIndex < 0} or {@code beginIndex > size()}.
   */
  public final ByteList substring(int beginIndex) {
    return substring(beginIndex, size());
  }

  /**
   * Return the substring from {@code beginIndex}, inclusive, to {@code endIndex}, exclusive.
   *
   * @param beginIndex start at this index
   * @param endIndex   the last character is the one before this index
   * @return substring sharing underlying data
   * @throws IndexOutOfBoundsException if {@code beginIndex < 0}, {@code endIndex > size()}, or
   *                                   {@code beginIndex > endIndex}.
   */
  public final ByteList substring(int beginIndex, int endIndex) {
    final int length = checkRange(beginIndex, endIndex, size());

    if (length == 0) {
      return EMPTY;
    }

    return new SubByteList(array, offset() + beginIndex, length);
  }

}
