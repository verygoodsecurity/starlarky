// Copyright 2014 The Bazel Authors. All rights reserved.
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

import static com.google.devtools.build.lib.exec.ExecutionOptions.TestSummaryFormat.DETAILED;
import static com.google.devtools.build.lib.exec.ExecutionOptions.TestSummaryFormat.TESTCASE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.devtools.build.lib.analysis.test.TestResult;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.exec.ExecutionOptions;
import com.google.devtools.build.lib.exec.ExecutionOptions.TestOutputFormat;
import com.google.devtools.build.lib.exec.ExecutionOptions.TestSummaryFormat;
import com.google.devtools.build.lib.exec.TestLogHelper;
import com.google.devtools.build.lib.runtime.TestSummaryPrinter.TestLogPathFormatter;
import com.google.devtools.build.lib.util.StringUtil;
import com.google.devtools.build.lib.util.io.AnsiTerminalPrinter;
import com.google.devtools.build.lib.view.test.TestStatus.BlazeTestStatus;
import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionDocumentationCategory;
import com.google.devtools.common.options.OptionEffectTag;
import com.google.devtools.common.options.OptionsBase;
import com.google.devtools.common.options.OptionsParsingResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Prints the test results to a terminal.
 */
public class TerminalTestResultNotifier implements TestResultNotifier {
  // The number of failed-to-build tests to report.
  // (We do not want to report hundreds of failed-to-build tests as it would probably be caused
  // by some intermediate target not related to tests themselves.)
  // The total number of failed-to-build tests will be reported in any case.
  @VisibleForTesting public static final int NUM_FAILED_TO_BUILD = 5;

  private static class TestResultStats {
    int numberOfTargets;
    int passCount;
    int failedToBuildCount;
    int failedCount;
    int failedRemotelyCount;
    int failedLocallyCount;
    int noStatusCount;
    int numberOfExecutedTargets;
    boolean wasUnreportedWrongSize;

    int totalTestCases;
    int totalFailedTestCases;
    int totalUnknownTestCases;
  }

  /**
   * Flags specific to test summary reporting.
   */
  public static class TestSummaryOptions extends OptionsBase {
    @Option(
      name = "verbose_test_summary",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.LOGGING,
      effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
      help =
          "If true, print additional information (timing, number of failed runs, etc) in the"
              + " test summary."
    )
    public boolean verboseSummary;

    @Option(
      name = "test_verbose_timeout_warnings",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.LOGGING,
      effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
      help =
          "If true, print additional warnings when the actual test execution time does not "
              + "match the timeout defined by the test (whether implied or explicit)."
    )
    public boolean testVerboseTimeoutWarnings;

    @Option(
        name = "print_relative_test_log_paths",
        defaultValue = "false",
        documentationCategory = OptionDocumentationCategory.LOGGING,
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help =
            "If true, when printing the path to a test log, use relative path that makes use of "
                + "the 'testlogs' convenience symlink. N.B. - A subsequent 'build'/'test'/etc "
                + "invocation with a different configuration can cause the target of this symlink "
                + "to change, making the path printed previously no longer useful."
    )
    public boolean printRelativeTestLogPaths;
  }

  private final AnsiTerminalPrinter printer;
  private final TestLogPathFormatter testLogPathFormatter;
  private final OptionsParsingResult options;
  private final TestSummaryOptions summaryOptions;

  /**
   * @param printer The terminal to print to
   */
  public TerminalTestResultNotifier(
      AnsiTerminalPrinter printer,
      TestLogPathFormatter testLogPathFormatter,
      OptionsParsingResult options) {
    this.printer = printer;
    this.testLogPathFormatter = testLogPathFormatter;
    this.options = options;
    this.summaryOptions = options.getOptions(TestSummaryOptions.class);
  }

  /**
   * Decide if two tests with the same label are contained in the set of test summaries
   */
  private boolean duplicateLabels(Set<TestSummary> summaries) {
    Set<Label> labelsSeen = new HashSet<>();
    for (TestSummary summary : summaries) {
      if (labelsSeen.contains(summary.getLabel())) {
        return true;
      }
      labelsSeen.add(summary.getLabel());
    }
    return false;
  }

  /**
   * Prints test result summary.
   *
   * @param summaries summaries of tests {@link TestSummary}
   * @param showAllTests if true, print information about each test regardless of its status
   * @param showNoStatusTests if true, print information about not executed tests (no status tests)
   * @param printFailedTestCases if true, print details about which test cases in a test failed
   */
  private void printSummary(
      Set<TestSummary> summaries,
      boolean showAllTests,
      boolean showNoStatusTests,
      boolean printFailedTestCases) {
    boolean withConfig = duplicateLabels(summaries);
    int numFailedToBuildReported = 0;
    for (TestSummary summary : summaries) {
      if (!showAllTests
          && (BlazeTestStatus.PASSED == summary.getStatus()
              || (!showNoStatusTests && BlazeTestStatus.NO_STATUS == summary.getStatus()))) {
        continue;
      }
      if (BlazeTestStatus.FAILED_TO_BUILD == summary.getStatus()) {
        if (numFailedToBuildReported == NUM_FAILED_TO_BUILD) {
          printer.printLn("(Skipping other failed to build tests)");
        }
        numFailedToBuildReported++;
        if (numFailedToBuildReported > NUM_FAILED_TO_BUILD) {
          continue;
        }
      }
      TestSummaryPrinter.print(
          summary,
          printer,
          testLogPathFormatter,
          summaryOptions.verboseSummary,
          printFailedTestCases,
          withConfig);
    }
  }

  /**
   * Returns true iff the --check_tests_up_to_date option is enabled.
   */
  private boolean optionCheckTestsUpToDate() {
    return options.getOptions(ExecutionOptions.class).testCheckUpToDate;
  }


  /**
   * Prints a test summary information for all tests to the terminal.
   *
   * @param summaries Summary of all targets that were ran
   * @param numberOfExecutedTargets the number of targets that were actually ran
   */
  @Override
  public void notify(Set<TestSummary> summaries, int numberOfExecutedTargets) {
    TestResultStats stats = new TestResultStats();
    stats.numberOfTargets = summaries.size();
    stats.numberOfExecutedTargets = numberOfExecutedTargets;

    ExecutionOptions executionOptions =
        Preconditions.checkNotNull(options.getOptions(ExecutionOptions.class));
    TestOutputFormat testOutput = executionOptions.testOutput;

    for (TestSummary summary : summaries) {
      if (summary.isLocalActionCached()
          && TestLogHelper.shouldOutputTestLog(testOutput,
              TestResult.isBlazeTestStatusPassed(summary.getStatus()))) {
        TestSummaryPrinter.printCachedOutput(
            summary,
            testOutput,
            printer,
            testLogPathFormatter,
            executionOptions.maxTestOutputBytes);
      }
    }

    for (TestSummary summary : summaries) {
      if (TestResult.isBlazeTestStatusPassed(summary.getStatus())) {
        stats.passCount++;
      } else if (summary.getStatus() == BlazeTestStatus.NO_STATUS) {
        stats.noStatusCount++;
      } else if (summary.getStatus() == BlazeTestStatus.FAILED_TO_BUILD) {
        stats.failedToBuildCount++;
      } else if (summary.ranRemotely()) {
        stats.failedRemotelyCount++;
      } else {
        stats.failedLocallyCount++;
      }

      if (summary.wasUnreportedWrongSize()) {
        stats.wasUnreportedWrongSize = true;
      }

      stats.totalTestCases += summary.getTotalTestCases();
      stats.totalUnknownTestCases += summary.getUnkownTestCases();
      stats.totalFailedTestCases += summary.getFailedTestCases().size();
    }

    stats.failedCount = summaries.size() - stats.passCount;

    TestSummaryFormat testSummaryFormat = executionOptions.testSummary;
    switch (testSummaryFormat) {
      case DETAILED:
        printSummary(
            summaries,
            /* showAllTests= */ false,
            /* showNoStatusTests= */ true,
            /* printFailedTestCases= */ true);
        break;

      case SHORT:
        printSummary(
            summaries,
            /* showAllTests= */ true,
            /* showNoStatusTests= */ false,
            /* printFailedTestCases= */ false);
        break;

      case TERSE:
        printSummary(
            summaries,
            /* showAllTests= */ false,
            /* showNoStatusTests= */ false,
            /* printFailedTestCases= */ false);
        break;

      case TESTCASE:
      case NONE:
        break;
    }

    printStats(stats);
  }

  private void addFailureToErrorList(List<String> list, String failureDescription, int count) {
    addToErrorList(list, "fails", "fail", failureDescription, count);
  }

  private void addToErrorList(
      List<String> list, String singularPrefix, String pluralPrefix, String message, int count) {
    if (count > 0) {
      list.add(String.format("%s%d %s %s%s", AnsiTerminalPrinter.Mode.ERROR, count,
          count == 1 ? singularPrefix : pluralPrefix, message, AnsiTerminalPrinter.Mode.DEFAULT));
    }
  }

  private void printStats(TestResultStats stats) {
    TestSummaryFormat testSummaryFormat = options.getOptions(ExecutionOptions.class).testSummary;
    if (testSummaryFormat == DETAILED || testSummaryFormat == TESTCASE) {
      int passCount =
          stats.totalTestCases - stats.totalFailedTestCases - stats.totalUnknownTestCases;
      String message =
          String.format(
              "Test cases: finished with %s%d passing%s and %s%d failing%s out of %d test cases",
              passCount > 0 ? AnsiTerminalPrinter.Mode.INFO : "",
              passCount,
              AnsiTerminalPrinter.Mode.DEFAULT,
              stats.totalFailedTestCases > 0 ? AnsiTerminalPrinter.Mode.ERROR : "",
              stats.totalFailedTestCases,
              AnsiTerminalPrinter.Mode.DEFAULT,
              stats.totalTestCases);
      if (stats.totalUnknownTestCases != 0) {
        // It is possible for a target to fail even if all of its test cases pass. To avoid
        // confusion, we append the following disclaimer.
        message += " (some targets did not have test case information)";
      }
      printer.printLn(message);
    }

    if (!optionCheckTestsUpToDate()) {
      List<String> results = new ArrayList<>();
      if (stats.passCount == 1) {
        results.add(stats.passCount + " test passes");
      } else if (stats.passCount > 0) {
        results.add(stats.passCount + " tests pass");
      }
      addFailureToErrorList(results, "to build", stats.failedToBuildCount);
      addFailureToErrorList(results, "locally", stats.failedLocallyCount);
      addFailureToErrorList(results, "remotely", stats.failedRemotelyCount);
      addToErrorList(results, "was", "were", "skipped", stats.noStatusCount);
      printer.print(String.format("\nExecuted %d out of %d %s: %s.\n",
              stats.numberOfExecutedTargets,
              stats.numberOfTargets,
              stats.numberOfTargets == 1 ? "test" : "tests",
              StringUtil.joinEnglishList(results, "and")));
    } else {
      int failingUpToDateCount = stats.failedCount - stats.noStatusCount;
      printer.print(String.format(
          "\nFinished with %d passing and %s%d failing%s tests up to date, %s%d out of date.%s\n",
          stats.passCount,
          failingUpToDateCount > 0 ? AnsiTerminalPrinter.Mode.ERROR : "",
          failingUpToDateCount,
          AnsiTerminalPrinter.Mode.DEFAULT,
          stats.noStatusCount > 0 ? AnsiTerminalPrinter.Mode.ERROR : "",
          stats.noStatusCount,
          AnsiTerminalPrinter.Mode.DEFAULT));
    }

    if (stats.wasUnreportedWrongSize) {
       printer.print("There were tests whose specified size is too big. Use the "
           + "--test_verbose_timeout_warnings command line option to see which "
           + "ones these are.\n");
     }
  }
}
