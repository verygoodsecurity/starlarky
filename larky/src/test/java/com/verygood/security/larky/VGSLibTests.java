package com.verygood.security.larky;

import com.google.common.base.Strings;
import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.PathBasedStarFile;
import net.starlark.java.eval.EvalException;
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


public class VGSLibTests {
  private static final String PROPERTY_NAME = "larky.vgs_test";
  private static final Path VGS_TEST_DIR = Paths.get(
      "src","test", "resources", "vgs_tests"
  );
  private static final TestingConsole console = new TestingConsole();

  private LarkyScript interpreter;
  private ModuleSupplier.ModuleSet moduleSet;
  private List<Path> vgsDefaultTestFiles;

  @BeforeEach
  public void setUp() {
    moduleSet = new ModuleSupplier().modulesToVariableMap(true);
    interpreter = new LarkyScript(CORE_MODULES, LarkyScript.StarlarkMode.STRICT);
    vgsDefaultTestFiles = enumerateTests();
  }

  private List<Path> enumerateTests() {
    List<Path> filteredTestFiles;
    String singleTestDesired = System.getProperty(PROPERTY_NAME);
    try (Stream<Path> testFiles = Files.walk(VGS_TEST_DIR)) {
      filteredTestFiles = testFiles
          .filter(Files::isRegularFile)
          .filter(f -> {
            String fileName = f.getFileName().toString();

            if(!Strings.isNullOrEmpty(singleTestDesired)) {
              return fileName.equals(singleTestDesired);
            }

            return fileName.startsWith("test_default_") && fileName.endsWith(".star");
          })
          .sorted((o1, o2) -> Collator.getInstance(Locale.ENGLISH).compare(o1.toString(), o2.toString()))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
    return filteredTestFiles;
  }

  @TestFactory
  public Iterator<DynamicTest> testVGSDefaultLib() {
    return vgsDefaultTestFiles.stream().map(f -> DynamicTest.dynamicTest(
            String.format("%s=%s", PROPERTY_NAME, f.getFileName()),
            () -> evaluateTest(interpreter, moduleSet, f)
    )).iterator();
  }

  private static void evaluateTest(LarkyScript interpreter,
                                   ModuleSupplier.ModuleSet moduleSet,
                                   Path pathToTestFile) {
    try {
      VGSLibTests.console.info("Running test: " + pathToTestFile.toAbsolutePath());
      interpreter.evaluate(
          new PathBasedStarFile(pathToTestFile.toAbsolutePath(), null, null),
          moduleSet,
          VGSLibTests.console
      );
      VGSLibTests.console.info("Successfully executed: " + pathToTestFile);
    } catch (IOException | EvalException e) {
      Assertions.fail(e.getMessage(), e.fillInStackTrace());
    }
  }
}