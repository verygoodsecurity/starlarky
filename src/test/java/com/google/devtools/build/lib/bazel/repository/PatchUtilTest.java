// Copyright 2019 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.bazel.repository;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.github.difflib.patch.PatchFailedException;
import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.testutil.Scratch;
import com.google.devtools.build.lib.vfs.DigestHashFunction;
import com.google.devtools.build.lib.vfs.FileSystem;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.inmemoryfs.InMemoryFileSystem;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PatchUtil}. */
@RunWith(JUnit4.class)
public class PatchUtilTest {

  private FileSystem fs;
  private Scratch scratch;
  private Path root;

  @Before
  public final void initializeFileSystemAndDirectories() throws Exception {
    fs = new InMemoryFileSystem(DigestHashFunction.SHA256);
    scratch = new Scratch(fs, "/root");
    root = scratch.dir("/root");
  }

  @Test
  public void testAddFile() throws IOException, PatchFailedException {
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/newfile b/newfile",
            "new file mode 100544",
            "index 0000000..f742c88",
            "--- /dev/null",
            "+++ b/newfile",
            "@@ -0,0 +1,2 @@",
            "+I'm a new file",
            "+hello, world",
            "-- ",
            "2.21.0.windows.1");
    PatchUtil.apply(patchFile, 1, root);
    Path newFile = root.getRelative("newfile");
    ImmutableList<String> newFileContent = ImmutableList.of("I'm a new file", "hello, world");
    assertThat(PatchUtil.readFile(newFile)).containsExactlyElementsIn(newFileContent);
    // Make sure file permission is set as specified.
    assertThat(newFile.isReadable()).isTrue();
    assertThat(newFile.isWritable()).isFalse();
    assertThat(newFile.isExecutable()).isTrue();
  }

  @Test
  public void testAddOneLineFile() throws IOException, PatchFailedException {
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/newfile b/newfile",
            "new file mode 100644",
            "index 0000000..f742c88",
            "--- /dev/null",
            "+++ b/newfile",
            "@@ -0,0 +1 @@", // diff will produce such chunk header for one line file.
            "+hello, world");
    PatchUtil.apply(patchFile, 1, root);
    Path newFile = root.getRelative("newfile");
    ImmutableList<String> newFileContent = ImmutableList.of("hello, world");
    assertThat(PatchUtil.readFile(newFile)).containsExactlyElementsIn(newFileContent);
  }

  @Test
  public void testDeleteFile() throws IOException, PatchFailedException {
    Path oldFile = scratch.file("/root/oldfile", "I'm an old file", "bye, world");
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "--- a/oldfile",
            "+++ /dev/null",
            "@@ -1,2 +0,0 @@",
            "-I'm an old file",
            "-bye, world");
    PatchUtil.apply(patchFile, 1, root);
    assertThat(oldFile.exists()).isFalse();
  }

  @Test
  public void testDeleteOneLineFile() throws IOException, PatchFailedException {
    Path oldFile = scratch.file("/root/oldfile", "bye, world");
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "--- a/oldfile",
            "+++ /dev/null",
            "@@ -1 +0,0 @@", // diff will produce such chunk header for one line file.
            "-bye, world");
    PatchUtil.apply(patchFile, 1, root);
    assertThat(oldFile.exists()).isFalse();
  }

  @Test
  public void testDeleteAllContentButNotFile() throws IOException, PatchFailedException {
    // If newfile is not /dev/null, we don't delete the file even it's empty after patching,
    // this is the behavior of patch command line tool.
    Path oldFile = scratch.file("/root/oldfile", "I'm an old file", "bye, world");
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "--- a/oldfile",
            "+++ b/oldfile",
            "@@ -1,2 +0,0 @@",
            "-I'm an old file",
            "-bye, world");
    PatchUtil.apply(patchFile, 1, root);
    assertThat(oldFile.exists()).isTrue();
    assertThat(PatchUtil.readFile(oldFile)).isEmpty();
  }

  @Test
  public void testApplyToOldFile() throws IOException, PatchFailedException {
    // If both oldfile and newfile exist, we should patch the old file.
    Path oldFile = scratch.file("/root/oldfile", "line one");
    Path newFile = scratch.file("/root/newfile", "line one");
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "--- oldfile",
            "+++ newfile",
            "@@ -1,1 +1,2 @@",
            " line one",
            "+line two");
    PatchUtil.apply(patchFile, 0, root);
    ImmutableList<String> newContent = ImmutableList.of("line one", "line two");
    assertThat(PatchUtil.readFile(oldFile)).containsExactlyElementsIn(newContent);
    // new file should not change
    assertThat(PatchUtil.readFile(newFile)).containsExactly("line one");
  }

  @Test
  public void testApplyToNewFile() throws IOException, PatchFailedException {
    // If only newfile exists, we should patch the new file.
    Path newFile = scratch.file("/root/newfile", "line one");
    newFile.setReadable(true);
    newFile.setWritable(true);
    newFile.setExecutable(true);
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "--- oldfile",
            "+++ newfile",
            "@@ -1,1 +1,2 @@",
            " line one",
            "+line two");
    PatchUtil.apply(patchFile, 0, root);
    ImmutableList<String> newContent = ImmutableList.of("line one", "line two");
    assertThat(PatchUtil.readFile(newFile)).containsExactlyElementsIn(newContent);
    // Make sure file permission is preserved.
    assertThat(newFile.isReadable()).isTrue();
    assertThat(newFile.isWritable()).isTrue();
    assertThat(newFile.isExecutable()).isTrue();
  }

  @Test
  public void testChangeFilePermission() throws IOException, PatchFailedException {
    Path myFile = scratch.file("/root/test.sh", "line one");
    myFile.setReadable(true);
    myFile.setWritable(true);
    myFile.setExecutable(false);
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/test.sh b/test.sh",
            "old mode 100644",
            "new mode 100755");
    PatchUtil.apply(patchFile, 1, root);
    assertThat(PatchUtil.readFile(myFile)).containsExactly("line one");
    assertThat(myFile.isReadable()).isTrue();
    assertThat(myFile.isWritable()).isTrue();
    assertThat(myFile.isExecutable()).isTrue();
  }

  @Test
  public void testGitFormatPatching() throws IOException, PatchFailedException {
    Path foo =
        scratch.file(
            "/root/foo.cc",
            "#include <stdio.h>",
            "",
            "void main(){",
            "  printf(\"Hello foo\");",
            "}");
    Path bar = scratch.file("/root/bar.cc", "void lib(){", "  printf(\"Hello bar\");", "}");
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "From d205551eab3350afdb380f90ef83442ffcc0e22b Mon Sep 17 00:00:00 2001",
            "From: Yun Peng <pcloudy@google.com>",
            "Date: Thu, 6 Jun 2019 11:34:08 +0200",
            "Subject: [PATCH] 2",
            "",
            "---",
            " bar.cc | 2 +-",
            " foo.cc | 1 +",
            " 2 files changed, 2 insertions(+), 1 deletion(-)",
            "",
            "diff --git a/bar.cc b/bar.cc",
            "index e77137b..36dc9ab 100644",
            "--- a/bar.cc",
            "+++ b/bar.cc",
            "@@ -1,3 +1,3 @@",
            " void lib(){",
            "-  printf(\"Hello bar\");",
            "+  printf(\"Hello patch\");",
            " }",
            "diff --git a/foo.cc b/foo.cc",
            "index f3008f9..ec4aaa0 100644",
            "--- a/foo.cc",
            "+++ b/foo.cc",
            "@@ -2,4 +2,5 @@",
            " ",
            " void main(){",
            "   printf(\"Hello foo\");",
            "+  printf(\"Hello from patch\");",
            " }",
            "-- ",
            "2.21.0.windows.1",
            "",
            "");
    PatchUtil.apply(patchFile, 1, root);
    ImmutableList<String> newFoo =
        ImmutableList.of(
            "#include <stdio.h>",
            "",
            "void main(){",
            "  printf(\"Hello foo\");",
            "  printf(\"Hello from patch\");",
            "}");
    ImmutableList<String> newBar =
        ImmutableList.of("void lib(){", "  printf(\"Hello patch\");", "}");
    assertThat(PatchUtil.readFile(foo)).containsExactlyElementsIn(newFoo);
    assertThat(PatchUtil.readFile(bar)).containsExactlyElementsIn(newBar);
  }

  @Test
  public void testGitFormatRenaming() throws IOException, PatchFailedException {
    Path foo =
        scratch.file(
            "/root/foo.cc",
            "#include <stdio.h>",
            "",
            "void main(){",
            "  printf(\"Hello foo\");",
            "}");
    Path bar = scratch.file("/root/bar.cc", "void lib(){", "  printf(\"Hello bar\");", "}");
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/bar.cc b/bar.cpp",
            "similarity index 61%",
            "rename from bar.cc",
            "rename to bar.cpp",
            "index e77137b..9e35ee4 100644",
            "--- a/bar.cc",
            "+++ b/bar.cpp",
            "@@ -1,3 +1,4 @@",
            " void lib(){",
            "   printf(\"Hello bar\");",
            "+  printf(\"Hello cpp\");",
            " }",
            "diff --git a/foo.cc b/foo.cpp",
            "similarity index 100%",
            "rename from foo.cc",
            "rename to foo.cpp");
    PatchUtil.apply(patchFile, 1, root);
    ImmutableList<String> newFoo =
        ImmutableList.of("#include <stdio.h>", "", "void main(){", "  printf(\"Hello foo\");", "}");
    ImmutableList<String> newBar =
        ImmutableList.of(
            "void lib(){", "  printf(\"Hello bar\");", "  printf(\"Hello cpp\");", "}");
    Path fooCpp = root.getRelative("foo.cpp");
    Path barCpp = root.getRelative("bar.cpp");
    assertThat(foo.exists()).isFalse();
    assertThat(bar.exists()).isFalse();
    assertThat(PatchUtil.readFile(fooCpp)).containsExactlyElementsIn(newFoo);
    assertThat(PatchUtil.readFile(barCpp)).containsExactlyElementsIn(newBar);
  }

  @Test
  public void testMatchWithOffset() throws IOException, PatchFailedException {
    Path foo =
        scratch.file(
            "/root/foo.cc",
            "#include <stdio.h>",
            "",
            "void main(){",
            "  printf(\"Hello foo\");",
            "}");
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/foo.cc b/foo.cc",
            "index f3008f9..ec4aaa0 100644",
            "--- a/foo.cc",
            "+++ b/foo.cc",
            "@@ -6,4 +6,5 @@", // Should match with offset -4, original is "@@ -2,4 +2,5 @@"
            " ",
            " void main(){",
            "   printf(\"Hello foo\");",
            "+  printf(\"Hello from patch\");",
            " }");
    PatchUtil.apply(patchFile, 1, root);
    ImmutableList<String> newFoo =
        ImmutableList.of(
            "#include <stdio.h>",
            "",
            "void main(){",
            "  printf(\"Hello foo\");",
            "  printf(\"Hello from patch\");",
            "}");
    assertThat(PatchUtil.readFile(foo)).containsExactlyElementsIn(newFoo);
  }

  @Test
  public void testMultipleChunksWithDifferentOffset() throws IOException, PatchFailedException {
    Path foo =
        scratch.file("/root/foo", "1", "3", "4", "5", "6", "7", "8", "9", "10", "11", "13", "14");
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/foo b/foo",
            "index c20ab12..b83bdb1 100644",
            "--- a/foo",
            "+++ b/foo",
            "@@ -3,4 +3,5 @@", // Should match with offset -2, original is "@@ -1,4 +1,5 @@"
            " 1",
            "+2",
            " 3",
            " 4",
            " 5",
            "@@ -4,5 +5,6 @@", // Should match with offset 4, original is "@@ -8,4 +9,5 @@"
            " 9",
            " 10",
            " 11",
            "+12",
            " 13",
            " 14");
    PatchUtil.apply(patchFile, 1, root);
    ImmutableList<String> newFoo =
        ImmutableList.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14");
    assertThat(PatchUtil.readFile(foo)).containsExactlyElementsIn(newFoo);
  }

  @Test
  public void testFailedToGetFileName() throws IOException {
    scratch.file(
        "/root/foo.cc", "#include <stdio.h>", "", "void main(){", "  printf(\"Hello foo\");", "}");
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/foo.cc b/foo.cc",
            "index f3008f9..ec4aaa0 100644",
            "--- a/foo.cc",
            "+++ b/foo.cc",
            "@@ -2,4 +2,5 @@",
            " ",
            " void main(){",
            "   printf(\"Hello foo\");",
            "+  printf(\"Hello from patch\");",
            " }");
    PatchFailedException expected =
        assertThrows(
            PatchFailedException.class,
            () -> PatchUtil.apply(patchFile, 2, root)); // strip=2 is wrong
    assertThat(expected)
        .hasMessageThat()
        .contains("Cannot determine file name with strip = 2 at line 3:\n--- a/foo.cc");
  }

  @Test
  public void testPatchFileNotFound() {
    PatchFailedException expected =
        assertThrows(
            PatchFailedException.class,
            () -> PatchUtil.apply(root.getRelative("patchfile"), 1, root));
    assertThat(expected).hasMessageThat().contains("Cannot find patch file: /root/patchfile");
  }

  @Test
  public void testMissingBothOldAndNewFile() throws IOException {
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/ b/",
            "index f3008f9..ec4aaa0 100644",
            "@@ -2,4 +2,5 @@",
            " ",
            " void main(){",
            "   printf(\"Hello foo\");",
            "+  printf(\"Hello from patch\");",
            " }");
    PatchFailedException expected =
        assertThrows(PatchFailedException.class, () -> PatchUtil.apply(patchFile, 1, root));
    assertThat(expected)
        .hasMessageThat()
        .contains("Wrong patch format near line 3, neither new file or old file are specified.");
  }

  @Test
  public void testCannotFindFileToPatch() throws IOException {
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/foo.cc b/foo.cc",
            "index f3008f9..ec4aaa0 100644",
            "--- a/foo.cc",
            "+++ /dev/null",
            "@@ -2,4 +2,5 @@",
            " ",
            " void main(){",
            "   printf(\"Hello foo\");",
            "+  printf(\"Hello from patch\");",
            " }");
    PatchFailedException expected =
        assertThrows(PatchFailedException.class, () -> PatchUtil.apply(patchFile, 1, root));
    assertThat(expected)
        .hasMessageThat()
        .contains(
            "Cannot find file to patch (near line 3), old file name (foo.cc) doesn't exist, "
                + "new file name is not specified.");
  }

  @Test
  public void testCannotRenameFile() throws IOException {
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/bar.cc b/bar.cpp",
            "similarity index 61%",
            "rename from bar.cc",
            "rename to bar.cpp",
            "index e77137b..9e35ee4 100644",
            "--- a/bar.cc",
            "+++ b/bar.cpp",
            "@@ -1,3 +1,4 @@",
            " void lib(){",
            "   printf(\"Hello bar\");",
            "+  printf(\"Hello cpp\");",
            " }",
            "diff --git a/foo.cc b/foo.cpp",
            "similarity index 100%",
            "rename from foo.cc",
            "rename to foo.cpp");

    PatchFailedException expected;
    expected = assertThrows(PatchFailedException.class, () -> PatchUtil.apply(patchFile, 1, root));
    assertThat(expected)
        .hasMessageThat()
        .contains("Cannot rename file (near line 6), old file name (bar.cc) doesn't exist.");

    scratch.file("/root/bar.cc", "void lib(){", "  printf(\"Hello bar\");", "}");
    scratch.file("/root/foo.cc");
    scratch.file("/root/foo.cpp");

    expected = assertThrows(PatchFailedException.class, () -> PatchUtil.apply(patchFile, 1, root));
    assertThat(expected)
        .hasMessageThat()
        .contains("Cannot rename file (near line 17), new file name (foo.cpp) already exists.");
  }

  @Test
  public void testPatchOutsideOfRepository() throws IOException {
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/foo.cc b/foo.cc",
            "index f3008f9..ec4aaa0 100644",
            "--- a/../other_root/foo.cc",
            "+++ b/../other_root/foo.cc",
            "@@ -2,4 +2,5 @@",
            " ",
            " void main(){",
            "   printf(\"Hello foo\");",
            "+  printf(\"Hello from patch\");",
            " }");
    PatchFailedException expected =
        assertThrows(PatchFailedException.class, () -> PatchUtil.apply(patchFile, 1, root));
    assertThat(expected)
        .hasMessageThat()
        .contains(
            "Cannot patch file outside of external repository (/root), "
                + "file path = \"../other_root/foo.cc\" at line 3");
  }

  @Test
  public void testChunkDoesNotMatch() throws IOException {
    scratch.file(
        "/root/foo.cc", "#include <stdio.h>", "", "void main(){", "  printf(\"Hello foo\");", "}");
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/foo.cc b/foo.cc",
            "index f3008f9..ec4aaa0 100644",
            "--- a/foo.cc",
            "+++ b/foo.cc",
            "@@ -2,4 +2,5 @@",
            " ",
            " void main(){",
            "   printf(\"Hello bar\");", // Should be "Hello foo"
            "+  printf(\"Hello from patch\");",
            " }");
    PatchFailedException expected =
        assertThrows(PatchFailedException.class, () -> PatchUtil.apply(patchFile, 1, root));
    assertThat(expected)
        .hasMessageThat()
        .contains(
            "Incorrect Chunk: the chunk content doesn't match the target\n"
                + "**Original Position**: 2\n"
                + "\n"
                + "**Original Content**:\n"
                + "\n"
                + "void main(){\n"
                + "  printf(\"Hello bar\");\n"
                + "}\n"
                + "\n"
                + "**Revised Content**:\n"
                + "\n"
                + "void main(){\n"
                + "  printf(\"Hello bar\");\n"
                + "  printf(\"Hello from patch\");\n"
                + "}\n");
  }

  @Test
  public void testWrongChunkFormat1() throws IOException {
    scratch.file(
        "/root/foo.cc", "#include <stdio.h>", "", "void main(){", "  printf(\"Hello foo\");", "}");
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/foo.cc b/foo.cc",
            "index f3008f9..ec4aaa0 100644",
            "--- a/foo.cc",
            "+++ b/foo.cc",
            "@@ -2,4 +2,5 @@",
            " ",
            " void main(){",
            "   printf(\"Hello foo\");",
            "+  printf(\"Hello from patch\");",
            "+", // Adding this line will cause the chunk body not matching the header "@@ -2,4 +2,5
            // @@"
            " }");
    PatchFailedException expected =
        assertThrows(PatchFailedException.class, () -> PatchUtil.apply(patchFile, 1, root));
    assertThat(expected)
        .hasMessageThat()
        .contains("Wrong chunk detected near line 11:  }, does not expect a context line here.");
  }

  @Test
  public void testWrongChunkFormat2() throws IOException {
    scratch.file(
        "/root/foo.cc", "#include <stdio.h>", "", "void main(){", "  printf(\"Hello foo\");", "}");
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/foo.cc b/foo.cc",
            "index f3008f9..ec4aaa0 100644",
            "--- a/foo.cc",
            "+++ b/foo.cc",
            "@@ -2,4 +2,5 @@",
            " ",
            " void main(){",
            "   printf(\"Hello foo\");",
            "+  printf(\"Hello from patch\");");
    PatchFailedException expected =
        assertThrows(PatchFailedException.class, () -> PatchUtil.apply(patchFile, 1, root));
    assertThat(expected).hasMessageThat().contains("Expecting more chunk line at line 10");
  }

  @Test
  public void testWrongChunkFormat3() throws IOException {
    scratch.file(
        "/root/foo.cc", "#include <stdio.h>", "", "void main(){", "  printf(\"Hello foo\");", "}");
    Path patchFile =
        scratch.file(
            "/root/patchfile",
            "diff --git a/foo.cc b/foo.cc",
            "index f3008f9..ec4aaa0 100644",
            "--- a/foo.cc",
            "+++ b/foo.cc",
            // Missing @@ -l,s +l,s @@ line
            " ",
            " void main(){",
            "   printf(\"Hello foo\");",
            "+  printf(\"Hello from patch\");",
            " }");
    PatchFailedException expected =
        assertThrows(PatchFailedException.class, () -> PatchUtil.apply(patchFile, 1, root));
    assertThat(expected)
        .hasMessageThat()
        .contains("Looks like a unified diff at line 3, but no patch chunk was found.");
  }
}
