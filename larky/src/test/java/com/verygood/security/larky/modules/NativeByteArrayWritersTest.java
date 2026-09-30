package com.verygood.security.larky.modules;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.modules.crypto.Cipher.Engine;
import com.verygood.security.larky.modules.crypto.CryptoCipherModule;
import java.util.LinkedHashMap;
import java.util.Map;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkBytes.StarlarkByteArray;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.Tuple;
import org.junit.Test;

/**
 * Native functions that write into a script's bytearray must refuse a frozen bytearray, or one
 * being iterated, with Starlark's usual mutation errors (not a Java UnsupportedOperationException),
 * and must leave its contents unchanged.
 */
public class NativeByteArrayWritersTest {

  private interface Writer {
    void write(StarlarkByteArray buf) throws Exception;
  }

  private static Engine aes() throws EvalException {
    return CryptoCipherModule.INSTANCE.AES(StarlarkBytes.immutableOf(new byte[16]));
  }

  private static Map<String, Writer> writers() {
    StarlarkBytes block = StarlarkBytes.immutableOf(new byte[16]);
    StarlarkBytes iv = StarlarkBytes.immutableOf(new byte[16]);
    Map<String, Writer> w = new LinkedHashMap<>();
    w.put("struct.pack_into", buf -> StructModule.INSTANCE.struct__pack_into(
        "B", buf, StarlarkInt.of(0), Tuple.of(StarlarkInt.of(7))));
    w.put("ECB encrypt", buf -> CryptoCipherModule.INSTANCE.ECBMode(aes()).encrypt(block, buf, null));
    w.put("ECB decrypt", buf -> CryptoCipherModule.INSTANCE.ECBMode(aes()).decrypt(block, buf, null));
    w.put("CTR encrypt",
        buf -> CryptoCipherModule.INSTANCE.CTRMode(aes(), iv).encrypt(block, buf, null));
    w.put("CTR decrypt",
        buf -> CryptoCipherModule.INSTANCE.CTRMode(aes(), iv).decrypt(block, buf, null));
    w.put("zlib inflate", buf -> ZLibModule.INSTANCE.inflater(false)
        .inflate(buf, StarlarkInt.of(0), Starlark.UNBOUND));
    w.put("zlib deflate", buf -> ZLibModule.INSTANCE.deflater(StarlarkInt.of(6), false)
        .deflate(buf, StarlarkInt.of(0)));
    return w;
  }

  private static byte[] contents() {
    byte[] b = new byte[32];
    for (int i = 0; i < b.length; i++) {
      b[i] = (byte) (i + 1);
    }
    return b;
  }

  @Test
  public void frozenBytearrayIsRejected() throws Exception {
    for (Map.Entry<String, Writer> e : writers().entrySet()) {
      Mutability mu = Mutability.create("test");
      StarlarkByteArray buf = StarlarkByteArray.of(mu, contents());
      mu.freeze();
      EvalException ex = assertThrows(e.getKey(), EvalException.class, () -> e.getValue().write(buf));
      assertEquals(e.getKey(), "trying to mutate a frozen bytearray value", ex.getMessage());
      assertArrayEquals(e.getKey(), contents(), buf.toByteArray());
    }
  }

  @Test
  public void bytearrayBeingIteratedIsRejected() throws Exception {
    for (Map.Entry<String, Writer> e : writers().entrySet()) {
      StarlarkByteArray buf = StarlarkByteArray.of(Mutability.create("test"), contents());
      buf.updateIteratorCount(+1); // as a for loop over buf does
      EvalException ex = assertThrows(e.getKey(), EvalException.class, () -> e.getValue().write(buf));
      assertEquals(
          e.getKey(),
          "bytearray value is temporarily immutable due to active for-loop iteration",
          ex.getMessage());
      assertArrayEquals(e.getKey(), contents(), buf.toByteArray());

      buf.updateIteratorCount(-1); // the loop ended: writing works again
      e.getValue().write(buf);
    }
  }
}
