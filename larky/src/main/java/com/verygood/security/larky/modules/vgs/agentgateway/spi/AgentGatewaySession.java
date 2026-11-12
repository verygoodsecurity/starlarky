package com.verygood.security.larky.modules.vgs.agentgateway.spi;

import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;

/**
 * The reveal session of one message on the Agent Web Gateway, as a host gives it to Larky functions through
 * {@code load("@vgs//agent_gateway", "session")}. Aliases are session aliases (stand-ins): a function reveals them
 * through {@code @vgs//vault}.
 */
public interface AgentGatewaySession {

  /**
   * One card of the session: a Starlark value whose fields {@code number}, {@code cvc}, {@code expiry} and
   * {@code holder} are the session aliases of its parts (None for a part the session does not have).
   *
   * @param label the card's label; null for the session's only card
   * @throws EvalException when there is no such card, or the label is null and the session has several
   */
  Object card(@Nullable String label) throws EvalException;

  /**
   * The session alias of a text alias, by its label.
   *
   * @throws EvalException when the session has no alias, or more than one, with that label
   */
  String alias(String label) throws EvalException;

  /** A string field of the session's context, or null. */
  @Nullable
  String context(String key);

  /**
   * A value the current function's adapter captured in this session, or null.
   *
   * @throws EvalException when the current function may not capture
   */
  @Nullable
  String get(String key) throws EvalException;

  /**
   * Captures a value for the current function's adapter.
   *
   * @throws EvalException when the current function may not capture, or the session holds too many values
   */
  void put(String key, String value) throws EvalException;
}
