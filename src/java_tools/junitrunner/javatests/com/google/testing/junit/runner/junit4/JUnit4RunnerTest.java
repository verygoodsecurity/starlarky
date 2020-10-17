// Copyright 2010 The Bazel Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.testing.junit.runner.junit4;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.google.testing.junit.runner.internal.SignalHandlers.HandlerInstaller;
import com.google.testing.junit.runner.internal.junit4.CancellableRequestFactory;
import com.google.testing.junit.runner.internal.junit4.SettableCurrentRunningTest;
import com.google.testing.junit.runner.junit4.JUnit4InstanceModules.SuiteClass;
import com.google.testing.junit.runner.model.AntXmlResultWriter;
import com.google.testing.junit.runner.model.XmlResultWriter;
import com.google.testing.junit.runner.sharding.ShardingEnvironment;
import com.google.testing.junit.runner.sharding.ShardingFilters;
import com.google.testing.junit.runner.sharding.api.ShardingFilterFactory;
import com.google.testing.junit.runner.sharding.testing.FakeShardingFilters;
import com.google.testing.junit.runner.util.CurrentRunningTest;
import com.google.testing.junit.runner.util.FakeTestClock;
import com.google.testing.junit.runner.util.GoogleTestSecurityManager;
import com.google.testing.junit.runner.util.TestClock;
import com.google.testing.junit.runner.util.TestNameProvider;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.JUnit4;
import org.junit.runners.Suite;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Tests for {@link JUnit4Runner}
 */
@RunWith(MockitoJUnitRunner.class)
public class JUnit4RunnerTest {
  private final ByteArrayOutputStream stdoutByteStream = new ByteArrayOutputStream();
  private final PrintStream stdoutPrintStream = new PrintStream(stdoutByteStream, true);
  @Mock private RunListener mockRunListener;
  @Mock private ShardingEnvironment shardingEnvironment = new StubShardingEnvironment();
  @Mock private ShardingFilters shardingFilters;
  private JUnit4Config config;
  private boolean wasSecurityManagerInstalled = false;
  private SecurityManager previousSecurityManager;

  @After
  public void closeStream() throws Exception {
    stdoutPrintStream.close();
  }

  @After
  public void reinstallPreviousSecurityManager() {
    if (wasSecurityManagerInstalled) {
      wasSecurityManagerInstalled = false;
      System.setSecurityManager(previousSecurityManager);
    }
  }

  private JUnit4Runner createRunner(Class<?> suiteClass) {
    return createComponent(suiteClass).runner();
  }

  private JUnit4BazelMock createComponent(Class<?> suiteClass) {
    return JUnit4BazelMock.builder()
        .suiteClass(new SuiteClass(suiteClass))
        .testModule(new TestModule()) // instance method to support outer-class instance variables.
        .build();
  }

  @Test
  public void testPassingTest() throws Exception {
    config = createConfig();
    mockRunListener = mock(RunListener.class);

    JUnit4Runner runner = createRunner(SamplePassingTest.class);

    Description testDescription =
        Description.createTestDescription(SamplePassingTest.class, "testThatAlwaysPasses");
    Description suiteDescription =
        Description.createSuiteDescription(SamplePassingTest.class);
    suiteDescription.addChild(testDescription);

    Result result = runner.run();

    assertThat(result.getRunCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(0);
    assertThat(result.getIgnoreCount()).isEqualTo(0);

    assertPassingTestHasExpectedOutput(stdoutByteStream, SamplePassingTest.class);

    InOrder inOrder = inOrder(mockRunListener);

    inOrder.verify(mockRunListener).testRunStarted(suiteDescription);
    inOrder.verify(mockRunListener).testStarted(testDescription);
    inOrder.verify(mockRunListener).testFinished(testDescription);
    inOrder.verify(mockRunListener).testRunFinished(any(Result.class));
  }

  @Test
  public void testFailingTest() throws Exception {
    config = createConfig();
    mockRunListener = mock(RunListener.class);

    JUnit4Runner runner = createRunner(SampleFailingTest.class);

    Description testDescription = Description.createTestDescription(SampleFailingTest.class,
        "testThatAlwaysFails");
    Description suiteDescription = Description.createSuiteDescription(SampleFailingTest.class);
    suiteDescription.addChild(testDescription);

    Result result = runner.run();

    assertThat(result.getRunCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getIgnoreCount()).isEqualTo(0);

    assertThat(extractOutput(stdoutByteStream))
        .contains(
            "1) testThatAlwaysFails("
                + SampleFailingTest.class.getName()
                + ")\n"
                + "java.lang.AssertionError: expected");

    InOrder inOrder = inOrder(mockRunListener);

    inOrder.verify(mockRunListener).testRunStarted(any(Description.class));
    inOrder.verify(mockRunListener).testStarted(any(Description.class));
    inOrder.verify(mockRunListener).testFailure(any(Failure.class));
    inOrder.verify(mockRunListener).testFinished(any(Description.class));
    inOrder.verify(mockRunListener).testRunFinished(any(Result.class));
  }

  @Test
  public void testFailingInternationalCharsTest() throws Exception {
    config = createConfig();
    mockRunListener = mock(RunListener.class);

    JUnit4Runner runner = createRunner(SampleInternationalFailingTest.class);

    Description testDescription = Description.createTestDescription(
        SampleInternationalFailingTest.class, "testFailingInternationalCharsTest");
    Description suiteDescription = Description.createSuiteDescription(
        SampleInternationalFailingTest.class);
    suiteDescription.addChild(testDescription);

    Result result = runner.run();

    assertThat(result.getRunCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getIgnoreCount()).isEqualTo(0);

    String output = new String(stdoutByteStream.toByteArray(), StandardCharsets.UTF_8);
    // Intentionally swapped "Test 日\u672C." / "Test \u65E5本." to make sure that the "raw"
    // character does not get corrupted (would become ? in both cases and we would not notice).
    assertThat(output).contains("expected:<Test [Japan].> but was:<Test [日\u672C].>");

    InOrder inOrder = inOrder(mockRunListener);

    inOrder.verify(mockRunListener).testRunStarted(any(Description.class));
    inOrder.verify(mockRunListener).testStarted(any(Description.class));
    inOrder.verify(mockRunListener).testFailure(any(Failure.class));
    inOrder.verify(mockRunListener).testFinished(any(Description.class));
    inOrder.verify(mockRunListener).testRunFinished(any(Result.class));
  }

  @Test
  public void testInterruptedTest() throws Exception {
    config = createConfig();
    mockRunListener = mock(RunListener.class);
    JUnit4BazelMock component = createComponent(SampleSuite.class);
    JUnit4Runner runner = component.runner();
    final CancellableRequestFactory requestFactory = component.cancellableRequestFactory();

    Description testDescription = Description.createTestDescription(SamplePassingTest.class,
        "testThatAlwaysPasses");

    doAnswer(cancelTestRun(requestFactory))
        .when(mockRunListener).testStarted(testDescription);

    RuntimeException e = assertThrows(RuntimeException.class, () -> runner.run());
    assertThat(e).hasMessageThat().isEqualTo("Test run interrupted");
      assertWithMessage("Expected cause to be a StoppedByUserException")
          .that(e.getCause() instanceof StoppedByUserException)
          .isTrue();

      InOrder inOrder = inOrder(mockRunListener);
      inOrder.verify(mockRunListener).testRunStarted(any(Description.class));
      inOrder.verify(mockRunListener).testStarted(testDescription);
    inOrder.verify(mockRunListener).testFinished(testDescription);
  }

  private static Answer<Void> cancelTestRun(final CancellableRequestFactory requestFactory) {
    return new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) {
        requestFactory.cancelRun();
        return null;
      }
    };
  }

  @Test
  public void testSecurityManagerInstalled() throws Exception {
    // If there is already a security manager installed, the runner would crash when trying to
    // install another one. In order to avoid that, the security manager should be uninstalled here
    // and restored afterwards.
    uninstallGoogleTestSecurityManager();

    config = new JUnit4Config(null, null, null, createProperties("1", true));

    JUnit4Runner runner = createRunner(SampleExitingTest.class);
    Result result = runner.run();

    assertThat(result.getRunCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getIgnoreCount()).isEqualTo(0);
  }

  @Test
  public void testShardingIsSupported() {
    config = createConfig();
    shardingEnvironment = mock(ShardingEnvironment.class);
    shardingFilters = new FakeShardingFilters(
        Description.createTestDescription(SamplePassingTest.class, "testThatAlwaysPasses"),
        Description.createTestDescription(SampleFailingTest.class, "testThatAlwaysFails"));

    when(shardingEnvironment.isShardingEnabled()).thenReturn(true);

    JUnit4Runner runner = createRunner(SampleSuite.class);
    Result result = runner.run();

    verify(shardingEnvironment).touchShardFile();

    assertThat(result.getRunCount()).isEqualTo(2);
    if (result.getFailureCount() > 1) {
      fail("Too many failures: " + result.getFailures());
    }
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getIgnoreCount()).isEqualTo(0);
    assertThat(runner.getModel().getNumTestCases()).isEqualTo(2);
  }

  @Test
  public void testFilteringIsSupported() {
    config = createConfig("testThatAlwaysFails");
    JUnit4Runner runner = createRunner(SampleSuite.class);
    Result result = runner.run();

    assertThat(result.getRunCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getIgnoreCount()).isEqualTo(0);
    assertThat(result.getFailures().get(0).getDescription())
        .isEqualTo(
            Description.createTestDescription(SampleFailingTest.class, "testThatAlwaysFails"));
  }

  @Test
  public void testRunFailsWithAllTestsFilteredOut() {
    config = createConfig("doesNotMatchAnything");
    JUnit4Runner runner = createRunner(SampleSuite.class);
    Result result = runner.run();

    assertThat(result.getRunCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getIgnoreCount()).isEqualTo(0);
    assertThat(result.getFailures().get(0).getMessage()).contains("No tests found");
  }

  @Test
  public void testRunExcludeFilterAlwaysExits() {
    config = new JUnit4Config("test", "CallsSystemExit", null, createProperties("1", false));
    JUnit4Runner runner = createRunner(SampleSuite.class);
    Result result = runner.run();

    assertThat(result.getRunCount()).isEqualTo(2);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getIgnoreCount()).isEqualTo(0);
    assertThat(result.getFailures().get(0).getDescription())
        .isEqualTo(
            Description.createTestDescription(SampleFailingTest.class, "testThatAlwaysFails"));
  }

  @Test
  public void testFilteringAndShardingTogetherIsSupported() {
    config = createConfig("testThatAlways(Passes|Fails)");
    shardingEnvironment = mock(ShardingEnvironment.class);
    shardingFilters = new FakeShardingFilters(
        Description.createTestDescription(SamplePassingTest.class, "testThatAlwaysPasses"),
        Description.createTestDescription(SampleFailingTest.class, "testThatAlwaysFails"));

    when(shardingEnvironment.isShardingEnabled()).thenReturn(true);

    JUnit4Runner runner = createRunner(SampleSuite.class);
    Result result = runner.run();

    verify(shardingEnvironment).touchShardFile();

    assertThat(result.getRunCount()).isEqualTo(2);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getIgnoreCount()).isEqualTo(0);
    assertThat(result.getFailures().get(0).getDescription())
        .isEqualTo(
            Description.createTestDescription(SampleFailingTest.class, "testThatAlwaysFails"));
  }

  @Test
  public void testRunPassesWhenNoTestsOnCurrentShardWithFiltering() {
    config = createConfig("testThatAlwaysFails");
    shardingEnvironment = mock(ShardingEnvironment.class);
    shardingFilters = new FakeShardingFilters(
        Description.createTestDescription(SamplePassingTest.class, "testThatAlwaysPasses"));

    when(shardingEnvironment.isShardingEnabled()).thenReturn(true);

    JUnit4Runner runner = createRunner(SampleSuite.class);
    Result result = runner.run();

    verify(shardingEnvironment).touchShardFile();

    assertThat(result.getRunCount()).isEqualTo(0);
    assertThat(result.getFailureCount()).isEqualTo(0);
    assertThat(result.getIgnoreCount()).isEqualTo(0);
  }

  @Test
  public void testRunFailsWhenNoTestsOnCurrentShardWithoutFiltering() {
    config = createConfig();
    shardingEnvironment = mock(ShardingEnvironment.class);
    shardingFilters = mock(ShardingFilters.class);

    when(shardingEnvironment.isShardingEnabled()).thenReturn(true);
    when(shardingFilters.createShardingFilter(anyList())).thenReturn(new NoneShallPassFilter());

    JUnit4Runner runner = createRunner(SampleSuite.class);
    Result result = runner.run();

    assertThat(result.getRunCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getIgnoreCount()).isEqualTo(0);
    assertThat(result.getFailures().get(0).getMessage()).contains("No tests found");

    verify(shardingEnvironment).touchShardFile();
    verify(shardingFilters).createShardingFilter(anyList());
  }

  @Test
  public void testMustSpecifySupportedJUnitApiVersion() {
    config = new JUnit4Config(null, null, null, createProperties("2", false));
    JUnit4Runner runner = createRunner(SamplePassingTest.class);

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> runner.run());
    assertThat(e).hasMessageThat().startsWith("Unsupported JUnit Runner API version");
  }

  /**
   * Uninstall {@link GoogleTestSecurityManager} if it is installed. If it was installed, it will
   * be reinstalled after the test completes.
   */
  private void uninstallGoogleTestSecurityManager() {
    previousSecurityManager = System.getSecurityManager();
    GoogleTestSecurityManager.uninstallIfInstalled();
    if (previousSecurityManager != System.getSecurityManager()) {
      wasSecurityManagerInstalled = true;
    }
  }

  private void assertPassingTestHasExpectedOutput(ByteArrayOutputStream outputStream,
      Class<?> testClass) {
    ByteArrayOutputStream expectedOutputStream = getExpectedOutput(testClass);

    assertThat(extractOutput(outputStream)).isEqualTo(extractOutput(expectedOutputStream));
  }

  private String extractOutput(ByteArrayOutputStream outputStream) {
    String output = new String(outputStream.toByteArray(), Charset.defaultCharset());
    return output.replaceFirst("\nTime: .*\n", "\nTime: 0\n");
  }

  private ByteArrayOutputStream getExpectedOutput(Class<?> testClass) {
    JUnitCore core = new JUnitCore();

    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(byteStream);
    printStream.println("JUnit4 Test Runner");
    RunListener listener = new TextListener(printStream);
    core.addListener(listener);

    Request request = Request.classWithoutSuiteMethod(testClass);

    core.run(request);
    printStream.close();

    return byteStream;
  }

  private static JUnit4Config createConfig() {
    return createConfig(null);
  }

  private static JUnit4Config createConfig(@Nullable String includeFilter) {
    return new JUnit4Config(includeFilter, null, null, createProperties("1", false));
  }

  private static Properties createProperties(
      String apiVersion, boolean shouldInstallSecurityManager) {
    Properties properties = new Properties();
    properties.setProperty(JUnit4Config.JUNIT_API_VERSION_PROPERTY, apiVersion);
    if (!shouldInstallSecurityManager) {
      properties.setProperty("java.security.manager", "whatever");
    }
    return properties;
  }

  /** Sample test that passes. */
  @RunWith(JUnit4.class)
  public static class SamplePassingTest {

    @Test
    public void testThatAlwaysPasses() {
    }
  }


  /** Sample test that fails. */
  @RunWith(JUnit4.class)
  public static class SampleFailingTest {

    @Test
    public void testThatAlwaysFails() {
      org.junit.Assert.fail("expected");
    }
  }


  /** Sample test that fails and shows international text without corrupting it. */
  @RunWith(JUnit4.class)
  public static class SampleInternationalFailingTest {

    @Test
    public void testThatAlwaysFails() {
      // Use JUnit asserts instead of Truth, since Truth's message format is subject to change.
      assertEquals("Test Japan.", "Test \u65E5本.");
    }
  }


  /** Sample test that calls System.exit(). */
  @RunWith(JUnit4.class)
  public static class SampleExitingTest {

    @Test
    public void testThatAlwaysCallsSystemExit() {
      System.exit(1);
    }
  }


  /** Sample suite. */
  @RunWith(Suite.class)
  @Suite.SuiteClasses({
      JUnit4RunnerTest.SamplePassingTest.class,
      JUnit4RunnerTest.SampleFailingTest.class,
      JUnit4RunnerTest.SampleExitingTest.class
  })
  public static class SampleSuite {}


  private static class StubShardingEnvironment extends ShardingEnvironment {

    @Override
    public boolean isShardingEnabled() {
      return false;
    }

    @Override
    public int getShardIndex() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getTotalShards() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void touchShardFile() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getTestShardingStrategy() {
      throw new UnsupportedOperationException();
    }
  }


  /**
   * Filter that won't run any tests.
   */
  private static class NoneShallPassFilter extends Filter {

    @Override
    public boolean shouldRun(Description description) {
      return false;
    }

    @Override
    public String describe() {
      return "none-shall-pass filter";
    }
  }


  private static class StubHandlerInstaller implements HandlerInstaller {

    @Override
    public SignalHandler install(Signal signal, SignalHandler handler) {
      return null;
    }
  }


  class TestModule {

    ShardingEnvironment shardingEnvironment() {
      return shardingEnvironment;
    }

    TestClock clock() {
      return new FakeTestClock();
    }

    JUnit4Config config() {
      return config;
    }

    HandlerInstaller handlerInstaller() {
      return new StubHandlerInstaller();
    }

    OutputStream xmlOutputStream() {
      return ByteStreams.nullOutputStream();
    }

    XmlResultWriter xmlResultWriter(AntXmlResultWriter impl) {
      return impl;
    }

    Set<RunListener> mockRunListener() {
      return (mockRunListener == null)
          ? ImmutableSet.<RunListener>of()
          : ImmutableSet.of(mockRunListener);
    }

    ShardingFilters shardingFilters(
        ShardingEnvironment shardingEnvironment, ShardingFilterFactory defaultShardingStrategy) {
      return (shardingFilters == null)
          ? new ShardingFilters(shardingEnvironment, defaultShardingStrategy)
          : shardingFilters;
    }

    PrintStream provideStdoutStream() {
      return new PrintStream(stdoutByteStream);
    }

    PrintStream provideStderrStream() {
      return new PrintStream(ByteStreams.nullOutputStream());
    }

    CurrentRunningTest provideCurrentRunningTest() {
      return new SettableCurrentRunningTest() {
        @Override
        protected void setGlobalTestNameProvider(TestNameProvider provider) {
          // Do not set the global current running test when the JUnit4Runner is being tested itself,
          // in order not to override the real one.
        }
      };
    }
  }
}
