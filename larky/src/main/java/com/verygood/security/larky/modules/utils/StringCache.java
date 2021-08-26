package com.verygood.security.larky.modules.utils;


// Nearly all chars in Starlark strings are ASCII.
// This is a cache of single-char strings to avoid allocation in the s[i] operation.
public final class StringCache {
  private StringCache() {} // un-instantiable

  private static final String[] charsCache = new String[0x80];

  static {
    int i = 0;
    for (i = 0; i < 0x80; i++) {
      charsCache[i] = String.valueOf((char) i).intern();
    }
  }

  /**
   * Returns the string representation of the specified character
   *
   * Semantically equivalent to {@link String#valueOf(char)} but faster for ASCII strings.
   *
   * @param c the character
   * @return the string representation of the specified character
   */
  public static String valueOf(char c) {
    return (c < 0x80) ? charsCache[c] : String.valueOf(c);
  }
}
