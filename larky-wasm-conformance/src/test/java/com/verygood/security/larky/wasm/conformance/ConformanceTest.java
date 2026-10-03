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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.verygood.security.larky.wasm.conformance.Fixtures.deadlineIn;
import static com.verygood.security.larky.wasm.conformance.Fixtures.memoryCap;
import static com.verygood.security.larky.wasm.conformance.Fixtures.outputCap;
import static com.verygood.security.larky.wasm.conformance.Fixtures.seeded;
import static com.verygood.security.larky.wasm.conformance.Fixtures.utf8;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.wasm.WasmException;
import com.verygood.security.larky.wasm.WasmException.Kind;
import com.verygood.security.larky.wasm.WasmLimits;
import com.verygood.security.larky.wasm.WasmProgram;
import com.verygood.security.larky.wasm.WasmResult;
import com.verygood.security.larky.wasm.WasmRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Checks each {@link WasmRuntime} on the class path against the contract in {@link WasmRuntime}
 * and {@link WasmProgram#run}. The fixtures are in {@code src/test/resources/wasm}.
 */
@RunWith(Parameterized.class)
public class ConformanceTest {

  private static final long DEFAULT_CAP_PAGES = WasmLimits.DEFAULT_MAX_MEMORY_BYTES / 65536;

  @Parameters(name = "{0}")
  public static List<Object[]> runtimes() {
    return Fixtures.runtimeParameters();
  }

  @Parameter(0)
  public String name;

  @Parameter(1)
  public WasmRuntime runtime;

  private WasmResult run(String fixture, String stdin, WasmLimits limits) throws Exception {
    return Fixtures.run(runtime, fixture, utf8(stdin), limits);
  }

  private WasmResult run(String fixture, String stdin) throws Exception {
    return run(fixture, stdin, WasmLimits.defaults());
  }

  /** Runs {@code fixture} expecting a {@link WasmException} from {@code run} (not compile). */
  private WasmException runFails(String fixture, byte[] stdin, WasmLimits limits)
      throws Exception {
    WasmProgram program = runtime.compile(Fixtures.wasm(fixture));
    return assertThrows(WasmException.class, () -> program.run(stdin, limits));
  }

  private WasmException compileFails(byte[] wasm) {
    return assertThrows(WasmException.class, () -> runtime.compile(wasm));
  }

  private static void assertOutput(WasmResult result, int exitCode, String stdout, String stderr) {
    assertWithMessage("stdout").that(utf8(result.stdout())).isEqualTo(stdout);
    assertWithMessage("stderr").that(utf8(result.stderr())).isEqualTo(stderr);
    assertWithMessage("exit code").that(result.exitCode()).isEqualTo(exitCode);
  }

  @Test
  public void runtimeHasAName() {
    assertThat(runtime.name()).isNotEmpty();
  }

  // stdin and stdout

  @Test
  public void echoCopiesStdinToStdout() throws Exception {
    assertOutput(run("echo", "hello, wasm\n"), 0, "hello, wasm\n", "");
  }

  @Test
  public void echoWithEmptyStdin() throws Exception {
    assertOutput(run("echo", ""), 0, "", "");
  }

  @Test
  public void echoCopiesEveryByteValueOfALargeInput() throws Exception {
    byte[] input = new byte[300_000];
    for (int i = 0; i < input.length; i++) {
      input[i] = (byte) (i * 31 + i / 256);
    }
    WasmResult result = Fixtures.run(runtime, "echo", input, WasmLimits.defaults());
    assertThat(result.stdout()).isEqualTo(input);
    assertThat(result.exitCode()).isEqualTo(0);
  }

  @Test
  public void exitCodeAndStderrAreResults() throws Exception {
    assertOutput(run("exit3", ""), 3, "", "boom");
  }

  @Test
  public void eachRunGetsAFreshInstance() throws Exception {
    WasmProgram program = runtime.compile(Fixtures.wasm("fresh"));
    for (int i = 0; i < 3; i++) {
      assertOutput(program.run(new byte[0], WasmLimits.defaults()), 0, "1", "");
    }
  }

  @Test
  public void aProgramRunsConcurrently() throws Exception {
    WasmProgram program = runtime.compile(Fixtures.wasm("echo"));
    ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
      List<Future<WasmResult>> futures = new ArrayList<>();
      for (int i = 0; i < 32; i++) {
        byte[] input = utf8("input " + i);
        futures.add(pool.submit(() -> program.run(input, WasmLimits.defaults())));
      }
      for (int i = 0; i < futures.size(); i++) {
        assertThat(utf8(futures.get(i).get(30, TimeUnit.SECONDS).stdout())).isEqualTo("input " + i);
      }
    } finally {
      pool.shutdownNow();
    }
  }

  // The sandbox: argv ["module"], no environment, no preopens, clock 0, seedable random.

  @Test
  public void argvIsModuleAndThereIsNoEnvironmentOrPreopen() throws Exception {
    // argc 1, argv buffer "module\0" (7), no environment, fd_prestat_get(3) = EBADF (8).
    assertOutput(run("env", ""), 0, "1 7 0 0 8\nmodule", "");
  }

  @Test
  public void clocksReadZero() throws Exception {
    assertOutput(run("clock", ""), 0, "0\n0\n", "");
  }

  @Test
  public void seededRandomIsRepeatable() throws Exception {
    String first = utf8(run("random", "", seeded(42)).stdout());
    assertThat(first).matches("[0-9a-f]{32}");
    assertThat(utf8(run("random", "", seeded(42)).stdout())).isEqualTo(first);
    assertThat(utf8(run("random", "", seeded(43)).stdout())).isNotEqualTo(first);
  }

  @Test
  public void unseededRandomDiffersBetweenRuns() throws Exception {
    WasmResult first = run("random", "");
    WasmResult second = run("random", "");
    assertThat(first.exitCode()).isEqualTo(0);
    assertThat(utf8(first.stdout())).matches("[0-9a-f]{32}");
    assertThat(utf8(second.stdout())).isNotEqualTo(utf8(first.stdout()));
  }

  // Failures

  @Test
  public void trapIsTrap() throws Exception {
    assertThat(runFails("trap", new byte[0], WasmLimits.defaults()).kind()).isEqualTo(Kind.TRAP);
  }

  @Test
  public void deadlineStopsAnInfiniteLoop() throws Exception {
    long start = System.nanoTime();
    WasmException e = runFails("spin", new byte[0], deadlineIn(200));
    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    assertThat(e.kind()).isEqualTo(Kind.TIMEOUT);
    assertWithMessage("milliseconds until TIMEOUT").that(elapsedMs).isLessThan(2000L);
  }

  @Test
  public void interruptStopsAnInfiniteLoop() throws Exception {
    WasmProgram program = runtime.compile(Fixtures.wasm("spin"));
    AtomicReference<Throwable> thrown = new AtomicReference<>();
    // The deadline is only a backstop so a runtime that ignores interrupts cannot hang the build.
    WasmLimits limits = deadlineIn(10_000);
    Thread thread =
        new Thread(
            () -> {
              try {
                program.run(new byte[0], limits);
              } catch (Throwable t) {
                thrown.set(t);
              }
            });
    thread.start();
    Thread.sleep(200);
    long start = System.nanoTime();
    thread.interrupt();
    thread.join(15_000);
    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    assertThat(thread.isAlive()).isFalse();
    assertThat(thrown.get()).isInstanceOf(InterruptedException.class);
    assertWithMessage("milliseconds from interrupt to return").that(elapsedMs).isLessThan(2000L);
  }

  @Test
  public void initialMemoryOverTheDefaultCapIsMemoryLimit() throws Exception {
    // bigmem declares 2048 pages (128 MiB); the default cap is 1024 (64 MiB).
    WasmException e = runFails("bigmem", new byte[0], WasmLimits.defaults());
    assertThat(e.kind()).isEqualTo(Kind.MEMORY_LIMIT);
  }

  @Test
  public void initialMemoryExactlyAtTheCapRuns() throws Exception {
    assertOutput(
        Fixtures.run(runtime, "bigmem", new byte[0], memoryCap(2048L * 65536)), 0, "", "");
  }

  @Test
  public void memoryGrowStopsAtTheDefaultCap() throws Exception {
    assertThat(DEFAULT_CAP_PAGES).isEqualTo(1024);
    assertOutput(run("grow", ""), 1024, "1024", "");
  }

  @Test
  public void memoryCapIsRoundedDownToPages() throws Exception {
    // 2 MiB + 65535 bytes is 32 whole pages.
    assertOutput(run("grow", "", memoryCap((2L << 20) + 65535)), 32, "32", "");
  }

  @Test
  public void endlessStdoutIsOutputLimit() throws Exception {
    // The deadline is only a backstop for a runtime that never enforces the output limit.
    WasmException e = runFails("flood", new byte[0], deadlineIn(10_000));
    assertThat(e.kind()).isEqualTo(Kind.OUTPUT_LIMIT);
  }

  @Test
  public void outputLimitAllowsExactlyTheLimit() throws Exception {
    byte[] input = new byte[1000];
    java.util.Arrays.fill(input, (byte) 'a');
    WasmResult result = Fixtures.run(runtime, "echo", input, outputCap(1000));
    assertThat(result.stdout()).isEqualTo(input);
    WasmException e = runFails("echo", input, outputCap(999));
    assertThat(e.kind()).isEqualTo(Kind.OUTPUT_LIMIT);
  }

  @Test
  public void moduleWithoutStartIsInvalid() {
    assertThat(compileFails(Fixtures.wasm("nostart")).kind()).isEqualTo(Kind.INVALID_MODULE);
  }

  @Test
  public void moduleWithoutExportedMemoryIsInvalid() {
    assertThat(compileFails(Fixtures.wasm("nomemory")).kind()).isEqualTo(Kind.INVALID_MODULE);
  }

  @Test
  public void bytesThatAreNotAModuleAreInvalid() {
    assertThat(compileFails(new byte[0]).kind()).isEqualTo(Kind.INVALID_MODULE);
    assertThat(compileFails(utf8("(module)")).kind()).isEqualTo(Kind.INVALID_MODULE);
    byte[] truncated = java.util.Arrays.copyOf(Fixtures.wasm("echo"), 20);
    assertThat(compileFails(truncated).kind()).isEqualTo(Kind.INVALID_MODULE);
  }

  // JavaScript compiled with Javy

  @Test
  public void javyModuleProducesTheRecordedOutputs() throws Exception {
    WasmProgram program = runtime.compile(Fixtures.wasm(Fixtures.SAMPLE_ENCRYPT));
    for (Map.Entry<String, String> c : Fixtures.SAMPLE_ENCRYPT_CASES.entrySet()) {
      WasmResult result = program.run(utf8(c.getKey()), WasmLimits.defaults());
      assertWithMessage("stdout for %s", c.getKey())
          .that(utf8(result.stdout()))
          .isEqualTo(c.getValue());
      assertWithMessage("exit code for %s", c.getKey()).that(result.exitCode()).isEqualTo(0);
    }
  }

  @Test
  public void javyModuleReportsBadInputOnStderrAndStdout() throws Exception {
    WasmResult result = run(Fixtures.SAMPLE_ENCRYPT, "not json");
    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(utf8(result.stdout())).startsWith("{\"error\":\"input is not JSON: ");
    assertThat(utf8(result.stderr())).startsWith("sample_encrypt: input is not JSON: ");
  }

  @Test
  public void javyModuleIsDeterministicAndKeyed() throws Exception {
    String a = "{\"pan\": \"4111111111111111\", \"key\": \"k1\"}";
    String b = "{\"pan\": \"4111111111111111\", \"key\": \"k2\"}";
    String first = utf8(run(Fixtures.SAMPLE_ENCRYPT, a).stdout());
    assertThat(utf8(run(Fixtures.SAMPLE_ENCRYPT, a).stdout())).isEqualTo(first);
    assertThat(utf8(run(Fixtures.SAMPLE_ENCRYPT, b).stdout()))
        .isEqualTo("{\"encrypted\":\"6481209987721111\",\"keyId\":\"953d7c08\"}");
  }
}
