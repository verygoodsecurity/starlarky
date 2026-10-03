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

package com.verygood.security.larky.wasm.graal;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.wasm.WasmException;
import com.verygood.security.larky.wasm.WasmLimits;
import com.verygood.security.larky.wasm.WasmProgram;
import com.verygood.security.larky.wasm.WasmResult;
import com.verygood.security.larky.wasm.WasmRuntime;
import com.verygood.security.larky.wasm.WasmRuntimes;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import run.endive.wabt.Wat2Wasm;

@RunWith(JUnit4.class)
public final class GraalWasmRuntimeTest {

  static final String WASI = "\"wasi_snapshot_preview1\"";

  static final String IMPORTS =
      "(import " + WASI + " \"fd_read\" (func $fd_read (param i32 i32 i32 i32) (result i32)))\n"
          + "(import " + WASI + " \"fd_write\" (func $fd_write (param i32 i32 i32 i32) (result i32)))\n"
          + "(import " + WASI + " \"proc_exit\" (func $proc_exit (param i32)))\n"
          + "(import " + WASI + " \"random_get\" (func $random_get (param i32 i32) (result i32)))\n"
          + "(import " + WASI + " \"clock_time_get\" (func $clock_time_get (param i32 i64 i32) (result i32)))\n"
          + "(import " + WASI + " \"args_sizes_get\" (func $args_sizes_get (param i32 i32) (result i32)))\n"
          + "(import " + WASI + " \"environ_sizes_get\" (func $environ_sizes_get (param i32 i32) (result i32)))\n"
          // write(fd, ptr, len): one iovec at address 0
          + "(func $write (param $fd i32) (param $ptr i32) (param $len i32) (result i32)\n"
          + "  (i32.store (i32.const 0) (local.get $ptr))\n"
          + "  (i32.store (i32.const 4) (local.get $len))\n"
          + "  (call $fd_write (local.get $fd) (i32.const 0) (i32.const 1) (i32.const 8)))\n";

  static final String ECHO =
      "(module " + IMPORTS
          + "(memory (export \"memory\") 1)\n"
          + "(func (export \"_start\")\n"
          + "  (loop $l\n"
          + "    (i32.store (i32.const 0) (i32.const 1024))\n"
          + "    (i32.store (i32.const 4) (i32.const 4096))\n"
          + "    (drop (call $fd_read (i32.const 0) (i32.const 0) (i32.const 1) (i32.const 16)))\n"
          + "    (if (i32.gt_u (i32.load (i32.const 16)) (i32.const 0))\n"
          + "      (then\n"
          + "        (drop (call $write (i32.const 1) (i32.const 1024) (i32.load (i32.const 16))))\n"
          + "        (br $l))))))";

  static final WasmRuntime RUNTIME = new GraalWasmRuntime();

  static byte[] wat(String wat) {
    return Wat2Wasm.parse(wat);
  }

  static WasmProgram compile(String wat) throws WasmException {
    return RUNTIME.compile(wat(wat));
  }

  static WasmProgram start(String memory, String body) throws WasmException {
    return compile(
        "(module " + IMPORTS + memory + "\n(func (export \"_start\") " + body + "))");
  }

  static WasmLimits deadlineIn(long ms) {
    return new WasmLimits(
        WasmLimits.DEFAULT_MAX_MEMORY_BYTES,
        System.currentTimeMillis() + ms,
        WasmLimits.DEFAULT_MAX_OUTPUT_BYTES,
        null);
  }

  @Test
  public void registeredByName() throws Exception {
    assertThat(WasmRuntimes.byName("graal")).isInstanceOf(GraalWasmRuntime.class);
  }

  @Test
  public void echoesStdinToStdout() throws Exception {
    WasmProgram echo = compile(ECHO);
    byte[] input = "hello, wasm\n".repeat(1000).getBytes(UTF_8);
    WasmResult result = echo.run(input, WasmLimits.defaults());
    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.stdout()).isEqualTo(input);
    assertThat(result.stderr()).isEmpty();
    // A program runs again from a fresh instance.
    assertThat(echo.run("again".getBytes(UTF_8), WasmLimits.defaults()).stdout())
        .isEqualTo("again".getBytes(UTF_8));
  }

  @Test
  public void procExitReturnsCodeAndStderr() throws Exception {
    WasmProgram program =
        start(
            "(memory (export \"memory\") 1) (data (i32.const 100) \"bad input\\n\")",
            "(drop (call $write (i32.const 2) (i32.const 100) (i32.const 10)))"
                + "(call $proc_exit (i32.const 3))");
    WasmResult result = program.run(new byte[0], WasmLimits.defaults());
    assertThat(result.exitCode()).isEqualTo(3);
    assertThat(new String(result.stderr(), UTF_8)).isEqualTo("bad input\n");
    assertThat(result.stdout()).isEmpty();
  }

  @Test
  public void unreachableTraps() throws Exception {
    WasmProgram program = start("(memory (export \"memory\") 1)", "unreachable");
    WasmException e =
        assertThrows(WasmException.class, () -> program.run(new byte[0], WasmLimits.defaults()));
    assertThat(e.kind()).isEqualTo(WasmException.Kind.TRAP);
    assertThat(e.getCause()).isNotNull();
  }

  @Test
  public void infiniteLoopTimesOut() throws Exception {
    WasmProgram program = start("(memory (export \"memory\") 1)", "(loop $l (br $l))");
    long start = System.nanoTime();
    WasmException e =
        assertThrows(WasmException.class, () -> program.run(new byte[0], deadlineIn(200)));
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
    assertThat(e.kind()).isEqualTo(WasmException.Kind.TIMEOUT);
    assertThat(elapsedMs).isAtLeast(150L);
    assertThat(elapsedMs).isLessThan(2000L);
  }

  @Test
  public void memoryGrowStopsAtCap() throws Exception {
    WasmProgram program =
        start(
            "(memory (export \"memory\") 1)",
            "(block $done (loop $l"
                + " (br_if $done (i32.eq (memory.grow (i32.const 1)) (i32.const -1)))"
                + " (br $l)))"
                + "(call $proc_exit (memory.size))");
    WasmLimits limits = new WasmLimits(16 * 65536, 0, 1024, null);
    assertThat(program.run(new byte[0], limits).exitCode()).isEqualTo(16);
    // Rounded down to whole pages.
    WasmLimits partial = new WasmLimits(5 * 65536 + 100, 0, 1024, null);
    assertThat(program.run(new byte[0], partial).exitCode()).isEqualTo(5);
    // The module's own maximum still applies under a larger cap.
    WasmProgram ownMax =
        start(
            "(memory (export \"memory\") 1 3)",
            "(block $done (loop $l"
                + " (br_if $done (i32.eq (memory.grow (i32.const 1)) (i32.const -1)))"
                + " (br $l)))"
                + "(call $proc_exit (memory.size))");
    assertThat(ownMax.run(new byte[0], limits).exitCode()).isEqualTo(3);
  }

  @Test
  public void initialMemoryOverCapFails() throws Exception {
    WasmProgram program = start("(memory (export \"memory\") 20)", "nop");
    WasmLimits limits = new WasmLimits(16 * 65536, 0, 1024, null);
    WasmException e = assertThrows(WasmException.class, () -> program.run(new byte[0], limits));
    assertThat(e.kind()).isEqualTo(WasmException.Kind.MEMORY_LIMIT);
    // The same program runs under a larger cap.
    assertThat(program.run(new byte[0], WasmLimits.defaults()).exitCode()).isEqualTo(0);
  }

  @Test
  public void outputFloodHitsLimit() throws Exception {
    WasmProgram program =
        start(
            "(memory (export \"memory\") 1)",
            "(loop $l (drop (call $write (i32.const 1) (i32.const 1024) (i32.const 1000))) (br $l))");
    WasmLimits limits = new WasmLimits(WasmLimits.DEFAULT_MAX_MEMORY_BYTES, 0, 4096, null);
    WasmException e = assertThrows(WasmException.class, () -> program.run(new byte[0], limits));
    assertThat(e.kind()).isEqualTo(WasmException.Kind.OUTPUT_LIMIT);
  }

  @Test
  public void outputExactlyAtLimitIsAllowed() throws Exception {
    WasmProgram program =
        start(
            "(memory (export \"memory\") 1)",
            "(drop (call $write (i32.const 1) (i32.const 1024) (i32.const 4096)))");
    WasmLimits limits = new WasmLimits(WasmLimits.DEFAULT_MAX_MEMORY_BYTES, 0, 4096, null);
    assertThat(program.run(new byte[0], limits).stdout()).hasLength(4096);
  }

  static final String RANDOM_16 =
      "(drop (call $random_get (i32.const 1024) (i32.const 16)))"
          + "(drop (call $write (i32.const 1) (i32.const 1024) (i32.const 16)))";

  @Test
  public void seededRandomIsDeterministic() throws Exception {
    WasmProgram program = start("(memory (export \"memory\") 1)", RANDOM_16);
    WasmLimits seed42 = new WasmLimits(WasmLimits.DEFAULT_MAX_MEMORY_BYTES, 0, 1024, 42L);
    WasmLimits seed43 = new WasmLimits(WasmLimits.DEFAULT_MAX_MEMORY_BYTES, 0, 1024, 43L);
    byte[] a = program.run(new byte[0], seed42).stdout();
    byte[] b = program.run(new byte[0], seed42).stdout();
    byte[] c = program.run(new byte[0], seed43).stdout();
    assertThat(a).hasLength(16);
    assertThat(a).isEqualTo(b);
    assertThat(a).isNotEqualTo(c);
    byte[] expected = new byte[16];
    new java.util.Random(42).nextBytes(expected);
    assertThat(a).isEqualTo(expected);
    // Unseeded runs differ.
    assertThat(program.run(new byte[0], WasmLimits.defaults()).stdout())
        .isNotEqualTo(program.run(new byte[0], WasmLimits.defaults()).stdout());
  }

  @Test
  public void clockIsZero() throws Exception {
    WasmProgram program =
        start(
            "(memory (export \"memory\") 1) (data (i32.const 1024) \"\\ff\\ff\\ff\\ff\\ff\\ff\\ff\\ff\")",
            "(call $proc_exit (i32.add"
                + " (i32.mul (i32.const 10) (call $clock_time_get (i32.const 0) (i64.const 0) (i32.const 1024)))"
                + " (i32.add (i32.wrap_i64 (i64.load (i32.const 1024)))"
                + "   (call $clock_time_get (i32.const 1) (i64.const 0) (i32.const 1024)))))");
    assertThat(program.run(new byte[0], WasmLimits.defaults()).exitCode()).isEqualTo(0);
  }

  @Test
  public void argvIsModuleAndEnvironmentIsEmpty() throws Exception {
    WasmProgram program =
        start(
            "(memory (export \"memory\") 1)",
            "(drop (call $args_sizes_get (i32.const 100) (i32.const 104)))"
                + "(drop (call $environ_sizes_get (i32.const 108) (i32.const 112)))"
                + "(call $proc_exit (i32.add (i32.add"
                + "  (i32.mul (i32.load (i32.const 100)) (i32.const 1000))"
                + "  (i32.mul (i32.load (i32.const 104)) (i32.const 10)))"
                + "  (i32.add (i32.load (i32.const 108)) (i32.load (i32.const 112)))))");
    // argc 1, argv buffer "module\0" = 7 bytes, no environment.
    assertThat(program.run(new byte[0], WasmLimits.defaults()).exitCode()).isEqualTo(1070);
  }

  @Test
  public void rejectsModuleWithoutStart() {
    WasmException e =
        assertThrows(
            WasmException.class,
            () -> compile("(module (memory (export \"memory\") 1) (func (export \"main\")))"));
    assertThat(e.kind()).isEqualTo(WasmException.Kind.INVALID_MODULE);
  }

  @Test
  public void rejectsModuleWithoutMemoryExport() {
    WasmException e =
        assertThrows(
            WasmException.class, () -> compile("(module (memory 1) (func (export \"_start\")))"));
    assertThat(e.kind()).isEqualTo(WasmException.Kind.INVALID_MODULE);
  }

  @Test
  public void rejectsGarbage() {
    WasmException e =
        assertThrows(WasmException.class, () -> RUNTIME.compile("not wasm".getBytes(UTF_8)));
    assertThat(e.kind()).isEqualTo(WasmException.Kind.INVALID_MODULE);
  }

  @Test
  public void rejectsInvalidCode() {
    byte[] wasm = wat("(module (memory (export \"memory\") 1) (func (export \"_start\") nop))");
    // Replace the body's `nop` (0x01) with `i32.add` (0x6a), which underflows the stack.
    for (int i = wasm.length - 1; i >= 0; i--) {
      if (wasm[i] == 0x0b && wasm[i - 1] == 0x01) {
        wasm[i - 1] = 0x6a;
        break;
      }
    }
    WasmException e = assertThrows(WasmException.class, () -> RUNTIME.compile(wasm));
    assertThat(e.kind()).isEqualTo(WasmException.Kind.INVALID_MODULE);
  }

  @Test
  public void externalInterruptThrowsInterruptedException() throws Exception {
    WasmProgram program = start("(memory (export \"memory\") 1)", "(loop $l (br $l))");
    AtomicReference<Throwable> thrown = new AtomicReference<>();
    Thread runner =
        new Thread(
            () -> {
              try {
                program.run(new byte[0], WasmLimits.defaults());
              } catch (Throwable t) {
                thrown.set(t);
              }
            });
    runner.start();
    Thread.sleep(200);
    runner.interrupt();
    runner.join(5000);
    assertThat(runner.isAlive()).isFalse();
    assertThat(thrown.get()).isInstanceOf(InterruptedException.class);
  }
}
