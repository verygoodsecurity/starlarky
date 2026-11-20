package com.verygood.security.larky.jsr223;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.verygood.security.larky.modules.vgs.ModuleProviders;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import com.verygood.security.larky.modules.vgs.vault.spi.RevealOutcome;
import com.verygood.security.larky.parser.DefaultLarkyInterpreter;
import com.verygood.security.larky.parser.LarkyEvaluator;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import net.starlark.java.eval.DiagnosticRedactor;
import net.starlark.java.eval.DiagnosticRedactor.Mode;
import net.starlark.java.eval.StarlarkBytes;
import org.junit.jupiter.api.Test;

class VaultDiagnosticRedactionTest {

  private static final String SECRET = "synthetic vault value";
  private static final String REVEAL = """
      load('@vgs//vault', 'vault')
      secret = vault.reveal(vault.redact('synthetic vault value'))
      """;

  @Test
  void masksKnownVaultValuesInPrintWithoutModifyingScriptResults() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));

    engine.eval(REVEAL + "print('value', secret, 'done', sep=' | ')\n");
    assertEquals("value | ***REDACTED*** | done", output.toString().strip());
    assertEquals(SECRET, engine.get("secret"));
  }

  @Test
  void usesTheProvidersOutcomeForHitsAndMisses() throws Exception {
    LarkyVault vault = mock(LarkyVault.class);
    when(vault.revealOutcome(eq("missing alias"), any()))
        .thenAnswer(call -> RevealOutcome.miss(new String((String) call.getArgument(0))));
    when(vault.revealOutcome(eq("stored value"), any()))
        .thenAnswer(call -> RevealOutcome.hit(call.getArgument(0)));

    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.put(LarkyEvaluator.MODULE_PROVIDERS, ModuleProviders.of().with(LarkyVault.class, vault));
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));
    engine.eval("""
        load('@vgs//vault', 'vault')
        print(vault.reveal('missing alias'))
        print(vault.reveal('stored value'))
        """);

    assertEquals(List.of("missing alias", "***REDACTED***"), output.toString().lines().toList());
  }

  @Test
  void masksOnlyTheHitsInAMixedBatchAndPreservesTheReturnValue() throws Exception {
    LarkyVault vault = mock(LarkyVault.class);
    List<Object> values = List.of("missing alias", SECRET);
    when(vault.revealOutcome(any(), any()))
        .thenReturn(new RevealOutcome(values, List.of(SECRET)));

    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.put(LarkyEvaluator.MODULE_PROVIDERS, ModuleProviders.of().with(LarkyVault.class, vault));
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));
    engine.eval("""
        load('@vgs//vault', 'vault')
        revealed = vault.reveal(['missing alias', 'mapped alias'])
        print(revealed)
        """);

    assertEquals(values, List.copyOf((List<?>) engine.get("revealed")));
    assertEquals("[\"missing alias\", \"***REDACTED***\"]", output.toString().strip());
  }

  @Test
  void observeReportsMatchesAndKeepsTheDiagnostic() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    List<DiagnosticRedactor.Sink> hits = new ArrayList<>();
    engine.put(DiagnosticRedactor.BINDING_NAME,
        new DiagnosticRedactor(Mode.OBSERVE, (sink, reason) -> hits.add(sink)));

    engine.eval(REVEAL + "print(secret)\n");
    assertEquals(SECRET, output.toString().strip());
    assertEquals(List.of(DiagnosticRedactor.Sink.PRINT), hits);
  }

  @Test
  void failKeepsItsRawDiagnosticUntilTheHostRendersIt() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    DiagnosticRedactor redactor = new DiagnosticRedactor(Mode.ENFORCE);
    engine.put(DiagnosticRedactor.BINDING_NAME, redactor);

    ScriptException failure = assertThrows(ScriptException.class,
        () -> engine.eval(REVEAL
            + "fail('done', msg=secret, attr='field', sep=' | ', stack_trace=False)\n"));
    assertTrue(failure.getMessage().contains("attribute field: | " + SECRET + " | done"));
    assertTrue(redactor.sanitizeError(failure.getMessage())
        .contains("attribute field: | ***REDACTED*** | done"));
    assertFalse(redactor.sanitizeError(failure.getMessage()).contains(SECRET));
  }

  @Test
  void invocationsUseTheCurrentRunBinding() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.eval("""
        load('@vgs//vault', 'vault')
        def reveal_and_report():
          print(vault.reveal(vault.redact('synthetic vault value')))
        def report_public_text(text):
          print(text)
        """);

    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));
    engine.invokeFunction("reveal_and_report");
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));
    engine.invokeFunction("report_public_text", SECRET);
    assertEquals(List.of("***REDACTED***", SECRET), output.toString().lines().toList());
  }

  @Test
  void diagnosticBindingBelongsToTheHost() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    DiagnosticRedactor redactor = new DiagnosticRedactor(Mode.ENFORCE);
    SimpleBindings shared = new SimpleBindings();
    shared.put(DiagnosticRedactor.BINDING_NAME, redactor);
    engine.setBindings(shared, ScriptContext.GLOBAL_SCOPE);

    engine.eval(DiagnosticRedactor.BINDING_NAME + " = 'script global'\n");
    assertSame(redactor, shared.get(DiagnosticRedactor.BINDING_NAME));
    assertFalse(engine.getBindings(ScriptContext.ENGINE_SCOPE)
        .containsKey(DiagnosticRedactor.BINDING_NAME));
  }

  @Test
  void globalScopeCannotShareDiagnosticStateAcrossEngines() throws Exception {
    SimpleBindings shared = new SimpleBindings();
    shared.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));
    LarkyScriptEngine first = new LarkyScriptEngine();
    first.setBindings(shared, ScriptContext.GLOBAL_SCOPE);
    first.eval(REVEAL);

    LarkyScriptEngine second = new LarkyScriptEngine();
    second.setBindings(shared, ScriptContext.GLOBAL_SCOPE);
    StringWriter output = writer(second);
    second.eval("""
        def report_text(text):
          print(text)
        """);

    second.invokeFunction("report_text", SECRET);
    assertEquals(SECRET, output.toString().strip());
  }

  @Test
  void defaultDiagnosticBuiltinsDoNotBecomeEngineBindings() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    writer(engine);
    engine.eval("print('ordinary')\n");
    assertNull(engine.get("print"));
    assertNull(engine.get("fail"));

    engine.eval("def print():\n  return 'user definition'\n");
    assertEquals("user definition", engine.invokeFunction("print"));
  }

  @Test
  void includesTheVerboseCallerLocationInThePrintScan() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.put(DefaultLarkyInterpreter.VERBOSE, true);
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));
    engine.compile(REVEAL + "print('ordinary')\n", SECRET + ".star").eval();

    assertFalse(output.toString().contains(SECRET));
    assertTrue(output.toString().contains("***REDACTED***.star"));
    assertTrue(output.toString().contains("ordinary"));
  }

  @Test
  void matchesFormsEmittedByTheBundledUrlModule() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));
    engine.eval("""
        load('@vgs//vault', 'vault')
        load('@stdlib//urllib/parse', 'parse')
        value = vault.reveal(vault.redact('item /~*?'))
        print(parse.quote(value), parse.quote_plus(value), sep=' | ')
        """);

    assertEquals("***REDACTED*** | ***REDACTED***", output.toString().strip());
  }

  @Test
  void masksAFormattedFragmentInPlaceThroughAugmentedAssignment() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));

    engine.eval(REVEAL + """
        fragment = '%.4s'
        fragment %= secret
        message = 'before<'
        message += fragment
        message += '>after'
        print(message)
        matches = secret == 'other'
        print('public synt')
        """);

    assertEquals("before<synt>after", engine.get("message"));
    assertEquals(List.of("before<***REDACTED***>after", "public synt"),
        output.toString().lines().toList());
  }

  @Test
  void tracksBatchHitsAndLeavesAliasMissesReadableAfterFormatting() throws Exception {
    String hit = new String(SECRET);
    LarkyVault vault = mock(LarkyVault.class);
    when(vault.revealOutcome(any(), any()))
        .thenReturn(new RevealOutcome(List.of("missing alias", hit), List.of(hit)));
    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.put(LarkyEvaluator.MODULE_PROVIDERS, ModuleProviders.of().with(LarkyVault.class, vault));
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));

    engine.eval("""
        load('@vgs//vault', 'vault')
        values = vault.reveal(['missing alias', 'mapped alias'])
        print('%.4s' % (values[0],), ['%.4s' % (values[1],)], sep=' | ')
        """);

    assertEquals(List.of("missing alias", SECRET), List.copyOf((List<?>) engine.get("values")));
    assertEquals("miss | [\"***REDACTED***\"]", output.toString().strip());
  }

  @Test
  void masksADerivedFailureAtTheHostSinkAndPreservesTheOriginalException() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    DiagnosticRedactor redactor = new DiagnosticRedactor(Mode.ENFORCE);
    engine.put(DiagnosticRedactor.BINDING_NAME, redactor);

    ScriptException failure = assertThrows(ScriptException.class,
        () -> engine.eval(REVEAL + """
            fail('after', msg='before<' + ('%.4s' % secret) + '>', sep=' | ')
            """));
    assertTrue(failure.getMessage().contains("before<synt> | after"));
    assertTrue(redactor.sanitizeError(failure.getMessage())
        .contains("before<***REDACTED***> | after"));
    assertFalse(redactor.sanitizeError(failure.getMessage()).contains("before<synt>"));
  }

  @Test
  void keepsDerivedTagsThroughLoadsAndSameRunInvocationOnly() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.put(LarkyScriptEngine.MODULES, java.util.Map.of("helper.star", REVEAL + """
        fragment = '%.4s' % secret
        """));
    DiagnosticRedactor redactor = new DiagnosticRedactor(Mode.ENFORCE);
    engine.put(DiagnosticRedactor.BINDING_NAME, redactor);

    engine.eval("""
        load('helper', 'fragment')
        def report():
          print('value<' + fragment + '>')
        report()
        """);
    engine.invokeFunction("report");
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));
    engine.invokeFunction("report");

    assertEquals(List.of("value<***REDACTED***>", "value<***REDACTED***>", "value<synt>"),
        output.toString().lines().toList());
  }

  @Test
  void cleanFailureKeepsItsMessageAfterAnEarlierTaggedFailure() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    DiagnosticRedactor redactor = new DiagnosticRedactor(Mode.ENFORCE);
    engine.put(DiagnosticRedactor.BINDING_NAME, redactor);

    ScriptException tagged = assertThrows(ScriptException.class,
        () -> engine.eval(REVEAL + "fail('%.4s' % secret)\n"));
    assertFalse(redactor.sanitizeError(tagged.getMessage()).contains("synt"));

    ScriptException syntax = assertThrows(ScriptException.class,
        () -> engine.eval("def invalid("));
    assertEquals(syntax.getMessage(), redactor.sanitizeError(syntax.getMessage()));

    ScriptException clean = assertThrows(ScriptException.class,
        () -> engine.eval("fail('public timeout')\n"));
    assertEquals(clean.getMessage(), redactor.sanitizeError(clean.getMessage()));
  }

  @Test
  void taggedPrintKeepsLongPublicContextAndLaterPublicLines() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));

    engine.eval(REVEAL + """
        print('a' * 6000 + ('%.4s' % secret))
        print('public after')
        """);

    assertEquals(List.of("a".repeat(6000) + "***REDACTED***", "public after"),
        output.toString().lines().toList());
  }

  @Test
  void preservesEscapedPublicContextInAQuotedDerivedString() throws Exception {
    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));

    engine.eval(REVEAL + "print(['public\\n<' + ('%.4s' % secret) + '>after'])\n");

    assertEquals("[\"public\\n<***REDACTED***>after\"]", output.toString().strip());
  }

  @Test
  void acceptsEqualButDistinctScalarHitMetadata() throws Exception {
    LarkyVault vault = mock(LarkyVault.class);
    when(vault.revealOutcome(any(), any()))
        .thenReturn(new RevealOutcome(new String(SECRET), List.of(new String(SECRET))));
    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.put(LarkyEvaluator.MODULE_PROVIDERS, ModuleProviders.of().with(LarkyVault.class, vault));
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));

    engine.eval("""
        load('@vgs//vault', 'vault')
        value = vault.reveal('mapped alias')
        print('%.4s' % value)
        """);

    assertEquals(SECRET, engine.get("value"));
    assertEquals("***REDACTED***", output.toString().strip());
  }

  @Test
  void masksDerivedBytesEvenWhenTheSourceIsTooShortForTextMatching() throws Exception {
    StarlarkBytes hit = StarlarkBytes.immutableOf(new byte[] {'Q'});
    LarkyVault vault = mock(LarkyVault.class);
    when(vault.revealOutcome(any(), any())).thenReturn(RevealOutcome.hit(hit));
    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.put(LarkyEvaluator.MODULE_PROVIDERS, ModuleProviders.of().with(LarkyVault.class, vault));
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));

    engine.eval("""
        load('@vgs//vault', 'vault')
        derived = b'prefix' + vault.reveal('mapped alias')
        print('before', derived, 'after')
        print('public Q')
        """);

    assertEquals(StarlarkBytes.immutableOf("prefixQ".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        engine.get("derived"));
    assertEquals(List.of("before ***REDACTED*** after", "public Q"),
        output.toString().lines().toList());
  }

  @Test
  void countsRepeatedHitMetadataAfterMatchingExactBatchOccurrences() throws Exception {
    String first = new String(SECRET);
    String second = new String(SECRET);
    LarkyVault vault = mock(LarkyVault.class);
    when(vault.revealOutcome(any(), any()))
        .thenReturn(new RevealOutcome(List.of(first, second), List.of(first, first)));
    LarkyScriptEngine engine = new LarkyScriptEngine();
    StringWriter output = writer(engine);
    engine.put(LarkyEvaluator.MODULE_PROVIDERS, ModuleProviders.of().with(LarkyVault.class, vault));
    engine.put(DiagnosticRedactor.BINDING_NAME, new DiagnosticRedactor(Mode.ENFORCE));

    engine.eval("""
        load('@vgs//vault', 'vault')
        values = vault.reveal(['one', 'two'])
        print('%.4s' % values[0], '%.4s' % values[1], sep=' | ')
        """);

    assertEquals(List.of(SECRET, SECRET), List.copyOf((List<?>) engine.get("values")));
    assertEquals("***REDACTED*** | ***REDACTED***", output.toString().strip());
  }

  private static StringWriter writer(LarkyScriptEngine engine) {
    StringWriter output = new StringWriter();
    engine.getContext().setWriter(output);
    return output;
  }
}
