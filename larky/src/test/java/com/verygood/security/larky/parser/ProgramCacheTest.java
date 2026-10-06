package com.verygood.security.larky.parser;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.util.concurrent.atomic.AtomicInteger;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.StarlarkFile;
import net.starlark.java.syntax.SyntaxError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProgramCacheTest {

  private static final String SOURCE = "y = x + len([1])\n";

  private final AtomicInteger compiles = new AtomicInteger();

  @Before
  public void setUp() {
    ProgramCache.clear();
    ModuleCache.clear(); // a cached module would skip the program cache entirely
  }

  @After
  public void tearDown() {
    ProgramCache.clear();
    ModuleCache.clear(); // a cached module would skip the program cache entirely
  }

  private ProgramCache.Executable get(Module module) throws EvalException {
    return ProgramCache.get(
        "test/cached.star",
        module,
        FileOptions.DEFAULT,
        StarlarkSemantics.DEFAULT,
        parsed -> {
          compiles.incrementAndGet();
          try {
            StarlarkFile file =
                StarlarkFile.parse(ParserInput.fromString(SOURCE, "cached.star"), FileOptions.DEFAULT);
            Program program = Program.compileFile(file, module);
            parsed[0] = file;
            return program;
          } catch (SyntaxError.Exception e) {
            throw new EvalException(e.getMessage());
          }
        });
  }

  private static Module env(ImmutableMap<String, Object> predeclared) {
    return Module.withPredeclared(StarlarkSemantics.DEFAULT, predeclared);
  }

  @Test
  public void reusesProgramForTheSameEnvironment() throws Exception {
    ProgramCache.Executable first = get(env(ImmutableMap.of("x", StarlarkInt.of(1))));
    ProgramCache.Executable second = get(env(ImmutableMap.of("x", StarlarkInt.of(2))));
    assertThat(second).isSameInstanceAs(first);
    assertThat(compiles.get()).isEqualTo(1);
  }

  @Test
  public void reusesProgramWhenUnreferencedNamesChange() throws Exception {
    ProgramCache.Executable first = get(env(ImmutableMap.of("x", StarlarkInt.of(1))));
    // Per-evaluation bindings (e.g. JSR-223's) the file never mentions don't affect resolution.
    ProgramCache.Executable second =
        get(env(ImmutableMap.of("x", StarlarkInt.of(1), "script_input_1234", StarlarkInt.of(0))));
    assertThat(second).isSameInstanceAs(first);
    assertThat(compiles.get()).isEqualTo(1);
  }

  @Test
  public void recompilesWhenAPredeclaredNameIsMissing() throws Exception {
    get(env(ImmutableMap.of("x", StarlarkInt.of(1))));
    assertThrows(EvalException.class, () -> get(env(ImmutableMap.of())));
    assertThat(compiles.get()).isEqualTo(2);
  }

  @Test
  public void recompilesWhenAUniversalNameBecomesPredeclared() throws Exception {
    ProgramCache.Executable first = get(env(ImmutableMap.of("x", StarlarkInt.of(1))));
    // `len` resolved as UNIVERSAL the first time; a predeclared `len` now shadows it.
    ProgramCache.Executable second =
        get(env(ImmutableMap.of("x", StarlarkInt.of(1), "len", StarlarkInt.of(0))));
    assertThat(second).isNotSameInstanceAs(first);
    assertThat(compiles.get()).isEqualTo(2);
  }

  @Test
  public void compilesEachDistinctScriptOnce() throws Exception {
    for (String source : new String[] {"x = 1\n", "x = 1\n", "x = 2\n", "x = 1\n"}) {
      newEvaluator().eval(InMemMapBackedStarFile.createStarFile("main.star", source));
    }
    assertThat(ProgramCache.scriptCount()).isEqualTo(2);
  }

  @Test
  public void usesPrecompiledModulesWhenBytecodeIsEnabled() throws Exception {
    newEvaluator().eval(ResourceContentStarFile.buildStarFile("@stdlib//sets"));
    if (net.starlark.java.eval.compiler.BytecodeCompiler.enabledByDefault()) {
      // stdlib/sets.starc, written at build time by LarkyPrecompiler.
      assertThat(ProgramCache.loadedPrecompiledCount()).isGreaterThan(0);
    } else {
      assertThat(ProgramCache.loadedPrecompiledCount()).isEqualTo(0);
      assertThat(ProgramCache.compiledFromSourceCount()).isGreaterThan(0);
    }
  }

  private static LarkyEvaluator newEvaluator() {
    return new LarkyEvaluator(
        new LarkyScript(LarkyScript.StarlarkMode.STRICT), new TestingConsole());
  }
}
