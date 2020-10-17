// Copyright 2015 The Bazel Authors. All rights reserved.
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

import com.google.devtools.build.lib.rules.repository.RepositoryFunction.RepositoryFunctionException;
import com.google.devtools.build.lib.vfs.DigestHashFunction;
import com.google.devtools.build.lib.vfs.FileSystem;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.inmemoryfs.InMemoryFileSystem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link DecompressorValue}.
 */
@RunWith(JUnit4.class)
public class DecompressorValueTest {

  private final FileSystem fs = new InMemoryFileSystem(DigestHashFunction.SHA256);

  @Test
  public void testKnownFileExtensionsDoNotThrow() throws Exception {
    Path path = fs.getPath("/foo/.external-repositories/some-repo/bar.zip");
    DecompressorDescriptor.builder().setArchivePath(path).build();
    path = fs.getPath("/foo/.external-repositories/some-repo/bar.jar");
    DecompressorDescriptor.builder().setArchivePath(path).build();
    path = fs.getPath("/foo/.external-repositories/some-repo/bar.baz.zip");
    DecompressorDescriptor.builder().setArchivePath(path).build();
    path = fs.getPath("/foo/.external-repositories/some-repo/bar.baz.tar.gz");
    DecompressorDescriptor.builder().setArchivePath(path).build();
    path = fs.getPath("/foo/.external-repositories/some-repo/bar.baz.tgz");
    DecompressorDescriptor.builder().setArchivePath(path).build();
    path = fs.getPath("/foo/.external-repositories/some-repo/bar.baz.tar.xz");
    DecompressorDescriptor.builder().setArchivePath(path).build();
    path = fs.getPath("/foo/.external-repositories/some-repo/bar.baz.txz");
    DecompressorDescriptor.builder().setArchivePath(path).build();
    path = fs.getPath("/foo/.external-repositories/some-repo/bar.baz.tar.bz2");
    DecompressorDescriptor.builder().setArchivePath(path).build();
  }

  @Test
  public void testUnknownFileExtensionsThrow() throws Exception {
    Path zipPath = fs.getPath("/foo/.external-repositories/some-repo/bar.baz");
    RepositoryFunctionException expected =
        assertThrows(
            RepositoryFunctionException.class,
            () -> DecompressorDescriptor.builder().setArchivePath(zipPath).build());
    assertThat(expected).hasMessageThat().contains("Expected a file with a .zip, .jar,");
  }

}
