/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.runtime;

import static com.google.common.truth.Truth.assertWithMessage;

import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.util.HashMap;
import java.util.Map;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.Test;

/**
 * {@code +} and {@code *} on a frozen bytearray (here, one a loaded module keeps) give a new
 * bytearray that the evaluation computing it owns, as in Python: it can be changed, and the operand
 * stays as it was.
 */
public class FrozenBytearrayOperandTest {

  private static String eval(String expression, ModuleCache cache) throws Exception {
    String script =
        String.join(
            "\n",
            "load(\"@vendor//_isolation_fixtures/frozen_bytearray\", \"BA\")",
            "x = " + expression,
            "x.append(100) if type(x) == \"bytearray\" else None",
            "result = (type(x), x, BA)",
            "");
    ParsedStarFile r =
        new CachedTestScript(
                ModuleSupplier.CORE_MODULES,
                LarkyScript.StarlarkMode.STRICT,
                Map.of("ctx", new HashMap<String, Object>()))
            .withModuleCache(cache)
            .evaluate(InMemMapBackedStarFile.createStarFile("main.star", script), new TestingConsole());
    return Starlark.str(r.getGlobalEnvironmentVariable("result", Object.class), null);
  }

  @Test
  void operatorsGiveAnEvaluationOwnedBytearray() throws Exception {
    Map<String, String> expected =
        Map.of(
            "BA + b'c'", "(\"bytearray\", bytearray(b\"abcd\"), bytearray(b\"ab\"))",
            "BA * 2", "(\"bytearray\", bytearray(b\"ababd\"), bytearray(b\"ab\"))",
            "2 * BA", "(\"bytearray\", bytearray(b\"ababd\"), bytearray(b\"ab\"))",
            "BA + BA", "(\"bytearray\", bytearray(b\"ababd\"), bytearray(b\"ab\"))",
            "b'c' + BA", "(\"bytes\", b\"cab\", bytearray(b\"ab\"))");
    for (boolean cached : new boolean[] {false, true}) {
      ModuleCache cache = new ModuleCache(cached);
      for (int run = 0; run < 2; run++) { // the second run takes the module from the cache
        for (Map.Entry<String, String> e : expected.entrySet()) {
          assertWithMessage("%s (cache %s, run %s)", e.getKey(), cached, run)
              .that(eval(e.getKey(), cache))
              .isEqualTo(e.getValue());
        }
      }
    }
  }
}
