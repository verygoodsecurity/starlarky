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
import org.graalvm.polyglot.SandboxPolicy;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.ByteSequence;
import org.graalvm.polyglot.io.IOAccess;

/**
 * Runs modules on GraalWasm. The ordinary backend shares one {@link Engine}, so a module parsed
 * once can be reused by later contexts. The opt-in isolate subclass instead creates a new
 * UNTRUSTED engine for validation and every run; the pooled backend owns one engine per trust
 * domain and creates a fresh context per operation.
 *
 * <p>GraalWasm's built-in WASI is not used: {@link #compile} renames every WASI import to
 * {@value #HOST_MODULE}, whose functions each run serves from {@link WasiHost}, as Endive's runs
 * do. GraalWasm has no option to cap linear memory, so each run uses a copy of the module whose
 * memory maximum is capped with {@link WasmBinaries#capMemory}. GraalWasm's own output (log
 * messages such as its warning about vector instructions) is discarded rather than reaching
 * the module's stderr.
 */
class GraalWasmRuntime implements WasmRuntime {

  static final String HOST_MODULE = "larky_wasi_host";
  private final WasmHostLimits limits;
  private final GraalIsolateSettings isolateSettings;

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
    this(null);
  }

  GraalWasmRuntime(GraalIsolateSettings isolateSettings) {
    this(isolateSettings, true);
  }

  GraalWasmRuntime(GraalIsolateSettings isolateSettings, boolean verifyIsolate) {
    this(isolateSettings, verifyIsolate, WasmHostLimits.configured());
  }

  GraalWasmRuntime(GraalIsolateSettings isolateSettings, boolean verifyIsolate,
                   WasmHostLimits hostLimits) {
    limits = hostLimits;
    this.isolateSettings = isolateSettings;
    if (isolateSettings == null && !engine().getLanguages().containsKey("wasm")) {
      throw new IllegalStateException("GraalWasm's wasm language is not on the class path");
    }
    if (isolateSettings != null && verifyIsolate) {
      // Fail at runtime selection if the host lacks GraalVM or the matching isolate image.
      try (Context ignored = newContextBuilder(isolateSettings.compileCpuMillis(), 0).build()) {
        // Building the context validates the UNTRUSTED policy and isolate availability.
      }
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

  Context.Builder newContextBuilder(long cpuMillis, int outputBytes) {
    if (isolateSettings == null) return contextBuilder();
    return isolatedContextBuilder(null, cpuMillis, outputBytes);
  }

  /** Builds an isolated context, optionally on an engine owned by one trust domain. */
  final Context.Builder isolatedContextBuilder(
      Engine pooledEngine, long cpuMillis, int outputBytes) {
    int streamBytes = Math.max(0, outputBytes);
    Context.Builder builder = Context.newBuilder("wasm")
        .sandbox(SandboxPolicy.UNTRUSTED)
        .in(InputStream.nullInputStream())
        .out(OutputStream.nullOutputStream())
        .err(OutputStream.nullOutputStream())
        .logHandler(OutputStream.nullOutputStream())
        .allowIO(IOAccess.NONE)
        .allowHostAccess(HostAccess.UNTRUSTED)
        .allowHostClassLookup(name -> false)
        .allowEnvironmentAccess(EnvironmentAccess.NONE)
        .allowPolyglotAccess(PolyglotAccess.NONE)
        .allowNativeAccess(false)
        .allowCreateThread(false)
        .allowCreateProcess(false)
        .option("sandbox.MaxHeapMemory", isolateSettings.maxGuestHeapBytes() + "B")
        .option("sandbox.MaxCPUTime", cpuMillis + "ms")
        .option("sandbox.MaxThreads", "1")
        .option("sandbox.MaxASTDepth", Integer.toString(isolateSettings.maxAstDepth()))
        .option("sandbox.MaxStackFrames", Integer.toString(isolateSettings.maxStackFrames()))
        .option("sandbox.MaxOutputStreamSize", streamBytes + "B")
        .option("sandbox.MaxErrorStreamSize", streamBytes + "B");
    if (pooledEngine == null) {
      builder.option("engine.MaxIsolateMemory", isolateSettings.maxIsolateMemoryBytes() + "B");
    } else {
      builder.engine(pooledEngine);
    }
    return builder;
  }

  GraalIsolateSettings isolateSettings() {
    return isolateSettings;
  }

  @Override
  public String name() {
    return "graal";
  }

  @Override
  public String compilePolicyKey() {
    return name() + ":" + limits + ":" + isolateSettings;
  }

  @Override
  public WasmRuntime.Program compile(byte[] wasm) throws WasmException {
    if (wasm.length > limits.maxModuleBytes()) {
      throw new WasmException(
          WasmException.Kind.INVALID_MODULE,
          "module is " + wasm.length + " bytes; the limit is " + limits.maxModuleBytes());
    }
    byte[] bytes = wasm.clone();
    EndivePreflight.check(bytes, limits);
    Set<String> imports = WasmBinaries.checkWasiCommand(bytes, limits);
    bytes = WasmBinaries.capTables(bytes, limits);
    byte[] redirected = WasmBinaries.renameImports(bytes, WasiHost.MODULE, imports, HOST_MODULE);
    Source source = source(redirected, isolateSettings == null);
    validate(source);
    return new GraalWasmProgram(this, redirected, source, imports);
  }

  static Source source(byte[] wasm) {
    return source(wasm, true);
  }

  static Source source(byte[] wasm, boolean cached) {
    try {
      return Source.newBuilder("wasm", ByteSequence.create(wasm), "module").cached(cached).build();
    } catch (IOException e) {
      throw new IllegalStateException(e); // a ByteSequence source does no I/O
    }
  }

  /** Parses (and so validates) {@code source} in a throwaway context, without instantiating it. */
  void validate(Source source) throws WasmException {
    long cpuMillis = isolateSettings == null ? 0 : isolateSettings.compileCpuMillis();
    try (Context context = newContextBuilder(cpuMillis, 0).build()) {
      context.eval(source);
    } catch (PolyglotException e) {
      if (e.isResourceExhausted()) {
        throw new WasmException(WasmException.Kind.MEMORY_LIMIT,
            "module validation exceeded the GraalWasm isolate resource limit", e);
      }
      throw new WasmException(
          WasmException.Kind.INVALID_MODULE, "invalid module: " + e.getMessage(), e);
    }
  }
}
