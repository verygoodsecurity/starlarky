package com.verygood.security.larky.parser;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeFalse;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
  public void theProcessWideCacheIsOffUnlessEnabled() {
    assumeFalse(Boolean.getBoolean("larky.moduleCache.enable"));
    assertThat(ModuleCache.SHARED.isEnabled()).isFalse();
  }

  private static final FileOptions OPTIONS = FileOptions.DEFAULT;
  private static final StarlarkSemantics SEMANTICS = StarlarkSemantics.DEFAULT;

  @Test
  public void notReusedWhenAUsedNameIsBoundToADifferentObject() {
    Module module = Module.create();
    Object v1 = new Object();
    cache.put("a.star", OPTIONS, SEMANTICS, module, ImmutableSet.of("x"),
        ImmutableMap.of("x", v1), ImmutableMap.of());
    assertThat(cache.lookup("a.star", OPTIONS, SEMANTICS, ImmutableMap.of("x", v1)))
        .isSameInstanceAs(module);
    // Another evaluation's object.
    assertThat(cache.lookup("a.star", OPTIONS, SEMANTICS,
        ImmutableMap.of("x", new Object()))).isNull();
    // Unused names don't matter.
    assertThat(cache.lookup("a.star", OPTIONS, SEMANTICS,
        ImmutableMap.of("x", v1, "other", StarlarkInt.of(1)))).isSameInstanceAs(module);
  }

  @Test
  public void notReusedWhenADependencyIsNotReusable() {
    Object v1 = new Object();
    Module dep = Module.create();
    cache.put("dep.star", OPTIONS, SEMANTICS, dep, ImmutableSet.of("x"),
        ImmutableMap.of("x", v1), ImmutableMap.of());
    Module top = Module.create();
    cache.put("top.star", OPTIONS, SEMANTICS, top, ImmutableSet.of(),
        ImmutableMap.of("x", v1), Map.of("dep", dep));
    assertThat(cache.lookup("top.star", OPTIONS, SEMANTICS, ImmutableMap.of("x", v1)))
        .isSameInstanceAs(top);
    assertThat(cache.lookup("top.star", OPTIONS, SEMANTICS,
        ImmutableMap.of("x", new Object()))).isNull();
  }

  @Test
  public void notCachedWhenADependencyIsNotShared() {
    Module unshared = Module.create();
    Module top = Module.create();
    cache.put("top.star", OPTIONS, SEMANTICS, top, ImmutableSet.of(),
        ImmutableMap.of(), Map.of("unshared", unshared));
    assertThat(cache.lookup("top.star", OPTIONS, SEMANTICS, ImmutableMap.of())).isNull();
  }
}
