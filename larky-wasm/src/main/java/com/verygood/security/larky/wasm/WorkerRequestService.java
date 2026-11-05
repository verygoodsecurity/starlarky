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
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/** The only worker-server component that decodes or executes a guest request. */
final class WorkerRequestService {
  private final WorkerWasmRuntime runtime;
  private final WasmHostLimits hostLimits;
  private final int maxInputBytes;
  private final long maxRequestMillis;

  WorkerRequestService(WorkerWasmRuntime runtime, long maxRequestMillis) {
    this.runtime = runtime;
    this.hostLimits = WasmHostLimits.configured();
    this.maxInputBytes = WorkerWasmRuntime.configuredMaxInputBytes();
    this.maxRequestMillis = maxRequestMillis;
  }

  long maxRequestBytes() {
    return 8192L + hostLimits.maxModuleBytes() + maxInputBytes;
  }

  byte[] handle(byte[] frame) throws IOException, InterruptedException {
    WorkerProtocol.Request request = WorkerProtocol.readRequest(
        new DataInputStream(new ByteArrayInputStream(frame)),
        hostLimits.maxModuleBytes(), maxInputBytes);
    long budget = Math.min(maxRequestMillis, request.remainingMillis());
    long now = System.currentTimeMillis();
    long deadline = budget > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + budget;
    try {
      WasmRuntime.Result result;
      if (request.operation() == WorkerProtocol.COMPILE) {
        runtime.compile(request.wasm(), deadline);
        result = new WasmRuntime.Result(0, new byte[0], new byte[0]);
      } else {
        result = runtime.runOneShot(request.wasm(), request.stdin(),
            new WasmRuntime.Limits(request.maxMemoryBytes(), deadline,
                request.maxOutputBytes(), request.randomSeed(), request.policy()));
      }
      return WorkerProtocol.encodeSuccess(result);
    } catch (WasmException e) {
      return WorkerProtocol.encodeFailure(e);
    }
  }
}
