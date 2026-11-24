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
import com.verygood.security.larky.runtime.StarlarkInterop;
import com.verygood.security.larky.runtime.LarkyEvaluator;
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

  /**
   * The file options Larky's own modules (stdlib, vendor, vgs) are parsed and resolved with in
   * {@code mode}; see {@link #scriptFileOptions} for a script that is not one of them.
   */
  public static FileOptions fileOptions(StarlarkMode mode) {
    switch (mode) {
      case STRICT:
        return STARLARK_STRICT_FILE_OPTIONS;
      case LOOSE:
        return STARLARK_LOOSE_FILE_OPTIONS;
      default:
        throw new IllegalArgumentException("Undefined StarlarkMode: " + mode);
    }
  }

  /** Whether {@code semantics} enables Starlark's static or dynamic type checking. */
  public static boolean typeChecking(StarlarkSemantics semantics) {
    return semantics.getBool(StarlarkSemantics.EXPERIMENTAL_STARLARK_STATIC_TYPE_CHECKING)
        || semantics.getBool(StarlarkSemantics.EXPERIMENTAL_STARLARK_DYNAMIC_TYPE_CHECKING);
  }

  /**
   * The file options a script that is not one of Larky's own modules is parsed and resolved with:
   * {@link #fileOptions}, plus type syntax when {@code semantics} enables type checking. Every path
   * that parses a script (LarkyEvaluator, and LarkyCompiledScript's compile()) gets them here, so
   * they accept the same scripts.
   */
  public static FileOptions scriptFileOptions(StarlarkMode mode, StarlarkSemantics semantics) {
    FileOptions options = fileOptions(mode);
    return typeChecking(semantics)
        ? options.toBuilder().allowTypeSyntax(true).resolveTypeSyntax(true).build()
        : options;
  }

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
  /** The host's settings for each evaluation this runs; never a binding of the script. */
  @Getter
  private final EvaluationSettings settings;

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

  /** As the five-argument constructor, with no settings ({@link EvaluationSettings#NONE}). */
  public LarkyScript(Set<Class<?>> builtinModules, StarlarkMode validation, Map<String, Object> globals, ModuleSet moduleSet, StarlarkSemantics larkySemantics) {
    this(builtinModules, validation, globals, moduleSet, larkySemantics, EvaluationSettings.NONE);
  }

  /** A script runner whose evaluations run with the host's {@code settings}. */
  public LarkyScript(Set<Class<?>> builtinModules, StarlarkMode validation, Map<String, Object> globals, EvaluationSettings settings) {
    this(builtinModules, validation, globals, new ModuleSupplier().create(), LarkySemantics.LARKY_SEMANTICS, settings);
  }

  /**
   * A script runner whose evaluations run with the host's {@code settings}.
   *
   * @throws IllegalArgumentException if a global uses a retired setting name (see {@link
   *     LarkyEvaluator#refuseRetiredSettings})
   */
  public LarkyScript(Set<Class<?>> builtinModules, StarlarkMode validation, Map<String, Object> globals, ModuleSet moduleSet, StarlarkSemantics larkySemantics, EvaluationSettings settings) {
    LarkyEvaluator.refuseRetiredSettings(globals.keySet());
    this.settings = Objects.requireNonNull(settings, "settings");
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

    LarkyEvaluator.EvaluationResult result = newEvaluator(moduleSet, console).eval(content);
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

  @VisibleForTesting
  protected LarkyEvaluator newEvaluator(ModuleSet moduleSet, Console console) {
    return new LarkyEvaluator(this, moduleSet, console);
  }

  /**
   * Runs {@code content} and returns its output as Java's {@code toString} of it (what the larky
   * runner writes), computed through the evaluation's {@link StarlarkInterop}: the output may be a
   * struct whose {@code __repr__} is Starlark code, which needs a current evaluation.
   */
  public String executeStarlarkWithOutputAsString(
      StarFile content, ModuleSet moduleSet, Console console)
      throws IOException, InterruptedException, EvalException {
    LarkyEvaluator evaluator = newEvaluator(moduleSet, console);
    Object output = evaluator.eval(content).output();
    return evaluator.interop().javaString(output);
  }

  /**
   * As {@link #executeStarlarkWithOutputAsString(StarFile, ModuleSet, Console)}, and gives {@code
   * metadata} what happened in the evaluation, whether it succeeded or failed.
   */
  public String executeStarlarkWithOutputAsString(
      StarFile content,
      ModuleSet moduleSet,
      Console console,
      java.util.function.Consumer<EvaluationMetadata> metadata)
      throws IOException, InterruptedException, EvalException {
    LarkyEvaluator evaluator = newEvaluator(moduleSet, console);
    Object output;
    try {
      output = evaluator.eval(content).output();
    } finally {
      if (evaluator.lastMetadata() != null) {
        metadata.accept(evaluator.lastMetadata());
      }
    }
    return evaluator.interop().javaString(output);
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
   * As {@link #evaluate(StarFile, ModuleSet, Console)}, and gives {@code metadata} what happened
   * in the evaluation whether it succeeded or failed: a failure that surfaces as a plain {@link
   * EvalException} (an error in a file the script loads, say) carries none itself.
   */
  public ParsedStarFile evaluate(
      StarFile content,
      ModuleSet moduleSet,
      Console console,
      java.util.function.Consumer<EvaluationMetadata> metadata)
      throws IOException, EvalException {
    return getStarFileWithTransitiveImports(content, moduleSet, console, metadata).getStarFile();
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
    return getStarFileWithTransitiveImports(starScriptFile, moduleSet, console, null);
  }

  private StarFileWithDependencies getStarFileWithTransitiveImports(
      StarFile starScriptFile,
      ModuleSet moduleSet,
      Console console,
      @javax.annotation.Nullable java.util.function.Consumer<EvaluationMetadata> metadata)
      throws IOException, EvalException {
    CapturingStarFile capturingConfigFile = new CapturingStarFile(starScriptFile);
    StarFilesSupplier starFilesSupplier = new StarFilesSupplier();

    ParsedStarFile parsedConfig = loadStarFileInternal(capturingConfigFile, moduleSet,
        console, metadata);

    ImmutableMap<String, StarFile> allLoadedFiles = capturingConfigFile.getAllLoadedFiles();

    starFilesSupplier.setStarFiles(allLoadedFiles);

    return new StarFileWithDependencies(allLoadedFiles, parsedConfig);
  }

  private ParsedStarFile loadStarFileInternal(StarFile content, ModuleSet moduleSet,
                                              Console console,
                                              @javax.annotation.Nullable java.util.function.Consumer<EvaluationMetadata> metadata)
      throws IOException, EvalException {
    Module module;
    final LarkyEvaluator evaluator = newEvaluator(moduleSet, console);
    try {
      module = evaluator.eval(content).module();
    } catch (InterruptedException e) {
      // This should not happen since we shouldn't have anything interruptable during loading.
      throw new RuntimeException("Internal error", e);
    } finally {
      if (metadata != null && evaluator.lastMetadata() != null) {
        metadata.accept(evaluator.lastMetadata());
      }
    }
    return new ParsedStarFile(
        content.path(),
        ImmutableMap.<String, Object>builder()
        .putAll(module.getPredeclaredBindings())
        .putAll(module.getGlobals())
        .buildKeepingLast(),
        module,
        evaluator.interop(),
        evaluator.lastMetadata());
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
