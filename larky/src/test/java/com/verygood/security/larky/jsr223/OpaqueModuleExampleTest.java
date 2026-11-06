/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.io.BaseEncoding;
import com.verygood.security.larky.modules.VaultModule;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Example (issue #100): a host ships a module it owns next to a customer's script, through
 * {@link LarkyScriptEngine#MODULES}, and the module keeps a vault alias where the script cannot
 * load it.
 *
 * <p>The module {@code vgs_owned.star} keeps the alias of an HMAC key in {@code _KEY_ALIAS}. Names
 * that start with {@code _} are private to their file: a {@code load()} of one fails when the
 * script is compiled ({@code FileOptions.allowLoadPrivateSymbols} is false for Larky scripts). The
 * module's public {@code sign(message)} reveals the key from the vault and returns an HMAC, never
 * the alias or the key.
 *
 * <p>What this does and does not hide:
 *
 * <ul>
 *   <li>The alias stays hidden only while no public function returns it, a value derived from it,
 *       or an error message that contains it. Hiding a name is not access control.
 *   <li>The script can call {@code sign} with any message as often as it likes: the module is an
 *       oracle for whatever its public functions do.
 *   <li>The script's own {@code vault.reveal} works on any alias it learns, so the alias must not
 *       reach it by another route (logs, responses, other modules).
 *   <li>Storing a module per tenant, choosing which one to inject next to which script, and keeping
 *       tenants apart are the host's job. {@link LarkyScriptEngine#CACHE_NAMESPACE} only keeps
 *       compiled modules of different tenants apart in Larky's cache.
 * </ul>
 *
 * <p>The vault here is the in-memory {@code DefaultVault} that the test resources register; a host
 * would use its own vault and an alias it already holds.
 */
public class OpaqueModuleExampleTest {

  private static final String KEY = "example-hmac-key-0123456789abcdef";
  private static String keyAlias;

  /** The module the host owns; the alias is filled in by the host (here, by the test). */
  private static String vgsOwnedModule(String alias) {
    return String.join(
        "\n",
        "load('@stdlib//hashlib', 'hashlib')",
        "load('@vendor//Crypto/Hash/HMAC', 'HMAC')",
        "load('@vgs//vault', 'vault')",
        "",
        // Starlark.repr quotes and escapes the alias, so any string is safe to embed.
        "_KEY_ALIAS = " + Starlark.repr(alias, StarlarkSemantics.DEFAULT),
        "",
        "def _key():",
        "    return bytes(vault.reveal(_KEY_ALIAS), encoding='utf-8')",
        "",
        "def sign(message):",
        "    \"\"\"Hex HMAC-SHA256 of message (a string) under the hidden key.\"\"\"",
        "    if type(message) != 'string':",
        "        fail('sign: message must be a string, got %s' % type(message))",
        "    return HMAC.new(_key(), bytes(message, encoding='utf-8'), hashlib.sha256()).hexdigest()",
        "");
  }

  @BeforeClass
  public static void storeKeyInVault() throws Exception {
    keyAlias =
        (String)
            VaultModule.INSTANCE
                .getVault()
                .redact(KEY, Starlark.NONE, Starlark.NONE, List.of(), Starlark.NONE);
  }

  private static ParsedStarFile run(String customerScript) throws ScriptException {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    SimpleScriptContext context = new SimpleScriptContext();
    context.setAttribute(
        LarkyScriptEngine.MODULES,
        Map.of("vgs_owned.star", vgsOwnedModule(keyAlias)),
        ScriptContext.ENGINE_SCOPE);
    context.setAttribute(
        LarkyScriptEngine.CACHE_NAMESPACE, "tenant-example", ScriptContext.ENGINE_SCOPE);
    engine.setContext(context);
    return (ParsedStarFile) engine.eval(customerScript, new SimpleBindings());
  }

  private static String hmacSha256Hex(String key, String message) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return BaseEncoding.base16()
        .lowerCase()
        .encode(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void customerScriptCallsThePublicFunction() throws Exception {
    String customer =
        String.join(
            "\n",
            "load('vgs_owned', 'sign')",
            "",
            "def process(message):",
            "    return sign(message)",
            "",
            "output = process('order-1234')",
            "");
    String output = run(customer).getGlobalEnvironmentVariable("output", String.class);
    // The module used the key behind the hidden alias...
    assertThat(output).isEqualTo(hmacSha256Hex(KEY, "order-1234"));
    // ...and returned neither the alias nor the key.
    assertThat(output).doesNotContain(keyAlias);
    assertThat(output).doesNotContain(KEY);
  }

  @Test
  public void customerScriptCannotLoadThePrivateAlias() {
    String customer = String.join("\n", "load('vgs_owned', '_KEY_ALIAS')", "output = _KEY_ALIAS", "");
    ScriptException e = assertThrows(ScriptException.class, () -> run(customer));
    assertThat(e.getMessage())
        .contains("larky.star:1:20: symbol '_KEY_ALIAS' is private and cannot be imported");
    assertThat(e.getMessage()).doesNotContain(keyAlias);
  }

  @Test
  public void customerScriptCannotLoadThePrivateHelper() {
    // Every `_` name is private, functions included.
    String customer = String.join("\n", "load('vgs_owned', key='_key')", "output = key()", "");
    ScriptException e = assertThrows(ScriptException.class, () -> run(customer));
    assertThat(e.getMessage()).contains("symbol '_key' is private and cannot be imported");
  }

  @Test
  public void errorsInThePublicFunctionDoNotShowTheAlias() {
    String customer = String.join("\n", "load('vgs_owned', 'sign')", "output = sign(123)", "");
    ScriptException e = assertThrows(ScriptException.class, () -> run(customer));
    assertThat(e.getMessage()).contains("sign: message must be a string, got int");
    assertThat(e.getMessage()).doesNotContain(keyAlias);
    assertThat(e.getMessage()).doesNotContain(KEY);
  }

  @Test
  public void publicFunctionIsAnOracle() throws Exception {
    // Opacity does not stop the script from using the function: it can sign any message.
    String customer =
        String.join(
            "\n",
            "load('vgs_owned', 'sign')",
            "output = [sign(m) for m in ['a', 'b', 'chosen by the script']]",
            "");
    List<?> output = run(customer).getGlobalEnvironmentVariable("output", List.class);
    assertThat(output)
        .containsExactly(
            hmacSha256Hex(KEY, "a"),
            hmacSha256Hex(KEY, "b"),
            hmacSha256Hex(KEY, "chosen by the script"))
        .inOrder();
  }
}
