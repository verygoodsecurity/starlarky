package com.verygood.security.larky.runtime;

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
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.parser.StarFile;

import net.starlark.java.eval.EvalException;

import javax.script.Bindings;

public class DefaultLarkyInterpreter {

  private final ModuleSet moduleSet;
  private final LarkyScript larkyScript;

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
    this.larkyScript = new LarkyScript(ModuleSupplier.CORE_MODULES, mode, toStarlark(merged));
  }

  // Converts on a fresh Starlark thread using the bindings' step limit, if given.
  private static Map<String, Object> toStarlark(Map<String, Object> merged) throws EvalException {
    Object stepLimit = merged.get(LarkyEvaluator.STEP_LIMIT);
    StarlarkInterop interop =
        StarlarkInterop.of(
            LarkySemantics.LARKY_SEMANTICS,
            stepLimit == null ? null : Long.parseLong(stepLimit.toString()),
            null);
    Map<String, Object> converted = new HashMap<>();
    try {
      for (Map.Entry<String, Object> e : merged.entrySet()) {
        converted.put(e.getKey(), interop.toStarlark(e.getValue()));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EvalException("interrupted while converting bindings", e);
    }
    return converted;
  }

  public ParsedStarFile evaluate(StarFile script, Writer writer) throws IOException, EvalException {
    return evaluate(script, new StreamWriterConsole(writer));
  }

  public ParsedStarFile evaluate(StarFile script, Console console) throws IOException, EvalException {
    return larkyScript.evaluate(script, moduleSet, console);
  }
}
