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

/** Host-owned limits for compilation and guest-thread admission. Read when a runtime is created. */
record WasmHostLimits(
    int maxModuleBytes,
    int maxFunctions,
    int maxTables,
    int maxTableElements,
    int maxLocalsPerFunction,
    int maxTotalLocals,
    int maxTypeEntries,
    int maxFunctionArity,
    long guestStackBytes,
    int maxConcurrentGuests) {

  static WasmHostLimits configured() {
    return new WasmHostLimits(
        intProperty("maxModuleBytes", WasmRuntime.MAX_MODULE_BYTES),
        intProperty("maxFunctions", WasmRuntime.MAX_FUNCTIONS),
        intProperty("maxTables", WasmRuntime.MAX_TABLES),
        intProperty("maxTableElements", WasmRuntime.MAX_TABLE_ELEMENTS),
        intProperty("maxLocalsPerFunction", 50_000),
        intProperty("maxTotalLocals", 100_000),
        intProperty("maxTypeEntries", 100_000),
        intProperty("maxFunctionArity", 1_000),
        longProperty("guestStackBytes", 8L << 20),
        intProperty("maxConcurrentGuests", 4));
  }

  WasmHostLimits {
    if (maxModuleBytes < 8 || maxFunctions < 0 || maxTables < 0 || maxTableElements < 0
        || maxLocalsPerFunction < 0 || maxTotalLocals < 0 || maxTypeEntries < 0
        || maxFunctionArity < 0 || guestStackBytes <= 0 || maxConcurrentGuests <= 0) {
      throw new IllegalArgumentException("invalid WebAssembly host limits");
    }
  }

  private static int intProperty(String suffix, int fallback) {
    long value = longProperty(suffix, fallback);
    if (value > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("larky.wasm." + suffix + " exceeds Integer.MAX_VALUE");
    }
    return (int) value;
  }

  private static long longProperty(String suffix, long fallback) {
    String key = "larky.wasm." + suffix;
    String value = System.getProperty(key);
    if (value == null) return fallback;
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(key + " must be an integer", e);
    }
  }
}
