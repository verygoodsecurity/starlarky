package com.verygood.security.larky;

import static com.verygood.security.larky.ModuleSupplier.CORE_MODULES;

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


public class VendorLibTests {
  private static final String PROPERTY_NAME = "larky.vendor_test";
  private static final Path VENDOR_TEST_DIR = Paths.get(
      "src","test", "resources", "vendor_tests"
  );
  private static final TestingConsole console = new TestingConsole();

  private LarkyScript interpreter;
  private ModuleSupplier.ModuleSet moduleSet;
  private List<Path> vendorTestFiles;

  @BeforeEach
  public void setUp() {
    ImmutableSet<StarlarkValue> testModules = ImmutableSet.of(
        new UnittestModule(),
        new AssertionsModule()
    );
    moduleSet = new ModuleSupplier().modulesToVariableMap(true);
    interpreter = new LarkyScript(CORE_MODULES, LarkyScript.StarlarkMode.STRICT);
    vendorTestFiles = enumerateTests();
  }

  private List<Path> enumerateTests() {
    // Did we pass in a specific filename?
    // -Dlarky.stdlib_test=test_base64.star
    String singleTestDesired = System.getProperty(PROPERTY_NAME);
    try (Stream<Path> testFiles = Files.walk(VENDOR_TEST_DIR)) {
      vendorTestFiles = testFiles
          .filter(Files::isRegularFile)
          //.filter(f -> f.getFileName().startsWith("test_") && f.endsWith(".star"))
          .filter(f -> {
            String fileName = f.getFileName().toString();

            if(!Strings.isNullOrEmpty(singleTestDesired)) {
              return fileName.equals(singleTestDesired);
            }

            return fileName.startsWith("test_") && fileName.endsWith(".star");
          })
          .sorted((o1, o2) -> Collator.getInstance(Locale.ENGLISH).compare(o1.toString(), o2.toString()))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
    return vendorTestFiles;
  }

  @TestFactory
  public Iterator<DynamicTest> testVendorLib() {
    return vendorTestFiles.stream().map(f -> DynamicTest.dynamicTest(
        String.format("%s=%s", PROPERTY_NAME, f.getFileName()),
        () -> evaluateTest(interpreter, moduleSet, f)
    )).iterator();
  }

  private static void evaluateTest(LarkyScript interpreter,
                                   ModuleSupplier.ModuleSet moduleSet,
                                   Path pathToTestFile) {
    try {
      VendorLibTests.console.info("Running test: " + pathToTestFile.toAbsolutePath());
      interpreter.evaluate(
          new PathBasedStarFile(pathToTestFile.toAbsolutePath(), null, null),
          moduleSet,
          VendorLibTests.console
      );
      VendorLibTests.console.info("Successfully executed: " + pathToTestFile);
    } catch (IOException | EvalException e) {
      Assertions.fail(e.getMessage(), e.fillInStackTrace());
    }
  }
}
