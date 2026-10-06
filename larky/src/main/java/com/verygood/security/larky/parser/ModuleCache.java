package com.verygood.security.larky.parser;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.syntax.FileOptions;

/**
 * Process-wide cache of loaded Larky modules (stdlib, vendor, vgs): the module values themselves,
 * shared by every evaluation instead of re-running each module's top-level code.
 *
 * <p>Sharing is safe because a loaded module is frozen: its lists, dicts, structs and classes
 * reject mutation (see SharedModuleIsolationTest), and each evaluation keeps its own top-level
 * module and globals, discarded when it ends.
 *
 * <p>A module's values may capture the values of the predeclared names it uses and of the modules
 * it loads, so a cached module is reused only if, in the new evaluation's environment, every
 * predeclared name it uses is bound to the identical object, and every module it loaded is itself
 * reusable. Per-evaluation bindings (e.g. JSR-223's) that a module uses therefore prevent sharing
 * it rather than leaking one evaluation's values into another.
 *
 * <p>{@code -Dlarky.moduleCache.disable=true} turns it off.
 */
final class ModuleCache {

  private ModuleCache() {}

  static final boolean DISABLED = Boolean.getBoolean("larky.moduleCache.disable");

  private record Key(String path, FileOptions options, StarlarkSemantics semantics) {}

  /**
   * A cached module and the conditions for reusing it: the values it and, transitively, every
   * module it loaded captured from the environment, flattened when it is cached so that checking
   * them does not walk the dependency graph (which revisits shared dependencies) on every load.
   */
  private record Entry(Module module, ImmutableMap<String, Object> captured) {

    boolean reusableIn(Map<String, Object> env) {
      for (Map.Entry<String, Object> e : captured.entrySet()) {
        if (env.get(e.getKey()) != e.getValue()) {
          return false;
        }
      }
      return true;
    }
  }

  private static final ConcurrentHashMap<Key, Entry> CACHE = new ConcurrentHashMap<>();

  // Cached (and native) modules by identity, to find a loaded module's entry.
  private static final Map<Module, Entry> BY_MODULE =
      Collections.synchronizedMap(new IdentityHashMap<>());

  private static Key key(String path, FileOptions options, StarlarkSemantics semantics) {
    return new Key(path, options, semantics);
  }

  /** Returns the cached module for {@code path} if it can be reused in {@code env}, else null. */
  @Nullable
  static Module lookup(
      String path, FileOptions options, StarlarkSemantics semantics, Map<String, Object> env) {
    if (DISABLED) {
      return null;
    }
    Entry entry = CACHE.get(key(path, options, semantics));
    return entry != null && entry.reusableIn(env) ? entry.module() : null;
  }

  /**
   * Caches a module just loaded from {@code path}, if everything it depends on is shareable.
   *
   * @param predeclaredNames names the file resolved as PREDECLARED (null if unknown: not cached)
   * @param env the environment the module was loaded in
   * @param loaded the modules it loaded
   */
  static void put(
      String path,
      FileOptions options,
      StarlarkSemantics semantics,
      Module module,
      @Nullable Set<String> predeclaredNames,
      Map<String, Object> env,
      Map<String, Module> loaded) {
    if (DISABLED || predeclaredNames == null) {
      return;
    }
    Map<String, Object> captured = new java.util.HashMap<>();
    for (String name : predeclaredNames) {
      Object value = env.get(name);
      if (value != null) {
        captured.put(name, value);
      }
    }
    for (Map.Entry<String, Module> load : loaded.entrySet()) {
      Module dependency = load.getValue();
      if (env.get(load.getKey()) == dependency) {
        captured.put(load.getKey(), dependency); // a module from the environment itself
        continue;
      }
      Entry entry = BY_MODULE.get(dependency);
      if (entry == null) {
        return; // depends on a module that is not shared: don't share this one either
      }
      for (Map.Entry<String, Object> e : entry.captured().entrySet()) {
        Object previous = captured.putIfAbsent(e.getKey(), e.getValue());
        if (previous != null && previous != e.getValue()) {
          return; // no environment can satisfy both: don't share
        }
      }
    }
    Entry entry = new Entry(module, ImmutableMap.copyOf(captured));
    CACHE.put(key(path, options, semantics), entry);
    BY_MODULE.put(module, entry);
  }

  /** Registers a module whose values never depend on the evaluation (a native module wrapper). */
  static void putStable(Module module) {
    BY_MODULE.put(module, new Entry(module, ImmutableMap.of()));
  }

  /** Number of cached modules, for tests. */
  static int size() {
    return CACHE.size();
  }

  /** Empties the cache, for tests. */
  static void clear() {
    CACHE.clear();
    BY_MODULE.clear();
  }
}
