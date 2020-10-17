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
package com.google.devtools.build.lib.remote.merkletree;

import static com.google.common.truth.Truth.assertThat;

import com.google.devtools.build.lib.actions.ActionInput;
import com.google.devtools.build.lib.actions.ActionInputHelper;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.FileArtifactValue;
import com.google.devtools.build.lib.actions.cache.VirtualActionInput;
import com.google.devtools.build.lib.actions.util.ActionsTestUtil;
import com.google.devtools.build.lib.remote.merkletree.DirectoryTree.FileNode;
import com.google.devtools.build.lib.remote.util.StaticMetadataProvider;
import com.google.devtools.build.lib.remote.util.StringActionInput;
import com.google.devtools.build.lib.vfs.FileSystemUtils;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.Test;

/** Tests for {@link DirectoryTreeBuilder#buildFromActionInputs}. */
public class ActionInputDirectoryTreeTest extends DirectoryTreeTest {

  @Override
  protected DirectoryTree build(Path... paths) throws IOException {
    SortedMap<PathFragment, ActionInput> inputFiles = new TreeMap<>();
    Map<ActionInput, FileArtifactValue> metadata = new HashMap<>();

    for (Path path : paths) {
      PathFragment relPath = path.relativeTo(execRoot);
      Artifact a = ActionsTestUtil.createArtifact(artifactRoot, path);

      inputFiles.put(relPath, a);
      metadata.put(a, FileArtifactValue.createForTesting(a));
    }

    return DirectoryTreeBuilder.fromActionInputs(
        inputFiles, new StaticMetadataProvider(metadata), execRoot, digestUtil);
  }

  @Test
  public void virtualActionInputShouldWork() throws Exception {
    SortedMap<PathFragment, ActionInput> sortedInputs = new TreeMap<>();
    Map<ActionInput, FileArtifactValue> metadata = new HashMap<>();

    Artifact foo = addFile("srcs/foo.cc", "foo", sortedInputs, metadata);
    VirtualActionInput bar = addVirtualFile("srcs/bar.cc", "bar", sortedInputs);

    DirectoryTree tree =
        DirectoryTreeBuilder.fromActionInputs(
            sortedInputs, new StaticMetadataProvider(metadata), execRoot, digestUtil);
    assertLexicographicalOrder(tree);

    assertThat(directoriesAtDepth(0, tree)).containsExactly("srcs");
    assertThat(directoriesAtDepth(1, tree)).isEmpty();

    FileNode expectedFooNode =
        new FileNode("foo.cc", foo.getPath(), digestUtil.computeAsUtf8("foo"));
    FileNode expectedBarNode =
        new FileNode("bar.cc", bar.getBytes(), digestUtil.computeAsUtf8("bar"));
    assertThat(fileNodesAtDepth(tree, 0)).isEmpty();
    assertThat(fileNodesAtDepth(tree, 1)).containsExactly(expectedFooNode, expectedBarNode);
  }

  @Test
  public void directoryInputShouldBeExpanded() throws Exception {
    // Test that directory inputs are fully expanded and added to the input tree.
    // Note that this test is not about tree artifacts, but normal artifacts that point to
    // a directory on disk.

    SortedMap<PathFragment, ActionInput> sortedInputs = new TreeMap<>();
    Map<ActionInput, FileArtifactValue> metadata = new HashMap<>();

    Artifact foo = addFile("srcs/foo.cc", "foo", sortedInputs, metadata);

    Path dirPath = execRoot.getRelative("srcs/dir");
    dirPath.createDirectoryAndParents();

    Path barPath = dirPath.getRelative("bar.cc");
    FileSystemUtils.writeContentAsLatin1(barPath, "bar");
    ActionInput bar = ActionInputHelper.fromPath(barPath.relativeTo(execRoot));
    metadata.put(bar, FileArtifactValue.createForTesting(barPath));

    dirPath.getRelative("fizz").createDirectoryAndParents();
    Path buzzPath = dirPath.getRelative("fizz/buzz.cc");
    FileSystemUtils.writeContentAsLatin1(dirPath.getRelative("fizz/buzz.cc"), "buzz");
    ActionInput buzz = ActionInputHelper.fromPath(buzzPath.relativeTo(execRoot));
    metadata.put(buzz, FileArtifactValue.createForTesting(buzzPath));

    Artifact dir = ActionsTestUtil.createArtifact(artifactRoot, dirPath);
    sortedInputs.put(dirPath.relativeTo(execRoot), dir);
    metadata.put(dir, FileArtifactValue.createForTesting(dirPath));

    DirectoryTree tree =
        DirectoryTreeBuilder.fromActionInputs(
            sortedInputs, new StaticMetadataProvider(metadata), execRoot, digestUtil);
    assertLexicographicalOrder(tree);

    assertThat(directoriesAtDepth(0, tree)).containsExactly("srcs");
    assertThat(directoriesAtDepth(1, tree)).containsExactly("dir");
    assertThat(directoriesAtDepth(2, tree)).containsExactly("fizz");
    assertThat(directoriesAtDepth(3, tree)).isEmpty();

    FileNode expectedFooNode =
        new FileNode("foo.cc", foo.getPath(), digestUtil.computeAsUtf8("foo"));
    FileNode expectedBarNode =
        new FileNode(
            "bar.cc", execRoot.getRelative(bar.getExecPath()), digestUtil.computeAsUtf8("bar"));
    FileNode expectedBuzzNode =
        new FileNode(
            "buzz.cc", execRoot.getRelative(buzz.getExecPath()), digestUtil.computeAsUtf8("buzz"));
    assertThat(fileNodesAtDepth(tree, 0)).isEmpty();
    assertThat(fileNodesAtDepth(tree, 1)).containsExactly(expectedFooNode);
    assertThat(fileNodesAtDepth(tree, 2)).containsExactly(expectedBarNode);
    assertThat(fileNodesAtDepth(tree, 3)).containsExactly(expectedBuzzNode);
  }

  private static VirtualActionInput addVirtualFile(
      String path, String content, SortedMap<PathFragment, ActionInput> sortedInputs) {
    VirtualActionInput input = new StringActionInput(content, PathFragment.create(path));
    sortedInputs.put(PathFragment.create(path), input);
    return input;
  }

  private Artifact addFile(
      String path,
      String content,
      SortedMap<PathFragment, ActionInput> sortedInputs,
      Map<ActionInput, FileArtifactValue> metadata)
      throws IOException {
    Path p = execRoot.getRelative(path);
    p.getParentDirectory().createDirectoryAndParents();
    FileSystemUtils.writeContentAsLatin1(p, content);
    Artifact a = ActionsTestUtil.createArtifact(artifactRoot, p);

    sortedInputs.put(PathFragment.create(path), a);
    metadata.put(a, FileArtifactValue.createForTesting(a));
    return a;
  }
}
