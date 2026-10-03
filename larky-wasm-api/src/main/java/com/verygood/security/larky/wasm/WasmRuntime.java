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
 * Runs WebAssembly modules for Larky's {@code @vgs//wasm}. Implementations (Endive, GraalWasm)
 * register with {@link java.util.ServiceLoader}; {@link WasmRuntimes} picks one.
 *
 * <p>Every runtime must behave identically (the conformance tests compare them): a module is a
 * WASI preview 1 command (it exports {@code _start} and its memory). A run gets a fresh instance
 * with the input as stdin; its stdout is the result. It has no files (no preopens), no
 * environment, argv {@code ["module"]}, no network, {@code clock_time_get} always returns 0, and
 * {@code random_get} reads {@link WasmLimits#randomSeed()} if set, else a {@code SecureRandom}.
 */
public interface WasmRuntime {

  /** The name {@code -Dlarky.wasm.runtime} selects it by, e.g. "endive" or "graal". */
  String name();

  /**
   * Validates and prepares {@code wasm} for running. Callers cache the result by content.
   *
   * @throws WasmException of kind {@link WasmException.Kind#INVALID_MODULE} if the bytes are not
   *     a valid module or it lacks the {@code _start} or {@code memory} export
   */
  WasmProgram compile(byte[] wasm) throws WasmException;
}
