package com.verygood.security.larky.modules.crypto;

import com.google.common.truth.Truth;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.SyntaxError;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CryptoCipherModuleTest {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testModule() throws SyntaxError.Exception, EvalException, InterruptedException {
    Module module = Module.create();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
      Starlark.execFile(ParserInput.fromLines("True = 123"), FileOptions.DEFAULT, module, thread);
    }
    Truth.assertThat(module.getGlobal("True")).isEqualTo(StarlarkInt.of(123));
  }

  @Test
  public void testDES3() throws Exception {
    //CryptoCipherModule.Engine larkyCipher = CryptoCipherModule.INSTANCE.DES3();
    //assertNotNull(larkyCipher);
  }
}