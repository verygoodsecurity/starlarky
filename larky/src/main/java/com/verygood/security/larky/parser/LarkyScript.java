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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.ModuleSupplier.ModuleSet;
import com.verygood.security.larky.console.Console;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.syntax.FileOptions;

/**
 * Loads Larky out of Starlark files.
 */
public class LarkyScript {

  public static final String STAR_EXTENSION = ".star";

  /**
   * Modes for parsing config files.
   */
  public enum StarlarkMode {
    LOOSE,
    STRICT
  }

  // Even in strict mode, we allow top-level if and for statements.
  public static final FileOptions STARLARK_STRICT_FILE_OPTIONS =
      FileOptions.DEFAULT.toBuilder()
          .allowToplevelRebinding(true)
          .loadBindsGlobally(true)
          .build();

  public static final FileOptions STARLARK_LOOSE_FILE_OPTIONS =
      STARLARK_STRICT_FILE_OPTIONS.toBuilder()
          .requireLoadStatementsFirst(false)
          .build();

  // For now all the modules are namespaces. We don't use variables except for 'core'.
  @Getter
  private final Iterable<Class<?>> builtinModules;
  @Getter
  private final StarlarkMode validation;
  @Getter
  private final Map<String, Object> globals;
  @Getter
  private final ModuleSet moduleSet;
  @Getter
  private final StarlarkSemantics larkySemantics;

  // TODO: convert to builder
  public LarkyScript(StarlarkMode validation) {
    this(validation, new ModuleSupplier().create());
  }

  public LarkyScript(StarlarkMode validation, ModuleSet moduleSet) {
    this(ImmutableSet.<Class<?>>builder()
            .addAll(ModuleSupplier.CORE_MODULES)
            .build(),
        validation, ImmutableMap.of(), moduleSet);
  }

  public LarkyScript(Set<Class<?>> builtinModules, StarlarkMode validation) {
    this(builtinModules, validation, ImmutableMap.of());
  }

  public LarkyScript(Set<Class<?>> builtinModules, StarlarkMode validation, Map<String, Object> globals) {
    this(ImmutableSet.<Class<?>>builder().addAll(builtinModules).build(), validation, globals, new ModuleSupplier().create());
  }

  public LarkyScript(Set<Class<?>> builtinModules, StarlarkMode validation, Map<String, Object> globals, ModuleSet moduleSet) {
    this(builtinModules, validation, globals, moduleSet, LarkySemantics.LARKY_SEMANTICS);
  }

  public LarkyScript(Set<Class<?>> builtinModules, StarlarkMode validation, Map<String, Object> globals, ModuleSet moduleSet, StarlarkSemantics larkySemantics) {

    this.builtinModules = ImmutableSet.<Class<?>>builder()
        .addAll(builtinModules)
        .build();
    this.validation = validation;
    this.moduleSet = moduleSet;
    // TODO(mahmoudimus): differentiate between globals + module predeclared
    this.globals =
      Stream.concat(
        // order matters because we want globals to override any
        // environmental defaults
        moduleSet.getEnvironment().entrySet().stream(),
        globals.entrySet().stream()
      )
      .filter(Objects::nonNull)
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        entry -> StarlarkUtil.valueToStarlark(entry.getValue()),
        // keep last
        (a, b) -> b)
      );
    this.larkySemantics = larkySemantics;
  }

  private LarkyEvaluator.EvaluationResult executeStarlark(StarFile content, ModuleSet moduleSet, Console console)
    throws IOException, InterruptedException, EvalException {
    CapturingStarFile capturingConfigFile = new CapturingStarFile(content);
    StarFilesSupplier starFilesSupplier = new StarFilesSupplier();

    LarkyEvaluator.EvaluationResult result = new LarkyEvaluator(this, moduleSet, console).eval(content);
    starFilesSupplier.setStarFiles(capturingConfigFile.getAllLoadedFiles());
    return result;
  }

  @VisibleForTesting
  public Module executeSkylark(StarFile content, ModuleSet moduleSet, Console console)
      throws IOException, InterruptedException, EvalException {
    return executeStarlark(content, moduleSet, console).module();
  }

  @VisibleForTesting
  public Object executeSkylarkWithOutput(StarFile content, ModuleSet moduleSet, Console console)
      throws IOException, InterruptedException, EvalException {
    return executeStarlark(content, moduleSet, console).output();
  }

  public ParsedStarFile evaluate(StarFile content, Console console)
      throws IOException, EvalException {
    return evaluate(content, moduleSet, console);
  }

  public ParsedStarFile evaluate(StarFile content, ModuleSet moduleSet, Console console)
      throws IOException, EvalException {
    return getStarFileWithTransitiveImports(content, moduleSet, console).getStarFile();
  }

  /**
   * Collect all ConfigFiles retrieved by the parser while loading {code config}.
   *
   * @param starScriptFile Root file of the configuration.
   * @param console        the console to use for printing error/information
   * @return A map linking paths to the captured StarFile and the parsed StarFile
   * @throws IOException      If files cannot be read
   * @throws RuntimeException If config is invalid, references an invalid file or contains
   *                          dependency cycles.
   */
  public StarFileWithDependencies getStarFileWithTransitiveImports(
      StarFile starScriptFile, ModuleSet moduleSet, Console console)
      throws IOException, EvalException {
    CapturingStarFile capturingConfigFile = new CapturingStarFile(starScriptFile);
    StarFilesSupplier starFilesSupplier = new StarFilesSupplier();

    ParsedStarFile parsedConfig = loadStarFileInternal(capturingConfigFile, moduleSet,
        console);

    ImmutableMap<String, StarFile> allLoadedFiles = capturingConfigFile.getAllLoadedFiles();

    starFilesSupplier.setStarFiles(allLoadedFiles);

    return new StarFileWithDependencies(allLoadedFiles, parsedConfig);
  }

  private ParsedStarFile loadStarFileInternal(StarFile content, ModuleSet moduleSet,
                                              Console console)
      throws IOException, EvalException {
    Module module;
    try {
      module = new LarkyEvaluator(this, moduleSet, console).eval(content).module();
    } catch (InterruptedException e) {
      // This should not happen since we shouldn't have anything interruptable during loading.
      throw new RuntimeException("Internal error", e);
    }
    return new ParsedStarFile(
        content.path(),
        ImmutableMap.<String, Object>builder()
        .putAll(module.getPredeclaredBindings())
        .putAll(module.getGlobals())
        .buildKeepingLast(),
        module);
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
  private static class StarFileWithDependencies {

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

}
