package com.verygood.security.larky.modules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.verygood.security.larky.modules.types.results.Result;
import java.util.List;
import java.util.Map;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.SecretTaint;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.TaintMode;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import org.junit.jupiter.api.Test;

class ResultModuleTaintTest {

  @Test
  void safeWrapTagsTheResultOfAKnownTaintedKeywordArgument() throws Exception {
    try (Mutability mutability = Mutability.create("safe-wrap test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mutability, StarlarkSemantics.DEFAULT);
      SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, null);
      thread.setSecretTaint(taint);
      StarlarkCallable function =
          defineFunction("def f(value):\n    return 'fixture-result'\n", Map.of(), thread);
      StarlarkCallable wrapped = ResultModule.safeWrap(function);

      Object input = taint.tag(new String("synthetic-value"));
      Result result = (Result) Starlark.call(thread, wrapped, List.of(), Map.of("value", input));
      Result publicResult =
          (Result) Starlark.call(thread, wrapped, List.of(), Map.of("value", "ordinary-value"));

      assertTrue(result.isOk());
      assertEquals("fixture-result", result.getValue());
      assertTrue(taint.isTainted(result));
      assertFalse(taint.isTainted(publicResult));
    }
  }

  @Test
  void safeWrapTagsCaughtErrorsFromCallsWithTaintedPositionalArguments() throws Exception {
    try (Mutability mutability = Mutability.create("safe-wrap test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mutability, StarlarkSemantics.DEFAULT);
      SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, null);
      thread.setSecretTaint(taint);
      StarlarkCallable function =
          defineFunction("def f(value):\n    fail('fixture failed')\n", Map.of(), thread);

      Object input = taint.tag(new String("synthetic-value"));
      Result result =
          (Result) Starlark.call(thread, ResultModule.safeWrap(function), List.of(input), Map.of());

      assertTrue(result.isError());
      assertTrue(taint.isTainted(result));
    }
  }

  @Test
  void safeKeepsTaintWhenWrappingAnAlreadyTaggedReturnValue() throws Exception {
    try (Mutability mutability = Mutability.create("safe-wrap test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mutability, StarlarkSemantics.DEFAULT);
      SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, null);
      thread.setSecretTaint(taint);
      Object value = taint.tag(new String("synthetic-value"));
      StarlarkCallable function =
          defineFunction("def f():\n    return fixture\n", Map.of("fixture", value), thread);

      Result direct = ResultModule.safe(function, Tuple.empty(), Dict.empty(), thread);
      Result wrapped =
          (Result) Starlark.call(thread, ResultModule.safeWrap(function), List.of(), Map.of());

      assertTrue(direct.isOk());
      assertTrue(wrapped.isOk());
      assertTrue(taint.isTainted(direct));
      assertTrue(taint.isTainted(wrapped));
    }
  }

  @Test
  void resultMappersKeepTaintOfValuesProducedByTheirCallbacks() throws Exception {
    try (Mutability mutability = Mutability.create("result mapper test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mutability, StarlarkSemantics.DEFAULT);
      SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, null);
      thread.setSecretTaint(taint);
      Object value = taint.tag(new String("synthetic-value"));
      StarlarkCallable function =
          defineFunction("def f(previous):\n    return fixture\n", Map.of("fixture", value), thread);

      Result ok = ResultModule.ok("ordinary input");
      Result error = ResultModule.error("ordinary error", thread);
      Result mapped = ok.map(function, thread);
      Result flatMapped = ok.flatMap(function, thread);
      Result mappedError = error.mapError(function, thread);

      assertTrue(mapped.isOk());
      assertTrue(flatMapped.isOk());
      assertTrue(mappedError.isError());
      for (Result result : List.of(mapped, flatMapped, mappedError)) {
        assertSame(value, result.getValue());
        assertTrue(taint.isTainted(result));
      }
    }
  }

  @Test
  void resultMappersKeepExistingResultIdentity() throws Exception {
    try (Mutability mutability = Mutability.create("result mapper test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mutability, StarlarkSemantics.DEFAULT);
      SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, null);
      thread.setSecretTaint(taint);
      Result fixture = ResultModule.error("ordinary error", thread);
      StarlarkCallable function =
          defineFunction("def f(previous):\n    return fixture\n", Map.of("fixture", fixture), thread);

      Result ok = ResultModule.ok("ordinary input");
      Result error = ResultModule.error("previous error", thread);

      assertSame(fixture, ok.map(function, thread));
      assertSame(fixture, ok.flatMap(function, thread));
      assertSame(fixture, error.mapError(function, thread));
      assertFalse(taint.isTainted(fixture));
    }
  }

  @Test
  void safeAndMappersKeepTaintOfTheSpecificDiagnosticTheyCatch() throws Exception {
    try (Mutability mutability = Mutability.create("result diagnostic test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mutability, StarlarkSemantics.DEFAULT);
      SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, null);
      thread.setSecretTaint(taint);
      Object diagnostic = taint.tag(new String("synthetic diagnostic"));
      StarlarkCallable function = defineFunction(
          "def f(value=None):\n    fail(fixture)\n", Map.of("fixture", diagnostic), thread);
      StarlarkCallable plainFailure =
          defineFunction("def f():\n    fail('ordinary diagnostic')\n", Map.of(), thread);
      StarlarkCallable keepValue =
          defineFunction("def f(value):\n    return value\n", Map.of(), thread);
      StarlarkCallable accessMessage = defineFunction(
          "def f(result):\n    return (result._val, result.unwrap_err())\n", Map.of(), thread);

      Result ok = ResultModule.ok("ordinary input");
      Result direct = ResultModule.safe(function, Tuple.empty(), Dict.empty(), thread);
      Result mappedError = direct.mapError(keepValue, thread);
      Result wrapped = (Result) Starlark.call(
          thread, ResultModule.safeWrap(function), List.of(), Map.of());
      Result mapped = ok.map(function, thread);
      Result flatMapped = ok.flatMap(function, thread);

      for (Result result : List.of(direct, wrapped, mapped, flatMapped, mappedError)) {
        assertTrue(result.isError());
        assertTrue(taint.isTainted(result));
        Tuple messages = (Tuple) Starlark.call(thread, accessMessage, List.of(result), Map.of());
        for (Object message : messages) {
          assertEquals("synthetic diagnostic", message);
          assertTrue(taint.isTainted(message));
        }
      }

      Result ordinary = ResultModule.safe(plainFailure, Tuple.empty(), Dict.empty(), thread);
      assertTrue(ordinary.isError());
      assertEquals("ordinary diagnostic", ordinary.getValue());
      assertFalse(taint.isTainted(ordinary));
    }
  }

  @Test
  void aMapperRetainsTaintWhenItCopiesTheDiagnosticOfAThrowingErrorMapper() throws Exception {
    try (Mutability mutability = Mutability.create("wrapped result diagnostic test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mutability, StarlarkSemantics.DEFAULT);
      SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, null);
      thread.setSecretTaint(taint);
      Object diagnostic = taint.tag(new String("synthetic wrapped diagnostic"));
      StarlarkCallable failure = defineFunction(
          "def f(previous):\n    fail(fixture)\n", Map.of("fixture", diagnostic), thread);
      StarlarkCallable callErrorMapper = defineFunction(
          "def f(previous):\n    return result.map_err(callback)\n",
          Map.of("result", ResultModule.error("ordinary input", thread), "callback", failure),
          thread);

      Result mapped = ResultModule.ok("ordinary input").map(callErrorMapper, thread);
      Result direct = ResultModule.safe(
          callErrorMapper, Tuple.of("ordinary input"), Dict.empty(), thread);

      assertTrue(mapped.isError());
      assertTrue(((String) mapped.getValue()).endsWith("synthetic wrapped diagnostic"));
      assertTrue(taint.isTainted(mapped));
      assertTrue(direct.isError());
      assertFalse(taint.isTainted(direct));
    }
  }

  @Test
  void onlyTheCopiedDiagnosticOfATaggedNativeFailureRetainsTaint() throws Exception {
    try (Mutability mutability = Mutability.create("native result diagnostic test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mutability, StarlarkSemantics.DEFAULT);
      SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, null);
      thread.setSecretTaint(taint);
      Object input = taint.tag(new String("synthetic-value"));
      NativeDiagnosticFixture fixture = new NativeDiagnosticFixture();
      String callback = "def f(previous):\n    fixture.diagnose(input)\n";
      StarlarkCallable tagged =
          defineFunction(callback, Map.of("fixture", fixture, "input", input), thread);
      StarlarkCallable ordinary =
          defineFunction(callback, Map.of("fixture", fixture, "input", "ordinary value"), thread);
      Result base = ResultModule.ok("ordinary input");

      Result mapped = base.map(tagged, thread);
      Result flatMapped = base.flatMap(tagged, thread);
      Result unrelated = base.map(ordinary, thread);
      Result wrapper = ResultModule.safe(tagged, Tuple.of("ordinary input"), Dict.empty(), thread);

      assertEquals("synthetic native diagnostic", mapped.getValue());
      assertEquals("synthetic native diagnostic", flatMapped.getValue());
      assertEquals("synthetic native diagnostic", unrelated.getValue());
      assertTrue(taint.isTainted(mapped));
      assertTrue(taint.isTainted(flatMapped));
      assertFalse(taint.isTainted(unrelated));
      assertFalse(taint.isTainted(wrapper));
    }
  }

  public static final class NativeDiagnosticFixture implements StarlarkValue {
    @StarlarkMethod(name = "diagnose", parameters = @Param(name = "input"))
    public String diagnose(String input) {
      throw new IllegalStateException("synthetic native diagnostic");
    }
  }

  private static StarlarkCallable defineFunction(
      String source, Map<String, Object> predeclared, StarlarkThread thread) throws Exception {
    Module module = Module.withPredeclared(StarlarkSemantics.DEFAULT, predeclared);
    Starlark.execFile(
        ParserInput.fromString(source, "fixture.star"), FileOptions.DEFAULT, module, thread);
    return (StarlarkCallable) module.getGlobal("f");
  }
}
