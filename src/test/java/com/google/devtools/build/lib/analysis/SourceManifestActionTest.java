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
package com.google.devtools.build.lib.analysis;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.devtools.build.lib.actions.util.ActionsTestUtil.NULL_ACTION_OWNER;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.ActionExecutionException;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.ArtifactRoot;
import com.google.devtools.build.lib.actions.util.ActionsTestUtil;
import com.google.devtools.build.lib.analysis.SourceManifestAction.ManifestType;
import com.google.devtools.build.lib.analysis.util.BuildViewTestCase;
import com.google.devtools.build.lib.util.Fingerprint;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.Root;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link SourceManifestAction}.
 */
@RunWith(JUnit4.class)
public final class SourceManifestActionTest extends BuildViewTestCase {

  private Map<PathFragment, Artifact> fakeManifest;

  private Path pythonSourcePath;
  private Artifact pythonSourceFile;
  private Path buildFilePath;
  private Artifact buildFile;

  private Path manifestOutputPath;
  private Artifact manifestOutputFile;

  @Before
  public final void createFiles() throws Exception  {
    analysisMock.pySupport().setup(mockToolsConfig);
    // Test with a raw manifest Action.
    fakeManifest = new LinkedHashMap<>();
    ArtifactRoot trivialRoot =
        ArtifactRoot.asSourceRoot(Root.fromPath(rootDirectory.getRelative("trivial")));
    buildFilePath = scratch.file("trivial/BUILD",
                                "py_binary(name='trivial', srcs =['trivial.py'])");
    buildFile = ActionsTestUtil.createArtifact(trivialRoot, buildFilePath);

    pythonSourcePath = scratch.file("trivial/trivial.py",
                                   "#!/usr/bin/python \n print 'Hello World'");
    pythonSourceFile = ActionsTestUtil.createArtifact(trivialRoot, pythonSourcePath);
    fakeManifest.put(buildFilePath.relativeTo(rootDirectory), buildFile);
    fakeManifest.put(pythonSourcePath.relativeTo(rootDirectory), pythonSourceFile);
    ArtifactRoot outputDir = ArtifactRoot.asDerivedRoot(rootDirectory, "blaze-output");
    manifestOutputPath = rootDirectory.getRelative("blaze-output/trivial.runfiles_manifest");
    manifestOutputFile = ActionsTestUtil.createArtifact(outputDir, manifestOutputPath);
  }

  /**
   * Get the contents of a file internally using an in memory output stream.
   *
   * @return returns the file contents as a string.
   * @throws ActionExecutionException
   * @throws InterruptedException
   * @throws IOException
   */
  public String getFileContentsAsString(SourceManifestAction manifest) throws IOException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    manifest.writeOutputFile(stream, reporter);
    return stream.toString();
  }

  private SourceManifestAction createSymlinkAction() {
    return createAction(ManifestType.SOURCE_SYMLINKS, true);
  }

  private SourceManifestAction createSourceOnlyAction() {
    return createAction(ManifestType.SOURCES_ONLY, true);
  }

  private SourceManifestAction createAction(ManifestType type, boolean addInitPy) {
    Runfiles.Builder builder = new Runfiles.Builder("TESTING", false);
    builder.addSymlinks(fakeManifest);
    if (addInitPy) {
      builder.setEmptyFilesSupplier(
          analysisMock.pySupport().getPythonSemantics().getEmptyRunfilesSupplier());
    }
    return new SourceManifestAction(type, NULL_ACTION_OWNER, manifestOutputFile, builder.build());
  }

  /**
   * Manifest writer that validates an expected call sequence.
   */
  private class MockManifestWriter implements SourceManifestAction.ManifestWriter {
    private List<Map.Entry<PathFragment, Artifact>> expectedSequence = new ArrayList<>();

    public MockManifestWriter() {
      expectedSequence.addAll(fakeManifest.entrySet());
    }

    @Override
    public void writeEntry(Writer manifestWriter, PathFragment rootRelativePath,
        @Nullable Artifact symlink) throws IOException {
      assertWithMessage("Expected manifest input to be exhausted").that(expectedSequence)
          .isNotEmpty();
      Map.Entry<PathFragment, Artifact> expectedEntry = expectedSequence.remove(0);
      assertThat(rootRelativePath)
          .isEqualTo(PathFragment.create("TESTING").getRelative(expectedEntry.getKey()));
      assertThat(symlink).isEqualTo(expectedEntry.getValue());
    }

    public int unconsumedInputs() {
      return expectedSequence.size();
    }

    @Override public String getMnemonic() { return null; }
    @Override public String getRawProgressMessage() { return null; }

    @Override
    public boolean isRemotable() {
      return false;
    }
  }

  /**
   * Tests that SourceManifestAction calls its manifest writer with the expected call sequence.
   */
  @Test
  public void testManifestWriterIntegration() throws Exception {
    MockManifestWriter mockWriter = new MockManifestWriter();
    getFileContentsAsString(
        new SourceManifestAction(
            mockWriter,
            NULL_ACTION_OWNER,
            manifestOutputFile,
            new Runfiles.Builder("TESTING", false).addSymlinks(fakeManifest).build()));
    assertThat(mockWriter.unconsumedInputs()).isEqualTo(0);
  }

  @Test
  public void testSimpleFileWriting() throws Exception {
    String manifestContents = getFileContentsAsString(createSymlinkAction());
    assertThat(manifestContents)
        .isEqualTo(
            "TESTING/trivial/BUILD /workspace/trivial/BUILD\n"
                + "TESTING/trivial/__init__.py \n"
                + "TESTING/trivial/trivial.py /workspace/trivial/trivial.py\n");
  }

  /**
   * Tests that the source-only formatting strategy includes relative paths only
   * (i.e. not symlinks).
   */
  @Test
  public void testSourceOnlyFormatting() throws Exception {
    String manifestContents = getFileContentsAsString(createSourceOnlyAction());
    assertThat(manifestContents)
        .isEqualTo(
            "TESTING/trivial/BUILD\n"
                + "TESTING/trivial/__init__.py\n"
                + "TESTING/trivial/trivial.py\n");
  }

  /**
   * Test that a directory which has only a .so file in the manifest triggers
   * the inclusion of a __init__.py file for that directory.
   */
  @Test
  public void testSwigLibrariesTriggerInitDotPyInclusion() throws Exception {
    ArtifactRoot swiggedLibPath =
        ArtifactRoot.asSourceRoot(Root.fromPath(rootDirectory.getRelative("swig")));
    Path swiggedFile = scratch.file("swig/fakeLib.so");
    Artifact swigDotSO = ActionsTestUtil.createArtifact(swiggedLibPath, swiggedFile);
    fakeManifest.put(swiggedFile.relativeTo(rootDirectory), swigDotSO);
    String manifestContents = getFileContentsAsString(createSymlinkAction());
    assertThat(manifestContents).containsMatch(".*TESTING/swig/__init__.py .*");
    assertThat(manifestContents).containsMatch("fakeLib.so");
  }

  @Test
  public void testNoPythonOrSwigLibrariesDoNotTriggerInitDotPyInclusion() throws Exception {
    ArtifactRoot nonPythonPath =
        ArtifactRoot.asSourceRoot(Root.fromPath(rootDirectory.getRelative("not_python")));
    Path nonPythonFile = scratch.file("not_python/blob_of_data");
    Artifact nonPython = ActionsTestUtil.createArtifact(nonPythonPath, nonPythonFile);
    fakeManifest.put(nonPythonFile.relativeTo(rootDirectory), nonPython);
    String manifestContents = getFileContentsAsString(createSymlinkAction());
    assertThat(manifestContents).doesNotContain("not_python/__init__.py \n");
    assertThat(manifestContents).containsMatch("blob_of_data");
  }

  @Test
  public void testGetMnemonic() throws Exception {
    assertThat(createSymlinkAction().getMnemonic()).isEqualTo("SourceSymlinkManifest");
    assertThat(createAction(ManifestType.SOURCE_SYMLINKS, false).getMnemonic())
        .isEqualTo("SourceSymlinkManifest");
    assertThat(createSourceOnlyAction().getMnemonic()).isEqualTo("PackagingSourcesManifest");
  }

  @Test
  public void testSymlinkProgressMessage() throws Exception {
    String progress = createSymlinkAction().getProgressMessage();
    assertWithMessage("null action not found in " + progress)
        .that(progress.contains("//null/action:owner"))
        .isTrue();
  }

  @Test
  public void testSymlinkProgressMessageNoPyInitFiles() throws Exception {
    String progress = createAction(ManifestType.SOURCE_SYMLINKS, false).getProgressMessage();
    assertWithMessage("null action not found in " + progress)
        .that(progress.contains("//null/action:owner"))
        .isTrue();
  }

  @Test
  public void testSourceOnlyProgressMessage() throws Exception {
    SourceManifestAction action =
        new SourceManifestAction(
            ManifestType.SOURCES_ONLY,
            NULL_ACTION_OWNER,
            getBinArtifactWithNoOwner("trivial.runfiles_manifest"),
            Runfiles.EMPTY);
    String progress = action.getProgressMessage();
    assertWithMessage("null action not found in " + progress)
        .that(progress.contains("//null/action:owner"))
        .isTrue();
  }

  @Test
  public void testRootSymlinksAffectKey() throws Exception {
    Artifact manifest1 = getBinArtifactWithNoOwner("manifest1");
    Artifact manifest2 = getBinArtifactWithNoOwner("manifest2");

    SourceManifestAction action1 =
        new SourceManifestAction(
            ManifestType.SOURCE_SYMLINKS,
            NULL_ACTION_OWNER,
            manifest1,
            new Runfiles.Builder("TESTING", false)
                .addRootSymlinks(ImmutableMap.of(PathFragment.create("a"), buildFile))
                .build());

    SourceManifestAction action2 =
        new SourceManifestAction(
            ManifestType.SOURCE_SYMLINKS,
            NULL_ACTION_OWNER,
            manifest2,
            new Runfiles.Builder("TESTING", false)
                .addRootSymlinks(ImmutableMap.of(PathFragment.create("b"), buildFile))
                .build());

    assertThat(computeKey(action2)).isNotEqualTo(computeKey(action1));
  }

  // Regression test for b/116254698.
  @Test
  public void testEmptyFilesAffectKey() throws Exception {
    Artifact manifest1 = getBinArtifactWithNoOwner("manifest1");
    Artifact manifest2 = getBinArtifactWithNoOwner("manifest2");

    SourceManifestAction action1 =
        new SourceManifestAction(
            ManifestType.SOURCE_SYMLINKS,
            NULL_ACTION_OWNER,
            manifest1,
            new Runfiles.Builder("TESTING", false)
                .addSymlink(PathFragment.create("a"), buildFile)
                .setEmptyFilesSupplier(
                    paths ->
                        paths.stream()
                            .map(p -> p.replaceName(p.getBaseName() + "~"))
                            .collect(Collectors.toSet()))
                .build());

    SourceManifestAction action2 =
        new SourceManifestAction(
            ManifestType.SOURCE_SYMLINKS,
            NULL_ACTION_OWNER,
            manifest2,
            new Runfiles.Builder("TESTING", false)
                .addSymlink(PathFragment.create("a"), buildFile)
                .setEmptyFilesSupplier(
                    paths ->
                        paths.stream()
                            .map(p -> p.replaceName(p.getBaseName() + "~~"))
                            .collect(Collectors.toSet()))
                .build());

    assertThat(computeKey(action2)).isNotEqualTo(computeKey(action1));
  }

  private String computeKey(SourceManifestAction action) {
    Fingerprint fp = new Fingerprint();
    action.computeKey(actionKeyContext, /*artifactExpander=*/ null, fp);
    return fp.hexDigestAndReset();
  }
}
