package com.verygood.security.larky.jsr223;

import static javax.script.ScriptContext.ENGINE_SCOPE;
import static javax.script.ScriptContext.GLOBAL_SCOPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.parser.EvaluationSettings;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import org.junit.Test;

public class LarkyScriptContextTest {

  @Test
  public void compiledHostCanSupplySettingsAndWriterDirectly() throws Exception {
    var writer = new StringWriter();
    var settings = EvaluationSettings.builder().stepLimit(1000).build();
    var context = LarkyScriptContext.builder()
        .settings(settings)
        .writer(writer)
        .build();
    context.setAttribute("message", "hello", ENGINE_SCOPE);
    var compiled = new LarkyScriptEngine().compile("print(message)\n", "context.star");

    new LarkyScriptEvaluator().compute(compiled, context, null, (run, unused) -> null);

    assertSame(writer, context.getWriter());
    assertSame(settings, context.getSettings());
    assertEquals("hello" + System.lineSeparator(), writer.toString());
  }

  @Test
  public void builderKeepsContextDefaultsAndOmitsConfigurationFromToString() {
    var builder = LarkyScriptContext.builder();
    var context = builder.build();
    assertSame(EvaluationSettings.NONE, context.getSettings());
    assertEquals(List.of(ENGINE_SCOPE, GLOBAL_SCOPE), context.getScopes());

    var writer = new StringWriter();
    writer.write("sample request log");
    builder.writer(writer);
    assertEquals("LarkyScriptContext.Builder", builder.toString());
  }

  @Test
  public void engineBindingsShadowGlobalsEvenWithNullValues() {
    ScriptContext context = new LarkyScriptContext();
    context.setAttribute("ignored", "value", GLOBAL_SCOPE);
    assertNull(context.getAttribute("ignored"));
    assertEquals(List.of(ENGINE_SCOPE, GLOBAL_SCOPE), context.getScopes());
    context.setBindings(new SimpleBindings(Map.of("name", "global")), GLOBAL_SCOPE);
    context.setAttribute("name", null, ENGINE_SCOPE);

    assertEquals(ENGINE_SCOPE, context.getAttributesScope("name"));
    assertNull(context.getAttribute("name"));
    assertEquals("global", context.getAttribute("name", GLOBAL_SCOPE));
    context.removeAttribute("name", ENGINE_SCOPE);
    assertEquals("global", context.getAttribute("name"));
    assertThrows(NullPointerException.class, () -> context.setBindings(null, ENGINE_SCOPE));
    assertThrows(IllegalArgumentException.class, () -> context.getBindings(-1));
    assertThrows(IllegalArgumentException.class, () -> context.getAttribute("", GLOBAL_SCOPE));
  }

  @Test
  public void streamSettersReplaceDefaultsIncludingWithNull() {
    ScriptContext context = new LarkyScriptContext();
    assertSame(context.getReader(), context.getReader());
    assertSame(context.getWriter(), context.getWriter());
    assertSame(context.getErrorWriter(), context.getErrorWriter());

    var reader = new StringReader("script source");
    var writer = new StringWriter();
    var errorWriter = new StringWriter();
    context.setReader(reader);
    context.setWriter(writer);
    context.setErrorWriter(errorWriter);
    assertSame(reader, context.getReader());
    assertSame(writer, context.getWriter());
    assertSame(errorWriter, context.getErrorWriter());

    context.setReader(null);
    context.setWriter(null);
    context.setErrorWriter(null);
    assertNull(context.getReader());
    assertNull(context.getWriter());
    assertNull(context.getErrorWriter());
  }
}
