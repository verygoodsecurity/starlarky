package com.verygood.security.larky.modules.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;

import org.junit.Test;

public class CryptoMathModuleTest {

  @Test
  public void toBytes() {
   LarkyByteLike big = null;
    try (Mutability mu = Mutability.create("test")) {
     StarlarkThread thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
      big = CryptoMathModule.INSTANCE.toBytes(StarlarkInt.of(0x1122334455667788L), StarlarkInt.of(0), "big", false, thread);
    } catch (EvalException e) {
     fail(e.getMessageWithStack());
    }
    assertNotNull(big);
    int[] unsignedBytes = big.getUnsignedBytes();
    assertArrayEquals(unsignedBytes, new int[]{0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88});  }
}