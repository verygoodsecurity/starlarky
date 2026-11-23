package com.verygood.security.larky.parser;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.util.Map;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.syntax.FileOptions;
import org.junit.Test;

/** Each test uses a cache of its own, so tests can run concurrently with each other and others. */
public class ModuleCacheTest {

  private final ModuleCache cache = new ModuleCache(true);

  private static LarkyEvaluator newEvaluator(ModuleCache cache) {
    return new LarkyEvaluator(
        new LarkyScript(LarkyScript.StarlarkMode.STRICT).withModuleCache(cache),
        new TestingConsole());
  }

  @Test
  public void notSharedWhenDisabled() throws Exception {
    ModuleCache disabled = new ModuleCache(false);
    Module first =
        newEvaluator(disabled).eval(ResourceContentStarFile.buildStarFile("@stdlib//sets")).module();
    Module second =
        newEvaluator(disabled).eval(ResourceContentStarFile.buildStarFile("@stdlib//sets")).module();
    assertThat(second).isNotSameInstanceAs(first);
    assertThat(disabled.size()).isEqualTo(0);
  }

  @Test
  public void evaluationsShareALoadedStdlibModule() throws Exception {
    Module first =
        newEvaluator(cache).eval(ResourceContentStarFile.buildStarFile("@stdlib//sets")).module();
    Module second =
        newEvaluator(cache).eval(ResourceContentStarFile.buildStarFile("@stdlib//sets")).module();
    assertThat(second).isSameInstanceAs(first);
    assertThat(cache.size()).isGreaterThan(0);
  }

  @Test
  public void theProcessWideCacheIsOnUnlessTurnedOff() {
    assumeTrue(System.getProperty("larky.moduleCache.enable") == null
        && System.getProperty("larky.moduleCache.disable") == null);
    assertThat(ModuleCache.SHARED.isEnabled()).isTrue();
  }

  @Test
  public void eitherPropertyTurnsTheProcessWideCacheOff() {
    assertThat(ModuleCache.enabled(null, false)).isTrue();
    assertThat(ModuleCache.enabled("true", false)).isTrue();
    assertThat(ModuleCache.enabled("false", false)).isFalse();
    assertThat(ModuleCache.enabled(null, true)).isFalse();
    assertThat(ModuleCache.enabled("true", true)).isFalse();
  }

  private static final ModuleCache.Registry REGISTRY = ModuleCache.Registry.DEFAULT;
  private static final FileOptions OPTIONS = FileOptions.DEFAULT;
  private static final StarlarkSemantics SEMANTICS = StarlarkSemantics.DEFAULT;

  @Test
  public void notReusedWhenAUsedNameIsBoundToADifferentObject() {
    Module module = Module.create();
    Object v1 = new Object();
    cache.put("a.star", OPTIONS, SEMANTICS, REGISTRY, module, ImmutableSet.of("x"), ImmutableSet.of(),
        ImmutableMap.of("x", v1), ImmutableMap.of(), ImmutableMap.of());
    assertThat(cache.lookup("a.star", OPTIONS, SEMANTICS, REGISTRY, ImmutableMap.of("x", v1)))
        .isSameInstanceAs(module);
    // Another evaluation's object.
    assertThat(cache.lookup("a.star", OPTIONS, SEMANTICS, REGISTRY,
        ImmutableMap.of("x", new Object()))).isNull();
    // Unused names don't matter.
    assertThat(cache.lookup("a.star", OPTIONS, SEMANTICS, REGISTRY,
        ImmutableMap.of("x", v1, "other", StarlarkInt.of(1)))).isSameInstanceAs(module);
  }

  @Test
  public void notReusedWhenADependencyIsNotReusable() {
    Object v1 = new Object();
    Module dep = Module.create();
    cache.put("dep.star", OPTIONS, SEMANTICS, REGISTRY, dep, ImmutableSet.of("x"), ImmutableSet.of(),
        ImmutableMap.of("x", v1), ImmutableMap.of(), ImmutableMap.of());
    Module top = Module.create();
    cache.put("top.star", OPTIONS, SEMANTICS, REGISTRY, top, ImmutableSet.of(), ImmutableSet.of(),
        ImmutableMap.of("x", v1), ImmutableMap.of(), Map.of("dep", dep));
    assertThat(cache.lookup("top.star", OPTIONS, SEMANTICS, REGISTRY, ImmutableMap.of("x", v1)))
        .isSameInstanceAs(top);
    assertThat(cache.lookup("top.star", OPTIONS, SEMANTICS, REGISTRY,
        ImmutableMap.of("x", new Object()))).isNull();
  }

  @Test
  public void notCachedWhenADependencyIsNotShared() {
    Module unshared = Module.create();
    Module top = Module.create();
    cache.put("top.star", OPTIONS, SEMANTICS, REGISTRY, top, ImmutableSet.of(), ImmutableSet.of(),
        ImmutableMap.of(), ImmutableMap.of(), Map.of("unshared", unshared));
    assertThat(cache.lookup("top.star", OPTIONS, SEMANTICS, REGISTRY, ImmutableMap.of())).isNull();
  }

  @Test
  public void sharedResourceDoesNotRetainUnusedHostBindings() throws Exception {
    LarkyEvaluator evaluator =
        new LarkyEvaluator(
            new LarkyScript(
                    ModuleSupplier.CORE_MODULES,
                    LarkyScript.StarlarkMode.STRICT,
                    Map.of("unused_host_binding", "request value"))
                .withModuleCache(cache),
            new TestingConsole());
    Module module = evaluator.eval(ResourceContentStarFile.buildStarFile("@stdlib//sets")).module();

    assertThat(cache.admitted(module)).isTrue();
    assertThat(module.getPredeclaredBindings()).doesNotContainKey("unused_host_binding");
  }

  @Test
  public void overridingAUniversalNameDoesNotReuseTheResourceModule() throws Exception {
    var starlarkFile = ResourceContentStarFile.buildStarFile("@stdlib//sets");
    Module original = newEvaluator(cache).eval(starlarkFile).module();
    LarkyEvaluator evaluator =
        new LarkyEvaluator(
            new LarkyScript(
                    ModuleSupplier.CORE_MODULES,
                    LarkyScript.StarlarkMode.STRICT,
                    Map.of("dict", StarlarkInt.of(1)))
                .withModuleCache(cache),
            new TestingConsole());

    Module overridden = evaluator.eval(starlarkFile).module();

    assertThat(original).isNotSameInstanceAs(overridden);
    assertThat(overridden.getPredeclaredBindings()).containsKey("dict");
  }

  @Test
  public void transitiveResolutionMustMatch() {
    Module nativeModule = Module.create();
    ModuleCache.putStable(nativeModule);
    Module dependency = Module.create();
    cache.put("dep.star", OPTIONS, SEMANTICS, REGISTRY, dependency, ImmutableSet.of(),
        ImmutableSet.of("dict"), ImmutableMap.of(), ImmutableMap.of(),
        Map.of("@stdlib//c99math", nativeModule));
    Module top = Module.create();
    cache.put("top.star", OPTIONS, SEMANTICS, REGISTRY, top, ImmutableSet.of(),
        ImmutableSet.of(), ImmutableMap.of(), ImmutableMap.of(), Map.of("dep", dependency));

    assertThat(cache.lookup("top.star", OPTIONS, SEMANTICS, REGISTRY, ImmutableMap.of()))
        .isSameInstanceAs(top);
    assertThat(cache.lookup("top.star", OPTIONS, SEMANTICS, ModuleCache.Registry.TEST,
        ImmutableMap.of())).isNull();
    assertThat(cache.lookup("top.star", OPTIONS, SEMANTICS, REGISTRY,
        Map.of("dict", StarlarkInt.of(1)))).isNull();
    assertThat(cache.lookup("top.star", OPTIONS, SEMANTICS, REGISTRY,
        Map.of("c99math", Module.create()))).isNull();
  }

  @Test
  public void aLosingCandidateIsNotRetained() {
    Module winner = Module.create();
    Module loser = Module.create();
    for (Module module : new Module[] {winner, loser}) {
      cache.put("entry.star", OPTIONS, SEMANTICS, REGISTRY, module, ImmutableSet.of(),
          ImmutableSet.of(), ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of());
    }
    assertThat(cache.admitted(winner)).isTrue();
    assertThat(cache.admitted(loser)).isFalse();
  }

}
