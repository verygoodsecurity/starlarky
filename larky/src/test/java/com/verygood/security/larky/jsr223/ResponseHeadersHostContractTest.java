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

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.Map;
import javax.script.SimpleBindings;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.Tuple;
import org.junit.Test;

/**
 * The host builds a VGSHttpResponse from its input, runs the script, and reads the response's
 * {@code __dict__["headers"]} as a {@code Map<String, String>} (issue #292). The wrapper below
 * follows the shape of the host's response pre- and post-processing.
 */
public class ResponseHeadersHostContractTest {

  private static final String WRAPPER = String.join("\n",
      "load('@stdlib//builtins', builtins_h='builtins')",
      "load('@vgs//http/response', vgs_response_h='VGSHttpResponse')",
      "load('@vgs//proxy', 'proxy')",
      "%s",
      "def pre_h(input, ctx):",
      "    input_dict = dict(input)",
      "    return vgs_response_h(",
      "        body=input_dict['body'],",
      "        headers=dict(input_dict['headers']),",
      "        status_code=input_dict['status_code'],",
      "    ), ctx",
      "def post_h(output):",
      "    if getattr(output, '__name__', None) == 'ShortCircuitResponse':",
      "        return dict(output.response.__dict__)",
      "    if builtins_h.isinstance(output, vgs_response_h):",
      "        return output.__dict__",
      "    return None",
      "out_h = post_h(process(*pre_h(in_h, {})))",
      "");

  @SuppressWarnings("unchecked")
  private static Map<String, String> headersSentToHost(String script) throws Exception {
    List<Tuple> headers = List.of(
        Tuple.pair("Content-Type", "application/json"), Tuple.pair("Set-Cookie", "a=1"));
    SimpleBindings bindings = new SimpleBindings();
    bindings.put("in_h", List.of(
        Tuple.pair("body", StarlarkBytes.of(Mutability.IMMUTABLE, "{}".getBytes())),
        Tuple.pair("status_code", 200),
        Tuple.pair("headers", headers)));
    new LarkyScriptEngine().eval(String.format(WRAPPER, script), bindings);
    Dict<String, Object> out = (Dict<String, Object>) bindings.get("out_h");
    Object value = out.get("headers");
    assertThat(value).isInstanceOf(Map.class);
    for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
      assertThat(e.getKey()).isInstanceOf(String.class);
      assertThat(e.getValue()).isInstanceOf(String.class);
    }
    return (Map<String, String>) value;
  }

  @Test
  public void unchangedHeaders() throws Exception {
    assertThat(headersSentToHost("def process(input, ctx):\n    return input\n"))
        .containsExactly("Content-Type", "application/json", "Set-Cookie", "a=1");
  }

  @Test
  public void changedHeaders() throws Exception {
    String script = String.join("\n",
        "def process(input, ctx):",
        "    input.add_header('X-A', '1')",
        "    input.remove_header('set-cookie')",
        "    input.body = b'changed'",
        "    return input",
        "");
    assertThat(headersSentToHost(script))
        .containsExactly("Content-Type", "application/json", "X-A", "1");
  }

  @Test
  public void headersAssignedAsPairs() throws Exception {
    String script = "def process(input, ctx):\n"
        + "    input.headers = [('X-B', '2')]\n"
        + "    return input\n";
    assertThat(headersSentToHost(script)).containsExactly("X-B", "2");
  }

  @Test
  public void shortCircuitResponse() throws Exception {
    String script = "def process(input, ctx):\n"
        + "    return proxy.ShortCircuitResponse(vgs_response_h(body=b'no', headers=[('X-C', '3')]))\n";
    assertThat(headersSentToHost(script)).containsExactly("X-C", "3");
  }
}
