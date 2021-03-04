/*
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

*/
package com.verygood.security.larky.modules.io;

import java.io.DataInput;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;

/**
 * Mostly taken from Apache Arrow and from RE2j's Unicode class.
 *
 * It allows for utilities for dealing with Unicode better than Java does.
 *
 */
public class TextUtil {

  private static final ThreadLocal<CharsetEncoder> ENCODER_FACTORY =
      ThreadLocal.withInitial(() -> StandardCharsets.UTF_8.newEncoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT));

  private static final ThreadLocal<CharsetDecoder> DECODER_FACTORY =
      ThreadLocal.withInitial(() -> StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT));

  private static final byte[] EMPTY_BYTES = new byte[0];

  // The highest legal rune value.
  static final int MAX_RUNE = 0x10FFFF;

  // The highest legal ASCII value.
  static final int MAX_ASCII = 0x7f;

  // The highest legal Latin-1 value.
  static final int MAX_LATIN1 = 0xFF;

  // Minimum and maximum runes involved in folding.
  // Checked during test.
  static final int MIN_FOLD = 0x0041;
  static final int MAX_FOLD = 0x1044f;

  private byte[] bytes;
  private int length;

  public TextUtil() {
    bytes = EMPTY_BYTES;
  }

  /**
   * Construct from a string.
   *
   * @param string initialize from that string
   */
  public TextUtil(String string) {
    set(string);
  }

  /**
   * Construct from another text.
   *
   * @param utf8 initialize from that Text
   */
  @SuppressWarnings("CopyConstructorMissesField") // set() copies the fields
  public TextUtil(TextUtil utf8) {
    set(utf8);
  }

  /**
   * Construct from a byte array.
   *
   * @param utf8 initialize from that byte array
   */
  public TextUtil(byte[] utf8) {
    set(utf8);
  }

  /**
   * Get a copy of the bytes that is exactly the length of the data. See {@link #getBytes()} for
   * faster access to the underlying array.
   *
   * @return a copy of the underlying array
   */
  public byte[] copyBytes() {
    byte[] result = new byte[length];
    System.arraycopy(bytes, 0, result, 0, length);
    return result;
  }

  /**
   * Returns the raw bytes; however, only data up to {@link #getLength()} is valid. Please use
   * {@link #copyBytes()} if you need the returned array to be precisely the length of the data.
   *
   * @return the underlying array
   */
  public byte[] getBytes() {
    return bytes;
  }

  /**
   * Get the number of bytes in the byte array.
   *
   * @return the number of bytes in the byte array
   */
  public int getLength() {
    return length;
  }

  /**
   * Returns the Unicode Scalar Value (32-bit integer value) for the character at
   * <code>position</code>. Note that this method avoids using the converter or doing String
   * instantiation.
   *
   * @param position the index of the char we want to retrieve
   * @return the Unicode scalar value at position or -1 if the position is invalid or points to a
   * trailing byte
   */
  public int charAt(int position) {
    if (position > this.length) {
      return -1; // too long
    }
    if (position < 0) {
      return -1; // duh.
    }

    Buffer wrap = ByteBuffer.wrap(bytes).position(position);
    // This is to allow compilation by JDK9+ with targeting JDK8 byte code
    //noinspection CastCanBeRemovedNarrowingVariableType
    return bytesToCodePoint(((ByteBuffer) wrap).slice());
  }

  public int find(String what) {
    return find(what, 0);
  }

  /**
   * Finds any occurrence of <code>what</code> in the backing buffer, starting as position
   * <code>start</code>. The starting position is measured in bytes and the return value is in
   * terms
   * of byte position in the buffer. The backing buffer is not converted to a string for this
   * operation.
   *
   * @param what  the string to search for
   * @param start where to start from
   * @return byte position of the first occurrence of the search string in the UTF-8 buffer or -1 if
   * not found
   */
  public int find(String what, int start) {
    try {
      ByteBuffer src = ByteBuffer.wrap(this.bytes, 0, this.length);
      ByteBuffer tgt = encode(what);
      byte b = tgt.get();
      src.position(start);

      while (src.hasRemaining()) {
        if (b == src.get()) { // matching first byte
          src.mark(); // save position in loop
          tgt.mark(); // save position in target
          boolean found = true;
          int pos = src.position() - 1;
          while (tgt.hasRemaining()) {
            if (!src.hasRemaining()) { // src expired first
              tgt.reset();
              src.reset();
              found = false;
              break;
            }
            if (!(tgt.get() == src.get())) {
              tgt.reset();
              src.reset();
              found = false;
              break; // no match
            }
          }
          if (found) {
            return pos;
          }
        }
      }
      return -1; // not found
    } catch (CharacterCodingException e) {
      // can't get here
      e.printStackTrace();
      return -1;
    }
  }

  /**
   * Set to contain the contents of a string.
   *
   * @param string the string to initialize from
   */
  public void set(String string) {
    try {
      ByteBuffer bb = encode(string, true);
      bytes = bb.array();
      length = bb.limit();
    } catch (CharacterCodingException e) {
      throw new RuntimeException("Should not have happened ", e);
    }
  }

  /**
   * Set to a utf8 byte array.
   *
   * @param utf8 the byte array to initialize from
   */
  public void set(byte[] utf8) {
    set(utf8, 0, utf8.length);
  }

  /**
   * copy a text.
   *
   * @param other the text to initialize from
   */
  public void set(TextUtil other) {
    set(other.getBytes(), 0, other.getLength());
  }

  /**
   * Set the Text to range of bytes.
   *
   * @param utf8  the data to copy from
   * @param start the first position of the new string
   * @param len   the number of bytes of the new string
   */
  public void set(byte[] utf8, int start, int len) {
    setCapacity(len, false);
    System.arraycopy(utf8, start, bytes, 0, len);
    this.length = len;
  }

  /**
   * Append a range of bytes to the end of the given text.
   *
   * @param utf8  the data to copy from
   * @param start the first position to append from utf8
   * @param len   the number of bytes to append
   */
  public void append(byte[] utf8, int start, int len) {
    setCapacity(length + len, true);
    System.arraycopy(utf8, start, bytes, length, len);
    length += len;
  }

  /**
   * Clear the string to empty.
   *
   * <em>Note</em>: For performance reasons, this call does not clear the underlying byte array
   * that
   * is retrievable via {@link #getBytes()}. In order to free the byte-array memory, call {@link
   * #set(byte[])} with an empty byte array (For example, <code>new byte[0]</code>).
   */
  public void clear() {
    length = 0;
  }

  /**
   * Sets the capacity of this Text object to <em>at least</em> <code>len</code> bytes. If the
   * current buffer is longer, then the capacity and existing content of the buffer are unchanged.
   * If <code>len</code> is larger than the current capacity, the Text object's capacity is
   * increased to match.
   *
   * @param len      the number of bytes we need
   * @param keepData should the old data be kept
   */
  private void setCapacity(int len, boolean keepData) {
    if (bytes == null || bytes.length < len) {
      if (bytes != null && keepData) {
        bytes = Arrays.copyOf(bytes, Math.max(len, length << 1));
      } else {
        bytes = new byte[len];
      }
    }
  }

  @Override
  public String toString() {
    try {
      return decode(bytes, 0, length);
    } catch (CharacterCodingException e) {
      throw new RuntimeException("Should not have happened ", e);
    }
  }

  /**
   * Read a Text object whose length is already known. This allows creating Text from a stream which
   * uses a different serialization format.
   *
   * @param in  the input to initialize from
   * @param len how many bytes to read from in
   * @throws IOException if something bad happens
   */
  public void readWithKnownLength(DataInput in, int len) throws IOException {
    setCapacity(len, false);
    in.readFully(bytes, 0, len);
    length = len;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o == null) {
      return false;
    }
    if (!(o instanceof TextUtil)) {
      return false;
    }

    final TextUtil that = (TextUtil) o;
    if (this.getLength() != that.getLength()) {
      return false;
    }

    // copied from Arrays.equals so we don'thave to copy the byte arrays
    for (int i = 0; i < length; i++) {
      if (bytes[i] != that.bytes[i]) {
        return false;
      }
    }

    return true;
  }

  /**
   * Copied from Arrays.hashCode so we don't have to copy the byte array.
   *
   * @return hashCode
   */
  @Override
  public int hashCode() {
    if (bytes == null) {
      return 0;
    }

    int result = 1;
    for (int i = 0; i < length; i++) {
      result = 31 * result + bytes[i];
    }

    return result;
  }

  // / STATIC UTILITIES FROM HERE DOWN

  // Returns true iff |c| is an ASCII letter or decimal digit.
   static boolean isalnum(int c) {
     return ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
   }

   // If |c| is an ASCII hex digit, returns its value, otherwise -1.
   static int unhex(int c) {
     if ('0' <= c && c <= '9') {
       return c - '0';
     }
     if ('a' <= c && c <= 'f') {
       return c - 'a' + 10;
     }
     if ('A' <= c && c <= 'F') {
       return c - 'A' + 10;
     }
     return -1;
   }

   private static final String METACHARACTERS = "\\.+*?()|[]{}^$";

   // Appends a RE2 literal to |out| for rune |rune|,
   // with regexp metacharacters escaped.
   static void escapeRune(StringBuilder out, int rune) {
     if (TextUtil.isPrint(rune)) {
       if (METACHARACTERS.indexOf((char) rune) >= 0) {
         out.append('\\');
       }
       out.appendCodePoint(rune);
       return;
     }

     switch (rune) {
       case '"':
         out.append("\\\"");
         break;
       case '\\':
         out.append("\\\\");
         break;
       case '\t':
         out.append("\\t");
         break;
       case '\n':
         out.append("\\n");
         break;
       case '\r':
         out.append("\\r");
         break;
       case '\b':
         out.append("\\b");
         break;
       case '\f':
         out.append("\\f");
         break;
       default:
         {
           String s = Integer.toHexString(rune);
           if (rune < 0x100) {
             out.append("\\x");
             if (s.length() == 1) {
               out.append('0');
             }
             out.append(s);
           } else {
             out.append("\\x{").append(s).append('}');
           }
           break;
         }
     }
   }

   // Returns the array of runes in the specified Java UTF-16 string.
   static int[] stringToRunes(String str) {
     int charlen = str.length();
     int runelen = str.codePointCount(0, charlen);
     int[] runes = new int[runelen];
     int r = 0, c = 0;
     while (c < charlen) {
       int rune = str.codePointAt(c);
       runes[r++] = rune;
       c += Character.charCount(rune);
     }
     return runes;
   }

   // Returns the Java UTF-16 string containing the single rune |r|.
   static String runeToString(int r) {
     char c = (char) r;
     return r == c ? String.valueOf(c) : new String(Character.toChars(c));
   }

   // Returns a new copy of the specified subarray.
   @SuppressWarnings("CommentedOutCode")
   static int[] subarray(int[] array, int start, int end) {
     int[] r = new int[end - start];
     //for (int i = start; i < end; ++i) {
     //  r[i - start] = array[i];
     //}
     if (end - start >= 0) {
       System.arraycopy(array, start, r, 0, end - start);
     }
     return r;
   }

   // Returns a new copy of the specified subarray.
   static byte[] subarray(byte[] array, int start, int end) {
     byte[] r = new byte[end - start];
     if (end - start >= 0) {
       System.arraycopy(array, start, r, 0, end - start);
     }
     return r;
   }

   // Returns the index of the first occurrence of array |target| within
   // array |source| after |fromIndex|, or -1 if not found.
   static int indexOf(byte[] source, byte[] target, int fromIndex) {
     if (fromIndex >= source.length) {
       return target.length == 0 ? source.length : -1;
     }
     if (fromIndex < 0) {
       fromIndex = 0;
     }
     if (target.length == 0) {
       return fromIndex;
     }

     byte first = target[0];
     for (int i = fromIndex, max = source.length - target.length; i <= max; i++) {
       // Look for first byte.
       if (source[i] != first) {
         while (true) {
           if (++i > max || source[i] == first) break;
         }
       }

       // Found first byte, now look at the rest of v2.
       if (i <= max) {
         int j = i + 1;
         int end = j + target.length - 1;
         int k = 1;
         while (j < end && source[j] == target[k]) {
           j++;
           k++;
         }

         if (j == end) {
           return i; // found whole array
         }
       }
     }
     return -1;
   }

   // isWordRune reports whether r is consider a ``word character''
   // during the evaluation of the \b and \B zero-width assertions.
   // These assertions are ASCII-only: the word characters are [A-Za-z0-9_].
   static boolean isWordRune(int r) {
     return (('A' <= r && r <= 'Z') || ('a' <= r && r <= 'z') || ('0' <= r && r <= '9') || r == '_');
   }

   //// EMPTY_* flags

   static final int EMPTY_BEGIN_LINE = 0x01;
   static final int EMPTY_END_LINE = 0x02;
   static final int EMPTY_BEGIN_TEXT = 0x04;
   static final int EMPTY_END_TEXT = 0x08;
   static final int EMPTY_WORD_BOUNDARY = 0x10;
   static final int EMPTY_NO_WORD_BOUNDARY = 0x20;
   static final int EMPTY_ALL = -1; // (impossible)

   // emptyOpContext returns the zero-width assertions satisfied at the position
   // between the runes r1 and r2, a bitmask of EMPTY_* flags.
   // Passing r1 == -1 indicates that the position is at the beginning of the
   // text.
   // Passing r2 == -1 indicates that the position is at the end of the text.
   // TODO(adonovan): move to Machine.
   static int emptyOpContext(int r1, int r2) {
     int op = 0;
     if (r1 < 0) {
       op |= EMPTY_BEGIN_TEXT | EMPTY_BEGIN_LINE;
     }
     if (r1 == '\n') {
       op |= EMPTY_BEGIN_LINE;
     }
     if (r2 < 0) {
       op |= EMPTY_END_TEXT | EMPTY_END_LINE;
     }
     if (r2 == '\n') {
       op |= EMPTY_END_LINE;
     }
     if (isWordRune(r1) != isWordRune(r2)) {
       op |= EMPTY_WORD_BOUNDARY;
     } else {
       op |= EMPTY_NO_WORD_BOUNDARY;
     }
     return op;
   }


  //is32 uses binary search to test whether rune is in the specified
  //slice of 32-bit ranges.

  // TODO(adonovan): opt: consider using int[n*3] instead of int[n][3].
  private static boolean is32(int[][] ranges, int r) {
    // binary search over ranges
    for (int lo = 0, hi = ranges.length; lo < hi; ) {
      int m = lo + (hi - lo) / 2;
      int[] range = ranges[m]; // [lo, hi, stride]
      if (range[0] <= r && r <= range[1]) {
        return ((r - range[0]) % range[2]) == 0;
      }
      if (r < range[0]) {
        hi = m;
      } else {
        lo = m + 1;
      }
    }
    return false;
  }

  // is tests whether rune is in the specified table of ranges.
  private static boolean is(int[][] ranges, int r) {
    // common case: rune is ASCII or Latin-1, so use linear search.
    if (r <= MAX_LATIN1) {
      for (int[] range : ranges) { // range = [lo, hi, stride]
        if (r > range[1]) {
          continue;
        }
        if (r < range[0]) {
          return false;
        }
        return ((r - range[0]) % range[2]) == 0;
      }
      return false;
    }
    return ranges.length > 0 && r >= ranges[0][0] && is32(ranges, r);
  }

  // isUpper reports whether the rune is an upper case letter.
  static boolean isUpper(int r) {
    // See comment in isGraphic.
    if (r <= MAX_LATIN1) {
      return Character.isUpperCase((char) r);
    }
    return is(UnicodeTables.Upper, r);
  }

  // isPrint reports whether the rune is printable (Unicode L/M/N/P/S or ' ').
  static boolean isPrint(int r) {
    if (r <= MAX_LATIN1) {
      return (r >= 0x20 && r < 0x7F) || (r >= 0xA1 && r != 0xAD);
    }
    return is(UnicodeTables.L, r)
        || is(UnicodeTables.M, r)
        || is(UnicodeTables.N, r)
        || is(UnicodeTables.P, r)
        || is(UnicodeTables.S, r);
  }

  // simpleFold iterates over Unicode code points equivalent under
  // the Unicode-defined simple case folding.  Among the code points
  // equivalent to rune (including rune itself), SimpleFold returns the
  // smallest r >= rune if one exists, or else the smallest r >= 0.
  //
  // For example:
  //      SimpleFold('A') = 'a'
  //      SimpleFold('a') = 'A'
  //
  //      SimpleFold('K') = 'k'
  //      SimpleFold('k') = '\u212A' (Kelvin symbol, K)
  //      SimpleFold('\u212A') = 'K'
  //
  //      SimpleFold('1') = '1'
  //
  // Derived from Go's unicode.SimpleFold.
  //
  static int simpleFold(int r) {
    // Consult caseOrbit table for special cases.
    if (r < UnicodeTables.CASE_ORBIT.length && UnicodeTables.CASE_ORBIT[r] != 0) {
      return UnicodeTables.CASE_ORBIT[r];
    }

    // No folding specified.  This is a one- or two-element
    // equivalence class containing rune and toLower(rune)
    // and toUpper(rune) if they are different from rune.
    int l = Character.toLowerCase(r);
    if (l != r) {
      return l;
    }
    return Character.toUpperCase(r);
  }

  /**
   * Converts the provided byte array to a String using the UTF-8 encoding. If the input is
   * malformed, replace by a default value.
   *
   * @param utf8 bytes to decode
   * @return the decoded string
   * @throws CharacterCodingException if this is not valid UTF-8
   */
  public static String decode(byte[] utf8) throws CharacterCodingException {
    return decode(ByteBuffer.wrap(utf8), true);
  }

  public static String decode(byte[] utf8, int start, int length)
      throws CharacterCodingException {
    return decode(ByteBuffer.wrap(utf8, start, length), true);
  }

  /**
   * Converts the provided byte array to a String using the UTF-8 encoding. If <code>replace</code>
   * is true, then malformed input is replaced with the substitution character, which is U+FFFD.
   * Otherwise the method throws a MalformedInputException.
   *
   * @param utf8    the bytes to decode
   * @param start   where to start from
   * @param length  length of the bytes to decode
   * @param replace whether to replace malformed characters with U+FFFD
   * @return the decoded string
   * @throws CharacterCodingException if the input could not be decoded
   */
  public static String decode(byte[] utf8, int start, int length, boolean replace)
      throws CharacterCodingException {
    return decode(ByteBuffer.wrap(utf8, start, length), replace);
  }

  private static String decode(ByteBuffer utf8, boolean replace)
      throws CharacterCodingException {
    CharsetDecoder decoder = DECODER_FACTORY.get();
    if (replace) {
      decoder.onMalformedInput(
          java.nio.charset.CodingErrorAction.REPLACE);
      decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
    }
    String str = decoder.decode(utf8).toString();
    // set decoder back to its default value: REPORT
    if (replace) {
      decoder.onMalformedInput(CodingErrorAction.REPORT);
      decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
    }
    return str;
  }

  /**
   * Converts the provided String to bytes using the UTF-8 encoding. If the input is malformed,
   * invalid chars are replaced by a default value.
   *
   * @param string the string to encode
   * @return ByteBuffer: bytes stores at ByteBuffer.array() and length is ByteBuffer.limit()
   * @throws CharacterCodingException if the string could not be encoded
   */
  public static ByteBuffer encode(String string)
      throws CharacterCodingException {
    return encode(string, true);
  }

  /**
   * Converts the provided String to bytes using the UTF-8 encoding. If <code>replace</code> is
   * true, then malformed input is replaced with the substitution character, which is U+FFFD.
   * Otherwise the method throws a MalformedInputException.
   *
   * @param string  the string to encode
   * @param replace whether to replace malformed characters with U+FFFD
   * @return ByteBuffer: bytes stores at ByteBuffer.array() and length is ByteBuffer.limit()
   * @throws CharacterCodingException if the string could not be encoded
   */
  public static ByteBuffer encode(String string, boolean replace)
      throws CharacterCodingException {
    CharsetEncoder encoder = ENCODER_FACTORY.get();
    if (replace) {
      encoder.onMalformedInput(CodingErrorAction.REPLACE);
      encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
    }
    ByteBuffer bytes =
        encoder.encode(CharBuffer.wrap(string.toCharArray()));
    if (replace) {
      encoder.onMalformedInput(CodingErrorAction.REPORT);
      encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
    }
    return bytes;
  }

  public static final int DEFAULT_MAX_LEN = 1024 * 1024;

  // //// states for validateUTF8

  private static final int LEAD_BYTE = 0;

  private static final int TRAIL_BYTE_1 = 1;

  private static final int TRAIL_BYTE = 2;

  /**
   * Check if a byte array contains valid utf-8.
   *
   * @param utf8 byte array
   * @throws MalformedInputException if the byte array contains invalid utf-8
   */
  public static void validateUTF8(byte[] utf8) throws MalformedInputException {
    validateUTF8(utf8, 0, utf8.length);
  }

  /**
   * Check to see if a byte array is valid utf-8.
   *
   * @param utf8  the array of bytes
   * @param start the offset of the first byte in the array
   * @param len   the length of the byte sequence
   * @throws MalformedInputException if the byte array contains invalid bytes
   */
  public static void validateUTF8(byte[] utf8, int start, int len)
      throws MalformedInputException {
    int count = start;
    int leadByte = 0;
    int length = 0;
    int state = LEAD_BYTE;
    while (count < start + len) {
      int aByte = utf8[count] & 0xFF;

      switch (state) {
        case LEAD_BYTE:
          leadByte = aByte;
          length = bytesFromUTF8[aByte];

          switch (length) {
            case 0: // check for ASCII
              if (leadByte > 0x7F) {
                throw new MalformedInputException(count);
              }
              break;
            case 1:
              if (leadByte < 0xC2 || leadByte > 0xDF) {
                throw new MalformedInputException(count);
              }
              state = TRAIL_BYTE_1;
              break;
            case 2:
              if (leadByte < 0xE0 || leadByte > 0xEF) {
                throw new MalformedInputException(count);
              }
              state = TRAIL_BYTE_1;
              break;
            case 3:
              if (leadByte < 0xF0 || leadByte > 0xF4) {
                throw new MalformedInputException(count);
              }
              state = TRAIL_BYTE_1;
              break;
            default:
              // too long! Longest valid UTF-8 is 4 bytes (lead + three)
              // or if < 0 we got a trail byte in the lead byte position
              throw new MalformedInputException(count);
          } // switch (length)
          break;

        case TRAIL_BYTE_1:
          if (leadByte == 0xF0 && aByte < 0x90) {
            throw new MalformedInputException(count);
          }
          if (leadByte == 0xF4 && aByte > 0x8F) {
            throw new MalformedInputException(count);
          }
          if (leadByte == 0xE0 && aByte < 0xA0) {
            throw new MalformedInputException(count);
          }
          if (leadByte == 0xED && aByte > 0x9F) {
            throw new MalformedInputException(count);
          }
          // falls through to regular trail-byte test!!
        case TRAIL_BYTE:
          if (aByte < 0x80 || aByte > 0xBF) {
            throw new MalformedInputException(count);
          }
          if (--length == 0) {
            state = LEAD_BYTE;
          } else {
            state = TRAIL_BYTE;
          }
          break;
        default:
          break;
      } // switch (state)
      count++;
    }
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

  /**
   * Returns the next code point at the current position in the buffer. The buffer's position will
   * be incremented. Any mark set on this buffer will be changed by this method!
   *
   * @param bytes the incoming bytes
   * @return the corresponding unicode codepoint
   */
  public static int bytesToCodePoint(ByteBuffer bytes) {
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

  static final int[] offsetsFromUTF8 =
      {0x00000000, 0x00003080, 0x000E2080, 0x03C82080, 0xFA082080, 0x82082080};

  /**
   * For the given string, returns the number of UTF-8 bytes required to encode the string.
   *
   * @param string text to encode
   * @return number of UTF-8 bytes required to encode
   */
  public static int utf8Length(String string) {
    CharacterIterator iter = new StringCharacterIterator(string);
    char ch = iter.first();
    int size = 0;
    while (ch != CharacterIterator.DONE) {
      if ((ch >= 0xD800) && (ch < 0xDC00)) {
        // surrogate pair?
        char trail = iter.next();
        if ((trail > 0xDBFF) && (trail < 0xE000)) {
          // valid pair
          size += 4;
        } else {
          // invalid pair
          size += 3;
          iter.previous(); // rewind one
        }
      } else if (ch < 0x80) {
        size++;
      } else if (ch < 0x800) {
        size += 2;
      } else {
        // ch < 0x10000, that is, the largest char value
        size += 3;
      }
      ch = iter.next();
    }
    return size;
  }
}
