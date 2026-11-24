package com.verygood.security.larky.runtime;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.parser.EvaluationMetadata;
import com.verygood.security.larky.parser.EvaluationSettings;
import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ResourceContentStarFile;
import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import net.starlark.java.eval.StarlarkEvalWrapper;
import org.junit.Test;

/**
 * The steps of the script's own code ({@link EvaluationMetadata#executedSteps()}) are the same
 * whether or not the Larky modules it loads were already loaded in the process; initializing those
 * modules is reported apart ({@link EvaluationMetadata#libraryInitSteps()}), and counts against
 * the step limit only when {@link EvaluationSettings#chargeLibraryInit()}.
 */
public class LibraryInitStepsTest {

  private static final String SCRIPT =
      String.join(
          "\n",
          "load('@stdlib//base64', 'base64')",
          "def f():",
          "    return base64.b64encode(b'larky')",
          "out = f()",
          "");

  // Each run gets a module cache of the test's own (turned on), so a warm run shares the modules
  // a cold one loaded, without touching the process-wide cache.
  private static EvaluationMetadata run(EvaluationSettings settings, ModuleCache cache)
      throws Exception {
    return new CachedTestScript(
            ModuleSupplier.CORE_MODULES,
            LarkyScript.StarlarkMode.STRICT,
            ImmutableMap.of(),
            settings)
        .withModuleCache(cache)
        .evaluate(InMemMapBackedStarFile.createStarFile("main.star", SCRIPT), new TestingConsole())
        .metadata();
  }

  @Test
  public void theScriptsStepsDoNotDependOnWhatWasAlreadyLoaded() throws Exception {
    ModuleCache cache = new ModuleCache(true);
    EvaluationMetadata cold = run(EvaluationSettings.NONE, cache);
    EvaluationMetadata warm = run(EvaluationSettings.NONE, cache);

    assertThat(cold.libraryInitSteps()).isGreaterThan(0L);
    assertThat(warm.libraryInitSteps()).isAtMost(cold.libraryInitSteps());
    assertThat(warm.executedSteps()).isEqualTo(cold.executedSteps());
    assertThat(cold.engine()).isEqualTo(EvaluationMetadata.TREE_WALKER);
  }

  @Test
  public void libraryInitCountsAgainstTheLimitUnlessTheHostSaysOtherwise() throws Exception {
    ModuleCache warmCache = new ModuleCache(true);
    long own = run(EvaluationSettings.NONE, warmCache).executedSteps();
    long init = run(EvaluationSettings.NONE, warmCache).libraryInitSteps(); // what is left to init
    long coldInit = run(EvaluationSettings.NONE, new ModuleCache(true)).libraryInitSteps();
    assertThat(coldInit).isGreaterThan(init + 10);

    // a limit just above the script's own steps
    long limit = own + 10;

    StarlarkEvalWrapper.Exc.RuntimeEvalException e =
        assertThrows(
            StarlarkEvalWrapper.Exc.RuntimeEvalException.class,
            () -> run(EvaluationSettings.builder().stepLimit(limit).build(), new ModuleCache(true)));
    assertThat(e.metadata().outcome()).isEqualTo(EvaluationMetadata.Outcome.STEP_LIMIT);

    EvaluationMetadata free =
        run(
            EvaluationSettings.builder().stepLimit(limit).chargeLibraryInit(false).build(),
            new ModuleCache(true));
    assertThat(free.outcome()).isEqualTo(EvaluationMetadata.Outcome.OK);
    assertThat(free.executedSteps()).isEqualTo(own);
    assertThat(free.libraryInitSteps()).isEqualTo(coldInit);
  }

  @Test
  public void hostStreamsAreChargedWhenLibraryInitializationIsFree() throws Exception {
    var settings = EvaluationSettings.builder().stepLimit(1).chargeLibraryInit(false).build();
    var evaluator =
        new LarkyEvaluator(
            new LarkyScript(ModuleSupplier.CORE_MODULES, LarkyScript.StarlarkMode.STRICT,
                ImmutableMap.of(), settings),
            new TestingConsole());
    var file =
        ResourceContentStarFile.buildStarFile(
            "stdlib/sets.star",
            new ByteArrayInputStream("result = 1 + 2".getBytes(StandardCharsets.UTF_8)));

    assertThrows(StarlarkEvalWrapper.Exc.RuntimeEvalException.class, () -> evaluator.eval(file));
    assertThat(evaluator.lastMetadata().libraryInitSteps()).isEqualTo(0L);
    assertThat(evaluator.lastMetadata().outcome()).isEqualTo(EvaluationMetadata.Outcome.STEP_LIMIT);
  }

}
