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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class LimitsTest {

  @Test
  public void negativeLimitsAreRejected() {
    // A negative memory cap would otherwise mean "no cap" to code that uses -1 as a sentinel.
    assertThrows(IllegalArgumentException.class, () -> new WasmRuntime.Limits(-65536, 0, 0, null));
    assertThrows(IllegalArgumentException.class, () -> new WasmRuntime.Limits(0, -1, 0, null));
    assertThrows(IllegalArgumentException.class, () -> new WasmRuntime.Limits(0, 0, -1, null));
  }

  @Test
  public void zeroLimitsAreAllowed() {
    WasmRuntime.Limits limits = new WasmRuntime.Limits(0, 0, 0, null);
    assertThat(limits.maxMemoryPages()).isEqualTo(0);
  }
}
