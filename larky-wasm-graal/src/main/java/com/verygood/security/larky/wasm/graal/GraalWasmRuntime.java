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

package com.verygood.security.larky.wasm.graal;

import com.verygood.security.larky.wasm.WasmBinaries;
import com.verygood.security.larky.wasm.WasmException;
import com.verygood.security.larky.wasm.WasmProgram;
import com.verygood.security.larky.wasm.WasmRuntime;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.ByteSequence;
import org.graalvm.polyglot.io.IOAccess;

/**
 * Runs modules on GraalWasm. All runtimes share one {@link Engine}, so a module parsed once is
 * reused by every later context that evaluates the same {@link Source}.
 *
 * <p>GraalWasm's built-in WASI reads the host clock and random source, and no option changes
 * that, so {@link #compile} renames the module's {@code clock_time_get} and {@code random_get}
 * imports to {@value #HOST_MODULE}, which each run supplies itself. GraalWasm has no option to
 * cap linear memory either, so each run uses a copy of the module whose memory maximum is capped
 * with {@link WasmBinaries#capMemory}.
 */
public final class GraalWasmRuntime implements WasmRuntime {

  static final String WASI_MODULE = "wasi_snapshot_preview1";
  static final String HOST_MODULE = "larky_wasi_host";
  static final Set<String> HOST_FUNCTIONS = Set.of("clock_time_get", "random_get");

  private static final class EngineHolder {
    static final Engine ENGINE =
        Engine.newBuilder("wasm")
            .option("engine.WarnInterpreterOnly", "false")
            .build();
  }

  static Engine engine() {
    return EngineHolder.ENGINE;
  }

  /**
   * A context builder on the shared engine with nothing but the guest allowed. Every context on
   * one engine must use the same host access policy.
   */
  static Context.Builder contextBuilder() {
    return Context.newBuilder("wasm")
        .engine(engine())
        .allowIO(IOAccess.NONE)
        .allowHostAccess(HostAccess.NONE)
        .allowHostClassLookup(name -> false)
        .allowEnvironmentAccess(EnvironmentAccess.NONE)
        .allowPolyglotAccess(PolyglotAccess.NONE)
        .allowNativeAccess(false)
        .allowCreateThread(false)
        .allowCreateProcess(false);
  }

  @Override
  public String name() {
    return "graal";
  }

  @Override
  public WasmProgram compile(byte[] wasm) throws WasmException {
    byte[] bytes = wasm.clone();
    if (!WasmBinaries.hasExport(bytes, "_start", WasmBinaries.KIND_FUNC)) {
      throw new WasmException(WasmException.Kind.INVALID_MODULE, "module does not export _start");
    }
    if (!WasmBinaries.hasExport(bytes, "memory", WasmBinaries.KIND_MEMORY)) {
      throw new WasmException(WasmException.Kind.INVALID_MODULE, "module does not export memory");
    }
    byte[] redirected = WasmBinaries.renameImports(bytes, WASI_MODULE, HOST_FUNCTIONS, HOST_MODULE);
    Source source = source(redirected);
    validate(source);
    return new GraalWasmProgram(redirected, source);
  }

  static Source source(byte[] wasm) {
    try {
      return Source.newBuilder("wasm", ByteSequence.create(wasm), "module").cached(true).build();
    } catch (IOException e) {
      throw new IllegalStateException(e); // a ByteSequence source does no I/O
    }
  }

  /** Parses (and so validates) {@code source} in a throwaway context, without instantiating it. */
  static void validate(Source source) throws WasmException {
    try (Context context = contextBuilder().in(new ByteArrayInputStream(new byte[0])).build()) {
      context.eval(source);
    } catch (PolyglotException e) {
      throw new WasmException(
          WasmException.Kind.INVALID_MODULE, "invalid module: " + e.getMessage(), e);
    }
  }
}
