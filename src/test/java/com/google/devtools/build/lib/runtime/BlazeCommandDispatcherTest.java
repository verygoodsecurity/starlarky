// Copyright 2020 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.runtime;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.google.common.testing.GcFinalization;
import com.google.common.util.concurrent.SettableFuture;
import com.google.devtools.build.lib.analysis.BlazeDirectories;
import com.google.devtools.build.lib.analysis.ServerDirectories;
import com.google.devtools.build.lib.analysis.config.BuildOptions;
import com.google.devtools.build.lib.bugreport.BugReporter;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.events.EventKind;
import com.google.devtools.build.lib.events.Reporter;
import com.google.devtools.build.lib.runtime.CommandDispatcher.LockingMode;
import com.google.devtools.build.lib.runtime.proto.InvocationPolicyOuterClass.InvocationPolicy;
import com.google.devtools.build.lib.server.FailureDetails;
import com.google.devtools.build.lib.server.FailureDetails.BuildProgress;
import com.google.devtools.build.lib.server.FailureDetails.BuildProgress.Code;
import com.google.devtools.build.lib.server.FailureDetails.Crash;
import com.google.devtools.build.lib.server.FailureDetails.FailureDetail;
import com.google.devtools.build.lib.server.FailureDetails.Spawn;
import com.google.devtools.build.lib.testutil.MoreAsserts;
import com.google.devtools.build.lib.testutil.Scratch;
import com.google.devtools.build.lib.testutil.TestConstants;
import com.google.devtools.build.lib.testutil.TestThread;
import com.google.devtools.build.lib.testutil.TestUtils;
import com.google.devtools.build.lib.util.AbruptExitException;
import com.google.devtools.build.lib.util.DetailedExitCode;
import com.google.devtools.build.lib.util.ExitCode;
import com.google.devtools.build.lib.util.io.OutErr;
import com.google.devtools.build.lib.util.io.RecordingOutErr;
import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionDocumentationCategory;
import com.google.devtools.common.options.OptionEffectTag;
import com.google.devtools.common.options.OptionsBase;
import com.google.devtools.common.options.OptionsParser;
import com.google.devtools.common.options.OptionsParsingResult;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests {@link BlazeCommandDispatcher}.
 */
@RunWith(JUnit4.class)
public class BlazeCommandDispatcherTest {

  private Scratch scratch = new Scratch();
  private BlazeRuntime runtime;
  private RecordingOutErr outErr = new RecordingOutErr();
  private FooCommand foo = new FooCommand();
  private BarCommand bar = new BarCommand();
  private Map<String, String> clientEnv;
  private AbruptExitException errorOnAfterCommand;

  @Before
  public final void initializeRuntime() throws Exception  {
    String productName = TestConstants.PRODUCT_NAME;
    ServerDirectories serverDirectories =
       new ServerDirectories(scratch.dir("install_base"), scratch.dir("output_base"),
           scratch.dir("user_root"));
    // no ConfiguredTargetFactory is needed for testing command dispatch
    this.runtime =
        new BlazeRuntime.Builder()
            .setFileSystem(scratch.getFileSystem())
            .setServerDirectories(serverDirectories)
            .setProductName(productName)
            .setStartupOptionsProvider(
                OptionsParser.builder().optionsClasses(BlazeServerStartupOptions.class).build())
            .addBlazeModule(
                new BlazeModule() {
                  @Override
                  public void beforeCommand(CommandEnvironment env) {
                    clientEnv = env.getClientEnv();
                  }

                  @Override
                  public void afterCommand() throws AbruptExitException {
                    if (errorOnAfterCommand != null) {
                      throw errorOnAfterCommand;
                    }
                  }

                  @Override
                  public BuildOptions getDefaultBuildOptions(BlazeRuntime runtime) {
                    return BuildOptions.of(ImmutableMap.of());
                  }
                })
            .build();

    BlazeDirectories directories =
        new BlazeDirectories(
            serverDirectories,
            scratch.dir("scratch"),
            /* defaultSystemJavabase= */ null,
            productName);
    runtime.initWorkspace(directories, /*binTools=*/null);
    errorOnAfterCommand = null;
  }

  /** Options for {@link FooCommand}. */
  public static class FooOptions extends OptionsBase {

    @Option(
      name = "success",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.NO_OP},
      defaultValue = "true"
    )
    public boolean exitStatus;

    @Option(
      name = "stdout",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.NO_OP},
      defaultValue = ""
    )
    public String stdout;

    @Option(
      name = "stderr",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.NO_OP},
      defaultValue = ""
    )
    public String stderr;
  }

  @Command(name = "foo", options = {FooOptions.class},
           shortDescription = "", help = "")
  private static class FooCommand implements BlazeCommand {

    @Override
    public BlazeCommandResult exec(CommandEnvironment env, OptionsParsingResult options) {
      FooOptions fooOptions = options.getOptions(FooOptions.class);
      env.getReporter().getOutErr().printOut(fooOptions.stdout);
      env.getReporter().getOutErr().printErr(fooOptions.stderr);
      if (fooOptions.exitStatus) {
        return BlazeCommandResult.success();
      } else {
        return BlazeCommandResult.failureDetail(
            FailureDetail.newBuilder()
                .setSpawn(Spawn.newBuilder().setCode(Spawn.Code.NON_ZERO_EXIT))
                .build());
      }
    }

    @Override
    public void editOptions(OptionsParser optionsParser) {}
  }

  @Command(name = "bar", shortDescription = "", help = "")
  private static class BarCommand implements BlazeCommand {

    @Override
    public BlazeCommandResult exec(CommandEnvironment env, OptionsParsingResult options) {
      env.getReporter().getOutErr().printOut("Hello, bar.\n");
      return BlazeCommandResult.success();
    }

    @Override
    public void editOptions(OptionsParser optionsParser) {}
  }

  private abstract static class AnsiTestingCommand implements BlazeCommand {
    public static final String ANSI_CODE = "\u001B[34mFoo";

    @Override
    public BlazeCommandResult exec(CommandEnvironment env, OptionsParsingResult options) {
      OutErr outErr = env.getReporter().getOutErr();
      try {
        env.getReporter().switchToAnsiAllowingHandler();
        byte[] ansiBytes = ANSI_CODE.getBytes(US_ASCII);
        env.getReporter().handle(Event.of(EventKind.STDOUT, null, ansiBytes));
        outErr.getOutputStream().flush();
        outErr.getErrorStream().flush();
      } catch (IOException e) {
        return BlazeCommandResult.failureDetail(
            FailureDetail.newBuilder().setCrash(Crash.getDefaultInstance()).build());
      }

      return BlazeCommandResult.success();
    }

    @Override
    public void editOptions(OptionsParser optionsParser) {}
  }

  @Command(name = "binary", binaryStdOut = true, shortDescription = "", help = "")
  private static class BinaryCommand extends AnsiTestingCommand {
    // Same logic as AsciiCommand, but binary.
  }

  @Command(name = "ascii", binaryStdOut = false, shortDescription = "", help = "")
  private static class AsciiCommand extends AnsiTestingCommand {
    // Same logic as BinaryCommand, but not binary.
  }

  @Test
  public void testOutErrorAndExitStatus() throws Exception {
    runtime.overrideCommands(ImmutableList.of(foo));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);
    String[] args = {"foo", "--stdout=Hello, out.",
                     "--stderr=Hello, err.", "--success=false"};
    BlazeCommandResult result = dispatch.exec(Arrays.asList(args), "test", outErr);
    assertThat(outErr.outAsLatin1()).isEqualTo("Hello, out.");
    assertThat(outErr.errAsLatin1()).isEqualTo("Hello, err.");
    assertThat(result.getExitCode()).isEqualTo(ExitCode.BUILD_FAILURE);
  }

  @Test
  public void testExecReportsHardCrashStatus() throws Exception {
    CommandCompleteRecordingCommand crashCommand =
        new CommandCompleteRecordingCommand(
            () -> {
              throw new OutOfMemoryError("oom message");
            });
    runtime.overrideCommands(ImmutableList.of(crashCommand));
    // Mocked bug reporter to avoid hard crash.
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime, mock(BugReporter.class));

    BlazeCommandResult directResult =
        dispatch.exec(ImmutableList.of("testcommand"), "clientdesc", outErr);

    CommandCompleteEvent commandCompleteEvent =
        crashCommand.commandCompleteEvent.get(TestUtils.WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    DetailedExitCode exitCode = commandCompleteEvent.getExitCode();
    assertThat(exitCode.getExitCode()).isEqualTo(ExitCode.OOM_ERROR);
    assertThat(exitCode.getFailureDetail()).isNotNull();
    FailureDetails.Crash crash = exitCode.getFailureDetail().getCrash();
    assertThat(crash.getCode()).isEqualTo(FailureDetails.Crash.Code.CRASH_OOM);
    assertThat(crash.getCausesCount()).isEqualTo(1);
    assertThat(crash.getCauses(0).getMessage()).isEqualTo("oom message");
    assertThat(crash.getCauses(0).getStackTrace(0)).contains("BlazeCommandDispatcherTest.java");
    assertThat(directResult.getExitCode()).isEqualTo(ExitCode.OOM_ERROR);
    assertThat(directResult.shutdown()).isTrue();
  }

  @Test
  public void testExecReportsStatus() throws Exception {
    FailureDetail failureDetail =
        FailureDetail.newBuilder()
            .setSpawn(Spawn.newBuilder().setCode(Spawn.Code.NON_ZERO_EXIT))
            .build();
    CommandCompleteRecordingCommand crashCommand =
        new CommandCompleteRecordingCommand(() -> BlazeCommandResult.failureDetail(failureDetail));
    runtime.overrideCommands(ImmutableList.of(crashCommand));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);

    BlazeCommandResult directResult =
        dispatch.exec(ImmutableList.of("testcommand"), "clientdesc", outErr);

    CommandCompleteEvent commandCompleteEvent =
        crashCommand.commandCompleteEvent.get(TestUtils.WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(commandCompleteEvent.getExitCode()).isEqualTo(DetailedExitCode.of(failureDetail));
    assertThat(directResult.shutdown()).isFalse();
  }

  @Test
  public void testClientEnv() throws Exception {
    runtime.overrideCommands(ImmutableList.of(foo));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);
    String[] args = {"foo", "--client_env=V1=val1", "--client_env=V2=",  "--client_env=V3=val3"};
    dispatch.exec(Arrays.asList(args), "test", outErr);
    assertThat(clientEnv).containsExactly("V1", "val1", "V2", "", "V3", "val3");
  }

  @Test
  public void testClientEnvEmpty() throws Exception {
    runtime.overrideCommands(ImmutableList.of(foo));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);
    String[] args = {"foo"};
    dispatch.exec(Arrays.asList(args), "test", outErr);
    assertThat(clientEnv).isEmpty();
  }

  @Test
  public void testAfterCommandCanModifyExitStatus() throws Exception {
    DetailedExitCode detailedExitCode =
        DetailedExitCode.of(
            FailureDetail.newBuilder()
                .setMessage("afterCommandError")
                .setBuildProgress(
                    BuildProgress.newBuilder().setCode(Code.BES_UPLOAD_LOCAL_FILE_ERROR))
                .build());
    errorOnAfterCommand = new AbruptExitException(detailedExitCode);
    runtime.overrideCommands(ImmutableList.of(foo));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);
    BlazeCommandResult result =
        dispatch.exec(Arrays.asList("foo", "--success=true"), "test", outErr);
    assertThat(result.getExitCode()).isEqualTo(ExitCode.TRANSIENT_BUILD_EVENT_SERVICE_UPLOAD_ERROR);
    assertThat(result.getDetailedExitCode()).isEqualTo(detailedExitCode);
    assertThat(outErr.errAsLatin1()).contains("afterCommandError");
  }

  @Test
  public void testMultipleCommands() throws Exception {
    runtime.overrideCommands(ImmutableList.of(foo, bar));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);

    dispatch.exec(asList("foo", "--stdout=Hello, foo."), "test", outErr);
    assertThat(outErr.outAsLatin1()).isEqualTo("Hello, foo.");
    outErr.reset();
    dispatch.exec(asList("bar"), "test", outErr);
    assertThat(outErr.outAsLatin1()).isEqualTo("Hello, bar.\n");
  }

  @Command(name = "block", help = "", shortDescription = "")
  private static class BlockCommand implements BlazeCommand {
    private final CountDownLatch waitLatch = new CountDownLatch(1);
    private final CountDownLatch started = new CountDownLatch(1);

    void unblock() {
      waitLatch.countDown();
    }

    void awaitRunning() throws InterruptedException {
      started.await();
    }

    @Override
    public BlazeCommandResult exec(CommandEnvironment env, OptionsParsingResult options) {
      started.countDown();
      try {
        waitLatch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Should not have been interrupted");
      }
      return BlazeCommandResult.success();
    }
  }

  @Test
  public void testConcurrentCommandsWaitForLock() throws Exception {
    BlockCommand blockCommand = new BlockCommand();
    runtime.overrideCommands(ImmutableList.of(bar, blockCommand));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime, /*serverPid=*/ 42);

    Thread blockCommandThread =
        new TestThread(
            () ->
                dispatch.exec(ImmutableList.of("block"), "blocking client", new RecordingOutErr()));
    TestThread blockedCommandThread =
        new TestThread(
            () ->
                dispatch.exec(
                    InvocationPolicy.getDefaultInstance(),
                    ImmutableList.of("bar"),
                    outErr,
                    LockingMode.WAIT,
                    "test client",
                    runtime.getClock().currentTimeMillis(),
                    /*startupOptionsTaggedWithBazelRc=*/ Optional.empty()));

    try {
      blockCommandThread.start();
      blockCommand.awaitRunning();
      blockedCommandThread.start();

      while (!outErr.errAsLatin1().contains("Another command")) {
        Thread.sleep(100);
      }
      assertThat(outErr.errAsLatin1())
          .contains(
              "Another command (blocking client) is running. Waiting for it to complete on the"
                  + " server (server_pid=42)...");
    } finally {
      blockCommand.unblock();
      // We don't care what happened on the threads, don't assert state to make sure we join both.
      blockCommandThread.join();
      blockedCommandThread.join();
    }
  }

  @Test
  public void testDetectsInvalidCommandLineOptions() throws Exception {
    runtime.overrideCommands(ImmutableList.of(foo));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);
    BlazeCommandResult result = dispatch.exec(asList("foo", "--invalid"), "test", outErr);
    assertThat(result.getExitCode()).isEqualTo(ExitCode.COMMAND_LINE_ERROR);
    assertThat(outErr.errAsLatin1()).contains("Unrecognized option: --invalid\n");
  }

  @Test
  public void testReportsCommandNotFound() throws Exception {
    runtime.overrideCommands(ImmutableList.of(foo));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);
    BlazeCommandResult result = dispatch.exec(asList("baz"), "test", outErr);
    assertThat(result.getExitCode()).isEqualTo(ExitCode.COMMAND_LINE_ERROR);
    assertThat(outErr.errAsLatin1())
        .matches("Command 'baz' not found. Try '(blaze|bazel) help'.\n");
  }

  @Test
  public void testProvidesHelpWhenNoCommandSpecified() throws Exception {
    @Command(name = "help", shortDescription = "", help = "")
    class HelpCommand implements BlazeCommand {
      @Override
      public BlazeCommandResult exec(CommandEnvironment env, OptionsParsingResult options) {
        env.getReporter().getOutErr().printOutLn("This is the help message.");
        return BlazeCommandResult.success();
      }

      @Override
      public void editOptions(OptionsParser optionsParser) {}
    }
    runtime.overrideCommands(ImmutableList.of(new HelpCommand()));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);
    BlazeCommandResult result = dispatch.exec(Collections.<String>emptyList(), "test", outErr);
    assertThat(result.getExitCode()).isEqualTo(ExitCode.SUCCESS);
    assertThat(outErr.outAsLatin1()).isEqualTo("This is the help message.\n");
  }

  @Test
  public void testOptionsDefaults() throws Exception {
    List<String> blazercOpts =
        ImmutableList.of(
            "--rc_source=/home/jrluser/.blazerc",
            "--default_override=0:foo=--stdout",
            "--default_override=0:foo=stdout",
            "--default_override=0:foo=--stderr",
            "--default_override=0:foo=stderr",
            "--announce_rc");

    runtime.overrideCommands(ImmutableList.of(foo));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);

    List<String> cmdLine = Lists.newArrayList("foo");
    cmdLine.addAll(blazercOpts);
    BlazeCommandResult result = dispatch.exec(cmdLine, "test", outErr);
    assertThat(outErr.outAsLatin1()).isEqualTo("stdout");
    // TODO(bazel-team): Fix inconsistent line breaks that make the regex match necessary.
    assertThat(outErr.errAsLatin1())
        .matches(
            "INFO: Reading rc options for 'foo' from /home/jrluser/.blazerc:\\s+"
                + "  'foo' options: --stdout stdout --stderr stderr\\s+"
                + "stderr");
    assertThat(result.getExitCode()).isEqualTo(ExitCode.SUCCESS);

    // Explicit options override those from config file:
    result = dispatch.exec(asList("foo", "--success=false"), "test", outErr);
    assertThat(result.getExitCode()).isEqualTo(ExitCode.BUILD_FAILURE);
  }

  @Test
  public void testIllegalOptions() throws Exception {
    runtime.overrideCommands(ImmutableList.of(foo));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);
    BlazeCommandResult result = dispatch.exec(
        asList("foo", "--not_a_valid_option"), "test", outErr);
    assertThat(result.getExitCode()).isEqualTo(ExitCode.COMMAND_LINE_ERROR);
  }

  @Command(name = "wiz", inherits = {FooCommand.class}, shortDescription = "", help = "")
  private static class WizCommand extends FooCommand {}

  @Test
  public void testInheritanceOfOptionDefaults() throws Exception {
    // "foo" options in ~/.blazerc should apply to "wiz" too...
    List<String> blazercOpts =
        ImmutableList.of(
            "--rc_source=/home/jrluser/.blazerc",
            "--default_override=0:foo=--stdout",
            "--default_override=0:foo=stdout",
            "--default_override=0:foo=--stderr",
            "--default_override=0:foo=stderr",
            "--announce_rc");
    runtime.overrideCommands(ImmutableList.of(foo, new WizCommand()));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);

    List<String> cmdLine = Lists.newArrayList("wiz");
    cmdLine.addAll(blazercOpts);
    dispatch.exec(cmdLine, "test", outErr);
    assertThat(outErr.outAsLatin1()).isEqualTo("stdout");
    // TODO(bazel-team): Fix inconsistent line breaks that make the regex match necessary.
    assertThat(outErr.errAsLatin1())
        .matches(
            "INFO: Reading rc options for 'wiz' from /home/jrluser/.blazerc:\\s+"
                + "  Inherited 'foo' options: --stdout stdout --stderr stderr\\s+"
                + "stderr");
  }

  @Test
  public void testBinaryCommandOutput() throws Exception {
    runtime.overrideCommands(ImmutableList.of(new BinaryCommand()));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);

    final String ansiEscapedString = AnsiTestingCommand.ANSI_CODE;

    // Binary commands do not remove ANSI control codes.
    BlazeCommandResult result = dispatch.exec(
        asList("binary", "--color=no"), "test", outErr);
    String out = outErr.outAsLatin1();
    String err = outErr.errAsLatin1();

    MoreAsserts.assertExitCode(ExitCode.SUCCESS.getNumericExitCode(),
        result.getExitCode().getNumericExitCode(), out, err);
    assertThat(out).contains(ansiEscapedString);
  }

  @Test
  public void testAsciiCommandOutput() throws Exception {
    runtime.overrideCommands(ImmutableList.of(new AsciiCommand()));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);

    final String ansiEscapedString = AnsiTestingCommand.ANSI_CODE;

    // ASCII commands remove ANSI control codes.
    BlazeCommandResult result = dispatch.exec(asList("ascii", "--color=no"),
        "test", outErr);
    String out = outErr.outAsLatin1();
    String err = outErr.errAsLatin1();

    MoreAsserts.assertExitCode(ExitCode.SUCCESS.getNumericExitCode(),
        result.getExitCode().getNumericExitCode(), out, err);
    assertThat(out).doesNotContain(ansiEscapedString);
  }

  @Test
  public void testWaitingForTimestampGranularityMonitor() throws Exception {
    runtime.overrideCommands(ImmutableList.of(foo));
    BlazeCommandDispatcher dispatch = new BlazeCommandDispatcher(runtime);

    for (int i = 0; i < 3; i++) {
      BlazeCommandResult result = dispatch.exec(Arrays.asList("foo"), "test", outErr);
      assertThat(result.getExitCode()).isEqualTo(ExitCode.SUCCESS);
    }

    assertThat(outErr.outAsLatin1()).isEmpty();
    for (String line : outErr.errAsLatin1().split("\n")) {
      assertThat(line)
          .containsMatch(
              "^|Blaze waited .* to avoid potential file system timestamp granularity issues");
    }
  }

  /**
   * Regression test for b/136003907.
   *
   * <p>Tests that even if {@link System#out} or {@link System#err} are read and retained during the
   * lifetime of a command (which we cannot prevent, since they are public), there is no memory leak
   * of {@link CommandEnvironment#getReporter}.
   */
  @Test
  public void noMemoryLeakOfReporterThroughSystemOutErr() throws Exception {
    @Command(name = "retain_out_err", shortDescription = "", help = "")
    final class SystemOutErrRetainingCommand implements BlazeCommand {
      private final PrintStream defaultStdout = System.out;
      private final PrintStream defaultStderr = System.err;
      private PrintStream overriddenStdout;
      private PrintStream overriddenStderr;
      private WeakReference<Reporter> reporterRef;

      @Override
      public BlazeCommandResult exec(CommandEnvironment env, OptionsParsingResult options) {
        overriddenStdout = System.out;
        assertThat(overriddenStdout).isNotNull();
        assertThat(overriddenStdout).isNotEqualTo(defaultStdout);

        overriddenStderr = System.err;
        assertThat(overriddenStderr).isNotNull();
        assertThat(overriddenStderr).isNotEqualTo(defaultStderr);

        Reporter reporter = env.getReporter();
        assertThat(reporter).isNotNull();
        reporterRef = new WeakReference<>(env.getReporter());

        return BlazeCommandResult.success();
      }

      @Override
      public void editOptions(OptionsParser optionsParser) {}
    }

    SystemOutErrRetainingCommand cmd = new SystemOutErrRetainingCommand();
    runtime.overrideCommands(ImmutableList.of(cmd));
    BlazeCommandDispatcher dispatcher = new BlazeCommandDispatcher(runtime);

    dispatcher.exec(ImmutableList.of("retain_out_err"), "test", outErr);

    GcFinalization.awaitClear(cmd.reporterRef);
  }

  @Command(
      name = "testcommand",
      options = {},
      shortDescription = "",
      help = "")
  private static class CommandCompleteRecordingCommand implements BlazeCommand {

    private final SettableFuture<CommandCompleteEvent> commandCompleteEvent =
        SettableFuture.create();
    private final Supplier<BlazeCommandResult> resultSupplier;

    private CommandCompleteRecordingCommand(Supplier<BlazeCommandResult> resultSupplier) {
      this.resultSupplier = resultSupplier;
    }

    @Subscribe
    public void onCommandComplete(CommandCompleteEvent commandComplete) {
      commandCompleteEvent.set(commandComplete);
    }

    @Override
    public BlazeCommandResult exec(CommandEnvironment env, OptionsParsingResult options) {
      env.getEventBus().register(this);
      return resultSupplier.get();
    }

    @Override
    public void editOptions(OptionsParser optionsParser) {
      // no-op
    }
  }
}
