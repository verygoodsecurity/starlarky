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

package com.verygood.security.larky.modules.multidict;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.runtime.CurrentEvaluation;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.util.List;
import java.util.Map;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkThread;
import org.junit.Test;

/** A multidict, read from Java, is a read-only ListMultimap of its lines. */
public class LarkyMultiDictMultimapTest {

  private static LarkyMultiDict global(String script, String name) throws Exception {
    var file = (ParsedStarFile) new LarkyScriptEngine().eval(script);
    return (LarkyMultiDict) file.getModule().getGlobal(name);
  }

  @Test
  public void aMultidictIsAListMultimapOfItsLinesInOrder() throws Exception {
    ListMultimap<Object, Object> headers = global("""
        load("@vgs//http/headers", "VGSCIMultiDict")
        headers = VGSCIMultiDict([("Set-Cookie", "a=1"), ("X-Trace", "one"), ("set-cookie", "b=2")])
        """, "headers");

    // not a java.util.Map, which would show one value per key
    assertFalse(((Object) headers) instanceof Map);
    assertEquals(3, headers.size());
    assertEquals(
        List.of(Maps.immutableEntry("Set-Cookie", "a=1"), Maps.immutableEntry("X-Trace", "one"),
            Maps.immutableEntry("set-cookie", "b=2")),
        List.copyOf(headers.entries()));
    assertEquals(List.of("a=1", "one", "b=2"), List.copyOf(headers.values()));
    // case-insensitive, as the multidict is
    assertEquals(List.of("a=1", "b=2"), List.copyOf(headers.get("SET-COOKIE")));
    assertTrue(headers.containsKey("x-trace"));
    assertTrue(headers.containsEntry("set-cookie", "a=1"));
    assertFalse(headers.containsEntry("set-cookie", "one"));
    assertEquals(List.of("Set-Cookie", "X-Trace"), List.copyOf(headers.keySet()));
    assertEquals(3, headers.keys().size());
    assertEquals(List.of("Set-Cookie", "X-Trace"), List.copyOf(headers.asMap().keySet()));
    assertEquals(List.of("a=1", "b=2"), List.copyOf(headers.asMap().get("Set-Cookie")));
  }

  @Test
  public void vgsHeadersPrintTheirItemsOrAnOverride() throws Exception {
    var file = (ParsedStarFile) new LarkyScriptEngine().eval("""
        load("@vgs//http/headers", "VGSCIMultiDict")
        headers = VGSCIMultiDict([("Authorization", "secret"), ("X-Trace", "one")])
        plain = str(headers)
        headers.items = lambda: [("Masked", "***")]
        masked = str(headers)
        """);
    assertEquals("{\"Authorization\": \"secret\", \"X-Trace\": \"one\"}", file.getModule().getGlobal("plain"));
    assertEquals("{\"Masked\": \"***\"}", file.getModule().getGlobal("masked"));
  }

  @Test
  public void aCaseSensitiveMultidictMatchesKeysExactly() throws Exception {
    ListMultimap<Object, Object> md = global("""
        load("@vendor//multidict", "MultiDict")
        md = MultiDict([("A", "1"), ("a", "2"), ("A", "3")])
        """, "md");

    assertEquals(List.of("1", "3"), List.copyOf(md.get("A")));
    assertEquals(List.of("2"), List.copyOf(md.get("a")));
    assertEquals(List.of("A", "a"), List.copyOf(md.keySet()));
  }

  @Test
  public void theViewIsReadOnly() throws Exception {
    ListMultimap<Object, Object> md = global("""
        load("@vendor//multidict", "MultiDict")
        md = MultiDict([("A", "1")])
        """, "md");

    assertThrows(UnsupportedOperationException.class, () -> md.put("B", "2"));
    assertThrows(UnsupportedOperationException.class, () -> md.removeAll("A"));
    assertThrows(UnsupportedOperationException.class, () -> md.replaceValues("A", List.of("2")));
    assertThrows(UnsupportedOperationException.class, md::clear);
    assertEquals(1, md.size());
  }

  @Test
  public void theViewFollowsChangesTheScriptMakes() throws Exception {
    var engine = new LarkyScriptEngine();
    var file = engine.eval("""
        load("@vendor//multidict", "CIMultiDict")
        def build():
            return CIMultiDict([("A", "1")])
        def add(md):
            md.add("a", "2")
        """);
    try (var mutability = Mutability.create("multimap-view")) {
      var thread = StarlarkThread.createTransient(mutability, LarkySemantics.LARKY_SEMANTICS);
      try (var scope = CurrentEvaluation.enter(thread)) {
        var md = (LarkyMultiDict) engine.invokeMethodInScope(thread, file, "build");
        List<Object> values = md.get("A");
        assertEquals(List.of("1"), List.copyOf(values));

        engine.invokeMethodInScope(thread, file, "add", md);

        assertEquals(List.of("1", "2"), List.copyOf(values));
        assertEquals(2, md.entries().size());
      }
    }
  }
}
