package com.verygood.security.larky;

import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.parser.LarkyParser;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.parser.PathBasedStarFile;
import com.verygood.security.larky.parser.StarFile;
import com.verygood.security.larky.stdlib.LarkyBuiltin;
import com.verygood.security.larky.stdlib.LarkyHashlibModule;

import net.starlark.java.syntax.ParserInput;

import org.junit.Assert;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

public class LarkyTest {

  @org.junit.Test
  public void main() {
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
  public void main2() throws IOException {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_loading_module.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    System.out.println(absolutePath);

    LarkyParser parser = new LarkyParser(
        new HashSet<>() {{
          add(LarkyHashlibModule.class);
        }},
        LarkyParser.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;

    //env.put("json", Json.INSTANCE);
    ModuleSet moduleSet = ModuleSet.getInstance();
    config = parser.loadStarFile(starFile, moduleSet, new TestingConsole());
    System.out.println("hello");
  }

  @org.junit.Test
  public void test3() throws IOException {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_struct.star");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    System.out.println(absolutePath);

    LarkyParser parser = new LarkyParser(
        new HashSet<>() {{
          add(LarkyBuiltin.class);
        }},
        LarkyParser.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;
    ModuleSet moduleSet = ModuleSet.getInstance();
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
        new HashSet<>() {{
          add(LarkyBuiltin.class);
        }},
        LarkyParser.StarlarkMode.STRICT);
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    ParsedStarFile config;
    ModuleSet moduleSet = ModuleSet.getInstance();
    config = parser.loadStarFile(starFile, moduleSet, new TestingConsole());
  }
}