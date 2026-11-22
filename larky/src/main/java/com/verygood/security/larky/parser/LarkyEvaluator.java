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
  /**
   * The prefix of the bindings that once carried a run's settings ({@code LARKY_EVAL_STEP_LIMIT},
   * {@code LARKY_EVAL_EXPIRATION_MS}, {@code LARKY_EVAL_MODULE_PROVIDERS}, {@code
   * LARKY_EVAL_SETTINGS}). Settings are now an {@link EvaluationSettings} passed outside the
   * bindings, and a binding with such a name is refused.
   */
  public static final String RETIRED_SETTING_PREFIX = "LARKY_EVAL_";

  /** The global that once reported a run's steps; see {@link EvaluationMetadata} instead. */
  public static final String RETIRED_EXECUTION_STEPS = "_EXECUTION_STEPS_";

  /**
   * Whether {@code name} is one of the retired setting names: it starts with {@link
   * #RETIRED_SETTING_PREFIX} or is {@link #RETIRED_EXECUTION_STEPS}. A binding with such a name is
   * refused ({@link #refuseRetiredSettings}), and a script's top-level variable with such a name is
   * never copied back into a host's bindings.
   */
  public static boolean isRetiredSettingName(String name) {
    return name.startsWith(RETIRED_SETTING_PREFIX) || name.equals(RETIRED_EXECUTION_STEPS);
  }

  /**
   * Refuses bindings that use a retired setting name, so a host that still passes its settings as
   * bindings fails instead of running without them.
   *
   * @throws IllegalArgumentException naming the first such binding
   */
  public static void refuseRetiredSettings(Iterable<String> names) {
    for (String name : names) {
      if (isRetiredSettingName(name)) {
        throw new IllegalArgumentException(
            name + " is no longer read from bindings: pass the run's settings as an "
                + EvaluationSettings.class.getName()
                + " (LarkyScriptContext#setSettings for JSR-223, or LarkyScript's settings"
                + " parameter), and read its steps from EvaluationMetadata");
      }
    }
  }

  private final LinkedHashSet<String> pending = new LinkedHashSet<>();
  private final Map<String, Module> loaded = new HashMap<>();

  /** The module this evaluator loaded from {@code path} (a resource path), for tests. */
  @VisibleForTesting
  @Nullable
  Module loadedModule(String path) {
    return loaded.get(path);
  }
  private final Reporter reporter;
  // Predeclared environment shared by all files (modules) loaded.
  @Getter
  private final ImmutableMap<String, Object> environment;
  // The host's settings for this run (LarkyScript#getSettings).
  @Getter
  private final EvaluationSettings settings;
  @Getter
  private final ModuleSupplier.ModuleSet moduleSet;
  private final LarkyScript.StarlarkMode validationMode;

  @Getter
  private final StarlarkSemantics larkySemantics;
  private final ModuleCache moduleCache;
  @Nullable private final ModuleCache.Registry moduleRegistry;
  // The predeclared builtins, created once per process and shared by every evaluation whatever
  // the module cache does (set by createEnvironment).
  private ImmutableMap<String, Object> builtinBindings = ImmutableMap.of();
  // ...and with them ModuleSupplier.CORE_ENVIRONMENT's (object, type, super...): the predeclared
  // values that are the same objects in every evaluation of the process.
  private ImmutableMap<String, Object> sharedBuiltins = ImmutableMap.of();

  public LarkyEvaluator(LarkyScript larkyScript, Console console) {
    this(larkyScript, larkyScript.getModuleSet(), console);
  }

  LarkyEvaluator(LarkyScript larkyScript, ModuleSupplier.ModuleSet moduleSet, Console console) {
    this.reporter = new Reporter(checkNotNull(console));
    this.moduleSet = checkNotNull(moduleSet);
    this.validationMode = larkyScript.getValidation();
    this.larkySemantics = larkyScript.getLarkySemantics();
    this.moduleCache = larkyScript.getModuleCache();
    this.moduleRegistry =
        moduleCache.isEnabled() ? ModuleCache.Registry.from(moduleSet.getModules()) : null;
    //todo(mahmoudimus): convert to builder pattern
    this.settings = larkyScript.getSettings();
    this.environment = createEnvironment(larkyScript.getBuiltinModules(), larkyScript.getGlobals());
  }

  /**
   * How host code reads the values of what this evaluator ran: with this evaluator's semantics,
   * the steps left of its step limit, its expiration date, and its reporter (where {@code print()}
   * of callbacks went when they ran on the evaluation's own thread), each access on a fresh
   * thread. See {@link HostValues}.
   */
  public HostValues hostValues() {
    return HostValues.of(
        getLarkySemantics(),
        settings.stepLimit().isPresent()
            ? Long.valueOf(Math.max(0, settings.stepLimit().getAsLong() - chargedSteps()))
            : null,
        lastExpirationMs,
        settings.moduleProviders())
        .withPrintHandler(reporter::report);
  }

  /**
   * What happened in the last evaluation this evaluator ran ({@link #eval} of a script, loads
   * included), or null before the first one has ended.
   */
  @Nullable
  public EvaluationMetadata lastMetadata() {
    return lastMetadata;
  }

  // eval() of the script calls eval() of each file it loads; the outermost call is the
  // evaluation, which measures the time and steps of all of them.
  private int evalDepth;
  private long evaluationSteps; // the script's own files (EvaluationMetadata.executedSteps)
  private long libraryInitSteps; // Larky's own modules' top-level code
  // With the module cache on, why each Larky module this evaluation ran was not shared.
  private final Map<String, String> uncachedModules = new java.util.LinkedHashMap<>();

  // what the evaluation has used of its step limit
  private long chargedSteps() {
    return evaluationSteps + (settings.chargeLibraryInit() ? libraryInitSteps : 0);
  }
  @Nullable private EvaluationMetadata.Outcome failure;
  @Nullable private EvaluationMetadata lastMetadata;

  // The expiration date of the last file this evaluator executed (the main script, whose loads
  // execute before it), and with evaluationSteps what hostValues() continues from.
  private long lastExpirationMs = Long.MAX_VALUE;

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
    if (evalDepth > 0) {
      return evalFile(content, null);
    }
    return evalEntry(content, null);
  }

  <I, O> O compute(StarFile content, List<String> imports, I input, Computation<I, O> computation)
      throws IOException, InterruptedException, EvalException {
    var pending = new PendingComputation<>(List.copyOf(imports), input, computation);
    evalEntry(content, pending);
    return pending.output;
  }

  private static final class PendingComputation<I, O> {
    final List<String> imports;
    final I input;
    final Computation<I, O> computation;
    O output;

    PendingComputation(List<String> imports, I input, Computation<I, O> computation) {
      this.imports = imports;
      this.input = input;
      this.computation = computation;
    }

    void run(Module module, Map<String, Module> loadedModules, StarlarkThread thread)
        throws EvalException, InterruptedException {
      checkComputationLimits(thread);
      var scope = new Computation.Scope(module, loadedModules, thread.mutability());
      output = computation.run(scope, input);
      checkComputationLimits(thread);
    }
  }

  private static void checkComputationLimits(StarlarkThread thread) throws EvalException, InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    if (thread.getExecutedSteps() >= thread.getMaxExecutionSteps()) {
      throw Starlark.errorf(TOO_MANY_STEPS);
    }
    if (thread.getExpirationMs() != Long.MAX_VALUE && thread.isExpired()) {
      throw Starlark.errorf(PAST_EXPIRATION);
    }
  }

  private EvaluationResult evalEntry(StarFile content, @Nullable PendingComputation<?, ?> computation)
      throws IOException, InterruptedException, EvalException {
    evalDepth++;
    evaluationSteps = 0;
    libraryInitSteps = 0;
    uncachedModules.clear();
    failure = null;
    long start = System.nanoTime();
    try {
      EvaluationResult result = evalFile(content, computation);
      lastMetadata = metadata(EvaluationMetadata.Outcome.OK, start);
      return result;
    } catch (StarlarkEvalWrapper.Exc.RuntimeEvalException e) {
      lastMetadata =
          metadata(failure != null ? failure : EvaluationMetadata.Outcome.SCRIPT_ERROR, start);
      e.setMetadata(lastMetadata);
      throw e;
    } catch (EvalException e) {
      // a file the script loads failed (processLoads reports the author's own files unwrapped),
      // or the script never ran: a syntax error, a load cycle, a missing module
      lastMetadata =
          metadata(failure != null ? failure : EvaluationMetadata.Outcome.SCRIPT_ERROR, start);
      throw e;
    } catch (InterruptedException e) {
      lastMetadata = metadata(EvaluationMetadata.Outcome.INTERRUPTED, start);
      throw e;
    } catch (RuntimeException | Error e) {
      lastMetadata = metadata(EvaluationMetadata.Outcome.INTERNAL, start);
      throw e;
    } finally {
      evalDepth--;
    }
  }

  private EvaluationMetadata metadata(EvaluationMetadata.Outcome outcome, long start) {
    return new EvaluationMetadata(
        outcome,
        evaluationSteps,
        libraryInitSteps,
        System.nanoTime() - start,
        EvaluationMetadata.TREE_WALKER,
        settings,
        uncachedModules);
  }

  private static final String TOO_MANY_STEPS = "Starlark computation cancelled: too many steps";
  private static final String PAST_EXPIRATION =
      "Starlark computation cancelled: past expiration date";

  /** Classifies a script failure using the error message and the thread's execution limits. */
  public static EvaluationMetadata.Outcome failureOf(StarlarkThread thread, EvalException cause) {
    String message = cause.getMessage();
    if (TOO_MANY_STEPS.equals(message)
        && thread.getExecutedSteps() >= thread.getMaxExecutionSteps()) {
      return EvaluationMetadata.Outcome.STEP_LIMIT;
    }
    if (PAST_EXPIRATION.equals(message) && thread.isExpired()) {
      return EvaluationMetadata.Outcome.DEADLINE;
    }
    return EvaluationMetadata.Outcome.SCRIPT_ERROR;
  }

  private EvaluationResult evalFile(StarFile content, @Nullable PendingComputation<?, ?> computation)
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
    ResourceContentStarFile library =
        computation == null && content instanceof ResourceContentStarFile resource && resource.isBundledLibrary()
            ? resource
            : null;
    boolean libraryFile = library != null;
    boolean shareLibrary = libraryFile && moduleRegistry != null;
    if (shareLibrary) {
      Module shared =
          moduleCache.lookup(
              content.path(), getStarlarkValidationOptions(), getLarkySemantics(), moduleRegistry,
              getEnvironment());
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
    if (typeChecking() && !libraryFile) {
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
      prog = library != null
          ? ProgramCache.get(
              library.path(),
              env,
              options,
              getLarkySemantics(),
              parsed -> compileStarlarkProgram(
                  env, ParserInput.fromUTF8(library.readContentBytes(), library.path()), options,
                  parsed))
          : scriptProgram(content, module, options);
      loadedModules = processLoads(content, prog.loads());
    }

    if (computation != null && !computation.imports.isEmpty()) {
      List<String> extraLoads = new ArrayList<>(computation.imports);
      extraLoads.removeAll(loadedModules.keySet());
      if (!extraLoads.isEmpty()) {
        loadedModules.putAll(processLoads(content, extraLoads));
      }
    }

    if (shareLibrary && prog.predeclaredNames() != null) {
      // A cached module must not retain unrelated bindings from the evaluation that loaded it.
      ImmutableMap.Builder<String, Object> used = ImmutableMap.builder();
      for (String name : prog.predeclaredNames()) {
        Object value = getEnvironment().get(name);
        if (value != null) {
          used.put(name, value);
        }
      }
      module = Module.withPredeclared(getLarkySemantics(), used.buildOrThrow());
    }

    Object starlarkOutput;

    // execute
    try (Mutability mu = Mutability.create("LarkyModules")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, getLarkySemantics());
      thread.setLoader(loadedModules::get);
      thread.setThreadLocal(Reporter.class, reporter);
      thread.setPrintHandler(reporter::report);
      // the step limit is the evaluation's: this file gets what the files before it left
      settings.applyTo(thread, libraryFile, chargedSteps());

      try (CurrentEvaluation ignored = CurrentEvaluation.enter(thread)) {
        starlarkOutput = prog.exec(module, thread);
        if (computation != null) {
          computation.run(module, loadedModules, thread);
        }
      } catch (EvalException cause) {
        if (failure == null) {
          failure = failureOf(thread, cause);
        }
        throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(cause, thread);
      } finally {
        if (libraryFile) {
          libraryInitSteps += thread.getExecutedSteps();
        } else {
          evaluationSteps += thread.getExecutedSteps();
        }
      }

      lastExpirationMs = thread.getExpirationMs();
    }
    if (shareLibrary) {
      ModuleAdmission.Decline declined =
          moduleCache.put(
              content.path(), options, getLarkySemantics(), moduleRegistry, module,
              prog.predeclaredNames(), prog.universalNames(), getEnvironment(), sharedBuiltins,
              loadedModules);
      if (declined != null) {
        String label = ResourceContentStarFile.labelOf(content.path());
        uncachedModules.put(label, declined.toString());
        UncachedModuleLog.once(label, declined);
      } else {
        // A compatible winner may have been published while this evaluation initialized the
        // module. Use it for further loads so a parent depends on an admitted instance.
        Module shared =
            moduleCache.lookup(content.path(), options, getLarkySemantics(), moduleRegistry,
                getEnvironment());
        if (shared != null) {
          module = shared;
        }
      }
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
        checkNamespace(moduleToLoad);
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
          checkNotTestOnly(moduleToLoad, targetModule);
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

    /**
     * Fails if {@code moduleToLoad} names a native module under a namespace other than its own,
     * e.g. {@code @vgs//json} for {@code @stdlib//json}.
     */
    void checkNamespace(String moduleToLoad) throws EvalException {
      if (!ResourceContentStarFile.startsWithPrefix(moduleToLoad)) {
        return;
      }
      String[] label = ResourceContentStarFile.splitLabel(moduleToLoad);
      if (label == null || !isNativeJavaModule(label[1])) {
        return;
      }
      String namespace = ModuleSupplier.namespaceOf(nativeJavaModule.get(label[1]));
      if (!label[0].equals(namespace)) {
        throw Starlark.errorf(
            "cannot load '%s': %s is a @%s module; load it as '@%s//%s'",
            moduleToLoad, label[1], namespace, namespace, label[1]);
      }
    }

    /**
     * Fails if {@code moduleToLoad} names a test-only native module ({@link
     * ModuleSupplier#TEST_MODULES}, e.g. {@code @stdlib//unittest}) that this evaluation was not
     * given: those exist only for test scripts, and would otherwise be reported as missing.
     */
    static void checkNotTestOnly(String moduleToLoad, String targetModule) throws EvalException {
      if (!ModuleSupplier.TEST_MODULE_NAMES.contains(targetModule)) {
        return;
      }
      String[] label = ResourceContentStarFile.splitLabel(moduleToLoad);
      if (label != null && label[0].equals("stdlib")) {
        throw Starlark.errorf(
            "cannot load '%s': %s is a test-only module, available only to test scripts",
            moduleToLoad, targetModule);
      }
    }

    // Only a Module is loadable from the environment. Any other entry of the same name is a predeclared value
    // (e.g. a host's binding named like the module), which a load() must not take for the module: the module
    // then comes from the module set as usual.
    private boolean inEvaluatorEnvironment(String moduleToLoad) {
      return evaluator.getEnvironment().get(moduleToLoad) instanceof Module;
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
      synchronized (NATIVE_WRAPPERS) {
        Module previous = NATIVE_WRAPPERS.get(key);
        if (previous != null) {
          return previous;
        }
        ModuleCache.putStable(wrapper);
        NATIVE_WRAPPERS.put(key, wrapper);
        return wrapper;
      }
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
        try (CurrentEvaluation ignored = CurrentEvaluation.enter(thread)) {
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

  /**
   * Loads the modules named by {@code loads}, the load() labels of {@code content}, as a script's
   * loads are; relative labels resolve against {@code content}. Also used by the runner's REPL,
   * which compiles each input itself.
   */
  public Map<String, Module> processLoads(StarFile content, List<String> loads) throws EvalException {
    Map<String, Module> loadedModules = new HashMap<>();
    LarkyLoader larkyLoader = new LarkyLoader(content, this);
    for (String load : loads) {
      //Module loadedModule = eval(content.resolve(load + LarkyScript.STAR_EXTENSION));
      larkyLoader.checkNamespace(load);
      Module loadedModule;
      try {
        loadedModule = larkyLoader.load(load);
      } catch (RuntimeException e) {
        // A file the script loads from next to itself (not one of Larky's modules) is the
        // script author's code: report its errors (e.g. a syntax error, with its own line
        // numbers) as they are, not wrapped in an internal load failure.
        if (!ResourceContentStarFile.startsWithPrefix(load) && e.getCause() instanceof EvalException cause) {
          throw cause;
        }
        throw e;
      }
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
      for (SyntaxError error : ex.errors()) {
        reporter.error(error.toString());
      }
      throw compileError(input.getFile(), ex);
    }
    return prog;
  }

  /** The error for a file that does not compile: its name, then each error with its location. */
  public static EvalException compileError(String file, SyntaxError.Exception ex) {
    List<String> errs = new ArrayList<>();
    for (SyntaxError error : ex.errors()) {
      errs.add(error.toString());
    }
    return new EvalException(
        String.format(
            "Error compiling Starlark program: %1$s%n" +
            "%2$s",
            file,
            String.join("\n", errs)));
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
    builtinBindings =
        MERGED_BUILTIN_BINDINGS.computeIfAbsent(
            com.google.common.collect.ImmutableList.copyOf(globalModules),
            LarkyEvaluator::mergedBuiltinBindings);
    sharedBuiltins =
        ImmutableMap.<String, Object>builder()
            .putAll(builtinBindings)
            .putAll(com.verygood.security.larky.ModuleSupplier.CORE_ENVIRONMENT)
            .buildKeepingLast();
    return ImmutableMap.<String, Object>builder()
        .putAll(builtinBindings)
        .putAll(globals)
        .buildKeepingLast();
  }

}
