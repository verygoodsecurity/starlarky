package com.verygood.security.larky;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.modules.testing.AssertionsModule;
import com.verygood.security.larky.modules.testing.UnittestModule;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.PathBasedStarFile;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.verygood.security.larky.ModuleSupplier.CORE_MODULES;


public class LarkyQuickTests {

  private static final String PROPERTY_NAME = "larky.quick_test";
  private static final Path QUICK_TEST_DIR = Paths.get(
//      "src", "test", "resources", "quick_tests"
      "..", "examples"
  );
  private static final TestingConsole console = new TestingConsole();

  private LarkyScript interpreter;
  private ModuleSupplier.ModuleSet moduleSet;
  private List<Path> scratchTestFiles;

  @BeforeEach
  public void setUp() {
    ImmutableSet<StarlarkValue> testModules = ImmutableSet.of(
        new UnittestModule(),
        new AssertionsModule()
    );
    moduleSet = new ModuleSupplier().modulesToVariableMap(true);
    interpreter = new LarkyScript(CORE_MODULES, LarkyScript.StarlarkMode.STRICT);
    scratchTestFiles = enumerateTests();
  }

  private List<Path> enumerateTests() {
    // Did we pass in a specific filename?
    // -Dlarky.quick_test=test_base64.star
    String singleTestDesired = System.getProperty(PROPERTY_NAME);
    try (Stream<Path> testFiles = Files.walk(QUICK_TEST_DIR)) {
      scratchTestFiles = testFiles
          .filter(Files::isRegularFile)
          //.filter(f -> f.getFileName().startsWith("test_") && f.endsWith(".star"))
          .filter(f -> {
            String fileName = f.getFileName().toString();

            if (!Strings.isNullOrEmpty(singleTestDesired)) {
              return fileName.equals(singleTestDesired);
            }

            return fileName.startsWith("test_") && fileName.endsWith(".star");
          })
          .sorted((o1, o2) -> Collator.getInstance(Locale.ENGLISH).compare(o1.toString(), o2.toString()))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
    return scratchTestFiles;
  }

  @TestFactory
  public Iterator<DynamicTest> dynamicTestGenerator() {
    return scratchTestFiles.stream().map(f -> DynamicTest.dynamicTest(
        String.format("%s=%s", PROPERTY_NAME, f.getFileName()),
        () -> evaluateTest(interpreter, moduleSet, f)
    )).iterator();
  }

  private static void evaluateTest(LarkyScript interpreter,
                                   ModuleSupplier.ModuleSet moduleSet,
                                   Path pathToTestFile) {
    try {
      LarkyQuickTests.console.info("Running test: " + pathToTestFile.toAbsolutePath());
      interpreter.evaluate(
          new PathBasedStarFile(pathToTestFile.toAbsolutePath(), null, null),
          moduleSet,
          LarkyQuickTests.console
      );
      LarkyQuickTests.console.info("Successfully executed: " + pathToTestFile);
    } catch (IOException | EvalException e) {
      Assertions.fail(e.getMessage(), e.fillInStackTrace());
    }
  }
}
