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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.starlark.java.eval.StarlarkBytes.StarlarkByteArray;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests that a bytearray obeys freezing and for-loop iteration locks, as a list does. */
@RunWith(JUnit4.class)
public final class StarlarkByteArrayTest {

  private static final ImmutableList<String> MUTATIONS =
      ImmutableList.of(
          "B.append(67)",
          "B.extend(b'x')",
          "B.insert(0, b'x')",
          "B.clear()",
          "B.pop()",
          "B.remove(65)");

  /** Runs {@code code} with {@code B} bound to {@code b}, on a thread with its own Mutability. */
  private static Module exec(StarlarkByteArray b, String... lines) throws Exception {
    Module module =
        Module.withPredeclared(StarlarkSemantics.DEFAULT, ImmutableMap.of("B", b));
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Starlark.execFile(ParserInput.fromLines(lines), FileOptions.DEFAULT, module, thread);
    }
    return module;
  }

  private static StarlarkByteArray frozenAB() {
    Mutability mu = Mutability.create("frozen");
    StarlarkByteArray b = StarlarkByteArray.of(mu, (byte) 'A', (byte) 'B');
    mu.freeze();
    return b;
  }

  private static String repr(Object x) {
    return Starlark.repr(x, StarlarkSemantics.DEFAULT);
  }

  @Test
  public void frozenBytearrayRejectsEveryMutation() {
    for (String mutation : MUTATIONS) {
      StarlarkByteArray b = frozenAB();
      EvalException e = assertThrows(mutation, EvalException.class, () -> exec(b, mutation));
      assertThat(e).hasMessageThat().contains("trying to mutate a frozen bytearray value");
      assertThat(repr(b)).isEqualTo("b\"AB\"");
    }
  }

  @Test
  public void bytearrayIsImmutableWhileIterated() {
    for (String loop : ImmutableList.of("for c in B:", "for c in B.elems():")) {
      for (String mutation : MUTATIONS) {
        Mutability mu = Mutability.create("iterated");
        StarlarkByteArray b = StarlarkByteArray.of(mu, (byte) 'A', (byte) 'B');
        EvalException e =
            assertThrows(
                loop + " " + mutation,
                EvalException.class,
                () -> exec(b, "def f():", "  " + loop, "    " + mutation, "f()"));
        assertThat(e)
            .hasMessageThat()
            .contains("bytearray value is temporarily immutable due to active for-loop iteration");
        assertThat(repr(b)).isEqualTo("b\"AB\"");
      }
    }
  }

  @Test
  public void bytearrayIsImmutableWhileIteratedByAComprehension() {
    StarlarkByteArray b = StarlarkByteArray.of(Mutability.create("iterated"), (byte) 'A');
    EvalException e = assertThrows(EvalException.class, () -> exec(b, "[B.append(1) for c in B]"));
    assertThat(e).hasMessageThat().contains("temporarily immutable");
  }

  @Test
  public void bytearrayIsMutableAgainAfterTheLoop() throws Exception {
    StarlarkByteArray b = StarlarkByteArray.of(Mutability.create("iterated"), (byte) 'A');
    exec(b, "def f():", "  for c in B:", "    pass", "  B.append(66)", "f()");
    assertThat(repr(b)).isEqualTo("b\"AB\"");
  }

  @Test
  public void copyIsIndependentAndMutable() throws Exception {
    StarlarkByteArray b = frozenAB();
    Module module = exec(b, "c = B.copy()", "c.append(67)", "c.clear()", "c.append(68)");
    assertThat(repr(b)).isEqualTo("b\"AB\"");
    assertThat(repr(module.getGlobal("c"))).isEqualTo("b\"D\"");

    Mutability mu = Mutability.create("mutable");
    StarlarkByteArray m = StarlarkByteArray.of(mu, (byte) 'A');
    Module copied = exec(m, "c = B.copy()", "c.append(67)", "B.append(66)");
    assertThat(repr(m)).isEqualTo("b\"AB\"");
    assertThat(repr(copied.getGlobal("c"))).isEqualTo("b\"AC\"");
  }

  @Test
  public void popReturnsTheUnsignedByte() throws Exception {
    StarlarkByteArray b = StarlarkByteArray.of(Mutability.create("m"), (byte) 0xff);
    assertThat(exec(b, "x = B.pop()").getGlobal("x")).isEqualTo(StarlarkInt.of(255));
  }

  @Test
  public void popTakesPythonIndices() throws Exception {
    StarlarkByteArray b = StarlarkByteArray.of(Mutability.create("m"), (byte) 'A', (byte) 'B', (byte) 'C');
    Module module = exec(b, "x = B.pop(0)", "y = B.pop(-1)");
    assertThat(module.getGlobal("x")).isEqualTo(StarlarkInt.of('A'));
    assertThat(module.getGlobal("y")).isEqualTo(StarlarkInt.of('C'));
    assertThat(repr(b)).isEqualTo("b\"B\"");
    EvalException e = assertThrows(EvalException.class, () -> exec(b, "B.pop(5)"));
    assertThat(e).hasMessageThat().contains("out of range");
  }
}
