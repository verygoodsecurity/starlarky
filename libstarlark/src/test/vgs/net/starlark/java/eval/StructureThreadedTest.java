// Copyright 2026 Very Good Security Authors
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** The interpreter gives a {@link Structure.Threaded} its thread for field reads and writes. */
@RunWith(JUnit4.class)
public final class StructureThreadedTest {

  /** Records how each field access reached it. */
  private static class Plain implements Structure {
    final Map<String, Object> fields = new LinkedHashMap<>(Map.of("a", StarlarkInt.of(1)));
    final List<String> calls = new ArrayList<>();

    @Nullable
    @Override
    public Object getValue(String name) {
      calls.add("get " + name);
      return fields.get(name);
    }

    @Override
    public void setField(String field, Object value) {
      calls.add("set " + field);
      fields.put(field, value);
    }

    @Override
    public ImmutableList<String> getFieldNames() {
      return ImmutableList.copyOf(fields.keySet());
    }

    @Nullable
    @Override
    public String getErrorMessageForUnknownField(String field) {
      return null;
    }
  }

  private static final class Threaded extends Plain implements Structure.Threaded {
    final List<StarlarkThread> threads = new ArrayList<>();

    @Nullable
    @Override
    public Object getValue(StarlarkThread thread, String name) {
      threads.add(thread);
      calls.add("get+thread " + name);
      return fields.get(name);
    }

    @Override
    public void setField(StarlarkThread thread, String field, Object value) {
      threads.add(thread);
      calls.add("set+thread " + field);
      fields.put(field, value);
    }
  }

  private static final String SCRIPT = """
      y = x.a
      x.b = 2
      x.b += 1
      z = getattr(x, "a")
      w = getattr(x, "missing", 9)
      """;

  private static StarlarkThread run(Object x) throws Exception {
    Module module = Module.withPredeclared(StarlarkSemantics.DEFAULT, ImmutableMap.of("x", x));
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Starlark.execFile(ParserInput.fromString(SCRIPT, "test.star"), FileOptions.DEFAULT, module, thread);
      assertThat(module.getGlobal("w")).isEqualTo(StarlarkInt.of(9));
      return thread;
    }
  }

  @Test
  public void aThreadedStructureGetsTheThread() throws Exception {
    Threaded x = new Threaded();
    StarlarkThread thread = run(x);
    assertThat(x.calls).containsExactly(
        "get+thread a", "set+thread b", "get+thread b", "set+thread b", "get+thread a", "get+thread missing")
        .inOrder();
    assertThat(x.threads).hasSize(6);
    for (StarlarkThread seen : x.threads) {
      assertThat(seen).isSameInstanceAs(thread);
    }
    assertThat(x.fields.get("b")).isEqualTo(StarlarkInt.of(3));
  }

  @Test
  public void aPlainStructureIsReadAndWrittenAsBefore() throws Exception {
    Plain x = new Plain();
    run(x);
    assertThat(x.calls).containsExactly("get a", "set b", "get b", "set b", "get a", "get missing").inOrder();
  }

  @Test
  public void aCallerWithoutAThreadUsesThePlainMethods() throws Exception {
    Threaded x = new Threaded();
    try (Mutability mu = Mutability.create("test")) {
      assertThat(Starlark.getattr(mu, StarlarkSemantics.DEFAULT, x, "a", null)).isEqualTo(StarlarkInt.of(1));
    }
    assertThat(x.calls).containsExactly("get a");
    assertThat(x.threads).isEmpty();
  }

  @Test
  public void javaCodeWithAThreadReachesTheThreadedMethods() throws Exception {
    Threaded x = new Threaded();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      StarlarkOperators.setattr(thread, x, "c", StarlarkInt.of(5));
      assertThat(StarlarkOperators.getattr(thread, x, "c", null)).isEqualTo(StarlarkInt.of(5));
      assertThat(x.threads).containsExactly(thread, thread);
    }
    assertThat(x.calls).containsExactly("set+thread c", "get+thread c").inOrder();
  }
}
