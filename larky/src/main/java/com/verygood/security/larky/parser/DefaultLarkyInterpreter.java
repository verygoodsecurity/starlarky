package com.verygood.security.larky.parser;

import static com.verygood.security.larky.ModuleSupplier.ModuleSet;
import static com.verygood.security.larky.parser.LarkyScript.StarlarkMode;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.console.StreamWriterConsole;

import net.starlark.java.eval.EvalException;

import javax.script.Bindings;

public class DefaultLarkyInterpreter {

  public static final String VERBOSE = "__VERBOSE__";
  private final ModuleSet moduleSet;
  private final LarkyScript interpreter;

  /**
   * @throws EvalException if converting a binding runs Starlark code that fails: a binding may be
   *     a value of an earlier evaluation (a JSR-223 engine keeps the globals of each eval), and
   *     copying a dict hashes its keys, which runs a struct's {@code __hash__}/{@code __eq__}
   */
  public DefaultLarkyInterpreter(StarlarkMode mode, Bindings... bindings) throws EvalException {
    this.moduleSet = new ModuleSupplier().create();
    // later bindings win, as before
    Map<String, Object> merged = new LinkedHashMap<>();
    Arrays.stream(bindings).filter(Objects::nonNull).forEach(merged::putAll);
    this.interpreter = new LarkyScript(ModuleSupplier.CORE_MODULES, mode, toStarlark(merged));
  }

  // Converts on a HostValues thread, bounded by the step limit the bindings ask for, if any.
  private static Map<String, Object> toStarlark(Map<String, Object> merged) throws EvalException {
    Object stepLimit = merged.get(LarkyEvaluator.STEP_LIMIT);
    HostValues host =
        HostValues.of(
            LarkySemantics.LARKY_SEMANTICS,
            stepLimit == null ? null : Long.parseLong(stepLimit.toString()),
            null);
    Map<String, Object> converted = new HashMap<>();
    try {
      for (Map.Entry<String, Object> e : merged.entrySet()) {
        converted.put(e.getKey(), host.toStarlark(e.getValue()));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EvalException("interrupted while converting bindings", e);
    }
    return converted;
  }

  public ParsedStarFile evaluate(StarFile script, Writer writer) throws IOException, EvalException {
    String verbose = interpreter.getGlobals().getOrDefault(VERBOSE, "false").toString();
    return evaluate(script, new StreamWriterConsole(writer, Boolean.parseBoolean(verbose)));
  }

  public ParsedStarFile evaluate(StarFile script, Console console) throws IOException, EvalException {
    return interpreter.evaluate(
        script, moduleSet, console);
  }
}
