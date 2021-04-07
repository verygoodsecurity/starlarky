package com.verygood.security.larky.modules.types;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;

import org.junit.Test;

public class LarkyByteLikeTest {


  @Test
  public void testSplit_1() throws EvalException {
    StarlarkList<LarkyByteLike> split;
    split = doSplit("hello world !".getBytes(), Starlark.NONE, Starlark.NONE);
    assertNotNull(split);
    assertEquals(split.size(), 3);
    assertArrayEquals(split.get(0).getBytes(), "hello".getBytes());
    assertArrayEquals(split.get(1).getBytes(), "world".getBytes());
    assertArrayEquals(split.get(2).getBytes(), "!".getBytes());
  }

  private StarlarkList<LarkyByteLike> doSplit(byte[] bytes, Object sepB, Object maxsplit) throws EvalException {
    StarlarkList<LarkyByteLike> split;
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
      Object sep = Starlark.isNullOrNone(sepB)
          ? null
          : makeLarkyByteLike(sepB, thread);
      LarkyByteLike tosplit = LarkyByteArray.builder(thread).setSequence(bytes).build();
      split = tosplit.split(sep, maxsplit, thread);
    }
    return split;
  }

  private LarkyByteLike makeLarkyByteLike(Object sepB, StarlarkThread thread) throws EvalException {
    byte[] bytes;
    if(sepB instanceof String) {
      bytes = ((String) sepB).getBytes();
    } else {
      bytes = (byte[]) sepB;
    }

    return LarkyByteArray.builder(thread).setSequence(bytes).build();
  }

  @Test
  public void testSplit_2() throws EvalException {
    StarlarkList<LarkyByteLike> split = doSplit("hello world !".getBytes(), "lo", Starlark.NONE);
    assertNotNull(split);
    assertEquals(split.size(), 2);
    assertArrayEquals(split.get(0).getBytes(), "hel".getBytes());
    assertArrayEquals(split.get(1).getBytes(), " world !".getBytes());
  }
  @Test
  public void testSplit_3() throws EvalException {
    StarlarkList<LarkyByteLike> split = doSplit(
        "hello world !\n\nfoo xxxx\tbrah".getBytes(),
        Starlark.NONE,
        Starlark.NONE
    );
//    In [10]: b"hello world !\n\nfoo xxxx\tbrah".split()
//    Out[10]: [b'hello', b'world', b'!', b'foo', b'xxxx', b'brah']
    assertNotNull(split);
    assertEquals(split.size(), 6);
    assertArrayEquals(split.get(0).getBytes(), "hello".getBytes());
    assertArrayEquals(split.get(1).getBytes(), "world".getBytes());
    assertArrayEquals(split.get(2).getBytes(), "!".getBytes());
    assertArrayEquals(split.get(3).getBytes(), "foo".getBytes());
    assertArrayEquals(split.get(4).getBytes(), "xxxx".getBytes());
    assertArrayEquals(split.get(5).getBytes(), "brah".getBytes());
  }
}