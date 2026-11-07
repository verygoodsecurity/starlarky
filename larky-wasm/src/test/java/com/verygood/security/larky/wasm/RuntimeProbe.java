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

/**
 * Prints what {@link WasmRuntime#byName} and {@link WasmRuntime#all} give for "graal", for tests
 * that run it in a JVM with a different class path.
 */
public final class RuntimeProbe {

  private RuntimeProbe() {}

  public static void main(String[] args) {
    try {
      WasmRuntime.byName("graal");
      System.out.println("byName: ok");
    } catch (WasmRuntime.WasmException e) {
      System.out.println("byName: WasmException " + e.kind());
    } catch (Throwable t) {
      System.out.println("byName: " + t.getClass().getName());
    }
    List<String> names = new ArrayList<>();
    for (WasmRuntime runtime : WasmRuntime.all()) {
      names.add(runtime.name());
    }
    System.out.println("all: " + names);
  }
}
