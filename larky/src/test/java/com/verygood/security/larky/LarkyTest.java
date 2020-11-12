package com.verygood.security.larky;

import com.google.common.collect.ImmutableSet;

import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.nativelib.LarkyGlobals;
import com.verygood.security.larky.nativelib.LarkyUnittest;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.parser.PathBasedStarFile;
import com.verygood.security.larky.parser.StarFile;
import com.verygood.security.messages.operations.Http;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.syntax.ParserInput;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

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
  public void testStdLib() throws IOException {
    Path stdlibTestDir = Paths.get(
            "src",
            "test",
            "resources",
            "stdlib_tests");
    LarkyScript interpreter = new LarkyScript(
      ImmutableSet.of(
          LarkyGlobals.class
      ),
      LarkyScript.StarlarkMode.STRICT);

    ModuleSupplier.ModuleSet moduleSet = new ModuleSupplier(ImmutableSet.of(
      new LarkyUnittest()
    )).create();

    TestingConsole console = new TestingConsole();
    try (Stream<Path> paths = Files.walk(stdlibTestDir)) {
      paths
        .filter(Files::isRegularFile)
        //.filter(f -> f.getFileName().startsWith("test_") && f.endsWith(".star"))
        .filter(f -> {
          String fileName = f.getFileName().toString();
          return fileName.startsWith("test_") && fileName.endsWith(".star");
        })
        .forEach(f -> {
          try {
            console.info("Running test: " + f);
            interpreter.evaluate(
              new PathBasedStarFile(f.toAbsolutePath(), null, null),
              moduleSet,
              console
            );
            console.info("Successfully executed: " + f);
          } catch (IOException e) {
            Assert.fail(e.getMessage());
          }
        });
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
        ImmutableSet.of(LarkyGlobals.class),
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

    LarkyScript interpreter = new LarkyScript(
        ImmutableSet.of(LarkyGlobals.class),
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
            LarkyGlobals.class,
            LarkyUnittest.class
        ),
        LarkyScript.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;
    config = interpreter.evaluate(starFile, new ModuleSupplier().create(), new TestingConsole());
  }

  @StarlarkBuiltin(
      name = "FCOHelper",
      category = "BUILTIN",
      doc = "messages namespace"
  )
  public static class FCOHelper implements StarlarkValue {

    @StarlarkMethod(
        name = "HttpMessage",
        parameters = {
            @Param(name = "function"),
        },
        useStarlarkThread = true)
    public Object httpMessage(Object function, StarlarkThread thread) {
      return null;
    }

    @StarlarkMethod(
        name = "HttpHeader",
        parameters = {
            @Param(name = "function"),
        },
        useStarlarkThread = true)
    public Object httpHeader(Object function, StarlarkThread thread) {
      return null;
    }

    @StarlarkMethod(
        name = "HttpPhase",
        parameters = {
            @Param(name = "function"),
        },
        useStarlarkThread = true)
    public Object httpPhase(Object function, StarlarkThread thread) {
      return null;
    }

  }

  @Test
  public void testFCOAlternative() throws IOException, URISyntaxException {

    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_fco_operation.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();


    Http.HttpPhase httpPhase = Http.HttpPhase.forNumber(1);
    System.out.println(httpPhase);


    LarkyScript interpreter = new LarkyScript(
        ImmutableSet.of(
            LarkyGlobals.class
        ),
        LarkyScript.StarlarkMode.STRICT);

    StarFile starFile = new PathBasedStarFile(
            Paths.get(absolutePath),
            null,
            null);
    ParsedStarFile config;
    config = interpreter.evaluate(starFile, new ModuleSupplier(ImmutableSet.of(
        new LarkyUnittest(),
        new FCOHelper()
    )).create(), new TestingConsole());
  }
}