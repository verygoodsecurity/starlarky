package com.verygood.security.larky.browser;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeNotNull;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.StarlarkBytes;
import org.junit.Test;

public class KitesurfBrowserTest {
  private static final URI LOCAL_ENDPOINT = URI.create("ws://127.0.0.1:8787/devtools/browser");

  @Test
  public void rejectsEndpointsOutsideTheLocalEngine() {
    assertThrows(IllegalArgumentException.class,
        () -> new KitesurfBrowser(URI.create("ws://example.com:8787/devtools/browser")));
    assertThrows(IllegalArgumentException.class,
        () -> new KitesurfBrowser(URI.create("ws://127.0.0.1:8787/another-path")));
  }

  @Test
  public void rejectsOversizedHtmlBeforeConnecting() throws Exception {
    try (var browser = new KitesurfBrowser(LOCAL_ENDPOINT)) {
      var engine = new LarkyScriptEngine();
      var bindings = new SimpleBindings(Map.of("browser", browser, "html", "a".repeat(300_000)));
      ScriptException error = assertThrows(
          ScriptException.class, () -> engine.eval("result = browser.render(html)", bindings));
      assertThat(error).hasMessageThat().contains("HTML exceeds");
    }
  }

  @Test
  public void rendersAnInlinePageThroughTheLarkyFunction() throws Exception {
    String endpoint = System.getProperty("larky.kitesurf.testEndpoint");
    assumeNotNull(endpoint);
    try (var browser = new KitesurfBrowser(URI.create(endpoint))) {
      var engine = new LarkyScriptEngine();
      var bindings = new SimpleBindings(new java.util.HashMap<>(Map.of("browser", browser)));
      var result = (ParsedStarFile) engine.eval("""
          html = "<p id='fixture'>initial</p><script>document.getElementById('fixture').textContent = 'Rendered by JavaScript'</script>"
          result = browser.render(html)
          """, bindings);
      Dict<?, ?> page = result.getGlobalEnvironmentVariable("result", Dict.class);
      assertThat(page.get("text")).isEqualTo("Rendered by JavaScript");
      ByteBuffer png = ByteBuffer.wrap(((StarlarkBytes) page.get("png")).toByteArray());
      assertThat(png.getLong()).isEqualTo(0x89504e470d0a1a0aL);
      assertThat(png.getInt(16)).isEqualTo(800);
      assertThat(png.getInt(20)).isEqualTo(600);
    }
  }
}
