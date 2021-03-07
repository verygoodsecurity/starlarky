package com.verygood.security.larky.modules.io;

/**
 * Some utility methods for generating hex dumps.
 *
 * @author Niclas Finne, Fredrik Osterlind
 */
public class StringUtils {

  private static final char[] HEX = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
  };

  private StringUtils() {
    // Prevent instances of this class
  }

  public static String toHex(byte data) {
    return "" + HEX[(data >> 4) & 0xf] + HEX[data & 0xf];
  }

  public static String toHex(byte[] data) {
    char[] buf = new char[data.length * 2];
    for (int i = 0, j = 0, n = data.length; i < n; i++, j += 2) {
      buf[j] = HEX[(data[i] >> 4) & 0xf];
      buf[j + 1] = HEX[data[i] & 0xf];
    }
    return new String(buf);
  }

  public static String toHex(byte[] data, int bytesPerGroup) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, n = data.length; i < n; i++) {
      if ((i % bytesPerGroup) == 0 && i > 0) {
        sb.append(' ');
      }
      sb.append(HEX[(data[i] >> 4) & 0xf]);
      sb.append(HEX[data[i] & 0xf]);
    }
    return sb.toString();
  }

  public static String hexDump(byte[] data) {
    return hexDump(data, 5, 4);
  }

  public static byte[] fromHex(String[] data) {
    StringBuilder sb = new StringBuilder();
    for (String s : data) {
      sb.append(s);
    }
    return fromHex(sb.toString());
  }

  /**
   * This method is compatible with the output from {@link #toHex(byte)}.
   *
   * @param data Hexadecimal data
   * @return Binary data
   * @see #toHex(byte[], int)
   */
  public static byte[] fromHex(String data) {
    data = data.replace(" ", "");
    if (data.length() % 2 != 0) {
      throw new RuntimeException("Bad hex string: " + data);
    }
    byte[] bin = new byte[data.length() / 2];
    for (int i = 0; i < bin.length; i++) {
      bin[i] = (byte) (0xff & Integer.parseInt(data.substring(i * 2, i * 2 + 2), 16));
    }
    return bin;
  }

  public static String hexDump(byte[] data, int groupsPerLine, int bytesPerGroup) {
    if (bytesPerGroup <= 0) {
      throw new IllegalArgumentException("0 bytes per group");
    }
    if (groupsPerLine <= 0) {
      groupsPerLine = 1;
    }
    final int bytesPerLine = groupsPerLine * bytesPerGroup;
    StringBuilder sb = new StringBuilder();
    for (int j = 0; j < data.length; j += bytesPerLine) {
      int n = data.length - j;
      if (n > bytesPerLine) {
        n = bytesPerLine;
      }
      for (int i = 0; i < bytesPerLine; i++) {
        if ((i % bytesPerGroup) == 0 && i > 0) {
          sb.append(' ');
        }
        if (i < n) {
          sb.append(HEX[(data[j + i] >> 4) & 0xf]);
          sb.append(HEX[data[j + i] & 0xf]);
        } else {
          sb.append("  ");
        }
      }
      sb.append("  ");
      for (int i = 0; i < n; i++) {
        if (data[j + i] >= 32) {
          sb.append((char) (data[j + i] & 0xff));
        } else {
          sb.append('.');
        }
      }
      sb.append('\n');
    }
    return sb.toString();

  }

  /**
   * Unescapes a string that contains standard Java escape sequences.
   * <ul>
   * <li><strong>&#92;b &#92;f &#92;n &#92;r &#92;t &#92;" &#92;'</strong> :
   * BS, FF, NL, CR, TAB, double and single quote.</li>
   * <li><strong>&#92;X &#92;XX &#92;XXX</strong> : Octal character
   * specification (0 - 377, 0x00 - 0xFF).</li>
   * <li><strong>&#92;uXXXX</strong> : Hexadecimal based Unicode character.</li>
   * </ul>
   *
   * @param st A string optionally containing standard java escape sequences.
   * @return The translated string.
   */
  public String unescapeJavaString(String st) {

    StringBuilder sb = new StringBuilder(st.length());

    for (int i = 0; i < st.length(); i++) {
      char ch = st.charAt(i);
      if (ch == '\\') {
        char nextChar = (i == st.length() - 1) ? '\\' : st
            .charAt(i + 1);
        // Octal escape?

        if (nextChar >= '0' && nextChar <= '7') {
          String code = "" + nextChar;
          i++;
          if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
              && st.charAt(i + 1) <= '7') {
            code += st.charAt(i + 1);
            i++;
            if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
                && st.charAt(i + 1) <= '7') {
              code += st.charAt(i + 1);
              i++;
            }
          }
          sb.append((char) Integer.parseInt(code, 8));
          continue;
        }
        switch (nextChar) {
          case '\\':
            ch = '\\';
            break;
          case 'b':
            ch = '\b';
            break;
          case 'f':
            ch = '\f';
            break;
          case 'n':
            ch = '\n';
            break;
          case 'r':
            ch = '\r';
            break;
          case 't':
            ch = '\t';
            break;
          case '\"':
            ch = '\"';
            break;
          case '\'':
            ch = '\'';
            break;
          // Hex Unicode: u????
          case 'u':
            if (i >= st.length() - 5) {
              ch = 'u';
              break;
            }
            int code = Integer.parseInt(
                "" + st.charAt(i + 2) + st.charAt(i + 3)
                    + st.charAt(i + 4) + st.charAt(i + 5), 16);
            sb.append(Character.toChars(code));
            i += 5;
            continue;
        }
        i++;
      }
      sb.append(ch);
    }
    return sb.toString();
  }
}