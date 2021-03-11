package com.verygood.security.larky.parser;

import static com.verygood.security.larky.ModuleSupplier.ModuleSet;
import static com.verygood.security.larky.parser.LarkyScript.StarlarkMode;

import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.console.StreamWriterConsole;

import net.starlark.java.eval.EvalException;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.script.Bindings;

public class DefaultLarkyInterpreter {

  private final ModuleSet moduleSet;
  private final LarkyScript interpreter;

  public DefaultLarkyInterpreter(StarlarkMode mode, Bindings... bindings) {
    this.moduleSet = new ModuleSupplier().create();
    this.interpreter = new LarkyScript(
        ModuleSupplier.CORE_MODULES,
        mode,
        mergeGlobalBindings(bindings));
  }

  public ParsedStarFile evaluate(StarFile script, Writer writer) throws IOException, EvalException {
    return evaluate(script, new StreamWriterConsole(writer));
  }

  public ParsedStarFile evaluate(StarFile script, Console console) throws IOException, EvalException {
    return interpreter.evaluate(
        script, moduleSet, console);
  }

  private Map<String, Object> mergeGlobalBindings(Bindings... bindings) {
    return mergeBindings(bindings)
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> StarlarkUtil.valueToStarlark(entry.getValue()), (a, b) -> b));
  }

  private Map<String, Object> mergeBindings(Bindings... bindingsToMerge) {
    Map<String, Object> variables = new HashMap<>();

    for (Bindings bindings : bindingsToMerge) {
      if (bindings != null) {
        for (Map.Entry<String, Object> globalEntry : bindings.entrySet()) {
          variables.put(globalEntry.getKey(), globalEntry.getValue());
        }
      }
    }

    return variables;
  }
}
