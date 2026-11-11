package com.verygood.security.larky.jsr223;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.console.StreamWriterConsole;
import com.verygood.security.larky.parser.DefaultLarkyInterpreter;
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
import net.starlark.java.syntax.LoadStatement;
import net.starlark.java.syntax.NodeVisitor;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.Resolver;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.StarlarkFile;
import net.starlark.java.syntax.Statement;
import net.starlark.java.syntax.SyntaxError;

import javax.annotation.Nullable;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;


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
  // The names the script reads and defines, worked out on the first load() (see names()).
  @Nullable private ScriptNames names;

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
    return Program.compileFile(file, lenientModule());
  }

  /** Resolves every name that is not a Starlark builtin as predeclared: a binding may supply it. */
  private static Resolver.Module lenientModule() {
    Module universe = Module.create();
    return new Resolver.Module() {
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
      final StarFile script = starFile(source, scriptNameOrDefault());
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
   * The names the script reads from its bindings: names that neither the script, its load() statements nor Starlark
   * define. Some may be Larky's own predeclared modules.
   */
  public Set<String> predeclaredNames() throws ScriptException {
    return names().predeclared();
  }

  /** {@link #load(ScriptContext, Set)} with the engine's context. */
  public LarkyLoadedScript load(Set<String> perCallNames) throws ScriptException {
    return load(engine.getContext(), perCallNames);
  }

  /**
   * Runs the script's top level once so that its functions can be called many times (see {@link LarkyLoadedScript}).
   *
   * <p>A loaded script is shared by every later call, so by design it gets nothing of the host's: the context's
   * bindings are not given to the script (only Larky's own settings in them are read: the step limit and verbosity,
   * and the context's writer). Every name the script reads that Larky does not provide must be declared in
   * {@code perCallNames} and given with each {@link LarkyLoadedScript#invoke}, where it has a value only during that
   * call. A script that reads any other name is refused. So nothing one call's host data put in can reach another.
   *
   * @param perCallNames names whose values are given with each call rather than here; a binding of the same name is
   *     ignored
   * @throws LarkyEvaluationScriptException if the top level fails
   * @throws LarkyScriptNotLoadableException if the script reads a name that is neither Larky's nor declared per call,
   *     its top level leaves a mutable value, or it defines a per-call name
   */
  public LarkyLoadedScript load(ScriptContext context, Set<String> perCallNames) throws ScriptException {
    if (cachedSource == null) {
      throw new IllegalStateException("compile the script before loading it");
    }
    final ScriptNames scriptNames = names();
    for (String name : perCallNames) {
      if (scriptNames.defined().contains(name)) {
        throw new LarkyScriptNotLoadableException(
            "the script defines " + name + ", which is declared per call");
      }
    }
    final LarkyLoadedScript.PerCallValues perCall = new LarkyLoadedScript.PerCallValues(perCallNames);
    final Map<String, Object> bindings = new java.util.HashMap<>();
    final Bindings globalBindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
    final Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
    if (globalBindings != null) {
      bindings.putAll(globalBindings);
    }
    if (engineBindings != null) {
      bindings.putAll(engineBindings);
    }
    // of the host's bindings only Larky's own settings; the script gets nothing else of the host's
    final Bindings settings = new SimpleBindings();
    for (String setting : List.of(LarkyEvaluator.STEP_LIMIT, DefaultLarkyInterpreter.VERBOSE)) {
      if (bindings.get(setting) != null) {
        settings.put(setting, bindings.get(setting));
      }
    }
    final Bindings standIns = new SimpleBindings(new java.util.HashMap<>(perCall.standIns()));
    final ParsedStarFile result;
    final java.io.Writer writer = writer(context);
    try {
      final DefaultLarkyInterpreter interpreter = new DefaultLarkyInterpreter(LARKY_MODE, settings, standIns);
      result = interpreter.evaluate(starFile(cachedSource, scriptNameOrDefault()), writer);
    } catch (IOException | StarlarkEvalWrapper.Exc.RuntimeEvalException | Starlark.UncheckedEvalException
             | EvalException e) {
      final Optional<String> fromHost = undefinedName(e).filter(scriptNames.predeclared()::contains);
      if (fromHost.isPresent()) {
        // Larky's resolver found a name only the host could supply, and the host gives a loaded script nothing
        throw new LarkyScriptNotLoadableException("the script reads " + fromHost.get()
            + ", which a loaded script is shared by every call and so can only be given per call");
      }
      throw LarkyEvaluationScriptException.of(e);
    }
    final Module module = result.getModule();
    for (String name : scriptNames.defined()) {
      final Object value = module.getGlobal(name);
      if (value != null && !isImmutable(value)) {
        throw new LarkyScriptNotLoadableException(
            "the script's top level leaves " + name + " mutable (" + Starlark.type(value) + ")");
      }
    }
    final boolean verbose = Boolean.parseBoolean(
        String.valueOf(bindings.getOrDefault(DefaultLarkyInterpreter.VERBOSE, "false")));
    // the load's expiration is a date for the load; calls give their own (see LarkyLoadedScript)
    final LarkyCalls.Limits limits = new LarkyCalls.Limits(LarkyCalls.Limits.of(bindings).stepLimit(), null);
    return new LarkyLoadedScript(module, perCall, new StreamWriterConsole(writer, verbose), limits);
  }

  private static final java.util.regex.Pattern UNDEFINED_NAME =
      java.util.regex.Pattern.compile("name '([A-Za-z_][A-Za-z0-9_]*)' is not defined");

  /** The name of Starlark's "name 'x' is not defined" resolution error, from {@code failure} or its causes. */
  private static Optional<String> undefinedName(Throwable failure) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      final java.util.regex.Matcher matcher =
          UNDEFINED_NAME.matcher(String.valueOf(cause.getMessage()));
      if (matcher.find()) {
        return Optional.of(matcher.group(1));
      }
      if (cause.getCause() == cause) {
        break;
      }
    }
    return Optional.empty();
  }

  private static boolean isImmutable(Object value) {
    try {
      return Starlark.isImmutable(value);
    } catch (RuntimeException e) {
      // not a Starlark value: nothing says a call cannot change it
      return false;
    }
  }

  static java.io.Writer writer(ScriptContext context) {
    final java.io.Writer writer = context.getWriter();
    return writer != null ? writer : new java.io.PrintWriter(System.out, true);
  }

  private String scriptNameOrDefault() {
    return cachedScriptName != null ? cachedScriptName : DEFAULT_SCRIPT_NAME;
  }

  private StarFile starFile(String source, String scriptName) {
    if (modules.isEmpty() && cacheNamespace.isEmpty()) {
      return InMemMapBackedStarFile.createStarFile(scriptName, source);
    }
    ImmutableMap.Builder<String, byte[]> files = ImmutableMap.builder();
    files.put(scriptName, source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    modules.forEach(
        (name, text) -> files.put(name, text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    return new InMemMapBackedStarFile(files.buildOrThrow(), scriptName, cacheNamespace);
  }

  /**
   * The names a script reads from its bindings ({@code predeclared}), and the module-level names it defines itself
   * ({@code defined}): not the names its load() statements bind, nor the builtins Larky adds to every module.
   */
  record ScriptNames(ImmutableSet<String> predeclared, ImmutableSet<String> defined) {
  }

  private synchronized ScriptNames names() throws ScriptException {
    if (names == null) {
      try {
        names = scriptNames(cachedSource, scriptNameOrDefault());
      } catch (SyntaxError.Exception e) {
        throw LarkyEvaluationScriptException.of(LarkyEvaluator.compileError(scriptNameOrDefault(), e));
      }
    }
    return names;
  }

  @VisibleForTesting
  static ScriptNames scriptNames(String source, String scriptName) throws SyntaxError.Exception {
    final StarlarkFile file = StarlarkFile.parse(ParserInput.fromString(source, scriptName),
        LarkyScript.scriptFileOptions(LARKY_MODE, LarkySemantics.LARKY_SEMANTICS));
    // resolves every identifier of the file in place
    Program.compileFile(file, lenientModule());
    final Set<String> loaded = new HashSet<>();
    for (Statement statement : file.getStatements()) {
      if (statement instanceof LoadStatement load) {
        load.getBindings().forEach(binding -> loaded.add(binding.getLocalName().getName()));
      }
    }
    final ImmutableSet.Builder<String> predeclared = ImmutableSet.builder();
    final ImmutableSet.Builder<String> defined = ImmutableSet.builder();
    new NodeVisitor() {
      @Override
      public void visit(Identifier identifier) {
        final Resolver.Binding binding = identifier.getBinding();
        if (binding == null) {
          return;
        }
        if (binding.getScope() == Resolver.Scope.PREDECLARED) {
          predeclared.add(identifier.getName());
        } else if (binding.getScope() == Resolver.Scope.GLOBAL && !loaded.contains(identifier.getName())) {
          defined.add(identifier.getName());
        }
      }
    }.visit(file);
    return new ScriptNames(predeclared.build(), defined.build());
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
