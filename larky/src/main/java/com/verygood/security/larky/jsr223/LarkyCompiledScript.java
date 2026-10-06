package com.verygood.security.larky.jsr223;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.parser.DefaultLarkyInterpreter;
import com.verygood.security.larky.parser.InMemMapBackedStarFile;
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

    if (!file.ok()) {
      throw new SyntaxError.Exception(file.errors());
    }

    // Resolve. Globals supplied through JSR-223 bindings are only known at
    // eval time, so any name that is not a universal builtin resolves as predeclared here.
    // Real resolution against the bindings happens again in eval().
    Module universe = Module.create();
    Resolver.Module lenient = new Resolver.Module() {
      @Override
      public Resolver.Scope resolve(String name, boolean resolveTypeSyntax) {
        try {
          return universe.resolve(name, resolveTypeSyntax);
        } catch (Resolver.Module.Undefined e) {
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
      throw LarkyEvaluationScriptException.of(
          e.getCause() instanceof Exception cause ? cause : e);
    }
  }

  /**
   * Returns true if this script has been compiled and cached.
   */
  public boolean isCompiled() {
    return cachedProgram != null;
  }

  @Override
  public Object eval(ScriptContext context) throws LarkyEvaluationScriptException {
    Bindings globalBindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
    Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);

    // Evaluation goes through the Larky interpreter, so bindings, load() and print() behave as
    // they do for an uncompiled script.
    return evalInterpreted(context, globalBindings, engineBindings);
  }

  /**
   * Evaluates through the Larky interpreter.
   */
  private Object evalInterpreted(ScriptContext context, Bindings globalBindings, Bindings engineBindings)
      throws LarkyEvaluationScriptException {
    ParsedStarFile result;

    try {
      String source = cachedSource != null ? cachedSource : readAndClose(context.getReader());
      String scriptName = cachedScriptName != null ? cachedScriptName : DEFAULT_SCRIPT_NAME;

      final StarFile script = InMemMapBackedStarFile.createStarFile(scriptName, source);
      final DefaultLarkyInterpreter larkyInterpreter = new DefaultLarkyInterpreter(LARKY_MODE, globalBindings, engineBindings);
      result = larkyInterpreter.evaluate(script, context.getWriter());
    } catch (IOException | StarlarkEvalWrapper.Exc.RuntimeEvalException | Starlark.UncheckedEvalException |
             EvalException e) {
      throw LarkyEvaluationScriptException.of(e);
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
