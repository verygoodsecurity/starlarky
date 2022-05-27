package com.verygood.security.larky.parser;

import static com.verygood.security.larky.ModuleSupplier.ModuleSet;
import static com.verygood.security.larky.parser.LarkyScript.StarlarkMode;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.console.StreamWriterConsole;

import net.starlark.java.eval.EvalException;

import javax.script.Bindings;

public class DefaultLarkyInterpreter {

  private final ModuleSet moduleSet;
  private final LarkyScript interpreter;

  public DefaultLarkyInterpreter(StarlarkMode mode, Bindings... bindings) {
    this.moduleSet = new ModuleSupplier().create();
    this.interpreter = new LarkyScript(
        ModuleSupplier.CORE_MODULES,
        mode,
        // creates a stream of Binding::entrySet::stream
        Arrays
          .<Map<String, Object>>stream(bindings)
          .filter(Objects::nonNull)
          .map(Map::entrySet)
          .flatMap(Collection::stream)
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> StarlarkUtil.valueToStarlark(entry.getValue()), (a, b) -> b)
        )
    );
  }

  public ParsedStarFile evaluate(StarFile script, Writer writer) throws IOException, EvalException {
    return evaluate(script, new StreamWriterConsole(writer));
  }

  public ParsedStarFile evaluate(StarFile script, Console console) throws IOException, EvalException {
    return interpreter.evaluate(
        script, moduleSet, console);
  }
}
