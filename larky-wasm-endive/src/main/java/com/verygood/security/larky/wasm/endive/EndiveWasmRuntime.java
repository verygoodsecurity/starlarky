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

package com.verygood.security.larky.wasm.endive;

import com.verygood.security.larky.wasm.WasmException;
import com.verygood.security.larky.wasm.WasmException.Kind;
import com.verygood.security.larky.wasm.WasmProgram;
import com.verygood.security.larky.wasm.WasmRuntime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import run.endive.compiler.InterpreterFallback;
import run.endive.compiler.MachineFactoryCompiler;
import run.endive.runtime.HostFunction;
import run.endive.runtime.Instance;
import run.endive.runtime.Machine;
import run.endive.wasi.WasiOptions;
import run.endive.wasi.WasiPreview1;
import run.endive.wasm.Parser;
import run.endive.wasm.WasmModule;
import run.endive.wasm.types.Export;
import run.endive.wasm.types.ExternalType;
import run.endive.wasm.types.FunctionImport;
import run.endive.wasm.types.FunctionType;
import run.endive.wasm.types.Import;
import run.endive.wasm.types.MemoryLimits;

/**
 * Runs WebAssembly with Endive, a WebAssembly runtime written in Java.
 *
 * <p>{@code -Dlarky.wasm.endive.mode=compiler} (the default) translates each module to JVM
 * bytecode once, at {@link #compile}; {@code interpreter} interprets it, which is what a GraalVM
 * native image must use since it cannot load classes generated at run time.
 */
public final class EndiveWasmRuntime implements WasmRuntime {

  /** System property selecting {@link Mode}. */
  public static final String MODE_PROPERTY = "larky.wasm.endive.mode";

  /** How Endive executes a module. */
  public enum Mode {
    /** Translate the module to JVM bytecode (falling back to the interpreter per function). */
    COMPILER,
    /** Interpret the module. */
    INTERPRETER;

    static Mode configured() {
      String value = System.getProperty(MODE_PROPERTY, "");
      switch (value) {
        case "":
        case "compiler":
          return COMPILER;
        case "interpreter":
          return INTERPRETER;
        default:
          throw new IllegalArgumentException(
              "-D" + MODE_PROPERTY + " must be 'compiler' or 'interpreter'; got '" + value + "'");
      }
    }
  }

  static final String WASI_MODULE = "wasi_snapshot_preview1";

  /** Every WASI preview 1 function Endive provides, by name, with its signature. */
  private static final Map<String, FunctionType> WASI_FUNCTIONS = wasiFunctions();

  private final Mode mode;

  /** The runtime in the mode {@code -Dlarky.wasm.endive.mode} selects (used by ServiceLoader). */
  public EndiveWasmRuntime() {
    this(Mode.configured());
  }

  public EndiveWasmRuntime(Mode mode) {
    this.mode = mode;
  }

  public Mode mode() {
    return mode;
  }

  @Override
  public String name() {
    return "endive";
  }

  @Override
  public WasmProgram compile(byte[] wasm) throws WasmException {
    WasmModule module;
    try {
      // Parser.parse also validates the module.
      module = Parser.parse(wasm);
    } catch (RuntimeException e) {
      throw new WasmException(Kind.INVALID_MODULE, "invalid WebAssembly module: " + e.getMessage(), e);
    }
    checkImports(module);
    checkExports(module);
    MemoryLimits declared = module.memorySection().get().getMemory(0).limits();
    Function<Instance, Machine> machineFactory = null;
    if (mode == Mode.COMPILER) {
      try {
        machineFactory =
            MachineFactoryCompiler.builder(module)
                .withInterpreterFallback(InterpreterFallback.SILENT)
                .compile();
      } catch (RuntimeException e) {
        throw new WasmException(
            Kind.INVALID_MODULE, "cannot compile WebAssembly module: " + e.getMessage(), e);
      }
    }
    return new EndiveWasmProgram(module, machineFactory, declared);
  }

  /** Only WASI preview 1 functions, with the signatures Endive implements them with. */
  private static void checkImports(WasmModule module) throws WasmException {
    var imports = module.importSection();
    for (int i = 0; i < imports.importCount(); i++) {
      Import imp = imports.getImport(i);
      String what = "import " + imp.module() + "." + imp.name();
      if (!(imp instanceof FunctionImport function) || !imp.module().equals(WASI_MODULE)) {
        throw new WasmException(
            Kind.INVALID_MODULE, what + " is not a " + WASI_MODULE + " function");
      }
      FunctionType expected = WASI_FUNCTIONS.get(imp.name());
      if (expected == null) {
        throw new WasmException(Kind.INVALID_MODULE, what + " is not a WASI preview 1 function");
      }
      if (!expected.equals(module.typeSection().getType(function.typeIndex()))) {
        throw new WasmException(Kind.INVALID_MODULE, what + " has the wrong signature");
      }
    }
  }

  /** A WASI command: {@code _start: [] -> []} and its own memory, exported as "memory". */
  private static void checkExports(WasmModule module) throws WasmException {
    Export start = null;
    Export memory = null;
    var exports = module.exportSection();
    for (int i = 0; i < exports.exportCount(); i++) {
      Export export = exports.getExport(i);
      if (export.name().equals("_start") && export.exportType() == ExternalType.FUNCTION) {
        start = export;
      } else if (export.name().equals("memory") && export.exportType() == ExternalType.MEMORY) {
        memory = export;
      }
    }
    if (start == null) {
      throw new WasmException(Kind.INVALID_MODULE, "module does not export a _start function");
    }
    if (memory == null) {
      throw new WasmException(Kind.INVALID_MODULE, "module does not export its memory");
    }
    // Imports are all functions (checkImports), so the exported memory is defined by the module.
    if (memory.index() != 0 || module.memorySection().isEmpty()) {
      throw new WasmException(Kind.INVALID_MODULE, "module must export its first memory");
    }
    int importedFunctions = module.importSection().count(ExternalType.FUNCTION);
    FunctionType startType =
        start.index() < importedFunctions
            ? null
            : module
                .functionSection()
                .getFunctionType(start.index() - importedFunctions, module.typeSection());
    if (startType == null || !startType.equals(FunctionType.of(List.of(), List.of()))) {
      throw new WasmException(Kind.INVALID_MODULE, "_start must be a function [] -> []");
    }
  }

  private static Map<String, FunctionType> wasiFunctions() {
    Map<String, FunctionType> functions = new HashMap<>();
    try (WasiPreview1 wasi =
        WasiPreview1.builder()
            .withLogger(QuietLogger.INSTANCE)
            .withOptions(WasiOptions.builder().build())
            .build()) {
      for (HostFunction f : wasi.toHostFunctions()) {
        functions.put(f.name(), f.functionType());
      }
    }
    return Map.copyOf(functions);
  }
}
