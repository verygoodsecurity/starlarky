package com.verygood.security.larky;

import static com.verygood.security.larky.ModuleSupplier.CORE_MODULES;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.modules.testing.AssertionsModule;
import com.verygood.security.larky.modules.testing.UnittestModule;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.PathBasedStarFile;

import net.starlark.java.eval.StarlarkValue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class StdLibTests {
  private static final String PROPERTY_NAME = "larky.stdlib_test";
  private static final Path STDLIB_TEST_DIR = Paths.get("src","test", "resources", "stdlib_tests");
  private static final TestingConsole console = new TestingConsole();

  private LarkyScript interpreter;
  private ModuleSupplier.ModuleSet moduleSet;
  private List<Path> stdLibTestFiles;

  @Before
  public void setUp() {
    ImmutableSet<StarlarkValue> testModules = ImmutableSet.of(
        new UnittestModule(),
        new AssertionsModule()
    );
    moduleSet = new ModuleSupplier(testModules).create();
    interpreter = new LarkyScript(CORE_MODULES, LarkyScript.StarlarkMode.STRICT);
    stdLibTestFiles = enumerateTests();
  }

  private List<Path> enumerateTests() {
    // Did we pass in a specific filename?
    // -Dlarky.stdlib_test=test_base64.star
    String singleTestDesired = System.getProperty(PROPERTY_NAME);
    try (Stream<Path> testFiles = Files.walk(STDLIB_TEST_DIR)) {
      stdLibTestFiles = testFiles
          .filter(Files::isRegularFile)
          //.filter(f -> f.getFileName().startsWith("test_") && f.endsWith(".star"))
          .filter(f -> {
            String fileName = f.getFileName().toString();

            if(!Strings.isNullOrEmpty(singleTestDesired)) {
              return fileName.equals(singleTestDesired);
            }

            return fileName.startsWith("test_") && fileName.endsWith(".star");
          }).collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
    return stdLibTestFiles;
  }

  @Test
  public void testStdLib() throws IOException {
    stdLibTestFiles.forEach(f -> {
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
