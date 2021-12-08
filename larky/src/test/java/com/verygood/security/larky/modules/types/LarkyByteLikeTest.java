package com.verygood.security.larky.modules.types;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;

import org.junit.Test;

public class LarkyByteLikeTest {


  @Test
  public void testSplit_1() throws EvalException {
    StarlarkList<StarlarkBytes> split;
    split = doSplit("hello world !".getBytes(), Starlark.NONE, Starlark.NONE);
    assertNotNull(split);
    assertEquals(split.size(), 3);
    assertArrayEquals(split.get(0).toByteArray(), "hello".getBytes());
    assertArrayEquals(split.get(1).toByteArray(), "world".getBytes());
    assertArrayEquals(split.get(2).toByteArray(), "!".getBytes());
  }

  private StarlarkList<StarlarkBytes> doSplit(byte[] bytes, Object sepB, Object maxsplit) throws EvalException {
    StarlarkList<StarlarkBytes> split;
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
      Object sep = Starlark.isNullOrNone(sepB)
          ? null
          : makeStarlarkBytes(sepB, thread);
      StarlarkBytes tosplit = StarlarkBytes.of(thread.mutability(), bytes);
      split = tosplit.split(sep, maxsplit, thread);
    }
    return split;
  }

  private StarlarkBytes makeStarlarkBytes(Object sepB, StarlarkThread thread) throws EvalException {
    byte[] bytes;
    if(sepB instanceof String) {
      bytes = ((String) sepB).getBytes();
    } else {
      bytes = (byte[]) sepB;
    }

    return StarlarkBytes.of(thread.mutability(), bytes);
  }

  @Test
  public void testSplit_2() throws EvalException {
    StarlarkList<StarlarkBytes> split = doSplit("hello world !".getBytes(), "lo", Starlark.NONE);
    assertNotNull(split);
    assertEquals(split.size(), 2);
    assertArrayEquals(split.get(0).toByteArray(), "hel".getBytes());
    assertArrayEquals(split.get(1).toByteArray(), " world !".getBytes());
  }
  @Test
  public void testSplit_3() throws EvalException {
    StarlarkList<StarlarkBytes> split = doSplit(
        "hello world !\n\nfoo xxxx\tbrah".getBytes(),
        Starlark.NONE,
        Starlark.NONE
    );
//    In [10]: b"hello world !\n\nfoo xxxx\tbrah".split()
//    Out[10]: [b'hello', b'world', b'!', b'foo', b'xxxx', b'brah']
    assertNotNull(split);
    assertEquals(split.size(), 6);
    assertArrayEquals(split.get(0).toByteArray(), "hello".getBytes());
    assertArrayEquals(split.get(1).toByteArray(), "world".getBytes());
    assertArrayEquals(split.get(2).toByteArray(), "!".getBytes());
    assertArrayEquals(split.get(3).toByteArray(), "foo".getBytes());
    assertArrayEquals(split.get(4).toByteArray(), "xxxx".getBytes());
    assertArrayEquals(split.get(5).toByteArray(), "brah".getBytes());
  }
}