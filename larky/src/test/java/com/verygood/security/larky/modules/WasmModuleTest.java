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

package com.verygood.security.larky.modules;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.wasm.WasmLimits;
import com.verygood.security.larky.wasm.WasmRuntimes;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests of {@code @vgs//wasm} ({@link WasmModule}) on {@link FakeWasmRuntime}. */
public class WasmModuleTest {

  // A fake module that is not valid UTF-8, so nothing may parse it as Starlark.
  private static final byte[] MODULE = {'F', 'A', 'K', 'E', 0, (byte) 0xff, (byte) 0xfe, 1};

  private static final String LOAD = "load('@vgs//wasm', 'wasm')\n";

  @Before
  public void useFakeRuntime() {
    System.setProperty(WasmRuntimes.PROPERTY, FakeWasmRuntime.NAME);
  }

  @After
  public void clearProperties() {
    System.clearProperty(WasmRuntimes.PROPERTY);
    System.clearProperty(WasmModule.MAX_MEMORY_PROPERTY);
    System.clearProperty(WasmModule.MAX_OUTPUT_PROPERTY);
    System.clearProperty(WasmModule.RANDOM_SEED_PROPERTY);
  }

  private static ParsedStarFile eval(String script, Map<String, Object> files, Long expirationMs)
      throws ScriptException {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    SimpleScriptContext context = new SimpleScriptContext();
    context.setAttribute(LarkyScriptEngine.MODULES, files, ScriptContext.ENGINE_SCOPE);
    context.setAttribute(LarkyScriptEngine.SCRIPT_NAME, "host.star", ScriptContext.ENGINE_SCOPE);
    engine.setContext(context);
    SimpleBindings bindings = new SimpleBindings();
    if (expirationMs != null) {
      bindings.put("LARKY_EVAL_EXPIRATION_MS", expirationMs);
    }
    return (ParsedStarFile) engine.eval(LOAD + script, bindings);
  }

  private static ParsedStarFile eval(String script) throws ScriptException {
    return eval(script, Map.of("chase/encrypt.wasm", MODULE), null);
  }

  private static String error(String script) {
    return assertThrows(ScriptException.class, () -> eval(script)).getMessage();
  }

  private static <T> T out(ParsedStarFile result, Class<T> type) {
    return result.getGlobalEnvironmentVariable("out", type);
  }

  @Test
  public void runReturnsStdoutAsBytes() throws Exception {
    ParsedStarFile result =
        eval("m = wasm.module('chase/encrypt.wasm')\nout = m.run(b'raw stdin')\nname = m.name");
    assertThat(out(result, StarlarkBytes.class).toByteArray()).isEqualTo("raw stdin".getBytes(UTF_8));
    assertThat(result.getGlobalEnvironmentVariable("name", String.class))
        .isEqualTo("chase/encrypt.wasm");
  }

  @Test
  public void runTakesAStringAsUtf8() throws Exception {
    ParsedStarFile result = eval("out = wasm.module('chase/encrypt.wasm').run('hé')");
    assertThat(out(result, StarlarkBytes.class).toByteArray()).isEqualTo("hé".getBytes(UTF_8));
  }

  @Test
  public void callEncodesAndDecodesJson() throws Exception {
    ParsedStarFile result =
        eval(
            "out = wasm.module('chase/encrypt.wasm').call("
                + "{'pan': '4111', 'n': 3, 'ok': True, 'l': [1, None]})");
    Dict<?, ?> out = out(result, Dict.class);
    assertThat(out.get("pan")).isEqualTo("4111");
    assertThat(out.get("n")).isEqualTo(StarlarkInt.of(3));
    assertThat(out.get("ok")).isEqualTo(true);
    assertThat(((StarlarkList<?>) out.get("l")).size()).isEqualTo(2);
  }

  @Test
  public void loadsMakesAnInlineModule() throws Exception {
    ParsedStarFile result =
        eval("m = wasm.loads(b'FAKE')\nout = m.run(b'x')\nname = m.name\nr = repr(m)");
    assertThat(out(result, StarlarkBytes.class).toByteArray()).isEqualTo(new byte[] {'x'});
    assertThat(result.getGlobalEnvironmentVariable("name", String.class)).isEqualTo("<inline>");
    assertThat(result.getGlobalEnvironmentVariable("r", String.class))
        .isEqualTo("<wasm module '<inline>'>");
  }

  @Test
  public void dumpsReturnsTheBytesLoadsAccepts() throws Exception {
    Map<String, Object> files = new LinkedHashMap<>();
    files.put("a.wasm", MODULE);
    ParsedStarFile result =
        eval(
            "data = wasm.dumps(wasm.module('a.wasm'))\n"
                + "out = [data, wasm.dumps(wasm.loads(data)) == data]",
            files,
            null);
    StarlarkList<?> out = out(result, StarlarkList.class);
    assertThat(((StarlarkBytes) out.get(0)).toByteArray()).isEqualTo(MODULE);
    assertThat(out.get(1)).isEqualTo(true);
  }

  @Test
  public void loadsTakesBytesOnly() {
    assertThat(error("wasm.loads('FAKE')")).contains("loads");
    // `load` is a keyword, so the module has no load method; loads/dumps follow pickle and json.
    assertThat(error("getattr(wasm, 'load')")).contains("has no field or method 'load'");
  }

  @Test
  public void moduleReadsByteAndStringFilesShippedWithTheScript() throws Exception {
    Map<String, Object> files = new LinkedHashMap<>();
    files.put("a.wasm", MODULE);
    files.put("b.wasm", "FAKE text module");
    files.put("lib.star", "def go(wasm):\n    return wasm.module('a.wasm').run(b'from lib')\n");
    ParsedStarFile result =
        eval(
            "load('lib', 'go')\n"
                + "out = [wasm.module('b.wasm').run(b'b'), go(wasm)]",
            files,
            null);
    StarlarkList<?> out = out(result, StarlarkList.class);
    assertThat(((StarlarkBytes) out.get(0)).toByteArray()).isEqualTo(new byte[] {'b'});
    assertThat(((StarlarkBytes) out.get(1)).toByteArray()).isEqualTo("from lib".getBytes(UTF_8));
  }

  @Test
  public void moduleInALoadedFileReadsTheSameFiles() throws Exception {
    Map<String, Object> files = new LinkedHashMap<>();
    files.put("a.wasm", MODULE);
    files.put("lib.star", "load('@vgs//wasm', 'wasm')\nM = wasm.module('a.wasm')\n");
    ParsedStarFile result = eval("load('lib', 'M')\nout = M.run(b'z')", files, null);
    assertThat(out(result, StarlarkBytes.class).toByteArray()).isEqualTo(new byte[] {'z'});
  }

  @Test
  public void missingFile() {
    assertThat(error("wasm.module('nope.wasm')")).contains("wasm.module: no file named 'nope.wasm'");
  }

  @Test
  public void moduleDoesNotReadClasspathResources() {
    // A file on Larky's class path, but not one shipped with the script.
    assertThat(error("wasm.module('stdlib/json.star')"))
        .contains("wasm.module: no file named 'stdlib/json.star'");
  }

  @Test
  public void invalidModule() {
    Map<String, Object> files = Map.of("bad.wasm", new byte[] {0, 'a', 's', 'm'});
    ScriptException e =
        assertThrows(ScriptException.class, () -> eval("wasm.module('bad.wasm')", files, null));
    assertThat(e.getMessage()).contains("wasm module 'bad.wasm' is not a valid WASI module");
    assertThat(e.getMessage()).doesNotContain("fake");
    assertThat(error("wasm.loads(b'nope')"))
        .contains("wasm module '<inline>' is not a valid WASI module");
  }

  @Test
  public void unknownRuntime() {
    System.setProperty(WasmRuntimes.PROPERTY, "nonesuch");
    assertThat(error("wasm.loads(b'FAKE')"))
        .contains("no WebAssembly runtime named 'nonesuch'");
  }

  @Test
  public void nonzeroExit() {
    assertThat(error("wasm.module('chase/encrypt.wasm').run(b'exit:7:bad pan')"))
        .contains("wasm module 'chase/encrypt.wasm' exited with code 7: bad pan");
  }

  @Test
  public void nonzeroExitShowsTheFirstKibOfStderr() {
    String message = error("wasm.loads(b'FAKE').run(b'bigerr')");
    assertThat(message).contains("wasm module '<inline>' exited with code 3: " + "e".repeat(1024));
    assertThat(message).doesNotContain("e".repeat(1025));
  }

  @Test
  public void trap() {
    String message = error("wasm.loads(b'FAKE').run(b'trap')");
    assertThat(message).contains("wasm module '<inline>' trapped");
    assertThat(message).doesNotContain("fake");
  }

  @Test
  public void memoryLimit() {
    assertThat(error("wasm.loads(b'FAKE').run(b'oom')"))
        .contains(
            "wasm module '<inline>' needs more memory than the "
                + WasmLimits.DEFAULT_MAX_MEMORY_BYTES
                + " bytes allowed");
    System.setProperty(WasmModule.MAX_MEMORY_PROPERTY, "131072");
    assertThat(error("wasm.loads(b'FAKE').run(b'oom')"))
        .contains("needs more memory than the 131072 bytes allowed");
  }

  @Test
  public void outputLimit() {
    assertThat(error("wasm.loads(b'FAKE').run(b'flood')"))
        .contains("wasm module '<inline>' wrote more than 1048576 bytes");
    System.setProperty(WasmModule.MAX_OUTPUT_PROPERTY, "10");
    assertThat(error("wasm.loads(b'FAKE').run(b'flood')"))
        .contains("wasm module '<inline>' wrote more than 10 bytes");
  }

  @Test
  public void limitsComeFromSystemProperties() throws Exception {
    System.setProperty(WasmModule.MAX_MEMORY_PROPERTY, "65536");
    System.setProperty(WasmModule.MAX_OUTPUT_PROPERTY, "99");
    System.setProperty(WasmModule.RANDOM_SEED_PROPERTY, "42");
    ParsedStarFile result = eval("out = wasm.loads(b'FAKE').run(b'limits')");
    assertThat(new String(out(result, StarlarkBytes.class).toByteArray(), UTF_8))
        .isEqualTo("65536 99 42");
  }

  @Test
  public void timeoutRaisesTheExpirationError() {
    long start = System.currentTimeMillis();
    ScriptException wasm =
        assertThrows(
            ScriptException.class,
            () -> eval("wasm.loads(b'FAKE').run(b'sleep')", Map.of(), start + 200));
    ScriptException starlark =
        assertThrows(
            ScriptException.class,
            () ->
                eval(
                    "def f():\n    for i in range(1000000000):\n        pass\nf()",
                    Map.of(),
                    System.currentTimeMillis() + 200));
    String expected = "Starlark computation cancelled: past expiration date";
    assertThat(starlark.getMessage()).contains(expected);
    assertThat(wasm.getMessage()).contains(expected);
    assertThat(wasm.getMessage()).doesNotContain("fake");
  }

  @Test
  public void safeCannotCatchTheTimeout() {
    // As with the expiration check, the thread stays expired: the next statement fails too.
    ScriptException e =
        assertThrows(
            ScriptException.class,
            () ->
                eval(
                    "load('@vendor//option/result', safe='safe')\n"
                        + "r = safe(lambda: wasm.loads(b'FAKE').run(b'sleep'))()\n"
                        + "out = [x for x in range(1000)]",
                    Map.of(),
                    System.currentTimeMillis() + 200));
    assertThat(e.getMessage()).contains("Starlark computation cancelled: past expiration date");
  }

  @Test
  public void programsAreCachedByContent() throws Exception {
    WasmModule.clearProgramCache();
    int before = FakeWasmRuntime.COMPILES.get();
    eval(
        "a = wasm.module('chase/encrypt.wasm')\n"
            + "b = wasm.module('chase/encrypt.wasm')\n"
            + "c = wasm.loads(b'FAKE\\x00\\xff\\xfe\\x01')\n"
            + "out = [a.run(b'1'), b.run(b'2'), c.run(b'3')]");
    assertThat(FakeWasmRuntime.COMPILES.get() - before).isEqualTo(1);
    eval("wasm.loads(b'FAKE another')");
    assertThat(FakeWasmRuntime.COMPILES.get() - before).isEqualTo(2);
  }

  @Test
  public void loadsOnlyFromTheVgsNamespace() {
    ScriptException e =
        assertThrows(
            ScriptException.class,
            () -> new LarkyScriptEngine().eval("load('@stdlib//wasm', 'wasm')\n", new SimpleBindings()));
    assertThat(e.getMessage())
        .contains("cannot load '@stdlib//wasm': wasm is a @vgs module; load it as '@vgs//wasm'");
  }

  @Test
  public void loadIsAKeywordAfterADot() {
    assertThat(error("wasm.load(b'FAKE')")).contains("expected identifier after dot");
  }

  @Test
  public void interruptionPropagatesUnwrapped() throws Exception {
    WasmModule.LoadedWasmModule m =
        WasmModule.INSTANCE.loads(StarlarkBytes.immutableOf("FAKE".getBytes(UTF_8)));
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      assertThrows(
          InterruptedException.class,
          () -> m.run(StarlarkBytes.immutableOf("interrupt".getBytes(UTF_8)), thread));
    }
  }
}
