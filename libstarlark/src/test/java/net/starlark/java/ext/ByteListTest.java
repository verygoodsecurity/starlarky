package net.starlark.java.ext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Iterables;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;

public class ByteListTest {

  public static byte[] b(final String s) {
    return s.getBytes(StandardCharsets.ISO_8859_1);
  }

  public void checkEqual(ByteList a1, ArrayList<Byte> a2) {
    assertEquals(a1.size(), a2.size());
    for (int i = 0; i < a1.size(); i++) {
      assertEquals(a1.get(i), (byte) a2.get(i));
      assertEquals(a1.byteAt(i), (byte) a2.get(i));
    }
    //noinspection AssertBetweenInconvertibleTypes
    assertEquals(a1, a2);  // testing implicit iterator conversion
    assertTrue(a1.iterator().itemsEqual(a2.iterator()));
    assertTrue(ByteList.OfByte.itemsEqual(a2.iterator(), a1.iterator()));
  }

  public void check(int length) {
    ByteList a1 = new ByteList();
    ArrayList<Byte> a2 = new ArrayList<>();
    Random random = new Random();
    for (int i = 0; i < length; i++) {
      byte l = ((byte) random.nextInt());
      a1.add(l);
      a2.add(l);
    }

    checkEqual(a1, a2);

    boolean flag = false;
    for (Byte l : a2) {
      flag = !flag;
      if (flag) {
        a1.remove(l);
      } else {
        a1.removeValue(l);
      }
    }

    assertTrue(a1.isEmpty());
    a1.addAll(0, a2);
    checkEqual(a1, a2);
  }

  public void performanceChecks(int length) {
    Random random = new Random();
    ByteList data = new ByteList();
    for (int i = 0; i < length; i++) {
      byte l = ((byte) random.nextInt());
      data.add(l);
    }

    long begin1 = System.currentTimeMillis();
    ByteList a1 = new ByteList();

    for (int i = 0; i < length; i++) {
      a1.add(data.get(i));
    }
    //a1.sort(null);
    Object a1c = ByteList.copy(a1);
    for (int i = 0; i < length; i++) {
      a1.removeValue(data.get(i));
    }
    long score1 = (System.currentTimeMillis() - begin1);
    System.out.println("ByteList time : " + score1);

    long begin2 = System.currentTimeMillis();
    ArrayList<Byte> a2 = new ArrayList<>();
    for (int i = 0; i < length; i++) {
      a2.add(data.get(i));
    }
    //a2.sort(null);
    Object a2c = a2.clone();
    for (int i = 0; i < length; i++) {
      a2.remove((Byte) data.get(i));
    }
    long score2 = (System.currentTimeMillis() - begin2);
    System.out.println("ArrayList<Byte> time : " + score2);
    assertTrue(score2 > score1);
    assertEquals(a1c, a2c);
  }

  @Test
  public void testMicroCapacity() {
    for (int i = 0; i < 1000; i++) {
      check(100);
    }
  }

  @Test
  public void testSmallCapacity() {
    for (int i = 0; i < 20; i++) {
      check(10000);
    }
  }

  @Test
  public void testMediumCapacity() {
    for (int i = 0; i < 5; i++) {
      check(100000);
    }
  }

  @Test
  public void testLargeCapacity() {
    for (int i = 0; i < 1; i++) {
      check(1000000);
    }
  }

  @Test
  public void testPerformanceIsBetterThanJDKListImplementation() {
    performanceChecks(100000);
  }

  @Test
  public void subSequenceTest() {
    byte[] init = {'t', 'h', 'i', 's', ' ', 'i', 's', ' ', 'a', ' ', 't', 'e', 's', 't'};
    ByteList ascii = new ByteList(init);
    final int start = 2;
    final int end = init.length;
    CharSequence sub1 = ascii.subSequence(start, end);
    CharSequence sub2 = ascii.subSequence(start, end);
    assertEquals(sub1, sub2);
    for (int i = start; i < end; ++i) {
      assertEquals(init[i], sub1.charAt(i - start));
    }
  }

  /**
   * Tests {@link ByteList#insert(int, int)}.
   */
  protected void testInsert(final ByteList array) {
    final int size = array.size();
    final Object e0 = array.get(0);
    final Object e2 = array.get(2);
    final Object e3 = array.get(3);
    final Object eN = array.get(size - 1);

    array.insert(size, 3);
    assertEquals(size + 3, array.size());
    assertEquals(e0, array.get(0));
    assertEquals(eN, array.get(size - 1));

    array.insert(3, 7);
    assertEquals(size + 10, array.size());
    assertEquals(e0, array.get(0));
    assertEquals(e3, array.get(10));
    assertEquals(eN, array.get(size + 6));

    array.insert(0, 5);
    assertEquals(size + 15, array.size());
    assertEquals(e0, array.get(5));
    assertEquals(e2, array.get(7));
    assertEquals(e3, array.get(15));
    assertEquals(eN, array.get(size + 11));
  }

  /**
   * Tests {@link ByteList#delete(int, int)}.
   */
  protected void testDelete(final ByteList array) {
    final byte[] a = array.toArray();

    array.delete(a.length - 2, 2);
    assertEquals(a.length - 2, array.size());
    for (int i = 0; i < a.length - 2; i++) {
      assertEquals("@" + i, a[i], array.get(i));
    }

    array.delete(0, 2);
    assertEquals(a.length - 4, array.size());
    for (int i = 0; i < a.length - 4; i++) {
      assertEquals("@" + i, a[i + 2], array.get(i));
    }
  }

  /**
   * Tests {@link ByteList#ByteList()}.
   */
  @Test
  public void testConstructorNoArgs() {
    final ByteList array = new ByteList();
    assertEquals(0, array.size());
    assertEquals(0, array.copy().length);
  }

  /**
   * Tests {@link ByteList#ByteList(int)}.
   */
  @Test
  public void testConstructorWithCapacity() {
    final ByteList array = new ByteList(10);
    assertEquals(0, array.size()); // capacity != size
    assertEquals(array.size(), array.length());
    assertEquals(10, array.capacity()); // but the underlying array length *should* have capacity of 10
  }

  /**
   * Tests {@link ByteList#ByteList(byte[])}.
   */
  @Test
  public void testConstructorArray() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw);
    assertSame(raw, array.getArrayUnsafe());
    assertEquals(raw.length, array.size());
    for (int i = 0; i < raw.length; i++) {
      assertEquals("@" + i, raw[i], array.getValue(i));
    }
    assertArrayEquals(raw, array.toArray());
  }

  /**
   * Tests {@link ByteList#addValue(byte)}.
   */
  @Test
  public void testAddValue() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    final byte e6 = 1, e7 = 2;
    array.addValue(e6);
    array.addValue(e7);
    assertEquals(raw.length + 2, array.size());
    for (int i = 0; i < raw.length; i++) {
      assertEquals("@" + i, raw[i], array.getValue(i));
    }
    assertEquals(e6, array.getValue(5));
    assertEquals(e7, array.getValue(6));
  }

  /**
   * Tests {@link ByteList#removeValue(byte)}.
   */
  @Test
  public void testRemoveValue() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    assertEquals(raw.length, array.size());
    array.removeValue(raw[0]);
    assertEquals(raw.length - 1, array.size());
    array.removeValue(raw[2]);
    assertEquals(raw.length - 2, array.size());
    array.removeValue(raw[4]);
    assertEquals(raw.length - 3, array.size());
    assertEquals(raw[1], array.getValue(0));
    assertEquals(raw[3], array.getValue(1));
  }

  /**
   * Tests {@link ByteList#getValue(int)}.
   */
  @Test
  public void testGetValue() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    for (int i = 0; i < raw.length; i++) {
      assertEquals("@" + i, raw[i], array.getValue(i));
    }
  }

  /**
   * Tests {@link ByteList#setValue(int, byte)}.
   */
  @Test
  public void testSetValue() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    final byte e0 = 7, e2 = 1, e4 = 2;
    array.setValue(0, e0);
    array.setValue(2, e2);
    array.setValue(4, e4);
    assertEquals(raw.length, array.size());
    assertEquals(e0, array.getValue(0));
    assertEquals(raw[1], array.getValue(1));
    assertEquals(e2, array.getValue(2));
    assertEquals(raw[3], array.getValue(3));
    assertEquals(e4, array.getValue(4));
  }

  /**
   * Tests {@link ByteList#addValue(int, byte)}.
   */
  @Test
  public void testAddValueIndex() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    final byte e0 = 7, e4 = 1, e7 = 2;
    array.addValue(0, e0);
    array.addValue(4, e4);
    array.addValue(7, e7);
    assertEquals(raw.length + 3, array.size());
    assertEquals(e0, array.getValue(0));
    assertEquals(raw[0], array.getValue(1));
    assertEquals(raw[1], array.getValue(2));
    assertEquals(raw[2], array.getValue(3));
    assertEquals(e4, array.getValue(4));
    assertEquals(raw[3], array.getValue(5));
    assertEquals(raw[4], array.getValue(6));
    assertEquals(e7, array.getValue(7));
  }

  /**
   * Tests {@link ByteList#removeAt(int)}.
   */
  @Test
  public void testRemoveIndex() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    assertEquals(raw.length, array.size());
    byte r = array.removeAt(0);
    assertEquals(3, r);
    assertEquals(raw.length - 1, array.size());
    // {5, 8, 13, 21};
    r = array.removeAt(2);
    assertEquals(13, r);
    assertEquals(raw.length - 2, array.size());
    // {5, 8, 21};
    assertEquals(raw[1], array.getValue(0));
    assertEquals(raw[2], array.getValue(1));
  }

  /**
   * Tests {@link ByteList#indexOf(byte)}.
   */
  @Test
  public void testIndexOf() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    for (int i = 0; i < raw.length; i++) {
      assertEquals("@" + i, i, array.indexOf(raw[i]));
    }
    assertEquals(-1, array.indexOf((byte) -1));
    assertEquals(-1, array.indexOf((byte) 0));
    assertEquals(-1, array.indexOf((byte) 1));
    assertEquals(-1, array.indexOf(Byte.MAX_VALUE));
    assertEquals(-1, array.indexOf(Byte.MIN_VALUE));
  }

  /**
   * Tests {@link ByteList#lastIndexOf(byte)}.
   */
  @Test
  public void testLastIndexOf() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    for (int i = 0; i < raw.length; i++) {
      assertEquals("@" + i, i, array.lastIndexOf(raw[i]));
    }
    assertEquals(-1, array.lastIndexOf((byte) -1));
    assertEquals(-1, array.lastIndexOf((byte) 0));
    assertEquals(-1, array.lastIndexOf((byte) 1));
    assertEquals(-1, array.lastIndexOf(Byte.MAX_VALUE));
    assertEquals(-1, array.lastIndexOf(Byte.MIN_VALUE));
  }

  /**
   * Tests {@link ByteList#contains(byte)}.
   */
  @Test
  public void testContains() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    for (int i = 0; i < raw.length; i++) {
      assertTrue("@" + i, array.contains(raw[i]));
    }
    assertFalse(array.contains(-1));
    assertFalse(array.contains(0));
    assertFalse(array.contains(1));
    assertFalse(array.contains(Byte.MAX_VALUE));
    assertFalse(array.contains(Byte.MIN_VALUE));
  }

  /**
   * Tests: - {@link ByteList#toArray()} - {@link ByteList#setArrayUnsafe(byte[])}. - {@link
   * ByteList#getArrayUnsafe()}.
   */
  @Test
  public void testSetArray() {
    final ByteList array = new ByteList();
    final byte[] raw = {1, 2, 3, 5, 8, 13, 21};
    array.setArrayUnsafe(raw);
    assertSame(raw, array.getArrayUnsafe());
  }

  /**
   * Tests {@link ByteList#insert(int, int)}.
   */
  @Test
  public void testInsert() {
    final byte[] raw = {3, 5, 8, 13, 21};
    testInsert(new ByteList(raw));
  }

  /**
   * Tests {@link ByteList#delete(int, int)}.
   */
  @Test
  public void testDelete() {
    final byte[] raw = {3, 5, 8, 13, 21};
    testDelete(new ByteList(raw));
  }

  /**
   * Tests {@link ByteList#get(int)}.
   */
  @Test
  public void testGet() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    for (int i = 0; i < raw.length; i++) {
      assertEquals("@" + i, raw[i], array.get(i));
    }
  }

  /**
   * Tests {@link ByteList#set(int, Byte)}.
   */
  @Test
  public void testSet() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    final Byte e0 = 7, e2 = 1, e4 = 2;
    array.set(0, e0);
    array.set(2, e2);
    array.set(4, e4);
    assertEquals(raw.length, array.size());
    assertEquals((byte) e0, array.get(0));
    assertEquals(raw[1], array.getValue(1));
    assertEquals((byte) e2, array.get(2));
    assertEquals(raw[3], array.getValue(3));
    assertEquals((byte) e4, array.get(4));
  }

  /**
   * Tests {@link ByteList#add(int, Byte)}.
   */
  @Test
  public void testAdd() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    final Byte e6 = 1, e7 = 2;
    array.add(e6);
    array.add(e7);
    assertEquals(raw.length + 2, array.size());
    for (int i = 0; i < raw.length; i++) {
      assertEquals("@" + i, raw[i], array.getValue(i));
    }
    assertEquals((byte) e6, array.get(5));
    assertEquals((byte) e7, array.get(6));
  }

  /**
   * Tests {@link ByteList#indexOf(byte)}.
   */
  @SuppressWarnings("UnnecessaryBoxing")
  @Test
  public void testIndexOfBoxed() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    for (int i = 0; i < raw.length; i++) {
      assertEquals("@" + i, i, array.indexOf(Byte.valueOf(raw[i])));
    }
    assertEquals(-1, array.indexOf((byte) -1));
    assertEquals(-1, array.indexOf(Byte.valueOf((byte) 0)));
    assertEquals(-1, array.indexOf(Byte.valueOf((byte) 1)));
    assertEquals(-1, array.indexOf(Byte.valueOf(Byte.MAX_VALUE)));
    assertEquals(-1, array.indexOf(Byte.valueOf(Byte.MIN_VALUE)));
  }

  /**
   * Tests {@link ByteList#lastIndexOf(byte)}}.
   */
  @SuppressWarnings("UnnecessaryBoxing")
  @Test
  public void testLastIndexOfBoxed() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    for (int i = 0; i < raw.length; i++) {
      assertEquals("@" + i, i, array.lastIndexOf(Byte.valueOf(raw[i])));
    }
    assertEquals(-1, array.lastIndexOf(Byte.valueOf((byte) -1)));
    assertEquals(-1, array.lastIndexOf(Byte.valueOf((byte) 0)));
    assertEquals(-1, array.lastIndexOf(Byte.valueOf((byte) 1)));
    assertEquals(-1, array.lastIndexOf(Byte.valueOf(Byte.MAX_VALUE)));
    assertEquals(-1, array.lastIndexOf(Byte.valueOf(Byte.MIN_VALUE)));
  }

  /**
   * Tests {@link ByteList#contains(byte)}.
   */
  @Test
  public void testContainsBoxed() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    for (int i = 0; i < raw.length; i++) {
      assertTrue("@" + i, array.contains(Byte.valueOf(raw[i])));
    }
    assertFalse(array.contains(Byte.valueOf((byte) -1)));
    assertFalse(array.contains(Byte.valueOf((byte) 0)));
    assertFalse(array.contains(Byte.valueOf((byte) 1)));
    assertFalse(array.contains(Byte.valueOf(Byte.MAX_VALUE)));
    assertFalse(array.contains(Byte.valueOf(Byte.MIN_VALUE)));
  }

  /**
   * Tests {@link ByteList#removeAt(int)}.
   */
  @Test
  public void testRemove() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    assertEquals(raw.length, array.size());
    array.remove(Byte.valueOf(raw[0]));
    assertEquals(raw.length - 1, array.size());
    array.remove(Byte.valueOf(raw[2]));
    assertEquals(raw.length - 2, array.size());
    array.remove(Byte.valueOf(raw[4]));
    assertEquals(raw.length - 3, array.size());
    assertEquals(raw[1], array.getValue(0));
    assertEquals(raw[3], array.getValue(1));
  }

  /**
   * Tests {@link ByteList#containsAll}.
   */
  @Test
  public void testContainsAll() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());

    final ArrayList<Byte> list = new ArrayList<>();
    assertTrue(array.containsAll(list));
    list.add((byte) 13);
    assertTrue(array.containsAll(list));
    list.add((byte) 1);
    assertFalse(array.containsAll(list));

    final ByteList yes = new ByteList(new byte[]{3, 8, 21});
    assertTrue(Iterables.all(yes, array::contains));

    final ByteList no = new ByteList(new byte[]{5, 13, 1});
    assertFalse(Iterables.all(no, array::contains));
  }

  /**
   * Tests {@link ByteList#addAll(int, java.util.Collection)}.
   */
  @Test
  public void testAddAll() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    final byte[] add = {1, 7};
    final ByteList toAdd = new ByteList(add.clone());
    final int index = 3;
    array.addAll(index, toAdd);
    for (int i = 0; i < index; i++) {
      assertEquals(raw[i], array.getValue(i));
    }
    for (int i = index; i < index + add.length; i++) {
      assertEquals(add[i - index], array.getValue(i));
    }
    for (int i = index + add.length; i < raw.length + add.length; i++) {
      assertEquals(raw[i - add.length], array.getValue(i));
    }
  }

  /**
   * Tests {@link ByteList#removeAll}.
   */
  @Test
  public void testRemoveAll() {
    final byte[] raw = {3, 5, 8, 13, 21};
    final ByteList array = new ByteList(raw.clone());
    final ByteList toRemove = new ByteList(new byte[]{3, 8, 21});
    assertEquals(raw.length, array.size());
    array.removeAll(toRemove);
    assertEquals(raw.length - 3, array.size());
    assertEquals(raw[1], array.getValue(0));
    assertEquals(raw[3], array.getValue(1));
  }

  @Test
  public void testHex() {
    final ByteList three_bytes = ByteList.of(new byte[] {(byte) 0xb9, 0x01, (byte) 0xef});
    assertEquals(three_bytes.hex(), "b901ef");
    assertEquals(three_bytes.hex(ByteList.of(b(":")), 0), "b901ef");
    assertEquals(three_bytes.hex(ByteList.of(b("\u0000"))), "b9\u000001\u0000ef");
    assertEquals(three_bytes.hex(ByteList.of(b("\u0000"))), "b9\u000001\u0000ef");
    assertEquals(three_bytes.hex(ByteList.of(b("\u007f"))), "b9\u007f01\u007fef");
    assertEquals(three_bytes.hex(ByteList.of(b("\u007f"))), "b9\u007f01\u007fef");
    assertEquals(three_bytes.hex(ByteList.of(b(":")), 3), "b901ef");
    assertEquals(three_bytes.hex(ByteList.of(b(":")), 4), "b901ef");
    assertEquals(three_bytes.hex(ByteList.of(b(":")), -4), "b901ef");
    assertEquals(three_bytes.hex(ByteList.of(b(":"))),"b9:01:ef");
    assertEquals(three_bytes.hex(ByteList.of(b("$"))),"b9$01$ef");
    assertEquals(three_bytes.hex(ByteList.of(b(":")), 1),"b9:01:ef");
    assertEquals(three_bytes.hex(ByteList.of(b(":")), -1), "b9:01:ef");
    assertEquals(three_bytes.hex(ByteList.of(b(":")), 2), "b9:01ef");
    assertEquals(three_bytes.hex(ByteList.of(b(":")), 1), "b9:01:ef");
    assertEquals(three_bytes.hex(ByteList.of(b("*")), -2), "b901*ef");

    final ByteList value = ByteList.of(b("{s\u0005\u0000\u0000\u0000worldi\u0002\u0000\u0000\u0000s\u0005\u0000\u0000\u0000helloi\u0001\u0000\u0000\u00000"));
    assertEquals(value.hex(ByteList.of(b(".")), 8), "7b7305000000776f.726c646902000000.730500000068656c.6c6f690100000030");
  }

  @Test
  public void testCount() {
    final ByteList b = ByteList.copy("mississippi");
    byte i = 105;
    byte p = 112;
    byte w = 119;
    assertEquals(4, b.count(b("i")));
    assertEquals(2, b.count(b("ss")));
    assertEquals(0, b.count(b("w")));

    assertEquals(4, b.count(i));
    assertEquals(0, b.count(w));

    assertEquals(2, b.count(b("i"), 6));
    assertEquals(2, b.count(b("p"), 6));
    assertEquals(1, b.count(b("i"), 1, 3));
    assertEquals(1, b.count(b("p"), 7, 9));

    assertEquals(2, b.count(i, 6));
    assertEquals(2, b.count(p, 6));
    assertEquals(1, b.count(i, 1, 3));
    assertEquals(1, b.count(p, 7, 9));
  }

  @Test
  public void testEndsWith() {
    ByteList b = ByteList.of(b("hello"));
    assertFalse(ByteList.empty().endsWith(b("anything")));
    assertTrue(b.endsWith(b("hello")));
    assertTrue(b.endsWith(b("llo")));
    assertTrue(b.endsWith(b("o")));
    assertFalse(b.endsWith(b("whello")));
    assertFalse(b.endsWith(b("no")));
  }

  @Test
  public void testFind() {
    final ByteList b = ByteList.copy("mississippi");
    byte i = 105;
    byte w = 119;
    assertEquals(2, b.find(b("ss")));
    assertEquals(-1, b.find(b("w")));
    assertEquals(-1, b.find(b("mississippian")));

    assertEquals(1, b.find(i));
    assertEquals(-1, b.find(w));

    assertEquals(5, b.find(b("ss"), 3));
    assertEquals(2, b.find(b("ss"), 1, 7));
    assertEquals(-1, b.find(b("ss"), 1, 3));

    assertEquals(7, b.find(i, 6));
    assertEquals(1, b.find(i, 1, 3));
    assertEquals(-1, b.find(w, 1, 3));
  }

  @Test
  public void testJoin() {
    assertEquals(ByteList.empty(), ByteList.of(b("")).join(new byte[][]{new byte[0]}));
    assertEquals(ByteList.empty(), ByteList.of(b("")).join(new byte[][]{b("")}));
    final byte[][][] abcs = {
      new byte[][] {b("abc")},
      new byte[][] {b("a"), b("bc")},
      new byte[][] {b("ab"), b("c")},
      new byte[][] {b("ab"), b("c")},
      new byte[][] {b("a"), b("b"), b("c")},
    };
    final ByteList abc = ByteList.of(b("abc"));
    for (byte[][] bytes : abcs) {
//      System.out.println(Arrays.deepToString(abcs[i]));
      assertEquals(ByteList.empty().join(bytes), abc);
    }
    final ByteList joiner = ByteList.of(b(".:"));
    //final $Function<ByteList, ByteList> dotJoin2 = ByteList.of(b(".:"))::join;
    assertEquals(ByteList.of(b("ab.:cd")), joiner.join(new byte[][]{b("ab"), b("cd")}));
    // Stress it with many items
    ByteList[] seq = new ByteList[100000];
    for (int i = 0; i < seq.length; i++) {
      seq[i] = ByteList.of(b("abc"));
    }
    ByteList expected = ByteList.of(b("abc"))
                          .mergedCopy(ByteList.of(b(".:abc")).repeat(99999));
    assertEquals(expected, joiner.join(seq));
     //Stress test with empty separator
    assertEquals(ByteList.of(b("abc")).repeat(100000), ByteList.of(b("")).join(seq));
   }

  @SuppressWarnings("HttpUrlsUsage")
  @Test
  public void testPartition() {
    final ByteList b = ByteList.copy("mississippi");
    assertArrayEquals(
      new ByteList[]{ByteList.copy("mi"), ByteList.copy("ss"), ByteList.copy("issippi")},
      b.partition(b("ss"))
    );
    assertArrayEquals(
      new ByteList[]{b, ByteList.empty(), ByteList.empty()},
      b.partition(b("w"))
    );
    final ByteList ex = ByteList.copy("this is the partition method");
    assertArrayEquals(
      new ByteList[]{ByteList.copy("this is the par"), ByteList.copy("ti"), ByteList.copy("tion method")},
      ex.partition(b("ti"))
    );
    // from raymond's original specification
    final ByteList S = ByteList.copy("http://www.python.org");

    assertArrayEquals(
      new ByteList[]{ByteList.copy("http"), ByteList.copy("://"), ByteList.copy("www.python.org")},
      S.partition(b("://"))
    );
    assertArrayEquals(
      new ByteList[]{ByteList.copy("http://www.python.org"), ByteList.empty(), ByteList.empty()},
      S.partition(b("?"))
    );
    assertArrayEquals(
      new ByteList[]{ByteList.empty(), ByteList.copy("http://"), ByteList.copy("www.python.org")},
      S.partition(b("http://"))
    );
    assertArrayEquals(
      new ByteList[]{ByteList.copy("http://www.python."), ByteList.copy("org"), ByteList.empty()},
      S.partition(b("org"))
    );
  }

  @SuppressWarnings("HttpUrlsUsage")
  @Test
  public void testRightPartition() {
    final ByteList b = ByteList.copy("mississippi");
    assertArrayEquals(
      new ByteList[]{ByteList.copy("missi"), ByteList.copy("ss"), ByteList.copy("ippi")},
      b.rpartition(b("ss"))
    );
    assertArrayEquals(
      new ByteList[]{ByteList.copy("mississipp"), ByteList.copy("i"), ByteList.empty()},
      b.rpartition(b("i"))
    );
    assertArrayEquals(
      new ByteList[]{ByteList.empty(), ByteList.empty(), b},
      b.rpartition(b("w"))
    );
    final ByteList ex = ByteList.copy("this is the rpartition method");
    assertArrayEquals(
      new ByteList[]{ByteList.copy("this is the rparti"), ByteList.copy("ti"), ByteList.copy("on method")},
      ex.rpartition(b("ti"))
    );
    // from raymond's original specification
    final ByteList S = ByteList.copy("http://www.python.org");

    assertArrayEquals(
      new ByteList[]{ByteList.copy("http"), ByteList.copy("://"), ByteList.copy("www.python.org")},
      S.rpartition(b("://"))
    );
    assertArrayEquals(
      new ByteList[]{ByteList.empty(), ByteList.empty(), ByteList.copy("http://www.python.org")},
      S.rpartition(b("?"))
    );
    assertArrayEquals(
      new ByteList[]{ByteList.empty(), ByteList.copy("http://"), ByteList.copy("www.python.org")},
      S.rpartition(b("http://"))
    );
    assertArrayEquals(
      new ByteList[]{ByteList.copy("http://www.python."), ByteList.copy("org"), ByteList.empty()},
      S.rpartition(b("org"))
    );
  }

  @Test
  public void testStartsWith() {
    final ByteList hello = ByteList.copy("hello");
    assertFalse(ByteList.empty().startsWith(b("anything")));
    assertFalse(ByteList.empty().startsWith(ByteList.copy("anything")));
    assertTrue(hello.startsWith(b("hello")));
    assertTrue(hello.startsWith(b("hel")));
    assertTrue(hello.startsWith(b("h")));
    assertFalse(hello.startsWith(b("hellow")));
    assertFalse(hello.startsWith(b("ha")));
    assertTrue(hello.startsWith(ByteList.copy("hello")));
    assertTrue(hello.startsWith(ByteList.copy("hel")));
    assertTrue(hello.startsWith(ByteList.copy("h")));
    assertFalse(hello.startsWith(ByteList.copy("hellow")));
    assertFalse(hello.startsWith(ByteList.copy("ha")));
  }

  @Test
  public void testReplace() {
    final ByteList hello = ByteList.copy("mississippi");
    assertEquals(ByteList.copy("massassappa"), hello.replace(b("i"), b("a")));
    assertEquals(ByteList.copy("mixixippi"), hello.replace(b("ss"), b("x")));
  }

  @Test
  public void testSplit() {
//    for b in (b'a\x1Cb', b'a\x1Db', b'a\x1Eb', b'a\x1Fb'):
    final byte[][] bytes = {
      b("a\u001Cb"), b("a\u001Db"), b("a\u001Eb"), b("a\u001Fb")
    };
    for(byte[] bz : bytes) {
//        assertEquals(b.split(), [b])
      assertArrayEquals(ByteList.of(bz).split(), new ByteList[] { ByteList.of(bz) });
    }

    final ByteList b = ByteList.copy("\u0009\n\u000B\u000C\r\u001C\u001D\u001E\u001F");
    assertArrayEquals(b.split(), new ByteList[] {ByteList.of(b("\u001c\u001d\u001e\u001f"))});
    assertArrayEquals(ByteList.of(b("a b")).split(ByteList.of(b(" "))), new ByteList[] {ByteList.copy("a"), ByteList.copy("b")});
    // by 1 byte
    assertArrayEquals(new ByteList[] { ByteList.of(b("a")), ByteList.of(b("b")), ByteList.of(b("c")), ByteList.of(b("d")) }, ByteList.of(b("a|b|c|d")).split(ByteList.of(b("|"))));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a|b|c|d")) }, ByteList.of(b("a|b|c|d")).split(ByteList.of(b("|")),0));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a")), ByteList.of(b("b|c|d"))}, ByteList.of(b("a|b|c|d")).split(ByteList.of(b("|")),1));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a")), ByteList.of(b("b")), ByteList.of(b("c|d"))}, ByteList.of(b("a|b|c|d")).split(ByteList.of(b("|")),2));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a")), ByteList.of(b("b")), ByteList.of(b("c")), ByteList.of(b("d"))}, ByteList.of(b("a|b|c|d")).split(ByteList.of(b("|")),3));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a")), ByteList.of(b("b")), ByteList.of(b("c")), ByteList.of(b("d"))}, ByteList.of(b("a|b|c|d")).split(ByteList.of(b("|")),4));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a")), ByteList.of(b("b")), ByteList.of(b("c")), ByteList.of(b("d"))}, ByteList.of(b("a|b|c|d")).split(ByteList.of(b("|")), Integer.MAX_VALUE - 2));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a|b|c|d"))}, ByteList.of(b("a|b|c|d")).split(ByteList.of(b("|")),0));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a")), ByteList.of(b("")), ByteList.of(b("b||c||d"))}, ByteList.of(b("a||b||c||d")).split(ByteList.of(b("|")),2));
    assertArrayEquals(new ByteList[] { ByteList.of(b("abcd"))}, ByteList.of(b("abcd")).split(ByteList.of(b("|"))));
    assertArrayEquals(new ByteList[] { ByteList.of(b(""))}, ByteList.of(b("")).split(ByteList.of(b("|"))));
    assertArrayEquals(new ByteList[] { ByteList.of(b("endcase ")), ByteList.of(b(""))}, ByteList.of(b("endcase |")).split(ByteList.of(b("|"))));
    assertArrayEquals(new ByteList[] { ByteList.of(b("")), ByteList.of(b(" startcase"))}, ByteList.of(b("| startcase")).split(ByteList.of(b("|"))));
    assertArrayEquals(new ByteList[] { ByteList.of(b("")), ByteList.of(b("bothcase")), ByteList.of(b(""))}, ByteList.of(b("|bothcase|")).split(ByteList.of(b("|"))));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a")), ByteList.of(b("")), ByteList.of(b("b\u0000c\u0000d"))}, ByteList.of(b("a\u0000\u0000b\u0000c\u0000d")).split(ByteList.of(b("\u0000")),2));

    // by a bytelist
    assertArrayEquals(new ByteList[] {ByteList.of(b("a")), ByteList.of(b("b")), ByteList.of(b("c")), ByteList.of(b("d"))}, ByteList.of(b("a//b//c//d")).split(ByteList.of(b("//"))));

  }

  @Test
  public void testRSplit() {
    final ByteList b = ByteList.of(b("\u0009\n\u000B\u000C\r\u001C\u001D\u001E\u001F"));
    assertArrayEquals(b.split(), new ByteList[] {ByteList.of(b("\u001c\u001d\u001e\u001f"))});
    assertArrayEquals(ByteList.of(b("a b")).rsplit(ByteList.of(b(" "))), new ByteList[] {ByteList.copy("a"), ByteList.copy("b")});
    // by 1 byte
    assertArrayEquals(new ByteList[] { ByteList.of(b("a")), ByteList.of(b("b")), ByteList.of(b("c")), ByteList.of(b("d")) }, ByteList.of(b("a|b|c|d")).rsplit(ByteList.of(b("|"))));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a|b|c")), ByteList.of(b("d")) }, ByteList.of(b("a|b|c|d")).rsplit(ByteList.of(b("|")),1));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a|b")), ByteList.of(b("c")), ByteList.of(b("d"))}, ByteList.of(b("a|b|c|d")).rsplit(ByteList.of(b("|")),2));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a")), ByteList.of(b("b")), ByteList.of(b("c")), ByteList.of(b("d"))}, ByteList.of(b("a|b|c|d")).rsplit(ByteList.of(b("|")),3));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a")), ByteList.of(b("b")), ByteList.of(b("c")), ByteList.of(b("d"))}, ByteList.of(b("a|b|c|d")).rsplit(ByteList.of(b("|")),4));
    assertArrayEquals(new ByteList[] { ByteList.of(b("a")), ByteList.of(b("b")), ByteList.of(b("c")), ByteList.of(b("d"))}, ByteList.of(b("a|b|c|d")).rsplit(ByteList.of(b("|")), Integer.MAX_VALUE - 100));

    // by a bytelist
    assertArrayEquals(new ByteList[] { ByteList.of(b("a")), ByteList.of(b("b")), ByteList.of(b("c")), ByteList.of(b("d"))}, ByteList.of(b("a//b//c//d")).rsplit(ByteList.of(b("//"))));
  }

  @Test
  public void testStrip() {
    assertEquals(ByteList.of(b("abc")).strip(ByteList.of(b("ac"))), ByteList.of(b("b")));
    // whitespace
    assertEquals(ByteList.of(b("hello")), ByteList.of(b("   hello   ")).strip());
    assertEquals(ByteList.of(b("hello")), ByteList.of(b("hello")).strip());
    final ByteList b = ByteList.copy(" \t\n\r\f\u000babc \t\n\r\f\u000b");
    assertEquals(ByteList.copy("abc"), b.strip());

    // strip with None arg
    assertEquals(ByteList.of(b("hello")), ByteList.of(b("   hello   ")).strip(null));
    assertEquals(ByteList.of(b("hello")), ByteList.of(b("hello")).strip(null));

    // strip with byte string arg
    assertEquals(ByteList.of(b("hello")), ByteList.of(b("xyzzyhelloxyzzy")).strip(ByteList.of(b("xyz"))));
    assertEquals(ByteList.of(b("hello")), ByteList.of(b("hello")).strip(ByteList.of(b("xyz"))));
    assertEquals(ByteList.of(b("")), ByteList.of(b("mississippi")).strip(ByteList.of(b("mississippi"))));

    // only trim the start and end; does not strip internal characters
    assertEquals(ByteList.of(b("mississipp")), ByteList.of(b("mississippi")).strip(ByteList.of(b("i"))));
  }

  @Test
  public void testLeftStrip() {
    assertEquals(ByteList.of(b("abc")).lstrip(ByteList.of(b("ac"))), ByteList.of(b("bc")));
    assertEquals(ByteList.of(b("hello   ")), ByteList.of(b("   hello   ")).lstrip());
    final ByteList b = ByteList.copy(" \t\n\r\f\u000babc \t\n\r\f\u000b");
    assertEquals(ByteList.copy("abc \t\n\r\f\u000b"), b.lstrip());
    // lstrip with None arg
    assertEquals(ByteList.of(b("hello   ")), ByteList.of(b("   hello   ")).lstrip(null));
    // lstrip with byte string arg
    assertEquals(ByteList.of(b("helloxyzzy")), ByteList.of(b("xyzzyhelloxyzzy")).lstrip(ByteList.of(b("xyz"))));
  }

  @Test
  public void testRightStrip() {
    assertEquals(ByteList.of(b("abc")).rstrip(ByteList.of(b("ac"))), ByteList.of(b("ab")));
    assertEquals(ByteList.of(b("   hello")), ByteList.of(b("   hello   ")).rstrip());
    final ByteList b = ByteList.copy(" \t\n\r\f\u000babc \t\n\r\f\u000b");
    assertEquals(ByteList.copy(" \t\n\r\f\u000babc"), b.rstrip());
    // rstrip with None arg
    assertEquals(ByteList.of(b("   hello")), ByteList.of(b("   hello   ")).rstrip(null));
    // rstrip with byte string arg
    assertEquals(ByteList.of(b("xyzzyhello")), ByteList.of(b("xyzzyhelloxyzzy")).rstrip(ByteList.of(b("xyz"))));
  }

  @Test
  public void testRightFind() {
    final ByteList b = ByteList.copy("mississippi");
    byte i = 105;
    byte w = 119;
    assertEquals(b.rfind(ByteList.of(b("ss"))), 5);
    assertEquals(b.rfind(ByteList.of(b("w"))), -1);
    assertEquals(b.rfind(ByteList.of(b("mississippian"))), -1);

    assertEquals(b.rfind(i), 10);
    assertEquals(b.rfind(w), -1);

    assertEquals(b.rfind(ByteList.of(b("ss")), 3), 5);
    assertEquals(b.rfind(ByteList.of(b("ss")), 0, 6), 2);

    assertEquals(b.rfind(i, 1, 3), 1);
    assertEquals(b.rfind(i, 3, 9), 7);
    assertEquals(b.rfind(w, 1, 3), -1);

    assertEquals(9,  ByteList.of(b("abcdefghiabc")).rfind(ByteList.of(b("abc"))));
    assertEquals(12, ByteList.of(b("abcdefghiabc")).rfind(ByteList.of(b(""))));
    assertEquals(0, ByteList.of(b("abcdefghiabc")).rfind(ByteList.of(b("abcd"))));
    assertEquals(-1, ByteList.of(b("abcdefghiabc")).rfind(ByteList.of(b("abcz"))));

    assertEquals(3, ByteList.of(b("abc")).rfind(ByteList.of(b("")),0));
    assertEquals(3, ByteList.of(b("abc")).rfind(ByteList.of(b("")),3));
    assertEquals(-1, ByteList.of(b("abc")).rfind(ByteList.of(b("")),4));

    // to check the ability to pass null as defaults;
    assertEquals(12, ByteList.of(b("rrarrrrrrrrra")).rfind(ByteList.of(b("a"))));
    assertEquals(12, ByteList.of(b("rrarrrrrrrrra")).rfind(ByteList.of(b("a")),4));
    assertEquals(-1, ByteList.of(b("rrarrrrrrrrra")).rfind(ByteList.of(b("a")),4,6));
    assertEquals(12, ByteList.of(b("rrarrrrrrrrra")).rfind(ByteList.of(b("a")),4,-1));
    assertEquals(2, ByteList.of(b("rrarrrrrrrrra")).rfind(ByteList.of(b("a")),0,6));
  }

  @Test
  public void testLeftJustify() {
    assertEquals(ByteList.of(b("abc       ")), ByteList.of(b("abc")).ljust(10));
    assertEquals(ByteList.of(b("abc   ")), ByteList.of(b("abc")).ljust(6));
    assertEquals(ByteList.of(b("abc")), ByteList.of(b("abc")).ljust(3));
    assertEquals(ByteList.of(b("abc")), ByteList.of(b("abc")).ljust(2));
    assertEquals(ByteList.of(b("abc*******")), ByteList.of(b("abc")).ljust(10,ByteList.of(b("*"))));
  }

  @Test
  public void testRightJustify() {
    assertEquals(ByteList.of(b("       abc")), ByteList.of(b("abc")).rjust(10));
    assertEquals(ByteList.of(b("   abc")), ByteList.of(b("abc")).rjust(6));
    assertEquals(ByteList.of(b("abc")), ByteList.of(b("abc")).rjust(3));
    assertEquals(ByteList.of(b("abc")), ByteList.of(b("abc")).rjust(2));
    assertEquals(ByteList.of(b("*******abc")), ByteList.of(b("abc")).rjust(10,b("*")));
    assertEquals(ByteList.of(b("*******abc")), ByteList.of(b("abc")).rjust(10,ByteList.of(b("*"))));
    final ByteList b = ByteList.copy("abc");
    assertEquals(b.rjust(7, b("-")), ByteList.copy("----abc"));
  }

  @Test
  public void testCenter() {
    assertEquals(ByteList.of(b("   abc    ")), ByteList.of(b("abc")).center(10));
    assertEquals(ByteList.of(b(" abc  ")), ByteList.of(b("abc")).center(6));
    assertEquals(ByteList.of(b("abc")), ByteList.of(b("abc")).center(3));
    assertEquals(ByteList.of(b("abc")), ByteList.of(b("abc")).center(2));
    assertEquals(ByteList.of(b("***abc****")), ByteList.of(b("abc")).center(10, b("*")));
    assertEquals(ByteList.of(b("***abc****")), ByteList.of(b("abc")).center(10, ByteList.of(b("*"))));
    // Fill character can be either bytes or bytearray (issue 12380)
    final ByteList b = ByteList.copy("abc");
    assertEquals(b.center(7, b("-")), ByteList.copy("--abc--"));
  }

  @Test
  public void testSwapCase() {
    assertEquals(
      ByteList.of(b("hEllO CoMPuTErS")),
      ByteList.of(b("HeLLo cOmpUteRs")).swapcase());
  }

  @Test
  public void testZFill() {
    assertEquals(ByteList.of(b("123")), ByteList.of(b("123")).zfill(2));
    assertEquals(ByteList.of(b("123")), ByteList.of(b("123")).zfill(3));
    assertEquals(ByteList.of(b("0123")), ByteList.of(b("123")).zfill(4));
    assertEquals(ByteList.of(b("+123")), ByteList.of(b("+123")).zfill(3));
    assertEquals(ByteList.of(b("+123")), ByteList.of(b("+123")).zfill(4));
    assertEquals(ByteList.of(b("+0123")), ByteList.of(b("+123")).zfill(5));
    assertEquals(ByteList.of(b("-123")), ByteList.of(b("-123")).zfill(3));
    assertEquals(ByteList.of(b("-123")), ByteList.of(b("-123")).zfill(4));
    assertEquals(ByteList.of(b("-0123")), ByteList.of(b("-123")).zfill(5));
    assertEquals(ByteList.of(b("000")), ByteList.of(b("")).zfill(3));
    assertEquals(ByteList.of(b("34")), ByteList.of(b("34")).zfill(1));
    assertEquals(ByteList.of(b("0034")), ByteList.of(b("34")).zfill(4));
  }

  @Test
  public void testIsLower() {
    assertFalse(ByteList.of(b("")).islower());
    assertTrue(ByteList.of(b("a")).islower());
    assertFalse(ByteList.of(b("A")).islower());
    assertFalse(ByteList.of(b("\n")).islower());
    assertTrue(ByteList.of(b("abc")).islower());
    assertFalse(ByteList.of(b("aBc")).islower());
    assertTrue(ByteList.of(b("abc\n")).islower());
  }

  @Test
  public void testIsUpper() {
    assertFalse(ByteList.of(b("")).isupper());
    assertFalse(ByteList.of(b("a")).isupper());
    assertTrue(ByteList.of(b("A")).isupper());
    assertFalse(ByteList.of(b("\n")).isupper());
    assertTrue(ByteList.of(b("ABC")).isupper());
    assertFalse(ByteList.of(b("AbC")).isupper());
    assertTrue(ByteList.of(b("ABC\n")).isupper());
  }

  @Test
  public void testIsTitle() {
    assertFalse(ByteList.of(b("")).istitle());
    assertFalse(ByteList.of(b("a")).istitle());
    assertTrue(ByteList.of(b("A")).istitle());
    assertFalse(ByteList.of(b("\n")).istitle());
    assertTrue(ByteList.of(b("A Titlecased Line")).istitle());
    assertTrue(ByteList.of(b("A\nTitlecased Line")).istitle());
    assertTrue(ByteList.of(b("A Titlecased, Line")).istitle());
    assertFalse(ByteList.of(b("Not a capitalized String")).istitle());
    assertFalse(ByteList.of(b("Not\ta Titlecase String")).istitle());
  }

  @Test
  public void testIsSpace() {
    assertFalse(ByteList.of(b("a")).isspace());
    assertTrue(ByteList.of(b(" ")).isspace());
    assertTrue(ByteList.of(b("\t")).isspace());
    assertTrue(ByteList.of(b("\r")).isspace());
    assertTrue(ByteList.of(b("\n")).isspace());
    assertTrue(ByteList.of(b(" \t\r\n")).isspace());
    assertFalse(ByteList.of(b(" \t\r\na")).isspace());
  }

  @Test
  public void testIsAlpha() {
    assertFalse(ByteList.of(b("")).isalpha());
    assertTrue(ByteList.of(b("a")).isalpha());
    assertTrue(ByteList.of(b("A")).isalpha());
    assertFalse(ByteList.of(b("\n")).isalpha());
    assertTrue(ByteList.of(b("abc")).isalpha());
    assertFalse(ByteList.of(b("aBc123")).isalpha());
    assertFalse(ByteList.of(b("abc\n")).isalpha());
  }

  @Test
  public void testIsAlphaNumeric() {
    assertFalse(ByteList.of(b("")).isalnum());
    assertTrue(ByteList.of(b("a")).isalnum());
    assertTrue(ByteList.of(b("A")).isalnum());
    assertFalse(ByteList.of(b("\n")).isalnum());
    assertTrue(ByteList.of(b("123abc456")).isalnum());
    assertTrue(ByteList.of(b("a1b3c")).isalnum());
    assertFalse(ByteList.of(b("aBc000 ")).isalnum());
    assertFalse(ByteList.of(b("abc\n")).isalnum());
  }

  @Test
  public void testIsAscii() {
    assertTrue(ByteList.of(b("")).isascii());
    assertTrue(ByteList.of(b(" ")).isascii());
    assertTrue(ByteList.of(b("")).isascii());
    assertTrue(ByteList.of(b(" ")).isascii());
    assertFalse(ByteList.of(b("")).isascii());
    assertFalse(ByteList.of(b("é")).isascii());
  }

  @Test
  public void testIsDigit() {
    assertFalse(ByteList.of(b("")).isdigit());
    assertFalse(ByteList.of(b("a")).isdigit());
    assertTrue(ByteList.of(b("0")).isdigit());
    assertTrue(ByteList.of(b("0123456789")).isdigit());
    assertFalse(ByteList.of(b("0123456789a")).isdigit());
  }

  @Test
  public void testTitle() {
    assertEquals(ByteList.of(b(" Hello ")), ByteList.of(b(" hello ")).title());
    assertEquals(ByteList.of(b("Hello ")), ByteList.of(b("hello ")).title());
    assertEquals(ByteList.of(b("Hello ")), ByteList.of(b("Hello ")).title());
    assertEquals(ByteList.of(b("Format This As Title String")), ByteList.of(b("fOrMaT thIs aS titLe String")).title());
    assertEquals(ByteList.of(b("Format,This-As*Title;String")), ByteList.of(b("fOrMaT,thIs-aS*titLe;String")).title());
    assertEquals(ByteList.of(b("Getint")), ByteList.of(b("getInt")).title());
  }

  @Test
  public void testLower() {
    assertEquals(ByteList.of(b("hello")), ByteList.of(b("HeLLo")).lower());
    assertEquals(ByteList.of(b("hello")), ByteList.of(b("hello")).lower());
  }

  @Test
  public void testUpper() {
    assertEquals(ByteList.of(b("HELLO")), ByteList.of(b("HeLLo")).upper());
    assertEquals(ByteList.of(b("HELLO")), ByteList.of(b("HELLO")).upper());
  }

  @Test
  public void testCapitalize() {
    assertEquals(ByteList.of(b(" hello ")), ByteList.of(b(" hello ")).capitalize());
    assertEquals(ByteList.of(b("Hello ")), ByteList.of(b("Hello ")).capitalize());
    assertEquals(ByteList.of(b("Hello ")), ByteList.of(b("hello ")).capitalize());
    assertEquals(ByteList.of(b("Aaaa")), ByteList.of(b("aaaa")).capitalize());
    assertEquals(ByteList.of(b("Aaaa")), ByteList.of(b("AaAa")).capitalize());
  }

  @Test
  public void testRemovePrefix() {
    assertEquals(ByteList.of(b("am")), ByteList.of(b("spam")).removeprefix(ByteList.of(b("sp"))));
    assertEquals(ByteList.of(b("spamspam")), ByteList.of(b("spamspamspam")).removeprefix(ByteList.of(b("spam"))));
    assertEquals(ByteList.of(b("spam")), ByteList.of(b("spam")).removeprefix(ByteList.of(b("python"))));
    assertEquals(ByteList.of(b("spam")), ByteList.of(b("spam")).removeprefix(ByteList.of(b("spider"))));
    assertEquals(ByteList.of(b("spam")), ByteList.of(b("spam")).removeprefix(ByteList.of(b("spam and eggs"))));
    assertEquals(ByteList.of(b("")), ByteList.of(b("")).removeprefix(ByteList.of(b(""))));
    assertEquals(ByteList.of(b("")), ByteList.of(b("")).removeprefix(ByteList.of(b("abcde"))));
    assertEquals(ByteList.of(b("abcde")), ByteList.of(b("abcde")).removeprefix(ByteList.of(b(""))));
    assertEquals(ByteList.of(b("")), ByteList.of(b("abcde")).removeprefix(ByteList.of(b("abcde"))));

  }

  @Test
  public void testRemoveSuffix() {
    assertEquals(ByteList.of(b("sp")), ByteList.of(b("spam")).removesuffix(ByteList.of(b("am"))));
    assertEquals(ByteList.of(b("spamspam")), ByteList.of(b("spamspamspam")).removesuffix(ByteList.of(b("spam"))));
    assertEquals(ByteList.of(b("spam")), ByteList.of(b("spam")).removesuffix(ByteList.of(b("python"))));
    assertEquals(ByteList.of(b("spam")), ByteList.of(b("spam")).removesuffix(ByteList.of(b("blam"))));
    assertEquals(ByteList.of(b("spam")), ByteList.of(b("spam")).removesuffix(ByteList.of(b("eggs and spam"))));

    assertEquals(ByteList.of(b("")), ByteList.of(b("")).removesuffix(ByteList.of(b(""))));
    assertEquals(ByteList.of(b("")), ByteList.of(b("")).removesuffix(ByteList.of(b("abcde"))));
    assertEquals(ByteList.of(b("abcde")), ByteList.of(b("abcde")).removesuffix(ByteList.of(b(""))));
    assertEquals(ByteList.of(b("")), ByteList.of(b("abcde")).removesuffix(ByteList.of(b("abcde"))));
  }

  @Test
  public void testSplitLines() {
    assertArrayEquals(new ByteList[] {ByteList.of(b("abc")), ByteList.of(b("def")), ByteList.of(b("")), ByteList.of(b("ghi"))}, ByteList.of(b("abc\ndef\n\rghi")).splitlines());
    assertArrayEquals(new ByteList[] {ByteList.of(b("abc")), ByteList.of(b("def")), ByteList.of(b("")), ByteList.of(b("ghi"))}, ByteList.of(b("abc\ndef\n\r\nghi")).splitlines());
    assertArrayEquals(new ByteList[] {ByteList.of(b("abc")), ByteList.of(b("def")), ByteList.of(b("ghi"))}, ByteList.of(b("abc\ndef\r\nghi")).splitlines());
    assertArrayEquals(new ByteList[] {ByteList.of(b("abc")), ByteList.of(b("def")), ByteList.of(b("ghi"))}, ByteList.of(b("abc\ndef\r\nghi\n")).splitlines());
    assertArrayEquals(new ByteList[] {ByteList.of(b("abc")), ByteList.of(b("def")), ByteList.of(b("ghi")), ByteList.of(b(""))}, ByteList.of(b("abc\ndef\r\nghi\n\r")).splitlines());
    assertArrayEquals(new ByteList[] {ByteList.of(b("")), ByteList.of(b("abc")), ByteList.of(b("def")), ByteList.of(b("ghi")), ByteList.of(b(""))}, ByteList.of(b("\nabc\ndef\r\nghi\n\r")).splitlines());
    assertArrayEquals(new ByteList[] {ByteList.of(b("")), ByteList.of(b("abc")), ByteList.of(b("def")), ByteList.of(b("ghi")), ByteList.of(b(""))}, ByteList.of(b("\nabc\ndef\r\nghi\n\r")).splitlines(false));
    assertArrayEquals(new ByteList[] {ByteList.of(b("\n")), ByteList.of(b("abc\n")), ByteList.of(b("def\r\n")), ByteList.of(b("ghi\n")), ByteList.of(b("\r"))},
                  ByteList.of(b("\nabc\ndef\r\nghi\n\r")).splitlines(true));
    assertArrayEquals(new ByteList[] {ByteList.of(b("")), ByteList.of(b("abc")), ByteList.of(b("def")), ByteList.of(b("ghi")), ByteList.of(b(""))}, ByteList.of(b("\nabc\ndef\r\nghi\n\r")).splitlines(false));
    assertArrayEquals(new ByteList[] {ByteList.of(b("\n")), ByteList.of(b("abc\n")), ByteList.of(b("def\r\n")), ByteList.of(b("ghi\n")), ByteList.of(b("\r"))},
                  ByteList.of(b("\nabc\ndef\r\nghi\n\r")).splitlines(true));
  }

  @Test
  public void testTranslate() {
    final ByteList b = ByteList.copy("hello");
    final byte[] rosetta = new byte[256];
    for (int i = 0; i < rosetta.length; i++) {
      rosetta[i] = (byte) i;
      if(i == 'o') {
        rosetta[i] = (byte) 'e';
      }
    }

    ByteList c = b.translate(rosetta);
    ByteList d = b.translate(rosetta, b(""));
    ByteList dd = b.translate(rosetta, ByteList.of(b("")));
    assertEquals(c, d);
    assertEquals(c, dd);
    assertEquals(c, ByteList.of(b("helle")));

    c = b.translate(rosetta, b("hello"));
    assertEquals(b, ByteList.of(b("hello")));
    assertEquals(c, ByteList.of(b("")));

    c = b.translate(rosetta, b("l"));
    assertEquals(c, ByteList.of(b("hee")));
    c = b.translate(null, b("e"));
    assertEquals(c, ByteList.of(b("hllo")));

    c = b.translate(rosetta, b(""));
    assertEquals(c, ByteList.of(b("helle")));
    c = b.translate(rosetta, b("l"));
    assertEquals(c, ByteList.of(b("hee")));
    c = b.translate(null, b("e"));
    assertEquals(c, ByteList.of(b("hllo")));
  }

  @Test
  public void testExpandTabs() {
    assertEquals(ByteList.of(b("abc\rab      def\ng       hi")), ByteList.of(b("abc\rab\tdef\ng\thi")).expandtabs());
    assertEquals(ByteList.of(b("abc\rab      def\ng       hi")), ByteList.of(b("abc\rab\tdef\ng\thi")).expandtabs(8));
    assertEquals(ByteList.of(b("abc\rab  def\ng   hi")), ByteList.of(b("abc\rab\tdef\ng\thi")).expandtabs(4));
    assertEquals(ByteList.of(b("abc\r\nab      def\ng       hi")), ByteList.of(b("abc\r\nab\tdef\ng\thi")).expandtabs());
    assertEquals(ByteList.of(b("abc\r\nab      def\ng       hi")), ByteList.of(b("abc\r\nab\tdef\ng\thi")).expandtabs(8));
    assertEquals(ByteList.of(b("abc\r\nab  def\ng   hi")), ByteList.of(b("abc\r\nab\tdef\ng\thi")).expandtabs(4));
    assertEquals(ByteList.of(b("abc\r\nab\r\ndef\ng\r\nhi")), ByteList.of(b("abc\r\nab\r\ndef\ng\r\nhi")).expandtabs(4));
    assertEquals(ByteList.of(b("abc\rab      def\ng       hi")), ByteList.of(b("abc\rab\tdef\ng\thi")).expandtabs(8));
    assertEquals(ByteList.of(b("abc\rab  def\ng   hi")), ByteList.of(b("abc\rab\tdef\ng\thi")).expandtabs(4));
    assertEquals(ByteList.of(b("  a\n b")), ByteList.of(b(" \ta\n\tb")).expandtabs(1));
  }
}