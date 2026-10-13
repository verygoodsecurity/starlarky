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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.graalvm.polyglot.Context;
import org.junit.Test;

/** Exercises the opt-in isolate through the same WASI bridge used by Larky. */
public final class GraalIsolateWasmRuntimeTest {
  private final WasmRuntime runtime = new GraalIsolateWasmRuntime();

  @Test
  public void contextsDoNotShareAnEngine() {
    GraalWasmRuntime isolated = (GraalWasmRuntime) runtime;
    try (Context first = isolated.newContextBuilder(1_000, 0).build();
        Context second = isolated.newContextBuilder(1_000, 0).build()) {
      assertThat(first.getEngine()).isNotSameInstanceAs(second.getEngine());
    }
  }

  @Test
  public void wasiRoundTripAndFreshInstance() throws Exception {
    WasmRuntime.Program echo = runtime.compile(Fixtures.wasm("echo"));
    for (String input : new String[] {"first", "second"}) {
      byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
      WasmRuntime.Result result = echo.run(bytes, WasmRuntime.Limits.defaults());
      assertThat(result.exitCode()).isEqualTo(0);
      assertThat(result.stdout()).isEqualTo(bytes);
    }
  }

  @Test
  public void timedOutGuestDoesNotPoisonTheNextRun() throws Exception {
    WasmRuntime.Program spin = runtime.compile(Fixtures.wasm("spin"));
    WasmRuntime.Limits d = WasmRuntime.Limits.defaults();
    WasmRuntime.Limits shortRun = new WasmRuntime.Limits(d.maxMemoryBytes(),
        System.currentTimeMillis() + 1_000, d.maxOutputBytes(), d.randomSeed());
    WasmException failure = assertThrows(WasmException.class,
        () -> spin.run(new byte[0], shortRun));
    assertThat(failure.kind()).isEqualTo(WasmException.Kind.TIMEOUT);
    WasmRuntime.Result result = runtime.compile(Fixtures.wasm("echo"))
        .run("alive".getBytes(StandardCharsets.UTF_8), WasmRuntime.Limits.defaults());
    assertThat(new String(result.stdout(), StandardCharsets.UTF_8)).isEqualTo("alive");
  }

  @Test
  public void cpuBudgetStopsALoopBeforeTheWallDeadline() throws Exception {
    GraalWasmRuntime cpuLimited = new GraalWasmRuntime(new GraalIsolateSettings(
        256L << 20, 128L << 20, 100, 5_000, 10_000, 10_000, 10_000));
    WasmRuntime.Program spin = cpuLimited.compile(Fixtures.wasm("spin"));
    WasmException failure = assertThrows(WasmException.class,
        () -> spin.run(new byte[0], WasmRuntime.Limits.defaults()));
    assertThat(failure.kind()).isEqualTo(WasmException.Kind.TIMEOUT);
  }

  @Test
  public void concurrentDeadlineFailureDoesNotCancelAnotherIsolate() throws Exception {
    WasmRuntime.Program spin = runtime.compile(Fixtures.wasm("spin"));
    WasmRuntime.Program echo = runtime.compile(Fixtures.wasm("echo"));
    WasmRuntime.Limits d = WasmRuntime.Limits.defaults();
    WasmRuntime.Limits shortRun = new WasmRuntime.Limits(d.maxMemoryBytes(),
        System.currentTimeMillis() + 2_000, d.maxOutputBytes(), d.randomSeed());
    CompletableFuture<WasmException.Kind> timedOut = CompletableFuture.supplyAsync(() -> {
      try {
        spin.run(new byte[0], shortRun);
        throw new AssertionError("spin returned");
      } catch (WasmException e) {
        return e.kind();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    });
    byte[] input = "independent".getBytes(StandardCharsets.UTF_8);
    assertThat(echo.run(input, WasmRuntime.Limits.defaults()).stdout()).isEqualTo(input);
    assertThat(timedOut.get(10, TimeUnit.SECONDS)).isEqualTo(WasmException.Kind.TIMEOUT);
  }

  @Test
  public void settingsClampEvenAnUnboundedJavaCaller() {
    GraalIsolateSettings settings = new GraalIsolateSettings(
        256L << 20, 128L << 20, 1_000, 2_000, 1_000, 10_000, 10_000);
    long before = System.currentTimeMillis();
    assertThat(settings.deadline(0)).isAtLeast(before + 2_000);
    assertThat(settings.deadline(Long.MAX_VALUE)).isAtMost(System.currentTimeMillis() + 2_000);
    assertThat(settings.deadline(1)).isEqualTo(1);
    assertThrows(IllegalArgumentException.class,
        () -> new GraalIsolateSettings(1, 2, 1, 1, 1, 1, 1));
  }
}
