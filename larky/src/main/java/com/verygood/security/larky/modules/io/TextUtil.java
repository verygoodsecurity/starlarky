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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.primitives.Bytes;

import org.apache.commons.text.translate.CharSequenceTranslator;

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
import java.util.Formatter;
import java.util.ListIterator;
import java.util.Map;

/**
 * Mostly taken from Apache Arrow and from RE2j's Unicode class.
 *
 * It allows for utilities for dealing with Unicode better than Java does.
 */
public class TextUtil {

  private final ThreadLocal<CharsetEncoder> ENCODER_FACTORY =
      ThreadLocal.withInitial(() -> StandardCharsets.UTF_8.newEncoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT));

  private final ThreadLocal<CharsetDecoder> DECODER_FACTORY =
      ThreadLocal.withInitial(() -> StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT));
  ;

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

  private byte[] bytes = EMPTY_BYTES;
  private int length;

  /* For static method usage */
  private static final TextUtil INSTANCE = new TextUtil(EMPTY_BYTES);

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
   * terms of byte position in the buffer. The backing buffer is not converted to a string for this
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
   * that is retrievable via {@link #getBytes()}. In order to free the byte-array memory, call
   * {@link #set(byte[])} with an empty byte array (For example, <code>new byte[0]</code>).
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

  public String decode(ByteBuffer utf8, boolean replace)
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
   * Converts the provided String to bytes using the UTF-8 encoding. If <code>replace</code> is
   * true, then malformed input is replaced with the substitution character, which is U+FFFD.
   * Otherwise the method throws a MalformedInputException.
   *
   * @param string  the string to encode
   * @param replace whether to replace malformed characters with U+FFFD
   * @return ByteBuffer: bytes stores at ByteBuffer.array() and length is ByteBuffer.limit()
   * @throws CharacterCodingException if the string could not be encoded
   */
  public ByteBuffer encode(String string, boolean replace)
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

  // / STATIC UTILITIES FROM HERE DOWN


  public static String unescapeJavaString(String oldstr) {

    /*
     * In contrast to fixing Java's broken regex charclasses,
     * this one need be no bigger, as unescaping shrinks the string
     * here, where in the other one, it grows it.
     */

    StringBuffer newstr = new StringBuffer(oldstr.length());

    boolean saw_backslash = false;

    for (int i = 0; i < oldstr.length(); i++) {
      int cp = oldstr.codePointAt(i);
      if (oldstr.codePointAt(i) > Character.MAX_VALUE) {
        i++; /****WE HATES UTF-16! WE HATES IT FOREVERSES!!!****/
      }

      if (!saw_backslash) {
        if (cp == '\\') {
          saw_backslash = true;
        } else {
          newstr.append(Character.toChars(cp));
        }
        continue; /* switch */
      }

      if (cp == '\\') {
        saw_backslash = false;
        newstr.append('\\');
        newstr.append('\\');
        continue; /* switch */
      }

      switch (cp) {

        case 'r':
          newstr.append('\r');
          break; /* switch */

        case 'n':
          newstr.append('\n');
          break; /* switch */

        case 'f':
          newstr.append('\f');
          break; /* switch */

        /* PASS a \b THROUGH!! */
        case 'b':
          newstr.append("\\b");
          break; /* switch */

        case 't':
          newstr.append('\t');
          break; /* switch */

        case 'a':
          newstr.append('\007');
          break; /* switch */

        case 'e':
          newstr.append('\033');
          break; /* switch */

        /*
         * A "control" character is what you get when you xor its
         * codepoint with '@'==64.  This only makes sense for ASCII,
         * and may not yield a "control" character after all.
         *
         * Strange but true: "\c{" is ";", "\c}" is "=", etc.
         */
        case 'c': {
          if (++i == oldstr.length()) {
            throw new IllegalArgumentException("trailing \\c");
          }
          cp = oldstr.codePointAt(i);
          /*
           * don't need to grok surrogates, as next line blows them up
           */
          if (cp > 0x7f) {
            throw new IllegalArgumentException("expected ASCII after \\c");
          }
          newstr.append(Character.toChars(cp ^ 64));
          break; /* switch */
        }

        case '8':
        case '9':
          throw new IllegalArgumentException("illegal octal digit");
          /* NOTREACHED */

          /*
           * may be 0 to 2 octal digits following this one
           * so back up one for fallthrough to next case;
           * unread this digit and fall through to next case.
           */
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
          --i;
          /* FALLTHROUGH */

          /*
           * Can have 0, 1, or 2 octal digits following a 0
           * this permits larger values than octal 377, up to
           * octal 777.
           */
        case '0': {
          if (i + 1 == oldstr.length()) {
            /* found \0 at end of string */
            newstr.append(Character.toChars(0));
            break; /* switch */
          }
          i++;
          int digits = 0;
          int j;
          for (j = 0; j <= 2; j++) {
            if (i + j == oldstr.length()) {
              break; /* for */
            }
            /* safe because will unread surrogate */
            int ch = oldstr.charAt(i + j);
            if (ch < '0' || ch > '7') {
              break; /* for */
            }
            digits++;
          }
          if (digits == 0) {
            --i;
            newstr.append('\0');
            break; /* switch */
          }
          int value = 0;
          try {
            value = Integer.parseInt(
                oldstr.substring(i, i + digits), 8);
          } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("invalid octal value for \\0 escape");
          }
          newstr.append(Character.toChars(value));
          i += digits - 1;
          break; /* switch */
        } /* end case '0' */

        case 'x': {
          if (i + 2 > oldstr.length()) {
            throw new IllegalArgumentException("string too short for \\x escape");
          }
          i++;
          boolean saw_brace = false;
          if (oldstr.charAt(i) == '{') {
            /* ^^^^^^ ok to ignore surrogates here */
            i++;
            saw_brace = true;
          }
          int j;
          for (j = 0; j < 8; j++) {

            if (!saw_brace && j == 2) {
              break;  /* for */
            }

            /*
             * ASCII test also catches surrogates
             */
            int ch = oldstr.charAt(i + j);
            if (ch > 127) {
              throw new IllegalArgumentException("illegal non-ASCII hex digit in \\x escape");
            }

            if (saw_brace && ch == '}') {
              break; /* for */
            }

            if (!((ch >= '0' && ch <= '9')
                ||
                (ch >= 'a' && ch <= 'f')
                ||
                (ch >= 'A' && ch <= 'F')
            )
            ) {
              throw new IllegalArgumentException(String.format(
                  "illegal hex digit #%d '%c' in \\x", ch, ch));
            }

          }
          if (j == 0) {
            throw new IllegalArgumentException("empty braces in \\x{} escape");
          }
          int value = 0;
          try {
            value = Integer.parseInt(oldstr.substring(i, i + j), 16);
          } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("invalid hex value for \\x escape");
          }
          newstr.append(Character.toChars(value));
          if (saw_brace) {
            j++;
          }
          i += j - 1;
          break; /* switch */
        }

        case 'u': {
          i++;
          if (i + 4 > oldstr.length()) {
            throw new IllegalArgumentException("string too short for \\u escape");
          }
          int j;
          for (j = 0; j < 4; j++) {
            /* this also handles the surrogate issue */
            if (oldstr.charAt(i + j) > 127) {
              throw new IllegalArgumentException("illegal non-ASCII hex digit in \\u escape");
            }
          }
          int value = 0;
          try {
            value = Integer.parseInt(oldstr.substring(i, i + j), 16);
          } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("invalid hex value for \\u escape");
          }
          newstr.append(Character.toChars(value));
          i += j - 1;
          break; /* switch */
        }

        case 'U': {
          if (i + 8 > oldstr.length()) {
            throw new IllegalArgumentException("string too short for \\U escape");
          }
          i++;
          int j;
          for (j = 0; j < 8; j++) {
            /* this also handles the surrogate issue */
            if (oldstr.charAt(i + j) > 127) {
              throw new IllegalArgumentException("illegal non-ASCII hex digit in \\U escape");
            }
          }
          int value = 0;
          try {
            value = Integer.parseInt(oldstr.substring(i, i + j), 16);
          } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("invalid hex value for \\U escape");
          }
          newstr.append(Character.toChars(value));
          i += j - 1;
          break; /* switch */
        }

        default:
          newstr.append('\\');
          newstr.append(Character.toChars(cp));
          /*
           * log.info(String.format(
           *       "DEFAULT unrecognized escape %c passed through",
           *       cp));
           */
          break; /* switch */

      }
      saw_backslash = false;
    }

    /* weird to leave one at the end */
    if (saw_backslash) {
      newstr.append('\\');
    }

    return newstr.toString();
  }

  /*
   * Return a string "U+XX.XXX.XXXX" etc, where each XX set is the
   * xdigits of the logical Unicode code point.
   *
   * No bloody brain-damaged UTF-16 surrogate crap, just true logical characters.
   */
  public static String uniplus(String s) {
    if (s.length() == 0) {
      return "";
    }
    /* This is just the minimum; sb will grow as needed. */
    StringBuffer sb = new StringBuffer(2 + 3 * s.length());
    sb.append("U+");
    for (int i = 0; i < s.length(); i++) {
      sb.append(String.format("%X", s.codePointAt(i)));
      if (s.codePointAt(i) > Character.MAX_VALUE) {
        i++; /****WE HATES UTF-16! WE HATES IT FOREVERSES!!!****/
      }
      if (i + 1 < s.length()) {
        sb.append(".");
      }
    }
    return sb.toString();
  }

  /**
   * The Unicode replacement character inserted in place of decoding errors.
   */
  public static final char REPLACEMENT_CHAR = '\uFFFD';

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
   * Determine whether a string consists entirely of characters in the range 0 to 255. Only such
   * characters are allowed in the <code>PyBytes</code> (<code>str</code>) type.
   *
   * @return true if and only if every character has a code less than 256
   */
  public static boolean isBytes(CharSequence s) {
    int k = s.length();
    if (k == 0) {
      return true;
    } else {
      // Bitwise-or the character codes together in order to test once.
      char c = 0;
      // Blocks of 8 to reduce loop tests
      while (k > 8) {
        c |= s.charAt(--k);
        c |= s.charAt(--k);
        c |= s.charAt(--k);
        c |= s.charAt(--k);
        c |= s.charAt(--k);
        c |= s.charAt(--k);
        c |= s.charAt(--k);
        c |= s.charAt(--k);
      }
      // Now the rest
      while (k > 0) {
        c |= s.charAt(--k);
      }
      // We require there to be no bits set from 0x100 upwards
      return c < 0x100;
    }
  }

  // Returns true iff |c| is an ASCII letter or decimal digit.
  public static boolean isalnum(int c) {
    return ('0' <= c && c <= '9') || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
  }

  // If |c| is an ASCII hex digit, returns its value, otherwise -1.
  public static int unhex(int c) {
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
  public static void escapeRegexRune(StringBuilder out, int rune) {
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
      default: {
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
  public static int[] stringToRunes(String str) {
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
  public static String runeToString(int r) {
    char c = (char) r;
    return r == c ? String.valueOf(c) : new String(Character.toChars(c));
  }

  // Returns a new copy of the specified subarray.
  @SuppressWarnings("CommentedOutCode")
  public static int[] subarray(int[] array, int start, int end) {
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
  public static byte[] subarray(byte[] array, int start, int end) {
    byte[] r = new byte[end - start];
    if (end - start >= 0) {
      System.arraycopy(array, start, r, 0, end - start);
    }
    return r;
  }

  // Returns the index of the first occurrence of array |target| within
  // array |source| after |fromIndex|, or -1 if not found.
  public static int indexOf(byte[] source, byte[] target, int fromIndex) {
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
  public static boolean isWordRune(int r) {
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
  public static int emptyOpContext(int r1, int r2) {
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
  public static boolean isUpper(int r) {
    // See comment in isGraphic.
    if (r <= MAX_LATIN1) {
      return Character.isUpperCase((char) r);
    }
    return is(UnicodeTables.Upper, r);
  }

  // isPrint reports whether the rune is printable (Unicode L/M/N/P/S or ' ').
  public static boolean isPrint(int r) {
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
  public static int simpleFold(int r) {
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
    return TextUtil.INSTANCE.decode(ByteBuffer.wrap(utf8), true);
  }

  public static String decode(byte[] utf8, int start, int length)
      throws CharacterCodingException {
    return TextUtil.INSTANCE.decode(ByteBuffer.wrap(utf8, start, length), true);
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
    return TextUtil.INSTANCE.decode(ByteBuffer.wrap(utf8, start, length), replace);
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
    return TextUtil.INSTANCE.encode(string, true);
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

  static final int[] offsetsFromUTF8 =
      {0x00000000, 0x00003080, 0x000E2080, 0x03C82080, 0xFA082080, 0x82082080};

  /**
   * For the given string, returns the number of UTF-8 bytes required to encode the string.
   *
   * @param string text to encode
   * @return number of UTF-8 bytes required to encode
   */
  static public int utf8Length(String string) {
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
  public static class CodecHelper {
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

  public static String PyUnicode_DecodeRawUnicodeEscape(String str, String errors) {
         int size = str.length();
         StringBuilder v = new StringBuilder(size);

         for (int i = 0; i < size;) {
             char ch = str.charAt(i);
             // Non-escape characters are interpreted as Unicode ordinals
             if (ch != '\\') {
                 v.append(ch);
                 i++;
                 continue;
             }

             // \\u-escapes are only interpreted if the number of leading backslashes is
             // odd
             int bs = i;
             while (i < size) {
                 ch = str.charAt(i);
                 if (ch != '\\') {
                     break;
                 }
                 v.append(ch);
                 i++;
             }
             if (((i - bs) & 1) == 0 || i >= size || (ch != 'u' && ch != 'U')) {
                 continue;
             }
             v.setLength(v.length() - 1);
             int count = ch == 'u' ? 4 : 8;
             i++;

             // \\uXXXX with 4 hex digits, \Uxxxxxxxx with 8
             int codePoint = 0, asDigit = -1;
             for (int j = 0; j < count; i++, j++) {
                 if (i == size) {
                     // EOF in a truncated escape
                     asDigit = -1;
                     break;
                 }

                 ch = str.charAt(i);
                 asDigit = Character.digit(ch, 16);
                 if (asDigit == -1) {
                     break;
                 }
                 codePoint = ((codePoint << 4) & ~0xF) + asDigit;
             }
             if (asDigit == -1) {
                 i++; // TODO: bug?
             } else {
                 v.appendCodePoint(codePoint);
             }
         }

         return v.toString();
     }

  private static char[] hexdigit = "0123456789ABCDEF".toCharArray();

  public static byte[] utf8encode(int codepoint) {
        return new String(new int[]{codepoint}, 0, 1).getBytes(StandardCharsets.UTF_8);
    }
  public static int utf8decode(byte[] bytes) {
         return new String(bytes, StandardCharsets.UTF_8).codePointAt(0);
     }

     public static String asHex(byte[] encoded) {
       Formatter formatter = new Formatter();
                   for (byte b : encoded) {
                       formatter.format("%02X ", b);
                   }
//                   int decoded = utf8decode(encoded);
                   return formatter.toString();
     }

  private static final Map<Character, String> NO_REPLACEMENTS =
      ImmutableMap.of();
  private static final Map<Character, String> SIMPLE_REPLACEMENTS =
      ImmutableMap.of(
          '\n', "<newline>",
          '\t', "<tab>",
          '&', "<and>");
  private static final char[] NO_CHARS = new char[0];

//  UnicodeEscaper escaper = new ArrayBasedUnicodeEscaper(SIMPLE_REPLACEMENTS,
//          Character.MIN_VALUE, Character.MAX_CODE_POINT, null) {
//            @Override protected char[] escapeUnsafe(int c) {
//              return NO_CHARS;
//            }
//      };
//  String escaped = escaper.escape(str);
    // The modified flag is used by cPickle.
    public static byte[] convertToByteArray(final int[] pIntArray)
    {
        final byte[] array = new byte[pIntArray.length * 4];
        for (int j = 0; j < pIntArray.length; j++)
        {
            final int c = pIntArray[j];
            array[j * 4] = (byte)((c & 0xFF000000) >> 24);
            array[j * 4 + 1] = (byte)((c & 0xFF0000) >> 16);
            array[j * 4 + 2] = (byte)((c & 0xFF00) >> 8);
            array[j * 4 + 3] = (byte)(c & 0xFF);
        }
        return array;
    }

    public static int[] convertToIntArray(final byte[] pByteArray)
    {
        final int[] array = new int[pByteArray.length / 4];
        for (int i = 0; i < array.length; i++)
            array[i] = (((int)(pByteArray[i * 4]) << 24) & 0xFF000000) |
                    (((int)(pByteArray[i * 4 + 1]) << 16) & 0xFF0000) |
                    (((int)(pByteArray[i * 4 + 2]) << 8) & 0xFF00) |
                    ((int)(pByteArray[i * 4 + 3]) & 0xFF);
        return array;
    }

  /**
   * starlark compatible
   *   -> utf-k => utf-8 encoding of unpaired surrogates => U+FFFD
   * @param bytearr
   * @return utf-8 encoded string compliant with Starlark spec
   */
  public static String starlarkDecodeUtf8(byte[] bytearr) {
      StringBuilder v = new StringBuilder(bytearr.length);
      int[] arrCodePoints = stringToRunes(decodeUTF8(bytearr, bytearr.length));

      ListIterator<Byte> it = Bytes.asList(bytearr).listIterator();
      int size = 0;
      StringBuffer surrogatePair;
      do {
        int ch = Byte.toUnsignedInt(it.next());
        if ((ch >= Character.MIN_HIGH_SURROGATE) &&
              (ch < Character.MIN_LOW_SURROGATE)) {
          // surrogate pair?
          int trail = it.next();
          if ((trail > Character.MAX_HIGH_SURROGATE) &&
              (trail <= Character.MAX_LOW_SURROGATE)) {
            //System.out.println(Character.toChars());
            surrogatePair = new StringBuffer(2);
            surrogatePair.append((char) ch);
            surrogatePair.append((char) trail);
            v.append(surrogatePair);
            // valid pair
            size += 4;
          } else {
            // invalid pair
            v.append("\\x");
            v.append(CharSequenceTranslator.hex(ch));
            v.append("\\x");
            v.append(CharSequenceTranslator.hex(trail));
            size += 3;
            it.previous(); // rewind one
          }
        } else if (ch < 0x80) {
          if (ch >= ' ' && ch <= '~') {
            v.append((char) ch);
          }
          else if(ch < ' ') {
            v.append("\\x");
            v.append(hexdigit[ch & 0xF]);
            v.append(hexdigit[(ch >> 4)]);
          }
          size++;
        } else if (ch <= 0x7FF) {
          // This is a 3 byte sequence with ranges: U+0080 - U+07FF
          try {
            String utf8decoded = TextUtil.decode(
                bytearr,
                /* zero indexed */ it.previousIndex(),
                /* length */3);
            v.append(utf8decoded);
            size += 2; // current + 2
            // advance iterator by same since string was successfully decoded
            Iterators.advance(it, /*numberToAdvance*/2);
          }catch (IndexOutOfBoundsException e) {
            v.append("\\x");
            v.append(CharSequenceTranslator.hex(ch));
            size += 1;
          } catch(CharacterCodingException e) {
            // Could not decode, so fallback to escaping the sequences
            System.err.println(e.toString());
            // TODO, how to handle?
            throw new RuntimeException(e);
          }
        } else {
          if (ch > Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            ch = REPLACEMENT_CHAR;
          }
          // MIN_SUPPLEMENTARY_CODE_POINT
          // ch < 0x10000, that is, the largest char value
          v.append("\\u");
          for (int s = 12; s >= 0; s -= 4) {
            v.append(hexdigit[ch >> s & 0xF]);
          }
          size += 3;
        }
      } while(it.hasNext());
    return v.toString();
    }
}
