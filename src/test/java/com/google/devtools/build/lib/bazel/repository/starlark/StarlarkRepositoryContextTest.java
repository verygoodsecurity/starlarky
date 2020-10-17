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

package com.google.devtools.build.lib.bazel.repository.starlark;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.io.CharStreams;
import com.google.devtools.build.lib.bazel.repository.downloader.DownloadManager;
import com.google.devtools.build.lib.events.ExtendedEventHandler;
import com.google.devtools.build.lib.packages.Attribute;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.Package.Builder.DefaultPackageSettings;
import com.google.devtools.build.lib.packages.Rule;
import com.google.devtools.build.lib.packages.RuleClass;
import com.google.devtools.build.lib.packages.RuleClass.Builder.RuleClassType;
import com.google.devtools.build.lib.packages.Type;
import com.google.devtools.build.lib.packages.WorkspaceFactoryHelper;
import com.google.devtools.build.lib.packages.semantics.BuildLanguageOptions;
import com.google.devtools.build.lib.pkgcache.PathPackageLocator;
import com.google.devtools.build.lib.rules.repository.RepositoryFunction.RepositoryFunctionException;
import com.google.devtools.build.lib.runtime.RepositoryRemoteExecutor;
import com.google.devtools.build.lib.runtime.RepositoryRemoteExecutor.ExecutionResult;
import com.google.devtools.build.lib.skyframe.BazelSkyframeExecutorConstants;
import com.google.devtools.build.lib.testutil.Scratch;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.lib.vfs.RootedPath;
import com.google.devtools.build.skyframe.SkyFunction;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkFunction;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.Location;
import net.starlark.java.syntax.ParserInput;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/** Unit tests for complex function of StarlarkRepositoryContext. */
@RunWith(JUnit4.class)
public final class StarlarkRepositoryContextTest {

  private Scratch scratch;
  private Path outputDirectory;
  private Root root;
  private Path workspaceFile;
  private StarlarkRepositoryContext context;
  private static final StarlarkThread thread =
      new StarlarkThread(Mutability.create("test"), StarlarkSemantics.DEFAULT);

  private static String ONE_LINE_PATCH = "@@ -1,1 +1,2 @@\n line one\n+line two\n";

  @Before
  public void setUp() throws Exception {
    scratch = new Scratch("/");
    outputDirectory = scratch.dir("/outputDir");
    root = Root.fromPath(scratch.dir("/wsRoot"));
    workspaceFile = scratch.file("/wsRoot/WORKSPACE");
  }

  protected static RuleClass buildRuleClass(Attribute... attributes) {
    RuleClass.Builder ruleClassBuilder =
        new RuleClass.Builder("test", RuleClassType.WORKSPACE, true);
    for (Attribute attr : attributes) {
      ruleClassBuilder.addAttribute(attr);
    }
    ruleClassBuilder.setWorkspaceOnly();
    ruleClassBuilder.setConfiguredTargetFunction(
        (StarlarkFunction) exec("def test(ctx): pass", "test"));
    return ruleClassBuilder.build();
  }

  private static Object exec(String... lines) {
    try {
      return Starlark.execFile(
          ParserInput.fromLines(lines), FileOptions.DEFAULT, Module.create(), thread);
    } catch (Exception ex) { // SyntaxError | EvalException | InterruptedException
      throw new AssertionError("exec failed", ex);
    }
  }

  private static final ImmutableList<StarlarkThread.CallStackEntry> DUMMY_STACK =
      ImmutableList.of(
          new StarlarkThread.CallStackEntry( //
              "<toplevel>", Location.fromFileLineColumn("BUILD", 10, 1)),
          new StarlarkThread.CallStackEntry( //
              "foo", Location.fromFileLineColumn("foo.bzl", 42, 1)),
          new StarlarkThread.CallStackEntry( //
              "myrule", Location.fromFileLineColumn("bar.bzl", 30, 6)));

  protected void setUpContextForRule(
      Map<String, Object> kwargs,
      ImmutableSet<PathFragment> ignoredPathFragments,
      StarlarkSemantics starlarkSemantics,
      @Nullable RepositoryRemoteExecutor repoRemoteExecutor,
      Attribute... attributes)
      throws Exception {
    Package.Builder packageBuilder =
        Package.newExternalPackageBuilder(
            DefaultPackageSettings.INSTANCE,
            RootedPath.toRootedPath(root, workspaceFile),
            "runfiles",
            starlarkSemantics);
    ExtendedEventHandler listener = Mockito.mock(ExtendedEventHandler.class);
    Rule rule =
        WorkspaceFactoryHelper.createAndAddRepositoryRule(
            packageBuilder,
            buildRuleClass(attributes),
            null,
            kwargs,
            starlarkSemantics,
            DUMMY_STACK);
    DownloadManager downloader = Mockito.mock(DownloadManager.class);
    SkyFunction.Environment environment = Mockito.mock(SkyFunction.Environment.class);
    when(environment.getListener()).thenReturn(listener);
    PathPackageLocator packageLocator =
        new PathPackageLocator(
            outputDirectory,
            ImmutableList.of(root),
            BazelSkyframeExecutorConstants.BUILD_FILES_BY_PRIORITY);
    context =
        new StarlarkRepositoryContext(
            rule,
            packageLocator,
            outputDirectory,
            ignoredPathFragments,
            environment,
            ImmutableMap.of("FOO", "BAR"),
            downloader,
            1.0,
            /*processWrapper=*/ null,
            new HashMap<>(),
            starlarkSemantics,
            repoRemoteExecutor);
  }

  protected void setUpContexForRule(String name) throws Exception {
    setUpContextForRule(
        ImmutableMap.of("name", name),
        ImmutableSet.of(),
        StarlarkSemantics.DEFAULT,
        /* repoRemoteExecutor= */ null);
  }

  @Test
  public void testAttr() throws Exception {
    setUpContextForRule(
        ImmutableMap.of("name", "test", "foo", "bar"),
        ImmutableSet.of(),
        StarlarkSemantics.DEFAULT,
        /* repoRemoteExecutor= */ null,
        Attribute.attr("foo", Type.STRING).build());

    assertThat(context.getAttr().getFieldNames()).contains("foo");
    assertThat(context.getAttr().getValue("foo")).isEqualTo("bar");
  }

  @Test
  public void testWhich() throws Exception {
    setUpContexForRule("test");
    StarlarkRepositoryContext.setPathEnvironment("/bin", "/path/sbin", ".");
    scratch.file("/bin/true").setExecutable(true);
    scratch.file("/path/sbin/true").setExecutable(true);
    scratch.file("/path/sbin/false").setExecutable(true);
    scratch.file("/path/bin/undef").setExecutable(true);
    scratch.file("/path/bin/def").setExecutable(true);
    scratch.file("/bin/undef");

    assertThat(context.which("anything", thread)).isNull();
    assertThat(context.which("def", thread)).isNull();
    assertThat(context.which("undef", thread)).isNull();
    assertThat(context.which("true", thread).toString()).isEqualTo("/bin/true");
    assertThat(context.which("false", thread).toString()).isEqualTo("/path/sbin/false");
  }

  @Test
  public void testFile() throws Exception {
    setUpContexForRule("test");
    context.createFile(context.path("foobar"), "", true, true, thread);
    context.createFile(context.path("foo/bar"), "foobar", true, true, thread);
    context.createFile(context.path("bar/foo/bar"), "", true, true, thread);

    testOutputFile(outputDirectory.getChild("foobar"), "");
    testOutputFile(outputDirectory.getRelative("foo/bar"), "foobar");
    testOutputFile(outputDirectory.getRelative("bar/foo/bar"), "");

    try {
      context.createFile(context.path("/absolute"), "", true, true, thread);
      fail("Expected error on creating path outside of the repository directory");
    } catch (RepositoryFunctionException ex) {
      assertThat(ex)
          .hasCauseThat()
          .hasMessageThat()
          .isEqualTo("Cannot write outside of the repository directory for path /absolute");
    }
    try {
      context.createFile(context.path("../somepath"), "", true, true, thread);
      fail("Expected error on creating path outside of the repository directory");
    } catch (RepositoryFunctionException ex) {
      assertThat(ex)
          .hasCauseThat()
          .hasMessageThat()
          .isEqualTo("Cannot write outside of the repository directory for path /somepath");
    }
    try {
      context.createFile(context.path("foo/../../somepath"), "", true, true, thread);
      fail("Expected error on creating path outside of the repository directory");
    } catch (RepositoryFunctionException ex) {
      assertThat(ex)
          .hasCauseThat()
          .hasMessageThat()
          .isEqualTo("Cannot write outside of the repository directory for path /somepath");
    }
  }

  @Test
  public void testDelete() throws Exception {
    setUpContexForRule("testDelete");
    Path bar = outputDirectory.getRelative("foo/bar");
    StarlarkPath barPath = context.path(bar.getPathString());
    context.createFile(barPath, "content", true, true, thread);
    assertThat(context.delete(barPath, thread)).isTrue();

    assertThat(context.delete(barPath, thread)).isFalse();

    Path tempFile = scratch.file("/abcde/b", "123");
    assertThat(context.delete(context.path(tempFile.getPathString()), thread)).isTrue();

    Path innerDir = scratch.dir("/some/inner");
    scratch.dir("/some/inner/deeper");
    scratch.file("/some/inner/deeper.txt");
    scratch.file("/some/inner/deeper/1.txt");
    assertThat(context.delete(innerDir.toString(), thread)).isTrue();

    Path underWorkspace = root.getRelative("under_workspace");
    try {
      context.delete(underWorkspace.toString(), thread);
      fail();
    } catch (EvalException expected) {
      assertThat(expected.getMessage())
          .startsWith("delete() can only be applied to external paths");
    }

    scratch.file(underWorkspace.getPathString(), "123");
    setUpContextForRule(
        ImmutableMap.of("name", "test"),
        ImmutableSet.of(PathFragment.create("under_workspace")),
        StarlarkSemantics.DEFAULT,
        /* repoRemoteExecutor= */ null);
    assertThat(context.delete(underWorkspace.toString(), thread)).isTrue();
  }

  @Test
  public void testRead() throws Exception {
    setUpContexForRule("test");
    context.createFile(context.path("foo/bar"), "foobar", true, true, thread);

    String content = context.readFile(context.path("foo/bar"), thread);
    assertThat(content).isEqualTo("foobar");
  }

  @Test
  public void testPatch() throws Exception {
    setUpContexForRule("test");
    StarlarkPath foo = context.path("foo");
    context.createFile(foo, "line one\n", false, true, thread);
    StarlarkPath patchFile = context.path("my.patch");
    context.createFile(
        context.path("my.patch"), "--- foo\n+++ foo\n" + ONE_LINE_PATCH, false, true, thread);
    context.patch(patchFile, StarlarkInt.of(0), thread);
    testOutputFile(foo.getPath(), String.format("line one%nline two%n"));
  }

  @Test
  public void testCannotFindFileToPatch() throws Exception {
    setUpContexForRule("test");
    StarlarkPath patchFile = context.path("my.patch");
    context.createFile(
        context.path("my.patch"), "--- foo\n+++ foo\n" + ONE_LINE_PATCH, false, true, thread);
    try {
      context.patch(patchFile, StarlarkInt.of(0), thread);
      fail("Expected RepositoryFunctionException");
    } catch (RepositoryFunctionException ex) {
      assertThat(ex)
          .hasCauseThat()
          .hasMessageThat()
          .isEqualTo(
              "Error applying patch /outputDir/my.patch: Cannot find file to patch (near line 1)"
                  + ", old file name (foo) doesn't exist, new file name (foo) doesn't exist.");
    }
  }

  @Test
  public void testPatchOutsideOfExternalRepository() throws Exception {
    setUpContexForRule("test");
    StarlarkPath patchFile = context.path("my.patch");
    context.createFile(
        context.path("my.patch"),
        "--- ../other_root/foo\n" + "+++ ../other_root/foo\n" + ONE_LINE_PATCH,
        false,
        true,
        thread);
    try {
      context.patch(patchFile, StarlarkInt.of(0), thread);
      fail("Expected RepositoryFunctionException");
    } catch (RepositoryFunctionException ex) {
      assertThat(ex)
          .hasCauseThat()
          .hasMessageThat()
          .isEqualTo(
              "Error applying patch /outputDir/my.patch: Cannot patch file outside of external "
                  + "repository (/outputDir), file path = \"../other_root/foo\" at line 1");
    }
  }

  @Test
  public void testPatchErrorWasThrown() throws Exception {
    setUpContexForRule("test");
    StarlarkPath foo = context.path("foo");
    StarlarkPath patchFile = context.path("my.patch");
    context.createFile(foo, "line three\n", false, true, thread);
    context.createFile(
        context.path("my.patch"), "--- foo\n+++ foo\n" + ONE_LINE_PATCH, false, true, thread);
    try {
      context.patch(patchFile, StarlarkInt.of(0), thread);
      fail("Expected RepositoryFunctionException");
    } catch (RepositoryFunctionException ex) {
      assertThat(ex)
          .hasCauseThat()
          .hasMessageThat()
          .isEqualTo(
              "Error applying patch /outputDir/my.patch: Incorrect Chunk: the chunk content "
                  + "doesn't match the target\n"
                  + "**Original Position**: 1\n"
                  + "\n"
                  + "**Original Content**:\n"
                  + "line one\n"
                  + "\n"
                  + "**Revised Content**:\n"
                  + "line one\n"
                  + "line two\n");
    }
  }

  @Test
  public void testRemoteExec() throws Exception {
    // Test that context.execute() can call out to remote execution and correctly forward
    // execution properties.

    // Arrange
    ImmutableMap<String, Object> attrValues =
        ImmutableMap.of(
            "name",
            "configure",
            "$remotable",
            true,
            "exec_properties",
            Dict.of((Mutability) null, "OSFamily", "Linux"));

    RepositoryRemoteExecutor repoRemoteExecutor = Mockito.mock(RepositoryRemoteExecutor.class);
    ExecutionResult executionResult =
        new ExecutionResult(
            0,
            "test-stdout".getBytes(StandardCharsets.US_ASCII),
            "test-stderr".getBytes(StandardCharsets.US_ASCII));
    when(repoRemoteExecutor.execute(any(), any(), any(), any(), any(), any()))
        .thenReturn(executionResult);

    setUpContextForRule(
        attrValues,
        ImmutableSet.of(),
        StarlarkSemantics.builder()
            .setBool(BuildLanguageOptions.EXPERIMENTAL_REPO_REMOTE_EXEC, true)
            .build(),
        repoRemoteExecutor,
        Attribute.attr("$remotable", Type.BOOLEAN).build(),
        Attribute.attr("exec_properties", Type.STRING_DICT).build());

    // Act
    StarlarkExecutionResult starlarkExecutionResult =
        context.execute(
            StarlarkList.of(/*mutability=*/ null, "/bin/cmd", "arg1"),
            /* timeoutI= */ StarlarkInt.of(10),
            /*uncheckedEnvironment=*/ Dict.empty(),
            /*quiet=*/ true,
            /*workingDirectory=*/ "",
            thread);

    // Assert
    verify(repoRemoteExecutor)
        .execute(
            /* arguments= */ ImmutableList.of("/bin/cmd", "arg1"),
            /* inputFiles= */ ImmutableSortedMap.of(),
            /* executionProperties= */ ImmutableMap.of("OSFamily", "Linux"),
            /* environment= */ ImmutableMap.of(),
            /* workingDirectory= */ "",
            /* timeout= */ Duration.ofSeconds(10));
    assertThat(starlarkExecutionResult.getReturnCode()).isEqualTo(0);
    assertThat(starlarkExecutionResult.getStdout()).isEqualTo("test-stdout");
    assertThat(starlarkExecutionResult.getStderr()).isEqualTo("test-stderr");
  }

  @Test
  public void testSymlink() throws Exception {
    setUpContexForRule("test");
    context.createFile(context.path("foo"), "foobar", true, true, thread);

    context.symlink(context.path("foo"), context.path("bar"), thread);
    testOutputFile(outputDirectory.getChild("bar"), "foobar");

    assertThat(context.path("bar").realpath()).isEqualTo(context.path("foo"));
  }

  private void testOutputFile(Path path, String content) throws IOException {
    assertThat(path.exists()).isTrue();
    try (InputStreamReader reader =
        new InputStreamReader(path.getInputStream(), StandardCharsets.UTF_8)) {
      assertThat(CharStreams.toString(reader)).isEqualTo(content);
    }
  }

  @Test
  public void testDirectoryListing() throws Exception {
    setUpContexForRule("test");
    scratch.file("/my/folder/a");
    scratch.file("/my/folder/b");
    scratch.file("/my/folder/c");
    assertThat(context.path("/my/folder").readdir()).containsExactly(
        context.path("/my/folder/a"), context.path("/my/folder/b"), context.path("/my/folder/c"));
  }
}
