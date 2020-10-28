/*
 * Copyright (C) 2016 Google Inc.
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

package com.verygood.security.larky.parser;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.flogger.FluentLogger;

import com.verygood.security.larky.annot.Library;
import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.ModuleSet;

import net.starlark.java.annot.StarlarkAnnotations;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.StarlarkFile;
import net.starlark.java.syntax.SyntaxError;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Loads Larky out of Starlark files.
 */
public class LarkyParser {

  /**
   * Modes for parsing config files.
   */
  public enum StarlarkMode {
    LOOSE,
    STRICT
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String STAR_EXTENSION = ".star";
  // For now all the modules are namespaces. We don't use variables except for 'core'.
  private final Iterable<Class<?>> globalModules;
  private final StarlarkMode validation;

  public LarkyParser(Set<Class<?>> globalModules, StarlarkMode validation) {
    this.globalModules = ImmutableSet.<Class<?>>builder()
        .addAll(globalModules).build();
    this.validation = validation;
  }

  public ParsedStarFile loadStarFile(StarFile config, ModuleSet moduleSet, Console console)
      throws IOException {
    return getStarFileWithTransitiveImports(config, moduleSet, console).starFile;
  }

  private ParsedStarFile loadStarFileInternal(StarFile content, ModuleSet moduleSet,
                                              Console console)
      throws IOException {
    Module module;
    try {
      module = new Evaluator(moduleSet, console).eval(content);
    } catch (InterruptedException e) {
      // This should not happen since we shouldn't have anything interruptable during loading.
      throw new RuntimeException("Internal error", e);
    }
    return new ParsedStarFile(content.path(), module.getTransitiveBindings());
  }

  @VisibleForTesting
  public Module executeSkylark(StarFile content, ModuleSet moduleSet, Console console)
      throws IOException, InterruptedException {
    CapturingStarFile capturingConfigFile = new CapturingStarFile(content);
    StarFilesSupplier starFilesSupplier = new StarFilesSupplier();

    Module module = new Evaluator(moduleSet, console).eval(content);
    starFilesSupplier.setStarFiles(capturingConfigFile.getAllLoadedFiles());
    return module;
  }

  /**
   * Collect all ConfigFiles retrieved by the parser while loading {code config}.
   *
   * @param starScriptFile Root file of the configuration.
   * @param console the console to use for printing error/information
   * @return A map linking paths to the captured StarFile and the parsed StarFile
   * @throws IOException If files cannot be read
   * @throws RuntimeException If config is invalid, references an invalid file or contains
   *     dependency cycles.
   */
  public StarFileWithDependencies getStarFileWithTransitiveImports(
      StarFile starScriptFile, ModuleSet moduleSet, Console console)
      throws IOException {
    CapturingStarFile capturingConfigFile = new CapturingStarFile(starScriptFile);
    StarFilesSupplier starFilesSupplier = new StarFilesSupplier();

    ParsedStarFile parsedConfig = loadStarFileInternal(capturingConfigFile, moduleSet,
        console);

    ImmutableMap<String, StarFile> allLoadedFiles = capturingConfigFile.getAllLoadedFiles();

    starFilesSupplier.setStarFiles(allLoadedFiles);

    return new StarFileWithDependencies(allLoadedFiles, parsedConfig);
  }

  private static class StarFilesSupplier
      implements Supplier<ImmutableMap<String, StarFile>> {

    private ImmutableMap<String, StarFile> starFiles = null;

    void setStarFiles(ImmutableMap<String, StarFile> starFiles) {
      Preconditions.checkState(this.starFiles == null, "Already set");
      this.starFiles = checkNotNull(starFiles);
    }

    @Override
    public ImmutableMap<String, StarFile> get() {
      // We need to load all the files before knowing the set of files in the config.
      checkNotNull(starFiles, "Don't call the supplier before loading"
          + " finishes.");
      return starFiles;
    }
  }

  /**
   * A class that contains a loaded config and all the config files that were
   * accessed during the parsing.
   */
  public static class StarFileWithDependencies {
    private final ImmutableMap<String, StarFile> allFiles;
    private final ParsedStarFile starFile;

    private StarFileWithDependencies(ImmutableMap<String, StarFile> allFiles, ParsedStarFile starFile) {
      this.starFile = starFile;
      this.allFiles = allFiles;
    }

    public ParsedStarFile getStarFile() {
      return starFile;
    }

    public ImmutableMap<String, StarFile> getAllFiles() {
      return allFiles;
    }
  }

  /**
   * An utility class for traversing and evaluating the config file dependency graph.
   */
  private final class Evaluator {

    private final LinkedHashSet<String> pending = new LinkedHashSet<>();
    private final Map<String, Module> loaded = new HashMap<>();
    private final Console console;
    // Predeclared environment shared by all files (modules) loaded.
    private final ImmutableMap<String, Object> environment;
    private final ModuleSet moduleSet;

    private Evaluator(ModuleSet moduleSet, Console console) {
      this.console = checkNotNull(console);
      this.moduleSet = checkNotNull(moduleSet);
      this.environment = createEnvironment(this.moduleSet);
    }

    private Module eval(StarFile content)
        throws IOException, InterruptedException {
      if (pending.contains(content.path())) {
        throw throwCycleError(content.path());
      }
      Module module = loaded.get(content.path());
      if (module != null) {
        return module;
      }
      pending.add(content.path());

      // Make the modules available as predeclared bindings.
      StarlarkSemantics semantics = StarlarkSemantics.DEFAULT;
      module = Module.withPredeclared(semantics, environment);

      // parse & compile
      ParserInput input = ParserInput.fromUTF8(content.readContentBytes(), content.path());
      FileOptions options;
      if (validation == StarlarkMode.STRICT) {
        options = STARLARK_STRICT_FILE_OPTIONS;
      } else if (validation == StarlarkMode.LOOSE) {
        options = STARLARK_LOOSE_FILE_OPTIONS;
      }
      else {
        throw new RuntimeException("Undefined StarlarkMode: " + validation);
      }
      Program prog;
      try {
        prog = Program.compileFile(StarlarkFile.parse(input, options), module);
      } catch (SyntaxError.Exception ex) {
        for (SyntaxError error : ex.errors()) {
          console.error(error.toString());
        }
        throw new RuntimeException("Error loading config file.");
      }

      // process loads
      Map<String, Module> loadedModules = new HashMap<>();
      for (String load : prog.getLoads()) {
        Module loadedModule = eval(content.resolve(load + STAR_EXTENSION));
        loadedModules.put(load, loadedModule);
      }

      // execute
      try (Mutability mu = Mutability.create("LarkyModules")) {
        StarlarkThread thread = new StarlarkThread(mu, semantics);
        thread.setLoader(loadedModules::get);
        thread.setPrintHandler(this::starlarkPrint);
        Starlark.execFileProgram(prog, module, thread);
      } catch (EvalException ex) {
        console.error(ex.getMessageWithStack());
        throw new RuntimeException("Error loading config file", ex);
      }

      pending.remove(content.path());
      loaded.put(content.path(), module);
      return module;
    }

    private void starlarkPrint(StarlarkThread thread, String msg) {
      console.verbose(thread.getCallerLocation() + ": " + msg);
    }

    private RuntimeException throwCycleError(String cycleElement) {
      StringBuilder sb = new StringBuilder();
      for (String element : pending) {
        sb.append(element.equals(cycleElement) ? "* " : "  ");
        sb.append(element).append("\n");
      }
      sb.append("* ").append(cycleElement).append("\n");
      console.error("Cycle was detected in the configuration: \n" + sb);
      throw new RuntimeException("Cycle was detected");
    }
  }

  // Even in strict mode, we allow top-level if and for statements.
  private static final FileOptions STARLARK_STRICT_FILE_OPTIONS =
      FileOptions.DEFAULT.toBuilder()
          .allowToplevelRebinding(true)
          .build();

  private static final FileOptions STARLARK_LOOSE_FILE_OPTIONS =
      STARLARK_STRICT_FILE_OPTIONS.toBuilder()
          .restrictStringEscapes(true)
          .requireLoadStatementsFirst(false)
          .build();

  /**
   * Create the environment for all evaluations (will be shared between all the dependent files
   * loaded).
   */
  private ImmutableMap<String, Object> createEnvironment(ModuleSet moduleSet) {
    Map<String, Object> env = Maps.newHashMap();
    for (Entry<String, Object> module : moduleSet.getModules().entrySet()) {
      logger.atInfo().log("Creating variable for %s", module.getKey());
      // Modules shouldn't use the same name
      env.put(module.getKey(), module.getValue());
    }

    for (Class<?> module : globalModules) {
      logger.atInfo().log("Creating variable for %s", module.getName());
      // Create the module object and associate it with the functions
      ImmutableMap.Builder<String, Object> envBuilder = ImmutableMap.builder();
      try {
        StarlarkBuiltin annot = StarlarkAnnotations.getStarlarkBuiltin(module);
        if (annot != null) {
          envBuilder.put(annot.name(), module.getConstructor().newInstance());
        } else if (module.isAnnotationPresent(Library.class)) {
          Starlark.addMethods(envBuilder, module.getConstructor().newInstance());
        }
      } catch (ReflectiveOperationException e) {
        throw new AssertionError(e);
      }
      env.putAll(envBuilder.build());
    }
    return ImmutableMap.copyOf(env);
  }

}
