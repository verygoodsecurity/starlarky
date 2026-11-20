package com.verygood.security.larky.parser;

import static com.verygood.security.larky.ModuleSupplier.ModuleSet;
import static com.verygood.security.larky.parser.LarkyScript.StarlarkMode;

import com.google.common.collect.Maps;
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

import net.starlark.java.eval.DiagnosticRedactor;
import net.starlark.java.eval.EvalException;

import javax.annotation.Nullable;
import javax.script.Bindings;
import javax.script.SimpleBindings;

public class DefaultLarkyInterpreter {

  public static final String VERBOSE = "__VERBOSE__";
  private final ModuleSet moduleSet;
  private final LarkyScript interpreter;

  /** Creates an interpreter for JSR223 scopes. Only engine bindings may supply the redactor. */
  public static DefaultLarkyInterpreter forScopes(
      StarlarkMode mode, @Nullable Bindings globalBindings, @Nullable Bindings engineBindings) {
    Bindings scriptGlobals = globalBindings;
    if (globalBindings != null && globalBindings.containsKey(DiagnosticRedactor.BINDING_NAME)) {
      scriptGlobals = new SimpleBindings(Maps.filterKeys(
          globalBindings, key -> !DiagnosticRedactor.BINDING_NAME.equals(key)));
    }
    return new DefaultLarkyInterpreter(mode, scriptGlobals, engineBindings);
  }

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
            entry -> DiagnosticRedactor.BINDING_NAME.equals(entry.getKey())
                ? entry.getValue()
                : StarlarkUtil.valueToStarlark(entry.getValue()),
            (a, b) -> b)
        )
    );
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
