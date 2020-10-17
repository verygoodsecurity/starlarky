// Copyright 2018 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.actions;

import javax.annotation.Nullable;

/**
 * Provides a method for registering associations between {@link ActionInput}s, their {@link
 * FileArtifactValue}s, and (optionally) the {@link Artifact} dependency responsible for their
 * inclusion in an action's inputs.
 */
public interface ActionInputMapSink {

  /** Returns true if an entry was added, false if the map already contains {@code input}. */
  boolean put(ActionInput input, FileArtifactValue metadata, @Nullable Artifact depOwner);
}
