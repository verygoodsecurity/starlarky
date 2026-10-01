package com.verygood.security.larky.jsr223;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.parser.ParsedStarFile;
import java.util.Map;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.junit.Test;

/** Tests of {@link LarkyScriptEngine#MODULES} and {@link LarkyScriptEngine#CACHE_NAMESPACE}. */
public class LarkyScriptEngineModulesTest {

  // A host script and a customer script that both define `helper`, as a service wrapping
  // customer code would.
  private static final String CUSTOMER =
      String.join(
          "\n",
          "def helper(x):",
          "    return 'customer ' + x",
          "",
          "def process(x):",
          "    return helper(x)",
          "");

  private static final String HOST =
      String.join(
          "\n",
          "load('larky', customer_process='process')",
          "",
          "def helper(x):",
          "    return 'host ' + x",
          "",
          "output = customer_process('input')",
          "");

  private static ParsedStarFile eval(String host, Map<String, String> modules, String namespace)
      throws ScriptException {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    SimpleScriptContext context = new SimpleScriptContext();
    context.setAttribute(LarkyScriptEngine.MODULES, modules, ScriptContext.ENGINE_SCOPE);
    context.setAttribute(LarkyScriptEngine.SCRIPT_NAME, "host.star", ScriptContext.ENGINE_SCOPE);
    if (namespace != null) {
      context.setAttribute(LarkyScriptEngine.CACHE_NAMESPACE, namespace, ScriptContext.ENGINE_SCOPE);
    }
    engine.setContext(context);
    return (ParsedStarFile) engine.eval(host, new SimpleBindings());
  }

  @Test
  public void loadedModuleKeepsItsOwnTopLevelNames() throws Exception {
    // Pasted into one file, the host's later `helper` would replace the customer's.
    ParsedStarFile result = eval(HOST, Map.of("larky.star", CUSTOMER), null);
    assertThat(result.getGlobalEnvironmentVariable("output", String.class))
        .isEqualTo("customer input");
  }

  @Test
  public void errorsReportTheLoadedFileAndItsOwnLines() {
    // Line 3 of the customer's file, not offset by the host script's lines.
    String customer = String.join("\n", "def process(x):", "    d = {}", "    return d['missing']", "");
    ScriptException e =
        assertThrows(
            ScriptException.class, () -> eval(HOST, Map.of("larky.star", customer), null));
    assertThat(e.getMessage()).contains("larky.star at line number 3");
  }

  @Test
  public void errorsInBuiltinsReportTheScriptsLine() {
    // int() raises inside a built-in; the error points at the script's call, not "<builtin>" 0.
    String customer = String.join("\n", "def process(x):", "    y = 1", "    return int('not a number')", "");
    ScriptException e =
        assertThrows(
            ScriptException.class, () -> eval(HOST, Map.of("larky.star", customer), null));
    assertThat(e.getMessage()).contains("larky.star at line number 3");
  }

  @Test
  public void errorsInLarkyModulesReportTheScriptsLine() {
    // b64decode raises inside stdlib/base64.star; the error points at the script's call.
    String customer =
        String.join(
            "\n",
            "load('@stdlib//base64', 'base64')",
            "def process(x):",
            "    return base64.b64decode(123)",
            "");
    ScriptException e =
        assertThrows(
            ScriptException.class, () -> eval(HOST, Map.of("larky.star", customer), null));
    assertThat(e.getMessage()).contains("larky.star at line number 3");
  }

  @Test
  public void compileErrorsInTheLoadedFileAreScriptErrorsWithItsLines() {
    String customer = String.join("\n", "def process(x):", "    return undefined_name", "");
    ScriptException e =
        assertThrows(
            ScriptException.class, () -> eval(HOST, Map.of("larky.star", customer), null));
    assertThat(e.getMessage()).contains("larky.star:2:12: name 'undefined_name' is not defined");
  }

  @Test
  public void missingModuleFails() {
    // As for any load() of a file that does not exist, this is not wrapped in a ScriptException.
    Exception e =
        assertThrows(Exception.class, () -> eval(HOST, Map.of("other.star", CUSTOMER), null));
    assertThat(e.getMessage()).contains("larky.star");
  }

  @Test
  public void cacheNamespaceSeparatesIdenticalScripts() throws Exception {
    // Scripts no other test evaluates, so neither is cached yet.
    String host = HOST + "\nhost_marker = 'namespaces'\n";
    String customer = CUSTOMER + "\nmarker = 'namespaces'\n";
    long before = com.verygood.security.larky.parser.ProgramCacheAccess.scriptCount();
    eval(host, Map.of("larky.star", customer), "tenant-a");
    eval(host, Map.of("larky.star", customer), "tenant-a");
    long one = com.verygood.security.larky.parser.ProgramCacheAccess.scriptCount();
    eval(host, Map.of("larky.star", customer), "tenant-b");
    long two = com.verygood.security.larky.parser.ProgramCacheAccess.scriptCount();
    // The host script is cached once; the customer file once per namespace.
    assertThat(one - before).isEqualTo(2);
    assertThat(two - one).isEqualTo(1);
  }
}
