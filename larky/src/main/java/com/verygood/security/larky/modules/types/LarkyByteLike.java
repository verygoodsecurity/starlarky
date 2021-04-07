package com.verygood.security.larky.modules.types;


import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;

import com.verygood.security.larky.modules.utils.ByteArrayUtil;
import com.verygood.security.larky.modules.utils.FnvHash;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;


public abstract class LarkyByteLike extends AbstractList<StarlarkInt> implements Comparable<LarkyByteLike>, Sequence<StarlarkInt> {

  private StarlarkList<StarlarkInt> delegate;

  Sequence<StarlarkInt> getSequenceStorage() {
    return this.delegate;
  }

  void setSequenceStorage(Sequence<StarlarkInt> store) {
    delegate = StarlarkList.immutableCopyOf(store.getImmutableList());
  }

  public abstract byte[] getBytes();

  public int[] getUnsignedBytes() {
    return Bytes.asList(getBytes())
        .stream()
        .map(Byte::toUnsignedInt)
        .mapToInt(i -> i)
        .toArray();
  }

  @Override
  public int hashCode() {
    return FnvHash.FnvHash32.hash(this.getBytes());
  }

  @Override
  public boolean containsKey(StarlarkSemantics semantics, Object key) throws EvalException {
    if (key instanceof LarkyByteLike) {
      // https://stackoverflow.com/a/32865087/133514
      //noinspection unchecked
      return -1 != Collections.indexOfSubList(getSequenceStorage(), (LarkyByteLike) key);
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

  @Override
  public int size() {
    return getSequenceStorage().size();
  }

  @Override
  public abstract boolean isImmutable();

  @Override
  public int compareTo(@NotNull LarkyByteLike o) {
    return UnsignedBytes
        .lexicographicalComparator()
        .compare(getBytes(), o.getBytes());
  }

  protected abstract ByteLikeBuilder builder();

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
          @Param(name = "bytes", doc = "The bytes to split on."),
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
   public StarlarkList<LarkyByteLike> split(Object bytesO, Object maxSplitO, StarlarkThread thread) throws EvalException {
    int maxSplit = Integer.MAX_VALUE;
    if (maxSplitO != Starlark.NONE) {
      maxSplit = Starlark.toInt(maxSplitO, "maxsplit");
    }
    List<byte[]> split;
    if(Starlark.isNullOrNone(bytesO)) {
      split = ByteArrayUtil.splitOnWhitespace(this.getBytes(), ByteArrayUtil.LATIN1_WHITESPACE);
    } else {
      split = ByteArrayUtil.split(this.getBytes(), 0, this.size(), ((LarkyByteLike) bytesO).getBytes());
    }

    StarlarkList<LarkyByteLike> res = StarlarkList.newList(thread.mutability());

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
                  @ParamType(type = LarkyByteLike.class),
                  @ParamType(type = NoneType.class),
              },
              defaultValue = "None")
      })
  public LarkyByteLike lstrip(Object bytesOrNone) throws EvalException {
    byte[] pattern = bytesOrNone != Starlark.NONE ? ((LarkyByteLike) bytesOrNone).getBytes() : ByteArrayUtil.LATIN1_WHITESPACE;
    byte[] replaced = ByteArrayUtil.lstrip(this.getBytes(), pattern);
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
  public LarkyByteLike join(Object elements) throws EvalException {
    Iterable<?> items = Starlark.toIterable(elements);
    int i = 0;
    List<byte[]> parts = new ArrayList<>();
    for (Object item : items) {
      if (!(item instanceof LarkyByteLike)) {
        throw Starlark.errorf(
            "expected bytes for sequence element %d, got '%s'", i, Starlark.type(item));
      }
      parts.add(((LarkyByteLike) item).getBytes());
      i++;
    }
    byte[] joined = ByteArrayUtil.join(getBytes(), parts);
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
                  @ParamType(type = LarkyByteLike.class),
                  @ParamType(type = Tuple.class, generic1 = LarkyByteLike.class),
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
    if (suffix instanceof LarkyByteLike) {
      return byteArrayEndsWith(lo(indices), hi(indices), ((LarkyByteLike) suffix).getBytes());
    }
    for (LarkyByteLike s : Sequence.cast(suffix, LarkyByteLike.class, "sub")) {
      if (byteArrayEndsWith(lo(indices), hi(indices), s.getBytes())) {
        return true;
      }
    }
    return false;
  }

  // Computes bytes.substring(start, end).endsWith(suffix) without allocation.
  private boolean byteArrayEndsWith(int start, int end, byte[] suffix) {
    int n = suffix.length;
    return start + n <= end && ByteArrayUtil.endsWith(
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
                  @ParamType(type = LarkyByteLike.class),
                  @ParamType(type = Tuple.class, generic1 = LarkyByteLike.class),
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
    if (sub instanceof LarkyByteLike) {
      return byteArrayStartsWith(lo(indices), hi(indices), ((LarkyByteLike) sub).getBytes());
    }
    for (LarkyByteLike s : Sequence.cast(sub, LarkyByteLike.class, "sub")) {
      if (byteArrayStartsWith(lo(indices), hi(indices), s.getBytes())) {
        return true;
      }
    }
    return false;
  }

  // Computes bytes.range(start, end).startsWith(prefix) without allocation.
  private boolean byteArrayStartsWith(int start, int end, byte[] prefix) {
    return start + prefix.length <= end && ByteArrayUtil.startsWith(getBytes(), start, prefix);
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
              @ParamType(type = LarkyByteLike.class),
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
    LarkyByteLike b = (LarkyByteLike) sub;
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

    LarkyByteLike build() throws EvalException;
  }

  static class BinaryOperations {

    /**
     * Attempts to multiply a LarkyByteLike type by an integer. The caller is responsible for
     * casting to the appropriate sub-type.
     */
    static public LarkyByteLike multiply(LarkyByteLike target, Integer num) throws EvalException {
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
     */
    static public StarlarkList<StarlarkInt> add(LarkyByteLike left, LarkyByteLike right) throws EvalException {
      StarlarkList<StarlarkInt> seq;
      seq = StarlarkList.concat(
          StarlarkList.immutableCopyOf(left.getSequenceStorage().getImmutableList()),
          StarlarkList.immutableCopyOf(right.getSequenceStorage().getImmutableList()),
          null);
      return seq;
    }
  }
}
