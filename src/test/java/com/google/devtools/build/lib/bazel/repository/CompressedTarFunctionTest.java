// Copyright 2016 The Bazel Authors. All rights reserved.
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

import static com.google.devtools.build.lib.bazel.repository.TestArchiveDescriptor.INNER_FOLDER_NAME;
import static com.google.devtools.build.lib.bazel.repository.TestArchiveDescriptor.ROOT_FOLDER_NAME;

import com.google.devtools.build.lib.vfs.Path;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests decompressing archives. */
@RunWith(JUnit4.class)
public class CompressedTarFunctionTest {
  /* Tarball, created by "tar -czf <ARCHIVE_NAME> <files...>" */
  private static final String ARCHIVE_NAME = "test_decompress_archive.tar.gz";

  private TestArchiveDescriptor archiveDescriptor;

  @Before
  public void setUpFs() throws Exception {
    archiveDescriptor = new TestArchiveDescriptor(ARCHIVE_NAME, "out", true);
  }

  /**
   * Test decompressing a tar.gz file with hard link file and symbolic link file inside without
   * stripping a prefix
   */
  @Test
  public void testDecompressWithoutPrefix() throws Exception {
    Path outputDir = decompress(archiveDescriptor.createDescriptorBuilder());

    archiveDescriptor.assertOutputFiles(outputDir, ROOT_FOLDER_NAME, INNER_FOLDER_NAME);
  }

  /**
   * Test decompressing a tar.gz file with hard link file and symbolic link file inside and
   * stripping a prefix
   */
  @Test
  public void testDecompressWithPrefix() throws Exception {
    DecompressorDescriptor.Builder descriptorBuilder =
        archiveDescriptor.createDescriptorBuilder().setPrefix(ROOT_FOLDER_NAME);
    Path outputDir = decompress(descriptorBuilder);

    archiveDescriptor.assertOutputFiles(outputDir, INNER_FOLDER_NAME);
  }

  private Path decompress(DecompressorDescriptor.Builder descriptorBuilder) throws Exception {
    descriptorBuilder.setDecompressor(TarGzFunction.INSTANCE);
    return new CompressedTarFunction() {
      @Override
      protected InputStream getDecompressorStream(DecompressorDescriptor descriptor)
          throws IOException {
        return new GZIPInputStream(new FileInputStream(descriptor.archivePath().getPathFile()));
      }
    }.decompress(descriptorBuilder.build());
  }
}
