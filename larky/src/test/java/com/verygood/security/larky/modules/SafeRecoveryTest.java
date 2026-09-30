package com.verygood.security.larky.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.junit.Test;

/** safe() recovers from a script's errors, but not from cancellation, and leaves no frames. */
public class SafeRecoveryTest {

  private static Object eval(String script, SimpleBindings bindings) throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    engine.setContext(new SimpleScriptContext());
    engine.eval(script, bindings);
    return bindings.get("OUT");
  }

  @Test
  public void aFunctionCanBeCalledAgainAfterSafeCaughtItsArgumentError() throws Exception {
    // An argument error leaves the callee's frame pushed; the next call used to fail with
    // "function 'dump' called recursively".
    Object out =
        eval(
            String.join(
                "\n",
                "load('@vendor//option/result', safe='safe')",
                "load('@stdlib//json', 'json')",
                "def dump(x):",
                "    r = safe(lambda: json.dumps(x, sort_keys=True))()",
                "    return r.unwrap() if r.is_ok else json.dumps(x)",
                "OUT = str([dump({'a': 1}), dump({'b': 2})])"),
            new SimpleBindings());
    assertEquals("[\"{\\\"a\\\":1}\", \"{\\\"b\\\":2}\"]", out);
  }

  @Test
  public void safeCannotOutliveTheExpirationDate() {
    // The expiry check reads the clock every 64 checks; with ~30+ checks inside each safe() call,
    // the check that saw the deadline was always inside safe(), which swallowed it, and the loop
    // ran to completion (seconds past a 300ms deadline).
    String script =
        String.join(
            "\n",
            "load('@vendor//option/result', safe='safe')",
            "def inner():",
            "    x = 0",
            "    for i in range(100):",
            "        x += i",
            "    return x",
            "def main():",
            "    for _ in range(100000000):",
            "        safe(inner)()",
            "OUT = main()");
    SimpleBindings bindings = new SimpleBindings();
    long start = System.currentTimeMillis();
    bindings.put("LARKY_EVAL_EXPIRATION_MS", start + 300);
    ScriptException e = assertThrows(ScriptException.class, () -> eval(script, bindings));
    assertTrue(e.getMessage(), e.getMessage().contains("past expiration date"));
    long elapsed = System.currentTimeMillis() - start;
    assertTrue("ran " + elapsed + "ms past a 300ms deadline", elapsed < 5_000);
  }
}
