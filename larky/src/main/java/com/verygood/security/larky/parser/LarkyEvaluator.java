package com.verygood.security.larky.parser;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.flogger.FluentLogger;
import com.google.common.io.Files;

import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.annot.Library;
import com.verygood.security.larky.console.Console;

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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An utility class for traversing and evaluating the config file dependency graph.
 */
public final class LarkyEvaluator {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final LinkedHashSet<String> pending = new LinkedHashSet<>();
  private final Map<String, Module> loaded = new HashMap<>();
  private final Console console;
  // Predeclared environment shared by all files (modules) loaded.
  private final ImmutableMap<String, Object> environment;
  private final ModuleSupplier.ModuleSet moduleSet;
  private final LarkyScript.StarlarkMode validationMode;

  public LarkyEvaluator(LarkyScript larkyScript, Console console) {
    this(larkyScript, larkyScript.getModuleSet(), console);
  }

  LarkyEvaluator(LarkyScript larkyScript, ModuleSupplier.ModuleSet moduleSet, Console console) {
    this.console = checkNotNull(console);
    this.moduleSet = checkNotNull(moduleSet);
    this.validationMode = larkyScript.getValidation();
    //todo(mahmoudimus): convert to builder pattern
    this.environment = createEnvironment(larkyScript.getBuiltinModules(), larkyScript.getGlobals());
  }

  private void starlarkPrint(StarlarkThread thread, String msg) {
    if (console.isVerbose()) {
      console.verbose(thread.getCallerLocation() + ": " + msg);
    } else {
      console.info(msg);
    }
  }

  public Module eval(StarFile content)
      throws IOException, InterruptedException, EvalException {
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
    FileOptions options = getStarlarkValidationOptions();
    ParserInput input = ParserInput.fromUTF8(content.readContentBytes(), content.path());
    Program prog = compileStarlarkProgram(module, input, options);
    Map<String, Module> loadedModules = processLoads(content, prog);

    // execute
    try (Mutability mu = Mutability.create("LarkyModules")) {
      StarlarkThread thread = new StarlarkThread(mu, semantics);
      thread.setLoader(loadedModules::get);
      thread.setPrintHandler(this::starlarkPrint);
      Starlark.execFileProgram(prog, module, thread);
    } catch (EvalException ex) {
      throw new RuntimeException("\n" + ex.getMessageWithStack());
    }
    pending.remove(content.path());
    loaded.put(content.path(), module);
    return module;
  }

  public Object evalWithOutput(StarFile content)
      throws IOException, InterruptedException, EvalException {

    // Make the modules available as predeclared bindings.
    StarlarkSemantics semantics = StarlarkSemantics.DEFAULT;
    Module module = Module.withPredeclared(semantics, environment);

    // parse & compile
    FileOptions options = getStarlarkValidationOptions();
    ParserInput input = ParserInput.fromUTF8(content.readContentBytes(), content.path());
    Program prog = compileStarlarkProgram(module, input, options);
    Map<String, Module> loadedModules = processLoads(content, prog);

    Object starlarkOutput;

    // execute
    try (Mutability mu = Mutability.create("LarkyModules")) {
      StarlarkThread thread = new StarlarkThread(mu, semantics);
      thread.setLoader(loadedModules::get);
      thread.setPrintHandler(this::starlarkPrint);
      starlarkOutput = Starlark.execFileProgram(prog, module, thread);
    } catch (EvalException ex) {
      throw new RuntimeException("\n" + ex.getMessageWithStack());
    }
    pending.remove(content.path());
    loaded.put(content.path(), module);
    return starlarkOutput;
  }

  public ModuleSupplier.ModuleSet getModuleSet() {
    return moduleSet;
  }

  class LarkyLoader implements StarlarkThread.Loader {
    /*
       Right now, it just has them all as built-ins, with namespaces
       __builtin__.struct() // struct()
       unittest // exists in the global namespace by default
       import unittest // unittest
       load('unitest', 'unitest') => it now is usable in global namespace, otherwise, unknown symbol is thrown
     */
    /**
     * load("//testlib/builtinz", "setz") # works, but root is not defined.
     * load("./testlib/builtinz", "setz") # works load("testlib/builtinz", "setz", "collections")
     * load("/testlib/builtinz", "setz")  # does not work
     */
    public static final String STDLIB = "@stdlib";
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
        if (moduleToLoad.startsWith(STDLIB)) {
          String targetModule = moduleToLoad.replace(STDLIB + "/", "");
          if (inEvaluatorEnvironment(targetModule)) {
            loadedModule = fromEvaluatorEnvironment(targetModule);
          } else {
            loadedModule = fromStdlib(targetModule);
          }
        } else {
          loadedModule = evaluator.eval(content.resolve(moduleToLoad + LarkyScript.STAR_EXTENSION));
        }
      } catch (IOException | InterruptedException | EvalException e) {
        throw new RuntimeException(e);
      }
      return loadedModule;
    }

    private boolean inEvaluatorEnvironment(String moduleToLoad) {
      return evaluator.environment.containsKey(moduleToLoad);
    }

    private Module fromEvaluatorEnvironment(String moduleToLoad) {
      return (Module) evaluator.environment.get(moduleToLoad);
    }

    private Module fromStdlib(String moduleToLoad) throws IOException, InterruptedException {
      /*
       * Check if the module is in the module set. If it is, return a module with an environment
       * of the module that was passed in via the module set.
       */
      if (nativeJavaModule.containsKey(moduleToLoad)) {
        return getNativeModule(moduleToLoad);
      }
      return getStarModule(moduleToLoad);
    }

    @NotNull
    private Module getNativeModule(String moduleToLoad) {
      Module newModule = Module.withPredeclared(
          StarlarkSemantics.DEFAULT,
          ImmutableMap.of("_" + moduleToLoad, nativeJavaModule.get(moduleToLoad)));
      newModule.setClientData(moduleToLoad);

      // We have to do this because Starlark Builtins are not actual modules, so as a result, they
      // do not export themselves to the modules.
      //
      // To circumvent around this limitation, we create an in-memory module and just evaluate it
      // to export the methods.
      // TODO(mahmoudimus): Move this to ModuleSupplier?
      try (Mutability mu = Mutability.create("InMemoryNativeModule")) {
        StarlarkThread thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
        Starlark.execFile(
            ParserInput.fromString(String.format("%1$s = _%1$s", moduleToLoad), "<builtin>"),
            this.evaluator.getStarlarkValidationOptions(),
            newModule,
            thread
        );
      } catch (InterruptedException | EvalException | SyntaxError.Exception e) {
        throw new RuntimeException(e);
      }
      return newModule;
    }

    @SneakyThrows
    @NotNull
    private Module getStarModule(String moduleToLoad) throws IOException, InterruptedException {
      return evaluator.eval(ResourceContentStarFile.buildStarFile(moduleToLoad));
    }

    @SneakyThrows
    @Nullable
    private Path getStdlibPath() {
      URL resourceUrl = this.getClass().getClassLoader()
          .getResource(STDLIB.replace("@", ""));
      assert resourceUrl != null;
      URI resourceAsURI;
      try {
        resourceAsURI = resourceUrl.toURI();
      } catch (URISyntaxException e) {
        return null;
      }

      return Paths.get(resourceAsURI);
    }

    @SuppressWarnings("UnstableApiUsage")
    private String withExtension(String moduleToLoad) {
      String nameWithoutExtension = Files.getNameWithoutExtension(moduleToLoad);
      String fname = Files.simplifyPath(nameWithoutExtension + LarkyScript.STAR_EXTENSION);
      return StarFile.ABSOLUTE_PREFIX + moduleToLoad.replace(nameWithoutExtension, fname);
    }

  }

  @NotNull
  private Map<String, Module> processLoads(StarFile content, Program prog) {
    Map<String, Module> loadedModules = new HashMap<>();
    LarkyLoader larkyLoader = new LarkyLoader(content, this);
    for (String load : prog.getLoads()) {
      //Module loadedModule = eval(content.resolve(load + LarkyScript.STAR_EXTENSION));
      Module loadedModule = larkyLoader.load(load);
      loadedModules.put(load, loadedModule);
    }
    return loadedModules;
  }

  @NotNull
  private Program compileStarlarkProgram(Module module, ParserInput input, FileOptions options) throws EvalException {
    Program prog;
    try {
      prog = Program.compileFile(StarlarkFile.parse(input, options), module);
    } catch (SyntaxError.Exception ex) {
      List<String> errs = new ArrayList<>();
      for (SyntaxError error : ex.errors()) {
        console.error(error.toString());
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

  private FileOptions getStarlarkValidationOptions() {
    FileOptions options;
    if (validationMode == LarkyScript.StarlarkMode.STRICT) {
      options = LarkyScript.STARLARK_STRICT_FILE_OPTIONS;
    } else if (validationMode == LarkyScript.StarlarkMode.LOOSE) {
      options = LarkyScript.STARLARK_LOOSE_FILE_OPTIONS;
    } else {
      throw new RuntimeException("Undefined StarlarkMode: " + validationMode);
    }
    return options;
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

  /**
   * Create the environment for all evaluations (will be shared between all the dependent files
   * loaded).
   */
  private ImmutableMap<String, Object> createEnvironment(Iterable<Class<?>> globalModules, Map<String, Object> globals) {
    Map<String, Object> env = Maps.newHashMap();

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
    env.putAll(globals);
    return ImmutableMap.copyOf(env);
  }

}
