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
package com.google.devtools.build.lib.analysis.testing;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.devtools.build.lib.analysis.testing.ToolchainContextSubject.toolchainContexts;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.Subject;
import com.google.devtools.build.lib.analysis.ToolchainCollection;

/** A Truth {@link Subject} for {@link ToolchainCollection}. */
public class ToolchainCollectionSubject extends Subject {
  // Static data.

  /** Entry point for test assertions related to {@link ToolchainCollection}. */
  public static ToolchainCollectionSubject assertThat(ToolchainCollection<?> toolchainCollection) {
    return assertAbout(TOOLCHAIN_COLLECTION_SUBJECT_FACTORY).that(toolchainCollection);
  }

  /** Static method for getting the subject factory (for use with assertAbout()). */
  public static Subject.Factory<ToolchainCollectionSubject, ToolchainCollection<?>>
      toolchainCollections() {
    return TOOLCHAIN_COLLECTION_SUBJECT_FACTORY;
  }

  private static final Subject.Factory<ToolchainCollectionSubject, ToolchainCollection<?>>
      TOOLCHAIN_COLLECTION_SUBJECT_FACTORY = ToolchainCollectionSubject::new;

  // Instance fields.

  private final ToolchainCollection<?> actual;

  private ToolchainCollectionSubject(
      FailureMetadata failureMetadata, ToolchainCollection<?> subject) {
    super(failureMetadata, subject);
    this.actual = subject;
  }

  public void hasDefaultExecGroup() {
    check("hasToolchainContext()")
        .that(actual.hasToolchainContext(ToolchainCollection.DEFAULT_EXEC_GROUP_NAME))
        .isTrue();
  }

  public ToolchainContextSubject defaultToolchainContext() {
    return check("defaultToolchainContext()")
        .about(toolchainContexts())
        .that(actual.getDefaultToolchainContext());
  }

  public IterableSubject resolvedToolchains() {
    return check("getResolvedToolchains()").that(actual.getResolvedToolchains());
  }

  public void hasExecGroup(String execGroup) {
    check("hasToolchainContext(%s)", execGroup)
        .that(actual.getToolchainContext(execGroup))
        .isNotNull();
  }

  public ToolchainContextSubject execGroup(String execGroup) {
    return check("getToolchainContext(%s)", execGroup)
        .about(toolchainContexts())
        .that(actual.getToolchainContext(execGroup));
  }
}
