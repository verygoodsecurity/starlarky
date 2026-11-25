package com.verygood.security.larky.runtime;

import static com.verygood.security.larky.ModuleSupplier.ModuleSet;
import static com.verygood.security.larky.parser.LarkyScript.StarlarkMode;

import com.verygood.security.larky.parser.Computation;
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

  /** As {@link #DefaultLarkyInterpreter(StarlarkMode, EvaluationSettings, Bindings...)}, with no settings. */
  public DefaultLarkyInterpreter(StarlarkMode mode, Bindings... bindings) throws EvalException {
    this(mode, EvaluationSettings.NONE, bindings);
  }

  /**
   * @param settings the host's settings for each evaluation (never a binding)
   * @throws IllegalArgumentException if a binding uses a retired setting name (see {@link
   *     LarkyEvaluator#refuseRetiredSettings})
   * @throws EvalException if converting a binding runs Starlark code that fails: a binding may be
   *     a value of an earlier evaluation (a JSR-223 engine keeps the globals of each eval), and
   *     copying a dict hashes its keys, which runs a struct's {@code __hash__}/{@code __eq__}
   */
  public DefaultLarkyInterpreter(StarlarkMode mode, EvaluationSettings settings, Bindings... bindings)
      throws EvalException {
    this.moduleSet = new ModuleSupplier().create();
    // later bindings win, as before
    Map<String, Object> merged = new LinkedHashMap<>();
    Arrays.stream(bindings).filter(Objects::nonNull).forEach(merged::putAll);
    LarkyEvaluator.refuseRetiredSettings(merged.keySet());
    this.larkyScript =
        new LarkyScript(
            ModuleSupplier.CORE_MODULES,
            mode,
            toStarlark(merged, settings),
            moduleSet,
            LarkySemantics.LARKY_SEMANTICS,
            settings);
  }

  // Converts on a fresh Starlark thread using the evaluation's step limit and deadline.
  private static Map<String, Object> toStarlark(Map<String, Object> merged, EvaluationSettings settings)
      throws EvalException {
    StarlarkInterop interop =
        StarlarkInterop.of(
            LarkySemantics.LARKY_SEMANTICS,
            settings.stepLimit().isPresent() ? settings.stepLimit().getAsLong() : null,
            settings.expirationMs().orElse(Long.MAX_VALUE),
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

  /** As {@link #evaluate(StarFile, Writer)}; see {@link LarkyScript#evaluate(StarFile, ModuleSet, Console, java.util.function.Consumer)}. */
  public ParsedStarFile evaluate(
      StarFile script, Writer writer, java.util.function.Consumer<EvaluationMetadata> metadata)
      throws IOException, EvalException {
    return evaluate(script, new StreamWriterConsole(writer), metadata);
  }

  public ParsedStarFile evaluate(
      StarFile script, Console console, java.util.function.Consumer<EvaluationMetadata> metadata)
      throws IOException, EvalException {
    return larkyScript.evaluate(script, moduleSet, console, metadata);
  }

  /** Executes a host computation with the entry module still mutable. */
  public <I, O> O compute(
      StarFile script, Writer writer, java.util.List<String> imports, I input,
      Computation<I, O> computation, java.util.function.Consumer<EvaluationMetadata> metadata)
      throws IOException, EvalException, InterruptedException {
    return compute(script, new StreamWriterConsole(writer), imports, input, computation, metadata);
  }

  public <I, O> O compute(
      StarFile script, Console console, java.util.List<String> imports, I input,
      Computation<I, O> computation, java.util.function.Consumer<EvaluationMetadata> metadata)
      throws IOException, EvalException, InterruptedException {
    var evaluator = new LarkyEvaluator(larkyScript, moduleSet, console);
    try {
      return evaluator.compute(script, imports, input, computation);
    } finally {
      if (evaluator.lastMetadata() != null) {
        metadata.accept(evaluator.lastMetadata());
      }
    }
  }

  public ParsedStarFile evaluate(StarFile script, Console console) throws IOException, EvalException {
    return larkyScript.evaluate(script, moduleSet, console);
  }
}
