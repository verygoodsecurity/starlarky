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

public final class WasmHostLimitsTest {
  @Test
  public void hostMaySetEveryCompileAndAdmissionCap() {
    String[] suffixes = {"maxModuleBytes", "maxFunctions", "maxTables", "maxTableElements",
        "maxLocalsPerFunction", "maxTotalLocals", "maxTypeEntries", "maxFunctionArity",
        "guestStackBytes", "maxConcurrentGuests"};
    String[] old = new String[suffixes.length];
    try {
      for (int i = 0; i < suffixes.length; i++) {
        String key = "larky.wasm." + suffixes[i];
        old[i] = System.getProperty(key);
        System.setProperty(key, Integer.toString(i + 16));
      }
      WasmHostLimits limits = WasmHostLimits.configured();
      assertThat(limits.maxModuleBytes()).isEqualTo(16);
      assertThat(limits.maxFunctions()).isEqualTo(17);
      assertThat(limits.maxTables()).isEqualTo(18);
      assertThat(limits.maxTableElements()).isEqualTo(19);
      assertThat(limits.maxLocalsPerFunction()).isEqualTo(20);
      assertThat(limits.maxTotalLocals()).isEqualTo(21);
      assertThat(limits.maxTypeEntries()).isEqualTo(22);
      assertThat(limits.maxFunctionArity()).isEqualTo(23);
      assertThat(limits.guestStackBytes()).isEqualTo(24);
      assertThat(limits.maxConcurrentGuests()).isEqualTo(25);
    } finally {
      for (int i = 0; i < suffixes.length; i++) {
        if (old[i] == null) System.clearProperty("larky.wasm." + suffixes[i]);
        else System.setProperty("larky.wasm." + suffixes[i], old[i]);
      }
    }
  }

  @Test
  public void invalidHostCapFailsClosed() {
    String key = "larky.wasm.maxConcurrentGuests";
    String old = System.getProperty(key);
    try {
      for (String value : new String[] {"0", "-1", "invalid", "2147483648"}) {
        System.setProperty(key, value);
        assertThrows(IllegalArgumentException.class, WasmHostLimits::configured);
      }
    } finally {
      if (old == null) System.clearProperty(key);
      else System.setProperty(key, old);
    }
  }
}
