package com.verygood.security.larky.parser;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.util.Map;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.syntax.FileOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ModuleCacheTest {

  @Before
  @After
  public void clear() {
    ModuleCache.clear();
  }

  private static LarkyEvaluator newEvaluator() {
    return new LarkyEvaluator(
        new LarkyScript(LarkyScript.StarlarkMode.STRICT), new TestingConsole());
  }

  @Test
  public void evaluationsShareALoadedStdlibModule() throws Exception {
    Module first =
        newEvaluator().eval(ResourceContentStarFile.buildStarFile("@stdlib//sets")).module();
    Module second =
        newEvaluator().eval(ResourceContentStarFile.buildStarFile("@stdlib//sets")).module();
    assertThat(second).isSameInstanceAs(first);
    assertThat(ModuleCache.size()).isGreaterThan(0);
  }

  private static final FileOptions OPTIONS = FileOptions.DEFAULT;
  private static final StarlarkSemantics SEMANTICS = StarlarkSemantics.DEFAULT;

  @Test
  public void notReusedWhenAUsedNameIsBoundToADifferentObject() {
    Module module = Module.create();
    Object v1 = new Object();
    ModuleCache.put("a.star", OPTIONS, SEMANTICS, module, ImmutableSet.of("x"),
        ImmutableMap.of("x", v1), ImmutableMap.of());
    assertThat(ModuleCache.lookup("a.star", OPTIONS, SEMANTICS, ImmutableMap.of("x", v1)))
        .isSameInstanceAs(module);
    // Another evaluation's object.
    assertThat(ModuleCache.lookup("a.star", OPTIONS, SEMANTICS,
        ImmutableMap.of("x", new Object()))).isNull();
    // Unused names don't matter.
    assertThat(ModuleCache.lookup("a.star", OPTIONS, SEMANTICS,
        ImmutableMap.of("x", v1, "other", StarlarkInt.of(1)))).isSameInstanceAs(module);
  }

  @Test
  public void notReusedWhenADependencyIsNotReusable() {
    Object v1 = new Object();
    Module dep = Module.create();
    ModuleCache.put("dep.star", OPTIONS, SEMANTICS, dep, ImmutableSet.of("x"),
        ImmutableMap.of("x", v1), ImmutableMap.of());
    Module top = Module.create();
    ModuleCache.put("top.star", OPTIONS, SEMANTICS, top, ImmutableSet.of(),
        ImmutableMap.of("x", v1), Map.of("dep", dep));
    assertThat(ModuleCache.lookup("top.star", OPTIONS, SEMANTICS, ImmutableMap.of("x", v1)))
        .isSameInstanceAs(top);
    assertThat(ModuleCache.lookup("top.star", OPTIONS, SEMANTICS,
        ImmutableMap.of("x", new Object()))).isNull();
  }

  @Test
  public void notCachedWhenADependencyIsNotShared() {
    Module unshared = Module.create();
    Module top = Module.create();
    ModuleCache.put("top.star", OPTIONS, SEMANTICS, top, ImmutableSet.of(),
        ImmutableMap.of(), Map.of("customer", unshared));
    assertThat(ModuleCache.lookup("top.star", OPTIONS, SEMANTICS, ImmutableMap.of())).isNull();
  }
}
