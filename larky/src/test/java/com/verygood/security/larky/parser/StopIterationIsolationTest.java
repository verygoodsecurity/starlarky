/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.verygood.security.larky.parser;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.modules.types.results.LarkyStopIteration;
import com.verygood.security.larky.runtime.LarkyEvaluator;
import java.nio.charset.StandardCharsets;
import net.starlark.java.eval.EvalException;
import org.junit.Test;

/** An uncaught StopIteration must report its own evaluation's location and stack, not another's. */
public class StopIterationIsolationTest {

  private static EvalException raise(String script) {
    ImmutableMap<String, byte[]> files =
        ImmutableMap.of("main.star", script.getBytes(StandardCharsets.UTF_8));
    try {
      new LarkyEvaluator(new LarkyScript(LarkyScript.StarlarkMode.STRICT), new TestingConsole())
          .eval(new InMemMapBackedStarFile(files, "main.star"));
    } catch (Throwable t) {
      for (Throwable c = t; c != null; c = c.getCause()) {
        if (c instanceof EvalException && "StopIteration".equals(c.getMessage())) {
          return (EvalException) c;
        }
      }
      throw new AssertionError("no StopIteration in the cause chain of " + t, t);
    }
    fail("expected StopIteration");
    return null;
  }

  @Test
  public void secondEvaluationReportsItsOwnLocationAndStack() {
    EvalException a =
        raise(
            "def tenant_a_secret_function():\n"
                + "    return next(iter([]))\n"
                + "result = tenant_a_secret_function()\n");
    EvalException b =
        raise(
            "x = 1\n"
                + "y = 2\n"
                + "z = 3\n"
                + "\n"
                + "def tenant_b_function():\n"
                + "    w = 1\n"
                + "    return        next(iter([]))\n"
                + "result = tenant_b_function()\n");

    assertThat(a.getMessageWithStack()).contains("tenant_a_secret_function");
    assertThat(a.getMessageWithStack()).contains("line 2, column 16");
    assertThat(b.getMessageWithStack()).contains("tenant_b_function");
    assertThat(b.getMessageWithStack()).doesNotContain("tenant_a_secret_function");
    assertThat(b.getMessageWithStack()).contains("line 7, column 23");
    assertThat(b.getMessage()).isEqualTo("StopIteration");
    assertThat(b).isNotSameInstanceAs(a);
  }

  /** The shared StopIteration value is not an exception, so {@code throw} on it cannot compile. */
  @Test
  public void theSharedStopIterationCannotBeThrown() {
    assertThat(Throwable.class.isAssignableFrom(LarkyStopIteration.Sentinel.class)).isFalse();
  }

  /** A thrown StopIteration and the {@code StopIteration()} value are equal, as before. */
  @Test
  public void thrownAndSharedStopIterationAreEqual() {
    LarkyStopIteration thrown = LarkyStopIteration.newInstance();
    assertThat((Object) thrown).isEqualTo(LarkyStopIteration.sentinel());
    assertThat((Object) LarkyStopIteration.sentinel()).isEqualTo(thrown);
    assertThat(thrown.hashCode()).isEqualTo(LarkyStopIteration.sentinel().hashCode());
    assertThat((Object) LarkyStopIteration.sentinel().getError())
        .isNotSameInstanceAs(LarkyStopIteration.sentinel().getError());
  }
}
