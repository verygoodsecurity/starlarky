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

import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.junit.Test;

/** A native module loads only from its own namespace (issue #111). */
public class NativeModuleNamespaceTest {

  private static void eval(String script) throws ScriptException {
    new LarkyScriptEngine().eval(script, new SimpleBindings());
  }

  @Test
  public void loadsFromItsOwnNamespace() throws Exception {
    eval("load('@stdlib//json', 'json')\nx = json.dumps({'a': 1})\n");
    eval("load('@stdlib/re2j', 're2j')\n");
    eval("load('@vgs//vault', 'vault')\n");
  }

  @Test
  public void stdlibModuleFromAnotherNamespaceFails() {
    ScriptException e =
        assertThrows(ScriptException.class, () -> eval("load('@vgs//json', 'json')\n"));
    assertThat(e.getMessage())
        .contains("cannot load '@vgs//json': json is a @stdlib module; load it as '@stdlib//json'");
    e = assertThrows(ScriptException.class, () -> eval("load('@vendor//re2j', 're2j')\n"));
    assertThat(e.getMessage()).contains("load it as '@stdlib//re2j'");
  }

  @Test
  public void vgsModuleFromAnotherNamespaceFails() {
    ScriptException e =
        assertThrows(ScriptException.class, () -> eval("load('@stdlib//vault', 'vault')\n"));
    assertThat(e.getMessage())
        .contains("cannot load '@stdlib//vault': vault is a @vgs module; load it as '@vgs//vault'");
  }

  @Test
  public void nativeHttpResponseFactoryIsVgsOnly() throws Exception {
    eval("load('@vgs//jhttpresponse', 'jhttpresponse')\nbuild = jhttpresponse.create\n");

    ScriptException wrongNamespace = assertThrows(ScriptException.class,
        () -> eval("load('@stdlib//jhttpresponse', 'jhttpresponse')\n"));
    assertThat(wrongNamespace.getMessage()).contains("load it as '@vgs//jhttpresponse'");

    ScriptException missingGlobal = assertThrows(ScriptException.class, () -> eval("build = _http_response\n"));
    assertThat(missingGlobal.getMessage()).contains("name '_http_response' is not defined");
  }

  @Test
  public void nativeHttpRequestFactoryIsVgsOnly() throws Exception {
    eval("load('@vgs//jhttprequest', 'jhttprequest')\nbuild = jhttprequest.VGSHttpRequest\n");

    ScriptException wrongNamespace = assertThrows(ScriptException.class,
        () -> eval("load('@stdlib//jhttprequest', 'jhttprequest')\n"));
    assertThat(wrongNamespace.getMessage()).contains("load it as '@vgs//jhttprequest'");
  }
}
