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
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import com.verygood.security.larky.wasm.conformance.Fixtures;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** The local launcher tests the worker protocol; deployment must supply an OS-limited launcher. */
public final class WorkerWasmRuntimeTest {
  private static final String LAUNCHER = "larky.wasm.worker.launcher";
  private static final String BACKEND = "larky.wasm.worker.backend";
  private static final String MAX_MILLIS = "larky.wasm.worker.maxRequestMillis";
  private static final String MAX_MEMORY = "larky.wasm.worker.maxMemoryBytes";
  private static final String MAX_OUTPUT = "larky.wasm.worker.maxOutputBytes";
  private Path launcher;
  private String workingLauncher;

  @Before
  public void setUp() throws Exception {
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    String classpath = System.getProperty("java.class.path");
    launcher = Files.createTempFile("larky-wasm-worker-", ".sh");
    workingLauncher = "#!/bin/sh\nexec '" + java.replace("'", "'\\''")
        + "' --add-modules jdk.incubator.vector -cp '" + classpath.replace("'", "'\\''")
        + "' com.verygood.security.larky.wasm.WorkerMain\n";
    Files.writeString(launcher, workingLauncher);
    launcher.toFile().setExecutable(true);
    System.setProperty(LAUNCHER, launcher.toString());
    System.setProperty(BACKEND, "endive");
    System.setProperty(MAX_MILLIS, "2000");
    System.setProperty(MAX_MEMORY, Long.toString(64L << 20));
    System.setProperty(MAX_OUTPUT, Integer.toString(1 << 20));
  }

  @After
  public void tearDown() throws Exception {
    System.clearProperty(LAUNCHER);
    System.clearProperty(BACKEND);
    System.clearProperty(MAX_MILLIS);
    System.clearProperty(MAX_MEMORY);
    System.clearProperty(MAX_OUTPUT);
    Files.deleteIfExists(launcher);
  }

  @Test
  public void compileAndRunStayInWorkerAndReturnOutput() throws Exception {
    var runtime = WasmRuntime.byName("worker");
    byte[] stdin = "worker echo".getBytes(StandardCharsets.UTF_8);
    var result = runtime.compile(Fixtures.wasm("echo"))
        .run(stdin, new WasmRuntime.Limits(1 << 20,
            System.currentTimeMillis() + 2000, 1024, 0L));
    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.stdout()).isEqualTo(stdin);
  }

  @Test
  public void invalidModuleFailsDuringCompile() throws Exception {
    var runtime = WasmRuntime.byName("worker");
    WasmException error = assertThrows(WasmException.class,
        () -> runtime.compile(new byte[] {0, 97, 115, 109}));
    assertThat(error.kind()).isEqualTo(WasmException.Kind.INVALID_MODULE);
  }

  @Test
  public void aTimedOutWorkerDoesNotPreventTheNextRun() throws Exception {
    var runtime = WasmRuntime.byName("worker");
    var spinning = runtime.compile(Fixtures.wasm("spin"));
    var shortDeadline = new WasmRuntime.Limits(1 << 20,
        System.currentTimeMillis() + 100, 1024, 0L);
    long started = System.nanoTime();
    WasmException error = assertThrows(WasmException.class,
        () -> spinning.run(new byte[0], shortDeadline));
    assertThat(error.kind()).isEqualTo(WasmException.Kind.TIMEOUT);
    assertThat(Duration.ofNanos(System.nanoTime() - started).toSeconds()).isLessThan(3L);
    var healthy = runtime.compile(Fixtures.wasm("echo"));
    assertThat(healthy.run(new byte[0], new WasmRuntime.Limits(1 << 20,
        System.currentTimeMillis() + 2000, 1024, 0L)).exitCode()).isEqualTo(0);
  }

  @Test
  public void failsClosedWithoutAnExplicitLauncher() {
    System.clearProperty(LAUNCHER);
    assertThrows(WasmException.class, () -> WasmRuntime.byName("worker"));
  }

  @Test
  public void excessiveWorkerOutputFailsAndTheNextRequestSucceeds() throws Exception {
    var runtime = WasmRuntime.byName("worker");
    Files.writeString(launcher, "#!/bin/sh\nwhile :; do printf '0123456789abcdef'; done\n");
    WasmException error = assertThrows(WasmException.class,
        () -> runtime.compile(Fixtures.wasm("echo")));
    assertThat(error.kind()).isEqualTo(WasmException.Kind.TRAP);
    Files.writeString(launcher, workingLauncher);
    var result = runtime.compile(Fixtures.wasm("echo")).run(new byte[0],
        new WasmRuntime.Limits(1 << 20, System.currentTimeMillis() + 2000, 1024, 0L));
    assertThat(result.exitCode()).isEqualTo(0);
  }

  @Test
  public void directJavaCallersCannotRaiseWorkerMemoryOrOutputCaps() throws Exception {
    var program = WasmRuntime.byName("worker").compile(Fixtures.wasm("echo"));
    var memory = assertThrows(WasmException.class, () -> program.run(new byte[0],
        new WasmRuntime.Limits(65L << 20, System.currentTimeMillis() + 2000, 1024, 0L)));
    assertThat(memory.kind()).isEqualTo(WasmException.Kind.MEMORY_LIMIT);
    var output = assertThrows(WasmException.class, () -> program.run(new byte[0],
        new WasmRuntime.Limits(1L << 20, System.currentTimeMillis() + 2000,
            (1 << 20) + 1, 0L)));
    assertThat(output.kind()).isEqualTo(WasmException.Kind.OUTPUT_LIMIT);
  }

  @Test
  public void nullableWorkerErrorMessageRoundTripsSafely() throws Exception {
    byte[] encoded = WorkerProtocol.encodeFailure(new WasmException(WasmException.Kind.TRAP, null));
    var response = WorkerProtocol.decodeResponse(encoded, 0);
    assertThat(response.error().kind()).isEqualTo(WasmException.Kind.TRAP);
    assertThat(response.error()).hasMessageThat().isEmpty();
  }

  @Test
  public void workerFailureDoesNotExposeRawStderr() throws Exception {
    Files.writeString(launcher, "#!/bin/sh\ncat >/dev/null\nprintf 'customer-secret' >&2\nexit 42\n");
    var runtime = WasmRuntime.byName("worker");
    var error = assertThrows(WasmException.class,
        () -> runtime.compile(Fixtures.wasm("echo")));
    assertThat(error.kind()).isEqualTo(WasmException.Kind.TRAP);
    assertThat(error).hasMessageThat().contains("exit code 42");
    assertThat(error).hasMessageThat().doesNotContain("customer-secret");
  }

  @Test
  public void rejectsOverlongRequestDeadlineWithPreciseConfigurationError() {
    System.setProperty(MAX_MILLIS, "86400001");
    var error = assertThrows(WasmException.class, () -> WasmRuntime.byName("worker"));
    assertThat(error).hasMessageThat().contains("must not exceed 24 hours");
  }
}
