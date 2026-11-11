package com.verygood.security.larky.modules.vgs.agentgateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.jsr223.LarkyCompiledScript;
import com.verygood.security.larky.jsr223.LarkyLoadedScript;
import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.modules.vgs.ModuleProviders;
import com.verygood.security.larky.parser.LarkyEvaluator;
import com.verygood.security.larky.modules.vgs.agentgateway.spi.AgentGatewaySession;
import com.verygood.security.larky.modules.vgs.agentgateway.spi.LarkyAgentGateway;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.script.ScriptException;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import org.junit.After;
import org.junit.Test;

public class AgentGatewayModuleTest {

  /** A session with one card, one labelled alias, one context field and in-memory captures. */
  private static class FakeSession implements AgentGatewaySession {
    final Map<String, String> captures = new HashMap<>();

    @Override
    public Object card(@Nullable String label) throws EvalException {
      if (label != null && !label.equals("visa")) {
        throw Starlark.errorf("the session has no card %s", label);
      }
      return "card:" + (label == null ? "only" : label);
    }

    @Override
    public String alias(String label) throws EvalException {
      if (!label.equals("email")) {
        throw Starlark.errorf("no alias %s", label);
      }
      return "shopper@stand.in";
    }

    @Nullable
    @Override
    public String context(String key) {
      return key.equals("order_id") ? "o-1" : null;
    }

    @Nullable
    @Override
    public String get(String key) {
      return captures.get(key);
    }

    @Override
    public void put(String key, String value) {
      captures.put(key, value);
    }
  }

  @After
  public void tearDown() {
    TestAgentGateway.CURRENT.remove();
  }

  private static Object eval(String... lines) throws ScriptException {
    final ParsedStarFile result = (ParsedStarFile) new LarkyScriptEngine().eval(String.join("\n", lines) + "\n");
    return result.getGlobalEnvironmentVariable("out", Object.class);
  }

  @Test
  public void aScriptLoadsTheSessionAndReadsTheCurrentMessagesSession() throws Exception {
    TestAgentGateway.CURRENT.set(new FakeSession());

    assertEquals("card:only|card:visa|shopper@stand.in|o-1|none", eval(
        "load(\"@vgs//agent_gateway\", \"session\")",
        "out = '|'.join([session.card(), session.card(label='visa'), session.alias('email'),",
        "                session.context('order_id'), session.context('missing', default='none')])"));
  }

  @Test
  public void putThenGetGoesThroughTheHost() throws Exception {
    final FakeSession session = new FakeSession();
    TestAgentGateway.CURRENT.set(session);

    assertEquals("k1", eval(
        "load(\"@vgs//agent_gateway\", \"session\")",
        "before = session.get('public_key')",
        "session.put('public_key', 'k1')",
        "out = session.get('public_key') if before == None else 'unexpected'"));
    assertEquals(Map.of("public_key", "k1"), session.captures);
  }

  @Test
  public void offSessionTrafficLoadingWorksButEveryMethodFails() throws Exception {
    // a script that only loads it (as a validator does) runs
    assertEquals("loaded", eval("load(\"@vgs//agent_gateway\", \"session\")", "out = 'loaded'"));

    final ScriptException e = assertThrows(ScriptException.class, () -> eval(
        "load(\"@vgs//agent_gateway\", \"session\")", "out = session.alias('email')"));
    assertTrue(e.getMessage(), e.getMessage().contains(AgentGatewayModule.UNAVAILABLE));
  }

  @Test
  public void withoutAHostEveryMethodFails() {
    final AgentGatewayModule module = new AgentGatewayModule(List.of());

    final EvalException e = assertThrows(EvalException.class, () -> module.session().get("k", null));
    assertTrue(e.getMessage(), e.getMessage().contains(AgentGatewayModule.UNAVAILABLE));
  }

  @Test
  public void aHostWithoutASessionForThisThreadFailsTheSameWay() {
    final LarkyAgentGateway none = Optional::empty;

    final EvalException e = assertThrows(EvalException.class,
        () -> new AgentGatewayModule(List.of(none)).session().card(Starlark.NONE, null));
    assertTrue(e.getMessage(), e.getMessage().contains(AgentGatewayModule.UNAVAILABLE));
  }

  /** A session whose single card is named after it. */
  private static AgentGatewaySession named(String name) {
    return new FakeSession() {
      @Override
      public Object card(@Nullable String label) {
        return "card:" + name;
      }
    };
  }

  @Test
  public void aSessionGivenToTheRunIsUsedWithoutAHostSession() throws Exception {
    final javax.script.Bindings bindings = new javax.script.SimpleBindings();
    bindings.put(LarkyEvaluator.MODULE_PROVIDERS, ModuleProviders.of().with(AgentGatewaySession.class, named("given")));

    final ParsedStarFile result = (ParsedStarFile) new LarkyScriptEngine().eval(
        "load(\"@vgs//agent_gateway\", \"session\")\nout = session.card()\n", bindings);

    assertEquals("card:given", result.getGlobalEnvironmentVariable("out", Object.class));
  }

  @Test
  public void aSessionGivenToTheRunWinsOverTheHostsSession() throws Exception {
    TestAgentGateway.CURRENT.set(named("host"));
    final javax.script.Bindings bindings = new javax.script.SimpleBindings();
    bindings.put(LarkyEvaluator.MODULE_PROVIDERS, ModuleProviders.of().with(AgentGatewaySession.class, named("given")));

    final ParsedStarFile result = (ParsedStarFile) new LarkyScriptEngine().eval(
        "load(\"@vgs//agent_gateway\", \"session\")\nout = session.card()\n", bindings);

    assertEquals("card:given", result.getGlobalEnvironmentVariable("out", Object.class));
  }

  @Test
  public void aFunctionLoadedOnceSeesEachCallsOwnSession() throws Exception {
    final javax.script.ScriptContext context = new javax.script.SimpleScriptContext();
    context.setWriter(new java.io.StringWriter());
    context.setBindings(new javax.script.SimpleBindings(), javax.script.ScriptContext.ENGINE_SCOPE);
    final LarkyLoadedScript loaded = ((LarkyCompiledScript) new LarkyScriptEngine().compile(
        "load(\"@vgs//agent_gateway\", \"session\")\ndef process():\n    return session.card()\n"))
        .load(context, java.util.Set.of());

    for (String name : List.of("first", "second")) {
      assertEquals("card:" + name, loaded.invoke("process", Map.of(LarkyEvaluator.MODULE_PROVIDERS,
          ModuleProviders.of().with(AgentGatewaySession.class, named(name)))));
    }
    final ScriptException e = assertThrows(ScriptException.class, () -> loaded.invoke("process", Map.of()));
    assertTrue(e.getMessage(), e.getMessage().contains(AgentGatewayModule.UNAVAILABLE));
  }

  @Test
  public void twoHostsAreRefused() {
    final LarkyAgentGateway none = Optional::empty;

    assertThrows(IllegalArgumentException.class, () -> new AgentGatewayModule(List.of(none, none)));
  }
}
