// Copyright 2026 Very Good Security Authors
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

package com.verygood.security.larky.modules.multidict;

import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.parser.EvaluationSettings;
import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyScript;
import org.junit.Test;

/**
 * The multidict's loops run natively, so they charge the evaluation's step budget for the entries
 * they visit: a lookup in a large multidict costs more steps than one in a small multidict.
 */
public class LarkyMultiDictStepsTest {

  private static long steps(int size, int lookups) throws Exception {
    String src = String.join("\n",
        "load('@vendor//multidict', 'CIMultiDict')",
        "d = CIMultiDict([('k%d' % i, 'v') for i in range(" + size + ")])",
        "def lookups():",
        "    for _ in range(" + lookups + "):",
        "        x = 'missing' in d",
        "lookups()",
        "");
    LarkyScript script =
        new LarkyScript(
            ModuleSupplier.CORE_MODULES,
            LarkyScript.StarlarkMode.STRICT,
            ImmutableMap.of(),
            EvaluationSettings.NONE);
    return script
        .evaluate(InMemMapBackedStarFile.createStarFile("steps.star", src), new TestingConsole())
        .metadata()
        .executedSteps();
  }

  @Test
  public void lookupsChargeOneStepPerEntryVisited() throws Exception {
    long small = steps(10, 100);
    long large = steps(1010, 100);
    // 100 lookups that each visit 1,000 more entries, plus the 1,000 more entries added
    long extra = large - small;
    assertTrue("extra steps: " + extra, extra >= 100 * 1000);
  }
}
