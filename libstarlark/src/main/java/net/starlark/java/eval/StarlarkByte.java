package net.starlark.java.eval;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.syntax.TokenKind;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class StarlarkByte extends AbstractList<StarlarkInt> implements Comparable<StarlarkByte>, Sequence<StarlarkInt>, HasBinary {

  /**
   * The Unicode replacement character inserted in place of decoding errors.
   */
  private static final char REPLACEMENT_CHAR = '\uFFFD';
  private StarlarkList<StarlarkInt> delegate;
  private final StarlarkThread currentThread;
  private final Map<String, Object> fields = new HashMap<>();


  private StarlarkByte(Builder builder) throws EvalException {
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

  public StarlarkByteElems elems() {
    return new StarlarkByteElems(this);
  }


  Sequence<StarlarkInt> getSequenceStorage() {
    return this.delegate;
  }

  void setSequenceStorage(Sequence<StarlarkInt> store) {
    delegate = StarlarkList.immutableCopyOf(store.getImmutableList());
  }

  public byte[] getBytes() {
    return Bytes.toArray(this.getSequenceStorage().stream()
            .map(StarlarkInt::toNumber)
            .map(Number::byteValue)
            .collect(Collectors.toList()));
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

  public int[] getUnsignedBytes() {
    return Bytes.asList(getBytes())
        .stream()
        .map(Byte::toUnsignedInt)
        .mapToInt(i -> i)
        .toArray();
  }

  public char[] toCharArray(Charset cs) {
    CharBuffer charBuffer = cs.decode(ByteBuffer.wrap(getBytes()));
    return Arrays.copyOf(charBuffer.array(), charBuffer.limit());
  }

  public char[] toCharArray() {
    // this is the right default charset for char arrays
    // specially in a password context
    // see: https://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords
    // as well as: https://stackoverflow.com/a/9670279/133514
    return toCharArray(StandardCharsets.ISO_8859_1);
  }

  @Override
  public StarlarkInt get(int index) {
    return this.getSequenceStorage().get(index);
  }

  @Override
  public int hashCode() {
    return FnvHash32.hash(this.getBytes());
  }

  @Override
  public boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {
    if (key instanceof StarlarkByte) {
      // https://stackoverflow.com/a/32865087/133514
      //noinspection unchecked
      return -1 != Collections.indexOfSubList(getSequenceStorage(), (StarlarkByte) key);
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
    if(bytes == null) {
      bytes = getBytes();
    }
    return this.builder().setSequence(bytes).build();
  }

  @Override
  public int size() {
    return getSequenceStorage().size();
  }

  @Override
  public int compareTo(@NotNull StarlarkByte o) {
    return UnsignedBytes
        .lexicographicalComparator()
        .compare(getBytes(), o.getBytes());
  }


  @Override
  public boolean isImmutable() {
    return true;
  }

  @Override
  public void str(Printer printer) {
    printer.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(getBytes())));
  }

  @Override
  public void repr(Printer printer) {
//    String s = decodeUTF8(getBytes(), getBytes().length);
//    String s2 = String.format("b\"%s\"", s);
    printer.append(repr(getBytes()));
  }

  public static String repr(byte[] bytes) {
    String s = decodeUTF8(bytes, bytes.length);
    return String.format("b\"%s\"", s);
  }

  @Override
  public Sequence<StarlarkInt> getSlice(Mutability mu, int start, int stop, int step) {
    StarlarkList<StarlarkInt> c = StarlarkList.copyOf(mu, new ArrayList<>(this.getSequenceStorage()));
    try {
      return this.builder()
          .setSequence(c.getSlice(mu, start, stop, step).stream()
              .map(StarlarkInt::toIntUnchecked)
              .map(Integer::byteValue)
              .map(Byte::toUnsignedInt)
              .mapToInt(i -> i)
              .toArray()
          ).build();
    } catch (EvalException e) {
      throw new RuntimeException(e.getMessage(), e.fillInStackTrace());
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
              @ParamType(type=StarlarkByte.class),
              @ParamType(type=NoneType.class)
          }, defaultValue="None"),
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
    if(Starlark.isNullOrNone(bytesO)) {
      split = StarlarkByteUtils.splitOnWhitespace(this.getBytes(), StarlarkByteUtils.LATIN1_WHITESPACE);
    } else {
      split = StarlarkByteUtils.split(this.getBytes(), 0, this.size(), ((StarlarkByte) bytesO).getBytes());
    }

    StarlarkList<StarlarkByte> res = StarlarkList.newList(thread.mutability());

    if(maxSplit < split.size()) {
      for (int i = 0; i < maxSplit; i++) {
        res.addElement(this.builder().setSequence(split.get(i)).build());
      }
    }

    else {
      for (byte[] i : split) {
        res.addElement(this.builder().setSequence(i).build());
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
    return this.builder().setSequence(replaced).build();
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
    return builder().setSequence(joined).build();
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
    return null;
  }


  public interface ByteLikeBuilder {

    default ByteLikeBuilder setSequence(byte[] buf) throws EvalException {
      return setSequence(ByteBuffer.wrap(buf));
    }

    default ByteLikeBuilder setSequence(byte[] buf, int off, int ending) throws EvalException {
      return setSequence(ByteBuffer.wrap(buf, off, ending));
    }

    default ByteLikeBuilder setSequence(@Nonnull CharSequence string) throws EvalException {
      return setSequence(string.chars().toArray());
    }

    default ByteLikeBuilder setSequence(ByteBuffer buf) throws EvalException {
      int[] arr = new int[buf.remaining()];
      for (int i = 0; i < arr.length; i++) {
        arr[i] = Byte.toUnsignedInt(buf.get(i));
      }
      return setSequence(arr);
    }

    default ByteLikeBuilder setSequence(int[] iterable_of_ints) throws EvalException {
      StarlarkList<StarlarkInt> collect = StarlarkList.immutableCopyOf(
          IntStream.of(iterable_of_ints)
              .mapToObj(StarlarkInt::of)
              .collect(Collectors.toList()));
      setSequence(collect);
      return this;
    }

    ByteLikeBuilder setSequence(@Nonnull Sequence<?> seq) throws EvalException;

    StarlarkByte build() throws EvalException;
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
    public StarlarkByte build() throws EvalException {
         return new StarlarkByte(this);
     }

   }

  protected ByteLikeBuilder builder() {
    return builder(this.currentThread);
  }

  protected static ByteLikeBuilder builder(StarlarkThread thread) {
    return new Builder(thread);
  }

  static class BinaryOperations {

    /**
     * Attempts to multiply a StarlarkByte type by an integer. The caller is responsible for
     * casting to the appropriate sub-type.
     */
    static public StarlarkByte multiply(StarlarkByte target, Integer num) throws EvalException {
      List<Sequence<StarlarkInt>> copies = Collections.nCopies(num, target.getSequenceStorage());
      Iterable<StarlarkInt> joined = Iterables.concat(copies);
      byte[] bytes = Bytes.toArray(
          Streams.stream(joined)
              .map(StarlarkInt::toNumber)
              .collect(Collectors.toList())
      );
      return target.builder().setSequence(bytes).build();
    }

    /**
     * Add right to left (i.e. [1] + [2] = [1, 2])
     * @return
     */
    static public StarlarkByte add(Object left, Object right) throws EvalException {
      StarlarkByte left_ = toStarlarkByte(left);
      StarlarkByte right_ = toStarlarkByte(right);

      StarlarkList<StarlarkInt> seq;
      seq = StarlarkList.concat(
          StarlarkList.immutableCopyOf(left_.getSequenceStorage().getImmutableList()),
          StarlarkList.immutableCopyOf(right_.getSequenceStorage().getImmutableList()),
          null);
      return left_.builder().setSequence(seq).build();
    }

    private static StarlarkByte toStarlarkByte(Object item) throws EvalException {
      if(item instanceof StarlarkList) {
        Sequence<StarlarkInt> cast = Sequence.cast(
            item,
            StarlarkInt.class,
            "Attempted to add list of non-Integer type to a bytearray");
        return builder(null).setSequence(cast).build();
      }
      return (StarlarkByte) item;
    }
  }

  private static class FnvHash32 {
    private FnvHash32() {}

       /**
        * Length of the hash is 32-bits (4-bytes), {@value}.
        */
       private static final int LENGTH = 4;

       /**
        * Default FNV-1 seed, {@value} == (signed) 2166136261
        */
       private static final int DEFAULT_SEED_INT = -2128831035;

       /**
        * Byte representation of DEFAULT_SEED_INT
        */
       protected static final byte[] DEFAULT_SEED = (BigInteger
           .valueOf(DEFAULT_SEED_INT)
           .toByteArray());

       /**
        * Default FNV-1 prime, {@value}.
        */
       public static final long DEFAULT_PRIME = 16777619;

       /**
        * FNV-1a 32-bit hash function
        *
        * @param input Input to hash
        * @return 32-bit FNV-1a hash
        */
       public static int hash(byte[] input) {
         return hash(input, DEFAULT_SEED_INT);
       }

       /**
        * FNV-1a 32-bit hash function
        *
        * @param input Input to hash
        * @param seed  Seed to use as the offset
        * @return 32-bit FNV-1a hash
        */
       public static int hash(byte[] input, int seed) {
         if (input == null) {
           return 0;
         }

         int hash = seed;
         for (byte b : input) {
           hash ^= b;
           hash *= DEFAULT_PRIME;
         }
         return hash;
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

  @StarlarkBuiltin(name = "bytes.elems")
  public class StarlarkByteElems extends AbstractList<StarlarkInt> implements Sequence<StarlarkInt> {

    final private StarlarkByte byteArray;

    public StarlarkByteElems(StarlarkByte byteArray) {
      this.byteArray = byteArray;
    }

    public byte[] getBytes() {
      return getLarkyByteArr().getBytes();
    }

    public StarlarkByte getLarkyByteArr() {
      return byteArray;
    }

    @Override
    public void repr(Printer printer) {
      printer.append(
          String.format("b'\"%s\".elems()'",
              decodeUTF8(
                  this.byteArray.getBytes(),
                  this.byteArray.getBytes().length
              )));
    }

    @Override
    public StarlarkInt get(int index) {
      return this.byteArray.get(index);
    }

    @Override
    public int size() {
      return this.byteArray.size();
    }

    @Override
    public Sequence<StarlarkInt> getSlice(Mutability mu, int start, int stop, int step) {
      return this.byteArray.getSlice(mu, start, stop, step);
    }
  }

  /**
     * Returns a String for the UTF-8 encoded byte sequence in <code>bytes[0..len-1]</code>. The
     * length of the resulting String will be the exact number of characters encoded by these bytes.
     * Since UTF-8 is a variable-length encoding, the resulting String may have a length anywhere from
     * len/3 to len, depending on the contents of the input array.<p>
     *
     * In the event of a bad encoding, the UTF-8 replacement character (code point U+FFFD) is inserted
     * for the bad byte(s), and decoding resumes from the next byte.
     */
    /*test*/
    public static String decodeUTF8(byte[] bytes, int len) {
      char[] res = new char[len];
      int cIx = 0;
      for (int bIx = 0; bIx < len; cIx++) {
        byte b1 = bytes[bIx];
        if ((b1 & 0x80) == 0) {
          // 1-byte sequence (U+0000 - U+007F)
          res[cIx] = (char) b1;
          bIx++;
        } else if ((b1 & 0xE0) == 0xC0) {
          // 2-byte sequence (U+0080 - U+07FF)
          byte b2 = (bIx + 1 < len) ? bytes[bIx + 1] : 0; // early end of array
          if ((b2 & 0xC0) == 0x80) {
            res[cIx] = (char) (((b1 & 0x1F) << 6) | (b2 & 0x3F));
            bIx += 2;
          } else {
            // illegal 2nd byte
            res[cIx] = REPLACEMENT_CHAR;
            bIx++; // skip 1st byte
          }
        } else if ((b1 & 0xF0) == 0xE0) {
          // 3-byte sequence (U+0800 - U+FFFF)
          byte b2 = (bIx + 1 < len) ? bytes[bIx + 1] : 0; // early end of array
          if ((b2 & 0xC0) == 0x80) {
            byte b3 = (bIx + 2 < len) ? bytes[bIx + 2] : 0; // early end of array
            if ((b3 & 0xC0) == 0x80) {
              res[cIx] = (char) (((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F));
              bIx += 3;
            } else {
              // illegal 3rd byte
              res[cIx] = REPLACEMENT_CHAR;
              bIx += 2; // skip 1st TWO bytes
            }
          } else {
            // illegal 2nd byte
            res[cIx] = REPLACEMENT_CHAR;
            bIx++; // skip 1st byte
          }
        } else {
          // illegal 1st byte
          res[cIx] = REPLACEMENT_CHAR;
          bIx++; // skip 1st byte
        }
      }
      return new String(res, 0, cIx);
    }

  /**
     * Magic numbers for UTF-8. These are the number of bytes that <em>follow</em> a given lead byte.
     * Trailing bytes have the value -1. The values 4 and 5 are presented in this table, even though
     * valid UTF-8 cannot include the five and six byte sequences.
     */
    static final int[] bytesFromUTF8 =
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0,
            // trail bytes
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3,
            3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5};

    static final int[] offsetsFromUTF8 =
        {0x00000000, 0x00003080, 0x000E2080, 0x03C82080, 0xFA082080, 0x82082080};

    /**
     * Returns the next code point at the current position in the buffer. The buffer's position will
     * be incremented. Any mark set on this buffer will be changed by this method!
     *
     * @param bytes the incoming bytes
     * @return the corresponding unicode codepoint
     */
    static public int bytesToCodePoint(ByteBuffer bytes) {
      bytes.mark();
      byte b = bytes.get();
      bytes.reset();
      int extraBytesToRead = bytesFromUTF8[(b & 0xFF)];
      if (extraBytesToRead < 0) {
        return -1; // trailing byte!
      }
      int ch = 0;

      switch (extraBytesToRead) {
        case 5:
          ch += (bytes.get() & 0xFF);
          ch <<= 6; /* remember, illegal UTF-8 */
          // fall through
        case 4:
          ch += (bytes.get() & 0xFF);
          ch <<= 6; /* remember, illegal UTF-8 */
          // fall through
        case 3:
          ch += (bytes.get() & 0xFF);
          ch <<= 6;
          // fall through
        case 2:
          ch += (bytes.get() & 0xFF);
          ch <<= 6;
          // fall through
        case 1:
          ch += (bytes.get() & 0xFF);
          ch <<= 6;
          // fall through
        case 0:
          ch += (bytes.get() & 0xFF);
          break;
        default: // do nothing
      }
      ch -= offsetsFromUTF8[extraBytesToRead];

      return ch;
    }

  // Returns the Java UTF-16 string containing the single rune |r|.
    public static String runeToString(int r) {
      char c = (char) r;
      return r == c ? String.valueOf(c) : new String(Character.toChars(c));
    }

  public static String starlarkStringTranscoding(byte[] bytearr) {
      /*
      The starlark spec says that UTF-8 gets encoded to UTF-K,
      where K is the host language: Go, Rust is UTF-8 and Java is
      UTF-16.
       */
      StringBuffer sb = new StringBuffer();
      ByteBuffer buf = ByteBuffer.wrap(bytearr);
      int lastpos = 0;
      int l = bytearr.length;
      while(buf.hasRemaining()) {
        int r = 0;
        try {
          r = bytesToCodePoint(buf);
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
          sb.append(REPLACEMENT_CHAR);
        }
        else {
          sb.append(runeToString(r));
        }

        //System.out.println(Integer.toHexString(r));
        //System.out.println("Chars: " + Arrays.toString(Character.toChars(r)));
      }
      return sb.toString();
    }


}
