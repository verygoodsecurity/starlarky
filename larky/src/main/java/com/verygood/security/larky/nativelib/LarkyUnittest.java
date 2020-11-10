package com.verygood.security.larky.nativelib;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import junit.framework.Test;
import junit.framework.TestCase;
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
    public Object addTest(Object functionTestCase) {
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
  public Object addCase(Object function, StarlarkThread thread) {
    LarkyFunctionTestCase tc = new LarkyFunctionTestCase(Starlark.repr(function));
    tc.setFunction((StarlarkFunction) function);
    tc.setThread(thread);
    return tc;
  }

  static class LarkyFunctionTestCase extends TestCase implements StarlarkValue {

    private StarlarkThread thread;
    private StarlarkFunction function;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public LarkyFunctionTestCase(String name) {
      super(name);
    }

    public void setThread(StarlarkThread thread) {
      this.thread = thread;
    }

    public void setFunction(StarlarkFunction function) {
      this.function = function;
    }

    @Override
    public void runTest() throws InterruptedException, EvalException {
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
    public LarkyTestRunner() {
      super(System.out);
    }

    @StarlarkMethod(
        name="run",
        parameters = {
            @Param(name = "suiteTest"),
        }
    )
    public void run(Object suiteTest) {
      LarkyTestSuite suite = (LarkyTestSuite) suiteTest;
      System.out.println(suite);
      TestRunner.run(suite);
    }

  }

}
