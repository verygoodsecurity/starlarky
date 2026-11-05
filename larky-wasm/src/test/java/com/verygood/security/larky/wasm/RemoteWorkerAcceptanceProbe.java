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
import com.verygood.security.larky.wasm.conformance.Fixtures;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import run.endive.wabt.Wat2Wasm;

/** Manual container probe; the credential volume is mounted only in this trusted client. */
public final class RemoteWorkerAcceptanceProbe {
  private RemoteWorkerAcceptanceProbe() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 1 || !(args[0].equals("smoke") || args[0].equals("healthy")
        || args[0].equals("supervised"))) {
      throw new IllegalArgumentException("expected smoke, healthy, or supervised");
    }
    String host = System.getenv("WASM_WORKER_HOST");
    if (host == null || host.isBlank()) throw new IllegalArgumentException("worker host required");
    System.setProperty("larky.wasm.worker.remote.host", host);
    System.setProperty("larky.wasm.worker.remote.port", "8443");
    System.setProperty("larky.wasm.worker.tls.keyStore", "/secrets/identity.p12");
    System.setProperty("larky.wasm.worker.tls.keyStorePasswordFile", "/secrets/key-password");
    System.setProperty("larky.wasm.worker.tls.trustStore", "/secrets/trust.p12");
    System.setProperty("larky.wasm.worker.tls.trustStorePasswordFile", "/secrets/trust-password");
    WasmRuntime runtime = WasmRuntime.byName("worker");
    if (args[0].equals("supervised")) {
      healthy(runtime);
      Path control = Path.of("/control");
      Files.writeString(control.resolve("ready"), "ready");
      await(control.resolve("resume"));
      healthy(runtime);
      Files.writeString(control.resolve("recovered"), "recovered");
      await(control.resolve("resume2"));
      healthy(runtime);
      System.out.println("same client JVM survived worker OOM and restart");
      return;
    }
    if (args[0].equals("smoke")) {
      expect(WasmException.Kind.INVALID_MODULE,
          () -> runtime.compile(new byte[] {0, 97, 115, 109}));
      System.out.println("malformed parser input: rejected");
      byte[] table = Wat2Wasm.parse("(module (memory (export \"memory\") 1)"
          + " (table 10000000 funcref) (func (export \"_start\")))");
      expect(WasmException.Kind.MEMORY_LIMIT, () -> runtime.compile(table));
      System.out.println("oversized table: rejected");
      WasmRuntime.Program loop = runtime.compile(Fixtures.wasm("spin"));
      expect(WasmException.Kind.TIMEOUT, () -> loop.run(new byte[0],
          limits(600)));
      System.out.println("infinite loop: deadline enforced");
      expect(WasmException.Kind.MEMORY_LIMIT,
          () -> runtime.compile(Fixtures.wasm("bigmem")).run(new byte[0], limits(5000)));
      System.out.println("oversized linear memory: rejected");
      String privateDir = System.getenv("WASM_PRIVATE_FIXTURES");
      if (privateDir != null && !privateDir.isBlank()) {
        Path root = Path.of(privateDir);
        expect(WasmException.Kind.INVALID_MODULE,
            () -> runtime.compile(Files.readAllBytes(root.resolve("parser.wasm"))));
        expect(WasmException.Kind.MEMORY_LIMIT,
            () -> runtime.compile(Files.readAllBytes(root.resolve("table.wasm"))));
        System.out.println("private parser and table probes: rejected");
      }
    }
    healthy(runtime);
  }

  private static void healthy(WasmRuntime runtime) throws Exception {
    byte[] input = "healthy".getBytes(StandardCharsets.US_ASCII);
    byte[] output = runtime.compile(Fixtures.wasm("echo")).run(input, limits(5000)).stdout();
    if (!Arrays.equals(input, output)) throw new AssertionError("worker echo mismatch");
    System.out.println("follow-up request: healthy");
  }

  private static void await(Path signal) throws Exception {
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
    while (!Files.exists(signal)) {
      if (System.nanoTime() >= deadline) throw new AssertionError("supervisor signal missing");
      Thread.sleep(100);
    }
  }

  private static WasmRuntime.Limits limits(long deadlineMillis) {
    return new WasmRuntime.Limits(1 << 20, System.currentTimeMillis() + deadlineMillis, 1024, 0L);
  }

  private static void expect(WasmException.Kind kind, Checked work) throws Exception {
    try {
      work.run();
    } catch (WasmException e) {
      if (e.kind() == kind) return;
      throw e;
    }
    throw new AssertionError("expected " + kind);
  }

  @FunctionalInterface private interface Checked { void run() throws Exception; }
}
