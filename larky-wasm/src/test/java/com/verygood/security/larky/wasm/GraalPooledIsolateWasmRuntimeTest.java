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
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.graalvm.polyglot.Engine;
import org.junit.Test;
import run.endive.wabt.Wat2Wasm;

public final class GraalPooledIsolateWasmRuntimeTest {

  @Test
  public void hostIdentityIsRequiredAndScoped() throws Exception {
    assertThrows(WasmException.class, () -> WasmRuntime.byName("graal-isolate-pooled"));
    assertThrows(IllegalArgumentException.class, () -> WasmTrustDomain.enter(" "));
    assertThrows(IllegalArgumentException.class, () -> WasmTrustDomain.enter("a".repeat(257)));
    try (WasmTrustDomain.Scope ignored = WasmTrustDomain.enter("tenant-a")) {
      assertThat(WasmRuntime.byName("graal-isolate-pooled").name())
          .isEqualTo("graal-isolate-pooled");
      assertThrows(IllegalStateException.class, () -> WasmTrustDomain.enter("tenant-b"));
    }
    assertThrows(WasmException.class, () -> WasmRuntime.byName("graal-isolate-pooled"));
  }

  @Test
  public void sameTenantReusesAnEngineButDifferentTenantsDoNot() throws Exception {
    Engine first = GraalPooledIsolateWasmRuntime.engineForTests("tenant-engine-a");
    assertThat(GraalPooledIsolateWasmRuntime.engineForTests("tenant-engine-a"))
        .isSameInstanceAs(first);
    assertThat(GraalPooledIsolateWasmRuntime.engineForTests("tenant-engine-b"))
        .isNotSameInstanceAs(first);
  }

  @Test
  public void programCannotRunUnderAnotherTenant() throws Exception {
    WasmRuntime.Program program;
    try (WasmTrustDomain.Scope ignored = WasmTrustDomain.enter("tenant-a")) {
      program = WasmRuntime.byName("graal-isolate-pooled").compile(Fixtures.wasm("echo"));
      assertEcho(program, "first");
    }
    try (WasmTrustDomain.Scope ignored = WasmTrustDomain.enter("tenant-b")) {
      WasmException failure = assertThrows(WasmException.class,
          () -> program.run(new byte[0], WasmRuntime.Limits.defaults()));
      assertThat(failure.getMessage()).contains("cannot cross trust domains");
      assertEcho(WasmRuntime.byName("graal-isolate-pooled").compile(Fixtures.wasm("echo")),
          "second");
    }
    try (WasmTrustDomain.Scope ignored = WasmTrustDomain.enter("tenant-a")) {
      assertEcho(program, "third");
    }
  }

  @Test
  public void evictedProgramRecompilesForItsOwnTenant() throws Exception {
    WasmRuntime.Program first;
    try (WasmTrustDomain.Scope ignored = WasmTrustDomain.enter("tenant-eviction-a")) {
      first = WasmRuntime.byName("graal-isolate-pooled").compile(Fixtures.wasm("echo"));
    }
    for (String tenant : new String[] {"tenant-eviction-b", "tenant-eviction-c"}) {
      try (WasmTrustDomain.Scope ignored = WasmTrustDomain.enter(tenant)) {
        assertEcho(WasmRuntime.byName("graal-isolate-pooled").compile(Fixtures.wasm("echo")),
            tenant);
      }
    }
    try (WasmTrustDomain.Scope ignored = WasmTrustDomain.enter("tenant-eviction-a")) {
      assertEcho(first, "alive after eviction");
    }
  }

  @Test
  public void adversarialInputsLeaveTheTenantAbleToRunAgain() throws Exception {
    try (WasmTrustDomain.Scope ignored = WasmTrustDomain.enter("tenant-adversarial")) {
      WasmRuntime runtime = WasmRuntime.byName("graal-isolate-pooled");
      assertThat(assertThrows(WasmException.class,
          () -> runtime.compile(new byte[WasmRuntime.MAX_MODULE_BYTES + 1])).kind())
          .isEqualTo(WasmException.Kind.INVALID_MODULE);
      byte[] forgedLength = HexFormat.of().parseHex(
          "0061736d010000000b070101ffffffff07");
      assertThat(assertThrows(WasmException.class, () -> runtime.compile(forgedLength)).kind())
          .isEqualTo(WasmException.Kind.INVALID_MODULE);

      byte[] hugeTable = Wat2Wasm.parse("(module (memory (export \"memory\") 1)"
          + " (table 10000000 funcref) (func (export \"_start\")))");
      assertThat(hugeTable).hasLength(59);
      assertThat(assertThrows(WasmException.class, () -> runtime.compile(hugeTable)).kind())
          .isEqualTo(WasmException.Kind.MEMORY_LIMIT);

      WasmRuntime.Program spin = runtime.compile(Fixtures.wasm("spin"));
      WasmRuntime.Limits defaults = WasmRuntime.Limits.defaults();
      WasmRuntime.Limits deadline = new WasmRuntime.Limits(defaults.maxMemoryBytes(),
          System.currentTimeMillis() + 1_000, defaults.maxOutputBytes(), defaults.randomSeed());
      assertThat(assertThrows(WasmException.class, () -> spin.run(new byte[0], deadline)).kind())
          .isEqualTo(WasmException.Kind.TIMEOUT);

      assertEcho(runtime.compile(Fixtures.wasm("echo")), "recovered");
    }
  }

  @Test
  public void oneTenantsTimeoutDoesNotStopAnotherTenant() throws Exception {
    WasmRuntime.Program spin;
    try (WasmTrustDomain.Scope ignored = WasmTrustDomain.enter("tenant-concurrent-a")) {
      spin = WasmRuntime.byName("graal-isolate-pooled").compile(Fixtures.wasm("spin"));
    }
    CompletableFuture<WasmException.Kind> timedOut = CompletableFuture.supplyAsync(() -> {
      try (WasmTrustDomain.Scope ignored = WasmTrustDomain.enter("tenant-concurrent-a")) {
        WasmRuntime.Limits defaults = WasmRuntime.Limits.defaults();
        WasmRuntime.Limits deadline = new WasmRuntime.Limits(defaults.maxMemoryBytes(),
            System.currentTimeMillis() + 1_000, defaults.maxOutputBytes(), defaults.randomSeed());
        spin.run(new byte[0], deadline);
        throw new AssertionError("the loop returned");
      } catch (WasmException e) {
        return e.kind();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    });
    try (WasmTrustDomain.Scope ignored = WasmTrustDomain.enter("tenant-concurrent-b")) {
      assertEcho(WasmRuntime.byName("graal-isolate-pooled").compile(Fixtures.wasm("echo")),
          "independent");
    }
    assertThat(timedOut.get(10, TimeUnit.SECONDS)).isEqualTo(WasmException.Kind.TIMEOUT);
  }

  @Test
  public void oneTenantCanUseTwoFreshContextsConcurrently() throws Exception {
    WasmRuntime.Program echo;
    try (WasmTrustDomain.Scope ignored = WasmTrustDomain.enter("tenant-parallel")) {
      echo = WasmRuntime.byName("graal-isolate-pooled").compile(Fixtures.wasm("echo"));
    }
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<byte[]> first = executor.submit(() -> runParallel(echo, "first", ready, start));
      Future<byte[]> second = executor.submit(() -> runParallel(echo, "second", ready, start));
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo("first".getBytes(StandardCharsets.UTF_8));
      assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo("second".getBytes(StandardCharsets.UTF_8));
    } finally {
      start.countDown();
      executor.shutdownNow();
    }
  }

  private static byte[] runParallel(WasmRuntime.Program program, String input,
                                    CountDownLatch ready, CountDownLatch start) throws Exception {
    try (WasmTrustDomain.Scope ignored = WasmTrustDomain.enter("tenant-parallel")) {
      ready.countDown();
      start.await();
      return program.run(input.getBytes(StandardCharsets.UTF_8), WasmRuntime.Limits.defaults())
          .stdout();
    }
  }

  private static void assertEcho(WasmRuntime.Program program, String input) throws Exception {
    byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
    assertThat(program.run(bytes, WasmRuntime.Limits.defaults()).stdout()).isEqualTo(bytes);
  }
}
