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

package com.verygood.security.larky.wasm.conformance;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.verygood.security.larky.wasm.conformance.Fixtures.deadlineIn;
import static com.verygood.security.larky.wasm.conformance.Fixtures.memoryCap;
import static com.verygood.security.larky.wasm.conformance.Fixtures.seeded;
import static com.verygood.security.larky.wasm.conformance.Fixtures.utf8;

import com.verygood.security.larky.wasm.WasmException;
import com.verygood.security.larky.wasm.WasmLimits;
import com.verygood.security.larky.wasm.WasmResult;
import com.verygood.security.larky.wasm.WasmRuntime;
import com.verygood.security.larky.wasm.WasmRuntimes;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Runs every fixture on every runtime and checks that they agree byte for byte: the same exit
 * code, stdout and stderr, or the same {@link WasmException.Kind}. Skipped unless at least two
 * runtimes are on the class path.
 */
@RunWith(Parameterized.class)
public class CrossRuntimeTest {

  /** One fixture run: the module, its stdin and its limits (made per run, for deadlines). */
  record Case(String fixture, byte[] stdin, Supplier<WasmLimits> limits) {}

  @Parameters(name = "{0}")
  public static List<Object[]> cases() {
    List<Object[]> cases = new ArrayList<>();
    add(cases, "echo", "echo", utf8("hello, wasm\n"), WasmLimits::defaults);
    add(cases, "echo-empty", "echo", new byte[0], WasmLimits::defaults);
    byte[] large = new byte[200_000];
    for (int i = 0; i < large.length; i++) {
      large[i] = (byte) (i * 31 + i / 256);
    }
    add(cases, "echo-large", "echo", large, WasmLimits::defaults);
    add(cases, "exit3", "exit3", new byte[0], WasmLimits::defaults);
    add(cases, "trap", "trap", new byte[0], WasmLimits::defaults);
    add(cases, "spin", "spin", new byte[0], () -> deadlineIn(200));
    add(cases, "grow", "grow", new byte[0], WasmLimits::defaults);
    add(cases, "grow-2MiB", "grow", new byte[0], () -> memoryCap(2L << 20));
    add(cases, "bigmem", "bigmem", new byte[0], WasmLimits::defaults);
    add(cases, "flood", "flood", new byte[0], () -> deadlineIn(10_000));
    add(cases, "random-seed-42", "random", new byte[0], () -> seeded(42));
    add(cases, "random-seed-7", "random", new byte[0], () -> seeded(7));
    add(cases, "random_split-seed-7", "random_split", new byte[0], () -> seeded(7));
    add(cases, "clock", "clock", new byte[0], WasmLimits::defaults);
    add(cases, "env", "env", new byte[0], WasmLimits::defaults);
    add(cases, "fresh", "fresh", new byte[0], WasmLimits::defaults);
    add(cases, "nostart", "nostart", new byte[0], WasmLimits::defaults);
    add(cases, "nomemory", "nomemory", new byte[0], WasmLimits::defaults);
    int i = 0;
    for (String input : Fixtures.SAMPLE_ENCRYPT_CASES.keySet()) {
      add(cases, "sample_encrypt-" + i++, Fixtures.SAMPLE_ENCRYPT, utf8(input), WasmLimits::defaults);
    }
    add(cases, "sample_encrypt-bad", Fixtures.SAMPLE_ENCRYPT, utf8("not json"), WasmLimits::defaults);
    return cases;
  }

  private static void add(
      List<Object[]> cases, String name, String fixture, byte[] stdin, Supplier<WasmLimits> limits) {
    cases.add(new Object[] {name, new Case(fixture, stdin, limits)});
  }

  @Parameter(0)
  public String name;

  @Parameter(1)
  public Case testCase;

  /** What a run produced, as text that is equal across runtimes exactly when they agree. */
  private String outcome(WasmRuntime runtime) throws InterruptedException {
    try {
      WasmResult r =
          runtime.compile(Fixtures.wasm(testCase.fixture())).run(testCase.stdin(), testCase.limits().get());
      return "exit "
          + r.exitCode()
          + "\nstdout "
          + HexFormat.of().formatHex(r.stdout())
          + "\nstderr "
          + HexFormat.of().formatHex(r.stderr());
    } catch (WasmException e) {
      return "WasmException " + e.kind();
    }
  }

  @Test
  public void runtimesAgree() throws Exception {
    List<WasmRuntime> runtimes = WasmRuntimes.all();
    Assume.assumeTrue(
        "needs at least two WebAssembly runtimes; found " + runtimes.size(), runtimes.size() >= 2);
    WasmRuntime reference = runtimes.get(0);
    String expected = outcome(reference);
    for (WasmRuntime other : runtimes.subList(1, runtimes.size())) {
      assertWithMessage("%s on %s versus %s", testCase.fixture(), other.name(), reference.name())
          .that(outcome(other))
          .isEqualTo(expected);
    }
  }
}
