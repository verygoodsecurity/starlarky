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
package com.google.devtools.build.lib.actions.cache;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.FileArtifactValue;
import com.google.devtools.build.lib.skyframe.TreeArtifactInjector;

/** Supports metadata injection of action outputs into skyframe. */
public interface MetadataInjector extends TreeArtifactInjector {

  /**
   * Injects the metadata of a file.
   *
   * <p>This can be used to save filesystem operations when the metadata is already known.
   *
   * <p>{@linkplain Artifact#isTreeArtifact Tree artifacts} and their {@linkplain
   * Artifact#isChildOfDeclaredDirectory children} must not be passed here. Instead, they should be
   * passed to {@link #injectTree}.
   *
   * @param output a regular output file
   * @param metadata the file metadata
   */
  void injectFile(Artifact output, FileArtifactValue metadata);
}
