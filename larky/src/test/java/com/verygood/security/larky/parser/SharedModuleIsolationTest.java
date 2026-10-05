package com.verygood.security.larky.parser;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

/**
 * A script must not be able to change anything another evaluation can see: values of a module it
 * loaded (frozen once loaded, and shared when modules are cached), or process-wide built-in types.
 */
public class SharedModuleIsolationTest {

  private static final String SHARED =
      String.join(
          "\n",
          "load(\"@stdlib//larky\", \"larky\")",
          "load(\"@stdlib//types\", \"make_class\")",
          "L = [1]",
          "D = {'a': 1}",
          "S = larky.mutablestruct(x = 1)",
          "C = make_class('C')",
          "obj = C()",
          "T = type('T', (), {'a': 1})",
          "tobj = T()",
          "");

  /** Runs {@code script} (which may load "shared"); returns null if it succeeded, else the error. */
  private static String run(String script) {
    ImmutableMap<String, byte[]> files =
        ImmutableMap.of(
            "shared.star", SHARED.getBytes(StandardCharsets.UTF_8),
            "main.star", script.getBytes(StandardCharsets.UTF_8));
    try {
      new LarkyEvaluator(new LarkyScript(LarkyScript.StarlarkMode.STRICT), new TestingConsole())
          .eval(new InMemMapBackedStarFile(files, "main.star"));
      return null;
    } catch (Throwable t) {
      return String.valueOf(t.getMessage());
    }
  }

  private static void assertRejected(String mutation) {
    String script =
        "load(\"shared\", \"L\", \"D\", \"S\", \"C\", \"obj\", \"T\", \"tobj\")\n" + mutation + "\n";
    assertWithMessage("%s should fail", mutation).that(run(script)).isNotNull();
  }

  @Test
  public void valuesOfALoadedModuleAreFrozen() {
    assertRejected("L.append(2)");
    assertRejected("D['b'] = 2");
    assertRejected("S.x = 2");
    assertRejected("C.x = 2");
    assertRejected("obj.x = 2");
    assertRejected("setattr(C, 'y', 2)");
    assertRejected("T.x = 2");
    assertRejected("T.__dict__['z'] = 2");
    assertRejected("tobj.x = 2");
  }

  @Test
  public void builtinTypesCannotBeChanged() {
    for (String type : new String[] {"type", "object"}) {
      assertWithMessage(type).that(run(type + ".leak = 1\n")).contains("built-in type");
      assertWithMessage("a later evaluation must not see %s.leak", type)
          .that(run("fail('saw leak') if hasattr(" + type + ", 'leak') else None\n"))
          .isNull();
    }
  }

  @Test
  public void aScriptCanStillChangeTheClassesItDefines() {
    // Until its module is frozen, a type defined by a script is mutable, as before.
    assert run("T2 = type('T2', (), {})\nT2.x = 1\nfail('x') if T2.x != 1 else None\n") == null;
  }
}
