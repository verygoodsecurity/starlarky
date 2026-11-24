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

package com.verygood.security.larky.parser;

import static com.google.common.truth.Truth.assertThat;

import com.verygood.security.larky.console.testing.TestingConsole;
import javax.script.SimpleBindings;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class LegacyInterpreterApiTest {

  @Test
  public void legacyEvaluatorExposesTheEvaluationResult() throws Exception {
    LarkyScript script = new LarkyScript(LarkyScript.StarlarkMode.STRICT);
    LarkyEvaluator evaluator = new LarkyEvaluator(script, new TestingConsole());
    LarkyEvaluator.EvaluationResult result =
        evaluator.eval(InMemMapBackedStarFile.createStarFile("main.star", "value = 42"));

    assertThat(result.hasModule()).isTrue();
    assertThat(result.hasOutput()).isTrue();
    assertThat(result.output()).isSameInstanceAs(Starlark.NONE);
    assertThat(result.module().getGlobal("value")).isEqualTo(StarlarkInt.of(42));
    assertThat(result.module().getGlobal(LarkyEvaluator.EXECUTION_STEPS)).isNotNull();
  }

  @Test
  public void legacyInterpreterPassesHostBindingsToRuntime() throws Exception {
    SimpleBindings bindings = new SimpleBindings();
    bindings.put("host_value", 41);
    DefaultLarkyInterpreter interpreter =
        new DefaultLarkyInterpreter(LarkyScript.StarlarkMode.STRICT, bindings);
    ParsedStarFile result =
        interpreter.evaluate(
            InMemMapBackedStarFile.createStarFile("main.star", "value = host_value + 1"),
            new TestingConsole());

    assertThat(result.getGlobalEnvironmentVariable("value", StarlarkInt.class))
        .isEqualTo(StarlarkInt.of(42));
  }
}
