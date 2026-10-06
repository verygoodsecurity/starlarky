package com.verygood.security.larky.parser;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.starlark.java.eval.CompiledModule;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.compiler.BytecodeCompiler;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.Identifier;
import net.starlark.java.syntax.NodeVisitor;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.Resolver;
import net.starlark.java.syntax.StarlarkFile;

/**
 * Process-wide cache of compiled Larky modules (stdlib, vendor, vgs), so that each evaluation does
 * not re-parse and re-resolve them.
 *
 * <p>Only the compiled code is shared. Every evaluation still executes it into a fresh {@link
 * Module}, so no module values are shared between evaluations. Compiled code is not mutated by
 * execution, so one can be run by several threads at once.
 *
 * <p>When bytecode is enabled, a module is first looked for precompiled: a {@code .starc} resource
 * next to its {@code .star} ({@link CompiledModule}, written at build time by {@link
 * LarkyPrecompiler}). If there is none, or it was written by an incompatible build, the source is
 * compiled.
 *
 * <p>Resolution depends on the environment only through the names a file resolves as PREDECLARED
 * or UNIVERSAL; cached code is reused only while its PREDECLARED names are still predeclared and
 * none of its UNIVERSAL names has become predeclared. Other environment entries (such as
 * per-evaluation JSR-223 bindings) do not affect it.
 *
 * <p>{@code -Dlarky.programCache.disable=true} turns the cache off, and {@code
 * -Dlarky.precompiled.disable=true} ignores precompiled modules.
 */
final class ProgramCache {

  private ProgramCache() {}

  /** A compiled module, from source or precompiled. */
  interface Executable {
    /** The modules the file loads, in source order. */
    List<String> loads();

    Object exec(Module module, StarlarkThread thread) throws EvalException, InterruptedException;

    /** The names the file resolved as PREDECLARED (whose values its module may capture). */
    java.util.Set<String> predeclaredNames();

    static Executable of(Program program) {
      return of(program, null);
    }

    static Executable of(Program program, java.util.Set<String> predeclared) {
      return new Executable() {
        @Override
        public List<String> loads() {
          return program.getLoads();
        }

        @Override
        public Object exec(Module module, StarlarkThread thread)
            throws EvalException, InterruptedException {
          return Starlark.execFileProgram(program, module, thread);
        }

        @Override
        public java.util.Set<String> predeclaredNames() {
          return predeclared;
        }
      };
    }

    static Executable of(CompiledModule compiled) {
      return new Executable() {
        @Override
        public List<String> loads() {
          return compiled.getLoads();
        }

        @Override
        public Object exec(Module module, StarlarkThread thread)
            throws EvalException, InterruptedException {
          return compiled.exec(module, thread);
        }

        @Override
        public java.util.Set<String> predeclaredNames() {
          return compiled.getPredeclaredNames();
        }
      };
    }
  }

  /** Compiles a module's source on a cache miss. */
  interface Compiler {
    Program compile(StarlarkFile[] parsed) throws EvalException;
  }

  private record Key(
      String path, FileOptions options, StarlarkSemantics semantics, boolean bytecode) {}

  private interface Validity {
    boolean resolvesTheSameIn(Module module);
  }

  private record Entry(Executable executable, Validity validity) {}

  private static final boolean DISABLED = Boolean.getBoolean("larky.programCache.disable");
  private static final boolean PRECOMPILED_DISABLED =
      Boolean.getBoolean("larky.precompiled.disable");
  private static final ConcurrentHashMap<Key, Entry> CACHE = new ConcurrentHashMap<>();

  private static final AtomicInteger loadedPrecompiled = new AtomicInteger();
  private static final AtomicInteger compiledFromSource = new AtomicInteger();

  /**
   * Returns the compiled module for the trusted resource at {@code path}: cached, precompiled, or
   * compiled from source with {@code compiler}, whichever resolves the same way in {@code
   * module}'s environment first.
   */
  static Executable get(
      String path,
      Module module,
      FileOptions options,
      StarlarkSemantics semantics,
      Compiler compiler)
      throws EvalException {
    if (DISABLED) {
      return Executable.of(compiler.compile(new StarlarkFile[1]));
    }
    boolean bytecode = BytecodeCompiler.enabledByDefault();
    Key key = new Key(path, options, semantics, bytecode);
    Entry cached = CACHE.get(key);
    if (cached != null && cached.validity().resolvesTheSameIn(module)) {
      return cached.executable();
    }
    if (bytecode && !PRECOMPILED_DISABLED) {
      CompiledModule precompiled = precompiled(path);
      if (precompiled != null && precompiled.resolvesTheSameIn(module)) {
        Entry entry = new Entry(Executable.of(precompiled), precompiled::resolvesTheSameIn);
        CACHE.put(key, entry);
        loadedPrecompiled.incrementAndGet();
        return entry.executable();
      }
    }
    StarlarkFile[] parsed = new StarlarkFile[1];
    Program program = compiler.compile(parsed);
    compiledFromSource.incrementAndGet();
    if (parsed[0] == null) {
      return Executable.of(program);
    }
    ImmutableSet<String> predeclared = namesOf(parsed[0], Resolver.Scope.PREDECLARED);
    ImmutableSet<String> universal = namesOf(parsed[0], Resolver.Scope.UNIVERSAL);
    Executable executable = Executable.of(program, predeclared);
    CACHE.put(key, new Entry(executable, validity(predeclared, universal)));
    return executable;
  }

  // ---- scripts (not Larky's own modules): e.g. a customer script evaluated per request ----

  private record ScriptKey(
      String path, String source, FileOptions options, StarlarkSemantics semantics,
      boolean bytecode) {}

  private static final int MAX_SCRIPTS = Integer.getInteger("larky.scriptCache.size", 1000);

  private static final com.google.common.cache.Cache<ScriptKey, Entry> SCRIPTS =
      com.google.common.cache.CacheBuilder.newBuilder().maximumSize(MAX_SCRIPTS).build();

  /**
   * Returns the compiled program for a script, compiling it with {@code compiler} unless a program
   * compiled from the identical source resolves the same way in {@code module}'s environment.
   * Keyed by the full source text, so different scripts never share an entry.
   */
  static Executable getScript(
      String path,
      String source,
      Module module,
      FileOptions options,
      StarlarkSemantics semantics,
      Compiler compiler)
      throws EvalException {
    if (DISABLED || MAX_SCRIPTS <= 0) {
      return Executable.of(compiler.compile(new StarlarkFile[1]));
    }
    ScriptKey key =
        new ScriptKey(path, source, options, semantics, BytecodeCompiler.enabledByDefault());
    Entry cached = SCRIPTS.getIfPresent(key);
    if (cached != null && cached.validity().resolvesTheSameIn(module)) {
      return cached.executable();
    }
    StarlarkFile[] parsed = new StarlarkFile[1];
    Program program = compiler.compile(parsed);
    if (parsed[0] == null) {
      return Executable.of(program);
    }
    ImmutableSet<String> predeclared = namesOf(parsed[0], Resolver.Scope.PREDECLARED);
    ImmutableSet<String> universal = namesOf(parsed[0], Resolver.Scope.UNIVERSAL);
    Executable executable = Executable.of(program, predeclared);
    SCRIPTS.put(key, new Entry(executable, validity(predeclared, universal)));
    return executable;
  }

  /** Number of cached scripts, for tests. */
  static long scriptCount() {
    return SCRIPTS.size();
  }

  /** Reads the precompiled form of a {@code .star} resource, or returns null. */
  private static CompiledModule precompiled(String path) {
    if (!path.endsWith(LarkyPrecompiler.SOURCE_SUFFIX)) {
      return null;
    }
    String resource = LarkyPrecompiler.compiledName(path);
    try (InputStream in = ProgramCache.class.getClassLoader().getResourceAsStream(resource)) {
      return in == null ? null : CompiledModule.read(in);
    } catch (IOException e) {
      return null; // incompatible or damaged: compile the source instead
    }
  }

  private static ImmutableSet<String> namesOf(StarlarkFile file, Resolver.Scope scope) {
    ImmutableSet.Builder<String> names = ImmutableSet.builder();
    new NodeVisitor() {
      @Override
      public void visit(Identifier id) {
        Resolver.Binding binding = id.getBinding();
        if (binding != null && binding.getScope() == scope) {
          names.add(id.getName());
        }
      }
    }.visit(file);
    return names.build();
  }

  private static Validity validity(ImmutableSet<String> predeclared, ImmutableSet<String> universal) {
    return module -> {
      Map<String, Object> env = module.getPredeclaredBindings();
      for (String name : predeclared) {
        if (!env.containsKey(name)) {
          return false;
        }
      }
      for (String name : universal) {
        if (env.containsKey(name)) {
          return false;
        }
      }
      return true;
    };
  }

  /** Number of cached modules, for tests. */
  static int size() {
    return CACHE.size();
  }

  /** How many modules were loaded precompiled / compiled from source, for tests and metrics. */
  static int loadedPrecompiledCount() {
    return loadedPrecompiled.get();
  }

  static int compiledFromSourceCount() {
    return compiledFromSource.get();
  }

  /** Empties the cache, for tests. */
  static void clear() {
    CACHE.clear();
    SCRIPTS.invalidateAll();
    loadedPrecompiled.set(0);
    compiledFromSource.set(0);
  }
}
