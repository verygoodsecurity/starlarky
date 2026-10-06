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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * <p>GraalWasm's built-in WASI is not used: {@link #compile} renames every WASI import to
 * {@value #HOST_MODULE}, whose functions each run serves from {@link WasiHost}, as Endive's runs
 * do. GraalWasm has no option to cap linear memory, so each run uses a copy of the module whose
 * memory maximum is capped with {@link WasmBinaries#capMemory}. GraalWasm's own output (log
 * messages such as its warning about vector instructions) is discarded rather than reaching
 * the module's stderr.
 */
final class GraalWasmRuntime implements WasmRuntime {

  static final String HOST_MODULE = "larky_wasi_host";

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
   * Checks that the shared engine has GraalWasm's language. Without it (the polyglot API but not
   * {@code wasm-community} on the class path) the engine still starts, and every compile would
   * throw {@link IllegalArgumentException}; failing here makes {@link WasmRuntime#byName} report a
   * {@link WasmException} instead.
   */
  GraalWasmRuntime() {
    if (!engine().getLanguages().containsKey("wasm")) {
      throw new IllegalStateException("GraalWasm's wasm language is not on the class path");
    }
  }

  /**
   * A context builder on the shared engine with nothing but the guest allowed. Every context on
   * one engine must use the same host access policy.
   */
  static Context.Builder contextBuilder() {
    return Context.newBuilder("wasm")
        .engine(engine())
        .in(InputStream.nullInputStream())
        .out(OutputStream.nullOutputStream())
        .err(OutputStream.nullOutputStream())
        .logHandler(OutputStream.nullOutputStream())
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
  public WasmRuntime.Program compile(byte[] wasm) throws WasmException {
    if (wasm.length > MAX_MODULE_BYTES) {
      throw new WasmException(
          WasmException.Kind.INVALID_MODULE,
          "module is " + wasm.length + " bytes; the limit is " + MAX_MODULE_BYTES);
    }
    byte[] bytes = wasm.clone();
    Set<String> imports = WasmBinaries.checkWasiCommand(bytes);
    byte[] redirected = WasmBinaries.renameImports(bytes, WasiHost.MODULE, imports, HOST_MODULE);
    Source source = source(redirected);
    validate(source);
    return new GraalWasmProgram(redirected, source, imports);
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
    try (Context context = contextBuilder().build()) {
      context.eval(source);
    } catch (PolyglotException e) {
      throw new WasmException(
          WasmException.Kind.INVALID_MODULE, "invalid module: " + e.getMessage(), e);
    }
  }
}
