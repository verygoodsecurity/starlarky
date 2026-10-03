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

package com.verygood.security.larky.wasm.conformance;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.parser.LarkyEvaluator;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.wasm.WasmRuntimes;
import java.util.List;
import java.util.Map;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Larky scripts calling WebAssembly shipped with them (as a service passes files through
 * {@link LarkyScriptEngine#MODULES}), on each runtime.
 */
@RunWith(Parameterized.class)
public class LarkyWasmEndToEndTest {

  @Parameters(name = "{0}")
  public static List<Object[]> runtimes() {
    return List.of(new Object[] {"endive"}, new Object[] {"graal"});
  }

  private final String runtime;

  public LarkyWasmEndToEndTest(String runtime) {
    this.runtime = runtime;
  }

  @Before
  public void selectRuntime() {
    System.setProperty(WasmRuntimes.PROPERTY, runtime);
  }

  @After
  public void clearRuntime() {
    System.clearProperty(WasmRuntimes.PROPERTY);
  }

  private static ParsedStarFile eval(String script, Map<String, ?> files, SimpleBindings bindings)
      throws ScriptException {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    SimpleScriptContext context = new SimpleScriptContext();
    context.setAttribute(LarkyScriptEngine.MODULES, files, ScriptContext.ENGINE_SCOPE);
    context.setAttribute(LarkyScriptEngine.SCRIPT_NAME, "main.star", ScriptContext.ENGINE_SCOPE);
    engine.setContext(context);
    return (ParsedStarFile) engine.eval(script, bindings);
  }

  @Test
  public void scriptCallsJavaScriptCompiledWithJavy() throws Exception {
    String script =
        String.join(
            "\n",
            "load('@vgs//wasm', 'wasm')",
            "out = wasm.module('vendor/encrypt.wasm').call({'pan': '4111111111111111', 'key': 'k1'})",
            "encrypted = out['encrypted']",
            "key_id = out['keyId']",
            "");
    ParsedStarFile result =
        eval(script, Map.of("vendor/encrypt.wasm", Fixtures.wasm("js/sample_encrypt")), new SimpleBindings());
    assertThat(result.getGlobalEnvironmentVariable("encrypted", String.class))
        .isEqualTo("5317929663681111");
    assertThat(result.getGlobalEnvironmentVariable("key_id", String.class)).isEqualTo("983d80c1");
  }

  @Test
  public void scriptRunsAModuleOnBytes() throws Exception {
    String script =
        String.join(
            "\n",
            "load('@vgs//wasm', 'wasm')",
            "out = wasm.module('echo.wasm').run(b'hello')",
            "same = out == b'hello'",
            "");
    ParsedStarFile result = eval(script, Map.of("echo.wasm", Fixtures.wasm("echo")), new SimpleBindings());
    assertThat(result.getGlobalEnvironmentVariable("same", Boolean.class)).isTrue();
  }

  @Test
  public void dumpsAndLoadsRoundTripAModule() throws Exception {
    String script =
        String.join(
            "\n",
            "load('@vgs//wasm', 'wasm')",
            "data = wasm.dumps(wasm.module('echo.wasm'))",
            "same = wasm.loads(data).run(b'again') == b'again'",
            "");
    ParsedStarFile result =
        eval(script, Map.of("echo.wasm", Fixtures.wasm("echo")), new SimpleBindings());
    assertThat(result.getGlobalEnvironmentVariable("same", Boolean.class)).isTrue();
  }

  @Test
  public void nonzeroExitIsAScriptErrorWithStderr() {
    String script =
        String.join("\n", "load('@vgs//wasm', 'wasm')", "wasm.module('x.wasm').run(b'')", "");
    ScriptException e =
        assertThrows(
            ScriptException.class,
            () -> eval(script, Map.of("x.wasm", Fixtures.wasm("exit3")), new SimpleBindings()));
    assertThat(e.getMessage()).contains("wasm module 'x.wasm' exited with code 3: boom");
  }

  @Test
  public void evaluationDeadlineStopsAModule() {
    String script =
        String.join("\n", "load('@vgs//wasm', 'wasm')", "wasm.module('spin.wasm').run(b'')", "");
    SimpleBindings bindings = new SimpleBindings();
    bindings.put(LarkyEvaluator.EXPIRATION_MS, System.currentTimeMillis() + 300);
    long start = System.nanoTime();
    ScriptException e =
        assertThrows(
            ScriptException.class,
            () -> eval(script, Map.of("spin.wasm", Fixtures.wasm("spin")), bindings));
    assertThat(e.getMessage()).contains("Starlark computation cancelled: past expiration date");
    assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(3000L);
  }
}
