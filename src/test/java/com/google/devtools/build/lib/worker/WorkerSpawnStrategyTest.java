// Copyright 2017 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.worker;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.vfs.FileSystem;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.util.FileSystems;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for the {@link WorkerSpawnStrategy}. */
@RunWith(JUnit4.class)
public class WorkerSpawnStrategyTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();
  private final FileSystem fs = FileSystems.getNativeFileSystem();

  @Test
  public void expandArgumentsPreservesEmptyLines() throws Exception {
    File flagfile = folder.newFile("flagfile.txt");

    ImmutableList<String> flags = ImmutableList.of("--hello", "", "--world");

    try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(flagfile.toPath(), UTF_8))) {
      flags.forEach(pw::println);
    }

    Path execRoot = fs.getPath(folder.getRoot().getAbsolutePath());
    WorkRequest.Builder requestBuilder = WorkRequest.newBuilder();
    WorkerSpawnRunner.expandArgument(execRoot, "@flagfile.txt", requestBuilder);

    assertThat(requestBuilder.getArgumentsList()).containsExactlyElementsIn(flags);
  }
}
