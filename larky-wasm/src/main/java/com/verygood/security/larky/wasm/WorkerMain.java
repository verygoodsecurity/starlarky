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

import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import java.io.DataInputStream;
import java.io.IOException;

/** One-shot entrypoint launched under host-provided OS limits. No untrusted bytes are parsed by the parent. */
public final class WorkerMain {
  private WorkerMain() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 0) throw new IllegalArgumentException("worker takes no command-line arguments");
    WasmHostLimits caps = WasmHostLimits.configured();
    int maxInput = WorkerWasmRuntime.configuredMaxInputBytes();
    WorkerProtocol.Request request = WorkerProtocol.readRequest(
        new DataInputStream(System.in), caps.maxModuleBytes(), maxInput);
    String backend = switch (request.backend()) {
      case WorkerProtocol.ENDIVE -> "endive";
      case WorkerProtocol.GRAAL_ISOLATE -> "graal-isolate";
      default -> throw new IOException("unsupported worker backend");
    };
    try {
      WasmRuntime.Program program = WasmRuntime.byName(backend).compile(request.wasm());
      WasmRuntime.Result result;
      if (request.operation() == WorkerProtocol.COMPILE) {
        result = new WasmRuntime.Result(0, new byte[0], new byte[0]);
      } else {
        long deadline = System.currentTimeMillis() + request.remainingMillis();
        if (deadline < 0) deadline = Long.MAX_VALUE;
        var limits = new WasmRuntime.Limits(request.maxMemoryBytes(), deadline,
            request.maxOutputBytes(), request.randomSeed(), request.policy());
        result = program.run(request.stdin(), limits);
      }
      System.out.write(WorkerProtocol.encodeSuccess(result));
      System.out.flush();
    } catch (WasmException e) {
      System.out.write(WorkerProtocol.encodeFailure(e));
      System.out.flush();
    }
  }
}
