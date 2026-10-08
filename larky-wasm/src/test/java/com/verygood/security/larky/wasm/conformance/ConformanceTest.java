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

import com.verygood.security.larky.wasm.WasmRuntime.WasiHostPolicy;
import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import com.verygood.security.larky.wasm.WasmRuntime.WasmException.Kind;
import com.verygood.security.larky.wasm.WasmRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
 * and {@link WasmRuntime.Program#run}. The fixtures are in {@code src/test/resources/wasm}.
 */
@RunWith(Parameterized.class)
public class ConformanceTest {

  private static final long DEFAULT_CAP_PAGES = WasmRuntime.Limits.DEFAULT_MAX_MEMORY_BYTES / 65536;

  @Parameters(name = "{0}")
  public static List<Object[]> runtimes() {
    return Fixtures.runtimeParameters();
  }

  @Parameter(0)
  public String name;

  @Parameter(1)
  public WasmRuntime runtime;

  private WasmRuntime.Result run(String fixture, String stdin, WasmRuntime.Limits limits) throws Exception {
    return Fixtures.run(runtime, fixture, utf8(stdin), limits);
  }

  private WasmRuntime.Result run(String fixture, String stdin) throws Exception {
    return run(fixture, stdin, WasmRuntime.Limits.defaults());
  }

  /** Runs {@code fixture} expecting a {@link WasmException} from {@code run} (not compile). */
  private WasmException runFails(String fixture, byte[] stdin, WasmRuntime.Limits limits)
      throws Exception {
    WasmRuntime.Program program = runtime.compile(Fixtures.wasm(fixture));
    return assertThrows(WasmException.class, () -> program.run(stdin, limits));
  }

  private WasmException compileFails(byte[] wasm) {
    return assertThrows(WasmException.class, () -> runtime.compile(wasm));
  }

  private static void assertOutput(WasmRuntime.Result result, int exitCode, String stdout, String stderr) {
    assertWithMessage("stdout").that(utf8(result.stdout())).isEqualTo(stdout);
    assertWithMessage("stderr").that(utf8(result.stderr())).isEqualTo(stderr);
    assertWithMessage("exit code").that(result.exitCode()).isEqualTo(exitCode);
  }

  @Test
  public void runtimeHasAName() {
    assertThat(runtime.name()).isNotEmpty();
  }

  @Test
  public void hostPolicyAppliesToEachRunOfTheSameProgram() throws Exception {
    WasmRuntime.Program program = runtime.compile(Fixtures.wasm("exit3"));
    var restricted = new WasmRuntime.Limits(1 << 20, 0, 1024, 0L,
        new WasiHostPolicy(Set.of("proc_exit")));
    assertOutput(program.run(new byte[0], restricted), 3, "", "");
    assertOutput(program.run(new byte[0], WasmRuntime.Limits.defaults()), 3, "", "boom");
  }

  @Test
  public void disabledProcExitTrapsOnEveryRuntime() throws Exception {
    var limits = new WasmRuntime.Limits(1 << 20, 0, 1024, 0L, WasiHostPolicy.none());
    assertThat(runFails("exit3", new byte[0], limits).kind()).isEqualTo(Kind.TRAP);
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
    WasmRuntime.Result result = Fixtures.run(runtime, "echo", input, WasmRuntime.Limits.defaults());
    assertThat(result.stdout()).isEqualTo(input);
    assertThat(result.exitCode()).isEqualTo(0);
  }

  @Test
  public void exitCodeAndStderrAreResults() throws Exception {
    assertOutput(run("exit3", ""), 3, "", "boom");
  }

  @Test
  public void eachRunGetsAFreshInstance() throws Exception {
    WasmRuntime.Program program = runtime.compile(Fixtures.wasm("fresh"));
    for (int i = 0; i < 3; i++) {
      assertOutput(program.run(new byte[0], WasmRuntime.Limits.defaults()), 0, "1", "");
    }
  }

  @Test
  public void aProgramRunsConcurrently() throws Exception {
    WasmRuntime.Program program = runtime.compile(Fixtures.wasm("echo"));
    ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
      List<Future<WasmRuntime.Result>> futures = new ArrayList<>();
      for (int i = 0; i < 32; i++) {
        byte[] input = utf8("input " + i);
        futures.add(pool.submit(() -> program.run(input, WasmRuntime.Limits.defaults())));
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
  public void functionsOutsideTheAllowedSetReturnNosysAtOnce() throws Exception {
    // poll_oneoff asks to sleep 5 s; like every function outside WasiHost.RUNTIME_FUNCTIONS it
    // returns NOSYS (52) without doing anything.
    long start = System.nanoTime();
    assertOutput(run("nosys", ""), 0, "52 52 52 52 52 ", "");
    assertWithMessage("milliseconds")
        .that(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start))
        .isLessThan(2000L);
  }

  @Test
  public void allowedDescriptorFunctionsBehaveTheSame() throws Exception {
    // For fds 0..2: fd_fdstat_get's errno and the 24 bytes it wrote (filetype unknown, rights
    // fd_read for stdin and fd_write for stdout and stderr); then fd_fdstat_get(3), fd_seek on
    // 0..2 (ESPIPE), fd_read(1), fd_write(0), fd_write(3) (EBADF), sched_yield, fd_close(3),
    // fd_close(0), and fd_read(0) once closed.
    assertOutput(
        run("fds", ""),
        0,
        "0 000000000000000002000000000000000000000000000000"
            + " 0 000000000000000040000000000000000000000000000000"
            + " 0 000000000000000040000000000000000000000000000000"
            + " 8 70 70 70 8 8 8 0 8 0 8 ",
        "");
  }

  @Test
  public void rejectsAModuleLargerThanTheLimit() throws Exception {
    // echo, padded with a custom section to one byte over MAX_MODULE_BYTES.
    byte[] echo = Fixtures.wasm("echo");
    java.io.ByteArrayOutputStream big = new java.io.ByteArrayOutputStream();
    big.write(echo);
    int payload = WasmRuntime.MAX_MODULE_BYTES - echo.length - 8; // 8: id, size and name bytes
    byte[] header = {0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0, 1, 'x'};
    int size = payload + 2; // the name's length byte and the name
    for (int i = 0; i < 4; i++) {
      header[1 + i] = (byte) ((size >>> (7 * i)) & 0x7f | 0x80);
    }
    header[5] = (byte) (size >>> 28);
    big.write(header);
    big.write(new byte[payload + 1]);
    assertThat(big.size()).isEqualTo(WasmRuntime.MAX_MODULE_BYTES + 1);
    assertThat(compileFails(big.toByteArray()).kind()).isEqualTo(Kind.INVALID_MODULE);
  }

  @Test
  public void rejectsAModuleWithMoreFunctionsThanTheLimit() throws Exception {
    StringBuilder wat = new StringBuilder("(module (memory (export \"memory\") 1)");
    wat.append(" (func (export \"_start\"))");
    for (int i = 1; i < WasmRuntime.MAX_FUNCTIONS; i++) {
      wat.append(" (func)");
    }
    String atLimit = wat + ")";
    assertThat(runtime.compile(run.endive.wabt.Wat2Wasm.parse(atLimit))).isNotNull();
    String overLimit = wat + " (func))";
    assertThat(compileFails(run.endive.wabt.Wat2Wasm.parse(overLimit)).kind())
        .isEqualTo(Kind.INVALID_MODULE);
  }

  @Test
  public void rejectsAStartSection() throws Exception {
    assertThat(compileFails(Fixtures.wasm("startsection")).kind()).isEqualTo(Kind.INVALID_MODULE);
  }

  @Test
  public void rejectsAWasiImportWithTheWrongSignature() throws Exception {
    assertThat(compileFails(Fixtures.wasm("badsig")).kind()).isEqualTo(Kind.INVALID_MODULE);
  }

  @Test
  public void clocksReadZero() throws Exception {
    assertOutput(run("clock", ""), 0, "0\n0\n", "");
  }

  @Test
  public void clocksWasiDoesNotDefineAreInval() throws Exception {
    // EINVAL (28) for clock ids 4 and -1, and nothing written.
    assertOutput(run("clock_badid", ""), 2828, "", "");
  }

  @Test
  public void seededRandomIsSplittableRandomLeastSignificantByteFirst() throws Exception {
    // The bytes of new SplittableRandom(seed).nextLong(), least significant first, twice.
    assertOutput(run("random", "", seeded(42)), 0, "956eeb2f2632d7bd03f166b233e3ef28", "");
    assertOutput(run("random", "", seeded(42)), 0, "956eeb2f2632d7bd03f166b233e3ef28", "");
    assertOutput(run("random", "", seeded(43)), 0, "88ef4feb90ec69ba4b03602e8598de9c", "");
  }

  @Test
  public void seededRandomIsOneStreamAcrossCalls() throws Exception {
    assertOutput(run("random_split", "", seeded(42)), 0, "956eeb2f2632d7bd03f166b233e3ef28", "");
  }

  /** stdin for random_at: ptr and len as little-endian u32s. */
  private static byte[] at(long ptr, long len) {
    return java.nio.ByteBuffer.allocate(8)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        .putInt((int) ptr)
        .putInt((int) len)
        .array();
  }

  @Test
  public void seededRandomFillsLargeBuffersFromOneStream() throws Exception {
    // More than one 64 KiB chunk: the bytes still continue new SplittableRandom(7)'s stream.
    int len = 150_000;
    byte[] expected = new byte[len];
    java.util.SplittableRandom random = new java.util.SplittableRandom(7);
    for (int i = 0; i < len; i += 8) {
      long word = random.nextLong();
      for (int j = 0; j < 8 && i + j < len; j++) {
        expected[i + j] = (byte) (word >>> (8 * j));
      }
    }
    WasmRuntime.Result result =
        runtime.compile(Fixtures.wasm("random_at")).run(at(1024, len), seeded(7));
    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.stdout()).isEqualTo(expected);
  }

  @Test
  public void randomGetUpToTheEndOfMemoryWorks() throws Exception {
    WasmRuntime.Result result = runtime.compile(Fixtures.wasm("random_at")).run(at(4 * 65536 - 16, 16), WasmRuntime.Limits.defaults());
    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.stdout()).hasLength(16);
  }

  @Test
  public void randomGetPastTheEndOfMemoryTrapsWithoutAllocatingIt() throws Exception {
    // A guest-chosen length must not make the host allocate it: 2^31 - 1 and 2^32 - 1 bytes.
    for (long[] c :
        new long[][] {
          {4 * 65536 - 15, 16}, {0, 0x7fff_ffffL}, {0, 0xffff_ffffL}, {0xffff_ffffL, 1}
        }) {
      WasmException e =
          assertThrows(
              WasmException.class,
              () -> runtime.compile(Fixtures.wasm("random_at")).run(at(c[0], c[1]), WasmRuntime.Limits.defaults()));
      assertWithMessage("random_get(%s, %s)", c[0], c[1]).that(e.kind()).isEqualTo(Kind.TRAP);
    }
  }

  @Test
  public void unseededRandomDiffersBetweenRuns() throws Exception {
    WasmRuntime.Result first = run("random", "");
    WasmRuntime.Result second = run("random", "");
    assertThat(first.exitCode()).isEqualTo(0);
    assertThat(utf8(first.stdout())).matches("[0-9a-f]{32}");
    assertThat(utf8(second.stdout())).isNotEqualTo(utf8(first.stdout()));
  }

  // Failures

  @Test
  public void trapIsTrap() throws Exception {
    assertThat(runFails("trap", new byte[0], WasmRuntime.Limits.defaults()).kind()).isEqualTo(Kind.TRAP);
  }

  @Test
  public void deadlineStopsALongRandomGet() throws Exception {
    // One random_get of 64 MiB takes longer than the deadline; the host stops filling.
    long start = System.nanoTime();
    WasmException e = runFails("random_fill", new byte[0], deadlineIn(50));
    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    assertThat(e.kind()).isEqualTo(Kind.TIMEOUT);
    assertWithMessage("milliseconds until TIMEOUT").that(elapsedMs).isLessThan(500L);
  }

  @Test
  public void runPastItsDeadlineIsTimeoutEvenIfItFinishes() throws Exception {
    // The deadline has passed before the run starts; the module would finish at once.
    WasmRuntime.Limits passed =
        new WasmRuntime.Limits(
            WasmRuntime.Limits.DEFAULT_MAX_MEMORY_BYTES,
            System.currentTimeMillis() - 1,
            WasmRuntime.Limits.DEFAULT_MAX_OUTPUT_BYTES,
            null);
    assertThat(runFails("echo", new byte[0], passed).kind()).isEqualTo(Kind.TIMEOUT);
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
    WasmRuntime.Program program = runtime.compile(Fixtures.wasm("spin"));
    AtomicReference<Throwable> thrown = new AtomicReference<>();
    // The deadline is only a backstop so a runtime that ignores interrupts cannot hang the build.
    WasmRuntime.Limits limits = deadlineIn(10_000);
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
  public void aCallerInterruptIsNeverLostToTheDeadline() throws Exception {
    // The caller is interrupted around the moment the deadline passes. Whichever comes first, the
    // caller must see its interrupt: as an InterruptedException, or still set afterwards.
    WasmRuntime.Program program = runtime.compile(Fixtures.wasm("spin"));
    java.util.Random random = new java.util.Random(7);
    for (int i = 0; i < 30; i++) {
      long deadlineMs = 20;
      long interruptAtNanos = TimeUnit.MILLISECONDS.toNanos(deadlineMs - 5) + random.nextInt(10_000_000);
      Thread caller = Thread.currentThread();
      long start = System.nanoTime();
      Thread interrupter =
          new Thread(
              () -> {
                while (System.nanoTime() - start < interruptAtNanos) {
                  Thread.onSpinWait();
                }
                caller.interrupt();
              });
      interrupter.start();
      boolean interruptedException = false;
      try {
        program.run(new byte[0], deadlineIn(deadlineMs));
      } catch (InterruptedException e) {
        interruptedException = true;
      } catch (WasmException e) {
        assertThat(e.kind()).isEqualTo(Kind.TIMEOUT);
      }
      // The interrupt may also come after the run, while waiting here.
      boolean interruptedAfter = false;
      while (true) {
        try {
          interrupter.join();
          break;
        } catch (InterruptedException e) {
          interruptedAfter = true;
        }
      }
      interruptedAfter |= Thread.interrupted();
      assertWithMessage("run %s: InterruptedException, or the interrupt after the run", i)
          .that(interruptedException || interruptedAfter)
          .isTrue();
    }
  }

  @Test
  public void deepNestingDoesNotDependOnTheCallersStack() throws Exception {
    // 1,000 nested calls: more than a 256 KiB stack holds, on any runtime.
    WasmRuntime.Program program = runtime.compile(Fixtures.wasm("nest"));
    byte[] depth = java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(1000).array();
    AtomicReference<Object> outcome = new AtomicReference<>();
    Thread small =
        new Thread(
            null,
            () -> {
              try {
                outcome.set(program.run(depth, WasmRuntime.Limits.defaults()).exitCode());
              } catch (Throwable t) {
                outcome.set(t);
              }
            },
            "small-stack",
            256 << 10);
    small.start();
    small.join();
    assertThat(outcome.get()).isEqualTo(0);
  }

  @Test
  public void initialMemoryOverTheDefaultCapIsMemoryLimit() throws Exception {
    // bigmem declares 2048 pages (128 MiB); the default cap is 1024 (64 MiB).
    WasmException e = runFails("bigmem", new byte[0], WasmRuntime.Limits.defaults());
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
    WasmRuntime.Result result = Fixtures.run(runtime, "echo", input, outputCap(1000));
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

}
