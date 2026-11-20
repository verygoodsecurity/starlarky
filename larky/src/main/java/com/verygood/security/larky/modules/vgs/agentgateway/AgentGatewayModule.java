package com.verygood.security.larky.modules.vgs.agentgateway;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.modules.vgs.ModuleProviders;
import com.verygood.security.larky.modules.vgs.agentgateway.spi.AgentGatewaySession;
import com.verygood.security.larky.modules.vgs.agentgateway.spi.LarkyAgentGateway;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import javax.annotation.Nullable;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

/**
 * The native side of {@code @vgs//agent_gateway}: the {@code session} of the message an Agent Web Gateway function
 * handles. The module and its {@code session} are the same objects for every script; each method finds the session
 * when it is called, so a script whose top level ran once still sees each message's own session: the session the host
 * gave the run ({@link ModuleProviders}), else the one its {@link LarkyAgentGateway} (found with
 * {@link ServiceLoader}) has for the current message. Without either, or off session traffic, every method fails.
 */
@StarlarkBuiltin(
    name = "native_agent_gateway",
    category = "BUILTIN",
    doc = "Agent Web Gateway API for adapter functions; load @vgs//agent_gateway")
public final class AgentGatewayModule implements StarlarkValue {

  public static final AgentGatewayModule INSTANCE = new AgentGatewayModule(providers());

  static final String UNAVAILABLE = "@vgs//agent_gateway's session is available only on reveal-session traffic";

  @Nullable private final LarkyAgentGateway gateway;
  private final Session session = new Session();

  AgentGatewayModule(List<LarkyAgentGateway> providers) {
    if (providers.size() > 1) {
      throw new IllegalArgumentException(String.format(
          "AgentGatewayModule expects at most 1 provider of type LarkyAgentGateway, found %d", providers.size()));
    }
    this.gateway = providers.isEmpty() ? null : providers.get(0);
  }

  private static List<LarkyAgentGateway> providers() {
    return ImmutableList.copyOf(ServiceLoader.load(LarkyAgentGateway.class).iterator());
  }

  @StarlarkMethod(name = "session", structField = true, doc = "The session of the message being handled.")
  public Session session() {
    return session;
  }

  /**
   * The session of the run of {@code thread}: the one the host gave this run ({@link ModuleProviders}), else the one
   * the {@link LarkyAgentGateway} found with ServiceLoader has for the current message.
   */
  private AgentGatewaySession current(StarlarkThread thread) throws EvalException {
    Optional<AgentGatewaySession> current = ModuleProviders.forRun(thread, AgentGatewaySession.class);
    if (current.isEmpty() && gateway != null) {
      current = gateway.currentSession();
    }
    if (current.isEmpty()) {
      throw Starlark.errorf("%s", UNAVAILABLE);
    }
    return current.get();
  }

  @Override
  public boolean isImmutable() {
    return true;
  }

  /** {@code session}: every method resolves the current message's session. */
  @StarlarkBuiltin(name = "AgentGatewaySession", documented = false)
  public final class Session implements StarlarkValue {

    @StarlarkMethod(name = "card", doc = "The session aliases of a card's parts: number, cvc, expiry, holder.",
        parameters = {@Param(name = "label", defaultValue = "None", named = true,
            allowedTypes = {@ParamType(type = String.class), @ParamType(type = NoneType.class)})},
        useStarlarkThread = true)
    public Object card(Object label, StarlarkThread thread) throws EvalException {
      return current(thread).card(label == Starlark.NONE ? null : (String) label);
    }

    @StarlarkMethod(name = "alias", doc = "The session alias of a text alias, by its label.",
        parameters = {@Param(name = "label")}, useStarlarkThread = true)
    public String alias(String label, StarlarkThread thread) throws EvalException {
      return current(thread).alias(label);
    }

    @StarlarkMethod(name = "context", doc = "A string field of the session's context, or the default.",
        parameters = {@Param(name = "key"), @Param(name = "default", defaultValue = "None", named = true)},
        useStarlarkThread = true)
    public Object context(String key, Object defaultValue, StarlarkThread thread) throws EvalException {
      final String value = current(thread).context(key);
      return value == null ? defaultValue : value;
    }

    @StarlarkMethod(name = "get", doc = "A value this function's adapter captured in the session, or None.",
        parameters = {@Param(name = "key")}, useStarlarkThread = true)
    public Object get(String key, StarlarkThread thread) throws EvalException {
      final String value = current(thread).get(key);
      return value == null ? Starlark.NONE : value;
    }

    @StarlarkMethod(name = "put", doc = "Captures a value for this function's adapter (in a response function).",
        parameters = {@Param(name = "key"), @Param(name = "value")}, useStarlarkThread = true)
    public void put(String key, String value, StarlarkThread thread) throws EvalException {
      current(thread).put(key, value);
    }

    @Override
    public boolean isImmutable() {
      // the same object for every script; what it reads belongs to each message
      return true;
    }

    @Override
    public void repr(Printer printer, StarlarkSemantics semantics) {
      printer.append("<agent_gateway session>");
    }
  }
}
