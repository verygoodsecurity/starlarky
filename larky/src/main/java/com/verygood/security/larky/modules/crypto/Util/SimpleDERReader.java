package com.verygood.security.larky.modules.crypto.Util;

import java.math.BigInteger;

public class SimpleDERReader {

  private static final int CONSTRUCTED = 0x20;

  private byte[] buffer;
  private int pos;
  private int count;

  /**
   * Create a new reader.
   *
   * @param b The new content of this reader.
   */
  public SimpleDERReader(final byte[] b) {
    resetInput(b);
  }

  /**
   * Create a new reader.
   *
   * @param b   The new content of this reader.
   * @param off The position to start reading from.
   * @param len The size limit of the content.
   */
  public SimpleDERReader(final byte[] b, final int off, final int len) {
    resetInput(b, off, len);
  }

  /**
   * Resets the reader state.
   *
   * @param b the new content.
   */
  public void resetInput(final byte[] b) {
    resetInput(b, 0, b.length);
  }

  /**
   * Resets the reader state.
   *
   * @param b   The new content of this reader.
   * @param off The position to start reading from.
   * @param len The size limit of the content.
   */
  public void resetInput(final byte[] b, final int off, final int len) {
    buffer = b.clone();
    pos = off;
    count = len;
  }

  /**
   * Reads a single byte.
   *
   * @return The byte from the DER stream.
   */
  private byte readByte() {
    if (count <= 0) {
      throw new IllegalArgumentException("DER byte array: out of data");
    }
    count--;
    return buffer[pos++];
  }

  /**
   * Reads an array of bytes.
   *
   * @param len The number of bytes to read.
   * @return The array of bytes.
   */
  private byte[] readBytes(final int len) {
    if (len > count) {
      throw new IllegalArgumentException("DER byte array: out of data");
    }
    final byte[] b = new byte[len];
    System.arraycopy(buffer, pos, b, 0, len);
    pos += len;
    count -= len;
    return b;
  }

  /**
   * Determine amount of available data.
   *
   * @return The number of bytes currently available.
   */
  public int available() {
    return count;
  }

  // CHECKSTYLE IGNORE MagicNumber FOR NEXT 147 LINES

  /**
   * Read the length of the next DER object.
   *
   * @return The length of the next object in bytes.
   */
  private int readLength() {
    int len = readByte() & 0xff;
    if ((len & 0x80) == 0) {
      return len;
    }
    int remain = len & 0x7F;
    if (remain == 0) {
      return -1;
    }
    len = 0;
    while (remain > 0) {
      len = len << 8;
      len = len | (readByte() & 0xff);
      remain--;
    }
    return len;
  }

  /**
   * Skips the next DER object.
   *
   * @return The type of the DER object just skipped.
   */
  public int ignoreNextObject() {
    final int type = readByte() & 0xff;
    final int len = readLength();
    if (len < 0 || len > available()) {
      throw new IllegalArgumentException("Illegal len in DER object (" + len + ")");
    }
    readBytes(len);
    return type;
  }

  /**
   * Reads a big integer value.
   *
   * @return The value of the integer.
   */
  public BigInteger readInt() {
    final int type = readByte() & 0xff;
    if (type != 0x02) {
      throw new IllegalArgumentException("Expected DER Integer, but found type " + type);
    }
    int len = readLength();
    if (len < 0 || len > available()) {
      throw new IllegalArgumentException("Illegal len in DER object (" + len + ")");
    }
    final byte[] b = readBytes(len);
    return new BigInteger(b);
  }

  /**
   * Reads the type of an constructed (compound) DER object.
   *
   * @return The type of the DER constructed object.
   */
  public int readConstructedType() {
    final int type = readByte() & 0xff;
    if ((type & CONSTRUCTED) != CONSTRUCTED) {
      throw new IllegalArgumentException("Expected constructed type, but was " + type);
    }
    return type & 0x1f;
  }

  /**
   * Prepare for reading a constructed (compound) DER object.
   *
   * @return A new reader instance for reading the constructed DER object.
   */
  public SimpleDERReader readConstructed() {
    final int len = readLength();
    if (len < 0 || len > available()) {
      throw new IllegalArgumentException("Illegal len in DER object (" + len + ")");
    }
    SimpleDERReader cr = new SimpleDERReader(buffer, pos, len);
    pos += len;
    count -= len;
    return cr;
  }

  /**
   * Reads a DER secuence.
   *
   * @return A byte array, containing the sequuence content.
   */
  public byte[] readSequenceAsByteArray() {
    final int type = readByte() & 0xff;
    if (type != 0x30) {
      throw new IllegalArgumentException("Expected DER Sequence, but found type " + type);
    }
    final int len = readLength();
    if (len < 0 || len > available()) {
      throw new IllegalArgumentException("Illegal len in DER object (" + len + ")");
    }
    return readBytes(len);
  }

  /**
   * Reads an OID.
   *
   * @return A String reprensentation of the OID.
   */
  public String readOid() {
    final int type = readByte() & 0xff;
    if (type != 0x06) {
      throw new IllegalArgumentException("Expected DER OID, but found type " + type);
    }
    final int len = readLength();
    if (len < 1 || len > available()) {
      throw new IllegalArgumentException("Illegal len in DER object (" + len + ")");
    }
    final byte[] b = readBytes(len);
    long value = 0;
    final StringBuilder sb = new StringBuilder(64);
    switch (b[0] / 40) {
      case 0:
        sb.append('0');
        break;
      case 1:
        sb.append('1');
        b[0] -= 40;
        break;
      default:
        sb.append('2');
        b[0] -= 80;
        break;
    }
    for (int i = 0; i < len; i++) {
      value = value << 7 + b[i] & 0x7F;
      if ((b[i] & 0x80) == 0) {
        sb.append('.');
        sb.append(value);
        value = 0;
      }
    }
    return sb.toString();
  }

  /**
   * Reads an octet string.
   *
   * @return The content ad byte array.
   */
  public byte[] readOctetString() {
    final int type = readByte() & 0xff;
    if (type != 0x04 && type != 0x03) {
      throw new IllegalArgumentException("Expected DER Octetstring, but found type " + type);
    }
    final int len = readLength();
    if (len < 0 || len > available()) {
      throw new IllegalArgumentException("Illegal len in DER object (" + len + ")");
    }
    return readBytes(len);
  }

}
