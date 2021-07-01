package net.starlark.java.ext;

import com.google.common.base.Ascii;
import com.google.common.primitives.Bytes;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.starlark.java.eval.Starlark;

public interface ByteStringModule extends ByteStringModuleApi {

  // stringlib whitespace =>
  //   - https://github.com/python/cpython/blob/main/Objects/bytesobject.c#L1720
  //   - https://github.com/python/cpython/blob/main/Objects/stringlib/stringdefs.h#L16
  //   - https://github.com/python/cpython/blob/main/Include/cpython/pyctype.h#L27
  //   - https://github.com/python/cpython/blob/main/Python/pyctype.c#L15-L19 + L38
  byte[] PY_CTF_SPACE =("\u0009" + (char)0x0A + "\u000B" + "\u000C" + (char)0x0D + "\u0020")
                         .getBytes(StandardCharsets.US_ASCII);

  byte[] HEXDIGITS = "0123456789abcdef".getBytes();

  /* implement me */
  byte[] getBytes();
  int[] getUnsignedBytes();


  static boolean PY_STRING_ISSPACE(char ch) {
    if (ch > 0x0020) return false;
    long isspace = 1;
    for (byte b : PY_CTF_SPACE) {
      isspace |= (1L << b);
    }
    return ((isspace >> ch) & 1L) != 0;
  }

  /** Reports whether {@code x} is Java null or Starlark None. */
  static boolean isNullOrNoneOrUnbound(Object x) {
    return x == null || x == Starlark.NONE || x == Starlark.UNBOUND;
  }

  default String _hex(byte[] argbuf, byte sep, int bytesPerSep) {
    int arglen = argbuf.length;
    if (arglen == 0) { // early-exit
      return "";
    }

    int absBytesPerSep = Math.abs(bytesPerSep);
    int resultlen = 0;
    if (bytesPerSep != 0) {
      resultlen = (arglen - 1) / absBytesPerSep;
    }

    if (arglen >= Integer.MAX_VALUE / 2 - resultlen) {
      throw new RuntimeException("invalid arglen parameter: " + arglen);
    }

    resultlen += arglen * 2;
    if (absBytesPerSep >= arglen) {
      bytesPerSep = absBytesPerSep = 0;
    }
    byte[] retbuf = new byte[resultlen];
    int i,j;
    for (i=j=0; i < arglen; ++i) {
      assert(j < resultlen);
      int c = argbuf[i] & 0xFF;
      retbuf[j++] = HEXDIGITS[c >>> 4];
      retbuf[j++] = HEXDIGITS[c & 0x0f];
      if ((bytesPerSep != 0) && (i < (arglen - 1))) {
          int anchor;
          anchor = (bytesPerSep > 0) ? (arglen - 1 - i) : (i + 1);
          if (anchor % absBytesPerSep == 0) {
              retbuf[j++] = sep;
          }
      }
    }

    return new String(retbuf, StandardCharsets.US_ASCII);
  }

  default int _count(byte[] bytes, byte[] sub, int start, int end) {
    if (sub == null || bytes == null) {
      throw new IllegalArgumentException("argument should be integer or bytes-like object, not 'null'");
    }
    int length = bytes.length;

    //If the sub string is longer than the value string a match cannot exist
    if (bytes.length < sub.length) {
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
    for (int i = istart; i < ((iend - sub.length) + 1); i++) {
      found_match = true;
      for (int j = 0; j < sub.length; j++) {
        if (bytes[i + j] != sub[j]) {
          found_match = false;
          break;
        }
      }
      if (found_match) {
        count++;
        //skip ahead by the length of the sub_array (-1 to account for i++ in outer loop)
        //this consumes the match from the value array
        i += sub.length - 1;
      }
    }
    return count;
  }

  /**
   * Computes bytes.range(start, end).endsWith(suffix) without allocation
   *
   * @param bytes    Byte array to examine
   * @param start    An offset into the <code>bytes</code> array to start searching from
   * @param end      An offset into the <code>bytes</code> array to end searching from
   * @param suffixes A 2D byte array to search for to locate in <code>bytes</code>
   * @return true If the ending bytes are equal
   */
  default boolean _endsWith(byte[] bytes, int start, int end, byte[][] suffixes) {
    int n;
    long indices = subsequenceIndices(bytes, start, end);
    int nstart = lo(indices);
    int nend = hi(indices);
    for(byte[] suffix : suffixes) {
      n = suffix.length;
      return ((nstart + n) <= nend) && _endsWith(bytes, nend - n, suffix, 0, n);
    }
    return false;
  }

  /**
   * Does this byte array end with suffix array content?
   *
   * @param bytes   Byte array to examine
   * @param toffset An offset into the <code>bytes</code> array
   * @param ov      Byte array to locate in <code>bytes</code>
   * @param ooffset An offset into the <code>ov</code> array
   * @param len     the number of bytes to compare.
   * @return true If the starting bytes are equal
   */
  default boolean _endsWith(byte[] bytes, int toffset, byte[] ov, int ooffset, int len) {
    // Note: toffset, ooffset, or len might be near -1>>>1.
    if ((ooffset < 0) || (toffset < 0) ||
          (toffset > (long) bytes.length - len) ||
          (ooffset > (long) ov.length - len)) {
      return false;
    }
    while (len-- > 0) {
      if (bytes[toffset++] != ov[ooffset++]) {
        return false;
      }
    }
    return true;
  }

  static long pack(int lo, int hi) {
    return (((long) hi) << 32) | (lo & 0xffffffffL);
  }

  static int lo(long x) {
    return (int) x;
  }

  static int hi(long x) {
    return (int) (x >>> 32);
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
    } else {
      return Math.min(index, length);
    }
  }

  // Returns the byte array denoted by byte[start:end], which is never out of bounds.
  // For speed, we don't return byte.range(start, end), to stop allocating a copy.
  // Instead we return the (start, end) indices, packed into the lo/hi arms of a long. @_@
  static long subsequenceIndices(byte[] bytearr, int start, int end) {
    // This function duplicates the logic of Starlark.slice for bytes.
    int n = bytearr.length;
    int istart;
    istart = toIndex(start, n);

    int iend;
    iend = toIndex(end, n);

    if (iend < istart) {
      iend = istart; // => empty result
    }
    return pack(istart, iend); // = str.substring(start, end)
  }

  /**
   * Common implementation for find, rfind, index, rindex.
   *
   * @param forward true if we want to return the first matching index.
   */
  default int _find(boolean forward, byte[] bytes, byte[] sub, int start, int end) {
    long indices = subsequenceIndices(bytes, start, end);
    byte[] subRange = Arrays.copyOfRange(bytes, lo(indices), hi(indices));

    if(subRange.length == 0 && sub.length == 0 && start > end) {
      return -1;
    }

    int subpos = forward ? Bytes.indexOf(subRange, sub) : _lastIndexOf(subRange, sub, 0, subRange.length);
    return subpos < 0
             ? subpos //
             : subpos + lo(indices);
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
  default byte[] _join(byte[] interlude, byte[][] parts) {
    if (parts == null) {
      return new byte[0];
    }
    int partCount = parts.length;
    if (partCount == 0) {
      return new byte[0];
    }

    if (interlude == null) {
      interlude = new byte[0];
    }

    int elementTotals = 0;
    int interludeSize = interlude.length;
    //noinspection ForLoopReplaceableByForEach -- allocation-free looping
    for (int i = 0, partsSize = parts.length; i < partsSize; i++) {
      byte[] e = parts[i];
      elementTotals += e.length;
    }

    byte[] dest = new byte[(interludeSize * (partCount - 1)) + elementTotals];

    int startByte = 0;
    int index = 0;
    //noinspection ForLoopReplaceableByForEach -- allocation-free looping
    for (int i = 0, partsSize = parts.length; i < partsSize; i++) {
      byte[] part = parts[i];
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
    return dest;
  }


  /**
   * Joins a variable number of byte arrays into one larger array.
   *
   * @param parts the elements to join. {@code null} elements are not allowed.
   * @return a newly created concatenation of the input
   */
  default byte[] _join(byte[]... parts) {
    return _join(null, parts);
  }

  default byte[] _replace(byte[] bytes, int len, byte[] sub, byte[] replacementBytes, int count) {
      int i, j, pos, maxcount = count, subLen = sub.length, repLen = replacementBytes.length;
      List<byte[]> list = new ArrayList<>();

      int resultLen = 0;
      i = 0;
      while (maxcount-- > 0) {
          pos = _find(true, bytes, sub, i, len);
          if (pos < 0) {
              break;
          }
          j = pos;
          list.add(Arrays.copyOfRange(bytes, i, j));
          list.add(replacementBytes);
          resultLen += (j - i) + repLen;
          i = j + subLen;
      }

      if (i == 0) {
          return Arrays.copyOfRange(bytes, 0, len);
      }

      list.add(Arrays.copyOfRange(bytes, i, len));
      resultLen += (len - i);

      i = 0;
      byte[] result = new byte[resultLen];
      for (byte[] b : list) {
        System.arraycopy(b, 0, result, i, b.length);
        i += b.length;
      }

      return result;
  }

  /**
   * Splits a byte array at any identified items in the delimiterset. This is particularly helpful
   * when splitting on a set of whitespace.
   *
   * @param src          the array to split
   * @param maxCount     num of splits
   * @return a list of byte arrays from {@code src} now not containing any of the items in {@code
   * delimiterset}
   */
  default List<byte[]> _splitWhitespace(byte[] src, int maxCount) {
    List<byte[]> list = new LinkedList<>();
    int i = 0, j;
    while((maxCount--) > 0) {
      while (i < src.length && PY_STRING_ISSPACE((char) src[i]))
        i++;
      if (i == src.length) break;
      j = i;
      i++;
      while (i < src.length && !PY_STRING_ISSPACE((char) src[i]))
        i++;
      if (j == 0 && i == src.length) {
        /* No whitespace in str_obj, so just use it as list[0] */
        list.add(0, src);
        break;
      }
      list.add(Arrays.copyOfRange(src, j, i));
    }
    if (i < src.length) {
      /* Only occurs when maxcount was reached */
      /* Skip any remaining whitespace and copy to end of string */
      while (i < src.length && PY_STRING_ISSPACE((char) src[i]))
        i++;
      if (i != src.length) {
        list.add(Arrays.copyOfRange(src, i, src.length));
      }
    }
    return list;
  }

  default List<byte[]> _split(byte[] bytes, byte[] sep, int maxsplit) {
      int i, j, pos, maxCount = maxsplit, sepLen = sep.length;
      List<byte[]> list = new ArrayList<>();

      i = 0;
      while ((maxCount--) > 0) {
        pos = _find(true, bytes, sep, i, bytes.length);
          //separator, end - seplen, end);
        if (pos < 0) {
            break;
        }
        j = pos;
        list.add(Arrays.copyOfRange(bytes, i, j));
        i = j + sepLen;
      }
      list.add(Arrays.copyOfRange(bytes, i, bytes.length));
      return list;
  }

  default List<byte[]> _rsplitWhitespace(byte[] src, int maxCount) {
    List<byte[]> list = new ArrayList<>(Math.min(src.length, maxCount));
    int i, j;
    i = src.length - 1;
    while(maxCount-- > 0) {
      while(i >= 0 && PY_STRING_ISSPACE((char) src[i]))
        i--;
      if (i < 0) break;
      j = i; i--;
      while (i >= 0 && !PY_STRING_ISSPACE((char) src[i]))
        i--;
      if (j == src.length - i && i < 0) {
        /* No whitespace in str_obj, so just use it as list[0] */
        list.add(0, src);
        break;
      }
      list.add(Arrays.copyOfRange(src,i+1,j+1));
    }
    if (i >= 0) {
      /* Only occurs when maxcount was reached */
      /* Skip any remaining whitespace and copy to beginning of string */
      while (i >= 0 && PY_STRING_ISSPACE((char) src[i]))
        i--;
      if (i >= 0) {
        list.add(Arrays.copyOfRange(src, 0, i+1));
      }
    }
    Collections.reverse(list);
    return list;
  }

  default List<byte[]> _rsplit(byte[] src, byte[] separator, int maxsplit) {
    if (separator.length == 0) {
      throw new UnsupportedOperationException("empty separator not supported");
    }
    int pos, end, maxcount = maxsplit;
    List<byte[]> result = new ArrayList<>();

    if (separator.length == 1) {
      pos = end = src.length - 1;
      while ((pos >= 0) && (maxcount-- > 0)) {
          for (; pos >= 0; pos--) {
              if (src[pos] == separator[0]) {
                result.add(Arrays.copyOfRange(src, pos + 1, end + 1));
                  end = pos = pos - 1;
                  break;
              }
          }
      }
      if (end >= -1) {
          result.add(Arrays.copyOfRange(src, 0, end + 1));
      }
      Collections.reverse(result);
      return result;
    }
    int seplen = separator.length;
    end = src.length;
    while (true) {
      pos = _lastIndexOf(src, separator, 0, end - 1);
      if (pos < 0 || maxcount-- == 0) {
        result.add(Arrays.copyOfRange(src, 0, end));
        break;
      }
      // if we're going to copy beyond the end of the array, copy to the end
      // and just return
      if(pos + seplen > end) {
        result.add(Arrays.copyOfRange(src, pos, end));
        break;
      }
      result.add(Arrays.copyOfRange(src, pos + seplen, end));
      end = pos;
    }
    Collections.reverse(result);
    return result;
  }

  byte LEFTSTRIP = 0;
  byte RIGHTSTRIP = 1;
  byte BOTHSTRIP = 2;

  default byte[] do_strip(byte[] src, byte striptype)
  {
    int i = 0, len = src.length;
    if (striptype != RIGHTSTRIP) {
        while (i < len && PY_STRING_ISSPACE((char) src[i])) {
            i++;
        }
    }

    int j = len;
    if (striptype != LEFTSTRIP) {
        do {
            j--;
        } while (j >= i && PY_STRING_ISSPACE((char) src[j]));
        j++;
    }

    if (i == 0 && j == len) {
        return src;
    }

    return Arrays.copyOfRange(src, i, i+(j-i));
  }

  /**
   * Searches within the first num bytes of haystack for the first occurrence of the byte type needle
   * and returns the location in the haystack to it.
   *
   * @param haystack - The byte array to search in
   * @param offset - The offset to start the search in
   * @param needle - The single byte to find
   * @param num - The number of the first bytes to search (usually haystack.length)
   * @return -1 if not found or position of needle in the haystack
   */
  static int membyte(byte[] haystack, int offset, byte needle, int num) {
    if (num != 0) {
        int p = 0;

        do {
            if (haystack[offset + p] == needle)
                return p;
            p++;
        } while (--num != 0);
    }
    return -1;
  }

  default byte[] do_xstrip(byte[] src, byte striptype, byte[] sepobj)
  {
    if (sepobj == null) {
      return null;
    }
    int len = src.length;
    int seplen = sepobj.length;
    int i = 0, j;
    if (striptype != RIGHTSTRIP) {
      while (i < len && membyte(sepobj, 0, src[i], seplen) != -1) {
        i++;
      }
    }

    j = len;
    if (striptype != LEFTSTRIP) {
      do {
        j--;
      } while (j >= i && membyte(sepobj, 0, src[j], seplen) != -1);
      j++;
    }

    if (i == 0 && j == len) {
      return src;
    }
    return Arrays.copyOfRange(src, i, i + (j-i));
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
  default boolean regionEquals(byte[] src, int start, byte[] pattern) {
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

    int result = 0;
    // time-constant comparison
    for (int i = 0; i < pattern.length; i++) {
      result |= pattern[i] ^ src[start + i];
    }
    return result == 0;
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
  default int _indexOf(byte[] src, byte what, int start, int end) {
    for (int i = start; i < end; i++) {
      if (src[i] == what)
        return i;
    }
    return -1;
  }

  default int stripLeft(byte[] s, byte[] stripChars, int right) {
    for (int left = 0; left < right; left++) {
      if (_indexOf(stripChars, s[left], 0, s.length) < 0) {
        return left;
      }
    }
    return right;
  }

  default int _lastIndexOf(byte[] bytes, byte[] sub, int start, int end) {
    int subl = sub.length;
    if(subl == 0) {
      return end;
    }

    outer:
    for (int i = end - 1, blen = bytes.length; i >= start; i--) {
      for (int j = 0; j < subl; j++) {
        if (i + j >= blen) {
          continue outer;
        }
        if (bytes[i + j] != sub[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }

  /**
   * Get a copy of an array, with first matching leading bytes contained in {@code target}
   * stripped.
   *
   * @param input  array to copy. Must not be null.
   * @param target any of the bytes to exclude from copy.
   * @return returns a copy of {@code input} excluding first leading matching bytes in {@code
   * target}.
   */
  default byte[] _lstrip(byte[] input, byte[] target) {
    if (!_startsWith(input, 0, target)) {
      return Arrays.copyOf(input, input.length);
    }
    // Leftmost non-whitespace character: cannot exceed length
    int left = stripLeft(input, target, input.length);
    return Arrays.copyOfRange(input, left, input.length);
  }

  /**
   * Does this byte array begin with match array content?
   *
   * @param array  Byte array to examine
   * @param offset An offset into the <code>source</code> array
   * @param prefix Byte array to locate in <code>source</code>
   * @return true If the starting bytes are equal
   */
  default boolean _startsWith(byte[] array, int offset, byte[] prefix) {
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
   * Computes bytes.range(start, end).endsWith(suffix) without allocation
   *
   * @param bytes    Byte array to examine
   * @param start    An offset into the <code>bytes</code> array to start searching from
   * @param end      An offset into the <code>bytes</code> array to end searching from
   * @param prefixes Byte arrays to locate in <code>bytes</code>
   * @return true If the starting bytes are equal
   */
  // Computes bytes.range(start, end).startsWith(prefix) without allocation.
  default boolean _startsWith(byte[] bytes, int start, int end, byte[][] prefixes) {
    int n;
    long indices = subsequenceIndices(bytes, start, end);
    int nstart = lo(indices);
    int nend = hi(indices);
    boolean prefixSets = false;

    //noinspection ForLoopReplaceableByForEach -- indexed loop no allocation
    for (int i = 0, prefixesLength = prefixes.length; i < prefixesLength; i++) {
      byte[] prefix = prefixes[i];
      n = prefix.length;
      prefixSets = ((nstart + n) <= nend) && _startsWith(bytes, nstart, prefix);
    }
    return prefixSets;
  }

  // most of these methods are ported over from
  //https://github.com/python/cpython/blob/main/Objects/bytes_methods.c

  default byte[] _center(byte[] input, int width, byte[] byteToFill) {
    if (byteToFill == null) {
      byteToFill = " ".getBytes();
    } else if (byteToFill.length != 1) {
      throw new IllegalArgumentException(
        "center() argument 2 must be a byte string of length 1, not bytes");
    }

    if (input.length >= width) {
      return input;
    } else {
      int diff = width - input.length;
      int lenfirst = (diff) / 2;
      int lensecond = diff - lenfirst;

      byte[] returnBytes;
      returnBytes = new byte[width];

      for (int i = 0; i < lenfirst; i++) {
        returnBytes[i] = byteToFill[0];
      }
      if (width - lensecond - lenfirst >= 0) {
        System.arraycopy(input, 0, returnBytes, lenfirst, width - lensecond - lenfirst);
      }
      for (int i = (width - lensecond); i < width; i++) {
        returnBytes[i] = byteToFill[0];
      }
      return returnBytes;
    }
  }

  default byte[] _capitalize(byte[] input) {
    byte[] value = new byte[input.length];
    for (int i = 0; i < input.length; i++) {
      byte b = input[i];
      if (b < 127 && b > 32) {
        char c = (char) b;
        if (i == 0) {
          c = Ascii.toUpperCase(c);
        } else {
          c = Ascii.toLowerCase(c);
        }
        value[i] = (byte) c;
      } else {
        value[i] = b;
      }
    }
    return value;
  }

  default byte[] _expandTabs(byte[] bytes, int tabsize, int max) {
    int i = 0, j = 0;
    for (byte p : bytes) {
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
    for (byte p : bytes) {
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
    return q;
  }

  default boolean _isalnum(byte[] input) {
    if (input.length == 0) {
      return false;
    }
    for (byte ch : input) {
      if (!_isalnum(ch)) {
        return false;
      }
    }
    return true;
  }

  default boolean _isalnum(byte ch) {
    return _isalpha(ch) || _isdigit(ch);
  }

  default boolean _isalpha(byte[] input) {
    if (input.length == 0) {
      return false;
    }
    for (byte ch : input) {
      if (!_isalpha(ch)) {
        return false;
      }
    }
    return true;
  }

  default boolean _isalpha(byte ch) {
    return Ascii.isUpperCase((char) ch) || Ascii.isLowerCase((char) ch);
  }

  default boolean _isAscii(byte[] input) {
    final int len = input.length;
    if (len == 0) {
      return true;
    }
    for (byte b : input) {
      if (b < 0) {  // remember, bytes are -127 to 127 and 0x7F is max ascii
        return false;
      }
    }
    return true;
   }

  default boolean _isdigit(byte[] input) {
     if (input.length == 0) {
       return false;
     }
     for (byte ch : input) {
       if (!_isdigit(ch)) {
         return false;
       }
     }
     return true;
   }

  default boolean _isdigit(byte ch) {
     return ch >= '0' && ch <= '9';
   }

   default boolean _islower(byte[] input) {
    if (input.length == 0) {
       return false;
    }
    boolean res = false;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, inputLength = input.length; i < inputLength; i++) {
      char b = (char) input[i];
      if (Ascii.isUpperCase(b)) {
        return false;
      }
      else if (!res && Ascii.isLowerCase(b)) {
        res = true;
      }
    }
    return res;
  }
  default boolean _isspace(byte[] input) {
    int inputLength = input.length;
    /* Shortcut for single character strings */
    if (inputLength == 1 && PY_STRING_ISSPACE((char)input[0])) {
      return true;
    }

    /* Special case for empty strings */
    if (inputLength == 0) {
      return false;
    }

    for (byte b : input) {
      if (!PY_STRING_ISSPACE((char) b)) {
        return false;
      }
    }
    return true;
  }

  default boolean _istitle(byte[] input) {
    int inputLength = input.length;
    /* Special case for empty strings */
    if (inputLength == 0) {
        return false;
    }
    boolean res = false;
    boolean previousIsCased = false;
    for (byte value : input) {
      char b = (char) value;
      if (Ascii.isUpperCase(b)) {
        if (previousIsCased) {
          return false;
        }
        previousIsCased = true;
        res = true;
      } else if (Ascii.isLowerCase(b)) {
        if (!previousIsCased) {
          return false;
        }
        previousIsCased = true;
        res = true;
      } else {
        previousIsCased = false;
      }
    }
    return res;
  }

  default boolean _isupper(byte[] input) {
    if (input.length == 0) {
       return false;
    }
    boolean res = false;

    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, inputLength = input.length; i < inputLength; i++) {
      char b = (char) input[i];
      if (Ascii.isLowerCase(b)) {
        return false;
      }
      else if (!res && Ascii.isUpperCase(b)) {
        res = true;
      }
    }
    return res;
  }

  default byte[] _lower(byte[] input) {
    for (int i = 0; i < input.length; i++) {
      byte b = (byte) Ascii.toLowerCase((char) input[i]);
      input[i] = b;
    }
    return input;
  }

  default byte[] _swapcase(byte[] input) {
    for (int idx = 0; idx < input.length; ++idx) {
      char lc = (char) input[idx];
      if (Ascii.isUpperCase(lc)) {
        input[idx] = (byte) Ascii.toLowerCase(lc);
      } else {
        input[idx] = (byte) Ascii.toUpperCase(lc);
      }
    }
    return input;
  }

  default byte[] _title(byte[] input) {
    boolean capitalizeNext = true;
    for (int idx = 0; idx < input.length; ++idx) {
      byte lc = input[idx];
      if (!_isalpha(lc)) {
        input[idx] = lc;
        capitalizeNext = true;
      } else if (capitalizeNext) {
        input[idx] = (byte) Ascii.toUpperCase((char) lc);
        capitalizeNext = false;
      } else {
        input[idx] = (byte) Ascii.toLowerCase((char) lc);
      }
    }
    return input;
  }
  default byte[] _upper(byte[] input) {
    for (int i = 0; i < input.length; i++) {
      byte b = (byte) Ascii.toUpperCase((char) input[i]);
      input[i] = b;
    }
    return input;
  }

  default byte[] pad(byte[] bytes, int l, int r, byte fillChar) {
    int left = Math.max(l, 0);
    int right = Math.max(r, 0);
    if (left == 0 && right == 0) {
        return bytes;
    }
    byte[] u = new byte[left + bytes.length + right];
    if (left > 0) {
        Arrays.fill(u, 0, left, fillChar);
    }
    for (int i = left, j = 0; i < (left + bytes.length); j++, i++) {
        u[i] = bytes[j];
    }
    if (right > 0) {
        Arrays.fill(u, left + bytes.length, u.length, fillChar);
    }
    return u;
  }

  default boolean[] createDeleteTable(byte[] delete) {
    boolean[] result = new boolean[256];
    for (int i = 0; i < 256; i++) {
        result[i] = false;
    }
    for (byte b : delete) {
        result[b] = true;
    }
    return result;
  }
}

