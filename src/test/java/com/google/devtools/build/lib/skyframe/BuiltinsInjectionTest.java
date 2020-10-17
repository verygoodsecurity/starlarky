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

package com.google.devtools.build.lib.skyframe;

import static com.google.common.truth.Truth.assertThat;

import com.google.devtools.build.lib.analysis.ConfiguredRuleClassProvider;
import com.google.devtools.build.lib.analysis.util.BuildViewTestCase;
import com.google.devtools.build.lib.analysis.util.MockRule;
import com.google.devtools.build.lib.testutil.TestRuleClassProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for Starlark builtin injection.
 *
 * <p>Essentially these are integration tests between {@link StarlarkBuiltinsFunction} and {@link
 * BzlLoadFunction}.
 */
@RunWith(JUnit4.class)
public class BuiltinsInjectionTest extends BuildViewTestCase {

  private static final MockRule OVERRIDABLE_RULE = () -> MockRule.define("overridable_rule");

  @Override
  protected ConfiguredRuleClassProvider createRuleClassProvider() {
    // Add a fake rule and top-level symbol to override.
    ConfiguredRuleClassProvider.Builder builder =
        new ConfiguredRuleClassProvider.Builder()
            .addRuleDefinition(OVERRIDABLE_RULE)
            .addStarlarkAccessibleTopLevels("overridable_symbol", "original_value");
    TestRuleClassProvider.addStandardRules(builder);
    return builder.build();
  }

  @Before
  public void setUp() throws Exception {
    setBuildLanguageOptions("--experimental_builtins_bzl_path=notdisabled");
  }

  /**
   * Writes an exports.bzl file with the given content, along with a BUILD file, in the builtins
   * location.
   *
   * <p>See {@link StarlarkBuiltinsFunction#EXPORTS_ENTRYPOINT} for the significance of exports.bzl.
   */
  // TODO(#11437): Don't write the BUILD file once it's no longer needed. Pass staging location into
  // test setup above.
  private void writeExportsBzl(String... lines) throws Exception {
    scratch.file("tools/builtins_staging/BUILD");
    scratch.file("tools/builtins_staging/exports.bzl", lines);
  }

  /**
   * Writes a pkg/dummy.bzl file, and an accompanying BUILD file that loads it and defines a
   * //pkg:dummy target.
   */
  private void writePkgBzl(String... lines) throws Exception {
    scratch.file(
        "pkg/BUILD",
        "load(':dummy.bzl', 'dummy_symbol')",
        // We define :dummy as a simple file rather than something like a java_library, because
        // native rules may depend on exports.bzl, which we are messing with in this test suite.
        "exports_files(['dummy'])");
    scratch.file("pkg/dummy");
    List<String> modifiedLines = new ArrayList<>(Arrays.asList(lines));
    modifiedLines.add(0, "dummy_symbol = None");
    scratch.file("pkg/dummy.bzl", modifiedLines.toArray(lines));
  }

  /**
   * Builds {@code //pkg:dummy} and asserts success.
   *
   * <p>This helps us fail fast when {@link #getConfiguredTarget} returns null without emitting
   * events; see b/26382502.
   */
  private void buildDummyAndAssertSuccess() throws Exception {
    Object result = getConfiguredTarget("//pkg:dummy");
    assertThat(result).isNotNull();
  }

  /** Builds {@code //pkg:dummy}, which may be in error. */
  private void buildDummyWithoutAssertingSuccess() throws Exception {
    getConfiguredTarget("//pkg:dummy");
  }

  @Test
  public void evalWithInjection() throws Exception {
    writeExportsBzl(
        "exported_toplevels = {'overridable_symbol': 'new_value'}",
        "exported_rules = {'overridable_rule': 'new_rule'}",
        "exported_to_java = {}");
    writePkgBzl(
        "print('overridable_symbol :: ' + str(overridable_symbol))",
        "print('overridable_rule :: ' + str(native.overridable_rule))");

    buildDummyAndAssertSuccess();
    assertContainsEvent("overridable_symbol :: new_value");
    assertContainsEvent("overridable_rule :: new_rule");
  }

  @Test
  public void errorInEvaluatingBuiltins() throws Exception {
    // Test case with a Starlark error in the @builtins pseudo-repo itself.
    // TODO(#11437): Use @builtins//:... syntax for load, once supported.
    scratch.file(
        "tools/builtins_staging/helper.bzl", //
        "toplevels = {'overridable_symbol': 1//0}  # <-- dynamic error");
    writeExportsBzl(
        "load('//tools/builtins_staging:helper.bzl', 'toplevels')",
        "exported_toplevels = toplevels",
        "exported_rules = {}",
        "exported_to_java = {}");
    writePkgBzl("print('evaluation completed')");
    reporter.removeHandler(failFastHandler);

    buildDummyWithoutAssertingSuccess();
    assertContainsEvent(
        "File \"/workspace/tools/builtins_staging/helper.bzl\", line 1, column 37, in <toplevel>");
    assertContainsEvent("Error: integer division by zero");
    assertContainsEvent(
        "error loading package 'pkg': Internal error while loading Starlark builtins for "
            + "//pkg:dummy.bzl: Failed to load builtins sources: in "
            + "/workspace/tools/builtins_staging/exports.bzl: Extension file "
            + "'tools/builtins_staging/helper.bzl' has errors");
    assertDoesNotContainEvent("evaluation completed");
  }

  @Test
  public void errorInProcessingExports() throws Exception {
    // Test case with an error in the symbols exported by exports.bzl, but no actual Starlark errors
    // in the @builtins files themselves.
    writeExportsBzl(
        "exported_toplevels = None", // should be dict
        "exported_rules = {}",
        "exported_to_java = {}");
    writePkgBzl("print('evaluation completed')");
    reporter.removeHandler(failFastHandler);

    buildDummyWithoutAssertingSuccess();
    assertContainsEvent(
        "error loading package 'pkg': Internal error while loading Starlark builtins for "
            + "//pkg:dummy.bzl: Failed to apply declared builtins: got NoneType for "
            + "'exported_toplevels dict', want dict");
    assertDoesNotContainEvent("evaluation completed");
  }

  // TODO(#11437): Remove once disabling is not allowed.
  @Test
  public void injectionDisabledByFlag() throws Exception {
    writeExportsBzl(
        "exported_toplevels = {'overridable_symbol': 'new_value'}",
        "exported_rules = {'overridable_rule': 'new_rule'}",
        "exported_to_java = {}");
    writePkgBzl(
        "print('overridable_symbol :: ' + str(overridable_symbol))",
        "print('overridable_rule :: ' + str(native.overridable_rule))");
    setBuildLanguageOptions("--experimental_builtins_bzl_path=");

    buildDummyAndAssertSuccess();
    assertContainsEvent("overridable_symbol :: original_value");
    assertContainsEvent("overridable_rule :: <built-in rule overridable_rule>");
  }

  // TODO(#11437): Remove once disabling is not allowed.
  @Test
  public void exportsBzlMayBeInErrorWhenInjectionIsDisabled() throws Exception {
    writeExportsBzl( //
        "PARSE ERROR");
    writePkgBzl(
        "print('overridable_symbol :: ' + str(overridable_symbol))",
        "print('overridable_rule :: ' + str(native.overridable_rule))");
    setBuildLanguageOptions("--experimental_builtins_bzl_path=");

    buildDummyAndAssertSuccess();
    assertContainsEvent("overridable_symbol :: original_value");
    assertContainsEvent("overridable_rule :: <built-in rule overridable_rule>");
  }

  // TODO(#11954): Once WORKSPACE- and BUILD-loaded bzls use the exact same environments, we'll want
  // to apply injection to both. This is for uniformity, not because we actually care about builtins
  // injection for WORKSPACE bzls. In the meantime, assert the status quo: WORKSPACE bzls do not use
  // injection.
  @Test
  public void workspaceBzlDoesNotUseInjection() throws Exception {
    writeExportsBzl(
        "exported_toplevels = {'overridable_symbol': 'new_value'}",
        "exported_rules = {'overridable_rule': 'new_rule'}",
        "exported_to_java = {}");
    writePkgBzl();
    scratch.appendFile(
        "WORKSPACE", //
        "load(':foo.bzl', 'dummy_symbol')",
        "print(dummy_symbol)");
    scratch.file("BUILD");
    scratch.file(
        "foo.bzl",
        "dummy_symbol = None",
        "print('overridable_symbol :: ' + str(overridable_symbol))");

    buildDummyAndAssertSuccess();
    // Builtins for WORKSPACE bzls are populated essentially the same as for BUILD bzls, except that
    // injection doesn't apply.
    assertContainsEvent("overridable_symbol :: original_value");
    // We don't assert that the rule isn't injected because the workspace native object doesn't
    // contain our original mock rule. We can test this for WORKSPACE files at the top-level though.
  }

  @Test
  public void builtinsBzlCannotAccessNative() throws Exception {
    // TODO(#11437): Use @builtins//:... syntax for load, once supported.
    scratch.file(
        "tools/builtins_staging/helper.bzl", //
        "builtins_dummy = None",
        "print('made it to evaluation')",
        "print('overridable_rule :: ' + str(native.overridable_rule))");
    writeExportsBzl(
        "load('//tools/builtins_staging:helper.bzl', 'builtins_dummy')",
        "exported_toplevels = {}",
        "exported_rules = {}",
        "exported_to_java = {}");
    writePkgBzl();
    reporter.removeHandler(failFastHandler);

    buildDummyWithoutAssertingSuccess();
    // The print() statement is never reached because we fail statically during name resolution in
    // BzlCompileFunction.
    assertDoesNotContainEvent("made it to evaluation");
    assertContainsEvent("name 'native' is not defined");
  }

  @Test
  public void builtinsBzlCannotAccessRuleSpecificSymbol() throws Exception {
    // TODO(#11437): Use @builtins//:... syntax for load, once supported.
    scratch.file(
        "tools/builtins_staging/helper.bzl", //
        "builtins_dummy = None",
        "print('made it to evaluation')",
        "print('overridable_symbol :: ' + str(overridable_symbol))");
    writeExportsBzl(
        "load('//tools/builtins_staging:helper.bzl', 'builtins_dummy')",
        "exported_toplevels = {}",
        "exported_rules = {}",
        "exported_to_java = {}");
    writePkgBzl();
    reporter.removeHandler(failFastHandler);

    buildDummyWithoutAssertingSuccess();
    // The print() statement is never reached because we fail statically during name resolution in
    // BzlCompileFunction.
    assertDoesNotContainEvent("made it to evaluation");
    assertContainsEvent("name 'overridable_symbol' is not defined");
  }

  @Test
  public void regularBzlCannotAccessInternal() throws Exception {
    writeExportsBzl(
        "exported_toplevels = {}", //
        "exported_rules = {}",
        "exported_to_java = {}");
    writePkgBzl("print('_internal :: ' + str(_internal))");
    reporter.removeHandler(failFastHandler);

    buildDummyWithoutAssertingSuccess();
    assertContainsEvent("name '_internal' is not defined");
  }

  @Test
  public void builtinsBzlCanAccessInternal() throws Exception {
    // TODO(#11437): Use @builtins//:... syntax for load, once supported.
    scratch.file(
        "tools/builtins_staging/helper.bzl", //
        "builtins_dummy = None",
        "print('_internal in helper.bzl :: ' + str(_internal))");
    writeExportsBzl(
        "load('//tools/builtins_staging:helper.bzl', 'builtins_dummy')",
        "exported_toplevels = {}",
        "exported_rules = {}",
        "exported_to_java = {}",
        "print('_internal in exports.bzl :: ' + str(_internal))");
    writePkgBzl();

    buildDummyAndAssertSuccess();
    assertContainsEvent("_internal in helper.bzl :: <_internal module>");
    assertContainsEvent("_internal in exports.bzl :: <_internal module>");
  }

  // TODO(#11437): Reject overriding symbols that don't already exist. Reject overriding "native".
  // Reject overriding non-rule-logic symbols such as package(), select(), environment_extension,
  // varref(), etc.

  // TODO(#11437): Add tests of the _internal symbol's usage within builtins bzls.

  // TODO(#11437): Add test cases for BUILD file injection, and WORKSPACE file non-injection, when
  // we add injection support to PackageFunction.

  /**
   * Tests for injection, under inlining of {@link BzlLoadFunction}.
   *
   * <p>See {@link BzlLoadFunction#computeInline} for an explanation of inlining.
   */
  @RunWith(JUnit4.class)
  public static class BuiltinsInjectionTestWithInlining extends BuiltinsInjectionTest {

    @Override
    protected boolean usesInliningBzlLoadFunction() {
      return true;
    }
  }
}
