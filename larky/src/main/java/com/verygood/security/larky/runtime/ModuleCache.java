package com.verygood.security.larky.runtime;

import com.verygood.security.larky.parser.ResourceContentStarFile;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.verygood.security.larky.ModuleSupplier;
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
 * <p>A loaded module is frozen: its lists, dicts, structs and classes reject mutation (see
 * SharedModuleIsolationTest), and each evaluation keeps its own top-level module and globals,
 * discarded when it ends.
 *
 * <p>A module's values may capture the values of the predeclared names it uses and of the modules
 * it loads, so a cached module is reused only if, in the new evaluation's environment, every
 * predeclared name it uses is bound to the identical object, and every module it loaded is itself
 * reusable. Per-evaluation bindings (e.g. JSR-223's) that a module uses therefore prevent sharing
 * it rather than leaking one evaluation's values into another.
 *
 * <p>Frozen is not enough to make a module safe to share: a value may keep Java state that
 * freezing does not cover. So a module is cached only if {@link ModuleAdmission} finds nothing in
 * it that belongs to one evaluation; otherwise it is declined, with a reason, and each evaluation
 * that loads it runs it itself. A declined module is never run again to be cached: the evaluation
 * that ran it keeps using it.
 *
 * <p>Off by default: {@code -Dlarky.moduleCache.enable=true} turns it on, and
 * {@code -Dlarky.moduleCache.disable=true} keeps it off even then.
 */
final class ModuleCache {

  /**
   * The cache evaluations use unless given another (see {@link LarkyEvaluator}): on
   * only with {@code -Dlarky.moduleCache.enable=true}.
   */
  static final ModuleCache SHARED =
      new ModuleCache(
          Boolean.getBoolean("larky.moduleCache.enable")
              && !Boolean.getBoolean("larky.moduleCache.disable"));

  private final boolean enabled;

  /** A cache of its own, for tests: they never turn the process-wide one on or off. */
  ModuleCache(boolean enabled) {
    this.enabled = enabled;
  }

  boolean isEnabled() {
    return enabled;
  }

  enum Registry {
    DEFAULT,
    TEST;

    private static final ImmutableMap<String, Object> DEFAULT_MODULES =
        new ModuleSupplier().modulesToVariableMap();
    private static final ImmutableMap<String, Object> TEST_MODULES =
        new ModuleSupplier().modulesToVariableMap(true).getModules();

    @Nullable
    static Registry from(ImmutableMap<String, Object> modules) {
      if (modules == DEFAULT_MODULES) {
        return DEFAULT;
      }
      return modules == TEST_MODULES ? TEST : null;
    }
  }

  private record Key(
      String path, FileOptions options, StarlarkSemantics semantics, Registry registry) {}

  /**
   * A cached module and the conditions for reusing it: the values it and, transitively, every
   * module it loaded captured from the environment, flattened when it is cached so that checking
   * them does not walk the dependency graph (which revisits shared dependencies) on every load.
   */
  private record Entry(
      Module module,
      ImmutableMap<String, Object> captured,
      ImmutableSet<String> universal,
      ImmutableSet<String> loadTargets) {

    boolean reusableIn(Map<String, Object> env) {
      for (Map.Entry<String, Object> e : captured.entrySet()) {
        if (env.get(e.getKey()) != e.getValue()) {
          return false;
        }
      }
      for (String name : universal) {
        if (env.containsKey(name)) {
          return false;
        }
      }
      for (String target : loadTargets) {
        if (env.get(target) instanceof Module) {
          return false;
        }
      }
      return true;
    }
  }

  private final ConcurrentHashMap<Key, Entry> cache = new ConcurrentHashMap<>();

  // Only published entries belong here. An evaluation still owns its losing candidates.
  private final Map<Module, Entry> byModule = Collections.synchronizedMap(new IdentityHashMap<>());

  // Native module wrappers: created once per process and never dependent on the evaluation, so
  // every cache can share a module that loads one.
  private static final Set<Module> STABLE =
      Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

  /** Returns the cached module for {@code path} if it can be reused in {@code env}, else null. */
  @Nullable
  Module lookup(
      String path, FileOptions options, StarlarkSemantics semantics, Registry registry,
      Map<String, Object> env) {
    if (!enabled) {
      return null;
    }
    Entry entry = cache.get(new Key(path, options, semantics, registry));
    return entry != null && entry.reusableIn(env) ? entry.module() : null;
  }

  /**
   * Caches a module just loaded from {@code path} if it may be shared: everything it loaded is
   * shared, and {@link ModuleAdmission} accepts what it reaches.
   *
   * @param predeclaredNames names the file resolved as PREDECLARED (null if unknown: not cached)
   * @param env the environment the module was loaded in
   * @param builtins the builtins that every evaluation shares anyway (created once per process),
   *     by name: the walk accepts them wherever it meets them, as the cache does not make them any
   *     more shared than they are. Any other predeclared value the module uses (a host binding) is
   *     checked
   * @param loaded the modules it loaded, by load label
   * @return why it was not cached, or null if it was (or the cache is off)
   */
  @Nullable
  ModuleAdmission.Decline put(
      String path,
      FileOptions options,
      StarlarkSemantics semantics,
      Registry registry,
      Module module,
      @Nullable Set<String> predeclaredNames,
      Set<String> universalNames,
      Map<String, Object> env,
      Map<String, Object> builtins,
      Map<String, Module> loaded) {
    if (!enabled) {
      return null;
    }
    if (predeclaredNames == null) {
      return new ModuleAdmission.Decline(
          ModuleAdmission.Reason.NOT_ANALYZED, "predeclared names unknown", "");
    }
    if (!predeclaredNames.containsAll(module.getPredeclaredBindings().keySet())) {
      return new ModuleAdmission.Decline(
          ModuleAdmission.Reason.NOT_ANALYZED, "unverified predeclared bindings", "");
    }
    Key key = new Key(path, options, semantics, registry);
    var universal = ImmutableSet.<String>builder().addAll(universalNames);
    var loadTargets = ImmutableSet.<String>builder();
    Map<String, Object> captured = new java.util.HashMap<>();
    Map<String, Object> usedPredeclared = new java.util.TreeMap<>();
    for (String name : predeclaredNames) {
      Object value = env.get(name);
      if (value != null) {
        captured.put(name, value);
        usedPredeclared.put(name, value);
      }
    }
    for (Map.Entry<String, Module> load : loaded.entrySet()) {
      Module dependency = load.getValue();
      if (ResourceContentStarFile.startsWithPrefix(load.getKey())) {
        String target = ResourceContentStarFile.getModulePath(load.getKey());
        if (env.get(target) instanceof Module) {
          return new ModuleAdmission.Decline(
              ModuleAdmission.Reason.DEPENDS_ON, load.getKey(), "");
        }
        loadTargets.add(target);
      }
      if (STABLE.contains(dependency)) {
        continue; // a native module wrapper: captures nothing
      }
      Entry entry = byModule.get(dependency);
      if (entry == null) {
        // depends on a module that is not shared: running this one from the cache would skip it
        return new ModuleAdmission.Decline(ModuleAdmission.Reason.DEPENDS_ON, load.getKey(), "");
      }
      universal.addAll(entry.universal());
      loadTargets.addAll(entry.loadTargets());
      for (Map.Entry<String, Object> e : entry.captured().entrySet()) {
        Object previous = captured.putIfAbsent(e.getKey(), e.getValue());
        if (previous != null && previous != e.getValue()) {
          // no environment can bind this name to both values
          return new ModuleAdmission.Decline(
              ModuleAdmission.Reason.DEPENDS_ON, load.getKey(), "<predeclared " + e.getKey() + ">");
        }
      }
    }
    Map<Module, String> labels = new IdentityHashMap<>();
    loaded.forEach((label, m) -> labels.putIfAbsent(m, label));
    ModuleAdmission.Decline decline =
        ModuleAdmission.check(
            module,
            usedPredeclared,
            builtins.values(),
            new ModuleAdmission.Modules() {
              @Override
              public boolean admitted(Module m) {
                return byModule.containsKey(m);
              }

              @Override
              public boolean nativeWrapper(Module m) {
                return STABLE.contains(m);
              }

              @Override
              public String label(Module m) {
                return labels.getOrDefault(m, "<module>");
              }
            });
    if (decline != null) {
      return decline;
    }
    Entry entry =
        new Entry(module, ImmutableMap.copyOf(captured), universal.build(), loadTargets.build());
    synchronized (byModule) {
      if (cache.putIfAbsent(key, entry) == null) {
        byModule.put(module, entry);
      }
    }
    return null;
  }

  /** Registers a module whose values never depend on the evaluation (a native module wrapper). */
  static void putStable(Module module) {
    STABLE.add(module);
  }

  /** Paths of the cached modules, for tests. */
  Set<String> cachedPaths() {
    Set<String> paths = new java.util.TreeSet<>();
    for (Key key : cache.keySet()) {
      paths.add(key.path());
    }
    return paths;
  }

  /** Number of cached modules, for tests. */
  int size() {
    return cache.size();
  }

  /** Whether {@code module} is a module this cache admitted, for tests. */
  boolean admitted(Module module) {
    return byModule.containsKey(module);
  }

  /** Empties the cache, for tests. */
  void clear() {
    cache.clear();
    byModule.clear();
  }
}
