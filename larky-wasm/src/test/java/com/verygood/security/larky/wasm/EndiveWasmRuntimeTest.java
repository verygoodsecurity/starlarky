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

package com.verygood.security.larky.wasm;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import com.verygood.security.larky.wasm.WasmRuntime.WasmException.Kind;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import run.endive.wabt.Wat2Wasm;

@RunWith(Parameterized.class)
public class EndiveWasmRuntimeTest {

  @Parameters(name = "{0}")
  public static List<Object[]> modes() {
    return Arrays.stream(EndiveWasmRuntime.Mode.values()).map(m -> new Object[] {m}).toList();
  }

  private static final String WASI = "\"wasi_snapshot_preview1\"";
  private static final byte[] NOTHING = new byte[0];

  static final String ECHO =
      "(module\n"
          + "  (import " + WASI + " \"fd_read\" (func $fd_read (param i32 i32 i32 i32) (result i32)))\n"
          + "  (import " + WASI + " \"fd_write\" (func $fd_write (param i32 i32 i32 i32) (result i32)))\n"
          + "  (memory (export \"memory\") 1)\n"
          + "  (func (export \"_start\") (local $n i32)\n"
          + "    (loop $l\n"
          + "      (i32.store (i32.const 0) (i32.const 16))\n"
          + "      (i32.store (i32.const 4) (i32.const 1024))\n"
          + "      (drop (call $fd_read (i32.const 0) (i32.const 0) (i32.const 1) (i32.const 8)))\n"
          + "      (local.set $n (i32.load (i32.const 8)))\n"
          + "      (if (i32.gt_u (local.get $n) (i32.const 0))\n"
          + "        (then\n"
          + "          (i32.store (i32.const 4) (local.get $n))\n"
          + "          (drop (call $fd_write (i32.const 1) (i32.const 0) (i32.const 1) (i32.const 8)))\n"
          + "          (br $l))))))";

  static final String EXIT_3 =
      "(module\n"
          + "  (import " + WASI + " \"fd_write\" (func $fd_write (param i32 i32 i32 i32) (result i32)))\n"
          + "  (import " + WASI + " \"proc_exit\" (func $proc_exit (param i32)))\n"
          + "  (memory (export \"memory\") 1)\n"
          + "  (data (i32.const 16) \"boom\\n\")\n"
          + "  (func (export \"_start\")\n"
          + "    (i32.store (i32.const 0) (i32.const 16))\n"
          + "    (i32.store (i32.const 4) (i32.const 5))\n"
          + "    (drop (call $fd_write (i32.const 2) (i32.const 0) (i32.const 1) (i32.const 8)))\n"
          + "    (call $proc_exit (i32.const 3))))";

  static final String UNREACHABLE =
      "(module (memory (export \"memory\") 1) (func (export \"_start\") unreachable))";

  static final String SPIN =
      "(module (memory (export \"memory\") 1) (func (export \"_start\") (loop $l (br $l))))";

  static final String GROW =
      "(module\n"
          + "  (import " + WASI + " \"proc_exit\" (func $proc_exit (param i32)))\n"
          + "  (memory (export \"memory\") 1)\n"
          + "  (func (export \"_start\")\n"
          + "    (block $done (loop $l\n"
          + "      (br_if $done (i32.eq (memory.grow (i32.const 1)) (i32.const -1)))\n"
          + "      (br $l)))\n"
          + "    (call $proc_exit (memory.size))))";

  static final String BIG_MEMORY =
      "(module (memory (export \"memory\") 20) (func (export \"_start\")))";

  static final String FLOOD =
      "(module\n"
          + "  (import " + WASI + " \"fd_write\" (func $fd_write (param i32 i32 i32 i32) (result i32)))\n"
          + "  (memory (export \"memory\") 1)\n"
          + "  (func (export \"_start\")\n"
          + "    (i32.store (i32.const 0) (i32.const 16))\n"
          + "    (i32.store (i32.const 4) (i32.const 1024))\n"
          + "    (loop $l\n"
          + "      (drop (call $fd_write (i32.const 1) (i32.const 0) (i32.const 1) (i32.const 8)))\n"
          + "      (br $l))))";

  static final String RANDOM =
      "(module\n"
          + "  (import " + WASI + " \"random_get\" (func $random_get (param i32 i32) (result i32)))\n"
          + "  (import " + WASI + " \"fd_write\" (func $fd_write (param i32 i32 i32 i32) (result i32)))\n"
          + "  (memory (export \"memory\") 1)\n"
          + "  (func (export \"_start\")\n"
          + "    (drop (call $random_get (i32.const 16) (i32.const 4)))\n"
          + "    (drop (call $random_get (i32.const 20) (i32.const 28)))\n"
          + "    (i32.store (i32.const 0) (i32.const 16))\n"
          + "    (i32.store (i32.const 4) (i32.const 32))\n"
          + "    (drop (call $fd_write (i32.const 1) (i32.const 0) (i32.const 1) (i32.const 8)))))";

  /** Writes clock_time_get's results for the realtime (0) and monotonic (1) clocks. */
  static final String CLOCK =
      "(module\n"
          + "  (import " + WASI + " \"clock_time_get\" (func $clock (param i32 i64 i32) (result i32)))\n"
          + "  (import " + WASI + " \"fd_write\" (func $fd_write (param i32 i32 i32 i32) (result i32)))\n"
          + "  (memory (export \"memory\") 1)\n"
          + "  (func (export \"_start\")\n"
          + "    (i64.store (i32.const 16) (i64.const -1))\n"
          + "    (i64.store (i32.const 24) (i64.const -1))\n"
          + "    (drop (call $clock (i32.const 0) (i64.const 1) (i32.const 16)))\n"
          + "    (drop (call $clock (i32.const 1) (i64.const 1) (i32.const 24)))\n"
          + "    (i32.store (i32.const 0) (i32.const 16))\n"
          + "    (i32.store (i32.const 4) (i32.const 16))\n"
          + "    (drop (call $fd_write (i32.const 1) (i32.const 0) (i32.const 1) (i32.const 8)))))";

  static final String RECURSE =
      "(module (memory (export \"memory\") 1)"
          + " (func $f (call $f)) (func (export \"_start\") (call $f)))";

  static final String OUT_OF_BOUNDS =
      "(module (memory (export \"memory\") 1)"
          + " (func (export \"_start\") (drop (i32.load (i32.const 65536)))))";

  static final String NO_START = "(module (memory (export \"memory\") 1) (func (export \"main\")))";

  static final String NO_MEMORY = "(module (memory 1) (func (export \"_start\")))";

  static final String FOREIGN_IMPORT =
      "(module (import \"env\" \"f\" (func)) (memory (export \"memory\") 1)"
          + " (func (export \"_start\")))";

  private final EndiveWasmRuntime.Mode mode;
  private final WasmRuntime runtime;

  public EndiveWasmRuntimeTest(EndiveWasmRuntime.Mode mode) {
    this.mode = mode;
    this.runtime = new EndiveWasmRuntime(mode);
  }

  private WasmRuntime.Program compile(String wat) throws WasmException {
    return runtime.compile(Wat2Wasm.parse(wat));
  }

  private static WasmRuntime.Limits limits(long memoryBytes, long deadline, int output, Long seed) {
    return new WasmRuntime.Limits(memoryBytes, deadline, output, seed);
  }

  @Test
  public void registeredByName() throws Exception {
    assertThat(WasmRuntime.byName("endive")).isInstanceOf(EndiveWasmRuntime.class);
  }

  @Test
  public void echoesStdinToStdout() throws Exception {
    WasmRuntime.Program echo = compile(ECHO);
    byte[] input = "hello, wasm".getBytes(UTF_8);
    for (int i = 0; i < 3; i++) {
      WasmRuntime.Result result = echo.run(input, WasmRuntime.Limits.defaults());
      assertThat(result.exitCode()).isEqualTo(0);
      assertThat(result.stdout()).isEqualTo(input);
      assertThat(result.stderr()).isEmpty();
    }
    byte[] big = new byte[5000];
    Arrays.fill(big, (byte) 'x');
    assertThat(echo.run(big, WasmRuntime.Limits.defaults()).stdout()).isEqualTo(big);
  }

  @Test
  public void concurrentRunsShareOneProgram() throws Exception {
    WasmRuntime.Program echo = compile(ECHO);
    java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(8);
    try {
      List<java.util.concurrent.Future<byte[]>> futures = new java.util.ArrayList<>();
      for (int i = 0; i < 200; i++) {
        byte[] input = ("input-" + i).getBytes(UTF_8);
        futures.add(pool.submit(() -> echo.run(input, WasmRuntime.Limits.defaults()).stdout()));
      }
      for (int i = 0; i < 200; i++) {
        assertThat(new String(futures.get(i).get(), UTF_8)).isEqualTo("input-" + i);
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  public void procExitReturnsTheCode() throws Exception {
    WasmRuntime.Result result = compile(EXIT_3).run(NOTHING, WasmRuntime.Limits.defaults());
    assertThat(result.exitCode()).isEqualTo(3);
    assertThat(new String(result.stderr(), UTF_8)).isEqualTo("boom\n");
    assertThat(result.stdout()).isEmpty();
  }

  @Test
  public void unreachableTraps() throws Exception {
    WasmException e =
        assertThrows(WasmException.class, () -> compile(UNREACHABLE).run(NOTHING, WasmRuntime.Limits.defaults()));
    assertThat(e.kind()).isEqualTo(Kind.TRAP);
    assertThat(e.getCause()).isNotNull();
  }

  @Test
  public void stackOverflowAndOutOfBoundsTrap() throws Exception {
    for (String wat : List.of(RECURSE, OUT_OF_BOUNDS)) {
      WasmException e =
          assertThrows(WasmException.class, () -> compile(wat).run(NOTHING, WasmRuntime.Limits.defaults()));
      assertThat(e.kind()).isEqualTo(Kind.TRAP);
    }
  }

  @Test
  public void modeSelectsTheMachine() throws Exception {
    EndiveWasmProgram program = (EndiveWasmProgram) compile(SPIN);
    String machine = program.newInstanceForTest().getMachine().getClass().getName();
    if (mode == EndiveWasmRuntime.Mode.INTERPRETER) {
      // On Java 25+ with the Vector API, the SIMD interpreter (a subclass) interprets every module.
      assertThat(machine)
          .isAnyOf("run.endive.runtime.InterpreterMachine", "run.endive.simd.SimdInterpreterMachine");
    } else {
      assertThat(machine)
          .isNoneOf("run.endive.runtime.InterpreterMachine", "run.endive.simd.SimdInterpreterMachine");
    }
  }

  @Test
  public void infiniteLoopTimesOut() throws Exception {
    WasmRuntime.Program spin = compile(SPIN);
    long start = System.nanoTime();
    long deadline = System.currentTimeMillis() + 200;
    WasmException e =
        assertThrows(
            WasmException.class,
            () -> spin.run(NOTHING, limits(1 << 20, deadline, 1024, null)));
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
    assertThat(e.kind()).isEqualTo(Kind.TIMEOUT);
    assertThat(elapsedMs).isLessThan(2000L);
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
    // The thread is usable again: a normal run afterwards succeeds.
    assertThat(compile(EXIT_3).run(NOTHING, WasmRuntime.Limits.defaults()).exitCode()).isEqualTo(3);
  }

  @Test
  public void pastDeadlineTimesOutImmediately() throws Exception {
    WasmException e =
        assertThrows(
            WasmException.class,
            () -> compile(SPIN).run(NOTHING, limits(1 << 20, System.currentTimeMillis() - 1, 1024, null)));
    assertThat(e.kind()).isEqualTo(Kind.TIMEOUT);
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }

  @Test
  public void deadlineDoesNotLeakIntoLaterRuns() throws Exception {
    WasmRuntime.Program exit = compile(EXIT_3);
    for (int i = 0; i < 50; i++) {
      long deadline = System.currentTimeMillis() + 1;
      try {
        exit.run(NOTHING, limits(1 << 20, deadline, 1024, null));
      } catch (WasmException e) {
        assertThat(e.kind()).isEqualTo(Kind.TIMEOUT);
      }
      assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }
  }

  @Test
  public void memoryGrowStopsAtTheCap() throws Exception {
    WasmRuntime.Program grow = compile(GROW);
    assertThat(grow.run(NOTHING, limits(16 * 65536, 0, 1024, null)).exitCode()).isEqualTo(16);
    // Rounded down to whole pages.
    assertThat(grow.run(NOTHING, limits(5 * 65536 + 100, 0, 1024, null)).exitCode()).isEqualTo(5);
  }

  @Test
  public void initialMemoryOverTheCap() throws Exception {
    WasmRuntime.Program big = compile(BIG_MEMORY);
    WasmException e =
        assertThrows(WasmException.class, () -> big.run(NOTHING, limits(16 * 65536, 0, 1024, null)));
    assertThat(e.kind()).isEqualTo(Kind.MEMORY_LIMIT);
    assertThat(big.run(NOTHING, limits(20 * 65536, 0, 1024, null)).exitCode()).isEqualTo(0);
  }

  @Test
  public void outputFloodHitsTheLimit() throws Exception {
    WasmException e =
        assertThrows(
            WasmException.class, () -> compile(FLOOD).run(NOTHING, limits(1 << 20, 0, 10_000, null)));
    assertThat(e.kind()).isEqualTo(Kind.OUTPUT_LIMIT);
  }

  @Test
  public void outputExactlyAtTheLimitIsAllowed() throws Exception {
    byte[] input = new byte[100];
    WasmRuntime.Result result = compile(ECHO).run(input, limits(1 << 20, 0, 100, null));
    assertThat(result.stdout()).hasLength(100);
    WasmException e =
        assertThrows(
            WasmException.class, () -> compile(ECHO).run(input, limits(1 << 20, 0, 99, null)));
    assertThat(e.kind()).isEqualTo(Kind.OUTPUT_LIMIT);
  }

  @Test
  public void seededRandomIsDeterministic() throws Exception {
    WasmRuntime.Program random = compile(RANDOM);
    byte[] first = random.run(NOTHING, limits(1 << 20, 0, 1024, 42L)).stdout();
    byte[] second = random.run(NOTHING, limits(1 << 20, 0, 1024, 42L)).stdout();
    byte[] other = random.run(NOTHING, limits(1 << 20, 0, 1024, 43L)).stdout();
    assertThat(first).hasLength(32);
    assertThat(second).isEqualTo(first);
    assertThat(other).isNotEqualTo(first);
    // The documented stream: SplittableRandom(seed).nextLong(), little-endian, continuous.
    byte[] expected = new byte[32];
    new EndiveWasmProgram.DeterministicBytes(42L).fill(expected);
    assertThat(first).isEqualTo(expected);
    java.util.SplittableRandom r = new java.util.SplittableRandom(42L);
    long l = r.nextLong();
    for (int i = 0; i < 8; i++) {
      assertThat(first[i]).isEqualTo((byte) (l >>> (8 * i)));
    }
  }

  @Test
  public void unseededRandomVaries() throws Exception {
    WasmRuntime.Program random = compile(RANDOM);
    byte[] first = random.run(NOTHING, WasmRuntime.Limits.defaults()).stdout();
    byte[] second = random.run(NOTHING, WasmRuntime.Limits.defaults()).stdout();
    assertThat(first).hasLength(32);
    assertThat(second).isNotEqualTo(first);
  }

  @Test
  public void clockReturnsZero() throws Exception {
    byte[] out = compile(CLOCK).run(NOTHING, WasmRuntime.Limits.defaults()).stdout();
    assertThat(out).isEqualTo(new byte[16]);
  }

  @Test
  public void invalidModules() throws Exception {
    for (String wat : List.of(NO_START, NO_MEMORY, FOREIGN_IMPORT)) {
      WasmException e = assertThrows(WasmException.class, () -> compile(wat));
      assertThat(e.kind()).isEqualTo(Kind.INVALID_MODULE);
    }
    WasmException e =
        assertThrows(WasmException.class, () -> runtime.compile(new byte[] {0, 'a', 's', 'm', 9}));
    assertThat(e.kind()).isEqualTo(Kind.INVALID_MODULE);
  }

  @Test
  public void externalInterruptPropagates() throws Exception {
    WasmRuntime.Program spin = compile(SPIN);
    Thread runner = Thread.currentThread();
    Thread interrupter =
        new Thread(
            () -> {
              try {
                Thread.sleep(100);
              } catch (InterruptedException ignored) {
                return;
              }
              runner.interrupt();
            });
    interrupter.start();
    long start = System.nanoTime();
    assertThrows(InterruptedException.class, () -> spin.run(NOTHING, WasmRuntime.Limits.defaults()));
    interrupter.join();
    assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(2000L);
    Thread.interrupted();
  }

  @Test
  public void interruptedBeforeRunPropagates() throws Exception {
    WasmRuntime.Program exit = compile(EXIT_3);
    Thread.currentThread().interrupt();
    assertThrows(InterruptedException.class, () -> exit.run(NOTHING, WasmRuntime.Limits.defaults()));
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }
}
