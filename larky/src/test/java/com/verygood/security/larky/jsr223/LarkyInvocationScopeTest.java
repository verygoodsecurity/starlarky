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

package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.modules.vgs.ShortCircuit;
import com.verygood.security.larky.modules.vgs.ShortCircuitResponse;
import com.verygood.security.larky.modules.vgs.VgsHttpResponse;
import com.verygood.security.larky.parser.CurrentEvaluation;
import com.verygood.security.larky.parser.EvaluationMetadata;
import com.verygood.security.larky.parser.EvaluationSettings;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import org.junit.Test;

public class LarkyInvocationScopeTest {

  @Test
  public void invocationConvertsJavaCollectionsInItsMutableScope() throws Exception {
    var engine = new LarkyScriptEngine();
    engine.eval("""
        def process(headers):
            headers["X-Test"].append("second")
            headers["X-New"] = ["added"]
            return headers
        """);
    var input = Map.of("X-Test", List.of("first"));

    var result = (Dict<?, ?>) engine.invokeFunction("process", input);

    assertEquals(List.of("first", "second"), result.get("X-Test"));
    assertEquals(List.of("added"), result.get("X-New"));
    assertEquals(Map.of("X-Test", List.of("first")), input);
    assertTrue(result.mutability().isFrozen());
  }

  @Test
  public void invocationBorrowsTheOwnersThreadAndLeavesItsValuesMutable() throws Exception {
    var engine = new LarkyScriptEngine();
    var file = engine.eval("""
        def process(values):
            print("from borrowed thread")
            values.append("script")
            return values
        """);
    StarlarkList<?> result;
    var logs = new StringBuilder();
    var mutability = Mutability.create("host");
    var thread = StarlarkThread.createTransient(mutability, LarkySemantics.LARKY_SEMANTICS);
    thread.setPrintHandler((running, message) -> logs.append(message));
    try (mutability) {
      result = (StarlarkList<?>) engine.invokeMethodInScope(thread, file, "process", List.of("input"));
      assertSame(mutability, result.mutability());
      assertFalse(result.mutability().isFrozen());
      engine.invokeMethodInScope(thread, result, "append", "host");
      assertEquals(List.of("input", "script", "host"), List.copyOf(result));
      assertEquals("from borrowed thread", logs.toString());
      assertNull(CurrentEvaluation.currentOrNull());

      thread.setMaxExecutionSteps(thread.getExecutedSteps() + 1);
      assertThrows(LarkyEvaluationScriptException.class, () ->
          engine.invokeMethodInScope(thread, file, "process", List.of("input")));
      assertFalse(mutability.isFrozen());
      assertNull(CurrentEvaluation.currentOrNull());
    }
    assertTrue(result.mutability().isFrozen());
    assertThrows(IllegalStateException.class, () ->
        engine.invokeMethodInScope(thread, file, "process", List.of("input")));
  }

  @Test
  public void borrowingPreservesTheIdentityOfAnExistingStarlarkDictionary() throws Exception {
    var engine = new LarkyScriptEngine();
    var file = engine.eval("def process(headers):\n    headers['new'] = 'value'\n    return headers\n");
    try (var mutability = Mutability.create("host-dict")) {
      var thread = StarlarkThread.createTransient(mutability, LarkySemantics.LARKY_SEMANTICS);
      var headers = Dict.<String, Object>of(mutability);
      headers.putEntry("old", StarlarkList.copyOf(mutability, List.of("value")));

      var result = engine.invokeMethodInScope(thread, file, "process", headers);

      assertSame(headers, result);
      assertEquals("value", headers.get("new"));
    }
  }

  @Test
  public void borrowedInvocationReturnsATypedShortCircuitWithOrderedHeaders() throws Exception {
    var engine = new LarkyScriptEngine();
    var file = engine.eval("""
        load("@vgs//http/response", "VGSHttpResponse")
        load("@vgs//proxy", "proxy")

        def process(stop):
            response = VGSHttpResponse(body="blocked", status_code=403)
            response.append_header("Set-Cookie", "first=1")
            response.append_header("set-cookie", "second=2")
            return proxy.ShortCircuitResponse(response) if stop else response
        """);
    var mutability = Mutability.create("host-http");
    var thread = StarlarkThread.createTransient(mutability, LarkySemantics.LARKY_SEMANTICS);
    try (mutability; var scope = CurrentEvaluation.enter(thread)) {
      var shortCircuit = (ShortCircuitResponse) engine.invokeMethodInScope(thread, file, "process", true);
      // any protocol's response; this one is HTTP
      assertTrue(shortCircuit.response() instanceof VgsHttpResponse);
      var response = (VgsHttpResponse) shortCircuit.response();
      assertEquals("blocked", response.body(thread));
      assertEquals(StarlarkInt.of(403), response.statusCode(thread));
      assertSame(response, shortCircuit.getValue("response"));
      assertEquals("ShortCircuitResponse", shortCircuit.getValue("__name__"));
      var headers = response.headerItems(thread);
      assertEquals(List.of(Tuple.pair("Set-Cookie", "first=1"), Tuple.pair("set-cookie", "second=2")), headers);
      Object ordinaryResponse = engine.invokeMethodInScope(thread, file, "process", false);
      assertTrue(ordinaryResponse instanceof VgsHttpResponse);
      assertFalse(ordinaryResponse instanceof ShortCircuit);
      assertFalse(mutability.isFrozen());
    }
  }

  @Test
  public void explicitInvocationContextOwnsTheWriterAndMetadata() throws Exception {
    var engine = new LarkyScriptEngine();
    var loadContext = LarkyScriptContext.builder().build();
    var file = engine.eval("""
        def process(value):
            print(value)
            return value
        """, loadContext);
    var loadMetadata = loadContext.metadata();
    var writer = new StringWriter();
    var callContext = LarkyScriptContext.builder().writer(writer).build();

    assertEquals("hello", engine.invokeMethodWithContext(callContext, file, "process", "hello"));
    assertEquals("hello\n", writer.toString());
    assertEquals(EvaluationMetadata.Outcome.OK, callContext.metadata().outcome());
    assertTrue(callContext.metadata().executedSteps() > 0);
    assertSame(loadMetadata, loadContext.metadata());

    callContext.setSettings(EvaluationSettings.builder().expiration(Instant.EPOCH).build());
    var error = assertThrows(LarkyEvaluationScriptException.class, () ->
        engine.invokeMethodWithContext(callContext, file, "process", "late"));
    assertEquals(EvaluationMetadata.Outcome.DEADLINE, callContext.metadata().outcome());
    assertSame(callContext.metadata(), error.metadata());
    assertSame(loadMetadata, loadContext.metadata());
  }
}
