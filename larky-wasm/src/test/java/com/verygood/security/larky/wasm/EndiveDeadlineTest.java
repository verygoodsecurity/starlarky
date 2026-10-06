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
import com.verygood.security.larky.wasm.WasmRuntime.WasmException.Kind;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** How a finished Endive run is classified, given its deadline and the caller's interrupts. */
@RunWith(JUnit4.class)
public final class EndiveDeadlineTest {

  @After
  public void clearInterrupt() {
    Thread.interrupted();
  }

  private static WasmRuntime.Result result(EndiveWasmProgram.Deadline deadline) throws Exception {
    return EndiveWasmProgram.result(
        0, deadline, new CappedOutputStream("stdout", 10), new CappedOutputStream("stderr", 10), 10);
  }

  @Test
  public void finishingAfterTheDeadlineIsTimeoutEvenIfTheWatchdogHasNotFired() {
    // A deadline that has passed but was never scheduled: the watchdog is late.
    EndiveWasmProgram.Deadline late =
        new EndiveWasmProgram.Deadline(Thread.currentThread(), System.currentTimeMillis() - 1);
    late.finish();
    assertThat(late.fired()).isFalse();
    WasmException e = assertThrows(WasmException.class, () -> result(late));
    assertThat(e.kind()).isEqualTo(Kind.TIMEOUT);
  }

  @Test
  public void finishingBeforeTheDeadlineIsAResult() throws Exception {
    EndiveWasmProgram.Deadline early =
        new EndiveWasmProgram.Deadline(Thread.currentThread(), System.currentTimeMillis() + 60_000);
    early.finish();
    assertThat(result(early).exitCode()).isEqualTo(0);
  }

  @Test
  public void anInterruptAfterTheGuestsLastCheckIsStillAnInterrupt() {
    Thread.currentThread().interrupt();
    assertThrows(InterruptedException.class, () -> result(EndiveWasmProgram.Deadline.NONE));
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }

  @Test
  public void theDeadlinesOwnInterruptDoesNotOutliveTheRun() {
    EndiveWasmProgram.Deadline deadline =
        EndiveWasmProgram.Deadline.start(Thread.currentThread(), System.currentTimeMillis() - 1);
    assertThat(deadline.fired()).isTrue();
    assertThat(Thread.currentThread().isInterrupted()).isTrue();
    deadline.finish();
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }

  @Test
  public void finishTwiceIsHarmless() {
    EndiveWasmProgram.Deadline deadline =
        EndiveWasmProgram.Deadline.start(Thread.currentThread(), System.currentTimeMillis() - 1);
    deadline.finish();
    Thread.currentThread().interrupt(); // someone else's, after the run's deadline handling
    deadline.finish();
    assertThat(Thread.currentThread().isInterrupted()).isTrue();
  }
}
