package com.verygood.security.larky.jsr223;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.parser.DefaultLarkyInterpreter;
import com.verygood.security.larky.parser.EvaluationMetadata;
import com.verygood.security.larky.parser.EvaluationSettings;
import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyEvaluator;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.parser.StarFile;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.syntax.TypeContext;
import net.starlark.java.syntax.TypeConstructor;
import net.starlark.java.syntax.StarlarkType;
import net.starlark.java.syntax.Identifier;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.Resolver;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.StarlarkFile;
import net.starlark.java.syntax.SyntaxError;

import javax.annotation.Nullable;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;


/**
 * A compiled Starlark script that can be executed multiple times efficiently.
 *
 * <p>{@link #compile} parses and resolves the script once, so syntax errors are reported at
 * compile time, and keeps the source so {@link #eval(ScriptContext)} does not read the context's
 * reader. Compiled programs are cached process-wide by (source, name).
 */
public class LarkyCompiledScript extends CompiledScript {

  private static final LarkyScript.StarlarkMode LARKY_MODE = LarkyScript.StarlarkMode.STRICT;
  private static final String DEFAULT_SCRIPT_NAME = "larky.star";

  private final LarkyScriptEngine engine;

  // Cached compilation results
  @Nullable private String cachedSource;
  @Nullable private String cachedScriptName;
  @Nullable private Program cachedProgram;
  // Other files the script may load() by name (see LarkyScriptEngine#MODULES).
  private ImmutableMap<String, String> modules = ImmutableMap.of();
  // Separates this script's cached compiled programs from other namespaces' (see
  // LarkyScriptEngine#CACHE_NAMESPACE).
  private String cacheNamespace = "";

  /**
   * Construct a {@link LarkyCompiledScript}.
   *
   * @param engine the {@link LarkyScriptEngine} that compiled this script
   */
  LarkyCompiledScript(LarkyScriptEngine engine) {
    this.engine = engine;
  }

  @Override
  public ScriptEngine getEngine() {
    return engine;
  }

  /**
   * Pre-compiles the script from the given source.
   *
   * <p>This method parses and resolves the script, caching the result for efficient repeated
   * execution via {@link #eval(ScriptContext)}.
   *
   * @param source the script source code
   * @param scriptName the script name for error reporting
   * @throws LarkyEvaluationScriptException if compilation fails
   */
  // Scripts compiled by compile(), by (source, name): a service compiling the same script for
  // every request parses and resolves it once. Programs are immutable, so they can be shared.
  private static final com.google.common.cache.Cache<List<String>, Program> COMPILED =
      com.google.common.cache.CacheBuilder.newBuilder()
          .maximumSize(Integer.getInteger("larky.scriptCache.size", 1000))
          .build();

  private static Program compileSource(String source, String scriptName)
      throws SyntaxError.Exception {
    // eval() runs the script with Larky's default semantics (see DefaultLarkyInterpreter).
    return compileSource(source, scriptName, LarkySemantics.LARKY_SEMANTICS);
  }

  @VisibleForTesting
  static Program compileSource(String source, String scriptName, StarlarkSemantics semantics)
      throws SyntaxError.Exception {
    // Parse the script with the file options of the Larky interpreter that eval() uses (top-level
    // rebinding, loads bound globally, type syntax if type checking is on), so compile() accepts
    // the scripts eval() runs.
    ParserInput input = ParserInput.fromString(source, scriptName);
    StarlarkFile file =
        StarlarkFile.parse(input, LarkyScript.scriptFileOptions(LARKY_MODE, semantics));

    // Resolve and compile, even if the file did not parse: compileFile then throws the parse
    // errors along with the resolver's, as the interpreter reports them. Globals supplied
    // through JSR-223 bindings are only known at eval time, so any name that is not a universal
    // builtin resolves as predeclared here. Real resolution against the bindings happens again
    // in eval().
    Module universe = Module.create();
    Resolver.Module lenient = new Resolver.Module() {
      @Override
      public Resolver.Scope resolve(String name, boolean resolveTypeSyntax)
          throws Resolver.Module.Undefined {
        try {
          return universe.resolve(name, resolveTypeSyntax);
        } catch (Resolver.Module.Undefined e) {
          // The parser's placeholder for misparsed text is not a name a binding could supply;
          // the resolver reports it as "contains syntax errors".
          if (!Identifier.isValid(name)) {
            throw e;
          }
          return Resolver.Scope.PREDECLARED;
        }
      }

      @Override
      public StarlarkType getPredeclaredSymbolType(String name) {
        return universe.getPredeclaredSymbolType(name);
      }

      @Override
      public StarlarkType getUniversalSymbolType(String name) {
        return universe.getUniversalSymbolType(name);
      }

      @Override
      public TypeConstructor getTypeConstructor(String name) throws Resolver.Module.Undefined {
        return universe.getTypeConstructor(name);
      }

      @Override
      public TypeContext getTypeContext() {
        return universe.getTypeContext();
      }
    };
    return Program.compileFile(file, lenient);
  }

  public void compile(String source, String scriptName) throws LarkyEvaluationScriptException {
    try {
      this.cachedSource = source;
      this.cachedScriptName = scriptName;

      this.cachedProgram = COMPILED.get(List.of(source, scriptName), () -> compileSource(source, scriptName));
    } catch (java.util.concurrent.ExecutionException
        | com.google.common.util.concurrent.UncheckedExecutionException e) {
      Exception cause = e.getCause() instanceof Exception c ? c : e;
      // Report a script that does not parse as eval() did before it compiled first: every
      // error, each with the script's name, line and column.
      if (cause instanceof SyntaxError.Exception syntax) {
        cause = LarkyEvaluator.compileError(scriptName, syntax);
      }
      throw LarkyEvaluationScriptException.of(cause);
    }
  }

  void setCacheNamespace(String cacheNamespace) {
    this.cacheNamespace = cacheNamespace;
  }

  /** Makes these files available to the script's load() statements, by name. */
  void setModules(Map<String, String> modules) {
    this.modules = ImmutableMap.copyOf(modules);
  }

  /**
   * Returns true if this script has been compiled and cached.
   */
  public boolean isCompiled() {
    return cachedProgram != null;
  }

  /**
   * {@inheritDoc}
   *
   * @throws LarkyGlobalScopeRefusedException if the engine refuses the context's global scope
   *     ({@link LarkyScriptEngine.GlobalScope#REFUSE})
   */
  @Override
  public Object eval(ScriptContext context) throws LarkyEvaluationScriptException {
    // the global scope as the engine uses it: as JSR-223 describes it, or none (LarkyScriptEngine.GlobalScope)
    Bindings globalBindings = engine.globalBindings(context);
    Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);

    // Evaluation goes through the Larky interpreter, so bindings, load() and print() behave as
    // they do for an uncompiled script.
    return evalInterpreted(
        context,
        globalBindings,
        engineBindings,
        context instanceof LarkyScriptContext larky ? larky : null);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Runs in a context built from the engine's, as {@link CompiledScript#eval(Bindings)} does,
   * with {@code bindings} as its engine scope. When the engine's context is a {@link
   * LarkyScriptContext}, its settings apply, and the evaluation's metadata is recorded on it.
   */
  @Override
  public Object eval(Bindings bindings) throws ScriptException {
    ScriptContext engineContext = engine.getContext();
    if (bindings == null) {
      return eval(engineContext);
    }
    SimpleScriptContext context = new SimpleScriptContext();
    context.setReader(engineContext.getReader());
    context.setWriter(engineContext.getWriter());
    context.setErrorWriter(engineContext.getErrorWriter());
    context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    context.setBindings(engineContext.getBindings(ScriptContext.GLOBAL_SCOPE), ScriptContext.GLOBAL_SCOPE);
    return evalInterpreted(
        context,
        engine.globalBindings(context),
        bindings,
        engineContext instanceof LarkyScriptContext larky ? larky : null);
  }

  /**
   * Evaluates through the Larky interpreter.
   */
  private Object evalInterpreted(
      ScriptContext context,
      Bindings globalBindings,
      Bindings engineBindings,
      @Nullable LarkyScriptContext settingsContext)
      throws LarkyEvaluationScriptException {
    // read once, when the evaluation starts
    final EvaluationSettings settings =
        settingsContext == null ? EvaluationSettings.NONE : settingsContext.getSettings();
    if (settingsContext != null) {
      settingsContext.setMetadata(null); // never the previous evaluation's
    }
    ParsedStarFile result;
    // what happened in the evaluation, for a failure whose exception carries none
    final java.util.concurrent.atomic.AtomicReference<EvaluationMetadata> metadata =
        new java.util.concurrent.atomic.AtomicReference<>();

    try {
      String source = cachedSource != null ? cachedSource : readAndClose(context.getReader());
      String scriptName = cachedScriptName != null ? cachedScriptName : DEFAULT_SCRIPT_NAME;

      final StarFile script;
      if (modules.isEmpty() && cacheNamespace.isEmpty()) {
        script = InMemMapBackedStarFile.createStarFile(scriptName, source);
      } else {
        ImmutableMap.Builder<String, byte[]> files = ImmutableMap.builder();
        files.put(scriptName, source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        modules.forEach(
            (name, text) -> files.put(name, text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        script = new InMemMapBackedStarFile(files.buildOrThrow(), scriptName, cacheNamespace);
      }
      final DefaultLarkyInterpreter larkyInterpreter =
          new DefaultLarkyInterpreter(LARKY_MODE, settings, globalBindings, engineBindings);
      result = larkyInterpreter.evaluate(script, context.getWriter(), metadata::set);
    } catch (IOException | StarlarkEvalWrapper.Exc.RuntimeEvalException | Starlark.UncheckedEvalException |
             EvalException e) {
      LarkyEvaluationScriptException failure =
          LarkyEvaluationScriptException.of(e).withMetadataIfAbsent(metadata.get());
      if (settingsContext != null) {
        settingsContext.setMetadata(failure.metadata());
      }
      throw failure;
    }
    if (settingsContext != null) {
      settingsContext.setMetadata(result.metadata() != null ? result.metadata() : metadata.get());
    }
    setBindingsValue(globalBindings, engineBindings, result.getGlobals());
    return result;
  }

  /**
   * Reads the script from the context reader. Only used when no source was compiled; the
   * default JSR-223 reader wraps System.in, so it must not be opened otherwise.
   */
  private static String readAndClose(Reader reader) throws IOException {
    try (Reader r = reader) {
      return CharStreams.toString(r);
    }
  }

  private void setBindingsValue(Bindings globalBindings, Bindings engineBindings, Map<String, Object> moduleGlobals) {
    for (Map.Entry<String, Object> entry : moduleGlobals.entrySet()) {
      String name = entry.getKey();
      Object value = entry.getValue();
      // a retired setting name is refused as a binding: copying a script's variable of that name
      // back would make the next run on these bindings fail
      if (LarkyEvaluator.isRetiredSettingName(name)) {
        continue;
      }
      if (globalBindings != null && globalBindings.containsKey(name)) {
        globalBindings.put(name, value);
      }
      // by default, if defined values are not globals, they belong in engine binding scope
      // to allow for multiple evals() of an instance
      // TODO(mahmoudimus): is this threadsafe?
      else if (engineBindings != null) {
        engineBindings.put(name, value);
      }
    }
  }
}
