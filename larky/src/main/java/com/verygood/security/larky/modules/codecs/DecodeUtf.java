package com.verygood.security.larky.modules.codecs;

import static com.verygood.security.larky.modules.codecs.TextUtil.CodecHelper.IGNORE;
import static com.verygood.security.larky.modules.codecs.TextUtil.CodecHelper.REPLACE;

public class DecodeUtf {

  // Constants for the categorization of code units
    private static final byte C_ILL = 0;            //- C0..C1, F5..FF  ILLEGAL octets that should never appear in a UTF-8 sequence
    private static final byte C_CR1 = 1;            //- 80..8F          Continuation range 1
    private static final byte C_CR2 = 2;            //- 90..9F          Continuation range 2
    private static final byte C_CR3 = 3;            //- A0..BF          Continuation range 3
    private static final byte C_L2A = 4;            //- C2..DF          Leading byte range A / 2-byte sequence
    private static final byte C_L3A = 5;            //- E0              Leading byte range A / 3-byte sequence
    private static final byte C_L3B = 6;            //- E1..EC, EE..EF  Leading byte range B / 3-byte sequence
    private static final byte C_L3C = 7;            //- ED              Leading byte range C / 3-byte sequence
    private static final byte C_L4A = 8;            //- F0              Leading byte range A / 4-byte sequence
    private static final byte C_L4B = 9;            //- F1..F3          Leading byte range B / 4-byte sequence
    private static final byte C_L4C = 10;           //- F4              Leading byte range C / 4-byte sequence
//  private static final byte C_ASC = 11;           //- 00..7F          ASCII leading byte range

    // Constants for the states of a DFA
    private static final byte S_ERR = -2;           //- Error state
    private static final byte S_END = -1;           //- End (or Accept) state
    private static final byte S_CS1 = 0x00;         //- Continuation state 1
    private static final byte S_CS2 = 0x10;         //- Continuation state 2
    private static final byte S_CS3 = 0x20;         //- Continuation state 3
    private static final byte S_P3A = 0x30;         //- Partial 3-byte sequence state A
    private static final byte S_P3B = 0x40;         //- Partial 3-byte sequence state B
    private static final byte S_P4A = 0x50;         //- Partial 4-byte sequence state A
    private static final byte S_P4B = 0x60;         //- Partial 4-byte sequence state B

    private static final short[] firstUnitTable = new short[128];
    private static final byte[] transitionTable = new byte[S_P4B + 16];

    private static void fill(byte[] table, int first, int last, byte b)
    {
        for (int i = first; i <= last; ++i)
        {
            table[i] = b;
        }
    }

    static
    {
        byte[] categories = new byte[128];
        fill(categories, 0x00, 0x0F, C_CR1);
        fill(categories, 0x10, 0x1F, C_CR2);
        fill(categories, 0x20, 0x3F, C_CR3);
        fill(categories, 0x40, 0x41, C_ILL);
        fill(categories, 0x42, 0x5F, C_L2A);
        fill(categories, 0x60, 0x60, C_L3A);
        fill(categories, 0x61, 0x6C, C_L3B);
        fill(categories, 0x6D, 0x6D, C_L3C);
        fill(categories, 0x6E, 0x6F, C_L3B);
        fill(categories, 0x70, 0x70, C_L4A);
        fill(categories, 0x71, 0x73, C_L4B);
        fill(categories, 0x74, 0x74, C_L4C);
        fill(categories, 0x75, 0x7F, C_ILL);

        fill(transitionTable, 0, transitionTable.length - 1, S_ERR);
        fill(transitionTable, S_CS1 + 0x8, S_CS1 + 0xB, S_END);
        fill(transitionTable, S_CS2 + 0x8, S_CS2 + 0xB, S_CS1);
        fill(transitionTable, S_CS3 + 0x8, S_CS3 + 0xB, S_CS2);
        fill(transitionTable, S_P3A + 0xA, S_P3A + 0xB, S_CS1);
        fill(transitionTable, S_P3B + 0x8, S_P3B + 0x9, S_CS1);
        fill(transitionTable, S_P4A + 0x9, S_P4A + 0xB, S_CS2);
        fill(transitionTable, S_P4B + 0x8, S_P4B + 0x8, S_CS2);

        byte[] firstUnitMasks = { 0x00, 0x00, 0x00, 0x00, 0x1F, 0x0F, 0x0F, 0x0F, 0x07, 0x07, 0x07 };
        byte[] firstUnitTransitions = { S_ERR, S_ERR, S_ERR, S_ERR, S_CS1, S_P3A, S_CS2, S_P3B, S_P4A, S_CS3, S_P4B };

        for (int i = 0x00; i < 0x80; ++i)
        {
            byte category = categories[i];

            int codePoint = i & firstUnitMasks[category];
            byte state = firstUnitTransitions[category];

            firstUnitTable[i] = (short)((codePoint << 8) | state);
        }
    }

    /**
     * Transcode a UTF-8 encoding into a UTF-16 representation. In the general case the output
     * {@code utf16} array should be at least as long as the input {@code utf8} one to handle
     * arbitrary inputs. The number of output UTF-16 code units is returned, or -1 if any errors are
     * encountered (in which case an arbitrary amount of data may have been written into the output
     * array). Errors that will be detected are malformed UTF-8, including incomplete, truncated or
     * "overlong" encodings, and unmappable code points. In particular, no unmatched surrogates will
     * be produced. An error will also result if {@code utf16} is found to be too small to store the
     * complete output.
     *
     * @param utf8
     *            A non-null array containing a well-formed UTF-8 encoding.
     * @param utf16
     *            A non-null array, at least as long as the {@code utf8} array in order to ensure
     *            the output will fit.
     * @return The number of UTF-16 code units written to {@code utf16} (beginning from index 0), or
     *         else -1 if the input was either malformed or encoded any unmappable characters, or if
     *         the {@code utf16} is too small.
     */
    public static int transcodeToUTF16(byte[] utf8, char[] utf16)
    {
        int i = 0, j = 0;

        while (i < utf8.length)
        {
            byte codeUnit = utf8[i++];
            if (codeUnit >= 0)
            {
                if (j >= utf16.length) { return -1; }

                utf16[j++] = (char)codeUnit;
                continue;
            }

            short first = firstUnitTable[codeUnit & 0x7F];
            int codePoint = first >>> 8;
            byte state = (byte)first;

            while (state >= 0)
            {
                if (i >= utf8.length) { return -1; }

                codeUnit = utf8[i++];
                codePoint = (codePoint << 6) | (codeUnit & 0x3F);
                state = transitionTable[state + ((codeUnit & 0xFF) >>> 4)];
            }

            if (state == S_ERR) { return -1; }

            if (codePoint <= 0xFFFF)
            {
                if (j >= utf16.length) { return -1; }

                // Code points from U+D800 to U+DFFF are caught by the DFA
                utf16[j++] = (char)codePoint;
            }
            else
            {
                if (j >= utf16.length - 1) { return -1; }

                // Code points above U+10FFFF are caught by the DFA
                utf16[j++] = (char)(0xD7C0 + (codePoint >>> 10));
                utf16[j++] = (char)(0xDC00 | (codePoint & 0x3FF));
            }
        }

        return j;
    }

  private static byte utf8_code_length[] = {//@formatter:off
          /* Map UTF-8 encoded prefix byte to sequence length.  zero means
          illegal prefix.  see RFC 2279 for details */
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
          2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
          3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
          4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 0, 0
  }; //@formatter:on


  public static String PyUnicode_DecodeUTF8Stateful(String str, String errors, int[] consumed) {
    int size = str.length();
    StringBuilder unicode = new StringBuilder(size);

    /* Unpack UTF-8 encoded data */
    int i;
    for (i = 0; i < size; ) {
      int ch = str.charAt(i);

      if (ch < 0x80) {
        unicode.append((char) ch);
        i++;
        continue;
      }
      if (ch > 0xFF) {
        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
            i, i + 1, "ordinal not in range(255)");
        continue;
      }

      int n = utf8_code_length[ch];

      if (i + n > size) {
        if (consumed != null) {
          break;
        }
        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
            i, i + 1, "unexpected end of data");
        continue;
      }

      switch (n) {
        case 0:
          i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
              i, i + 1, "unexpected code byte");
          continue;
        case 1:
          i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
              i, i + 1, "internal error");
          continue;
        case 2:
          char ch1 = str.charAt(i + 1);
          if ((ch1 & 0xc0) != 0x80) {
            i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                i, i + 2, "invalid data");
            continue;
          }
          ch = ((ch & 0x1f) << 6) | (ch1 & 0x3f);
          if (ch < 0x80) {
            i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                i, i + 2, "illegal encoding");
            continue;
          } else {
            unicode.appendCodePoint(ch);
          }
          break;

        case 3:
          ch1 = str.charAt(i + 1);
          char ch2 = str.charAt(i + 2);
          if ((ch1 & 0xc0) != 0x80 || (ch2 & 0xc0) != 0x80) {
            i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                i, i + 3, "invalid data");
            continue;
          }
          ch = ((ch & 0x0f) << 12) + ((ch1 & 0x3f) << 6) + (ch2 & 0x3f);
          if (ch < 0x800 || (ch >= 0xd800 && ch < 0xe000)) {
            i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                i, i + 3, "illegal encoding");
            continue;
          } else {
            unicode.appendCodePoint(ch);
          }
          break;

        case 4:
          ch1 = str.charAt(i + 1);
          ch2 = str.charAt(i + 2);
          char ch3 = str.charAt(i + 3);
          if ((ch1 & 0xc0) != 0x80 || (ch2 & 0xc0) != 0x80 || (ch3 & 0xc0) != 0x80) {
            i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                i, i + 4, "invalid data");
            continue;
          }
          ch = ((ch & 0x7) << 18) + ((ch1 & 0x3f) << 12) + //
              ((ch2 & 0x3f) << 6) + (ch3 & 0x3f);
          // validate and convert to UTF-16
          if ((ch < 0x10000) || // minimum value allowed for 4 byte encoding
              (ch > 0x10ffff)) { // maximum value allowed for UTF-16
            i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                i, i + 4, "illegal encoding");
            continue;
          }

          unicode.appendCodePoint(ch);
          break;

        default:
          // TODO: support
          /* Other sizes are only needed for UCS-4 */
          i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
              i, i + n, "unsupported Unicode code range");
          continue;
      }
      i += n;
    }

    if (consumed != null) {
      consumed[0] = i;
    }

    return unicode.toString();
  }


  /**
   * Given the return from some codec error handler (invoked while encoding or decoding), which
   * specifies a resume position, and the length of the input being encoded or decoded, check and
   * interpret the resume position. Negative indexes in the error handler return are interpreted as
   * "from the end". If the result would be out of bounds in the input, an
   * <code>IndexError</code> exception is raised.
   *
   * @param size       of byte buffer being decoded
   * @param newPosition returned from error handler
   * @return absolute resume position.
   */
  public static int calcNewPosition(int size, int newPosition) {
    if (newPosition < 0) {
      newPosition = size + newPosition;
    }
    if (newPosition > size || newPosition < 0) {
      throw new RuntimeException(newPosition + " out of bounds of encoded string");
    }
    return newPosition;
  }

  /**
       * Handler for errors encountered during decoding, adjusting the output buffer contents and
       * returning the correct position to resume decoding (if the handler does not simply raise an
       * exception).
       *
       * @param partialDecode output buffer of unicode (as UTF-16) that the codec is building
       * @param errors name of the error policy (or null meaning "strict")
       * @param encoding name of encoding that encountered the error
       * @param toDecode bytes being decoded
       * @param start index of first byte it couldn't decode
       * @param end index+1 of last byte it couldn't decode (usually becomes the resume point)
       * @param reason contribution to error message if any
       * @return the resume position: index of next byte to decode
       */
      public static int insertReplacementAndGetResume(StringBuilder partialDecode, String errors,
              String encoding, String toDecode, int start, int end, String reason) {

          // Handle the two special cases "ignore" and "replace" locally
          if (errors != null) {
              if (errors.equals(IGNORE)) {
                  // Just skip to the first non-problem byte
                  return end;
              } else if (errors.equals(REPLACE)) {
                  // Insert *one* Unicode replacement character and skip
                  partialDecode.appendCodePoint(TextUtil.REPLACEMENT_CHAR);
                  return end;
              }
          }

          // If errors not one of those, invoke the generic mechanism
          //PyObject replacementSpec = decoding_error(errors, encoding, toDecode, start, end, reason);
          // Deliver the replacement unicode text to the output buffer
          //partialDecode.append(replacementSpec.__getitem__(0).toString());

          // Return the index in toDecode at which we should resume
          return calcNewPosition(toDecode.length(), end + 1);
      }
}