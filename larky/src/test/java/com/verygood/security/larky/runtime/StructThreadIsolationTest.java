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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.modules.types.LarkyObject;
import com.verygood.security.larky.modules.types.Property;
import com.verygood.security.larky.modules.types.structs.SimpleStruct;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.util.ArrayList;
import java.util.List;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import org.junit.Test;

/**
 * A struct created by an evaluation must not run its callbacks (property getters, {@code
 * __getattr__}, {@code __str__}, {@code __eq__}, {@code __hash__}) on that evaluation's thread
 * once it has finished: without an active evaluation they fail closed, and {@link StarlarkInterop}
 * runs them on a fresh thread.
 */
public class StructThreadIsolationTest {

  private static final String SCRIPT =
      String.join(
          "\n",
          "load('@stdlib//larky', 'larky')",
          "def _body():",
          "    print('getter ran')",
          "    return [1, 2]",
          "def _str():",
          "    return 'struct-as-string'",
          "def _getattr(name):",
          "    return 'attr:' + name",
          "def _spin():",
          "    for i in range(10000000):",
          "        pass",
          "    return 1",
          "def _eq(other):",
          "    return True",
          "def _hash():",
          "    return 7",
          "S = larky.mutablestruct(",
          "    body = larky.property(_body),",
          "    spin = larky.property(_spin),",
          "    headers = {'a': 'b'},",
          "    __str__ = _str,",
          "    __getattr__ = _getattr)",
          "A = larky.struct(__eq__ = _eq, __hash__ = _hash)",
          "B = larky.struct(__eq__ = _eq, __hash__ = _hash)",
          "D = {A: 1}",
          "");

  private static Object global(ParsedStarFile parsed, String name) {
    return parsed.getGlobalEnvironmentVariable(name, Object.class);
  }

  @Test
  public void afterTheEvaluationValuesFailClosedWithoutTheHelper() throws Exception {
    ParsedStarFile parsed = StarlarkInteropTest.evaluate(SCRIPT, null);
    LarkyObject s = (LarkyObject) global(parsed, "S");

    IllegalStateException property =
        assertThrows(IllegalStateException.class, () -> s.getField("body", null));
    assertThat(property).hasMessageThat().contains("no Starlark evaluation is active");
    // a __getattr__ is not run on the finished evaluation's thread either
    assertThrows(IllegalStateException.class, () -> s.getField("missing", null));
    assertThrows(
        IllegalStateException.class, () -> Starlark.str(s, parsed.interop().semantics()));
    // plain fields need no thread
    assertThat(s.getField("headers", null)).isInstanceOf(Dict.class);

    // __eq__ and __hash__ (here through a dict lookup) have no thread parameter either
    Dict<Object, Object> d = Dict.cast(global(parsed, "D"), Object.class, Object.class, "D");
    Object b = global(parsed, "B");
    assertThrows(IllegalStateException.class, () -> d.get(b));
    assertThrows(IllegalStateException.class, () -> b.hashCode());
  }

  @Test
  public void throughTheHelperTheyRunOnAFreshThread() throws Exception {
    ParsedStarFile parsed = StarlarkInteropTest.evaluate(SCRIPT, null);
    StarlarkInterop host = parsed.interop();
    Object s = global(parsed, "S");

    assertThat(host.str(s)).isEqualTo("struct-as-string");
    assertThat(host.getField(s, "missing")).isEqualTo("attr:missing");
    assertThat(host.getField(s, "body").toString()).isEqualTo("[1, 2]");
    assertThat(host.getField(s, "headers")).isInstanceOf(Dict.class);

    Dict<Object, Object> d = Dict.cast(global(parsed, "D"), Object.class, Object.class, "D");
    assertThat(host.callMethod(d, "get", global(parsed, "B"))).isEqualTo(StarlarkInt.of(1));
  }

  @Test
  public void hostCallbacksPrintToTheHelperNotTheFinishedEvaluation() throws Exception {
    ParsedStarFile parsed = StarlarkInteropTest.evaluate(SCRIPT, null);
    Object s = global(parsed, "S");
    List<String> printed = new ArrayList<>();
    parsed.interop().withPrintHandler((t, m) -> printed.add(m)).getField(s, "body");
    assertThat(printed).containsExactly("getter ran");
  }

  @Test
  public void aLoopingGetterStopsAtTheEvaluationsStepLimit() throws Exception {
    ParsedStarFile parsed = StarlarkInteropTest.evaluate(SCRIPT, 2_000_000L);
    Object s = global(parsed, "S");
    // an EvalException, not the RuntimeException the struct wraps it in
    EvalException e =
        assertThrows(EvalException.class, () -> parsed.interop().getField(s, "spin"));
    assertThat(e).hasMessageThat().contains("too many steps");
  }

  @Test
  public void aReusedEngineCarriesStructDictKeysIntoTheNextEval() throws Exception {
    // the engine keeps each eval's globals as bindings; the next eval converts them, which copies
    // D and so hashes its struct key (a __hash__ in Starlark) outside any evaluation
    javax.script.ScriptEngine engine =
        new com.verygood.security.larky.jsr223.LarkyScriptEngineFactory().getScriptEngine();
    engine.eval(
        String.join(
            "\n",
            "load('@stdlib//larky', 'larky')",
            "def _hash():",
            "    return 7",
            "def _eq(other):",
            "    return True",
            "K = larky.struct(__hash__ = _hash, __eq__ = _eq)",
            "D = {K: [1, 2]}",
            ""));
    engine.eval("n = len(D)\n");
    assertThat(engine.get("n")).isEqualTo(StarlarkInt.of(1));
  }

  @Test
  public void propertySetChecksItsArgumentsWithoutShowingJava() throws Exception {
    String script =
        String.join(
            "\n",
            "load('@stdlib//larky', 'larky')",
            "seen = []",
            "p = larky.property(lambda: 41, lambda v: seen.append(v))",
            "p.set(5)",
            "got = seen[0]",
            "");
    ParsedStarFile parsed = StarlarkInteropTest.evaluate(script, null);
    assertThat(global(parsed, "got")).isEqualTo(StarlarkInt.of(5));

    // LarkyScript reports a failed script as RuntimeEvalException, with the EvalException as cause
    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () ->
                StarlarkInteropTest.evaluate(
                    "load('@stdlib//larky', 'larky')\n"
                        + "p = larky.property(lambda: 41, lambda v: None)\n"
                        + "p.set()\n",
                    null));
    assertThat(e.getCause()).isInstanceOf(EvalException.class);
    assertThat(e.getCause())
        .hasMessageThat()
        .isEqualTo("set() missing 1 required positional argument: val");
  }

  /** A callable that records the threads it ran on, and the current evaluation then. */
  private static final class Probe implements StarlarkCallable {
    final List<StarlarkThread> threads = new ArrayList<>();
    final List<StarlarkThread> current = new ArrayList<>();

    @Override
    public String getName() {
      return "probe";
    }

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) {
      threads.add(thread);
      current.add(CurrentEvaluation.currentOrNull());
      return "probed";
    }
  }

  @Test
  public void callbacksDoNotSeeTheThreadThatCreatedTheStruct() throws Exception {
    Probe probe = new Probe();
    StarlarkThread creator;
    SimpleStruct struct;
    try (Mutability mu = Mutability.create("creator")) {
      creator = StarlarkThread.createTransient(mu, StarlarkInterop.create().semantics());
      struct =
          SimpleStruct.mutable(
              Dict.<String, Object>builder()
                  .put("body", Property.builder().fget(probe).build())
                  .put("__getattr__", probe)
                  .put("__str__", probe)
                  .build(mu),
              creator);
    }
    // the creating evaluation is over (its Mutability is closed); the struct fails closed
    assertThrows(IllegalStateException.class, () -> struct.getField("body", null));
    assertThat(probe.threads).isEmpty();

    StarlarkInterop host = StarlarkInterop.create();
    assertThat(host.getField(struct, "body")).isEqualTo("probed");
    assertThat(host.getField(struct, "other")).isEqualTo("probed");
    assertThat(host.str(struct)).isEqualTo("probed");
    assertThat(probe.threads).hasSize(3);
    for (StarlarkThread t : probe.threads) {
      assertThat(t).isNotSameInstanceAs(creator);
    }
    assertThat(probe.threads.get(0)).isNotSameInstanceAs(probe.threads.get(1));
    for (int i = 0; i < probe.threads.size(); i++) {
      assertThat(probe.current.get(i)).isSameInstanceAs(probe.threads.get(i));
    }
  }
}
