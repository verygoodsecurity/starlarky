/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;
import net.starlark.java.lib.StarlarkEncodable;
import org.junit.Test;

/** json.dumps encodes a Java-defined value as upstream Starlark's json does: by its StarlarkEncodable. */
public class JsonModuleEncodableTest {

  @StarlarkBuiltin(name = "Token")
  static final class Token implements StarlarkValue, StarlarkEncodable {
    @Override
    public Object objectForEncoding(StarlarkSemantics semantics) {
      return Dict.immutableCopyOf(ImmutableMap.of("b", Tuple.of(StarlarkInt.of(1)), "a", "x"));
    }
  }

  @StarlarkBuiltin(name = "Opaque")
  static final class Opaque implements StarlarkValue {}

  private static Object eval(String script, Object value) throws Exception {
    SimpleBindings bindings = new SimpleBindings();
    bindings.put("value", value);
    LarkyScriptEngine engine = new LarkyScriptEngine();
    engine.setContext(new SimpleScriptContext());
    engine.eval(
        "load(\"@stdlib//json\", \"json\")\n" + script,
        bindings);
    return bindings.get("OUT");
  }

  @Test
  public void aStarlarkEncodableIsEncodedAsItsObjectForEncoding() throws Exception {
    assertEquals(
        "[{\"a\":\"x\",\"b\":[1]}]", eval("OUT = json.dumps([value])\n", new Token()));
  }

  @Test
  public void anyOtherJavaValueIsNotSerializable() {
    ScriptException e =
        assertThrows(ScriptException.class, () -> eval("OUT = json.dumps(value)\n", new Opaque()));
    assertTrue(e.getMessage(), e.getMessage().contains("Object of type Opaque is not JSON serializable"));
  }
}
