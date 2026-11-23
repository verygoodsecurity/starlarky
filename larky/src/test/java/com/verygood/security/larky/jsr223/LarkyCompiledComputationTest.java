package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.modules.vgs.ShortCircuitHttpResponse;
import com.verygood.security.larky.parser.Computation;
import com.verygood.security.larky.parser.CurrentEvaluation;
import com.verygood.security.larky.parser.EvaluationMetadata;
import com.verygood.security.larky.parser.EvaluationSettings;
import java.io.StringWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.Tuple;
import org.junit.Test;

public class LarkyCompiledComputationTest {

  @Test
  public void evaluatorUsesTheSuppliedScriptAndItsBindingPolicy() throws Exception {
    var permissive = new LarkyScriptEngine();
    var strict = LarkyScriptEngine.builder().globalScope(LarkyScriptEngine.GlobalScope.REFUSE).build();
    var first = permissive.compile("def process(): return from_host\n", "shared-name.star");
    var second = strict.compile("def process(): return 'second'\n", "shared-name.star");
    var firstContext = new LarkyScriptContext();
    firstContext.setBindings(new SimpleBindings(Map.of("from_host", "first")), ScriptContext.GLOBAL_SCOPE);
    var secondContext = new LarkyScriptContext();

    assertEquals("first",
        LarkyScriptEvaluator.compute(first, firstContext, null, (run, unused) -> run.call("process")));
    assertEquals("second",
        LarkyScriptEvaluator.compute(second, secondContext, null, (run, unused) -> run.call("process")));
    assertThrows(LarkyGlobalScopeRefusedException.class, () ->
        LarkyScriptEvaluator.compute(second, firstContext, null, (run, unused) -> run.call("process")));
  }

  @Test
  public void hostCanModifyTheResultAndCallAgainBeforeTheScopeCloses() throws Exception {
    var compiled = new LarkyScriptEngine().compile("""
        values = []
        def process(input, ctx):
            values.append(input[0] + ctx["suffix"])
            return values
        def finish(result):
            result.append("finished")
        """, "computation.star");
    var context = new LarkyScriptContext();
    var scope = new AtomicReference<Computation.Scope>();

    StarlarkList<?> result = LarkyScriptEvaluator.compute(compiled, context, "input", (run, input) -> {
      scope.set(run);
      var output = (StarlarkList<?>) run.call("process", List.of(input), Map.of("suffix", ""));
      run.callMethod(output, "append", "host");
      run.call("finish", output);
      assertFalse(output.mutability().isFrozen());
      return output;
    });

    assertEquals(List.of("input", "host", "finished"), List.copyOf(result));
    assertTrue(result.mutability().isFrozen());
    assertThrows(IllegalStateException.class, () -> scope.get().call("process", "escaped"));
    assertNull(CurrentEvaluation.currentOrNull());
    assertFalse(context.getBindings(ScriptContext.ENGINE_SCOPE).containsKey("values"));
  }

  @Test
  public void atlasCanBuildAndReadHttpResponsesInsideOneComputation() throws Exception {
    var compiled = new LarkyScriptEngine().compile("""
        load("@vgs//proxy", "proxy")
        def process(response, ctx):
            if ctx["short"]:
                response = proxy.ShortCircuitResponse(
                    body=response.body, headers=response.headers, status_code=response.status_code)
            response.append_header("Set-Cookie", "first=1")
            ctx["seen"] = True
            print("handled")
            return response
        """, "atlas.star");

    for (boolean shortCircuit : List.of(false, true)) {
      var context = new LarkyScriptContext();
      var logs = new StringWriter();
      context.setWriter(logs);
      List<?> headers = LarkyScriptEvaluator.compute(
          compiled, context, List.of("@vgs//http/response"), shortCircuit, (run, stop) -> {
        var constructor = run.export("@vgs//http/response", "VGSHttpResponse");
        Object input = run.callValue(constructor, List.of(), Map.of("status_code", 403, "body", "blocked"));
        Object response = run.call("process", input, Map.of("short", stop));
        assertEquals(stop, response instanceof ShortCircuitHttpResponse);
        assertSame(constructor, run.getField(response, "__class__"));
        run.callMethod(response, "append_header", "set-cookie", "second=2");
        return List.copyOf((List<?>) run.callMethod(run.getField(response, "headers"), "items"));
      });

      assertEquals(List.of(Tuple.pair("Set-Cookie", "first=1"), Tuple.pair("set-cookie", "second=2")), headers);
      assertTrue(logs.toString().contains("handled"));
      assertTrue(context.metadata().executedSteps() > 0);
    }
  }

  @Test
  public void callsShareTheStepBudgetAndCloseOnFailure() throws Exception {
    var compiled = new LarkyScriptEngine().compile("""
        def process():
            values = []
            for i in range(16):
                values.append(i)
            return values
        """, "steps.star");
    var baseline = new LarkyScriptContext();
    LarkyScriptEvaluator.compute(compiled, baseline, null, (run, unused) -> run.call("process"));

    var context = new LarkyScriptContext();
    context.setSettings(EvaluationSettings.builder().stepLimit(baseline.metadata().executedSteps() + 1).build());
    var firstResult = new AtomicReference<StarlarkList<?>>();
    var failure = assertThrows(LarkyEvaluationScriptException.class, () ->
        LarkyScriptEvaluator.compute(compiled, context, null, (run, unused) -> {
          firstResult.set((StarlarkList<?>) run.call("process"));
          return run.call("process");
        }));

    assertNotNull(firstResult.get());
    assertTrue(firstResult.get().mutability().isFrozen());
    assertTrue(failure.getMessage().contains("too many steps"));
    assertEquals(EvaluationMetadata.Outcome.STEP_LIMIT, context.metadata().outcome());
    assertNull(CurrentEvaluation.currentOrNull());
  }

  @Test
  public void anExpiredHostComputationClosesWithoutReturningItsResult() throws Exception {
    var compiled = new LarkyScriptEngine().compile("def process(): return []\n", "deadline.star");
    var context = new LarkyScriptContext();
    Instant expiration = Instant.now().plusSeconds(300);
    context.setSettings(EvaluationSettings.builder().expiration(expiration).build());
    var result = new AtomicReference<StarlarkList<?>>();

    assertThrows(LarkyEvaluationScriptException.class, () ->
        LarkyScriptEvaluator.compute(compiled, context, null, (run, unused) -> {
          result.set((StarlarkList<?>) run.call("process"));
          CurrentEvaluation.requireThread().setClock(Clock.fixed(expiration.plusMillis(1), ZoneOffset.UTC));
          return result.get();
        }));

    assertNotNull(result.get());
    assertTrue(result.get().mutability().isFrozen());
    assertEquals(EvaluationMetadata.Outcome.DEADLINE, context.metadata().outcome());
    assertNull(CurrentEvaluation.currentOrNull());
  }

  @Test
  public void hostImportsAndScriptLoadsUseFrozenLibraryValues() throws Exception {
    var engine = new LarkyScriptEngine();
    engine.put(LarkyScriptEngine.MODULES, Map.of("helper.star", "values = []\n"));
    var compiled = engine.compile("""
        load("helper", "values")
        def process():
            return values
        """, "caller.star");

    LarkyScriptEvaluator.compute(compiled, new LarkyScriptContext(), List.of("helper"), null, (run, unused) -> {
      Object libraryValue = run.export("helper", "values");
      assertSame(libraryValue, run.call("process"));
      EvalException error = assertThrows(EvalException.class, () -> run.callMethod(libraryValue, "append", 1));
      assertTrue(error.getMessage().contains("frozen"));
      return null;
    });
  }
}
