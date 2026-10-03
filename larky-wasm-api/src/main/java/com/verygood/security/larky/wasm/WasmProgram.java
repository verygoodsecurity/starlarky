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

/** A compiled module. Thread-safe: each {@link #run} instantiates it afresh. */
public interface WasmProgram {

  /**
   * Instantiates the module and runs {@code _start} with {@code stdin}.
   *
   * <p>A module that returns from {@code _start} exits with 0; {@code proc_exit(n)} exits with n.
   * Both are results, not exceptions.
   *
   * @throws WasmException of kind {@link WasmException.Kind#TRAP} if execution traps,
   *     {@link WasmException.Kind#MEMORY_LIMIT} if the module's initial memory exceeds
   *     {@link WasmLimits#maxMemoryBytes()} (growing past it makes {@code memory.grow} return -1,
   *     as the specification says), {@link WasmException.Kind#TIMEOUT} at the deadline, and
   *     {@link WasmException.Kind#OUTPUT_LIMIT} when stdout or stderr exceeds the output limit
   * @throws InterruptedException if the calling thread is interrupted (not by the deadline)
   */
  WasmResult run(byte[] stdin, WasmLimits limits) throws WasmException, InterruptedException;
}
