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
import com.verygood.security.larky.modules.types.results.LarkyAttributeError;
import com.verygood.security.larky.modules.types.results.LarkyIndexError;
import com.verygood.security.larky.runtime.LarkyEvaluator;
import java.nio.charset.StandardCharsets;
import net.starlark.java.eval.EvalException;
import org.junit.Test;

/**
 * {@code IndexError()} and {@code AttributeError()} return one shared value. Raising it ({@code
 * unwrap()}) must raise a new exception that reports its own evaluation's location and stack.
 */
public class ErrorValueIsolationTest {

  private static void eval(String script) throws Exception {
    new LarkyEvaluator(new LarkyScript(LarkyScript.StarlarkMode.STRICT), new TestingConsole())
        .eval(
            new InMemMapBackedStarFile(
                ImmutableMap.of("main.star", script.getBytes(StandardCharsets.UTF_8)),
                "main.star"));
  }

  private static EvalException raise(String script, String message) {
    try {
      eval(script);
    } catch (Throwable t) {
      for (Throwable c = t; c != null; c = c.getCause()) {
        if (c instanceof EvalException && message.equals(c.getMessage())) {
          return (EvalException) c;
        }
      }
      throw new AssertionError("no " + message + " in the cause chain of " + t, t);
    }
    fail("expected " + message);
    return null;
  }

  private static void assertSecondEvaluationReportsItsOwnStack(String error) {
    EvalException a =
        raise(
            "def tenant_a_secret_function():\n"
                + "    return " + error + "().unwrap()\n"
                + "tenant_a_secret_function()\n",
            error);
    EvalException b =
        raise(
            "x = 1\n"
                + "\n"
                + "def tenant_b_function():\n"
                + "    return        " + error + "().unwrap()\n"
                + "tenant_b_function()\n",
            error);

    assertThat(a.getMessageWithStack()).contains("tenant_a_secret_function");
    assertThat(b.getMessageWithStack()).contains("tenant_b_function");
    assertThat(b.getMessageWithStack()).doesNotContain("tenant_a_secret_function");
    assertThat(b.getMessageWithStack()).contains("line 4, column");
    assertThat(b).isNotSameInstanceAs(a);
  }

  @Test
  public void indexErrorReportsItsOwnEvaluationsStack() {
    assertSecondEvaluationReportsItsOwnStack("IndexError");
  }

  @Test
  public void attributeErrorReportsItsOwnEvaluationsStack() {
    assertSecondEvaluationReportsItsOwnStack("AttributeError");
  }

  /** The shared values are not exceptions, so {@code throw} on them cannot compile. */
  @Test
  public void theSharedValuesCannotBeThrown() {
    assertThat(Throwable.class.isAssignableFrom(LarkyIndexError.class)).isFalse();
    assertThat(Throwable.class.isAssignableFrom(LarkyAttributeError.class)).isFalse();
  }

  /** What scripts see of these values is unchanged. */
  @Test
  public void scriptsSeeTheSameValues() throws Exception {
    eval(
        String.join(
            "\n",
            "def check(got, want):",
            "    if got != want:",
            "        fail('got %r, want %r' % (got, want))",
            "",
            "def getattr_hook(name):",
            "    return AttributeError()",
            "",
            "def getitem_hook(self, i):",
            "    return i if i < 3 else IndexError()",
            "",
            "def main():",
            "    for e, name in [(IndexError(), 'IndexError'), (AttributeError(), 'AttributeError')]:",
            "        check(type(e), name)",
            "        check(str(e), name)",
            "        check(repr(e), 'Error{' + name + '}')",
            "        check(e.unwrap_err(), e)",
            "        check(e.unwrap_or_else(lambda v: v), e)",
            "        check(e.is_err, True)",
            "        check(bool(e), False)",
            "        check(e.unwrap_or(1), 1)",
            "    check(IndexError() == IndexError(), True)",
            "    check(AttributeError() == AttributeError(), True)",
            "    check(IndexError() == AttributeError(), False)",
            "    # hasattr is False when __getattr__ returns AttributeError().",
            "    obj = type('WithGetattr', (), {})()",
            "    obj.__getattr__ = getattr_hook",
            "    check(hasattr(obj, 'anything'), False)",
            "    # Iterating with __getitem__ stops when it returns IndexError().",
            "    seq = type('WithGetitem', (), {'__getitem__': getitem_hook})()",
            "    check([x for x in iter(seq)], [0, 1, 2])",
            "",
            "main()",
            ""));
  }
}
