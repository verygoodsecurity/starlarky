package com.verygood.security.larky.modules.codecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

public class PyCompat {

  private static final char[] HEXDIGIT = "0123456789ABCDEF".toCharArray();
  private static final char REPLACEMENT_CHAR = '\uFFFD';

  public static List<Byte> asList(byte... backingArray) {
    if (backingArray.length == 0) {
      return Collections.emptyList();
    }
    List<Byte> byteList = new ArrayList<>();
    for(byte b : backingArray) {
      byteList.add(b);
    }
    return byteList;
  }

  public static String hex(final int codepoint) {
      return Integer.toHexString(codepoint).toUpperCase(Locale.ENGLISH);
  }
  /**
     *
     * @param bytearr
     * @return A string representation of the underlying byte array compatible with
     *         python's utf-8 repr() of a bytes-like object.
     */
    public static String pyCompatUTF8repr(byte[] bytearr) {
      StringBuilder v = new StringBuilder(bytearr.length);
      ListIterator<Byte> it = asList(bytearr).listIterator();
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
           v.append(hex(ch));
           v.append("\\x");
           v.append(hex(trail));
           size += 3;
           it.previous(); // rewind one
         }
       } else if (ch < 0x80) {
         if (ch >= ' ' && ch <= '~') {
           v.append((char) ch);
         }
         else if(ch < ' ') {
           v.append("\\x");
           v.append(HEXDIGIT[ch & 0xF]);
           v.append(HEXDIGIT[(ch >> 4)]);
         }
         size++;
       } else if (ch < 0x800) {
         v.append("\\x");
         v.append(hex(ch));
         size += 2;
       } else {
         if (ch > Character.MIN_SUPPLEMENTARY_CODE_POINT) {
           ch = REPLACEMENT_CHAR;
         }
         // MIN_SUPPLEMENTARY_CODE_POINT
         // ch < 0x10000, that is, the largest char value
         v.append("\\u");
         for (int s = 12; s >= 0; s -= 4) {
           v.append(HEXDIGIT[ch >> s & 0xF]);
         }
         size += 3;
       }
      } while(it.hasNext());
      return v.toString();
    }
}
