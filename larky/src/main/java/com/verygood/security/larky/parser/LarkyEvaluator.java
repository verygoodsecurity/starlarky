package com.verygood.security.larky.parser;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.flogger.FluentLogger;

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

import java.io.IOException;
import java.util.HashMap;
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
  private final LarkyParser.StarlarkMode validationMode;

  LarkyEvaluator(LarkyParser larkyParser, ModuleSupplier.ModuleSet moduleSet, Console console) {
    this.console = checkNotNull(console);
    this.moduleSet = checkNotNull(moduleSet);
    this.validationMode = larkyParser.getValidation();
    //todo(mahmoudimus): convert to builder pattern
    this.environment = createEnvironment(this.moduleSet, larkyParser.getGlobalModules());
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
      console.error(ex.getMessageWithStack());
      throw new RuntimeException("Error loading config file", ex);
    }
    pending.remove(content.path());
    loaded.put(content.path(), module);
    return module;
  }

  @NotNull
  private Map<String, Module> processLoads(StarFile content, Program prog) throws IOException, InterruptedException {
    /*
       TODO: Build better import semantics:

       - load from globals first.
       - load from larkylib (native, larky)
       - load from local path (./) <- has to be like go? //external: load('//github

       Right now, it just has them all as built-ins, with namespaces
       __builtin__.struct() // struct()
       unittest // exists in the global namespace by default
       import unittest // unittest
       load('unitest', 'unitest') => it now is usable in global namespace, otherwise, unknown symbol is thrown
     */
    // process loads (local star files)
    Map<String, Module> loadedModules = new HashMap<>();
    for (String load : prog.getLoads()) {
      // isLoadInLarkyLib(), loadLibrary()
      // else, do the thing below: resolve load for built-in
      // ->
      Module loadedModule = eval(content.resolve(load + LarkyParser.STAR_EXTENSION));
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
    if (validationMode == LarkyParser.StarlarkMode.STRICT) {
      options = LarkyParser.STARLARK_STRICT_FILE_OPTIONS;
    } else if (validationMode == LarkyParser.StarlarkMode.LOOSE) {
      options = LarkyParser.STARLARK_LOOSE_FILE_OPTIONS;
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
  public ImmutableMap<String, Object> createEnvironment(ModuleSupplier.ModuleSet moduleSet, Iterable<Class<?>> globalModules) {
     Map<String, Object> env = Maps.newHashMap();
     for (Map.Entry<String, Object> module : moduleSet.getModules().entrySet()) {
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
