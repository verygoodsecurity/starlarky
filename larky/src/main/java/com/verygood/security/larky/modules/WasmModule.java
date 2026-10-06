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

package com.verygood.security.larky.modules;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hashing;
import com.verygood.security.larky.parser.StarFile;
import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import com.verygood.security.larky.wasm.WasmRuntime;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.ThreadExpiration;

/**
 * {@code @vgs//wasm}: runs WebAssembly (WASI preview 1 command) modules shipped with a script.
 *
 * <p>A run gets its input as stdin and returns its stdout. The runtime is the one {@link
 * WasmRuntime#configured()} picks; its deadline is the evaluation's expiration date.
 */
@StarlarkBuiltin(
    name = "wasm",
    category = "BUILTIN",
    doc = "Runs WebAssembly (WASI) modules shipped with the script: stdin in, stdout out.")
public final class WasmModule implements StarlarkValue {

  public static final WasmModule INSTANCE = new WasmModule();

  /** The name of a module made by {@code wasm.loads}. */
  static final String INLINE_NAME = "<inline>";

  static final String MAX_MEMORY_PROPERTY = "larky.wasm.maxMemoryBytes";
  static final String MAX_OUTPUT_PROPERTY = "larky.wasm.maxOutputBytes";
  static final String RANDOM_SEED_PROPERTY = "larky.wasm.randomSeed";
  static final String PROGRAM_CACHE_SIZE_PROPERTY = "larky.wasm.programCache.size";

  private static final int MAX_STDERR_IN_ERROR = 1024;

  // Compiled programs, by runtime name and SHA-256 of the module's bytes.
  private static final Cache<String, WasmRuntime.Program> PROGRAMS =
      CacheBuilder.newBuilder()
          .maximumSize(Long.getLong(PROGRAM_CACHE_SIZE_PROPERTY, 100))
          .build();

  private WasmModule() {}

  @StarlarkMethod(
      name = "module",
      doc =
          "Returns the WebAssembly module in the file named <code>name</code> that was shipped"
              + " with the script.",
      parameters = {@Param(name = "name", allowedTypes = {@ParamType(type = String.class)})},
      useStarlarkThread = true)
  public LoadedWasmModule module(String name, StarlarkThread thread) throws EvalException {
    StarFile script = thread.getThreadLocal(StarFile.class);
    byte[] wasm = script == null ? null : script.readShippedFile(name);
    if (wasm == null) {
      throw Starlark.errorf("wasm.module: no file named '%s'", name);
    }
    return new LoadedWasmModule(name, wasm, compile(name, wasm));
  }

  // `load` is a keyword, so loading is spelled as in Python's pickle and json: loads/dumps.
  @StarlarkMethod(
      name = "loads",
      doc =
          "Returns the WebAssembly module whose binary is <code>data</code> (as"
              + " <code>pickle.loads</code> takes bytes).",
      parameters = {@Param(name = "data", allowedTypes = {@ParamType(type = StarlarkBytes.class)})})
  public LoadedWasmModule loads(StarlarkBytes data) throws EvalException {
    byte[] wasm = data.toByteArray();
    return new LoadedWasmModule(INLINE_NAME, wasm, compile(INLINE_NAME, wasm));
  }

  @StarlarkMethod(
      name = "dumps",
      doc = "Returns the binary of <code>module</code>, which <code>loads</code> accepts.",
      parameters = {
        @Param(name = "module", allowedTypes = {@ParamType(type = LoadedWasmModule.class)})
      })
  public StarlarkBytes dumps(LoadedWasmModule module) {
    return StarlarkBytes.immutableOf(module.wasm.clone());
  }

  private static WasmRuntime.Program compile(String name, byte[] wasm) throws EvalException {
    WasmRuntime runtime;
    try {
      runtime = WasmRuntime.configured();
    } catch (WasmException e) {
      throw new EvalException(e.getMessage()); // e.g. "no WebAssembly runtime named 'x'"
    }
    String key = runtime.name() + ":" + Hashing.sha256().hashBytes(wasm);
    WasmRuntime.Program program = PROGRAMS.getIfPresent(key);
    if (program == null) {
      try {
        program = runtime.compile(wasm);
      } catch (WasmException e) {
        throw new EvalException(String.format("wasm module '%s' is not a valid WASI module", name));
      }
      PROGRAMS.put(key, program);
    }
    return program;
  }

  /** A compiled module, as {@code wasm.module} and {@code wasm.loads} return it. */
  @StarlarkBuiltin(name = "wasm_module", doc = "A WebAssembly module that can be run.")
  public static final class LoadedWasmModule implements StarlarkValue {

    private final String name;
    private final byte[] wasm; // never modified
    private final WasmRuntime.Program program;

    LoadedWasmModule(String name, byte[] wasm, WasmRuntime.Program program) {
      this.name = name;
      this.wasm = wasm;
      this.program = program;
    }

    @StarlarkMethod(name = "name", doc = "The module's file name, or \"<inline>\".", structField = true)
    public String name() {
      return name;
    }

    @StarlarkMethod(
        name = "run",
        doc = "Runs the module with <code>input</code> as stdin and returns its stdout.",
        parameters = {
          @Param(
              name = "input",
              allowedTypes = {
                @ParamType(type = StarlarkBytes.class),
                @ParamType(type = String.class)
              })
        },
        useStarlarkThread = true)
    public StarlarkBytes run(Object input, StarlarkThread thread)
        throws EvalException, InterruptedException {
      byte[] stdin =
          input instanceof StarlarkBytes bytes
              ? bytes.toByteArray()
              : ((String) input).getBytes(UTF_8);
      return StarlarkBytes.immutableOf(execute(stdin, thread));
    }

    @StarlarkMethod(
        name = "call",
        doc =
            "Runs the module with <code>value</code>, JSON-encoded, as stdin and returns its"
                + " stdout, JSON-decoded.",
        parameters = {@Param(name = "value")},
        useStarlarkThread = true)
    public Object call(Object value, StarlarkThread thread)
        throws EvalException, InterruptedException {
      byte[] stdin = JsonModule.INSTANCE.encode(value).getBytes(UTF_8);
      byte[] stdout = execute(stdin, thread);
      return JsonModule.INSTANCE.decode(new String(stdout, UTF_8), thread);
    }

    private byte[] execute(byte[] stdin, StarlarkThread thread)
        throws EvalException, InterruptedException {
      long expirationMs = thread.getExpirationMs();
      WasmRuntime.Limits limits =
          new WasmRuntime.Limits(
              Long.getLong(MAX_MEMORY_PROPERTY, WasmRuntime.Limits.DEFAULT_MAX_MEMORY_BYTES),
              // The thread expires once its clock is past expirationMs; a run stops at its deadline.
              expirationMs == Long.MAX_VALUE ? 0 : expirationMs + 1,
              Integer.getInteger(MAX_OUTPUT_PROPERTY, WasmRuntime.Limits.DEFAULT_MAX_OUTPUT_BYTES),
              Long.getLong(RANDOM_SEED_PROPERTY));
      WasmRuntime.Result result;
      try {
        result = program.run(stdin, limits);
      } catch (WasmException e) {
        throw error(e, limits, thread);
      }
      if (result.exitCode() != 0) {
        byte[] stderr = result.stderr();
        String message =
            new String(stderr, 0, Math.min(stderr.length, MAX_STDERR_IN_ERROR), UTF_8);
        throw Starlark.errorf(
            "wasm module '%s' exited with code %d: %s", name, result.exitCode(), message);
      }
      return result.stdout();
    }

    // The same text whichever runtime ran the module: its own message is never shown.
    private EvalException error(WasmException e, WasmRuntime.Limits limits, StarlarkThread thread)
        throws EvalException {
      switch (e.kind()) {
        case TRAP:
          return Starlark.errorf("wasm module '%s' trapped", name);
        case MEMORY_LIMIT:
          return Starlark.errorf(
              "wasm module '%s' needs more memory than the %d bytes allowed",
              name, limits.maxMemoryBytes());
        case OUTPUT_LIMIT:
          return Starlark.errorf(
              "wasm module '%s' wrote more than %d bytes", name, limits.maxOutputBytes());
        case TIMEOUT:
          // The error evaluation raises at the expiration date (and, like it, marks the thread
          // expired so a script that catches it cannot keep running).
          ThreadExpiration.check(thread);
          return new EvalException("Starlark computation cancelled: past expiration date");
        case INVALID_MODULE:
        default:
          return Starlark.errorf("wasm module '%s' is not a valid WASI module", name);
      }
    }

    @Override
    public boolean isImmutable() {
      return true;
    }

    @Override
    public void repr(Printer printer, StarlarkSemantics semantics) {
      printer.append("<wasm module '").append(name).append("'>");
    }
  }

  /** For tests: forgets every compiled program. */
  static void clearProgramCache() {
    PROGRAMS.invalidateAll();
  }
}
