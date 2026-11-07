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

/** Host-owned bounds for the opt-in GraalVM UNTRUSTED isolate runtime. */
record GraalIsolateSettings(
    long maxIsolateMemoryBytes,
    long maxGuestHeapBytes,
    long maxCpuMillis,
    long maxWallMillis,
    long compileCpuMillis,
    int maxAstDepth,
    int maxStackFrames) {

  static GraalIsolateSettings configured() {
    return new GraalIsolateSettings(
        property("maxIsolateMemoryBytes", 256L << 20),
        property("maxGuestHeapBytes", 128L << 20),
        property("maxCpuMillis", 30_000),
        property("maxWallMillis", 30_000),
        property("compileCpuMillis", 10_000),
        (int) property("maxAstDepth", 10_000),
        (int) property("maxStackFrames", 10_000));
  }

  GraalIsolateSettings {
    if (maxIsolateMemoryBytes <= 0 || maxGuestHeapBytes <= 0
        || maxGuestHeapBytes > maxIsolateMemoryBytes || maxCpuMillis <= 0
        || maxWallMillis <= 0 || compileCpuMillis <= 0
        || maxAstDepth <= 0 || maxStackFrames <= 0) {
      throw new IllegalArgumentException("invalid GraalWasm isolate limits");
    }
  }

  private static long property(String suffix, long fallback) {
    String key = "larky.wasm.isolate." + suffix;
    String raw = System.getProperty(key);
    if (raw == null) return fallback;
    try {
      long value = Long.parseLong(raw);
      if (value <= 0) throw new NumberFormatException("not positive");
      if ((suffix.equals("maxAstDepth") || suffix.equals("maxStackFrames"))
          && value > Integer.MAX_VALUE) {
        throw new NumberFormatException("exceeds Integer.MAX_VALUE");
      }
      return value;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(key + " must be a positive integer", e);
    }
  }

  long deadline(long requestedEpochMs) {
    long now = System.currentTimeMillis();
    long configured = now > Long.MAX_VALUE - maxWallMillis
        ? Long.MAX_VALUE : now + maxWallMillis;
    return requestedEpochMs == 0 ? configured : Math.min(requestedEpochMs, configured);
  }
}
