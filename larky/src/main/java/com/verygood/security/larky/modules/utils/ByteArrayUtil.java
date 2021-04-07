package com.verygood.security.larky.modules.utils;

import com.google.common.primitives.Bytes;

import org.bouncycastle.util.Pack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Utility functions for operating on byte arrays. Although built for the FoundationDB tuple layer,
 * some functions may be useful otherwise, such as use of {@link #printable(byte[])} for debugging
 * non-text keys and values.
 */
public class ByteArrayUtil {
  private static final byte EQUALS_CHARACTER = (byte) '=';
  private static final byte DOUBLE_QUOTE_CHARACTER = (byte) '"';
  private static final byte BACKSLASH_CHARACTER = (byte) '\\';
  private static final byte MINIMUM_PRINTABLE_CHARACTER = 32;
  private static final int MAXIMUM_PRINTABLE_CHARACTER = 127;

  private static final char[] HEX_CHARS =
      {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  /**
   * For consistency with Python we recognize the same whitespace characters as they do over the
   * range 0x00-0xFF. See https://github.com/python/cpython/blob/master/Objects/unicodetype_db.h#L6208-L6243
   * This list is a consequence of Unicode character information.
   *
   * <p>Note that this differs from Python 2.7, which uses ctype.h#isspace(), and from
   * java.lang.Character#isWhitespace(), which does not recognize U+00A0.
   */
  public static final byte[] LATIN1_WHITESPACE =
      ("\u0009" + "\n" + "\u000B" + "\u000C" + "\r" + "\u001C" + "\u001D" + "\u001E" + "\u001F"
          + "\u0020" + "\u0085" + "\u00A0").getBytes(StandardCharsets.US_ASCII);


  @Nullable
  public static String toHexString(@Nullable byte[] bytes) {
    if (bytes == null) {
      return null;
    }

    char[] hex = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hex[j * 2] = HEX_CHARS[v >>> 4];
      hex[j * 2 + 1] = HEX_CHARS[v & 0x0F];
    }
    return new String(hex);
  }

  /**
     * Fills the given array with array.length new instances of the given class.
     * @return the array
     */
    public static <T> T[] fill(T[] array, Class<T> c) throws ReflectiveOperationException {
      for (int i = 0; i < array.length; i++) {
        array[i] = c.getDeclaredConstructor().newInstance();
      }
      return array;
    }

    public static String toHexString(@NotNull byte[] bytes, int digitGrouping, String groupDelimiter) {
      if (bytes == null || bytes.length == 0)
        return "";
      char[] hexChars = toHexString(bytes).toCharArray();
      StringBuilder buf = new StringBuilder(bytes.length*3);
      for (int i = 0; i < hexChars.length; i++) {
        buf.append(hexChars[i]);
        if (i < hexChars.length-1 && i % digitGrouping == (digitGrouping-1))
          buf.append(groupDelimiter);
      }
      return buf.toString();
    }

  @Nullable
  public static String loggable(byte[] bytes) {
    if (bytes == null) {
      return null;
    } else {
      StringBuilder sb = new StringBuilder();

      for (byte b : bytes) {
        // remove '=' and '"' because they confuse parsing of key=value log messages
        if (b >= MINIMUM_PRINTABLE_CHARACTER && b < MAXIMUM_PRINTABLE_CHARACTER &&
            b != BACKSLASH_CHARACTER && b != EQUALS_CHARACTER && b != DOUBLE_QUOTE_CHARACTER) {
          sb.append((char) b);
        } else if (b == BACKSLASH_CHARACTER) {
          sb.append("\\\\");
        } else {
          sb.append(String.format("\\x%02x", b));
        }
      }

      return sb.toString();
    }
  }

  public static byte[] unprint(@Nullable String loggedBytes) {
    if (loggedBytes == null) {
      return null;
    }
    List<Byte> bytes = new ArrayList<>();
    for (int i = 0; i < loggedBytes.length(); i++) {
      char c = loggedBytes.charAt(i);
      if (c == '\\') {
        i++;
        c = loggedBytes.charAt(i);
        if (c == '\\') {
          bytes.add((byte) '\\');
        } else if (c == 'x') {
          i++;
          bytes.add((byte) Integer.parseInt(loggedBytes.substring(i, i + 2), 16));
          i++;
        } else {
          throw new IllegalArgumentException("unexpected char at " + i);
        }
      } else {
        bytes.add((byte) c);
      }
    }
    byte[] bytesArray = new byte[bytes.size()];
    for (int i = 0; i < bytes.size(); i++) {
      bytesArray[i] = bytes.get(i);
    }
    return bytesArray;
  }

  public static boolean hasCommonPrefix(@Nonnull byte[] bytes1, @Nonnull byte[] bytes2, int prefixSize) {
    if (bytes1.length < prefixSize || bytes2.length < prefixSize) {
      return false;
    }
    for (int i = 0; i < prefixSize; i++) {
      if (bytes1[i] != bytes2[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Joins a set of byte arrays into a larger array. The {@code interlude} is placed between each of
   * the elements, but not at the beginning or end. In the case that the list is empty or {@code
   * null}, a zero-length byte array will be returned.
   *
   * @param interlude can be {@code null} or zero length. Placed internally between concatenated
   *                  elements.
   * @param parts     the pieces to be joined. May be {@code null}, but does not allow for elements
   *                  in the list to be {@code null}.
   * @return a newly created concatenation of the input
   */
  public static byte[] join(byte[] interlude, List<byte[]> parts) {
    if (parts == null)
      return new byte[0];
    int partCount = parts.size();
    if (partCount == 0)
      return new byte[0];

    if (interlude == null)
      interlude = new byte[0];

    int elementTotals = 0;
    int interludeSize = interlude.length;
    for (byte[] e : parts) {
      elementTotals += e.length;
    }

    byte[] dest = new byte[(interludeSize * (partCount - 1)) + elementTotals];

    //System.out.println(" interlude -> " + ArrayUtils.printable(interlude));

    int startByte = 0;
    int index = 0;
    for (byte[] part : parts) {
      //System.out.println(" section -> " + ArrayUtils.printable(parts.get(i)));
      int length = part.length;
      if (length > 0) {
        System.arraycopy(part, 0, dest, startByte, length);
        startByte += length;
      }
      if (index < partCount - 1 && interludeSize > 0) {
        // If this is not the last element, append the interlude
        System.arraycopy(interlude, 0, dest, startByte, interludeSize);
        startByte += interludeSize;
      }
      index++;
    }

    //System.out.println(" complete -> " + ArrayUtils.printable(dest));
    return dest;
  }

  /**
   * Joins a variable number of byte arrays into one larger array.
   *
   * @param parts the elements to join. {@code null} elements are not allowed.
   * @return a newly created concatenation of the input
   */
  public static byte[] join(byte[]... parts) {
    return join(null, Arrays.asList(parts));
  }

  /**
   * Tests for the presence of a specific sequence of bytes in a larger array at a specific
   * location.<br/> If {@code src} is {@code null} there is a case for a match. First, if {@code
   * start} is non-zero, an {@code IllegalArgumentException} will be thrown. If {@code start} is
   * {@code 0}, will evaluate to {@code true} if {@code pattern} is {@code null}; {@code false}
   * otherwise.<br/> In all other cases, a {@code null} pattern will never match.
   *
   * @param src     the sequence of bytes in which to search for {@code pattern}
   * @param start   the index at which to look for a match. The length of {@code pattern} added to
   *                this index must not pass the end of {@code src.}
   * @param pattern the series of {@code byte}s to match. If {@code null}, will only match a {@code
   *                null} {@code src} at position {@code 0}.
   * @return {@code true} if {@code pattern} is found in {@code src} at {@code start}.
   */
  public static boolean regionEquals(byte[] src, int start, byte[] pattern) {
    if (src == null) {
      if (start == 0) {
        return pattern == null;
      }
      throw new IllegalArgumentException("start index after end of src");
    }
    if (pattern == null)
      return false;

    // At this point neither src or pattern are null...

    if (start >= src.length)
      throw new IllegalArgumentException("start index after end of src");

    if (src.length < start + pattern.length)
      return false;

    for (int i = 0; i < pattern.length; i++)
      if (pattern[i] != src[start + i])
        return false;

    return true;
  }

  /**
   * Replaces occurrences of a pattern in a byte array. Does not mutate the contents of the
   * parameter {@code src}.
   *
   * @param src         the source to search for {@code pattern}
   * @param pattern     the pattern for which to search
   * @param replacement the sequence of bytes to replace {@code pattern} with.
   * @return a newly created array where {@code pattern} replaced with {@code replacement}
   */
  public static byte[] replace(byte[] src, byte[] pattern, byte[] replacement) {
    if (src == null) {
      return null;
    }
    if (src.length == 0) {
      return new byte[0];
    }
    return join(replacement, split(src, pattern));
  }

  /**
   * Replaces occurrences of a pattern in a byte array. Does not mutate the contents of the
   * parameter {@code src}.
   *
   * @param src         the source to search for {@code pattern}
   * @param offset      the location in {@code src} at which to start the operation
   * @param length      the number of bytes past {@code offset} to search for {@code pattern}
   * @param pattern     the pattern for which to search
   * @param replacement the sequence of bytes to replace {@code pattern} with.
   * @return a newly created array where {@code pattern} replaced with {@code replacement}
   */
  public static byte[] replace(byte[] src, int offset, int length,
                               byte[] pattern, byte[] replacement) {
    if (src == null) {
      return null;
    }
    if (src.length == 0) {
      return new byte[0];
    }
    return join(replacement, split(src, offset, length, pattern));
  }

  /**
   * Splits a byte array at any identified items in the delimiterset. This is particularly helpful
   * when splitting on a set of whitespace.
   *
   * @param src             the array to split
   * @param delimiterset    the byte array set that contains items to split on
   * @return a list of byte arrays from {@code src} now not containing any of the items
   * in {@code delimiterset}
   */
  public static List<byte[]> splitOnWhitespace(byte[] src, byte[] delimiterset) {
    int src_length = src.length;
    byte[] _delimiters = new byte[delimiterset.length];
    // first go through to see if we identified any of the delimiterset in the array
    int delimiter_length = 0;
    for (byte b : delimiterset) {
      if (findNext(src, b, 0, src_length) != -1) {
        _delimiters[delimiter_length] = b;
        delimiter_length++; // keep a pointer to the length of found delimiters
      }
    }
    byte[] delimiters = new byte[delimiter_length];
    System.arraycopy(_delimiters, 0, delimiters, 0, delimiter_length);

    if(delimiter_length == 1) {
      return split(src, delimiters);
    }

    // there are many delimiters to split on, so we need to split on multiple spaces
    List<byte[]> parts = new LinkedList<>();
    int idx = 0;
    int lastSplitEnd = 0;
    while (idx < src_length) {
      for(byte b : delimiters) {
        byte sb = src[idx];
        if (sb == b) {
          // copy the last region of bytes into "parts", copyOfRange is happy with zero-sized ranges
          byte[] bytes = Arrays.copyOfRange(src, lastSplitEnd, idx);
          if(bytes.length != 0) { // ignore empty copies
            parts.add(bytes);
          }
          lastSplitEnd = idx + 1;
          break;
        }
      }
      idx++;
    }
    if (lastSplitEnd == idx)
      // if the last replacement ended at the end of src, we need a tailing empty entry
      parts.add(new byte[0]);
    else {
      parts.add(Arrays.copyOfRange(src, lastSplitEnd, src_length));
    }
    return parts;
  }
  /**
   * Splits a byte array at each occurrence of a pattern. If the pattern is found at the beginning
   * or end of the array the result will have a leading or trailing zero-length array. The delimiter
   * is not included in the output array. Does not mutate the contents the source array.
   *
   * @param src       the array to split
   * @param delimiter the byte pattern on which to split
   * @return a list of byte arrays from {@code src} now not containing {@code delimiter}
   */
  public static List<byte[]> split(byte[] src, byte[] delimiter) {
    return split(src, 0, src.length, delimiter);
  }

  /**
   * Splits a byte array at each occurrence of a pattern. If the pattern is found at the beginning
   * or end of the array the result will have a leading or trailing zero-length array. The delimiter
   * is not included in the output array. Does not mutate the contents the source array.
   *
   * @param src       the array to split
   * @param offset    the location in the array at which to start the operation
   * @param length    the number of bytes to search, must not extend past the end of {@code src}
   * @param delimiter the byte pattern on which to split
   * @return a list of byte arrays from {@code src} now not containing {@code delimiter}
   */
  public static List<byte[]> split(byte[] src, int offset, int length, byte[] delimiter) {
    List<byte[]> parts = new LinkedList<>();
    int idx = offset;
    int lastSplitEnd = offset;
    while (idx <= (offset + length) - delimiter.length) {
      if (regionEquals(src, idx, delimiter)) {
        // copy the last region of bytes into "parts", copyOfRange is happy with zero-sized ranges
        parts.add(Arrays.copyOfRange(src, lastSplitEnd, idx));
        idx += delimiter.length;
        lastSplitEnd = idx;
      } else {
        idx++;
      }
    }
    if (lastSplitEnd == offset + length)
      // if the last replacement ended at the end of src, we need a tailing empty entry
      parts.add(new byte[0]);
    else {
      parts.add(Arrays.copyOfRange(src, lastSplitEnd, offset + length));
    }
    return parts;
  }

  public static int bisectLeft(BigInteger[] arr, BigInteger i) {
    int n = Arrays.binarySearch(arr, i);
    if (n >= 0)
      return n;
    int ip = (n + 1) * -1;
    return ip;
  }

  /**
   * Compare byte arrays for equality and ordering purposes. Elements in the array are interpreted
   * and compared as unsigned bytes. Neither parameter may be {@code null}.
   *
   * @param l byte array on the left-hand side of the inequality
   * @param r byte array on the right-hand side of the inequality
   * @return return -1, 0, or 1 if {@code l} is less than, equal to, or greater than {@code r}.
   */
  public static int compareUnsigned(byte[] l, byte[] r) {
    for (int idx = 0; idx < l.length && idx < r.length; ++idx) {
      if (l[idx] != r[idx]) {
        return (l[idx] & 0xFF) < (r[idx] & 0xFF) ? -1 : 1;
      }
    }
    if (l.length == r.length)
      return 0;
    return l.length < r.length ? -1 : 1;
  }

  /**
   * Check if a byte array starts with another byte array.
   *
   * @param array  the source byte array
   * @param prefix the byte array that we are checking if {@code src} starts with.
   * @return {@code true} if {@code array} starts with {@code prefix}
   */
  public static boolean startsWith(byte[] array, byte[] prefix) {
    return startsWith(array, 0, prefix);

  }

  /**
     * Does this byte array begin with match array content?
     *
     * @param array Byte array to examine
     * @param offset An offset into the <code>source</code> array
     * @param prefix  Byte array to locate in <code>source</code>
     * @return true If the starting bytes are equal
     */
    public static boolean startsWith(byte[] array, int offset, byte[] prefix) {

        if (prefix.length > (array.length - offset)) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if (array[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }


    /**
     * Tests if the bytes ends with the specified suffix.
     *
     * @param bytes
     * @param suffix
     * @return
     */
    public static boolean endsWith(byte[] bytes, byte[] suffix) {
        if (null == suffix || null == bytes || bytes.length < suffix.length) return false;
        int length = bytes.length - suffix.length;
        for (int i = suffix.length - 1; i >= 0; i--) {
            if (bytes[length + i] != suffix[i]) return false;
        }
        return true;
    }

  /**
       * Does this byte array end with suffix array content?
       *
       * @param tv Byte array to examine
       * @param toffset An offset into the <code>tv</code> array
       * @param ov  Byte array to locate in <code>tv</code>
       * @param ooffset An offset into the <code>ov</code> array
       * @param len the number of bytes to compare.
       * @return true If the starting bytes are equal
       */
        public static boolean endsWith(byte[] tv, int toffset, byte[] ov, int ooffset, int len) {
           // Note: toffset, ooffset, or len might be near -1>>>1.
           if ((ooffset < 0) || (toffset < 0) ||
                (toffset > (long)tv.length - len) ||
                (ooffset > (long)ov.length - len)) {
               return false;
           }
             while (len-- > 0) {
                 if (tv[toffset++] != ov[ooffset++]) {
                     return false;
                 }
             }

            return true;
      }

  /**
   * Scan through an array of bytes to find the first occurrence of a specific value.
   *
   * @param src   array to scan. Must not be {@code null}.
   * @param what  the value for which to search.
   * @param start the index at which to start the search. If this is at or after the end of {@code
   *              src}, the result will always be {@code -1}.
   * @param end   the index one past the last entry at which to search
   * @return return the location of the first instance of {@code value}, or {@code -1} if not found.
   */
  public static int findNext(byte[] src, byte what, int start, int end) {
    for (int i = start; i < end; i++) {
      if (src[i] == what)
        return i;
    }
    return -1;
  }

  /**
   * Gets the index of the first element after the next occurrence of the byte sequence [nm]
   *
   * @param v     the bytes to scan through
   * @param n     first character to find
   * @param m     second character to find
   * @param start the index at which to start the scan
   * @return the index after the next occurrence of [nm]
   */
  public static int findTerminator(byte[] v, byte n, byte m, int start) {
    return findTerminator(v, n, m, start, v.length);
  }

  /**
   * Gets the index of the first element after the next occurrence of the byte sequence [nm]
   *
   * @param v     the bytes to scan through
   * @param n     first character to find
   * @param m     second character to find
   * @param start the index at which to start the scan
   * @param end   the index at which to stop the search (exclusive)
   * @return the index after the next occurrence of [nm]
   */
  public static int findTerminator(byte[] v, byte n, byte m, int start, int end) {
    int pos = start;
    while (true) {
      pos = findNext(v, n, pos, end);
      if (pos < 0)
        return end;
      if (pos + 1 == end || v[pos + 1] != m)
        return pos;
      pos += 2;
    }
  }

  /**
   * Computes the first key that would sort outside the range prefixed by {@code key}. {@code key}
   * must be non-null, and contain at least some character this is not {@code \xFF} (255).
   *
   * @param key prefix key
   * @return a newly created byte array
   */
  public static byte[] strinc(byte[] key) {
    byte[] copy = rstrip(key, (byte) 0xff);
    if (copy.length == 0)
      throw new IllegalArgumentException("No key beyond supplied prefix");

    // Since rstrip makes sure the last character is not \xff, we can be sure
    //  we're able to add 1 to it without overflow.
    copy[copy.length - 1] = (byte) (copy[copy.length - 1] + 1);
    return copy;
  }

  /**
   * Get a copy of an array, with all matching characters stripped from trailing edge.
   *
   * @param input  array to copy. Must not be null.
   * @param target byte to exclude from copy.
   * @return returns a copy of {@code input} excluding occurrences of {@code target} at the end.
   */
  public static byte[] rstrip(byte[] input, byte target) {
    int i = input.length - 1;
    for (; i >= 0; i--) {
      if (input[i] != target)
        break;
    }
    return Arrays.copyOfRange(input, 0, i + 1);
  }

  public static int stripLeft(byte[] s, byte[] stripChars, int right) {
    for (int left = 0; left < right; left++) {
      if (Bytes.indexOf(stripChars,s[left]) < 0) {
        return left;
      }
    }
    return right;
  }

  /**
   * Get a copy of an array, with first matching leading bytes contained in {@code target} stripped.
   *
   * @param input  array to copy. Must not be null.
   * @param target any of the bytes to exclude from copy.
   * @return returns a copy of {@code input} excluding first leading matching bytes in {@code target}.
   */
  public static byte[] lstrip(byte[] input, byte[] target) {
     if(!startsWith(input, target)) {
       return Arrays.copyOf(input, input.length);
     }
     // Leftmost non-whitespace character: cannot exceed length
     int left = stripLeft(input, target, input.length);
     return Arrays.copyOfRange(input, left, input.length);
  }

  /**
   * Encode an 64-bit integer (long) into a byte array. Encodes the integer in little endian byte
   * order.
   *
   * @param i the number to encode
   * @return an 8-byte array containing the
   */
  public static byte[] encodeInt(long i) {
    return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(i).array();
  }

  /**
   * Decode a little-endian encoded long integer from an 8-byte array.
   *
   * @param src the non-null, 8-element byte array from which to decode
   * @return a decoded 64-bit integer
   */
  public static long decodeInt(byte[] src) {
    if (src.length != 8) {
      throw new IllegalArgumentException("Source array must be of length 8");
    }
    return ByteBuffer.wrap(src).order(ByteOrder.LITTLE_ENDIAN).getLong();
  }

  /**
   * Gets a human readable version of a byte array. The bytes that correspond with ASCII printable
   * characters [32-127) are passed through. Other bytes are replaced with {@code \x} followed by a
   * two character zero-padded hex code for the byte.
   *
   * @param val the byte array for which to create a human readable form
   * @return a modification of the byte array with unprintable characters replaced.
   */
  public static String printable(byte[] val) {
    if (val == null)
      return null;
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < val.length; i++) {
      byte b = val[i];
      if (b >= 32 && b < 127 && b != '\\') s.append((char) b);
      else if (b == '\\') s.append("\\\\");
      else s.append(String.format("\\x%02x", b));
    }
    return s.toString();
  }

  public static boolean matchesAt(
      byte[] bytes1, int offset1,
      byte[] bytes2) {
    if (offset1 < 0 || offset1 + bytes2.length > bytes1.length) {
      return false;
    }
    if (bytes2.length == 1 && bytes1[offset1] == bytes2[0]) {
      return true;
    }
    for (int i = 0; i < bytes2.length; i++) {
      if (bytes1[offset1 + i] != bytes2[i]) {
        return false;
      }
    }
    return true;
  }

  // copied from pycryptodome/lib/Crypto/Util/number.py:424
  // this function makes no sense.. we will never have such a large number?
  // it sounds like it is trying to pack a large number in bigendian with 0 blocksize..
  public static byte[] long_to_bytes(BigInteger length) {
    long[] z = new long[9];
    BigInteger bit64 = BigInteger.valueOf(0xFFFF_FFFF_FFFF_FFFFL);
    // NOTE: Use a fixed number of loop iterations
    int i;
    for (i = 0; length.compareTo(BigInteger.ZERO) > 0; ++i) {
      z[i] = length.and(bit64).longValue();
      length = length.shiftRight(64);
    }
    byte[] bytes = Pack.longToBigEndian(Arrays.copyOf(z, i));
    bytes = new String(bytes).replace("\0", "").getBytes();
    return bytes;
  }

  // copied from pycryptodome/lib/Crypto/Util/asn1.py:163
  public static byte[] definiteForm(int length) {
    if (length > 127) {
      byte[] encoding = long_to_bytes(BigInteger.valueOf(length));
      return Bytes.concat(new byte[]{(byte) (encoding.length + 128)}, encoding);
    }
    return new byte[]{(byte) length};
  }

  /**
   * Returns a copy of the input array stripped of any leading zero bytes.
   */
  public static int[] stripLeadingZeroInts(int val[]) {
      int vlen = val.length;
      int keep;

      // Find first nonzero byte
      for (keep = 0; keep < vlen && val[keep] == 0; keep++)
          ;
      return java.util.Arrays.copyOfRange(val, keep, vlen);
  }

  /**
   * Returns the input array stripped of any leading zero bytes.
   * Since the source is trusted the copying may be skipped.
   */
  public static int[] trustedStripLeadingZeroInts(int val[]) {
      int vlen = val.length;
      int keep;

      // Find first nonzero byte
      for (keep = 0; keep < vlen && val[keep] == 0; keep++)
          ;
      return keep == 0 ? val : java.util.Arrays.copyOfRange(val, keep, vlen);
  }

  /**
   * Returns a copy of the input array stripped of any leading zero bytes.
   */
  public static int[] stripLeadingZeroBytes(byte a[], int off, int len) {
      int indexBound = off + len;
      int keep;

      // Find first nonzero byte
      for (keep = off; keep < indexBound && a[keep] == 0; keep++)
          ;

      // Allocate new array and copy relevant part of input array
      int intLength = ((indexBound - keep) + 3) >>> 2;
      int[] result = new int[intLength];
      int b = indexBound - 1;
      for (int i = intLength-1; i >= 0; i--) {
          result[i] = a[b--] & 0xff;
          int bytesRemaining = b - keep + 1;
          int bytesToTransfer = Math.min(3, bytesRemaining);
          for (int j=8; j <= (bytesToTransfer << 3); j += 8)
              result[i] |= ((a[b--] & 0xff) << j);
      }
      return result;
  }


  private ByteArrayUtil() {
  } //is not instantiable
}