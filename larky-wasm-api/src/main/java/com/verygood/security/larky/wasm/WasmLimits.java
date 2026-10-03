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

/**
 * Limits for one run.
 *
 * @param maxMemoryBytes the most linear memory the instance may have (rounded down to 64 KiB pages)
 * @param deadlineEpochMs wall-clock deadline in {@link System#currentTimeMillis()} terms; 0 for none
 * @param maxOutputBytes the most bytes stdout, and separately stderr, may receive
 * @param randomSeed if not null, {@code random_get} returns a deterministic stream from this seed
 *     (for tests); otherwise it reads a {@code SecureRandom}
 */
public record WasmLimits(
    long maxMemoryBytes, long deadlineEpochMs, int maxOutputBytes, Long randomSeed) {

  public static final long DEFAULT_MAX_MEMORY_BYTES = 64L << 20;
  public static final int DEFAULT_MAX_OUTPUT_BYTES = 1 << 20;

  public static WasmLimits defaults() {
    return new WasmLimits(DEFAULT_MAX_MEMORY_BYTES, 0, DEFAULT_MAX_OUTPUT_BYTES, null);
  }

  public long maxMemoryPages() {
    return maxMemoryBytes / 65536;
  }
}
