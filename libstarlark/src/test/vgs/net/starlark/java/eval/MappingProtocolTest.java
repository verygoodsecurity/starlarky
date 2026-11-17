// Copyright 2026 Very Good Security Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package net.starlark.java.eval;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests that dict(m) and d.update(m) accept any object with a keys() method, as Python does. */
@RunWith(JUnit4.class)
public final class MappingProtocolTest {

  /** A mapping with a repeated key, as a multidict: keys() lists "a" twice, [] returns the first. */
  static final class FakeMapping implements StarlarkValue, StarlarkIndexable {
    @StarlarkMethod(name = "keys")
    public StarlarkList<Object> keys() {
      return StarlarkList.immutableOf("a", "b", "a");
    }

    @Override
    public Object getIndex(StarlarkSemantics semantics, Object key) throws EvalException {
      switch ((String) key) {
        case "a":
          return StarlarkInt.of(1);
        case "b":
          return StarlarkInt.of(2);
        default:
          throw Starlark.errorf("key %s not found", Starlark.repr(key, semantics));
      }
    }

    @Override
    public boolean containsKey(StarlarkSemantics semantics, Object key) {
      return "a".equals(key) || "b".equals(key);
    }
  }

  /** Has an attribute named keys that is not callable. */
  static final class NotCallableKeys implements StarlarkValue {
    @StarlarkMethod(name = "keys", structField = true)
    public String keys() {
      return "nope";
    }
  }

  private static String repr(Object x) {
    return Starlark.repr(x, StarlarkSemantics.DEFAULT);
  }

  private static Module exec(Object m, String... lines) throws Exception {
    Module module = Module.withPredeclared(StarlarkSemantics.DEFAULT, ImmutableMap.of("M", m));
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Starlark.execFile(ParserInput.fromLines(lines), FileOptions.DEFAULT, module, thread);
    }
    return module;
  }

  @Test
  public void dictAcceptsObjectWithKeys() throws Exception {
    Module module = exec(new FakeMapping(), "d = dict(M)", "e = dict(M, c=3)");
    assertThat(repr(module.getGlobal("d"))).isEqualTo("{\"a\": 1, \"b\": 2}");
    assertThat(repr(module.getGlobal("e"))).isEqualTo("{\"a\": 1, \"b\": 2, \"c\": 3}");
  }

  @Test
  public void updateAcceptsObjectWithKeys() throws Exception {
    Module module = exec(new FakeMapping(), "d = {'z': 0, 'a': 9}", "d.update(M)", "d.update(M, q=1)");
    assertThat(repr(module.getGlobal("d")))
        .isEqualTo("{\"z\": 0, \"a\": 1, \"b\": 2, \"q\": 1}");
  }

  @Test
  public void pairsAndDictsAreUnchanged() throws Exception {
    Module module =
        exec(new FakeMapping(), "a = dict([('x', 1)])", "b = dict({'y': 2})", "c = dict([['x', 1]], z=2)");
    assertThat(repr(module.getGlobal("a"))).isEqualTo("{\"x\": 1}");
    assertThat(repr(module.getGlobal("b"))).isEqualTo("{\"y\": 2}");
    assertThat(repr(module.getGlobal("c"))).isEqualTo("{\"x\": 1, \"z\": 2}");
  }

  @Test
  public void badInputKeepsStarlarkErrors() throws Exception {
    EvalException e = assertThrows(EvalException.class, () -> exec(new FakeMapping(), "dict(['ab'])"));
    assertThat(e).hasMessageThat().contains("dictionary update sequence element #0 is not iterable (string)");
    e = assertThrows(EvalException.class, () -> exec(new FakeMapping(), "dict(1)"));
    assertThat(e).hasMessageThat().contains("in dict, got int, want iterable");
    e = assertThrows(EvalException.class, () -> exec(new NotCallableKeys(), "dict(M)"));
    assertThat(e).hasMessageThat().contains("in dict, got NotCallableKeys, want iterable");
  }
}
