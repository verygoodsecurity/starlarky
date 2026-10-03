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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/** Finds the {@link WasmRuntime} to use. */
public final class WasmRuntimes {

  /** System property naming the runtime, e.g. {@code -Dlarky.wasm.runtime=graal}. */
  public static final String PROPERTY = "larky.wasm.runtime";

  private WasmRuntimes() {}

  /** Every runtime on the class path. */
  public static List<WasmRuntime> all() {
    List<WasmRuntime> runtimes = new ArrayList<>();
    ServiceLoader.load(WasmRuntime.class, WasmRuntimes.class.getClassLoader()).forEach(runtimes::add);
    return runtimes;
  }

  /** The runtime named {@code name}. */
  public static WasmRuntime byName(String name) throws WasmException {
    for (WasmRuntime runtime : all()) {
      if (runtime.name().equals(name)) {
        return runtime;
      }
    }
    throw new WasmException(
        WasmException.Kind.INVALID_MODULE, "no WebAssembly runtime named '" + name + "'");
  }

  /**
   * The runtime {@link #PROPERTY} names; if it is unset, "endive" if present, else the only one.
   */
  public static WasmRuntime configured() throws WasmException {
    String name = System.getProperty(PROPERTY);
    if (name != null && !name.isEmpty()) {
      return byName(name);
    }
    List<WasmRuntime> runtimes = all();
    for (WasmRuntime runtime : runtimes) {
      if (runtime.name().equals("endive")) {
        return runtime;
      }
    }
    if (runtimes.size() == 1) {
      return runtimes.get(0);
    }
    throw new WasmException(
        WasmException.Kind.INVALID_MODULE,
        runtimes.isEmpty()
            ? "no WebAssembly runtime is available"
            : "several WebAssembly runtimes are available; set -D" + PROPERTY);
  }
}
