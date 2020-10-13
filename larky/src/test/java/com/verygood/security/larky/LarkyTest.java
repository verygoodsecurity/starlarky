package com.verygood.security.larky;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import com.verygood.security.larky.lang.ParsedStarFile;
import com.verygood.security.larky.lang.StarFile;
import com.verygood.security.larky.lang.PathBasedStarFile;
import com.verygood.security.larky.lang.LarkyParser;
import com.verygood.security.larky.modules.ModuleSet;
import com.verygood.security.larky.modules.hashlib.StarlarkHashlibModule;
import com.verygood.security.larky.console.StarlarkMode;
import com.verygood.security.larky.console.testing.TestingConsole;

import net.starlark.java.syntax.ParserInput;

import org.junit.Assert;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LarkyTest {

  @org.junit.Test
  public void main() {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_starlark_executes_example.bzl");
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
        "test_loading_module.bzl");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();

    System.out.println(absolutePath);
    ModuleSet moduleSet = ModuleSet.getInstance(
        ImmutableSet.of(
            StarlarkHashlibModule.class
        ),
        ImmutableMap.<String, Object>builder().build()
    );
    LarkyParser parser = new LarkyParser(
        moduleSet.getStaticModules(),
        StarlarkMode.STRICT);
    ParsedStarFile config;
    StarFile starFile = new PathBasedStarFile(
        Paths.get(absolutePath),
        null,
        null);
    config = parser.loadStarFile(starFile, moduleSet, new TestingConsole());
    System.out.println("hello");
  }
}