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

import com.verygood.security.larky.parser.ParsedStarFile;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import net.starlark.java.eval.Sequence;
import org.junit.Test;

/**
 * A binding named like a native module (a host binds its own {@code vault}) does not stand in for the module in a
 * {@code load()}: the loader takes an environment entry only when it is a {@code Module}.
 */
public class EnvironmentBindingLoadTest {

  private static Sequence<?> run(String script) throws Exception {
    final Bindings bindings = new SimpleBindings();
    bindings.put("vault", "the host's binding");
    final ParsedStarFile result = (ParsedStarFile) new LarkyScriptEngine().eval(script, bindings);
    return result.getGlobalEnvironmentVariable("out", Sequence.class);
  }

  @Test
  public void aBindingNamedLikeTheModuleDoesNotShadowTheModuleInALoad() throws Exception {
    assertThat(run("load('@vgs//vault', v='vault')\nout = [type(v), vault]\n"))
        .containsExactly("vault", "the host's binding")
        .inOrder();
  }

  @Test
  public void theLoadedModuleWorksUnderTheNameOfTheBinding() throws Exception {
    // the load binds `vault` in the script's own scope, in front of the predeclared binding of the same name
    assertThat(run("load('@vgs//vault', 'vault')\nout = [type(vault)]\n")).containsExactly("vault");
  }

  @Test
  public void aModuleThatLoadsTheVaultModuleItselfLoadsWithTheBindingPresent() throws Exception {
    // nts.star loads @vgs//vault; with the host's binding in the environment that load used to fail
    assertThat(run("load('@vgs//nts', 'nts')\nout = [type(nts), vault]\n"))
        .containsExactly("ImmutableStruct", "the host's binding")
        .inOrder();
  }
}
