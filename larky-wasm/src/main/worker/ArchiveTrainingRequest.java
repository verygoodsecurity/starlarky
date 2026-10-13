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

import run.endive.wabt.Wat2Wasm;

/** Emits a harmless compile request used only while building the worker class archive. */
public final class ArchiveTrainingRequest {
  private ArchiveTrainingRequest() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 0) throw new IllegalArgumentException("archive trainer takes no arguments");
    byte[] wasm = Wat2Wasm.parse("(module (memory (export \"memory\") 1)"
        + " (func (export \"_start\")))");
    var request = new WorkerProtocol.Request(WorkerProtocol.COMPILE, WorkerProtocol.ENDIVE,
        wasm, new byte[0], 0, 5000, 0, null, WasmRuntime.WasiHostPolicy.none());
    System.out.write(WorkerProtocol.encodeRequest(request));
  }
}
