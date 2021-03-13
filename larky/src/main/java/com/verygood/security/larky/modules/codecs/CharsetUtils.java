package com.verygood.security.larky.modules.codecs;

/**
 * Various special-case charset conversions (for performance).
 *
 * @hide internal use only
 */
public final class CharsetUtils {
    /**
     * Returns a new byte array containing the bytes corresponding to the characters in the given
     * string, encoded in US-ASCII. Unrepresentable characters are replaced by (byte) '?'.
     */
    public static native byte[] toAsciiBytes(String s, int offset, int length);

    /**
     * Returns a new byte array containing the bytes corresponding to the characters in the given
     * string, encoded in ISO-8859-1. Unrepresentable characters are replaced by (byte) '?'.
     */
    public static native byte[] toIsoLatin1Bytes(String s, int offset, int length);

    /**
     * Returns a new byte array containing the bytes corresponding to the characters in the given
     * string, encoded in UTF-8. All characters are representable in UTF-8.
     */
    public static native byte[] toUtf8Bytes(String s, int offset, int length);

    /**
     * Returns a new byte array containing the bytes corresponding to the characters in the given
     * string, encoded in UTF-16BE. All characters are representable in UTF-16BE.
     */
    public static byte[] toBigEndianUtf16Bytes(String s, int offset, int length) {
        byte[] result = new byte[length * 2];
        int end = offset + length;
        int resultIndex = 0;
        for (int i = offset; i < end; ++i) {
            char ch = s.charAt(i);
            result[resultIndex++] = (byte) (ch >> 8);
            result[resultIndex++] = (byte) ch;
        }
        return result;
    }

    /**
     * Decodes the given US-ASCII bytes into the given char[]. Equivalent to but faster than:
     *
     * for (int i = 0; i < count; ++i) {
     *     char ch = (char) (data[start++] & 0xff);
     *     value[i] = (ch <= 0x7f) ? ch : REPLACEMENT_CHAR;
     * }
     */
    public static native void asciiBytesToChars(byte[] bytes, int offset, int length, char[] chars);

    /**
     * Decodes the given ISO-8859-1 bytes into the given char[]. Equivalent to but faster than:
     *
     * for (int i = 0; i < count; ++i) {
     *     value[i] = (char) (data[start++] & 0xff);
     * }
     */
    public static native void isoLatin1BytesToChars(byte[] bytes, int offset, int length, char[] chars);

    private CharsetUtils() {
    }
}