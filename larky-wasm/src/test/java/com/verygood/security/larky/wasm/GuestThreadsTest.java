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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class GuestThreadsTest {

  private static final WasmRuntime.Result EMPTY =
      new WasmRuntime.Result(0, new byte[0], new byte[0]);

  @After
  public void clearInterrupt() {
    Thread.interrupted();
  }

  @Test
  public void returnsWhatTheRunReturnsOrThrows() throws Exception {
    assertThat(GuestThreads.run(() -> EMPTY)).isSameInstanceAs(EMPTY);
    WasmException trap = new WasmException(WasmException.Kind.TRAP, "trap");
    assertThat(assertThrows(WasmException.class, () -> GuestThreads.run(() -> { throw trap; })))
        .isSameInstanceAs(trap);
    IllegalStateException bug = new IllegalStateException("bug");
    assertThat(
            assertThrows(IllegalStateException.class, () -> GuestThreads.run(() -> { throw bug; })))
        .isSameInstanceAs(bug);
  }

  @Test
  public void theRunIsOnAnotherThread() throws Exception {
    AtomicReference<Thread> guest = new AtomicReference<>();
    GuestThreads.run(() -> {
      guest.set(Thread.currentThread());
      return EMPTY;
    });
    assertThat(guest.get()).isNotSameInstanceAs(Thread.currentThread());
    assertThat(guest.get().getName()).isEqualTo("larky-wasm-guest");
  }

  @Test
  public void aGuestInterruptDoesNotReachTheCaller() throws Exception {
    // As when a deadline interrupts the guest's thread.
    GuestThreads.run(() -> {
      Thread.currentThread().interrupt();
      return EMPTY;
    });
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
    // Nor the next run on the same pool thread.
    assertThat(GuestThreads.run(() -> {
      if (Thread.currentThread().isInterrupted()) {
        throw new IllegalStateException("interrupted on entry");
      }
      return EMPTY;
    })).isSameInstanceAs(EMPTY);
  }

  @Test
  public void aCallerInterruptStopsTheGuestAndWaitsForIt() throws Exception {
    AtomicBoolean guestStopped = new AtomicBoolean();
    Thread caller = Thread.currentThread();
    Thread interrupter =
        new Thread(
            () -> {
              try {
                Thread.sleep(100);
              } catch (InterruptedException e) {
                return;
              }
              caller.interrupt();
            });
    interrupter.start();
    assertThrows(
        InterruptedException.class,
        () ->
            GuestThreads.run(
                () -> {
                  try {
                    Thread.sleep(60_000);
                  } catch (InterruptedException e) {
                    long until = System.nanoTime() + 50_000_000;
                    while (System.nanoTime() < until) {
                      Thread.onSpinWait(); // still stopping when the caller is interrupted
                    }
                    guestStopped.set(true);
                    throw e;
                  }
                  return EMPTY;
                }));
    // The caller returned only after the guest had stopped.
    assertThat(guestStopped.get()).isTrue();
    interrupter.join();
  }

  @Test
  public void aPendingInterruptStopsTheRunBeforeItStarts() {
    AtomicBoolean ran = new AtomicBoolean();
    Thread.currentThread().interrupt();
    assertThrows(
        InterruptedException.class,
        () -> GuestThreads.run(() -> {
          ran.set(true);
          return EMPTY;
        }));
    assertThat(ran.get()).isFalse();
  }

  private static int depth() {
    try {
      return depth() + 1;
    } catch (StackOverflowError e) {
      return 0;
    }
  }

  @Test
  public void guestsGetTheirOwnStackWhateverTheCallers() throws Exception {
    // From a thread with a 256 KiB stack, a guest still has GuestThreads.STACK_BYTES.
    int[] depths = new int[2];
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread small =
        new Thread(
            null,
            () -> {
              try {
                depths[0] = depth();
                GuestThreads.run(() -> {
                  depths[1] = depth();
                  return EMPTY;
                });
              } catch (Throwable t) {
                failure.set(t);
              }
            },
            "small-stack",
            256 << 10);
    small.start();
    small.join();
    assertThat(failure.get()).isNull();
    assertThat(depths[1]).isGreaterThan(4 * depths[0]);
  }
}
