package net.starlark.java.eval;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.UnmodifiableListIterator;
import com.google.common.primitives.UnsignedBytes;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.ext.ByteList;
import net.starlark.java.ext.ByteStringModuleApi;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class StarlarkBytes implements ByteStringModuleApi,
                                        Sequence<StarlarkBytes>,
                                        Comparable<StarlarkBytes>,
                                        CharSequence,
                                        HasBinary,
                                        StarlarkValue {

  public static class StarlarkByte extends StarlarkBytes implements HasBinary,
                                                                    StarlarkValue,
                                                                    Comparable<StarlarkBytes> {

    final byte x;
    private static final int OFFSET = 128;

    private StarlarkByte(byte x) {
      super(null, ByteList.wrap(x));
      this.x = x;
    } // cannot instantiate publicly.

    public static StarlarkByte of(byte b) {
      return StarlarkByte.ByteCache.cache[(int) b + OFFSET];
    }

    public static StarlarkByte of(int b) throws EvalException {
      if(b >> Byte.SIZE != 0) {
        throw Starlark.errorf("int in bytes: %s out of range", b);
      }
      return of((byte)b);
    }

    public static StarlarkByte of(StarlarkInt b) throws EvalException {
      return of(b.toInt("StarlarkByte::of"));
    }

    @Override
    public int compareTo(@NotNull StarlarkBytes o) {
      //a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
      if(o.size() > 1) {
        return -1; // if size is > 1 then it's bigger
      }
      else if (o.size() == 0) {
        return 1;
      }
      byte ob = o.byteAt(0);

      if (this.x == ob) {
        return 0;
      } else if (this.x < ob) {
        return -1;
      }
      return 1;
    }

    public StarlarkInt toStarlarkInt() {
      return StarlarkInt.of(toUnsigned());
    }

    public int toUnsigned() {
      return Byte.toUnsignedInt(this.x);
    }

    public byte get() {
      return this.x;
    }

    @Override
    public void str(Printer printer) {
      printer.append((char) x & 0xFF);
    }

    @Override
    public void repr(Printer printer) {
      printer.append(String.format("b\"%s\"", (char) x & 0xFF));
    }

    @Override
    public boolean equals(Object o) {
      // this is probably a hack --> the `StarlarkByte` class is really just a
      // specialization of a 1-element StarlarkBytes sequence
      if (!(o instanceof StarlarkBytes || o instanceof StarlarkInt)) {
        return false;
      } else if (this == o) {
        return true;
      } else if (o instanceof StarlarkByte) {
        return this.compareTo((StarlarkByte) o) == 0;
      } else if (o instanceof StarlarkInt) {
        return ((StarlarkInt)o).compareTo(toStarlarkInt()) == 0;
      } else {
        StarlarkBytes sbo = ((StarlarkBytes) o);
        if (sbo.size() != 1) {
          return false;
        }
        return this.compareTo(StarlarkByte.of(sbo.byteAt(0))) == 0;
      }
    }

    @Override
    public boolean isImmutable() {
      return true;
    }

    /**
     * Returns a hash code for this {@code Byte}; equal to the result of invoking {@code
     * intValue()}.
     *
     * @return a hash code value for this {@code Byte}
     */
    @Override
    public int hashCode() {
      return Byte.hashCode(x);
    }

    @Nullable
    @Override
    public Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
      try(Mutability mu = Mutability.create("StarlarkBytesBinaryOp")) {
        StarlarkThread thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
        return EvalUtils.binaryOp(op, toStarlarkInt(), that, thread);
      }
    }

    private static class ByteCache {
      static final StarlarkByte[] cache = new StarlarkByte[-(-OFFSET) + 127 + 1];

      static {
        for (int i = 0; i < cache.length; i++)
          cache[i] = new StarlarkByte((byte) (i - OFFSET));
      }

      private ByteCache() {
      }
    }
  }

  public static class StarlarkByteArray extends StarlarkBytes {

    private StarlarkByteArray(StarlarkBytes bytes) {
      super(bytes.mutability, bytes.delegate);
    }

    private StarlarkByteArray(@Nullable Mutability mutability, ByteList elems) {
      super(mutability, elems);
    }

    static StarlarkByteArray wrap(@Nullable Mutability mutability, ByteList elems) {
      return new StarlarkByteArray(mutability, elems);
    }

    public static StarlarkByteArray of(Mutability mutability) {
      return new StarlarkByteArray(mutability, ByteList.empty());
    }

    public static StarlarkByteArray of(StarlarkBytes sb) {
      return new StarlarkByteArray(sb);
    }

    public static StarlarkByteArray of(@Nullable Mutability mutability, byte... elems) {
      return StarlarkByteArray.of(StarlarkBytes.of(mutability, elems));
    }
    public static StarlarkByteArray of(@Nullable Mutability mutability, StarlarkInt... elems) {
      return StarlarkByteArray.of(StarlarkBytes.of(mutability, elems));
    }
    public static StarlarkByteArray copyOf( @Nullable Mutability mutability, Iterable<StarlarkInt> elems) throws EvalException {
      return StarlarkByteArray.of(StarlarkBytes.copyOf(mutability, elems));
    }
//
//    public static StarlarkByteArray copyOf( @Nullable Mutability mutability, StarlarkByte ... elems) throws EvalException {
//      return StarlarkByteArray.of(StarlarkBytes.copyOf(mutability, elems));
//    }

    @Override
    public boolean isImmutable() {  // ByteArray is mutable
      return false;
    }

    @Override
    public StarlarkBytes set(int index, StarlarkBytes element) {
      if(element.size() != 1) {
        throw new IllegalArgumentException("Expected starlark element to be of size 1!");
      }
      return StarlarkBytes.immutableOf(
        this.delegate.setValue(index, element.byteAt(0))
      );
    }

    @Override
    public void add(int index, StarlarkBytes element) {
      this.delegate.addAll(index, element.delegate);
    }

    @Override
    public boolean add(StarlarkBytes o) {
      return this.delegate.extend(o.delegate);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends StarlarkBytes> c) {
      ensureNotFrozen();
      boolean modified = false;
      for (StarlarkBytes e : c) {
        add(e);
        modified = true;
      }
      return modified;
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends StarlarkBytes> c) {
      ensureNotFrozen();
      int i = index;
      for (StarlarkBytes e : c) {
        add(i, e);
        i++;
      }
      return i != index;
    }

    public boolean addAll(int index, byte[] c) {
      ensureNotFrozen();
      int i = index;
      for (byte e : c) {
        this.delegate.addValue(i,e);
        i++;
      }
      return i != index;
    }

    @Override
    public void replaceAll(UnaryOperator<StarlarkBytes> operator) {
      for (int i = 0; i < this.delegate.size(); i++) {
          this.set(i, operator.apply(this.get(i)));
      }
//      final ListIterator<StarlarkBytes> li = this.listIterator();
//      while (li.hasNext()) {
//          li.set(operator.apply(li.next()));
//      }
    }

    public void replaceAll(StarlarkBytes bl) {
      this.delegate.clear();
      this.delegate.extend(bl.delegate);
//      final ListIterator<StarlarkBytes> li = this.listIterator();
//      while (li.hasNext()) {
//          li.set(operator.apply(li.next()));
//      }
    }

    @StarlarkMethod(
         name = "append",
         doc = "Adds an integer to the end of the byte array.",
         parameters = {@Param(name = "item", doc = "Item to add at the end.")})
    public void append(StarlarkInt item) throws EvalException {
       this.add(StarlarkBytes.immutableOf(toByte(item.toInt("append"))));
     }

    @StarlarkMethod(name = "copy", doc = "Return a copy of bytearray.")
    public StarlarkByteArray copy() throws EvalException {
      return wrap(mutability, delegate);
    }

     @StarlarkMethod(
         name = "extend",
         doc = "Adds all items to the end of the list.",
         parameters = {@Param(name = "items", doc = "Items to add at the end.")})
     public void extend(StarlarkBytes items) throws EvalException {
       this.addAll(items);
     }

     @StarlarkMethod(
         name = "insert",
         doc = "Inserts an item at a given position.",
         parameters = {
             @Param(name = "index", doc = "The index of the given position."),
             @Param(name = "item", doc = "The item.")
         })
     public void insert(StarlarkInt index, StarlarkBytes item) throws EvalException {
       this.addAll(index.toInt("insert"), item);
     }

    @Override
    public void clear() {
      this.delegate.clear();
    }

    @StarlarkMethod(name = "clear", doc = "Removes all the elements of the list.")
     public void clearElements() throws EvalException {
       this.clear();
     }

    @Override
    public boolean remove(Object o) {
      // Overridden for performance to prevent a ton of boxing
      ensureNotFrozen();
      if (!(o instanceof StarlarkBytes)) {
        return false;
      }
      return this.delegate.remove(((StarlarkBytes)o).delegate);
    }

    @Override
    public StarlarkBytes remove(int index) {
      ensureNotFrozen();
      return StarlarkBytes.immutableOf(this.delegate.removeAt(index));
    }

    @StarlarkMethod(
         name = "pop",
         doc =
             "Removes the item at the given position in the list, and returns it. "
                 + "If no <code>index</code> is specified, "
                 + "it removes and returns the last item in the list.",
         parameters = {
             @Param(
                 name = "i",
                 allowedTypes = {
                     @ParamType(type = StarlarkInt.class),
                     @ParamType(type = NoneType.class),
                 },
                 defaultValue = "-1",
                 doc = "The index of the item.")
         })
     public StarlarkInt pop(Object i) throws EvalException {
       int arg = i == Starlark.NONE ? -1 : Starlark.toInt(i, "i");
       return StarlarkInt.of(this.remove(arg).byteAt(0));
     }

    @StarlarkMethod(
        name = "remove",
        doc ="Remove the first occurrence of a value in the bytearray. ",
        parameters = {
            @Param(
                name = "i",
                allowedTypes = {
                    @ParamType(type = StarlarkInt.class),
                },
                doc = "The value of the item.")
        })
    public void removeItem(Object i) throws EvalException {
      final int index = this.index(i, 0, this.size());
      this.remove(index);
    }

    @Override
    public @Nullable Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
      Object rval;
      if (op == TokenKind.PLUS) {
        if (that instanceof StarlarkBytes || that instanceof StarlarkList || that instanceof StarlarkByte) {
          if (thisLeft) {
            rval = BinaryOperations.add(this, that, this.mutability);
          } else {
            rval = BinaryOperations.add(that, this, this.mutability);
          }
        }
      }
      rval = super.binaryOp(op, that, thisLeft);
      if (rval == null) {
        return rval;
      }
      // should be starlarkbytearray
      return StarlarkByteArray.of((StarlarkBytes) rval);
    }
  }

  protected final ByteList delegate;

  protected final Mutability mutability;

  private StarlarkBytes(@Nullable Mutability mutability) {
    this(mutability, ByteList.empty());
  }

  private StarlarkBytes(@Nullable Mutability mutability, ByteList elems) {
    this.mutability = mutability == null ? Mutability.IMMUTABLE : mutability;
    this.delegate = elems;
  }

  /**
   * Takes ownership of the supplied ByteList returns a new StarlarkBytes instance that
   * initially wraps the ByteList. The caller must not subsequently modify the ByteList, but the
   * StarlarkBytes instance may do so.
   */
  static StarlarkBytes wrap(@Nullable Mutability mutability, ByteList elems) {
    return new StarlarkBytes(mutability, elems);
  }

  /**
   * Takes ownership of the supplied byte array and returns a new StarlarkBytes instance that
   * initially wraps the array. The caller must not subsequently modify the array, but the
   * StarlarkBytes instance may do so.
   */
  static StarlarkBytes wrap(@Nullable Mutability mutability, byte[] elems) {
    return wrap(mutability, ByteList.wrap(elems));
  }

  @Override
  public boolean isImmutable() {
    return true; // Starlark spec says that Byte is immutable
  }

  /**
   * A shared instance for the empty immutable byte array.
   */
  private static final StarlarkBytes EMPTY = new StarlarkBytes(Mutability.IMMUTABLE);

  /**
   * Returns an immutable instance backed by an empty byte array.
   */
  public static StarlarkBytes empty() {
    return EMPTY;
  }

  /**
   * Returns a {@code StarlarkBytes} whose items are given by an iterable of StarlarkInt and which
   * has the given {@link Mutability}.
   */
  public static StarlarkBytes copyOf(
    @Nullable Mutability mutability, Iterable<StarlarkInt> elems) throws EvalException {
    StarlarkInt[] arr = Iterables.toArray(elems, StarlarkInt.class);
    byte[] array = new byte[arr.length];
    for (int i = 0; i < arr.length; i++) {
      if (arr[i].toIntUnchecked() >> Byte.SIZE != 0) {
        throw Starlark.errorf("at index %d, %s out of range .want value" +
                                " in unsigned 8-bit range", i, arr[i]);
      }
      array[i] = (byte) arr[i].toIntUnchecked();
    }
    return wrap(mutability, array);
  }
//  private static StarlarkBytes copyOf(Mutability mutability, StarlarkByte... elems) {
//    StarlarkBytes sb = of(mutability);
//    //noinspection ForLoopReplaceableByForEach
//    for (int i = 0, elemsLength = elems.length; i < elemsLength; i++) {
//      StarlarkByte b = elems[i];
//      sb.add(b);
//    }
//    return sb;
//  }

  public static byte toByte(int x) {
    checkArgument(x >= 0 ? x >> Byte.SIZE == 0: x >= Byte.MIN_VALUE,  x);
    return (byte) x;
  }

  private static void checkElemsValid(StarlarkInt[] elems) {
    for (StarlarkInt elem : elems) {
      int value = elem.toIntUnchecked();
      checkArgument(value >> Byte.SIZE == 0, value);
    }
  }

  /**
   * Returns a {@code StarlarkBytes} whose items are given by a {@link ByteBuffer} and which
   * has the given {@link Mutability}. As the method nmae says, a copy of the data is made.
   */
  public static StarlarkBytes copyOf(@Nullable Mutability mutability, ByteBuffer buf) {
    return wrap(mutability, ByteList.fromByteBuffer(buf));
  }

  /**
   * Returns an immutable byte array with the given elements. Equivalent to {@code copyOf(null,
   * elems)}.
   */
  public static StarlarkBytes immutableCopyOf(Iterable<StarlarkInt> elems) throws EvalException {
    return copyOf(Mutability.IMMUTABLE, elems);
  }

  /**
    * Returns an empty {@code StarlarkBytes} with the given {@link Mutability}.
    */
   public static StarlarkBytes of(@Nullable Mutability mutability) {
     return of(mutability, new byte[0]);
   }

  /**
   * Returns a {@code StarlarkBytes} with the given items and the {@link Mutability}.
   */
  public static StarlarkBytes of(@Nullable Mutability mutability, byte... elems) {
    if (elems.length == 0 && (mutability == null || mutability.isFrozen())) {
      return empty();
    }

    return wrap(mutability, elems);
  }

  /**
   * Returns a {@code StarlarkBytes} with the given items and the {@link Mutability}.
   */
  public static StarlarkBytes of(@Nullable Mutability mutability, StarlarkInt... elems) {
    if (elems.length == 0 && (mutability == null || mutability.isFrozen())) {
      return empty();
    }

    checkElemsValid(elems);
    byte[] arr = new byte[elems.length];
    for (int i = 0; i < elems.length; i++) {
      arr[i] = UnsignedBytes.checkedCast(elems[i].toIntUnchecked());
    }
    return of(mutability, arr);
  }

  public static StarlarkBytes of(Mutability mutability, int[] out) {
    byte[] z = null;
    for (int i = 0, inputLength = out.length; i < inputLength; i++) {
      if(z == null) {
        z = new byte[inputLength];
      }
      z[i] = toByte(out[i]);
    }
    return z != null ? StarlarkBytes.of(mutability, z) : empty();
  }

  /**
   * Returns a {@code StarlarkBytes} with the given items and the {@link Mutability}.
   */
  public static StarlarkBytes immutableOf(byte... elems) {
    return of(Mutability.IMMUTABLE, elems);
  }

  /**
   * Returns an immutable {@code StarlarkList} with the given items.
   */
  public static StarlarkBytes immutableOf(StarlarkInt... elems) {
    return of(Mutability.IMMUTABLE, elems);
  }

  /**
   * Returns a {@code StarlarkBytes} with the given items and the {@link Mutability}.
   */
  public static StarlarkBytes immutableOf(char... elems) {
    byte[] barr = UTF16toUTF8(elems);
    return immutableOf(barr);
  }

  public int[] getUnsignedBytes() {
    return this.delegate.toUnsignedIntArray();
  }

  protected void ensureNotFrozen() {
    if (mutability.isFrozen()) {
      throw new UnsupportedOperationException("frozen");
    }
  }

  @Override
  public StarlarkByte get(int index) {
    return StarlarkByte.of(this.delegate.get(index)); // can throw OutOfBounds
  }

  @Override
  public StarlarkBytes set(int index, StarlarkBytes element) {
    throw new UnsupportedOperationException("bytes are immutable. use bytearray.");
  }

  @Override
  public void add(int index, StarlarkBytes element) {
    throw new UnsupportedOperationException("bytes are immutable. use bytearray.");
  }

  @Override
  public StarlarkBytes remove(int index) {
    throw new UnsupportedOperationException("bytes are immutable. use bytearray.");
  }

  @Override
  public int indexOf(Object o) {
    // Overridden for performance to prevent a ton of boxing
    if (!(o instanceof StarlarkBytes)) {
      return -1;
    }

    return this.delegate.indexOf(((StarlarkBytes)o).delegate);
  }

  @Override
  public int lastIndexOf(Object o) {
    // Overridden for performance to prevent a ton of boxing
    if (!(o instanceof StarlarkBytes)) {
      return -1;
    }

    return this.delegate.lastIndexOf(((StarlarkBytes)o).delegate, 0,  this.delegate.size());
  }

  @NotNull
  @Override
  public ListIterator<StarlarkBytes> listIterator() {
    final ByteList.ByteListIterator bListItr = this.delegate.listIterator();
    return newListIterator(bListItr);
  }

  @NotNull
  @Override
  public ListIterator<StarlarkBytes> listIterator(int index) {
    final ByteList.ByteListIterator bListItr = this.delegate.listIterator(index);
    return newListIterator(bListItr);
  }

  private ListIterator<StarlarkBytes> newListIterator(ByteList.ByteListIterator bListItr) {
    Mutability mu = this.mutability;
    return new UnmodifiableListIterator<StarlarkBytes>() {
      @Override
      public boolean hasPrevious() {
        return bListItr.hasPrevious();
      }

      @Override
      public StarlarkBytes previous() {
        return StarlarkBytes.immutableOf(bListItr.previousByte());
      }

      @Override
      public int nextIndex() {
        return bListItr.nextIndex();
      }

      @Override
      public int previousIndex() {
        return bListItr.previousIndex();
      }

      @Override
      public boolean hasNext() {
        return bListItr.hasNext();
      }

      @Override
      public StarlarkBytes next() {
        return StarlarkBytes.immutableOf(bListItr.nextByte());
      }
    };
  }

  @NotNull
  @Override
  public List<StarlarkBytes> subList(int fromIndex, int toIndex) {
    final ByteList substring = this.delegate.substring(fromIndex, toIndex);
    List<StarlarkBytes> list = new ArrayList<>(substring.size());
    for (int i = 0, loopLength = substring.size(); i < loopLength; i++) {
      list.add(i, StarlarkBytes.immutableOf(substring.get(i)));
    }
    return list;
  }

  @Override
  public int hashCode() {
    // Fnv32 hash
    if (this.delegate == null) {
      return 0;
    }

    int hash = -2128831035;
    for (byte b : this.delegate) {
      hash ^= b;
      hash *= (long) 16777619;
    }
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof StarlarkBytes)) {
      return false;
    }
    if (this == o) {
      return true;
    }
    return this.compareTo((StarlarkBytes) o) == 0;
  }

  @Override
  public boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {
    if (key instanceof StarlarkBytes) {
      return -1 != this.delegate.indexOf(((StarlarkBytes) key).delegate);
    } else if (key instanceof StarlarkInt) {
      StarlarkInt _key = ((StarlarkInt) key);
      if (!Range
             .closed(0, 255)
             .contains(_key.toIntUnchecked())) {
        throw Starlark.errorf("int in bytes: %s out of range", _key);
      }
      return -1 != this.delegate.indexOf(_key.toIntUnchecked());
    }
    //"requires bytes or int as left operand, not string"
    throw new EvalException(
      String.format("requires bytes or int as left operand, not %s", Starlark.type(key))
    );
  }

  @Override
  public int size() {
    return this.delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return this.delegate.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    if (!(o instanceof StarlarkBytes)) {
      return false;
    }

    return this.delegate.contains(((StarlarkBytes)o).delegate);
  }

  @NotNull
  @Override
  public Iterator<StarlarkBytes> iterator() {
    ByteList.OfByte x = this.delegate.iterator();
    return new Iterator<StarlarkBytes>() {

      @Override
      public boolean hasNext() {
        return x.hasNext();
      }

      @Override
      public StarlarkBytes next() {
        return StarlarkBytes.immutableOf(x.next());
      }
    };
  }

  @Override
  public Object[] toArray() {
    final int arraySize = size();
    Object[] r = new Object[arraySize];
    Arrays.fill(r, this.delegate.toArray());
    return r;
  }

  @Override
  public <T> T[] toArray(T @NotNull [] a) {
    Arrays.fill(a, this.delegate.toArray());
    return a;
  }

  @Override
  public boolean add(StarlarkBytes o) {
    throw new UnsupportedOperationException("bytes are immutable. use bytearray.");
  }

  public byte[] toByteArray() {
    return this.delegate.toArray();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException("bytes are immutable. use bytearray.");
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    // Overridden for performance to prevent a ton of boxing
    for (Object e : c)
        if (!contains(e))
            return false;
    return true;
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends StarlarkBytes> c) {
    throw new UnsupportedOperationException("bytes are immutable. use bytearray.");
  }

  @Override
  public boolean addAll(int index, @NotNull Collection<? extends StarlarkBytes> c) {
    throw new UnsupportedOperationException("bytes are immutable. use bytearray.");
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> c) {
    throw new UnsupportedOperationException("bytes are immutable. use bytearray.");
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    throw new UnsupportedOperationException("bytes are immutable. use bytearray.");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("bytes are immutable. use bytearray.");
  }

  @Override
  public int compareTo(@NotNull StarlarkBytes o) {
    return this.delegate.compareTo(o.delegate);
  }


  @Override
  public void str(Printer printer) {
    byte[] bytes = this.delegate.copy(); //todo
    String s;
    s = UTF8toUTF16(bytes, 0, bytes.length, /*allowMalformed*/false);
    printer.append(s);
  }

  @Override
  public String toString() {
    return Starlark.repr(this);
  }

  @Override
  public void repr(Printer printer) {
    byte[] bytes = this.delegate.copy(); //todo
    String s;
    try {
      s = UTF8toUTF16(bytes, 0, bytes.length, /*allowMalformed*/true);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < s.length(); i++) {
        quote(sb, s.codePointAt(i));
      }
      s = sb.toString();
    } catch(IndexOutOfBoundsException ex) {
      StringBuilder sb = new StringBuilder();
      for(byte b : this.delegate) {
        quote(sb, Byte.toUnsignedInt(b));
      }
      s = sb.toString();
    }
    printer.append(String.format("b\"%s\"", s));
  }

  @Override
  public StarlarkBytes getSlice(Mutability mu, int start, int stop, int step) {
    RangeList indices = new RangeList(start, stop, step);
    int n = indices.size();
    if (step == 1) { // common case
      final int at = indices.at(0);
      return wrap(mu, this.delegate.substring(at, at + n));
    }
    byte[] res = new byte[n];
    for (int i = 0; i < n; ++i) {
      res[i] = this.delegate.get(indices.at(i));
    }
    return wrap(mu, res);
  }

  @StarlarkMethod(
    name = "elems",
    doc =
      "Returns an iterable value containing successive 1-element byte of the underlying bytearray "
        + "Equivalent to <code>[b[i] for i in range(len(b))]</code>, except that the "
        + "returned value might not be a list.")
  public StarlarkByteElems elems() {
    return new StarlarkByteElems(this);
  }

  @Override
  public String hex(Object sepO, StarlarkInt bytesPerSep) throws EvalException {
    int nbytesPerSep = bytesPerSep.toIntUnchecked();
    byte sep;
    if(sepO instanceof CharSequence) {
      CharSequence sepChr = ((CharSequence) sepO);
      if(sepChr.length() != 1) {
        throw new EvalException("sep must be length 1.");
      }
      if (sepChr.charAt(0) > 0x7F) {
        throw new EvalException("sep must be ASCII.");
      }
      sep = (byte)(sepChr.charAt(0) & 0xFF);
    }
    else {
      sep = -1; // intentionally set to be less than -1 to avoid allocating an array
      nbytesPerSep = 0;
    }
    return this.delegate.hex(sep, nbytesPerSep);
  }

  /** Reports whether {@code x} is Java null or Starlark None. */
  static boolean isNullOrNoneOrUnbound(Object x) {
    return x == null || x == Starlark.NONE || x == Starlark.UNBOUND;
  }

  private ByteList starlarkObjectToByteList(Object sub) throws EvalException {
    if (sub instanceof StarlarkBytes) {
      return ((StarlarkBytes) sub).delegate;
    }

    StarlarkInt sub1 = (StarlarkInt) sub;
    int x;
    try {
      x = sub1.toInt("byte must be in range(0, 256)");
      checkArgument(x >> Byte.SIZE == 0, x);
    } catch (IllegalArgumentException e) {
      throw new EvalException(e.getMessage(), e.getCause());
    }
    return ByteList.wrap((byte) x);
  }

  @Override
  public int count(Object sub, Object start, Object end) throws EvalException {
    ByteList subarr = starlarkObjectToByteList(sub);
    return this.delegate.count(
      subarr,
      Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "count"),
      isNullOrNoneOrUnbound(end) ? size() : Starlark.toInt(end, "count"));
  }

  @Override
  public StarlarkBytes removeprefix(StarlarkBytes prefix) {
    final ByteList prefixeRemoved = this.delegate.removeprefix(prefix.delegate);
    if(prefixeRemoved == this.delegate) {
      return this;
    }
    return wrap(mutability, prefixeRemoved);
  }

  @Override
  public StarlarkBytes removesuffix(StarlarkBytes suffix) {
    final ByteList suffixRemoved = this.delegate.removesuffix(suffix.delegate);
    if(suffixRemoved == this.delegate) {
      return this;
    }
    return wrap(mutability, suffixRemoved);
   }

  @Override
  public String decode(String encoding, String errors) throws EvalException {
    try {
      return this.delegate.decode(encoding, errors);
    } catch (CharacterCodingException e) {
      throw new EvalException(e.getMessage(), e);
    }
  }

  @Override
  public boolean endsWith(Object suffixO, Object start, Object end) throws EvalException {
    if(suffixO instanceof StarlarkBytes) {
      StarlarkBytes suffix = ((StarlarkBytes) suffixO);
      return this.delegate.endsWith(suffix.delegate.substring(
          Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "endsWith"),
          Starlark.isNullOrNone(end) ? suffix.size() : Starlark.toInt(end, "endsWith")
        ));
    }
    Tuple _seq = ((Tuple) suffixO); // we want to throw a class cast exception here if not tuple
    Sequence<StarlarkBytes> seq = Sequence.cast(_seq, StarlarkBytes.class, "endsWith");
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, seqSize = seq.size(); i < seqSize; i++) { // no allocation loop
      if(this.delegate.endsWith(
        seq.get(i) // does not allocate because Tuple returns item @ index
          .delegate
          .substring(
            Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "endsWith"),
            Starlark.isNullOrNone(end) ? seq.get(i).size() : Starlark.toInt(end, "endsWith")
          ))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int find(Object sub, Object start, Object end) throws EvalException {
    ByteList subarr = starlarkObjectToByteList(sub);
    return this.delegate.find(
      subarr,
      Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "find"),
      isNullOrNoneOrUnbound(end) ? size() : Starlark.toInt(end, "find"));
  }


  @Override
  public int index(Object sub, Object start, Object end) throws EvalException {
    int loc = find(sub, start, end);
    if(loc == -1) {
      throw Starlark.errorf("subsection not found");
    }
    return loc;
  }

  @Override
  public StarlarkBytes join(Sequence<StarlarkBytes> elements) throws EvalException {
    ByteList[] parts = new ByteList[elements.size()];
    for (int i = 0, loopLength = elements.size(); i < loopLength; i++) {
      parts[i] = elements.get(i).delegate;
    }
    return wrap(mutability, this.delegate.join(parts));
  }

  @Override
  public Tuple partition(StarlarkBytes sep) {
    final ByteList[] partitioned = this.delegate.partition(sep.delegate);
    return Tuple.of(
      wrap(mutability, partitioned[0]),
      wrap(mutability, partitioned[1]),
      wrap(mutability, partitioned[2])
    );
  }

  @Override
  public StarlarkBytes replace(StarlarkBytes oldBytes, StarlarkBytes newBytes, StarlarkInt countI, StarlarkThread thread) throws EvalException {
    int count = Starlark.isNullOrNone(countI)
                  ? Integer.MAX_VALUE
                  : Starlark.toInt(countI, "replace");
    if(count == -1) {
      count = Integer.MAX_VALUE;
    }
    final ByteList replaced = this.delegate.replace(oldBytes.delegate, newBytes.delegate, count);
    return wrap(mutability, replaced);
  }

  @Override
   public int rfind(Object sub, Object start, Object end) throws EvalException {
    ByteList subarr = starlarkObjectToByteList(sub);
    return this.delegate.rfind(
      subarr,
      Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "rfind"),
      isNullOrNoneOrUnbound(end) ? size() : Starlark.toInt(end, "rfind"));
   }

  @Override
   public int rindex(Object sub, Object start, Object end) throws EvalException {
    int loc = rfind(sub, start, end);
    if(loc == -1) {
      throw Starlark.errorf("subsection not found");
    }
    return loc;
   }

  @Override
  public Tuple rpartition(StarlarkBytes sep) throws EvalException {
    final ByteList[] rightPartitioned = this.delegate.rpartition(sep.delegate);
    return Tuple.of(
      wrap(mutability, rightPartitioned[0]),
      wrap(mutability, rightPartitioned[1]),
      wrap(mutability, rightPartitioned[2])
    );
  }

  @Override
  public boolean startsWith(Object prefixO, Object start, Object end) throws EvalException {
    if(prefixO instanceof StarlarkBytes) {
      StarlarkBytes prefix = ((StarlarkBytes) prefixO);
      return this.delegate.startsWith(prefix.delegate.substring(
          Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "startsWith"),
          Starlark.isNullOrNone(end) ? prefix.size() : Starlark.toInt(end, "startsWith")
        ));
    }
    Tuple _seq = ((Tuple) prefixO); // we want to throw a class cast exception here if not tuple
    Sequence<StarlarkBytes> seq = Sequence.cast(_seq, StarlarkBytes.class, "startsWith");
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, seqSize = seq.size(); i < seqSize; i++) { // no allocation loop
      if(this.delegate.startsWith(
        seq.get(i) // does not allocate because Tuple returns item @ index
          .delegate
          .substring(
            Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "startsWith"),
            Starlark.isNullOrNone(end) ? seq.get(i).size() : Starlark.toInt(end, "startsWith")
          ))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public StarlarkBytes translate(Object tableO, StarlarkBytes delete) throws EvalException {
    ByteList table = null;
    if (!Starlark.isNullOrNone(tableO)) {
      table = ((StarlarkBytes) tableO).delegate;
    }
    try {
      return wrap(mutability, this.delegate.translate(table, delete.delegate));
    }catch(IllegalArgumentException ex) {
      throw new EvalException(ex.getMessage(), ex);
    }
  }

  @Override
  public StarlarkBytes center(StarlarkInt width, StarlarkBytes fillbyte) throws EvalException {
    return wrap(this.mutability, this.delegate.center(width.toInt("center"),fillbyte.delegate));
  }

  @Override
  public StarlarkBytes ljust(StarlarkInt width, StarlarkBytes fillbyte) throws EvalException {
    return wrap(this.mutability, this.delegate.ljust(width.toInt("ljust"), fillbyte.delegate));
  }

  @Override
  public StarlarkBytes lstrip(Object charsO) {
    ByteList chars = ByteList.empty();
    if(!Starlark.isNullOrNone(charsO)) {
      chars = ((StarlarkBytes) charsO).delegate;
    }
    return wrap(Mutability.IMMUTABLE, this.delegate.lstrip(chars));
  }

  @Override
  public StarlarkBytes rjust(StarlarkInt width, StarlarkBytes fillbyte)  throws EvalException {
    return wrap(this.mutability, this.delegate.rjust(width.toInt("rjust"), fillbyte.delegate));
  }

  @Override
  public StarlarkList<StarlarkBytes> rsplit(Object bytesO, Object maxSplitO, StarlarkThread thread) throws EvalException {
    int maxSplit = Starlark.isNullOrNone(maxSplitO)
                  ? Integer.MAX_VALUE
                  : Starlark.toInt(maxSplitO, "rsplit");
    if(maxSplit == -1) {
      maxSplit = Integer.MAX_VALUE;
    }
    ByteList splitOn = ByteList.empty();
    if (!Starlark.isNullOrNone(bytesO)) {
      splitOn = ((StarlarkBytes)bytesO).delegate;
    }
    final ByteList[] rsplited = this.delegate.rsplit(splitOn, maxSplit);
    StarlarkList<StarlarkBytes> res = StarlarkList.newList(thread.mutability());
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, loopLen = rsplited.length; i < loopLen; i++) {
      res.addElement(wrap(thread.mutability(), rsplited[i]));
    }
    return res;
  }

  @Override
  public StarlarkBytes rstrip(Object charsO) {
    ByteList chars = ByteList.empty();
    if(!Starlark.isNullOrNone(charsO)) {
      chars = ((StarlarkBytes) charsO).delegate;
    }
    return wrap(Mutability.IMMUTABLE, this.delegate.rstrip(chars));
  }

  @Override
  public StarlarkList<StarlarkBytes> split(Object bytesO, Object maxSplitO, StarlarkThread thread) throws EvalException {
    int maxSplit = Starlark.isNullOrNone(maxSplitO)
                  ? Integer.MAX_VALUE
                  : Starlark.toInt(maxSplitO, "split");
    if(maxSplit == -1) {
      maxSplit = Integer.MAX_VALUE;
    }
    ByteList splitOn = ByteList.empty();
    if (!Starlark.isNullOrNone(bytesO)) {
      splitOn = ((StarlarkBytes)bytesO).delegate;
    }
    final ByteList[] splitted = this.delegate.split(splitOn, maxSplit);
    StarlarkList<StarlarkBytes> res = StarlarkList.newList(thread.mutability());
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, loopLen = splitted.length; i < loopLen; i++) {
      res.addElement(wrap(thread.mutability(), splitted[i]));
    }
    return res;
  }

  @Override
  public StarlarkBytes strip(Object charsO) {
    ByteList chars = ByteList.empty();
    if(!Starlark.isNullOrNone(charsO)) {
      chars = ((StarlarkBytes) charsO).delegate;
    }
    return wrap(Mutability.IMMUTABLE, this.delegate.strip(chars));
  }

  @Override
  public StarlarkBytes capitalize() {
    return wrap(mutability, this.delegate.capitalize());
  }

  @Override
  public StarlarkBytes expandTabs(StarlarkInt tabSize) throws EvalException {
    if(size() == 0) {
      return empty();
    }
    return wrap(mutability, this.delegate.expandtabs(tabSize.toInt("expandTabs")));
  }

  @Override
  public boolean isAlnum() {
    return this.delegate.isalnum();
  }

  @Override
  public boolean isAlpha() {
    return this.delegate.isalpha();
  }

  @Override
  public boolean isAscii() {
    return this.delegate.isascii();
  }

  @Override
  public boolean isDigit() {
    return this.delegate.isdigit();
  }

  @Override
  public boolean isLower() {
    return this.delegate.islower();
  }

  @Override
  public boolean isSpace() {
    return this.delegate.isspace();
  }

  @Override
  public boolean isTitle() {
    return this.delegate.istitle();
  }

  @Override
  public boolean isUpper() {
    return this.delegate.isupper();
  }

  @Override
  public StarlarkBytes lower() {
    return wrap(mutability, this.delegate.lower());
  }

  @Override
  public Sequence<StarlarkBytes> splitLines(boolean keepEnds) throws EvalException {
    final ByteList[] splitted = this.delegate.splitlines(keepEnds);
    StarlarkList<StarlarkBytes> res = StarlarkList.newList(mutability);
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, loopLen = splitted.length; i < loopLen; i++) {
      res.addElement(wrap(mutability, splitted[i]));
    }
    return res;
  }

  @Override
  public StarlarkBytes swapcase() {
    return wrap(mutability, this.delegate.swapcase());
  }

  @Override
  public StarlarkBytes title() {
    return wrap(mutability, this.delegate.title());
  }

  @Override
  public StarlarkBytes upper() {
    return wrap(mutability, this.delegate.upper());
  }

  @Override
  public StarlarkBytes zfill(StarlarkInt width) throws EvalException {
    return wrap(mutability, this.delegate.zfill(width.toInt("zfill")));
  }

  /**
   * Ensures the truth of an expression involving one or more parameters
   * to the calling method.
   */
  static void checkArgument(boolean b, int p1) {
    if (!b) {
      throw new IllegalArgumentException(
        Strings.lenientFormat(
          "byte must be in range(0, 256). received %s", p1));
    }
  }


  @Override
  public int length() {
    return size();
  }

  @Override
  public char charAt(int index) {
    return this.delegate.charAt(index);
  }

  public byte byteAt(int index) {
    return this.delegate.byteAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return this.delegate.subSequence(start,end);
  }

  public char[] toCharArray(Charset cs) {
    CharBuffer charBuffer = cs.decode(ByteBuffer.wrap(this.toByteArray()));
    return Arrays.copyOf(charBuffer.array(), charBuffer.limit());
  }

  public char[] toCharArray() {
    // this is the right default charset for char arrays
    // specially in a password context
    // see: https://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords
    // as well as: https://stackoverflow.com/a/9670279/133514
    return toCharArray(StandardCharsets.ISO_8859_1);
  }

  @StarlarkBuiltin(name = "bytes.elems")
  public class StarlarkByteElems extends AbstractList<StarlarkInt>
    implements Sequence<StarlarkInt> {

    final private StarlarkBytes bytes;

    public StarlarkByteElems(StarlarkBytes bytes) {
      this.bytes = bytes;
    }

    @Override
    public void repr(Printer printer) {
      byte[] bytes = this.bytes.delegate.toArray();
      printer.append(
        String.format("b\"%s\".elems()",
          UTF8toUTF16(bytes, 0, bytes.length, /*allowMalformed*/ true)
        ));
    }

    @Override
    public StarlarkInt get(int index) {
      return StarlarkInt.of(Byte.toUnsignedInt(this.bytes.byteAt(index)));
//      int[] bytes = this.bytes.get(index).getUnsignedBytes();
      // guaranteed to be one entry per slice.
      // an index on a byte array will return 1 byte
//      return StarlarkInt.of(bytes[0]); // so this is safe.
    }

    @Override
    public int size() {
      return this.bytes.size();
    }

    @Override
    public Sequence<StarlarkInt> getSlice(Mutability mu, int start, int stop, int step) {
      int[] unsignedBytes = this.bytes.getSlice(mu, start, stop, step).getUnsignedBytes();
      StarlarkList<StarlarkInt> list = StarlarkList.newList(mutability);
      for (int i : unsignedBytes) {
        list.add(StarlarkInt.of(i));
      }
      return list;
    }

  }

  /**
   * Returns a new StarlarkBytes containing n consecutive repeats of this byte array.
   */
  public StarlarkBytes repeat(StarlarkInt n, Mutability mutability) throws EvalException {
    try {
      return wrap(mutability, this.delegate.repeat(n.toInt("repeat")));
    } catch(IllegalArgumentException ex) {
      throw new EvalException(ex.getMessage(), ex);
    }
  }

  @Nullable
  @Override
  public Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    switch (op) {
      case STAR:
        /*
          Attempts to multiply a StarlarkBytes type by an integer. The caller is responsible for casting
          to the appropriate sub-type.
         */
        if (that instanceof StarlarkInt) {
          return repeat((StarlarkInt) that, this.mutability);
        }
      case PLUS:
        if (thisLeft) {
          return BinaryOperations.add(this, that, this.mutability);
        } else {
          return BinaryOperations.add(that, this, this.mutability);
        }
      default:
        // unsupported binary operation!
        return null;
    }
  }

  static class BinaryOperations {

    /**
     * Add right to left (i.e. [1] + [2] = [1, 2])
     */
    static public StarlarkBytes add(Object left, Object right, Mutability mutability) throws EvalException {
      StarlarkBytes left_ = toStarlarkByte(left, mutability);
      StarlarkBytes right_ = toStarlarkByte(right, mutability);
      return wrap(mutability, ByteList.copy("").join(left_.delegate, right_.delegate));
    }

    private static StarlarkBytes toStarlarkByte(Object item, Mutability mutability) throws EvalException {
      if (item instanceof StarlarkList) {
        Sequence<StarlarkInt> cast = Sequence.cast(
          item,
          StarlarkInt.class,
          "Attempted to add list of non-Integer type to a bytearray");
        return StarlarkBytes.copyOf(mutability, cast);
      }
      return (StarlarkBytes) item;
    }
  }

  /**
   * The Unicode replacement character inserted in place of decoding errors.
   */
  private static final char REPLACEMENT_CHAR = '\uFFFD';

  static final int[] TABLE_UTF8_NEEDED = new int[]{
    //      0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
    0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 0xc0 - 0xcf
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 0xd0 - 0xdf
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, // 0xe0 - 0xef
    3, 3, 3, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0xf0 - 0xff
  };

  /**
   * Returns a String for the UTF-8 encoded byte sequence in <code>bytes[0..len-1]</code>. The
   * length of the resulting String will be the exact number of characters encoded by these bytes.
   * Since UTF-8 is a variable-length encoding, the resulting String may have a length anywhere from
   * len/3 to len, depending on the contents of the input array.<p>
   *
   * In the event of a bad encoding, the UTF-8 replacement character (code point {@code \uFFFD})
   * is inserted for the bad byte(s), and decoding resumes from the next byte.
   */
  static public String UTF8toUTF16(byte[] data, int offset, int byteCount, boolean allowMalformed) {
    if ((offset | byteCount) < 0 || byteCount > data.length - offset) {
      throw new RuntimeException("index out of bound: " + data.length + " " + offset + " " + byteCount);
    }
    char[] value;
    int length;
    char[] v = new char[byteCount];

    int idx = offset;
    int last = offset + byteCount;
    int s = 0;

    int codePoint = 0;
    int utf8BytesSeen = 0;
    int utf8BytesNeeded = 0;
    int lowerBound = 0x80;
    int upperBound = 0xbf;
    int b;
    while (idx < last) {
      b = data[idx++] & 0xff;
      if (utf8BytesNeeded == 0) {
        if ((b & 0x80) == 0) { // ASCII char. 0xxxxxxx
          v[s++] = (char) b;
          continue;
        }

        if ((b & 0x40) == 0) { // 10xxxxxx is illegal as first byte
          v[s++] = REPLACEMENT_CHAR;
          continue;
        }

        // 11xxxxxx
        int tableLookupIndex = b & 0x3f;
        utf8BytesNeeded = TABLE_UTF8_NEEDED[tableLookupIndex];
        if (utf8BytesNeeded == 0) {
          v[s++] = REPLACEMENT_CHAR;
          continue;
        }

        // utf8BytesNeeded
        // 1: b & 0x1f
        // 2: b & 0x0f
        // 3: b & 0x07
        codePoint = b & (0x3f >> utf8BytesNeeded);
        if (b == 0xe0) {
          lowerBound = 0xa0;
        } else if (b == 0xed) {
          upperBound = 0x9f;
        } else if (b == 0xf0) {
          lowerBound = 0x90;
        } else if (b == 0xf4) {
          upperBound = 0x8f;
        }
      } else {
        if (b < lowerBound || b > upperBound) {
          // The bytes seen are ill-formed. Substitute them with U+FFFD
          v[s++] = REPLACEMENT_CHAR;
          codePoint = 0;
          utf8BytesNeeded = 0;
          utf8BytesSeen = 0;
          lowerBound = 0x80;
          upperBound = 0xbf;
          /*
           * According to the Unicode Standard,
           * "a UTF-8 conversion process is required to never consume well-formed
           * subsequences as part of its error handling for ill-formed subsequences"
           * The current byte could be part of well-formed subsequences. Reduce the
           * index by 1 to parse it in next loop.
           */
          idx--;
          continue;
        }

        lowerBound = 0x80;
        upperBound = 0xbf;
        codePoint = (codePoint << 6) | (b & 0x3f);
        utf8BytesSeen++;
        if (utf8BytesNeeded != utf8BytesSeen) {
          continue;
        }

        // Encode chars from U+10000 up as surrogate pairs
        if (codePoint < 0x10000) {
          v[s++] = (char) codePoint;
        } else {
          v[s++] = (char) ((codePoint >> 10) + 0xd7c0);
          v[s++] = (char) ((codePoint & 0x3ff) + 0xdc00);
        }

        utf8BytesSeen = 0;
        utf8BytesNeeded = 0;
        codePoint = 0;
      }
    }

    // The bytes seen are ill-formed.
    if (utf8BytesNeeded != 0) {
      for (int i = 0; i < utf8BytesNeeded; i++) {
        if(s + 1 >= v.length) {
          value = new char[s + (utf8BytesNeeded-i)];
          System.arraycopy(v, 0, value, 0, s);
          v = value;
        }
        // the total number of utf8BytesNeeded should be replaced by the
        // actual escaped characters themselves if allowMalformed is true.
        if (allowMalformed) {
          // we have to back track utf8BytesNeeded and insert the characters
          v[s++] = (char) (data[idx - utf8BytesNeeded + i] & 0xff);
        } else {
          // Substitute them by U+FFFD
          v[s++] = REPLACEMENT_CHAR;
        }
      }
    }

    if (s == byteCount) {
      // We guessed right, so we can use our temporary array as-is.
      value = v;
      length = s;
    } else {
      // Our temporary array was too big, so reallocate and copy.
      value = new char[s];
      length = s;
      System.arraycopy(v, 0, value, 0, s);
    }
    return String.copyValueOf(value, 0, length);
  }

  /**
   * The Starlark spec defines text strings as sequences of UTF-k codes that encode Unicode code
   * points. In this Java implementation, k=16, whereas in a Go implementation, k=8s. For
   * portability, operations on strings should aim to avoid assumptions about the value of k.
   */
  static public byte[] UTF16toUTF8(char[] val) {
    int dp = 0;
    int sp = 0;
    int sl = val.length;
    byte[] dst = new byte[sl * 3];
    char c;
    while (sp < sl && (c = val[sp]) < 0x80) {
      // ascii fast loop;
      dst[dp++] = (byte) c;
      sp++;
    }
    while (sp < sl) {
      c = val[sp++];
      if (c < 0x80) {
        dst[dp++] = (byte) c;
      } else if (c < 0x800) {
        dst[dp++] = (byte) (0xc0 | (c >> 6));
        dst[dp++] = (byte) (0x80 | (c & 0x3f));
      } else if (Character.isSurrogate(c)) {
        int uc = -1;
        char c2;
        if (Character.isHighSurrogate(c) && sp < sl &&
              Character.isLowSurrogate(c2 = val[sp])) {
          uc = Character.toCodePoint(c, c2);
        }
        if (uc < 0) {
          dst[dp++] = (byte) 0xEF;
          dst[dp++] = (byte) 0xBF;
          dst[dp++] = (byte) 0xBD;
        } else {
          dst[dp++] = (byte) (0xf0 | ((uc >> 18)));
          dst[dp++] = (byte) (0x80 | ((uc >> 12) & 0x3f));
          dst[dp++] = (byte) (0x80 | ((uc >> 6) & 0x3f));
          dst[dp++] = (byte) (0x80 | (uc & 0x3f));
          sp++;  // 2 chars
        }
      } else {
        // 3 bytes, 16 bits
        dst[dp++] = (byte) (0xe0 | ((c >> 12)));
        dst[dp++] = (byte) (0x80 | ((c >> 6) & 0x3f));
        dst[dp++] = (byte) (0x80 | (c & 0x3f));
      }
    }
    if (dp == dst.length) {
      return dst;
    }
    return Arrays.copyOf(dst, dp);
  }

  public static void quote(StringBuilder sb, int codePoint) {
    Character.UnicodeBlock of;
    if (!Character.isISOControl(codePoint)
          && Character.isValidCodePoint(codePoint)
          && (of = Character.UnicodeBlock.of(codePoint)) != null
          && of.equals(Character.UnicodeBlock.BASIC_LATIN)) {
      sb.append((char) codePoint);
    } else if (Character.isWhitespace(codePoint) || codePoint <= 0xff) {
      switch ((char) codePoint) {
        case '\b':
          sb.append("\\b");
          break;
        case '\t':
          sb.append("\\t");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\f':
          sb.append("\\f");
          break;
        case '\r':
          sb.append("\\r");
          break;
        default: {
          String s = Integer.toHexString(codePoint);
          if (codePoint < 0x100) {
            sb.append("\\x");
            if (s.length() == 1) {
              sb.append('0');
            }
            sb.append(s);
          } else {
            sb.append("\\x").append(s);
          }
        }
      }
    } else {
      switch (Character.getType(codePoint)) {
        case Character.CONTROL:     // Cc
        case Character.FORMAT:      // Cf
        case Character.PRIVATE_USE: // Co
        case Character.SURROGATE:   // Cs
        case Character.UNASSIGNED:  // Cn
          sb.append(String.format("\\u%04x", codePoint));
          break;
        default:
          sb.append(Character.toChars(codePoint));
          break;
      }
    }
  }
}
