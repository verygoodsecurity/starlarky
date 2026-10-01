package com.verygood.security.larky.parser;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.annot.Library;
import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.modules.utils.Reporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import net.starlark.java.annot.StarlarkAnnotations;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.StarlarkFile;
import net.starlark.java.syntax.SyntaxError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * An utility class for traversing and evaluating the config file dependency graph.
 */
public final class LarkyEvaluator {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  public static final String EXECUTION_STEPS = "_EXECUTION_STEPS_";
  public static final String STEP_LIMIT = "LARKY_EVAL_STEP_LIMIT";
  public static final String EXPIRATION_MS = "LARKY_EVAL_EXPIRATION_MS";

  private final LinkedHashSet<String> pending = new LinkedHashSet<>();
  private final Map<String, Module> loaded = new HashMap<>();
  private final Reporter reporter;
  // Predeclared environment shared by all files (modules) loaded.
  @Getter
  private final ImmutableMap<String, Object> environment;
  @Getter
  private final ModuleSupplier.ModuleSet moduleSet;
  private final LarkyScript.StarlarkMode validationMode;

  @Getter
  private final StarlarkSemantics larkySemantics;

  public LarkyEvaluator(LarkyScript larkyScript, Console console) {
    this(larkyScript, larkyScript.getModuleSet(), console);
  }

  LarkyEvaluator(LarkyScript larkyScript, ModuleSupplier.ModuleSet moduleSet, Console console) {
    this.reporter = new Reporter(checkNotNull(console));
    this.moduleSet = checkNotNull(moduleSet);
    this.validationMode = larkyScript.getValidation();
    this.larkySemantics = larkyScript.getLarkySemantics();
    //todo(mahmoudimus): convert to builder pattern
    this.environment = createEnvironment(larkyScript.getBuiltinModules(), larkyScript.getGlobals());
  }

  /**
   * The output of a Larky script is an evaluated {@link Module} that contains various attributes such as
   * {@link Module#getGlobals()}, {@link Module#getPredeclaredBindings()}, as well as other various items.
   * <p>
   * Sometimes, when evaluating a Larky script, there is some output that is generated.
   * <p>
   * This interface encapsulates an interface that allows the caller to introspect the result of a Larky script
   * evaluation.
   */
  public interface EvaluationResult {

    boolean hasOutput();

    boolean hasModule();

    Object output();

    Module module();

  }

  @Builder
  protected record DefaultEvaluationResult(
      Object output,
      Module module
      )
      implements EvaluationResult {

    public boolean hasOutput() {
      return output != null;
    }

    public boolean hasModule() {
      return module != null;
    }
  }

  public EvaluationResult eval(StarFile content)
      throws IOException, InterruptedException, EvalException {
    if (pending.contains(content.path())) {
      throw throwCycleError(content.path());
    }
    Module module = loaded.get(content.path());
    if (module != null) {
      return DefaultEvaluationResult.builder()
          .module(module)
          .build();
    }
    // Larky's own modules are loaded once per process and shared (see ModuleCache).
    if (content instanceof ResourceContentStarFile) {
      Module shared =
          ModuleCache.lookup(
              content.path(), getStarlarkValidationOptions(), getLarkySemantics(), getEnvironment());
      if (shared != null) {
        loaded.put(content.path(), shared);
        return DefaultEvaluationResult.builder().module(shared).build();
      }
    }
    pending.add(content.path());

    // Make the modules available as predeclared bindings.
    module = Module.withPredeclared(getLarkySemantics(), getEnvironment());

    // parse & compile (Larky's own modules come from a process-wide cache)
    FileOptions options = getStarlarkValidationOptions();
    final Module env = module;
    ProgramCache.Executable prog;
    Map<String, Module> loadedModules;
    if (typeChecking() && !(content instanceof ResourceContentStarFile)) {
      // A typed script is tagged with the types of the modules it loads, so it is compiled here
      // rather than cached. Its annotations may name the classes it defines.
      module.allowForwardTypeReferences();
      Program program =
          compileStarlarkProgram(
              module,
              ParserInput.fromUTF8(content.readContentBytes(), content.path()),
              LarkyScript.scriptFileOptions(validationMode, larkySemantics));
      loadedModules = processLoads(content, program.getLoads());
      prog = ProgramCache.Executable.of(withTypeInfo(program, module, loadedModules));
    } else {
      prog = content instanceof ResourceContentStarFile resource
          ? ProgramCache.get(
              resource.path(),
              env,
              options,
              getLarkySemantics(),
              parsed -> compileStarlarkProgram(
                  env, ParserInput.fromUTF8(resource.readContentBytes(), resource.path()), options,
                  parsed))
          : scriptProgram(content, module, options);
      loadedModules = processLoads(content, prog.loads());
    }

    Object starlarkOutput;

    // execute
    try (Mutability mu = Mutability.create("LarkyModules")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, getLarkySemantics());
      thread.setLoader(loadedModules::get);
      thread.setThreadLocal(Reporter.class, reporter);
      thread.setPrintHandler(reporter::report);

      if (environment.containsKey(STEP_LIMIT) && environment.get(STEP_LIMIT) != null) {
        thread.setMaxExecutionSteps(Long.parseLong(environment.get(STEP_LIMIT).toString()));
      }

      if (environment.containsKey(EXPIRATION_MS) && environment.get(EXPIRATION_MS) != null) {
        thread.setExpirationMs(Long.parseLong(environment.get(EXPIRATION_MS).toString()));
      }

      try {
        starlarkOutput = prog.exec(module, thread);
      } catch (EvalException cause) {
        throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(cause, thread);
      }

      // Set some statistical information
      module.setGlobal(EXECUTION_STEPS, thread.getExecutedSteps());
    }
    if (content instanceof ResourceContentStarFile) {
      ModuleCache.put(
          content.path(), options, getLarkySemantics(), module, prog.predeclaredNames(),
          getEnvironment(), loadedModules);
    }
    pending.remove(content.path());
    loaded.put(content.path(), module);
    return DefaultEvaluationResult.builder()
        .output(starlarkOutput)
        .module(module)
        .build();
  }

  @VisibleForTesting
  static
  class LarkyLoader implements StarlarkThread.Loader {

    private final StarFile content;
    private final LarkyEvaluator evaluator;
    private final ImmutableMap<String, Object> nativeJavaModule;

    LarkyLoader(StarFile content, LarkyEvaluator evaluator) {
      this.content = content;
      this.evaluator = evaluator;
      this.nativeJavaModule = evaluator.getModuleSet().getModules();
    }

    @Nullable
    @Override
    public Module load(String moduleToLoad) {
      Module loadedModule = null;
      try {
        if (!ResourceContentStarFile.startsWithPrefix(moduleToLoad)) {
          loadedModule = evaluator.eval(content.resolve(moduleToLoad + LarkyScript.STAR_EXTENSION)).module();
          return loadedModule;
        }

        //  let's try to load from evaluator env
        String targetModule = ResourceContentStarFile.getModulePath(moduleToLoad);
        if (inEvaluatorEnvironment(targetModule)) {
          loadedModule = fromEvaluatorEnvironment(targetModule);
        }
        /*
         * Check if the module is in the module set. If it is, return a module with an environment
         * of the module that was passed in via the module set.
         */
        else if (isNativeJavaModule(targetModule)) {
          loadedModule = fromNativeModule(targetModule);
        } else {
          // try to load from directory...
          ResourceContentStarFile starFile = ResourceContentStarFile.buildStarFile(moduleToLoad);
          loadedModule = evaluator.eval(starFile).module();
        }

      } catch (IOException | InterruptedException | EvalException e) {
        throw new RuntimeException(
            String.format(
                "Encountered error (%s) while attempting to load %s from module: %s.",
                e.getMessage(),
                moduleToLoad,
                this.content.path()), e);
      }
      return loadedModule;
    }

    private boolean inEvaluatorEnvironment(String moduleToLoad) {
      return evaluator.getEnvironment().containsKey(moduleToLoad);
    }

    private Module fromEvaluatorEnvironment(String moduleToLoad) {
      return (Module) evaluator.getEnvironment().get(moduleToLoad);
    }

    private boolean isNativeJavaModule(String moduleToLoad) {
      return nativeJavaModule.containsKey(moduleToLoad);
    }


    // Wrapper modules for native modules, which are process-wide singletons: one wrapper each.
    private static final java.util.concurrent.ConcurrentHashMap<java.util.List<Object>, Module>
        NATIVE_WRAPPERS = new java.util.concurrent.ConcurrentHashMap<>();

    @NotNull
    private Module fromNativeModule(String moduleToLoad) throws IOException, InterruptedException {
      java.util.List<Object> key =
          java.util.Arrays.asList(
              moduleToLoad,
              System.identityHashCode(nativeJavaModule.get(moduleToLoad)),
              nativeJavaModule.get(moduleToLoad),
              evaluator.getLarkySemantics());
      Module cached = NATIVE_WRAPPERS.get(key);
      if (cached != null) {
        return cached;
      }
      Module wrapper = newNativeModule(moduleToLoad);
      Module previous = NATIVE_WRAPPERS.putIfAbsent(key, wrapper);
      if (previous != null) {
        return previous;
      }
      ModuleCache.putStable(wrapper);
      return wrapper;
    }

    private Module newNativeModule(String moduleToLoad) throws IOException, InterruptedException {
      Module newModule = Module.withPredeclaredAndData(
          evaluator.getLarkySemantics(),
          ImmutableMap.of("_" + moduleToLoad, nativeJavaModule.get(moduleToLoad)),
          moduleToLoad);

      // We have to do this because Starlark Builtins are not actual modules, so as a result, they
      // do not export themselves to the modules.
      //
      // To circumvent around this limitation, we create an in-memory module and just evaluate it
      // to export the methods.
      // TODO(mahmoudimus): Move this to ModuleSupplier?
      try (Mutability mu = Mutability.create("InMemoryNativeModule")) {
        StarlarkThread thread = StarlarkThread.createTransient(mu, evaluator.getLarkySemantics());
        try {
          // Not Starlark.execFile, which would type-check this generated file when the
          // semantics enable type checking (its options do not allow type syntax).
          Starlark.execFileProgram(
              Program.compileFile(
                  StarlarkFile.parse(
                      ParserInput.fromString(
                          String.format("%1$s = _%1$s", moduleToLoad), "<builtin>"),
                      evaluator.getStarlarkValidationOptions()),
                  newModule),
              newModule,
              thread);
        } catch (InterruptedException | EvalException | SyntaxError.Exception e) {
          throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(e, thread);
        }
      }
      return newModule;
    }

  }

  @NotNull
  @VisibleForTesting
  /** Compiles (or finds cached) a script that is not one of Larky's own modules. */
  private ProgramCache.Executable scriptProgram(StarFile content, Module module, FileOptions options)
      throws IOException, EvalException {
    byte[] bytes = content.readContentBytes();
    return ProgramCache.getScript(
        content.cacheNamespace(),
        content.path(),
        new String(bytes, java.nio.charset.StandardCharsets.UTF_8),
        module,
        options,
        getLarkySemantics(),
        parsed -> compileStarlarkProgram(
            module, ParserInput.fromUTF8(bytes, content.path()), options, parsed));
  }

  Map<String, Module> processLoads(StarFile content, List<String> loads) {
    Map<String, Module> loadedModules = new HashMap<>();
    LarkyLoader larkyLoader = new LarkyLoader(content, this);
    for (String load : loads) {
      //Module loadedModule = eval(content.resolve(load + LarkyScript.STAR_EXTENSION));
      Module loadedModule = larkyLoader.load(load);
      loadedModules.put(load, loadedModule);
    }
    return loadedModules;
  }

  @NotNull
  @VisibleForTesting
  Program compileStarlarkProgram(Module module, ParserInput input, FileOptions options) throws EvalException {
    return compileStarlarkProgram(module, input, options, new StarlarkFile[1]);
  }

  /** As above; also stores the parsed file in {@code parsed[0]} once it compiled successfully. */
  private Program compileStarlarkProgram(
      Module module, ParserInput input, FileOptions options, StarlarkFile[] parsed)
      throws EvalException {
    Program prog;
    try {
      StarlarkFile file = StarlarkFile.parse(input, options);
      prog = Program.compileFile(file, module);
      parsed[0] = file;
    } catch (SyntaxError.Exception ex) {
      List<String> errs = new ArrayList<>();
      for (SyntaxError error : ex.errors()) {
        reporter.error(error.toString());
        errs.add(error.toString());
      }
      throw new EvalException(
          String.format(
              "Error compiling Starlark program: %1$s%n" +
              "%2$s",
              input.getFile(),
              String.join("\n", errs)));
    }
    return prog;
  }

  private boolean typeChecking() {
    return LarkyScript.typeChecking(larkySemantics);
  }

  /** Attaches type information to {@code program}, reporting type errors like syntax errors. */
  private Program withTypeInfo(Program program, Module module, Map<String, Module> loadedModules)
      throws EvalException {
    try {
      return Starlark.maybeWithTypeInfo(program, module, larkySemantics, loadedModules::get);
    } catch (SyntaxError.Exception ex) {
      List<String> errs = new ArrayList<>();
      for (SyntaxError error : ex.errors()) {
        reporter.error(error.toString());
        errs.add(error.toString());
      }
      throw new EvalException(
          String.format(
              "Error type checking Starlark program: %1$s%n%2$s",
              program.getFilename(), String.join("\n", errs)));
    }
  }

  FileOptions getStarlarkValidationOptions() throws EvalException {
    try {
      return LarkyScript.fileOptions(validationMode);
    } catch (IllegalArgumentException e) {
      throw new EvalException(e.getMessage());
    }
  }

  private RuntimeException throwCycleError(String cycleElement) throws EvalException {
    StringBuilder sb = new StringBuilder();
    for (String element : pending) {
      sb.append(element.equals(cycleElement) ? "* " : "  ");
      sb.append(element).append("\n");
    }
    sb.append("* ").append(cycleElement).append("\n");
    reporter.error("Cycle was detected in the configuration: \n" + sb);
    throw new EvalException("Cycle was detected");
  }

  /**
   * Create the environment for all evaluations (will be shared between all the dependent files loaded).
   */
  // The bindings of each built-in module class. The classes are stateless (no fields), so one
  // instance per process serves every evaluation; sharing them is what lets cached modules, which
  // capture these values, be reused across evaluations (see ModuleCache).
  private static final java.util.concurrent.ConcurrentHashMap<Class<?>, ImmutableMap<String, Object>>
      BUILTIN_BINDINGS = new java.util.concurrent.ConcurrentHashMap<>();

  private static ImmutableMap<String, Object> builtinBindings(Class<?> module) {
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
    return envBuilder.build();
  }

  // The merged bindings of each set of built-in module classes (every evaluation uses the same
  // few sets), so that an evaluation only adds its own globals to them.
  private static final java.util.concurrent.ConcurrentHashMap<
      com.google.common.collect.ImmutableList<Class<?>>, ImmutableMap<String, Object>>
      MERGED_BUILTIN_BINDINGS = new java.util.concurrent.ConcurrentHashMap<>();

  private static ImmutableMap<String, Object> mergedBuiltinBindings(
      Iterable<Class<?>> globalModules) {
    ImmutableMap.Builder<String, Object> env = ImmutableMap.builder();
    for (Class<?> module : globalModules) {
      logger.atFine().log("Creating variable for %s", module.getName());
      // Create the module object and associate it with the functions
      env.putAll(BUILTIN_BINDINGS.computeIfAbsent(module, LarkyEvaluator::builtinBindings));
    }
    return env.buildKeepingLast();
  }

  private ImmutableMap<String, Object> createEnvironment(Iterable<Class<?>> globalModules,
      Map<String, Object> globals) {
    return ImmutableMap.<String, Object>builder()
        .putAll(MERGED_BUILTIN_BINDINGS.computeIfAbsent(
            com.google.common.collect.ImmutableList.copyOf(globalModules),
            LarkyEvaluator::mergedBuiltinBindings))
        .putAll(globals)
        .buildKeepingLast();
  }

}
