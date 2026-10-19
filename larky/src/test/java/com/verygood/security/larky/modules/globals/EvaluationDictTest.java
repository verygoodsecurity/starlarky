package com.verygood.security.larky.modules.globals;

import static org.junit.Assert.assertEquals;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.junit.Test;

/**
 * larky.evaluation_dict, and jose's jwk.register_key that keeps its keys there: what one script
 * puts there must not be visible to the next script run in the same JVM.
 */
public class EvaluationDictTest {

  private static Object eval(String... lines) throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    engine.setContext(new SimpleScriptContext());
    SimpleBindings bindings = new SimpleBindings();
    engine.eval(String.join("\n", lines), bindings);
    return bindings.get("OUT");
  }

  @Test
  public void oneDictPerNamePerEvaluation() throws Exception {
    Object out =
        eval(
            "load('@stdlib//larky', larky='larky')",
            "def main():",
            "    larky.evaluation_dict('a')['k'] = 1",
            "    return '%s|%s' % (larky.evaluation_dict('a'), larky.evaluation_dict('b'))",
            "OUT = main()");
    assertEquals("{\"k\": 1}|{}", out);
    Object next =
        eval(
            "load('@stdlib//larky', larky='larky')",
            "def main():",
            "    return str(larky.evaluation_dict('a'))",
            "OUT = main()");
    assertEquals("{}", next);
  }

  private static final String[] SUPPORTED = {
    "load('@vendor//jose/jwk', jwk='jwk')",
    "def main():",
    "    return '%s|%s' % (jwk.is_supported('XNI'), jwk.get_key('XNI'))",
    "OUT = main()"
  };

  @Test
  public void registeredKeysDoNotLeakIntoTheNextEvaluation() throws Exception {
    assertEquals("False|None", eval(SUPPORTED));
    Object registered =
        eval(
            "load('@vendor//jose/jwk', jwk='jwk')",
            "def XNIKey(key_data, algorithm):",
            "    return key_data",
            "def main():",
            "    jwk.register_key('XNI', XNIKey)",
            "    return '%s|%s' % (jwk.is_supported('XNI'), jwk.construct(b'k', 'XNI'))",
            "OUT = main()");
    assertEquals("True|k", registered);
    assertEquals("False|None", eval(SUPPORTED));
  }
}
