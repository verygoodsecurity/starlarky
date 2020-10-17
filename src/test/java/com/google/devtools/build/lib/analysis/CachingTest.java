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

import com.google.devtools.build.lib.actions.Action;
import com.google.devtools.build.lib.actions.ActionInput;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.actions.SpawnAction;
import com.google.devtools.build.lib.analysis.util.BuildViewTestCase;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests to verify that Bazel actions don't poison any output cache. */
@RunWith(JUnit4.class)
public class CachingTest extends BuildViewTestCase {
  /**
   * Regression test for bugs #2317593 and #2284024: Don't expand runfile middlemen.
   */
  @Test
  public void testRunfilesManifestNotAnInput() throws Exception {
    scratch.file(
        "x/BUILD",
        "sh_binary(name = 'tool', srcs = ['tool.sh'], data = ['tool.data'])",
        "genrule(name = 'x', tools = [':tool'], outs = ['x.out'], cmd = 'dummy')");

    Set<Action> actions = new HashSet<>();
    for (Artifact artifact : getFilesToBuild(getConfiguredTarget("//x:x")).toList()) {
      actions.add(getGeneratingAction(artifact));
    }

    boolean lookedAtAnyAction = false;
    boolean foundRunfilesMiddlemanSoRunfilesAreCorrectlyStaged = false;
    for (Action action : actions) {
      if (action instanceof SpawnAction) {
        for (ActionInput string : ((SpawnAction) action).getSpawn().getInputFiles().toList()) {
          lookedAtAnyAction = true;
          if (string.getExecPathString().endsWith("x_Stool-runfiles")) {
            foundRunfilesMiddlemanSoRunfilesAreCorrectlyStaged = true;
          } else {
            assertThat(string.getExecPathString().endsWith(".runfiles/MANIFEST")).isFalse();
          }
        }
      }
    }
    assertThat(lookedAtAnyAction).isTrue();
    assertThat(foundRunfilesMiddlemanSoRunfilesAreCorrectlyStaged).isTrue();
  }
}
