package com.verygood.security.larky.modules;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.junit.After;
import org.junit.Test;

/** zlib's output limit, and errors from a bounded loop inside a Result. */
public class ZLibOutputLimitTest {

  @After
  public void clearProperty() {
    System.clearProperty(ZLibModule.MAX_OUTPUT_BYTES_PROPERTY);
  }

  private static Object eval(String body) throws ScriptException {
    String script =
        "load('@stdlib//zlib', 'zlib')\n"
            + "load('@stdlib//larky', 'larky')\n"
            + "load('@vendor//option/result', 'Result')\n"
            + "def run():\n"
            + "    "
            + body
            + "\n"
            + "out = run()\n";
    return new LarkyScriptEngine().eval(script, new SimpleBindings());
  }

  private static String decompressLength(int mib) {
    return "return len(zlib.decompress(zlib.compress(b'\\x00' * (" + mib + " << 20))))";
  }

  @Test
  public void decompressesMoreThanTheOldLoopBound() throws Exception {
    // 32 MiB: with 1 KiB chunks this ran past larky.while_true()'s 16,384 iterations.
    eval(decompressLength(32));
  }

  @Test
  public void defaultLimitIs64MiB() {
    assertThat(ZLibModule.maxOutputBytes()).isEqualTo(64 << 20);
  }

  @Test
  public void outputOverTheLimitFailsClearly() {
    System.setProperty(ZLibModule.MAX_OUTPUT_BYTES_PROPERTY, String.valueOf(1 << 20));
    ScriptException e = assertThrows(ScriptException.class, () -> eval(decompressLength(2)));
    assertThat(e).hasMessageThat().contains("decompressed data is larger than 1048576 bytes");
    assertThat(e).hasMessageThat().doesNotContain("NullPointerException");
  }

  @Test
  public void outputAtTheLimitIsAllowed() throws Exception {
    System.setProperty(ZLibModule.MAX_OUTPUT_BYTES_PROPERTY, String.valueOf(1 << 20));
    eval(decompressLength(1));
  }

  @Test
  public void negativeOrMalformedLimitsMeanTheDefault() {
    for (String value : new String[] {"-1", "lots", ""}) {
      System.setProperty(ZLibModule.MAX_OUTPUT_BYTES_PROPERTY, value);
      assertThat(ZLibModule.maxOutputBytes()).isEqualTo(64 << 20);
    }
  }

  @Test
  public void aBoundedLoopsLimitKeepsItsMessageInsideAResult() {
    // Error.withThread used to throw a NullPointerException here: the loop's exception has no cause.
    ScriptException e =
        assertThrows(
            ScriptException.class,
            () ->
                eval(
                    "def loop():\n"
                        + "        for _ in larky.while_true(bound=2):\n"
                        + "            pass\n"
                        + "    return Result.try_(loop).finally_(lambda rval: rval).build().unwrap()"));
    assertThat(e).hasMessageThat().contains("Iteration limit exceeded");
    assertThat(e).hasMessageThat().doesNotContain("NullPointerException");
  }
}
