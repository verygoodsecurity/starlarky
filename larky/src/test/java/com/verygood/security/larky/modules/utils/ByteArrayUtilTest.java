package com.verygood.security.larky.modules.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ByteArrayUtilTest {
  @Test
  public void testCompareUnsigned() {
    assertEquals(0, ByteArrayUtil.compareUnsigned(new byte[]{-1, 0, 2, 4, 5}, new byte[]{-1, 0, 2, 4, 5}));
    assertEquals(-1, ByteArrayUtil.compareUnsigned(new byte[]{-1, 0, 2, 4, 5}, new byte[]{-1, 0, 2, 4, 6}));
    assertEquals(1, ByteArrayUtil.compareUnsigned(new byte[]{-1, 0, 2, 4, 6}, new byte[]{-1, 0, 2, 4, 5}));

  }

  /**
   * Test method for {@link ByteArrayUtil#join(byte[], java.util.List)}.
   */
  @Test
  public void testJoinByteArrayListOfbyte() {
    byte[] a = new byte[]{'a', 'b', 'c'};
    byte[] b = new byte[]{'d', 'e', 'f'};

    List<byte[]> parts = new ArrayList<byte[]>();
    parts.add(a);
    parts.add(b);
    parts.add(new byte[]{});
    byte[] result = new byte[]{'a', 'b', 'c', 'z', 'd', 'e', 'f', 'z'};
    assertArrayEquals(result, ByteArrayUtil.join(new byte[]{'z'}, parts));

    parts = new ArrayList<byte[]>();
    parts.add(new byte[]{});
    parts.add(a);
    parts.add(b);
    result = new byte[]{'z', 'a', 'b', 'c', 'z', 'd', 'e', 'f'};
    assertArrayEquals(result, ByteArrayUtil.join(new byte[]{'z'}, parts));

    parts = new ArrayList<byte[]>();
    parts.add(new byte[]{});
    parts.add(new byte[]{});
    parts.add(a);
    parts.add(b);
    result = new byte[]{'z', 'z', 'a', 'b', 'c', 'z', 'd', 'e', 'f'};
    assertArrayEquals(result, ByteArrayUtil.join(new byte[]{'z'}, parts));

    parts = new ArrayList<byte[]>();
    parts.add(a);
    parts.add(new byte[]{});
    parts.add(new byte[]{});
    parts.add(b);
    result = new byte[]{'a', 'b', 'c', 'z', 'z', 'z', 'd', 'e', 'f'};
    assertArrayEquals(result, ByteArrayUtil.join(new byte[]{'z'}, parts));

    parts = new ArrayList<byte[]>();
    parts.add(a);
    parts.add(b);
    parts.add(new byte[]{'b'});
    result = new byte[]{'a', 'b', 'c', 'z', 'd', 'e', 'f', 'z', 'b'};
    assertArrayEquals(result, ByteArrayUtil.join(new byte[]{'z'}, parts));

    parts = new ArrayList<byte[]>();
    parts.add(new byte[]{});
    parts.add(new byte[]{});
    parts.add(new byte[]{});
    result = new byte[]{'z', 'z'};
    assertArrayEquals(result, ByteArrayUtil.join(new byte[]{'z'}, parts));

    parts = new ArrayList<byte[]>();
    parts.add(new byte[]{});
    parts.add(new byte[]{});
    parts.add(new byte[]{});
    result = new byte[]{};
    assertArrayEquals(result, ByteArrayUtil.join(null, parts));
  }

  /**
   * Test method for {@link ByteArrayUtil#join(byte[][])}.
   */
  @Test
  public void testJoinByteArrayArray() {
    byte[] a = new byte[]{'a', 'b', 'c'};
    byte[] b = new byte[]{'d', 'e', 'f'};

    List<byte[]> parts = new ArrayList<byte[]>();
    parts.add(a);
    parts.add(b);
    parts.add(new byte[]{});
    byte[] result = new byte[]{'a', 'b', 'c', 'd', 'e', 'f'};
    assertArrayEquals(result, ByteArrayUtil.join(parts.toArray(new byte[][]{})));

    parts = new ArrayList<byte[]>();
    parts.add(new byte[]{});
    parts.add(a);
    parts.add(b);
    result = new byte[]{'a', 'b', 'c', 'd', 'e', 'f'};
    assertArrayEquals(result, ByteArrayUtil.join(parts.toArray(new byte[][]{})));

    parts = new ArrayList<byte[]>();
    parts.add(new byte[]{});
    parts.add(new byte[]{});
    parts.add(new byte[]{});
    parts.add(a);
    parts.add(b);
    parts.add(new byte[]{});
    parts.add(new byte[]{});
    parts.add(new byte[]{'b'});
    parts.add(new byte[]{});
    parts.add(new byte[]{});
    parts.add(new byte[]{});
    parts.add(new byte[]{});
    result = new byte[]{'a', 'b', 'c', 'd', 'e', 'f', 'b'};
    assertArrayEquals(result, ByteArrayUtil.join(parts.toArray(new byte[][]{})));

    parts = new ArrayList<byte[]>();
    parts.add(a);
    parts.add(b);
    parts.add(new byte[]{'b'});
    result = new byte[]{'a', 'b', 'c', 'd', 'e', 'f', 'b'};
    assertArrayEquals(result, ByteArrayUtil.join(parts.toArray(new byte[][]{})));

    // Self-referential, with conversion to array
    parts = new ArrayList<byte[]>();
    parts.add(new byte[]{});
    parts.add(a);
    parts.add(b);
    parts.add(new byte[]{});
    assertArrayEquals(ByteArrayUtil.join(a, b), ByteArrayUtil.join(parts.toArray(new byte[][]{})));

    // Test exception on null elements
    boolean isError = false;
    try {
      ByteArrayUtil.join(a, b, null);
    } catch (Exception e) {
      isError = true;
    } finally {
      assertTrue(isError);
    }
  }

  /**
   * Test method for {@link ByteArrayUtil#regionEquals(byte[], int, byte[])}.
   */
  @Test
  public void testRegionEquals() {
    byte[] src = new byte[]{'a', (byte) 12, (byte) 255, 'n', 'm', 'z', 'k'};
    assertTrue(ByteArrayUtil.regionEquals(src, 3, new byte[]{'n', 'm'}));

    assertFalse(ByteArrayUtil.regionEquals(src, 2, new byte[]{'n', 'm'}));

    assertTrue(ByteArrayUtil.regionEquals(null, 0, null));

    assertFalse(ByteArrayUtil.regionEquals(src, 0, null));
  }

  /**
   * Test method for {@link ByteArrayUtil#replace(byte[], byte[], byte[])}.
   */
  @Test
  public void testReplace() {
    byte[] a = new byte[]{'a', 'b', 'c'};
    byte[] b = new byte[]{'d', 'e', 'f'};

    byte[] src = ByteArrayUtil.join(a, b, a, b);
    byte[] result = new byte[]{'z', 'd', 'e', 'f', 'z', 'd', 'e', 'f'};
    assertArrayEquals(result, ByteArrayUtil.replace(src, a, new byte[]{'z'}));

    src = ByteArrayUtil.join(a, b, a, b);
    assertArrayEquals(ByteArrayUtil.join(b, b), ByteArrayUtil.replace(src, a, new byte[]{}));

    src = ByteArrayUtil.join(a, b, a, b);
    assertArrayEquals(ByteArrayUtil.join(a, a), ByteArrayUtil.replace(src, b, new byte[]{}));

    src = ByteArrayUtil.join(a, a, a);
    assertArrayEquals(new byte[]{}, ByteArrayUtil.replace(src, a, new byte[]{}));
  }

  /**
   * Test method for {@link ByteArrayUtil#split(byte[], byte[])}.
   */
  @Test
  public void testSplit() {
    byte[] a = new byte[]{'a', 'b', 'c'};
    byte[] b = new byte[]{'d', 'e', 'f'};

    byte[] src = ByteArrayUtil.join(a, b, a, b, a);
    List<byte[]> parts = ByteArrayUtil.split(src, b);
    assertEquals(parts.size(), 3);
    for (byte[] p : parts) {
      assertArrayEquals(a, p);
    }

    src = ByteArrayUtil.join(b, a, b, a, b, a);
    parts = ByteArrayUtil.split(src, b);
    assertEquals(parts.size(), 4);
    int counter = 0;
    for (byte[] p : parts) {
      if (counter++ == 0)
        assertArrayEquals(new byte[]{}, p);
      else
        assertArrayEquals(a, p);
    }

    src = ByteArrayUtil.join(a, b, a, b, a, b);
    parts = ByteArrayUtil.split(src, b);
    assertEquals(parts.size(), 4);
    counter = 0;
    for (byte[] p : parts) {
      if (counter++ < 3)
        assertArrayEquals(a, p);
      else
        assertArrayEquals(new byte[]{}, p);
    }

    // Multiple ending delimiters
    src = ByteArrayUtil.join(a, b, a, b, a, b, b, b);
    parts = ByteArrayUtil.split(src, b);
    assertEquals(parts.size(), 6);
    counter = 0;
    for (byte[] p : parts) {
      if (counter++ < 3)
        assertArrayEquals(a, p);
      else
        assertArrayEquals(new byte[]{}, p);
    }
  }

  @Test
  public void testReplaceStartingBytes() {
    final byte[] sequence = "abcdefghijklmnopqrstuvwxyz".getBytes();
    final byte[] replacedSequence = "abc".getBytes();
    final byte[] replacingSequence = "123".getBytes();
    final byte[] expected = "123defghijklmnopqrstuvwxyz".getBytes();
    assertArrayEquals(expected, ByteArrayUtil.replace(sequence, replacedSequence, replacingSequence));
  }

  @Test
  public void testReplaceIntermediateBytes() {
    final byte[] sequence = "abcdefghijklmnopqrstuvwxyz".getBytes();
    final byte[] replacedSequence = "mno".getBytes();
    final byte[] replacingSequence = "123".getBytes();
    final byte[] expected = "abcdefghijkl123pqrstuvwxyz".getBytes();
    assertArrayEquals(expected, ByteArrayUtil.replace(sequence, replacedSequence, replacingSequence));
  }

  @Test
  public void testReplaceEndingBytes() {
    final byte[] sequence = "abcdefghijklmnopqrstuvwxyz".getBytes();
    final byte[] replacedSequence = "xyz".getBytes();
    final byte[] replacingSequence = "123".getBytes();
    final byte[] expected = "abcdefghijklmnopqrstuvw123".getBytes();
    assertArrayEquals(expected, ByteArrayUtil.replace(sequence, replacedSequence, replacingSequence));
  }

  @Test
  public void testReplaceWithNotExistingReplacement() {
    final byte[] sequence = "abcdefghijklmnopqrstuvwxyz".getBytes();
    final byte[] replacedSequence = "123".getBytes();
    final byte[] replacingSequence = "456".getBytes();
    assertArrayEquals(sequence, ByteArrayUtil.replace(sequence, replacedSequence, replacingSequence));
  }

  @Test
  public void testReplaceWithNullSequence() {
    final byte[] replacedSequence = "123".getBytes();
    final byte[] replacingSequence = "456".getBytes();
    assertArrayEquals(null, ByteArrayUtil.replace(null, replacedSequence, replacingSequence));
  }

  @Test
  public void testReplaceWithEmptySequence() {
    final byte[] replacedSequence = "123".getBytes();
    final byte[] replacingSequence = "456".getBytes();
    assertArrayEquals(new byte[0], ByteArrayUtil.replace(new byte[0], replacedSequence, replacingSequence));
  }


  @Test
  public void testLoggable() throws Exception {
    String hexRegex = "^\\\\x[0-9a-f][0-9a-f]$";
    for (int i = Byte.MIN_VALUE; i < (byte) ' '; i++) {
      String l = ByteArrayUtil.loggable(new byte[]{(byte) i});
      assertTrue(l + " matches /" + hexRegex + "/", l.matches(hexRegex));
    }
    for (int i = (byte) ' '; i < (byte) '"'; i++) {
      assertEquals(Character.toString((char) i), ByteArrayUtil.loggable(new byte[]{(byte) i}));
    }
    assertEquals("\\x22", ByteArrayUtil.loggable(new byte[]{(byte) '"'}));
    for (int i = (byte) '"' + 1; i < (byte) '='; i++) {
      assertEquals(Character.toString((char) i), ByteArrayUtil.loggable(new byte[]{(byte) i}));
    }
    assertEquals("\\x3d", ByteArrayUtil.loggable(new byte[]{(byte) '='}));
    for (int i = (byte) '=' + 1; i < (byte) '\\'; i++) {
      assertEquals(Character.toString((char) i), ByteArrayUtil.loggable(new byte[]{(byte) i}));
    }
    assertEquals("\\\\", ByteArrayUtil.loggable(new byte[]{'\\'}));
    for (int i = (byte) '\\' + 1; i < (byte) 127; i++) {
      assertEquals(Character.toString((char) i), ByteArrayUtil.loggable(new byte[]{(byte) i}));
    }
    assertEquals("\\x7f", ByteArrayUtil.loggable(new byte[]{127}));
  }

  @Test
  public void testUnprint() throws Exception {
    byte[] allBytes = new byte[Math.abs((int) Byte.MIN_VALUE) + Byte.MAX_VALUE];
    for (byte b = Byte.MIN_VALUE; b < Byte.MAX_VALUE; b++) {
      allBytes[b - Byte.MIN_VALUE] = b;
    }
    assertArrayEquals(allBytes, ByteArrayUtil.unprint(ByteArrayUtil.loggable(allBytes)));
    assertArrayEquals(allBytes, ByteArrayUtil.unprint(ByteArrayUtil.printable(allBytes)));
  }


  @Test
  public void testNormalCase() {
    byte[] bytes1 = {12, 34, 56, 78, 90};
    byte[] bytes2 = {34, 56, 78};
    byte[] bytes3 = {12, 34, 56};
    byte[] bytes4 = {56, 78, 90};
    assertTrue(ByteArrayUtil.matchesAt(bytes1, 0, bytes3));
    assertFalse(ByteArrayUtil.matchesAt(bytes1, 0, bytes2));
    assertTrue(ByteArrayUtil.matchesAt(bytes1, 1, bytes2));
    assertFalse(ByteArrayUtil.matchesAt(bytes1, 1, bytes3));
    assertTrue(ByteArrayUtil.matchesAt(bytes1, 2, bytes4));
    assertFalse(ByteArrayUtil.matchesAt(bytes1, 2, bytes2));
  }

  @Test
  public void testOutOfBoundCase() {
    byte[] bytes1 = {12, 34, 56, 78, 90};
    byte[] bytes2 = {34, 56, 78};
    assertFalse(ByteArrayUtil.matchesAt(bytes1, -1, bytes2));
    assertFalse(ByteArrayUtil.matchesAt(bytes1, 3, bytes2));
  }
}