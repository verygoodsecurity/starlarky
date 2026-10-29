package com.verygood.security.larky.modules.crypto;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import org.junit.After;
import org.junit.Test;

/** The per-call limit on random bytes and its system property. */
public class CryptoRandomModuleLimitTest {

  @After
  public void clearProperty() {
    System.clearProperty(CryptoRandomModule.MAX_RANDOM_BYTES_PROPERTY);
  }

  private static int urandomLength(int n) throws EvalException {
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      return CryptoRandomModule.INSTANCE.urandom(StarlarkInt.of(n), thread).size();
    }
  }

  @Test
  public void defaultIs64KiB() throws Exception {
    assertThat(CryptoRandomModule.maxRandomBytes()).isEqualTo(65536);
    assertThat(urandomLength(65536)).isEqualTo(65536);
    EvalException e = assertThrows(EvalException.class, () -> urandomLength(65537));
    assertThat(e).hasMessageThat().contains("is limited to 65536 bytes per call");
  }

  @Test
  public void theHostCanChangeIt() throws Exception {
    System.setProperty(CryptoRandomModule.MAX_RANDOM_BYTES_PROPERTY, "16");
    assertThat(urandomLength(16)).isEqualTo(16);
    assertThrows(EvalException.class, () -> urandomLength(17));
    System.setProperty(CryptoRandomModule.MAX_RANDOM_BYTES_PROPERTY, "1048576");
    assertThat(urandomLength(1048576)).isEqualTo(1048576);
  }

  @Test
  public void negativeOrMalformedValuesMeanTheDefault() {
    for (String value : new String[] {"-1", "lots", ""}) {
      System.setProperty(CryptoRandomModule.MAX_RANDOM_BYTES_PROPERTY, value);
      assertThat(CryptoRandomModule.maxRandomBytes()).isEqualTo(65536);
    }
  }
}
