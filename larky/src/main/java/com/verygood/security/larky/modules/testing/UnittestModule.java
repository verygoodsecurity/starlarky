package com.verygood.security.larky.modules.testing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import java.util.Enumeration;
import java.util.Iterator;

import com.verygood.security.larky.modules.utils.NullPrintStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import lombok.Getter;
import lombok.Setter;


@StarlarkBuiltin(
    name = "unittest",
    category = "BUILTIN",
    doc = "This module implements unittests")
public class UnittestModule implements StarlarkValue {

  public static final UnittestModule INSTANCE = new UnittestModule();

  @StarlarkMethod(
      name = "TestSuite",
      useStarlarkThread = true)
  public Object createTestSuite(StarlarkThread thread) {
    return new LarkyTestSuite();
  }

  public static class LarkyTestSuite extends TestSuite implements StarlarkValue {
    @StarlarkMethod(
        name = "addTest",
        parameters = {
            @Param(name = "functionTestCase"),
        }
    )
    public Object addTestToSuite(Object functionTestCase) {
      super.addTest((Test) functionTestCase);
      return Starlark.NONE;
    }
  }

  @StarlarkMethod(
      name = "FunctionTestCase",
      parameters = {
          @Param(name = "function"),
      },
      useStarlarkThread = true)
  public Object addFunctionUnderTest(Object function, StarlarkThread thread) {
    LarkyFunctionTestCase tc = new LarkyFunctionTestCase(Starlark.repr(function));
    //TODO: if this fails, we need to return a better error message.
    tc.setFunction((StarlarkCallable) function);
    tc.setThread(thread);
    return tc;
  }


  static class LarkyFunctionTestCase extends TestCase implements StarlarkValue {

    @Getter
    @Setter
    private StarlarkThread thread;

    @Getter
    @Setter
    private StarlarkCallable function;

    public LarkyFunctionTestCase(String name) {
      super(name);
    }

    public LarkyFunctionTestCase(LarkyFunctionTestCase other) {
      super(other.getName());
      setFunction(other.getFunction());
      setThread(other.getThread());
    }

    @Override
    public void runTest() throws EvalException, InterruptedException {
      if(thread == null || function == null) {
        throw new RuntimeException(
            String.format("Thread (%s) and Function (%s) cannot be null!", thread, function));
      }
      Starlark.call(this.thread, this.function, ImmutableList.of(), ImmutableMap.of());
    }
  }

  @StarlarkMethod(
      name = "TextTestRunner",
      useStarlarkThread = true)
  public Object createTestRunner(StarlarkThread thread) {
    return new LarkyTestRunner();
  }

  public static class LarkyTestRunner extends TestRunner implements StarlarkValue {

    private String FINAL_TEST_RESULT = "Total number of tests: %d, started: %d, success: %d, error: %d, skipped: %d";

    public LarkyTestRunner() {
      //super(System.out);
      super(new NullPrintStream());
    }

    @StarlarkMethod(
        name="run",
        parameters = {
            @Param(name = "suiteTest"),
        }
    )
    public void runSuiteTest(Object suiteTest) throws EvalException {
      final Enumeration<Test> tests = ((LarkyTestSuite) suiteTest).tests();
      int allTests = ((LarkyTestSuite) suiteTest).testCount();
      int testCount = 0;
      StringBuilder testSuiteOutput = new StringBuilder(String.format("Running test suite (%d tests to run):\n", allTests));
      while (tests.hasMoreElements()) {
        final LarkyFunctionTestCase testCase = (LarkyFunctionTestCase) tests.nextElement();
        final String name = testCase.getFunction().getName();
        testCount++;
        final TestResult testResult = testCase.run();
        Iterator<TestFailure> it = Iterators.concat(
            Iterators.forEnumeration(testResult.errors()),
            Iterators.forEnumeration(testResult.failures())
        );
        if (it.hasNext()) {
          TestFailure f = it.next();
          testSuiteOutput.append(String.format("Testing %s >>> ERROR!", name)).append("\n")
              .append("Trace:").append("\n")
              .append(f.trace()).append("\n")
              .append(String.format(FINAL_TEST_RESULT, allTests, testCount, testCount - 1, 1, allTests - testCount));
          System.out.println(testSuiteOutput.toString());

          // Re-throw the original exception instead of wrapping the trace string
          Throwable thrown = f.thrownException();
          if (thrown instanceof EvalException) {
            throw (EvalException) thrown;
          } else if (thrown instanceof RuntimeException) {
            throw (RuntimeException) thrown;
          } else {
            throw new EvalException(thrown);
          }
        } else {
          testSuiteOutput.append(String.format("Testing %s >>> SUCCESS", name)).append("\n");
        }
      }
      testSuiteOutput.append(String.format(FINAL_TEST_RESULT, allTests, allTests, 0, 0, 0));
      System.out.println(testSuiteOutput.toString());
    }
    }


  @StarlarkMethod(
      name = "expectedFailure",
      doc = "Mark the test case as an expected failure. If the test fails it will be considered a " +
          "success. If the test passes, it will be considered a failure.",
      parameters = {
                 @Param(name = "testCase"),
             }
  )
  public Object expectTestFailure(Object testCase) {
    return new ExpectedFailure((LarkyFunctionTestCase) testCase);
  }

  @SuppressWarnings("UnconstructableJUnitTestCase")
  static class ExpectedFailure extends LarkyFunctionTestCase {

    public ExpectedFailure(LarkyFunctionTestCase other) {
      super(other);
    }

    @Override
    public void runTest() {
      try {
        super.runTest();
        fail(String.format("Expected %s to fail, but it succeeded.", getName()));
      }
      catch(EvalException
            | InterruptedException
            | StarlarkEvalWrapper.Exc.RuntimeEvalException
            | Starlark.UncheckedEvalException e) {
        // expected
      }
    }

  }

}
