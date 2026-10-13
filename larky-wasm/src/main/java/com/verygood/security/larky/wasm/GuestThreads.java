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

import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Runs each guest on a thread of its own, which every runtime uses:
 *
 * <ul>
 *   <li>A runtime stops a guest at its deadline by interrupting or cancelling the guest's thread,
 *       never the caller's, so the caller's own interrupts are neither consumed nor mistaken for
 *       the deadline.
 *   <li>Every guest gets the host-selected stack size hint (default {@link #STACK_BYTES}), so how deeply a module can nest
 *       calls does not depend on the thread that runs it.
 * </ul>
 */
final class GuestThreads {

  /** Default stack size hint for a thread that runs a guest. */
  static final long STACK_BYTES = 8L << 20;

  private static final class DefaultHolder {
    static final GuestThreads INSTANCE = configured();

    private static GuestThreads configured() {
      WasmHostLimits limits = WasmHostLimits.configured();
      return new GuestThreads(limits.maxConcurrentGuests(), limits.guestStackBytes());
    }
  }

  private final Semaphore slots;
  private final ExecutorService pool;

  GuestThreads(int maxConcurrentGuests, long stackBytes) {
    if (maxConcurrentGuests <= 0 || stackBytes <= 0) {
      throw new IllegalArgumentException("guest concurrency and stack size must be positive");
    }
    slots = new Semaphore(maxConcurrentGuests, true);
    pool = Executors.newCachedThreadPool(r -> {
      Thread t = new Thread(null, r, "larky-wasm-guest", stackBytes);
      t.setDaemon(true);
      return t;
    });
  }

  /** A run of a guest, on the thread {@link #run} gives it. */
  interface Run {
    WasmRuntime.Result run() throws WasmException, InterruptedException;
  }

  /**
   * Runs {@code run} on a guest thread and returns what it returns or throws. If the caller is
   * interrupted first, the guest thread is interrupted (each runtime stops a guest on that, as at
   * its deadline), and once it has stopped, this throws {@link InterruptedException}.
   */
  static WasmRuntime.Result run(Run run) throws WasmException, InterruptedException {
    return run(run, 0);
  }

  static WasmRuntime.Result run(Run run, long deadlineEpochMs)
      throws WasmException, InterruptedException {
    return DefaultHolder.INSTANCE.runGuest(run, deadlineEpochMs);
  }

  WasmRuntime.Result runGuest(Run run, long deadlineEpochMs)
      throws WasmException, InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    if (deadlineEpochMs == 0) {
      slots.acquire();
    } else {
      long remaining = deadlineEpochMs - System.currentTimeMillis();
      if (remaining <= 0 || !slots.tryAcquire(remaining, TimeUnit.MILLISECONDS)) {
        throw new WasmException(WasmException.Kind.TIMEOUT, "guest admission deadline expired");
      }
    }
    try {
      if (deadlineEpochMs != 0 && System.currentTimeMillis() >= deadlineEpochMs) {
        throw new WasmException(WasmException.Kind.TIMEOUT, "guest admission deadline expired");
      }
      Task task = new Task(run);
      pool.execute(task);
      try {
        task.done.await();
      } catch (InterruptedException e) {
        task.cancel();
        awaitUninterruptibly(task.done);
        throw e;
      }
      return task.result();
    } finally {
      slots.release();
    }
  }

  private static void awaitUninterruptibly(CountDownLatch latch) {
    boolean interrupted = false;
    while (true) {
      try {
        latch.await();
        break;
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  private static final class Task implements Runnable {
    final CountDownLatch done = new CountDownLatch(1);
    private final Run run;
    /** The guest thread while {@link #run} is in progress; guarded by this. */
    private Thread thread;
    /** Set by {@link #cancel}; guarded by this. */
    private boolean cancelled;
    // Written before done counts down, read after it has.
    private WasmRuntime.Result result;
    private Throwable failure;

    Task(Run run) {
      this.run = run;
    }

    @Override
    public void run() {
      synchronized (this) {
        if (cancelled) {
          done.countDown();
          return;
        }
        thread = Thread.currentThread();
      }
      try {
        result = run.run();
      } catch (Throwable t) {
        failure = t;
      } finally {
        synchronized (this) {
          thread = null;
        }
        Thread.interrupted(); // a cancel's interrupt stays with this run
        done.countDown();
      }
    }

    synchronized void cancel() {
      cancelled = true;
      if (thread != null) {
        thread.interrupt();
      }
    }

    WasmRuntime.Result result() throws WasmException, InterruptedException {
      if (failure == null) {
        return result;
      }
      if (failure instanceof WasmException e) {
        throw e;
      }
      if (failure instanceof InterruptedException e) {
        throw e;
      }
      if (failure instanceof RuntimeException e) {
        throw e;
      }
      if (failure instanceof Error e) {
        throw e;
      }
      throw new IllegalStateException(failure);
    }
  }
}
