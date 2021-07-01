package net.starlark.java.eval;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.ext.ByteStringModule;
import net.starlark.java.syntax.TokenKind;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class StarlarkBytes extends AbstractList<StarlarkBytes>
  implements ByteStringModule, Sequence<StarlarkBytes>, CharSequence, Comparable<StarlarkBytes>, HasBinary, StarlarkValue {

  // It's always possible to overeat in small bites but we'll
  // try to stop someone swallowing the world in one gulp.
  static final int MAX_ALLOC = 1 << 30;
  private final byte[] delegate;

  private final Mutability mutability;

  private static final byte[] EMPTY_ARRAY = new byte[]{};

  private StarlarkBytes(@Nullable Mutability mutability, byte[] elems) {
    this.mutability = mutability == null ? Mutability.IMMUTABLE : mutability;
    this.delegate = elems;
  }

  /**
   * Takes ownership of the supplied byte array and returns a new StarlarkBytes instance that
   * initially wraps the array. The caller must not subsequently modify the array, but the
   * StarlarkBytes instance may do so.
   */
  static StarlarkBytes wrap(@Nullable Mutability mutability, byte[] elems) {
    return new StarlarkBytes(mutability, elems);
  }

  @Override
  public boolean isImmutable() {
    return true; // Starlark spec says that Byte is immutable
  }

  /**
   * A shared instance for the empty immutable byte array.
   */
  private static final StarlarkBytes EMPTY = wrap(Mutability.IMMUTABLE, EMPTY_ARRAY);

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

  private static void checkElemsValid(StarlarkInt[] elems) {
    for (StarlarkInt elem : elems) {
      UnsignedBytes.checkedCast(elem.toIntUnchecked());
    }
  }

  /**
   * Returns an immutable byte array with the given elements. Equivalent to {@code copyOf(null,
   * elems)}.
   */
  public static StarlarkBytes immutableCopyOf(Iterable<StarlarkInt> elems) throws EvalException {
    return copyOf(null, elems);
  }

  /**
   * Returns a {@code StarlarkBytes} with the given items and the {@link Mutability}.
   */
  public static StarlarkBytes of(@Nullable Mutability mutability, byte... elems) {
    if (elems.length == 0) {
      return wrap(mutability, EMPTY_ARRAY);
    }

    return wrap(mutability, elems);
  }

  /**
   * Returns a {@code StarlarkBytes} with the given items and the {@link Mutability}.
   */
  public static StarlarkBytes of(@Nullable Mutability mutability, StarlarkInt... elems) {
    if (elems.length == 0) {
      return wrap(mutability, EMPTY_ARRAY);
    }

    checkElemsValid(elems);
    byte[] arr = new byte[elems.length];
    for (int i = 0; i < elems.length; i++) {
      arr[i] = UnsignedBytes.checkedCast(elems[i].toIntUnchecked());
    }
    return wrap(mutability, arr);
  }

  /**
   * Returns an immutable {@code StarlarkList} with the given items.
   */
  public static StarlarkBytes immutableOf(StarlarkInt... elems) {
    checkElemsValid(elems);
    byte[] arr = new byte[elems.length];
    for (int i = 0; i < elems.length; i++) {
      arr[i] = UnsignedBytes.checkedCast(elems[i].toIntUnchecked());
    }
    return wrap(null, arr);
  }

  /**
   * Returns a {@code StarlarkBytes} with the given items and the {@link Mutability}.
   */
  public static StarlarkBytes immutableOf(byte... elems) {
    return wrap(null, elems);
  }

  /**
   * Returns a {@code StarlarkBytes} with the given items and the {@link Mutability}.
   */
  public static StarlarkBytes immutableOf(char... elems) {
    byte[] barr = UTF16toUTF8(elems);
    return wrap(null, barr);
  }

  public byte[] getBytes() {
    return delegate;
  }

  public int[] getUnsignedBytes() {
    int[] arr = new int[delegate.length];
    for (int i = 0; i < delegate.length; i++) {
      arr[i] = Byte.toUnsignedInt(delegate[i]);
    }
    return arr;
  }

  @Override
  public StarlarkBytes get(int index) {
    return StarlarkBytes.of(mutability, this.delegate[index]); // can throw OutOfBounds
  }

  @Override
  public int hashCode() {
    // Fnv32 hash
    byte[] input = this.getBytes();
    if (input == null) {
      return 0;
    }

    int hash = -2128831035;
    for (byte b : input) {
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
      return -1 != Bytes.indexOf(this.delegate, ((StarlarkBytes) key).getBytes());
    } else if (key instanceof StarlarkInt) {
      StarlarkInt _key = ((StarlarkInt) key);
      if (!Range
             .closed(0, 255)
             .contains(_key.toIntUnchecked())) {
        throw Starlark.errorf("int in bytes: %s out of range", _key);
      }
      return -1 != Bytes.indexOf(this.delegate, (byte) ((StarlarkInt) key).toIntUnchecked());
    }
    //"requires bytes or int as left operand, not string"
    throw new EvalException(
      String.format("requires bytes or int as left operand, not %s", Starlark.type(key))
    );
  }

  @Override
  public int size() {
    return this.delegate.length;
  }

  @Override
  public int compareTo(@Nonnull StarlarkBytes o) {
    return UnsignedBytes
             .lexicographicalComparator()
             .compare(getBytes(), o.getBytes());
  }


  @Override
  public void str(Printer printer) {
    String s = UTF8toUTF16(getBytes(), 0, getBytes().length, false);
    printer.append(s);
  }

  @Override
  public void repr(Printer printer) {
    byte[] bytes = getBytes();
    String s = UTF8toUTF16(bytes, 0, bytes.length, /*allowMalformed*/true);
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      quote(b, s.codePointAt(i));
    }
    printer.append(String.format("b\"%s\"", b.toString()));
  }

  @Override
  public StarlarkBytes getSlice(Mutability mu, int start, int stop, int step) {
    RangeList indices = new RangeList(start, stop, step);
    int n = indices.size();
    byte[] res = new byte[n];
    if (step == 1) { // common case
      System.arraycopy(this.delegate, indices.at(0), res, 0, n);
    } else {
      for (int i = 0; i < n; ++i) {
        res[i] = this.delegate[indices.at(i)];
      }
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
    return _hex(getBytes(), sep, nbytesPerSep);
  }

  @Override
  public int count(Object sub, Object start, Object end) throws EvalException {
    byte[] subarr = fromStarlarkObjectToByteArray(sub);
    return _count(
      getBytes(),
      subarr,
      Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "count"),
      ByteStringModule.isNullOrNoneOrUnbound(end) ? size() : Starlark.toInt(end, "count"));
  }

  @Override
  public StarlarkBytes removeprefix(StarlarkBytes prefix) throws EvalException {
   byte[] prefixBytes = prefix.getBytes();
   final int prefix_len = prefixBytes.length;
   final int self_len = size();
   if(self_len >= prefix_len
       && prefix_len > 0
       && _startsWith(getBytes(),0,prefixBytes)) {
     return wrap(mutability, Arrays.copyOfRange(getBytes(), prefix_len,prefix_len + (self_len - prefix_len)));
   }
   return this;
  }

  @Override
  public StarlarkBytes removesuffix(StarlarkBytes suffix) throws EvalException {
    byte[] suffixBytes = suffix.getBytes();
    final int suffix_len = suffixBytes.length;
    final int self_len = size();
    if(self_len >= suffix_len
        && suffix_len > 0
        && _endsWith(getBytes(),self_len-suffix_len, suffixBytes, 0,suffix_len)) {
      return wrap(mutability, Arrays.copyOfRange(getBytes(),0,self_len-suffix_len));
    }
    return this;
   }

  @Override
  public boolean endsWith(Object suffix, Object start, Object end) throws EvalException {
    byte[][] suffixes;
    if(suffix instanceof StarlarkBytes) {
      suffixes = new byte[][] { ((StarlarkBytes)suffix).getBytes() };
    }
    else {
      Tuple _seq = ((Tuple) suffix); // we want to throw a class cast exception here if not tuple
      Sequence<StarlarkBytes> seq = Sequence.cast(_seq, StarlarkBytes.class, "endsWith");
      suffixes = new byte[seq.size()][];
      for (int i = 0, seqSize = seq.size(); i < seqSize; i++) {
        suffixes[i] = seq.get(i) // does not allocate because Tuple returns item @ index
                        .getBytes();
      }
    }
    return _endsWith(
      getBytes(),
      Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "endsWith"),
      Starlark.isNullOrNone(end) ? size() : Starlark.toInt(end, "endsWith"),
      suffixes
    );
  }

  @Override
  public int find(Object sub, Object start, Object end) throws EvalException {
    byte[] subarr = fromStarlarkObjectToByteArray(sub);
    return _find(
     /*forward*/true,
     getBytes(),
     subarr,
     Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "find"),
     ByteStringModule.isNullOrNoneOrUnbound(end) ? size() : Starlark.toInt(end, "find"));
  }


  @Override
  public int index(Object sub, Object start, Object end) throws EvalException {
    byte[] subarr = fromStarlarkObjectToByteArray(sub);
    int loc = _find(
      /*forward*/true,
      getBytes(),
      subarr,
      Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "index"),
      ByteStringModule.isNullOrNoneOrUnbound(end) ? size() : Starlark.toInt(end, "index"));
    if(loc == -1) {
      throw Starlark.errorf("subsection not found");
    }
    return loc;
  }

  @Override
  public StarlarkBytes join(Sequence<StarlarkBytes> elements) throws EvalException {
    byte[][] parts = new byte[elements.size()][];
    //noinspection ForLoopReplaceableByForEach --- allocation-free looping
    for (int i = 0; i < elements.size(); i++) {
      parts[i] = elements.get(i).getBytes();
    }
    byte[] joined = _join(getBytes(), parts);
    return StarlarkBytes.immutableOf(joined);
  }

  @Override
  public Tuple partition(StarlarkBytes sep) throws EvalException {
    int i = _find(true, getBytes(), sep.getBytes(), 0, size());
    if(i != -1) {
      return Tuple.of(
        StarlarkBytes.immutableOf(Arrays.copyOfRange(getBytes(), 0, i)),
        sep,
        StarlarkBytes.immutableOf(Arrays.copyOfRange(getBytes(), i + sep.size(), size()))
      );
    }
    return Tuple.of(this, StarlarkBytes.empty(),StarlarkBytes.empty());
  }

  @Override
  public StarlarkBytes replace(StarlarkBytes oldBytes, StarlarkBytes newBytes, StarlarkInt countI, StarlarkThread thread) throws EvalException {
    int count = Starlark.isNullOrNone(countI)
                  ? Integer.MAX_VALUE
                  : Starlark.toInt(countI, "replace");
    if(count == -1) {
      count = Integer.MAX_VALUE;
    }
    byte[] bytes = _replace(getBytes(), size(), oldBytes.getBytes(), newBytes.getBytes(), count);
    return wrap(mutability, bytes);
  }

  @Override
   public int rfind(Object sub, Object start, Object end) throws EvalException {
    byte[] subarr = fromStarlarkObjectToByteArray(sub);
     return _find(
       /*forward*/false,
       getBytes(),
       subarr,
       Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "rfind"),
       ByteStringModule.isNullOrNoneOrUnbound(end) ? size() : Starlark.toInt(end, "rfind")
     );
   }

  @Override
   public int rindex(Object sub, Object start, Object end) throws EvalException {
    byte[] subarr = fromStarlarkObjectToByteArray(sub);
    int loc = _find(
      /*forward*/false,
      getBytes(),
      subarr,
      Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "index"),
      ByteStringModule.isNullOrNoneOrUnbound(end) ? size() : Starlark.toInt(end, "index"));
    if(loc == -1) {
      throw Starlark.errorf("subsection not found");
    }
    return loc;
   }

  @Override
  public Tuple rpartition(StarlarkBytes sep) throws EvalException {
    int i = _find(false, getBytes(), sep.getBytes(), 0, size());
    if(i != -1) {
      return Tuple.of(
        StarlarkBytes.immutableOf(Arrays.copyOfRange(getBytes(), 0, i)),
        sep,
        StarlarkBytes.immutableOf(Arrays.copyOfRange(getBytes(), i + sep.size(), size()))
      );
    }
    return Tuple.of(StarlarkBytes.empty(),StarlarkBytes.empty(), this);
  }

  @Override
  public boolean startsWith(Object prefix, Object start, Object end) throws EvalException {
    byte[][] prefixes;
    if(prefix instanceof StarlarkBytes) {
      prefixes = new byte[][] { ((StarlarkBytes)prefix).getBytes() };
    }
    else {
      Tuple _seq = ((Tuple) prefix); // we want to throw a class cast exception here if not tuple
      Sequence<StarlarkBytes> seq = Sequence.cast(_seq, StarlarkBytes.class, "startsWith");
      prefixes = new byte[seq.size()][];
      for (int i = 0, seqSize = seq.size(); i < seqSize; i++) {
        prefixes[i] = seq.get(i) // does not allocate because Tuple returns item @ index
                        .getBytes();
      }
    }
    return _startsWith(
      getBytes(),
      Starlark.isNullOrNone(start) ? 0 : Starlark.toInt(start, "startsWith"),
      Starlark.isNullOrNone(end) ? size() : Starlark.toInt(end, "startsWith"),
      prefixes
    );
  }

  @Override
  public StarlarkBytes translate(Object tableO, StarlarkBytes delete) throws EvalException {
    StarlarkBytes table = empty();
    int dellen = delete.size();
    boolean changed = false;

    if (!Starlark.isNullOrNone(tableO)) {
      table = ((StarlarkBytes) tableO);
      if (table.size() != 256) {
        throw Starlark.errorf(
          "translation table must be 256 characters long. length of table was %d",
          table.size());
      }
    }

    final int total_size = size();

    if(dellen == 0 && !table.isEmpty()) {
      byte[] result = new byte[total_size];
      /* If no deletions are required, use faster code */
      for (int i = 0; i < total_size; i++) {
        byte c = byteAt(i);
        byte v = table.byteAt(c);
        if(!changed && c != v) {
          changed = true;
        }
        result[i] = v;
      }
      if(!changed) {
        return this;
      }
      return wrap(mutability, result);
    }

    byte[] table_bytes = null;
    if(!table.isEmpty()) {
      table_bytes = table.getBytes();
    }

    boolean[] toDelete = createDeleteTable(delete.getBytes());
    int resultLen = 0;
    byte[] result = new byte[total_size];

    for (int i = 0; i < total_size; i++) {
      byte c = byteAt(i);
      if(!toDelete[c]) {
        byte v = table_bytes == null ? c : table_bytes[c];
        if (!changed && c != v) {
            changed = true;
        }
        result[resultLen] = v;
        resultLen++;
      }
    }
    if(!changed && resultLen == total_size) {
      return this;
    }
    // optimize for pre-allocated if resultLen = 0
    if(resultLen == 0) {
      return empty();
    }
    return wrap(mutability, Arrays.copyOf(result, resultLen));
  }

  @Override
  public StarlarkBytes center(StarlarkInt width, StarlarkBytes fillbyte) throws EvalException {
    if(fillbyte.size() != 1) {
          throw new EvalException("fillbyte must be of length 1");
        }
        int nwidth = width.toIntUnchecked();
        if((nwidth - size()) <= 0) {
          return this;
        }
    int marg = nwidth - size();
    int left = marg / 2 + (marg & nwidth & 1);
    byte[] res = pad(getBytes(), left, marg - left, fillbyte.byteAt(0));
    return wrap(this.mutability, res);
  }

  @Override
  public StarlarkBytes ljust(StarlarkInt width, StarlarkBytes fillbyte) throws EvalException {
    if(fillbyte.size() != 1) {
      throw new EvalException("fillbyte must be of length 1");
    }
    int nwidth = width.toIntUnchecked();
    if((nwidth - size()) <= 0) {
      return this;
    }
    int l = nwidth - size();
    int resLen = l + size();
    byte[] res = new byte[resLen];
    System.arraycopy(getBytes(), 0, res, 0, size());
    Arrays.fill(res, size(), resLen, fillbyte.byteAt(0));
    return wrap(this.mutability, res);
  }

  @Override
  public StarlarkBytes lstrip(Object charsO) {
    byte[] stripped;
    if(Starlark.isNullOrNone(charsO)) {
      stripped = do_strip(getBytes(), LEFTSTRIP);
      return StarlarkBytes.immutableOf(stripped);
    }
    stripped = do_xstrip(getBytes(), LEFTSTRIP, ((StarlarkBytes) charsO).getBytes());
    return StarlarkBytes.immutableOf(stripped);
  }

  @Override
  public StarlarkBytes rjust(StarlarkInt width, StarlarkBytes fillbyte)  throws EvalException {
    if(fillbyte.size() != 1) {
      throw new EvalException("fillbyte must be of length 1");
    }
    int nwidth = width.toIntUnchecked();
    if((nwidth - size()) <= 0) {
      return this;
    }
    int l = nwidth - size();
    int resLen = l + size();
    byte[] res = new byte[resLen];
    Arrays.fill(res, 0, l, fillbyte.byteAt(0));
    for (int i = l, j = 0; i < (size() + l); j++, i++) {
      res[i] = this.byteAt(j);
    }
    return wrap(this.mutability, res);
  }

  @Override
  public StarlarkList<StarlarkBytes> rsplit(Object bytesO, Object maxSplitO, StarlarkThread thread) throws EvalException {
    int maxSplit = Starlark.isNullOrNone(maxSplitO)
                  ? Integer.MAX_VALUE
                  : Starlark.toInt(maxSplitO, "rsplit");
    if(maxSplit == -1) {
      maxSplit = Integer.MAX_VALUE;
    }
    List<byte[]> split;
    if (Starlark.isNullOrNone(bytesO)) {
      split = _rsplitWhitespace(this.getBytes(), maxSplit);
    } else {
      split = _rsplit(getBytes(),((StarlarkBytes)bytesO).getBytes(), maxSplit);
    }
    StarlarkList<StarlarkBytes> res = StarlarkList.newList(thread.mutability());
    for (int i = 0; i < split.size(); i++) {
      res.addElement(StarlarkBytes.immutableOf(split.get(i)));
    }
    return res;
  }

  @Override
  public StarlarkBytes rstrip(Object charsO) {
    byte[] stripped;
    if(Starlark.isNullOrNone(charsO)) {
      stripped = do_strip(getBytes(), RIGHTSTRIP);
      return StarlarkBytes.immutableOf(stripped);
    }
    stripped = do_xstrip(getBytes(), RIGHTSTRIP, ((StarlarkBytes) charsO).getBytes());
    return StarlarkBytes.immutableOf(stripped);
  }

  @Override
  public StarlarkList<StarlarkBytes> split(Object bytesO, Object maxSplitO, StarlarkThread thread) throws EvalException {
    int maxSplit = Starlark.isNullOrNone(maxSplitO)
                  ? Integer.MAX_VALUE
                  : Starlark.toInt(maxSplitO,"split");
    if(maxSplit == -1) {
      maxSplit = Integer.MAX_VALUE;
    }
    List<byte[]> split;
    if (Starlark.isNullOrNone(bytesO)) {
      split = _splitWhitespace(this.getBytes(), maxSplit);
    } else {
      split = _split(getBytes(),((StarlarkBytes) bytesO).getBytes(), maxSplit);
    }
    StarlarkList<StarlarkBytes> res = StarlarkList.newList(thread.mutability());
    for (int i = 0; i < split.size(); i++) {
      res.addElement(StarlarkBytes.immutableOf(split.get(i)));
    }
    return res;
  }

  @Override
  public StarlarkBytes strip(Object charsO) {
    byte[] stripped;
    if(Starlark.isNullOrNone(charsO)) {
      stripped = do_strip(getBytes(), BOTHSTRIP);
      return StarlarkBytes.immutableOf(stripped);
    }
    stripped = do_xstrip(getBytes(), BOTHSTRIP, ((StarlarkBytes) charsO).getBytes());
    return StarlarkBytes.immutableOf(stripped);
  }

  @Override
  public StarlarkBytes capitalize() throws EvalException {
    return StarlarkBytes.immutableOf(_capitalize(getBytes()));
  }

  @Override
  public StarlarkBytes expandTabs(StarlarkInt tabSize) throws EvalException {
    if(size() == 0) {
      return empty();
    }
    try {
      byte[] result = _expandTabs(getBytes(), tabSize.toInt("expandTabs"), Integer.MAX_VALUE);
      return wrap(mutability, result);
    } catch(ArrayIndexOutOfBoundsException ex) {
      throw new EvalException(ex);
    }
  }

  @Override
  public boolean isAlnum() throws EvalException {
    return _isalnum(getBytes());
  }

  @Override
  public boolean isAlpha() throws EvalException {
    return _isalpha(getBytes());
  }

  @Override
  public boolean isAscii() throws EvalException {
    return _isAscii(getBytes());
  }

  @Override
  public boolean isDigit() throws EvalException {
    return _isdigit(getBytes());
  }

  @Override
  public boolean isLower() throws EvalException {
    return _islower(getBytes());
  }

  @Override
  public boolean isSpace() throws EvalException {
    return _isspace(getBytes());
  }

  @Override
  public boolean isTitle() throws EvalException {
    return _istitle(getBytes());
  }

  @Override
  public boolean isUpper() throws EvalException {
    return _isupper(getBytes());
  }

  @Override
  public StarlarkBytes lower() throws EvalException {
    return StarlarkBytes.immutableOf(_lower(getBytes()));
  }

  @Override
  public Sequence<StarlarkBytes> splitLines(boolean keepEnds) throws EvalException {
    byte[] bytes = getBytes();
    int length = bytes.length;
    int start = 0;
    StarlarkList<StarlarkBytes> list = StarlarkList.newList(this.mutability);

    for (int i = 0; i < length; i++) {
      if (bytes[i] == '\n' || bytes[i] == '\r') {
          int end = i;
          if (bytes[i] == '\r' && i + 1 != length && bytes[i + 1] == '\n') {
              i++;
          }
          if (keepEnds) {
              end = i + 1;
          }
          byte[] slice = new byte[end - start];
          System.arraycopy(bytes, start, slice, 0, slice.length);
          list.addElement(StarlarkBytes.immutableOf(slice));
          start = i + 1;
      }
    }
    if(start == length) {
      return list;
    }
    // We have remaining parts, so let's process it.
    byte[] slice = new byte[length - start];
    System.arraycopy(bytes, start, slice, 0, slice.length);
    list.addElement(StarlarkBytes.immutableOf(slice));
    return list;
  }

  @Override
  public StarlarkBytes swapcase() throws EvalException {
    return StarlarkBytes.immutableOf(_swapcase(getBytes()));
  }

  @Override
  public StarlarkBytes title() throws EvalException {
    return StarlarkBytes.immutableOf(_title(getBytes()));
  }

  @Override
  public StarlarkBytes upper() throws EvalException {
    return StarlarkBytes.immutableOf(_upper(getBytes()));
  }

  @Override
  public StarlarkBytes zfill(StarlarkInt width) throws EvalException {
    int nwidth = width.toIntUnchecked();

    int len = delegate.length;
    if (len >= nwidth) {
        return this;
    }

    int fill = nwidth - len;
    byte[] p = pad(delegate, fill, 0, (byte) '0');

    if (len == 0) {
        return StarlarkBytes.immutableOf(p);
    }

    if (p[fill] == '+' || p[fill] == '-') {
        /* move sign to beginning of string */
        p[0] = p[fill];
        p[fill] = '0';
    }
    return StarlarkBytes.immutableOf(p);
  }

  /**
   * Ensures the truth of an expression involving one or more parameters
   * to the calling method.
   */
  static void checkArgument(boolean b, @Nullable String errorMessageTemplate, int p1) throws EvalException {
    if (!b) {
      throw new EvalException(Strings.lenientFormat(errorMessageTemplate, p1));
    }
  }

  private byte[] fromStarlarkObjectToByteArray(Object sub) throws EvalException {
    if (sub instanceof StarlarkBytes) {
      return ((StarlarkBytes) sub).getBytes();
    }

    StarlarkInt sub1 = (StarlarkInt) sub;
    int x;
    try {
      x = sub1.toInt("byte must be in range(0, 256)");
    } catch (IllegalArgumentException e) {
      throw new EvalException(e.getMessage(), e.getCause());
    }
    checkArgument(x >> Byte.SIZE == 0,
      "byte must be in range(0, 256). received %d", x);
    return new byte[]{(byte) x};
  }

  @Override
  public int length() {
    return size();
  }

  @Override
  public char charAt(int index) {
    return (char) delegate[index];
  }

  public byte byteAt(int index) {
    return delegate[index];
  }

  public byte[] subArray(int start, int end) {
    byte[] barr = getBytes();
    long indices = ByteStringModule.subsequenceIndices(barr, start, end);
    return Arrays.copyOfRange(barr, ByteStringModule.lo(indices), ByteStringModule.hi(indices));
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    byte[] barr = subArray(start,end);
    return StarlarkBytes.immutableOf(barr);
  }

  @StarlarkBuiltin(name = "bytes.elems")
  public class StarlarkByteElems extends AbstractList<StarlarkInt>
    implements Sequence<StarlarkInt> {

    final private StarlarkBytes bytes;

    public StarlarkByteElems(StarlarkBytes bytes) {
      this.bytes = bytes;
    }

    // TODO(mahmoud): is this needed?
    public byte[] getBytes() {
      return bytes.getBytes();
    }

    @Override
    public void repr(Printer printer) {
      printer.append(
        String.format("b\"%s\".elems()",
          UTF8toUTF16(
            this.bytes.getBytes(),
            0,
            this.bytes.getBytes().length,
            /*allowMalformed*/ true
          )));
    }

    @Override
    public StarlarkInt get(int index) {
      int[] bytes = this.bytes.get(index).getUnsignedBytes();
      // guaranteed to be one entry per slice.
      // an index on a byte array will return 1 byte
      return StarlarkInt.of(bytes[0]); // so this is safe.
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
    if (n.signum() <= 0) {
      return wrap(mutability, EMPTY_ARRAY);
    }

    int ni = n.toInt("repeat");
    long sz = (long) ni * delegate.length;
    if (sz > MAX_ALLOC) {
      throw Starlark.errorf("excessive repeat (%d * %d elements)", delegate.length, ni);
    }
    byte[] res = new byte[(int) sz];
    for (int i = 0; i < ni; i++) {
      System.arraycopy(delegate, 0, res, i * delegate.length, delegate.length);
    }
    return wrap(mutability, res);
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
          return BinaryOperations.add(this, that);
        } else {
          return BinaryOperations.add(that, this);
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
    static public StarlarkBytes add(Object left, Object right) throws EvalException {
      StarlarkBytes left_ = toStarlarkByte(left);
      StarlarkBytes right_ = toStarlarkByte(right);

      byte[] seq = Bytes.concat(left_.getBytes(), right_.getBytes());
      return wrap(null, seq);
    }

    private static StarlarkBytes toStarlarkByte(Object item) throws EvalException {
      if (item instanceof StarlarkList) {
        Sequence<StarlarkInt> cast = Sequence.cast(
          item,
          StarlarkInt.class,
          "Attempted to add list of non-Integer type to a bytearray");
        return StarlarkBytes.copyOf(null, cast);
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
   * In the event of a bad encoding, the UTF-8 replacement character (code point U+FFFD) is inserted
   * for the bad byte(s), and decoding resumes from the next byte.
   */
  static public String UTF8toUTF16(byte[] data, int offset, int byteCount, boolean allowMalformed) {
    if ((offset | byteCount) < 0 || byteCount > data.length - offset) {
      throw new RuntimeException("index out of bound: " + data.length + " " + offset + " " + byteCount);
    }
    char[] value;
    int length;
    byte[] d = data;
    char[] v = new char[byteCount];

    int idx = offset;
    int last = offset + byteCount;
    int s = 0;

    int codePoint = 0;
    int utf8BytesSeen = 0;
    int utf8BytesNeeded = 0;
    int lowerBound = 0x80;
    int upperBound = 0xbf;
    int b = 0;
    while (idx < last) {
      b = d[idx++] & 0xff;
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
        // the total number of utf8BytesNeeded should be replaced by the
        // actual escaped characters themselves if allowMalformed is true.
        if (allowMalformed) {
          // we have to back track utf8BytesNeeded and insert the characters
          v[s++] = (char) (d[idx - utf8BytesNeeded + i] & 0xff);
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
    Character.UnicodeBlock of = Character.UnicodeBlock.of(codePoint);
    if (!Character.isISOControl(codePoint)
          && of != null
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
