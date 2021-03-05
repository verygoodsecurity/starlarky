package com.verygood.security.larky;

import static com.verygood.security.larky.ModuleSupplier.CORE_MODULES;

import com.google.common.collect.ImmutableSet;

import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.modules.globals.LarkyGlobals;
import com.verygood.security.larky.modules.globals.PythonBuiltins;
import com.verygood.security.larky.modules.testing.UnittestModule;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.parser.PathBasedStarFile;
import com.verygood.security.larky.parser.StarFile;

import net.starlark.java.syntax.ParserInput;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LarkyTest {

  @Test
  public void testStarlarkExampleFile() {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_starlark_executes_example.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();

    System.out.println(absolutePath);

    try {
      Assert.assertEquals(
          "Did not successfully evaluate Starlark file",
          0,
          Larky.execute(ParserInput.readFile(absolutePath))
      );
    } catch (IOException e) {
      System.err.println(e.getMessage());
      System.err.println(e.getCause().toString());
      Throwable t = e.getCause();
      Assert.fail(t.toString());
    }
  }

  @Test
  public void testStructBuiltin() throws IOException {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_struct.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    System.out.println(absolutePath);

    LarkyScript interpreter = new LarkyScript(
        CORE_MODULES,
        LarkyScript.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;
    ModuleSupplier.ModuleSet moduleSet = new ModuleSupplier().create();
    config = interpreter.evaluate(starFile, moduleSet, new TestingConsole());
  }

  @Test
  public void testLoadingModules() throws IOException {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_loading_module.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    System.out.println(absolutePath);
/*
 thread.setThreadLocal(Reporter.class, ScriptTest::reportError);
 */
    LarkyScript interpreter = new LarkyScript(
        CORE_MODULES,
        LarkyScript.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;
    config = interpreter.evaluate(starFile, new ModuleSupplier().create(), new TestingConsole());
  }

  @Test
  public void testUnitTestModule() throws IOException {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_unittest.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    System.out.println(absolutePath);

    LarkyScript interpreter = new LarkyScript(
        ImmutableSet.of(
            PythonBuiltins.class,
            LarkyGlobals.class,
            UnittestModule.class
        ),
        LarkyScript.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;
    config = interpreter.evaluate(starFile, new ModuleSupplier().create(), new TestingConsole());
  }

  @Test
  public void testFCOAlternative() throws IOException, URISyntaxException {

    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_fco_operation.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();

    LarkyScript interpreter = new LarkyScript(
        CORE_MODULES,
        LarkyScript.StarlarkMode.STRICT);

    StarFile starFile = new PathBasedStarFile(
            Paths.get(absolutePath),
            null,
            null);
    ParsedStarFile config;
    config = interpreter.evaluate(starFile, new ModuleSupplier(ImmutableSet.of(
        new UnittestModule()
    )).create(), new TestingConsole());
  }
}
