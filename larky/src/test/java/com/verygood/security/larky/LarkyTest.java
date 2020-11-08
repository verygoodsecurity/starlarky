package com.verygood.security.larky;

import com.google.common.collect.ImmutableSet;

import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.nativelib.LarkyGlobals;
import com.verygood.security.larky.nativelib.LarkyUnittest;
import com.verygood.security.larky.parser.LarkyParser;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LarkyTest {

  @org.junit.Test
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

  @org.junit.Test
  public void testStructBuiltin() throws IOException {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_struct.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    System.out.println(absolutePath);

    LarkyParser parser = new LarkyParser(
        ImmutableSet.of(LarkyGlobals.class),
        LarkyParser.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;
    ModuleSupplier.ModuleSet moduleSet = new ModuleSupplier().create();
    config = parser.loadStarFile(starFile, moduleSet, new TestingConsole());
  }

  @org.junit.Test
  public void testLoadingModules() throws IOException {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_loading_module.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    System.out.println(absolutePath);

    LarkyParser parser = new LarkyParser(
        ImmutableSet.of(LarkyGlobals.class),
        LarkyParser.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;
    config = parser.loadStarFile(starFile, new ModuleSupplier().create(), new TestingConsole());
  }

  @org.junit.Test
  public void testUnitTestModule() throws IOException {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_unittest.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    System.out.println(absolutePath);

    LarkyParser parser = new LarkyParser(
        ImmutableSet.of(
            LarkyGlobals.class,
            LarkyUnittest.class
        ),
        LarkyParser.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;
    config = parser.loadStarFile(starFile, new ModuleSupplier().create(), new TestingConsole());
  }

  @org.junit.Test
  public void testAsserts() throws IOException {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_asserts.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    System.out.println(absolutePath);

    LarkyParser parser = new LarkyParser(
        ImmutableSet.of(
            LarkyGlobals.class,
            LarkyUnittest.class
        ),
        LarkyParser.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;
    config = parser.loadStarFile(starFile, new ModuleSupplier().create(), new TestingConsole());
  }

  @StarlarkBuiltin(
      name = "vgs_messages",
      category = "BUILTIN",
      doc = "messages namespace"
  )
  public static class VGSMessages implements StarlarkValue {

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

  @org.junit.Test
  public void testFCOAlternative() throws IOException, URISyntaxException {

    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_fco_operation.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();


    Http.HttpPhase httpPhase = Http.HttpPhase.forNumber(1);
    System.out.println(httpPhase);


    LarkyParser parser = new LarkyParser(
        ImmutableSet.of(
            LarkyGlobals.class
        ),
        LarkyParser.StarlarkMode.STRICT);

    URL stdlib = this.getClass().getClassLoader().getResource("stdlib");
    if (stdlib == null) throw new AssertionError("Cannot find: " + stdlib);
    String identifierPrefix = "@stdlib";
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        Paths.get(stdlib.toURI()),
        identifierPrefix);

    ParsedStarFile config;
    config = parser.loadStarFile(starFile, new ModuleSupplier(ImmutableSet.of(
        new LarkyUnittest(),
        new VGSMessages()
    )).create(), new TestingConsole());
  }
}