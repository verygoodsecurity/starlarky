package com.verygood.security.larky.nativelib;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.io.ByteStreams;

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
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import java.io.PrintStream;
import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;


@StarlarkBuiltin(
    name = "unittest",
    category = "BUILTIN",
    doc = "This module implements unittests")
public class LarkyUnittest implements StarlarkValue {


  @StarlarkMethod(
      name = "TestSuite",
      useStarlarkThread = true)
  public Object createTestSuite(StarlarkThread thread) {
    // Starlark.call(thread, function, ImmutableList.of(), ImmutableMap.of())
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
    tc.setFunction((StarlarkFunction) function);
    tc.setThread(thread);
    return tc;
  }


  static class LarkyFunctionTestCase extends TestCase implements StarlarkValue {

    @Getter
    @Setter
    private StarlarkThread thread;

    @Getter
    @Setter
    private StarlarkFunction function;

    @SuppressWarnings("CdiInjectionPointsInspection")
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

  static final class NullPrintStream extends PrintStream {
     @SuppressWarnings("UnstableApiUsage")
     public NullPrintStream() {
       super(ByteStreams.nullOutputStream());
     }
   }

  public static class LarkyTestRunner extends TestRunner implements StarlarkValue {
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
      LarkyTestSuite suite = (LarkyTestSuite) suiteTest;
      TestResult result = doRun(suite);
      if(!result.wasSuccessful()) {
        Iterator<TestFailure> it = Iterators.concat(
            result.errors().asIterator(),
            result.failures().asIterator());
        //noinspection LoopStatementThatDoesntLoop
        while (it.hasNext()) {
          TestFailure f = it.next();
          throw Starlark.errorf(f.trace());
        }
      }
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

    @SuppressWarnings("CdiInjectionPointsInspection")
    public ExpectedFailure(LarkyFunctionTestCase other) {
      super(other);
    }

    @Override
    public void runTest() {
      try {
        super.runTest();
        fail(String.format("Expected %s to fail, but it succeeded.", getName()));
      }
      catch(EvalException|InterruptedException e) {
        // expected
      }
    }

  }

}
