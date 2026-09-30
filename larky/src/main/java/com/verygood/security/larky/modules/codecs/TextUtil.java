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
package com.verygood.security.larky.modules.codecs;

import com.google.common.base.Utf8;
import com.google.common.collect.Iterators;
import com.google.common.primitives.Bytes;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.EntityArrays;

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

  /**
   * The Unicode replacement character inserted in place of decoding errors.
   */
  public static final char REPLACEMENT_CHAR = '\uFFFD';

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
   * Returns an input source that reads from a UTF-8-encoded byte array. The caller is free to
   * subsequently mutate the array.
   */
  public static String fromASCII(byte[] bytes) {
    CharBuffer cb = StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(bytes));
    char[] utf16 = new char[cb.length()];
    cb.get(utf16);
    return new String(utf16, 0, utf16.length);
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

    Buffer wrap = ((Buffer) ByteBuffer.wrap(bytes)).position(position);
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
      ((Buffer) src).position(start);

      while (src.hasRemaining()) {
        if (b == src.get()) { // matching first byte
          ((Buffer) src).mark(); // save position in loop
          ((Buffer) tgt).mark(); // save position in target
          boolean found = true;
          int pos = src.position() - 1;
          while (tgt.hasRemaining()) {
            if (!src.hasRemaining()) { // src expired first
              ((Buffer) tgt).reset();
              ((Buffer) src).reset();
              found = false;
              break;
            }
            if (!(tgt.get() == src.get())) {
              ((Buffer) tgt).reset();
              ((Buffer) src).reset();
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
      // nosemgrep: java.lang.security.audit.active-debug-code-printstacktrace.active-debug-code-printstacktrace
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

  public static byte[] chunk(int stream, String payload) {
    byte[] payloadBytes = payload.getBytes(Charset.defaultCharset());
    byte[] result = new byte[payloadBytes.length + 5];

    System.arraycopy(payloadBytes, 0, result, 5, payloadBytes.length);
    result[0] = (byte) stream;
    result[1] = (byte) (payloadBytes.length >> 24);
    result[2] = (byte) ((payloadBytes.length >> 16) & 0xff);
    result[3] = (byte) ((payloadBytes.length >> 8) & 0xff);
    result[4] = (byte) (payloadBytes.length & 0xff);
    return result;
  }

  public static byte[] concatByteArray(byte[]... chunks) {
    int length = 0;
    for (byte[] chunk : chunks) {
      length += chunk.length;
    }

    byte[] result = new byte[length];
    int previousChunks = 0;
    for (byte[] chunk : chunks) {
      System.arraycopy(chunk, 0, result, previousChunks, chunk.length);
      previousChunks += chunk.length;
    }
    return result;
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
      // Fast check for Latin-1
      return (r >= 0x20 && r < 0x7F) // All the ASCII is printable from space through DEL-1
          || (r >= 0xA1 && r != 0xAD); // Similarly for ¡ through ÿ...except for bizarre soft hyphen
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
    ((Buffer) bytes).reset();
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


  /**
   * Python codec lookup, encoding and decoding on top of Java charsets.
   *
   * <p>Encoding names follow CPython's {@code codecs.lookup}: they are lower-cased and normalized
   * as {@code encodings.normalize_encoding} does, then resolved through CPython's alias table
   * ({@code encodings.aliases}) for the codecs listed below. Names Java knows but CPython does
   * not are still accepted, for compatibility with scripts written against earlier Larky
   * versions. Anything else is {@code unknown encoding: <name>}, as in CPython's LookupError.
   *
   * <p>Error handlers: strict, ignore, replace, backslashreplace and surrogateescape (both
   * directions), xmlcharrefreplace and namereplace (encoding only). As in CPython, an unknown
   * handler name is only reported when an error has to be handled.
   */
  public static final class PyCodecs {
    private PyCodecs() {}

    /** A resolved codec. */
    public static final class Codec {
      /** CPython's normalized codec name, e.g. {@code utf_8} or {@code latin_1}. */
      public final String name;
      /** The codec name CPython puts in Unicode{En,De}codeError messages. */
      final String errorName;
      final Charset charset;

      Codec(String name, String errorName, Charset charset) {
        this.name = name;
        this.errorName = errorName;
        this.charset = charset;
      }
    }

    private static final Map<String, Codec> CODECS = new HashMap<>();

    private static void codec(String name, String javaName, String errorName, String... aliases) {
      Charset charset;
      try {
        charset = Charset.forName(javaName);
      } catch (IllegalArgumentException e) {
        return; // not provided by this JVM: the name stays unknown
      }
      Codec c = new Codec(name, errorName, charset);
      CODECS.put(name, c);
      for (String alias : aliases) {
        CODECS.put(alias, c);
      }
    }

    static {
      // Generated from CPython 3's encodings.aliases.
      codec("ascii", "US-ASCII", "ascii", "646", "ansi_x3.4_1968", "ansi_x3.4_1986", "ansi_x3_4_1968", "cp367", "csascii", "ibm367", "iso646_us", "iso_646.irv_1991", "iso_ir_6", "us", "us_ascii");
      codec("latin_1", "ISO-8859-1", "latin-1", "8859", "cp819", "csisolatin1", "ibm819", "iso8859", "iso8859_1", "iso_8859_1", "iso_8859_1_1987", "iso_ir_100", "l1", "latin", "latin1");
      codec("utf_8", "UTF-8", "utf-8", "cp65001", "u8", "utf", "utf8", "utf8_ucs2", "utf8_ucs4");
      // CPython's utf-16/utf-32 write a BOM and native (little-endian) order, and read either BOM.
      codec("utf_16", "x-UTF-16LE-BOM", "utf-16", "u16", "utf16");
      codec("utf_16_le", "UTF-16LE", "utf-16-le", "unicodelittleunmarked", "utf_16le");
      codec("utf_16_be", "UTF-16BE", "utf-16-be", "unicodebigunmarked", "utf_16be");
      codec("utf_32", "X-UTF-32LE-BOM", "utf-32", "u32", "utf32");
      codec("utf_32_le", "UTF-32LE", "utf-32-le", "utf_32le");
      codec("utf_32_be", "UTF-32BE", "utf-32-be", "utf_32be");
      codec("cp1250", "windows-1250", "charmap", "1250", "windows_1250");
      codec("cp1251", "windows-1251", "charmap", "1251", "windows_1251");
      codec("cp1252", "windows-1252", "charmap", "1252", "windows_1252");
      codec("cp1253", "windows-1253", "charmap", "1253", "windows_1253");
      codec("cp1254", "windows-1254", "charmap", "1254", "windows_1254");
      codec("cp1255", "windows-1255", "charmap", "1255", "windows_1255");
      codec("cp1256", "windows-1256", "charmap", "1256", "windows_1256");
      codec("cp1257", "windows-1257", "charmap", "1257", "windows_1257");
      codec("cp1258", "windows-1258", "charmap", "1258", "windows_1258");
      codec("iso8859_2", "ISO-8859-2", "charmap", "csisolatin2", "iso_8859_2", "iso_8859_2_1987", "iso_ir_101", "l2", "latin2");
      codec("iso8859_3", "ISO-8859-3", "charmap", "csisolatin3", "iso_8859_3", "iso_8859_3_1988", "iso_ir_109", "l3", "latin3");
      codec("iso8859_4", "ISO-8859-4", "charmap", "csisolatin4", "iso_8859_4", "iso_8859_4_1988", "iso_ir_110", "l4", "latin4");
      codec("iso8859_5", "ISO-8859-5", "charmap", "csisolatincyrillic", "cyrillic", "iso_8859_5", "iso_8859_5_1988", "iso_ir_144");
      codec("iso8859_6", "ISO-8859-6", "charmap", "arabic", "asmo_708", "csisolatinarabic", "ecma_114", "iso_8859_6", "iso_8859_6_1987", "iso_ir_127");
      codec("iso8859_7", "ISO-8859-7", "charmap", "csisolatingreek", "ecma_118", "elot_928", "greek", "greek8", "iso_8859_7", "iso_8859_7_1987", "iso_ir_126");
      codec("iso8859_8", "ISO-8859-8", "charmap", "csisolatinhebrew", "hebrew", "iso_8859_8", "iso_8859_8_1988", "iso_ir_138");
      codec("iso8859_9", "ISO-8859-9", "charmap", "csisolatin5", "iso_8859_9", "iso_8859_9_1989", "iso_ir_148", "l5", "latin5");
      codec("iso8859_13", "ISO-8859-13", "charmap", "iso_8859_13", "l7", "latin7");
      codec("iso8859_15", "ISO-8859-15", "charmap", "iso_8859_15", "l9", "latin9");
      codec("iso8859_16", "ISO-8859-16", "charmap", "iso_8859_16", "iso_8859_16_2001", "iso_ir_226", "l10", "latin10");
      codec("cp037", "IBM037", "charmap", "037", "csibm037", "ebcdic_cp_ca", "ebcdic_cp_nl", "ebcdic_cp_us", "ebcdic_cp_wt", "ibm037", "ibm039");
      codec("cp437", "IBM437", "charmap", "437", "cspc8codepage437", "ibm437");
      codec("cp500", "IBM500", "charmap", "500", "csibm500", "ebcdic_cp_be", "ebcdic_cp_ch", "ibm500");
      codec("cp737", "x-IBM737", "charmap");
      codec("cp775", "IBM775", "charmap", "775", "cspc775baltic", "ibm775");
      codec("cp850", "IBM850", "charmap", "850", "cspc850multilingual", "ibm850");
      codec("cp852", "IBM852", "charmap", "852", "cspcp852", "ibm852");
      codec("cp855", "IBM855", "charmap", "855", "csibm855", "ibm855");
      codec("cp857", "IBM857", "charmap", "857", "csibm857", "ibm857");
      codec("cp858", "IBM00858", "charmap", "858", "csibm858", "ibm858");
      codec("cp862", "IBM862", "charmap", "862", "cspc862latinhebrew", "ibm862");
      codec("cp866", "IBM866", "charmap", "866", "csibm866", "ibm866");
      codec("cp874", "x-windows-874", "charmap");
      codec("cp1026", "IBM1026", "charmap", "1026", "csibm1026", "ibm1026");
      codec("cp1140", "IBM01140", "charmap", "1140", "ibm1140");
      codec("koi8_r", "KOI8-R", "charmap", "cskoi8r");
      codec("koi8_u", "KOI8-U", "charmap");
      codec("mac_roman", "x-MacRoman", "charmap", "macintosh", "macroman");
      codec("shift_jis", "Shift_JIS", "shift_jis", "csshiftjis", "s_jis", "shiftjis", "sjis", "x_mac_japanese");
      codec("cp932", "windows-31j", "cp932", "932", "ms932", "ms_kanji", "mskanji");
      codec("euc_jp", "EUC-JP", "euc_jp", "eucjp", "u_jis", "ujis");
      codec("euc_kr", "EUC-KR", "euc_kr", "euckr", "korean", "ks_c_5601", "ks_c_5601_1987", "ks_x_1001", "ksc5601", "ksx1001", "x_mac_korean");
      codec("cp949", "x-windows-949", "cp949", "949", "ms949", "uhc");
      codec("gb2312", "GB2312", "gb2312", "chinese", "csiso58gb231280", "euc_cn", "euccn", "eucgb2312_cn", "gb2312_1980", "gb2312_80", "iso_ir_58", "x_mac_simp_chinese");
      codec("gbk", "GBK", "gbk", "936", "cp936", "ms936");
      codec("gb18030", "GB18030", "gb18030", "gb18030_2000");
      codec("big5", "Big5", "big5", "big5_tw", "csbig5", "x_mac_trad_chinese");
      codec("cp950", "x-windows-950", "cp950", "950", "ms950");
      codec("iso2022_jp", "ISO-2022-JP", "iso2022_jp", "csiso2022jp", "iso2022jp", "iso_2022_jp");
      codec("iso2022_kr", "ISO-2022-KR", "iso2022_kr", "csiso2022kr", "iso2022kr", "iso_2022_kr");
      codec("tis_620", "TIS-620", "charmap", "iso_ir_166", "tis620", "tis_620_0", "tis_620_2529_0", "tis_620_2529_1");
    }

    /** CPython's {@code encodings.normalize_encoding} applied to the lower-cased name. */
    static String normalize(String encoding) {
      StringBuilder sb = new StringBuilder(encoding.length());
      boolean punct = false;
      for (int i = 0; i < encoding.length(); i++) {
        char c = Character.toLowerCase(encoding.charAt(i));
        if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.') {
          if (punct && sb.length() > 0) {
            sb.append('_');
          }
          sb.append(c);
          punct = false;
        } else if (!Character.isLetterOrDigit(c)) {
          punct = true;
        }
        // non-ASCII letters and digits are dropped, as in CPython
      }
      return sb.toString();
    }

    public static Codec lookup(String encoding) throws EvalException {
      String n = normalize(encoding);
      Codec c = CODECS.get(n);
      if (c == null) {
        c = CODECS.get(n.replace('.', '_'));
      }
      if (c != null) {
        return c;
      }
      // Compatibility: a Java charset name Larky accepted before CPython names were supported.
      try {
        if (Charset.isSupported(encoding)) {
          Charset cs = Charset.forName(encoding);
          return new Codec(cs.name().toLowerCase(Locale.ROOT), cs.name().toLowerCase(Locale.ROOT), cs);
        }
      } catch (IllegalArgumentException e) {
        // illegal charset name: unknown
      }
      throw Starlark.errorf("unknown encoding: %s", encoding);
    }

    private static EvalException unknownHandler(String errors) {
      return Starlark.errorf("unknown error handler name '%s'", errors);
    }

    private static String decodeError(
        String codec, byte[] data, int start, int end, String reason) {
      if (end - start == 1) {
        return String.format(
            "'%s' codec can't decode byte 0x%02x in position %d: %s",
            codec, data[start] & 0xFF, start, reason);
      }
      return String.format(
          "'%s' codec can't decode bytes in position %d-%d: %s", codec, start, end - 1, reason);
    }

    private static String encodeError(String codec, String s, int start, int end, String reason) {
      if (Character.codePointCount(s, start, end) == 1) {
        return String.format(
            "'%s' codec can't encode character '%s' in position %d: %s",
            codec, pyEscape(s.codePointAt(start)), start, reason);
      }
      return String.format(
          "'%s' codec can't encode characters in position %d-%d: %s",
          codec, start, end - 1, reason);
    }

    /** The escape CPython's repr uses for a non-printable code point. */
    private static String pyEscape(int cp) {
      if (cp < 0x100) {
        return String.format("\\x%02x", cp);
      } else if (cp < 0x10000) {
        return String.format("\\u%04x", cp);
      }
      return String.format("\\U%08x", cp);
    }

    /**
     * Handles a decoding error over {@code data[start:end]} by appending the replacement to
     * {@code out}; throws for strict (and unknown) handlers.
     */
    private static void handleDecodeError(
        String errors, String codec, byte[] data, int start, int end, String reason,
        StringBuilder out) throws EvalException {
      switch (errors) {
        case CodecHelper.IGNORE:
          return;
        case CodecHelper.REPLACE:
          out.append(REPLACEMENT_CHAR);
          return;
        case CodecHelper.BACKSLASHREPLACE:
          for (int i = start; i < end; i++) {
            out.append(String.format("\\x%02x", data[i] & 0xFF));
          }
          return;
        case CodecHelper.SURROGATEESCAPE:
          for (int i = start; i < end; i++) {
            if ((data[i] & 0xFF) < 0x80) {
              throw Starlark.errorf("%s", decodeError(codec, data, start, end, reason));
            }
          }
          for (int i = start; i < end; i++) {
            out.append((char) (0xDC00 + (data[i] & 0xFF)));
          }
          return;
        case CodecHelper.STRICT:
        case CodecHelper.XMLCHARREFREPLACE:  // CPython: only valid for encoding
        case CodecHelper.NAMEREPLACE:
        case CodecHelper.SURROGATEPASS:
          throw Starlark.errorf("%s", decodeError(codec, data, start, end, reason));
        default:
          throw unknownHandler(errors);
      }
    }

    /** Result of {@link #utf8Decode}: the text and the number of bytes consumed. */
    public static final class Decoded {
      public final String text;
      public final int consumed;

      Decoded(String text, int consumed) {
        this.text = text;
        this.consumed = consumed;
      }
    }

    /**
     * Decodes UTF-8 exactly as CPython does, including which byte ranges are reported (and
     * replaced) as errors. If {@code last} is false, an incomplete sequence at the end of the
     * input is left unconsumed instead of being an error.
     */
    public static Decoded utf8Decode(byte[] data, String errors, boolean last)
        throws EvalException {
      StringBuilder out = new StringBuilder(data.length);
      int n = data.length;
      int i = 0;
      while (i < n) {
        int c = data[i] & 0xFF;
        if (c < 0x80) {
          out.append((char) c);
          i++;
          continue;
        }
        int need;
        int lo = 0x80;
        int hi = 0xBF;
        int cp;
        if (c >= 0xC2 && c <= 0xDF) {
          need = 1;
          cp = c & 0x1F;
        } else if (c >= 0xE0 && c <= 0xEF) {
          need = 2;
          cp = c & 0x0F;
          if (c == 0xE0) {
            lo = 0xA0;
          } else if (c == 0xED) {
            hi = 0x9F; // no surrogates
          }
        } else if (c >= 0xF0 && c <= 0xF4) {
          need = 3;
          cp = c & 0x07;
          if (c == 0xF0) {
            lo = 0x90;
          } else if (c == 0xF4) {
            hi = 0x8F; // <= U+10FFFF
          }
        } else {
          handleDecodeError(errors, "utf-8", data, i, i + 1, "invalid start byte", out);
          i++;
          continue;
        }
        int j = 1;
        int errEnd = -1;
        String reason = null;
        for (; j <= need; j++) {
          if (i + j >= n) {
            if (!last) {
              return new Decoded(out.toString(), i);
            }
            errEnd = n;
            reason = "unexpected end of data";
            break;
          }
          int cc = data[i + j] & 0xFF;
          if (cc < (j == 1 ? lo : 0x80) || cc > (j == 1 ? hi : 0xBF)) {
            errEnd = i + j;
            reason = "invalid continuation byte";
            break;
          }
          cp = (cp << 6) | (cc & 0x3F);
        }
        if (reason != null) {
          handleDecodeError(errors, "utf-8", data, i, errEnd, reason, out);
          i = errEnd;
        } else {
          out.appendCodePoint(cp);
          i += need + 1;
        }
      }
      return new Decoded(out.toString(), n);
    }

    public static String decode(byte[] data, String encoding, String errors)
        throws EvalException {
      Codec codec = lookup(encoding);
      if (codec.name.equals("utf_8")) {
        return utf8Decode(data, errors, true).text;
      }
      CharsetDecoder decoder = codec.charset.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT);
      ByteBuffer in = ByteBuffer.wrap(data);
      CharBuffer chunk = CharBuffer.allocate(Math.max(16, Math.min(data.length * 2, 8192)));
      StringBuilder out = new StringBuilder(data.length);
      boolean flushing = false;
      while (true) {
        CoderResult r = flushing ? decoder.flush(chunk) : decoder.decode(in, chunk, true);
        chunk.flip();
        out.append(chunk);
        chunk.clear();
        if (r.isOverflow()) {
          continue;
        }
        if (r.isUnderflow()) {
          if (flushing) {
            break;
          }
          flushing = true;
          continue;
        }
        int start = in.position();
        int end = start + r.length();
        handleDecodeError(errors, codec.errorName, data, start, end,
            decodeReason(codec, r, end == data.length), out);
        in.position(end);
      }
      return out.toString();
    }

    private static String decodeReason(Codec codec, CoderResult r, boolean atEnd) {
      switch (codec.name) {
        case "ascii":
          return "ordinal not in range(128)";
        case "utf_16":
        case "utf_16_le":
        case "utf_16_be":
        case "utf_32":
        case "utf_32_le":
        case "utf_32_be":
          return atEnd ? "truncated data" : "illegal encoding";
        default:
          if (codec.errorName.equals("charmap")) {
            return "character maps to <undefined>";
          }
          return atEnd && r.isMalformed()
              ? "incomplete multibyte sequence"
              : "illegal multibyte sequence";
      }
    }

    private static String encodeReason(Codec codec, String s, int pos) {
      switch (codec.name) {
        case "ascii":
          return "ordinal not in range(128)";
        case "latin_1":
          return "ordinal not in range(256)";
        case "utf_8":
        case "utf_16":
        case "utf_16_le":
        case "utf_16_be":
        case "utf_32":
        case "utf_32_le":
        case "utf_32_be":
          return "surrogates not allowed";
        default:
          if (codec.errorName.equals("charmap")) {
            return "character maps to <undefined>";
          }
          return "illegal multibyte sequence";
      }
    }

    public static byte[] encode(String s, String encoding, String errors) throws EvalException {
      Codec codec = lookup(encoding);
      CharsetEncoder encoder = codec.charset.newEncoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT);
      CharBuffer in = CharBuffer.wrap(s);
      ByteBuffer chunk = ByteBuffer.allocate(Math.max(32, Math.min(s.length() * 4, 8192)));
      ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());
      boolean flushing = false;
      while (true) {
        CoderResult r = flushing ? encoder.flush(chunk) : encoder.encode(in, chunk, true);
        drain(chunk, out);
        if (r.isOverflow()) {
          continue;
        }
        if (r.isUnderflow()) {
          if (flushing) {
            break;
          }
          flushing = true;
          continue;
        }
        int start = in.position();
        int end = start + r.length();
        String replacement = encodeReplacement(errors, codec, s, start, end, out);
        if (replacement != null && !replacement.isEmpty()) {
          // A separate encoder: the main one has already been told the input ended. For the
          // BOM-writing codecs, encode the replacement without a second BOM.
          Charset rcs = codec.name.equals("utf_16") ? StandardCharsets.UTF_16LE
              : codec.name.equals("utf_32") ? Charset.forName("UTF-32LE")
              : codec.charset;
          ByteBuffer rb;
          try {
            rb = rcs.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(replacement));
          } catch (CharacterCodingException e) {
            throw Starlark.errorf("%s", encodeError(codec.errorName, s, start, end,
                encodeReason(codec, s, start)));
          }
          out.write(rb.array(), rb.arrayOffset() + rb.position(), rb.remaining());
        }
        in.position(end);
      }
      return out.toByteArray();
    }

    private static void drain(ByteBuffer chunk, ByteArrayOutputStream out) {
      chunk.flip();
      out.write(chunk.array(), chunk.arrayOffset() + chunk.position(), chunk.remaining());
      chunk.clear();
    }

    /**
     * Returns the text to encode in place of {@code s[start:end]}, or null when the handler has
     * already written raw bytes to {@code out}; throws for strict (and unknown) handlers.
     */
    private static String encodeReplacement(
        String errors, Codec codec, String s, int start, int end, ByteArrayOutputStream out)
        throws EvalException {
      StringBuilder sb = new StringBuilder();
      switch (errors) {
        case CodecHelper.IGNORE:
          return "";
        case CodecHelper.REPLACE:
          for (int i = start; i < end; i = s.offsetByCodePoints(i, 1)) {
            sb.append('?');
          }
          return sb.toString();
        case CodecHelper.BACKSLASHREPLACE:
          for (int i = start; i < end; i = s.offsetByCodePoints(i, 1)) {
            int cp = s.codePointAt(i);
            sb.append(cp < 0x100 ? String.format("\\x%02x", cp) : pyEscape(cp));
          }
          return sb.toString();
        case CodecHelper.XMLCHARREFREPLACE:
          for (int i = start; i < end; i = s.offsetByCodePoints(i, 1)) {
            sb.append("&#").append(s.codePointAt(i)).append(';');
          }
          return sb.toString();
        case CodecHelper.NAMEREPLACE:
          for (int i = start; i < end; i = s.offsetByCodePoints(i, 1)) {
            int cp = s.codePointAt(i);
            String name = Character.getName(cp);
            sb.append(name != null ? "\\N{" + name + "}" : pyEscape(cp));
          }
          return sb.toString();
        case CodecHelper.SURROGATEESCAPE:
          for (int i = start; i < end; i++) {
            char ch = s.charAt(i);
            if (ch < 0xDC80 || ch > 0xDCFF) {
              throw Starlark.errorf("%s", encodeError(codec.errorName, s, start, end,
                  encodeReason(codec, s, start)));
            }
          }
          for (int i = start; i < end; i++) {
            out.write(s.charAt(i) - 0xDC00);
          }
          return null;
        case CodecHelper.STRICT:
        case CodecHelper.SURROGATEPASS:
          // CPython reports the whole run of unencodable characters.
          CharsetEncoder probe = codec.charset.newEncoder();
          while (end < s.length()) {
            int next = s.offsetByCodePoints(end, 1);
            if (probe.canEncode(s.subSequence(end, next))) {
              break;
            }
            end = next;
          }
          throw Starlark.errorf("%s", encodeError(codec.errorName, s, start, end,
              encodeReason(codec, s, start)));
        default:
          throw unknownHandler(errors);
      }
    }
  }

  public static char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

  /**
   * starlark compatible -> utf-k => utf-8 encoding of unpaired surrogates => U+FFFD
   *
   * @return utf-8 encoded string compliant with Starlark spec
   */
  public static String starlarkDecodeUtf8(byte[] bytearr) {
    if(bytearr.length == 0) {
      return "";
    }

    StringBuilder v = new StringBuilder(bytearr.length);

    ListIterator<Byte> it = Bytes.asList(bytearr).listIterator();
    int size = 0;
    StringBuffer surrogatePair;
    do {
      int ch = Byte.toUnsignedInt(it.next());
      if (ch == '"' || ch == '\\') { // always backslashed
        v.append('\\');
        v.append((char) ch);
        size++;
      }
      // characters below 0x80 are represented as themselves in a single byte.
      else if (ch < 0x80) {
        String chStr = Character.toString((char) ch);
        if(EntityArrays.JAVA_CTRL_CHARS_ESCAPE.containsKey(chStr)) {
          v.append(EntityArrays.JAVA_CTRL_CHARS_ESCAPE.get(chStr));
        }
        else if ((ch >= ' ') && (ch <= '~')) {
          v.append(chStr);
        }
        else {
          v.append("\\x");
          v.append(int2hex(ch));
        }
        size++;
      }
      else {
        if (ch <= 0x7FF) {
          // This is a 2 byte sequence with ranges: U+0080 - U+07FF
          byte[] subByteArr = null;
          if((it.previousIndex() + 3) <= bytearr.length) {
            subByteArr = subarray(bytearr,
                /* zero indexed */ it.previousIndex(),
                /* until..(end) */it.previousIndex() + 3);
            size += 2; // current + 2
          }
          //noinspection UnstableApiUsage
          if(subByteArr != null) {
            if(Utf8.isWellFormed(subByteArr)) {
              // advance iterator by same since encoding is well-formed
              Iterators.advance(it, /*numberToAdvance*/2);
              String s = decodeUTF8(subByteArr, subByteArr.length);
              if (!isPrint(s.codePointAt(0))) {
                s = StringEscapeUtils.escapeJava(s).toLowerCase();
              }
              v.append(s);
            }
            else {
              // unpaired surrogate, so iterator should be advanced by 1
              // and we replace by U+FFFD
              // v.append(REPLACEMENT_CHAR);  // unpaired surrogate => U+FFFD?
              size += 1;
              //Iterators.advance(it, /*numberToAdvance*/1);
              v.append("\\x");
              v.append(int2hex(ch));
            }
          }
          else {
            v.append("\\x");
            v.append(int2hex(ch));
            size += 1;
          }
        }
        else {
          if (ch > Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            ch = REPLACEMENT_CHAR;
          }
          if (Character.isHighSurrogate((char) ch)) {
            // surrogate pair?
            int trail = it.next();
            if (Character.isLowSurrogate((char) trail)) {
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
              v.append(int2hex(ch));
              v.append("\\x");
              v.append(int2hex(trail));
              size += 3;
              it.previous(); // rewind one
            }
          }
          else {
            // MIN_SUPPLEMENTARY_CODE_POINT
            // ch < 0x10000, that is, the largest char value
            v.append("\\u");
            for (int s = 12; s >= 0; s -= 4) {
              v.append(HEX_DIGITS[ch >> s & 0xF]);
            }
            size += 3;
          }
        }
      }
    } while (it.hasNext());
    return v.toString();
  }

  public static String int2hex(int ch) {
    return CharSequenceTranslator.hex(ch).toLowerCase();
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
        r = TextUtil.bytesToCodePoint(buf);
        if(r == -1) {
          break;
        }
        lastpos = buf.position();
      }catch(java.nio.BufferUnderflowException e) {
        ((Buffer) buf).position(lastpos);
        for(int i = lastpos; i < l; i++) {
          sb.append("\\x");
          sb.append(Integer.toHexString(Byte.toUnsignedInt(buf.get(i))));
        }
        break;
      }
      if(Character.isLowSurrogate((char) r) || Character.isHighSurrogate((char) r)) {
        sb.append(TextUtil.REPLACEMENT_CHAR);
      }
      else {
        sb.append(TextUtil.runeToString(r));
      }

      //System.out.println(Integer.toHexString(r));
      //System.out.println("Chars: " + Arrays.toString(Character.toChars(r)));
    }
    return sb.toString();
  }
}
