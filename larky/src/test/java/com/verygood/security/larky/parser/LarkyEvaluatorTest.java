package com.verygood.security.larky.parser;


import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;

import com.verygood.security.larky.console.testing.TestingConsole;

import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LarkyEvaluatorTest {

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testErrorMessageOnFailedLoad() {
    StarlarkThread thread = new StarlarkThread(Mutability.create(), StarlarkSemantics.DEFAULT);
    LarkyEvaluator.LarkyLoader larkyLoader = new LarkyEvaluator.LarkyLoader(
      new InMemMapBackedStarFile(ImmutableMap.of(), "vendor/herp.star"),
      new LarkyEvaluator(
        new LarkyScript(LarkyScript.StarlarkMode.STRICT), new TestingConsole()));

     thread.setLoader(larkyLoader);
     Starlark.UncheckedEvalException ex =
         assertThrows(
             Starlark.UncheckedEvalException.class,
             () ->
                 Starlark.execFile(
                     ParserInput.fromString("load('@stdlib//does/not/exist', 'x')", "herp.star"),
                     FileOptions.DEFAULT,
                     Module.create(),
                   thread));
     assertThat(ex.getCause()).hasMessageThat()
       .contains("Unable to find resource: stdlib/does/not/exist.star and additionally " +
                   "there was no module for stdlib/does/not/exist/__init__.star found) " +
                   "while attempting to load @stdlib//does/not/exist from module: vendor/herp.star");
   }


}