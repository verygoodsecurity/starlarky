package com.verygood.security.larky;

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
  public void main2() {
    Path resourceDirectory = Paths.get(
        "src",
        "test",
        "resources",
        "test_loading_module.bzl");
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
}