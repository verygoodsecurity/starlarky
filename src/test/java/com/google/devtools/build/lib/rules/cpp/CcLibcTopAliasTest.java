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

package com.google.devtools.build.lib.rules.cpp;

import static com.google.common.truth.Truth.assertThat;

import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.util.BuildViewTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for cc_libc_top_alias rule. */
@RunWith(JUnit4.class)
public class CcLibcTopAliasTest extends BuildViewTestCase {

  @Test
  public void testCcLibcTopAlias() throws Exception {
    scratch.file("a/BUILD", "cc_libc_top_alias(name='current_cc_libc_top')");

    ConfiguredTarget target = getConfiguredTarget("//a:current_cc_libc_top");

    assertThat(target.getLabel().toString()).isEqualTo("//a:current_cc_libc_top");
  }


  @Test
  public void testCcLibcTopAliasWithGrteTopArgument() throws Exception {
    scratch.file("a/BUILD", "cc_libc_top_alias(name='current_cc_libc_top')");
    scratch.file("b/BUILD",
        "filegroup(",
        "    name = 'everything',",
        "    srcs = []",
        ")");
    //value of this property replaced to :everything in {@code LibcTopLabelConverter}
    useConfiguration("--grte_top=//b:some_string");

    ConfiguredTarget target = getConfiguredTarget("//a:current_cc_libc_top");

    assertThat(target.getLabel().toString()).isEqualTo("//b:everything");
  }
}
