/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.jsr223;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import javax.script.SimpleBindings;
import org.junit.Test;

/**
 * Loading a test-only module outside a test says so, instead of reporting it missing (issue #120).
 */
public class TestOnlyModuleLoadTest {

  // A failed load surfaces as the loader's RuntimeException, as for any other missing module.
  private static String loadError(String script) {
    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () -> new LarkyScriptEngine().eval(script, new SimpleBindings()));
    return e.getMessage();
  }

  @Test
  public void unittestIsTestOnly() {
    String message = loadError("load('@stdlib//unittest', 'unittest')\n");
    assertThat(message)
        .contains(
            "cannot load '@stdlib//unittest': unittest is a test-only module,"
                + " available only to test scripts");
    assertThat(message).doesNotContain("Unable to find resource");
  }

  @Test
  public void assertionsIsTestOnly() {
    String message = loadError("load('@stdlib//assertions', 'assertions')\n");
    assertThat(message)
        .contains(
            "cannot load '@stdlib//assertions': assertions is a test-only module,"
                + " available only to test scripts");
    assertThat(message).doesNotContain("Unable to find resource");
  }

  @Test
  public void vendorAssertsNamesTheTestOnlyModuleItNeeds() {
    String message = loadError("load('@vendor//asserts', 'asserts')\n");
    assertThat(message).contains("from module: vendor/asserts.star");
    assertThat(message)
        .contains("cannot load '@stdlib//assertions': assertions is a test-only module");
    assertThat(message).doesNotContain("Unable to find resource");
  }

  @Test
  public void missingModuleIsStillReportedMissing() {
    String message = loadError("load('@stdlib//no_such_module', 'x')\n");
    assertThat(message).contains("Unable to find resource: stdlib/no_such_module.star");
    assertThat(message).doesNotContain("test-only");
  }
}
