package net.starlark.java.eval;

import com.google.common.base.Ascii;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.syntax.TokenKind;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class StarlarkByte extends AbstractList<StarlarkByte>
  implements Sequence<StarlarkByte>, Comparable<StarlarkByte>, HasBinary, StarlarkValue {

  // It's always possible to overeat in small bites but we'll
  // try to stop someone swallowing the world in one gulp.
  static final int MAX_ALLOC = 1 << 30;
  private byte[] delegate;

  private final Mutability mutability;

  private static final byte[] EMPTY_ARRAY = new byte[]{};

  static int HIGH_BYTE_SHIFT;
  static int LOW_BYTE_SHIFT;

  static {
    if (ByteOrder.LITTLE_ENDIAN.equals(ByteOrder.nativeOrder())) {
      HIGH_BYTE_SHIFT = 0;
      LOW_BYTE_SHIFT = 8;
    } else {
      HIGH_BYTE_SHIFT = 8;
      LOW_BYTE_SHIFT = 0;
    }
  }

  private StarlarkByte(@Nullable Mutability mutability, byte[] elems) {
    this.mutability = mutability == null ? Mutability.IMMUTABLE : mutability;
    this.delegate = elems;
  }

  /**
   * Takes ownership of the supplied byte array and returns a new StarlarkByte instance that
   * initially wraps the array. The caller must not subsequently modify the array, but the
   * StarlarkByte instance may do so.
   */
  static StarlarkByte wrap(@Nullable Mutability mutability, byte[] elems) {
    return new StarlarkByte(mutability, elems);
  }

  @Override
  public boolean isImmutable() {
    return true; // Starlark spec says that Byte is immutable
  }

  /**
   * A shared instance for the empty immutable byte array.
   */
  private static final StarlarkByte EMPTY = wrap(Mutability.IMMUTABLE, EMPTY_ARRAY);

  /**
   * Returns an immutable instance backed by an empty byte array.
   */
  public static StarlarkByte empty() {
    return EMPTY;
  }

  /**
   * Returns a {@code StarlarkByte} whose items are given by an iterable of StarlarkInt and which
   * has the given {@link Mutability}.
   */
  public static StarlarkByte copyOf(
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
  public static StarlarkByte immutableCopyOf(Iterable<StarlarkInt> elems) throws EvalException {
    return copyOf(null, elems);
  }

  /**
   * Returns a {@code StarlarkByte} with the given items and the {@link Mutability}.
   */
  public static StarlarkByte of(@Nullable Mutability mutability, byte... elems) {
    if (elems.length == 0) {
      return wrap(mutability, EMPTY_ARRAY);
    }

    return wrap(mutability, elems);
  }

  /**
   * Returns a {@code StarlarkByte} with the given items and the {@link Mutability}.
   */
  public static StarlarkByte of(@Nullable Mutability mutability, StarlarkInt... elems) {
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
  public static StarlarkByte immutableOf(StarlarkInt... elems) {
    checkElemsValid(elems);
    byte[] arr = new byte[elems.length];
    for (int i = 0; i < elems.length; i++) {
      arr[i] = UnsignedBytes.checkedCast(elems[i].toIntUnchecked());
    }
    return wrap(null, arr);
  }

  /**
   * Returns a {@code StarlarkByte} with the given items and the {@link Mutability}.
   */
  public static StarlarkByte immutableOf(byte... elems) {
    return wrap(null, elems);
  }

  /**
   * Returns a {@code StarlarkByte} with the given items and the {@link Mutability}.
   */
  public static StarlarkByte immutableOf(char... elems) {
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
  public StarlarkByte get(int index) {
    return StarlarkByte.of(mutability, this.delegate[index]); // can throw OutOfBounds
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
    if (!(o instanceof StarlarkByte)) {
      return false;
    }
    if (this == o) {
      return true;
    }
    return this.compareTo((StarlarkByte) o) == 0;
  }

  @Override
  public boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {
    if (key instanceof StarlarkByte) {
      return -1 != Bytes.indexOf(this.delegate, ((StarlarkByte) key).getBytes());
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

  public static String hexlify(byte[] data) {
    StringBuilder builder = new StringBuilder();
    for (byte b : data) {
      builder.append(String.format("%02x", b));
    }
    return builder.toString();
  }

  public static byte[] unhexlify(String data) {
    int length = data.length();
    byte[] result = new byte[length / 2];
    for (int i = 0; i < length; i += 2) {
      result[i / 2] = (byte) ((Character.digit(data.charAt(i), 16) << 4)
                                + Character.digit(data.charAt(i + 1), 16));
    }
    return result;
  }

  protected StarlarkByte copy(byte[] bytes) throws EvalException {
    if (bytes == null) {
      bytes = getBytes();
    }
    return wrap(mutability, Arrays.copyOf(bytes, bytes.length));
  }

  @Override
  public int size() {
    return this.delegate.length;
  }

  @Override
  public int compareTo(@Nonnull StarlarkByte o) {
    return UnsignedBytes
             .lexicographicalComparator()
             .compare(getBytes(), o.getBytes());
  }


  @Override
  public void str(Printer printer) {
    //printer.append(starlarkStringTranscoding(getBytes()));
    String s = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(getBytes())).toString();
    //String s = UTF8toUTF16(getBytes(), 0, getBytes().length);
    printer.append(s);
  }

  @Override
  public void repr(Printer printer) {
//    String s = decodeUTF8(getBytes(), getBytes().length);
//    String s2 = String.format("b\"%s\"", s);
    printer.append(repr(getBytes()));

  }

  public static String repr(byte[] bytes) {
    //    String s = decodeUTF8(bytes, bytes.length);
    String s = UTF8toUTF16(bytes, 0, bytes.length, true);
    //String s = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      quote(b, s.codePointAt(i));
    }
    return String.format("b\"%s\"", b.toString());
  }

  @Override
  public StarlarkByte getSlice(Mutability mu, int start, int stop, int step) {
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

  @StarlarkBuiltin(name = "bytes.elems")
  public class StarlarkByteElems extends AbstractList<StarlarkInt>
    implements Sequence<StarlarkInt> {

    final private StarlarkByte bytes;

    public StarlarkByteElems(StarlarkByte bytes) {
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
            true
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


  @StarlarkMethod(name = "hex")
  public String hex() {
    return hexlify(getBytes());
  }

  @StarlarkMethod(
    name = "decode",
    parameters = {
      @Param(name = "encoding", defaultValue = "'utf-8'"),
      @Param(name = "errors", defaultValue = "'strict'")
    })
  public String decode(String encoding, String errors) throws EvalException {
    CharsetDecoder decoder = Charset.forName(encoding)
                               .newDecoder()
                               .onMalformedInput(CodecHelper.convertCodingErrorAction(errors))
                               .onUnmappableCharacter(CodecHelper.convertCodingErrorAction(errors));
    try {
      return new String(
        decoder.decode(
          ByteBuffer.wrap(getBytes())
        ).array());
    } catch (CharacterCodingException e) {
      throw Starlark.errorf(e.getMessage());
    }
  }

  @StarlarkMethod(
    name = "lower",
    doc = "B.lower() -> copy of B\n" +
            "\n" +
            "Return a copy of B with all ASCII characters converted to lowercase.")
  public StarlarkByte lower() throws EvalException {
    byte[] bytes = getBytes();
    for (int i = 0; i < bytes.length; i++) {
      byte b = (byte) Ascii.toLowerCase((char) bytes[i]);
      bytes[i] = b;
    }
    return copy(bytes);
  }

  @StarlarkMethod(
    name = "upper",
    doc = "B.upper() -> copy of B\n" +
            "\n" +
            "Return a copy of B with all ASCII characters converted to uppercase.")
  public StarlarkByte upper() throws EvalException {
    byte[] bytes = getBytes();
    for (int i = 0; i < bytes.length; i++) {
      byte b = (byte) Ascii.toUpperCase((char) bytes[i]);
      bytes[i] = b;
    }
    return copy(bytes);
  }

  @StarlarkMethod(
    name = "split",
    doc = "" +
            "Return a list of the sections in the bytes, using sep as the delimiter.\n" +
            "\n" +
            "sep\n" +
            "  The delimiter according which to split the bytes.\n" +
            "  None (the default value) means split on ASCII whitespace characters\n" +
            "  (space, tab, return, newline, formfeed, vertical tab).\n" +
            "maxsplit\n" +
            "  Maximum number of splits to do.\n" +
            "  -1 (the default value) means no limit.",
    parameters = {
      @Param(name = "bytes", doc = "The bytes to split on.", allowedTypes = {
        @ParamType(type = StarlarkByte.class),
        @ParamType(type = NoneType.class)
      }, defaultValue = "None"),
      @Param(
        name = "maxsplit",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None",
        doc = "The maximum number of splits.")
    },
    useStarlarkThread = true)
  public StarlarkList<StarlarkByte> split(Object bytesO, Object maxSplitO, StarlarkThread thread) throws EvalException {
    int maxSplit = Integer.MAX_VALUE;
    if (maxSplitO != Starlark.NONE) {
      maxSplit = Starlark.toInt(maxSplitO, "maxsplit");
    }
    List<byte[]> split;
    if (Starlark.isNullOrNone(bytesO)) {
      split = StarlarkByteUtils.splitOnWhitespace(this.getBytes(), StarlarkByteUtils.LATIN1_WHITESPACE);
    } else {
      split = StarlarkByteUtils.split(this.getBytes(), 0, this.size(), ((StarlarkByte) bytesO).getBytes());
    }

    StarlarkList<StarlarkByte> res = StarlarkList.newList(thread.mutability());

    if (maxSplit < split.size()) {
      for (int i = 0; i < maxSplit; i++) {
        res.addElement(wrap(thread.mutability(), split.get(i)));
      }
    } else {
      for (byte[] i : split) {
        res.addElement(wrap(thread.mutability(), i));
      }
    }
    return res;
  }


  @StarlarkMethod(
    name = "lstrip",
    parameters = {
      @Param(
        name = "bytes",
        allowedTypes = {
          @ParamType(type = StarlarkByte.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None")
    })
  public StarlarkByte lstrip(Object bytesOrNone) throws EvalException {
    byte[] pattern = bytesOrNone != Starlark.NONE ? ((StarlarkByte) bytesOrNone).getBytes() : StarlarkByteUtils.LATIN1_WHITESPACE;
    byte[] replaced = StarlarkByteUtils.lstrip(this.getBytes(), pattern);
    //return stringLStrip(self, chars);
    return wrap(null, replaced);
  }

  @StarlarkMethod(
    name = "join",
    doc = "Concatenate any number of bytes objects.\n" +
            "\n" +
            "The bytes whose method is called is inserted in between each pair.\n" +
            "\n" +
            "The result is returned as a new bytes object.\n" +
            "\n" +
            "Example: b'.'.join([b'ab', b'pq', b'rs']) -> b'ab.pq.rs'.\n",
    parameters = {
      @Param(name = "iterable_of_bytes", doc = "The bytes to join,")
    })
  public StarlarkByte join(Object elements) throws EvalException {
    Iterable<?> items = Starlark.toIterable(elements);
    int i = 0;
    List<byte[]> parts = new ArrayList<>();
    for (Object item : items) {
      if (!(item instanceof StarlarkByte)) {
        throw Starlark.errorf(
          "expected bytes for sequence element %d, got '%s'", i, Starlark.type(item));
      }
      parts.add(((StarlarkByte) item).getBytes());
      i++;
    }
    byte[] joined = StarlarkByteUtils.join(getBytes(), parts);
    return wrap(null, joined);
  }


  @StarlarkMethod(
    name = "endswith",
    doc = "B.endswith(suffix[, start[, end]]) -> bool\n" +
            "\n" +
            "Return True if B ends with the specified suffix, False otherwise.\n" +
            "With optional start, test B beginning at that position.\n" +
            "With optional end, stop comparing B at that position.\n" +
            "suffix can also be a tuple of bytes to try.\n.",
    parameters = {
      @Param(
        name = "suffix",
        allowedTypes = {
          @ParamType(type = StarlarkByte.class),
          @ParamType(type = Tuple.class, generic1 = StarlarkByte.class),
        },
        doc = "The suffix (or tuple of alternative suffixes) to match."),
      @Param(
        name = "start",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "0",
        doc = "Test beginning at this position."),
      @Param(
        name = "end",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None",
        doc = "optional position at which to stop comparing.")
    })
  public boolean endsWith(Object suffix, Object start, Object end) throws EvalException {
    long indices = subsequenceIndices(this.getBytes(), start, end);
    if (suffix instanceof StarlarkByte) {
      return byteArrayEndsWith(lo(indices), hi(indices), ((StarlarkByte) suffix).getBytes());
    }
    for (StarlarkByte s : Sequence.cast(suffix, StarlarkByte.class, "sub")) {
      if (byteArrayEndsWith(lo(indices), hi(indices), s.getBytes())) {
        return true;
      }
    }
    return false;
  }

  // Computes bytes.substring(start, end).endsWith(suffix) without allocation.
  private boolean byteArrayEndsWith(int start, int end, byte[] suffix) {
    int n = suffix.length;
    return start + n <= end && StarlarkByteUtils.endsWith(
      this.getBytes(), end - n, suffix, 0, n
    );
  }

  @StarlarkMethod(
    name = "startswith",
    doc = "B.startswith(prefix[, start[, end]]) -> bool\n" +
            "\n" +
            "Return True if B starts with the specified prefix, False otherwise.\n" +
            "With optional start, test B beginning at that position.\n" +
            "With optional end, stop comparing B at that position.\n" +
            "prefix can also be a tuple of bytes to try.\n",
    parameters = {
      @Param(
        name = "prefix",
        allowedTypes = {
          @ParamType(type = StarlarkByte.class),
          @ParamType(type = Tuple.class, generic1 = StarlarkByte.class),
        },
        doc = "The prefix (or tuple of alternative prefixes) to match."),
      @Param(
        name = "start",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "0",
        doc = "Test beginning at this position."),
      @Param(
        name = "end",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None",
        doc = "Stop comparing at this position.")
    })
  public boolean startsWith(Object sub, Object start, Object end)
    throws EvalException {

    long indices = subsequenceIndices(this.getBytes(), start, end);
    if (sub instanceof StarlarkByte) {
      return byteArrayStartsWith(lo(indices), hi(indices), ((StarlarkByte) sub).getBytes());
    }
    for (StarlarkByte s : Sequence.cast(sub, StarlarkByte.class, "sub")) {
      if (byteArrayStartsWith(lo(indices), hi(indices), s.getBytes())) {
        return true;
      }
    }
    return false;
  }

  // Computes bytes.range(start, end).startsWith(prefix) without allocation.
  private boolean byteArrayStartsWith(int start, int end, byte[] prefix) {
    return start + prefix.length <= end && StarlarkByteUtils.startsWith(getBytes(), start, prefix);
  }

  @StarlarkMethod(
    name = "rfind",
    doc =
      "Return the highest index in the sequence where the subsequence sub is found, " +
        "such that sub is contained within s[start:end]. " +
        "Optional arguments start and end are interpreted as in slice notation. " +
        "Return -1 on failure.\n" +
        "\n" +
        "The subsequence to search for may be any bytes-like object or an integer in " +
        "the range 0 to 255.",
    parameters = {
      @Param(name = "sub", allowedTypes = {
        @ParamType(type = StarlarkInt.class),
        @ParamType(type = StarlarkByte.class),
      }),
      @Param(
        name = "start",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "0",
        doc = "Restrict to search from this position."),
      @Param(
        name = "end",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None",
        doc = "optional position before which to restrict to search.")
    })
  public int rfind(Object sub, Object start, Object end) throws EvalException {
    if (sub instanceof StarlarkInt) {
      byte b = UnsignedBytes.checkedCast(((StarlarkInt) sub).toIntUnchecked());
      return byteSequenceFind(false, getBytes(), new byte[]{b}, start, end);
    }
    StarlarkByte b = (StarlarkByte) sub;
    return byteSequenceFind(false, this.getBytes(), b.getBytes(), start, end);
  }

  public static int lastIndexOf(byte[] self, byte[] b, int start, int end) {
    outer:
    for (int i = end - 1; i >= start; i--) {
      for (int j = 0; j < b.length; j++) {
        if (self[i + j] != b[j]) {
          continue outer; // this literally..uses a goto..wtf
        }
      }
      return i;
    }
    return -1;
  }

  /**
   * Returns a new StarlarkByte containing n consecutive repeats of this byte array.
   */
  public StarlarkByte repeat(StarlarkInt n, Mutability mutability) throws EvalException {
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

  /**
   * Common implementation for find, rfind, index, rindex.
   *
   * @param forward true if we want to return the last matching index.
   */
  private static int byteSequenceFind(boolean forward, byte[] self, byte[] sub, Object start, Object end)
    throws EvalException {
    long indices = subsequenceIndices(self, start, end);
    // Unfortunately Java forces us to allocate here, even though
    // String has a private indexOf method that accepts indices.
    // Fortunately the common case is self[0:n].
    byte[] subRange = Arrays.copyOfRange(self, lo(indices), hi(indices));
    int subpos = forward ? Bytes.indexOf(subRange, sub) : lastIndexOf(subRange, sub, lo(indices), hi(indices));
    return subpos < 0
             ? subpos //
             : subpos + lo(indices);
  }

  // Returns the byte array denoted by byte[start:end], which is never out of bounds.
  // For speed, we don't return byte.range(start, end), to stop allocating a copy.
  // Instead we return the (start, end) indices, packed into the lo/hi arms of a long. @_@
  private static long subsequenceIndices(byte[] bytearr, Object start, Object end) throws EvalException {
    // This function duplicates the logic of Starlark.slice for bytes.
    int n = bytearr.length;
    int istart = 0;
    if (start != Starlark.NONE) {
      istart = toIndex(Starlark.toInt(start, "start"), n);
    }
    int iend = n;
    if (end != Starlark.NONE) {
      iend = toIndex(Starlark.toInt(end, "end"), n);
    }
    if (iend < istart) {
      iend = istart; // => empty result
    }
    return pack(istart, iend); // = str.substring(start, end)
  }

  /**
   * Returns the effective index denoted by a user-supplied integer. First, if the integer is
   * negative, the length of the sequence is added to it, so an index of -1 represents the last
   * element of the sequence. Then, the integer is "clamped" into the inclusive interval [0,
   * length].
   */
  static int toIndex(int index, int length) {
    if (index < 0) {
      index += length;
    }

    if (index < 0) {
      return 0;
    } else if (index > length) {
      return length;
    } else {
      return index;
    }
  }

  private static long pack(int lo, int hi) {
    return (((long) hi) << 32) | (lo & 0xffffffffL);
  }

  private static int lo(long x) {
    return (int) x;
  }

  private static int hi(long x) {
    return (int) (x >>> 32);
  }

  @Nullable
  @Override
  public Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    switch (op) {
      case STAR:
        if (that instanceof StarlarkInt) {
          return repeat((StarlarkInt) that, this.mutability);
        }
      default:
        // unsupported binary operation!
        return null;
    }
  }


  static class BinaryOperations {

    /**
     * Attempts to multiply a StarlarkByte type by an integer. The caller is responsible for casting
     * to the appropriate sub-type.
     */
    static public StarlarkByte multiply(StarlarkByte target, Integer n) throws EvalException {
      return target.repeat(StarlarkInt.of(n.intValue()), null);
    }

    /**
     * Add right to left (i.e. [1] + [2] = [1, 2])
     */
    static public StarlarkByte add(Object left, Object right) throws EvalException {
      StarlarkByte left_ = toStarlarkByte(left);
      StarlarkByte right_ = toStarlarkByte(right);

      byte[] seq = Bytes.concat(left_.getBytes(), right_.getBytes());
      return wrap(null, seq);
    }

    private static StarlarkByte toStarlarkByte(Object item) throws EvalException {
      if (item instanceof StarlarkList) {
        Sequence<StarlarkInt> cast = Sequence.cast(
          item,
          StarlarkInt.class,
          "Attempted to add list of non-Integer type to a bytearray");
        return StarlarkByte.copyOf(null, cast);
      }
      return (StarlarkByte) item;
    }
  }

  private static class CodecHelper {
    public static final String STRICT = "strict";
    public static final String IGNORE = "ignore";
    public static final String REPLACE = "replace";
    public static final String BACKSLASHREPLACE = "backslashreplace";
    public static final String NAMEREPLACE = "namereplace";
    public static final String XMLCHARREFREPLACE = "xmlcharrefreplace";
    public static final String SURROGATEESCAPE = "surrogateescape";
    public static final String SURROGATEPASS = "surrogatepass";

    public static CodingErrorAction convertCodingErrorAction(String errors) {
      CodingErrorAction errorAction;
      switch (errors) {
        case IGNORE:
          errorAction = CodingErrorAction.IGNORE;
          break;
        case REPLACE:
        case NAMEREPLACE:
          errorAction = CodingErrorAction.REPLACE;
          break;
        case STRICT:
        case BACKSLASHREPLACE:
        case SURROGATEPASS:
        case SURROGATEESCAPE:
        case XMLCHARREFREPLACE:
        default:
          errorAction = CodingErrorAction.REPORT;
          break;
      }
      return errorAction;
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
  static public String UTF8toUTF16(byte[] data, int offset, int byteCount, boolean bool) {
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

    // The bytes seen are ill-formed. Substitute them by U+FFFD
    if (utf8BytesNeeded != 0) {
      // the total number of utf8BytesNeeded should be replaced by the
      // actual escaped characters themselves if bool is true.
      // -- we have to back track utf8BytesNeeded and insert the characters
      if(bool) {
        for (int i = 0; i < utf8BytesNeeded; i++) {
          v[s++] = (char) (d[idx - utf8BytesNeeded + i] & 0xff);
        }
      }
      else {
        v[s++] = REPLACEMENT_CHAR;
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
    * The Starlark spec defines text strings as sequences of UTF-k
    * codes that encode Unicode code points. In this Java implementation,
    * k=16, whereas in a Go implementation, k=8s. For portability,
    * operations on strings should aim to avoid assumptions about
    * the value of k.
   */
  static public byte[] UTF16toUTF8(char[] val) {
    int dp = 0;
    int sp = 0;
    int sl = val.length;
    byte[] dst = new byte[sl * 3];
    char c;
    while (sp < sl && (c = val[sp]) < 0x80) {
       // ascii fast loop;
       dst[dp++] = (byte)c;
       sp++;
    }
    while (sp < sl) {
       c = val[sp++];
       if (c < 0x80) {
           dst[dp++] = (byte)c;
       } else if (c < 0x800) {
           dst[dp++] = (byte)(0xc0 | (c >> 6));
           dst[dp++] = (byte)(0x80 | (c & 0x3f));
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
               dst[dp++] = (byte)(0xf0 | ((uc >> 18)));
               dst[dp++] = (byte)(0x80 | ((uc >> 12) & 0x3f));
               dst[dp++] = (byte)(0x80 | ((uc >>  6) & 0x3f));
               dst[dp++] = (byte)(0x80 | (uc & 0x3f));
               sp++;  // 2 chars
           }
       } else {
           // 3 bytes, 16 bits
           dst[dp++] = (byte)(0xe0 | ((c >> 12)));
           dst[dp++] = (byte)(0x80 | ((c >>  6) & 0x3f));
           dst[dp++] = (byte)(0x80 | (c & 0x3f));
       }
    }
    if (dp == dst.length) {
       return dst;
    }
    return Arrays.copyOf(dst, dp);
   }

  public static void quote(StringBuilder sb, int codePoint) {
    if (Character.isLowSurrogate((char) codePoint) || Character.isHighSurrogate((char) codePoint)) {
      sb.append(REPLACEMENT_CHAR);
    } else {
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
            //sb.append(String.format("\\x%02x", codePoint));
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
}
