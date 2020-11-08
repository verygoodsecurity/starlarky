package com.verygood.security.larky.parser;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * An utility class for traversing and evaluating the config file dependency graph.
 */
final class LarkyEvaluator {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final LinkedHashSet<String> pending = new LinkedHashSet<>();
  private final Map<String, Module> loaded = new HashMap<>();
  private final Console console;
  // Predeclared environment shared by all files (modules) loaded.
  private final ImmutableMap<String, Object> environment;
  private final ModuleSupplier.ModuleSet moduleSet;
  private final LarkyScript.StarlarkMode validationMode;

  LarkyEvaluator(LarkyScript larkyScript, ModuleSupplier.ModuleSet moduleSet, Console console) {
    this.console = checkNotNull(console);
    this.moduleSet = checkNotNull(moduleSet);
    this.validationMode = larkyScript.getValidation();
    //todo(mahmoudimus): convert to builder pattern
    this.environment = createEnvironment(larkyScript.getGlobalModules());
  }

  private void starlarkPrint(StarlarkThread thread, String msg) {
    console.verbose(thread.getCallerLocation() + ": " + msg);
  }

  public Module eval(StarFile content)
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
     * load("./testlib/builtinz", "setz") # works
     * load("testlib/builtinz", "setz", "collections")
     * load("/testlib/builtinz", "setz")  # does not work
     */
    private static final String STDLIB = "@stdlib";
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
        if(moduleToLoad.startsWith(STDLIB)) {
          String targetModule = moduleToLoad.replace(STDLIB + "/", "");
          if (inEnvironment(targetModule)) {
            loadedModule = fromEnvironment(targetModule);
          } else {
            loadedModule = fromStdlib(targetModule);
          }
        }
        else {
            loadedModule = evaluator.eval(content.resolve(moduleToLoad + LarkyScript.STAR_EXTENSION));
        }
      } catch (IOException|InterruptedException e) {
        throw new RuntimeException(e);
      }
      return loadedModule;
    }

    private boolean inEnvironment(String moduleToLoad) {
      return evaluator.environment.containsKey(moduleToLoad);
    }

    private Module fromEnvironment(String moduleToLoad) {
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

    Field removeFinalFromField(Field field) throws Exception {
       field.setAccessible(true);
       Field modifiersField = Field.class.getDeclaredField("modifiers");
       modifiersField.setAccessible(true);
       modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
       return field;
    }

    @NotNull
    private Module getNativeModule(String moduleToLoad) {
      Module newModule = Module.withPredeclared(StarlarkSemantics.DEFAULT, ImmutableMap.of());
      newModule.setClientData(moduleToLoad);
      newModule.setGlobal(moduleToLoad, nativeJavaModule.get(moduleToLoad));
      // We have to do this via reflection because we're not doing any bindings via load..
      HashSet<String> build = new HashSet<>(ImmutableSet.<String>builder()
          .addAll(newModule.getExportedGlobals().keySet())
          .add(moduleToLoad)
          .build());
      try {
        // update globals with the export, I think this could be a bug in Module for keeping the exportedGlobal private
        Field exportedGlobals = removeFinalFromField(newModule.getClass().getDeclaredField("exportedGlobals"));
        exportedGlobals.set(newModule, build);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return newModule;
    }

    @NotNull
    private Module getStarModule(String moduleToLoad) throws IOException, InterruptedException {
      /*
      * If the module set does not contain the module to load, then we try to load it from the
      * stdlib.
      *
      * If it is not found in our stdlib location, we expect the evaluator to throw an error.
       */
      Path stdlib_path = getStdlibPath();
      assert stdlib_path != null;
      //String target = moduleToLoad.replace(STDLIB + "/", "");
      StarFile larky = new PathBasedStarFile(
          Path.of("/"),
          stdlib_path,
          STDLIB);
      larky = larky.resolve(withExtension(moduleToLoad));
      return evaluator.eval(larky);
    }

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

      return Path.of(resourceAsURI);
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
  private Program compileStarlarkProgram(Module module, ParserInput input, FileOptions options) {
    Program prog;
    try {
      prog = Program.compileFile(StarlarkFile.parse(input, options), module);
    } catch (SyntaxError.Exception ex) {
      for (SyntaxError error : ex.errors()) {
        console.error(error.toString());
      }
      throw new RuntimeException("Error loading config file.");
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
  private ImmutableMap<String, Object> createEnvironment(Iterable<Class<?>> globalModules) {
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
     return ImmutableMap.copyOf(env);
   }
}
