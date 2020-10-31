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
    return new LarkyFunctionTestCase(
        thread,
        (StarlarkFunction) function,
        Starlark.repr(function));
  }

  @SuppressWarnings("UnconstructableJUnitTestCase")
  public static class LarkyFunctionTestCase extends TestCase implements StarlarkValue {

    private final StarlarkThread thread;
    private final StarlarkFunction f;

    @SuppressWarnings("CdiInjectionPointsInspection")
    public LarkyFunctionTestCase(StarlarkThread thread, StarlarkFunction f, String name) {
      super(name);
      this.thread = thread;
      this.f = f;
    }

    @Override
    public void runTest() throws InterruptedException, EvalException {
      Starlark.call(this.thread, this.f, ImmutableList.of(), ImmutableMap.of());
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
