package com.verygood.security.larky;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.modules.VaultModule;
import com.verygood.security.larky.modules.testing.AssertionsModule;
import com.verygood.security.larky.modules.testing.UnittestModule;
import com.verygood.security.larky.modules.vgs.vault.NoopVault;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.PathBasedStarFile;
import com.verygood.security.larky.vgs.vault.TestLarkyVault;
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


public class VGSLibTests {
  private static final String PROPERTY_NAME = "larky.vgs_test";
  private static final Path VGS_TEST_DIR = Paths.get(
      "src","test", "resources", "vgs_tests"
  );
  private static final TestingConsole console = new TestingConsole();

  private LarkyScript interpreter;
  private ModuleSupplier.ModuleSet moduleSet;
  private List<Path> vgsTestFiles;
  private List<Path> vgsOverrideTestFiles;

  @BeforeEach
  public void setUp() {
    ImmutableSet<StarlarkValue> testModules = ImmutableSet.of(
        new UnittestModule(),
        new AssertionsModule()
    );
    moduleSet = new ModuleSupplier().modulesToVariableMap(true);
    interpreter = new LarkyScript(CORE_MODULES, LarkyScript.StarlarkMode.STRICT);
    vgsTestFiles = enumerateTests(false);
    vgsOverrideTestFiles = enumerateTests(true);
  }

  private List<Path> enumerateTests(boolean overrides) {
    // Did we pass in a specific filename?
    // -Dlarky.stdlib_test=test_base64.star

    List<Path> filteredTestFiles;
    String singleTestDesired = System.getProperty(PROPERTY_NAME);
    try (Stream<Path> testFiles = Files.walk(VGS_TEST_DIR)) {
      filteredTestFiles = testFiles
          .filter(Files::isRegularFile)
          //.filter(f -> f.getFileName().startsWith("test_") && f.endsWith(".star"))
          .filter(f -> {
            String fileName = f.getFileName().toString();

            if(!Strings.isNullOrEmpty(singleTestDesired)) {
              return fileName.equals(singleTestDesired);
            }

            if (overrides) {
              return fileName.startsWith("test_override_") && fileName.endsWith(".star");
            }
            return fileName.startsWith("test_") && !fileName.startsWith("test_override_") && fileName.endsWith(".star");
          })
          .sorted((o1, o2) -> Collator.getInstance(Locale.ENGLISH).compare(o1.toString(), o2.toString()))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
    return filteredTestFiles;
  }

  @TestFactory
  public Iterator<DynamicTest> testVGSLib() {
    setModuleOverrides(false);
    return vgsTestFiles.stream().map(f -> DynamicTest.dynamicTest(
        String.format("%s=%s", PROPERTY_NAME, f.getFileName()),
        () -> evaluateTest(interpreter, moduleSet, f)
    )).iterator();
  }

  @TestFactory
  public Iterator<DynamicTest> testVGSOverrides() {
    setModuleOverrides(true);
    return vgsOverrideTestFiles.stream().map(f -> DynamicTest.dynamicTest(
            String.format("%s=%s", PROPERTY_NAME, f.getFileName()),
            () -> evaluateTest(interpreter, moduleSet, f)
    )).iterator();
  }

  // Need this because ModuleSupplier.STD_MODULES are static, so need to be set/reset before running tests
  private void setModuleOverrides(boolean overrides) {
    VaultModule vaultModule = (VaultModule) moduleSet.getModules().get("vault");

    if (overrides) {
      vaultModule.addOverride(new TestLarkyVault());
    } else {
      vaultModule.addOverride(new NoopVault());
    }
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
