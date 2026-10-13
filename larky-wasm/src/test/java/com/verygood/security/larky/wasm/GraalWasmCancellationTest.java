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

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Test;

public final class GraalWasmCancellationTest {
  @Test
  public void blockedContextCancellationDoesNotBlockOtherWatchdogChecks() throws Exception {
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch cancellationStarted = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Context context = GraalWasmRuntime.contextBuilder().build();
    ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
    ProxyExecutable blocked = args -> {
      entered.countDown();
      boolean interrupted = false;
      while (true) {
        try {
          release.await();
          break;
        } catch (InterruptedException e) {
          interrupted = true;
          cancellationStarted.countDown();
        }
      }
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
      return null;
    };
    Thread guest = new Thread(() -> {
      try {
        Value module = context.eval(GraalWasmRuntime.source(GraalWasmRuntimeTest.wat(
            "(module (import \"env\" \"block\" (func $block))"
                + " (func (export \"_start\") (call $block)))")));
        module.newInstance(ProxyObject.fromMap(Map.of(
            "env", ProxyObject.fromMap(Map.of("block", blocked)))))
            .getMember("exports").getMember("_start").executeVoid();
      } catch (PolyglotException e) {
        if (!e.isCancelled()) {
          failure.set(e);
        }
      } catch (Throwable t) {
        failure.set(t);
      }
    }, "blocked-graal-test-guest");
    guest.setDaemon(true);
    try {
      guest.start();
      assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
      Class<?> watchType = Class.forName(GraalWasmProgram.class.getName() + "$Watch");
      Constructor<?> constructor = watchType.getDeclaredConstructor(
          Context.class, Thread.class, long.class, CappedOutputStream.class, CappedOutputStream.class);
      constructor.setAccessible(true);
      Runnable watch = (Runnable) constructor.newInstance(context, guest, 1L,
          new CappedOutputStream("stdout", 10), new CappedOutputStream("stderr", 10));
      watchdog.execute(watch);
      assertThat(cancellationStarted.await(5, TimeUnit.SECONDS)).isTrue();
      // close(true) is still waiting for the host call. A single watchdog must remain available
      // for other contexts even then; a timeout-only happy-path test cannot prove this property.
      CountDownLatch nextCheck = new CountDownLatch(1);
      watchdog.execute(nextCheck::countDown);
      assertThat(nextCheck.await(2, TimeUnit.SECONDS)).isTrue();
      assertThat(guest.isAlive()).isTrue();
    } finally {
      release.countDown();
      guest.join(5000);
      watchdog.shutdownNow();
      context.close(true);
    }
    assertThat(guest.isAlive()).isFalse();
    assertThat(failure.get()).isNull();
  }
}
