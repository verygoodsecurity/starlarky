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
 * A failed compile or run. Runtimes word their messages differently; callers (and the
 * conformance tests) rely only on {@link #kind()}.
 */
public final class WasmException extends Exception {

  public enum Kind {
    INVALID_MODULE,
    TRAP,
    MEMORY_LIMIT,
    TIMEOUT,
    OUTPUT_LIMIT,
  }

  private final Kind kind;

  public WasmException(Kind kind, String message) {
    super(message);
    this.kind = kind;
  }

  public WasmException(Kind kind, String message, Throwable cause) {
    super(message, cause);
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
