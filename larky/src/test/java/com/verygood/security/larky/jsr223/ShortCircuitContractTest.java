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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.modules.vgs.ShortCircuit;
import org.junit.Test;

/** What a host relies on: {@code instanceof ShortCircuit}, and {@link ShortCircuit#response()} to encode. */
public class ShortCircuitContractTest {

  private static final String SCRIPT = """
      load("@vgs//http/response", "VGSHttpResponse")
      load("@vgs//proxy", "proxy")
      original = []
      def wrapped():
          response = VGSHttpResponse(body=b"blocked", status_code=403)
          original.append(response)
          return proxy.ShortCircuitResponse(response)
      def from_fields():
          return proxy.ShortCircuitResponse(body=b"blocked", status_code=403)
      def first_original():
          return original
      def plain():
          return VGSHttpResponse(body=b"ok")
      """;

  @Test
  public void aWrappedResponseIsAShortCircuitOfThatResponse() throws Exception {
    var compiled = new LarkyScriptEngine().compile(SCRIPT, "short_circuit.star");
    LarkyScriptEvaluator.compute(compiled, new LarkyScriptContext(), null, (run, unused) -> {
      Object result = run.call("wrapped");
      assertTrue(result instanceof ShortCircuit);
      Object original = ((java.util.List<?>) run.call("first_original")).get(0);
      assertSame(original, ((ShortCircuit) result).response());
      assertFalse(((ShortCircuit) result).response() instanceof ShortCircuit);
      return null;
    });
  }

  @Test
  public void theHttpShorthandIsAShortCircuitOfItself() throws Exception {
    var compiled = new LarkyScriptEngine().compile(SCRIPT, "short_circuit.star");
    LarkyScriptEvaluator.compute(compiled, new LarkyScriptContext(), null, (run, unused) -> {
      Object result = run.call("from_fields");
      assertTrue(result instanceof ShortCircuit);
      assertSame(result, ((ShortCircuit) result).response());
      assertEquals("403", String.valueOf(run.getField(result, "status_code")));
      assertFalse(run.call("plain") instanceof ShortCircuit);
      return null;
    });
  }
}
