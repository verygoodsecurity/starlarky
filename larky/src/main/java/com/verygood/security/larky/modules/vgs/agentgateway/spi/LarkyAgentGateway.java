package com.verygood.security.larky.modules.vgs.agentgateway.spi;

import java.util.Optional;

/**
 * What a host that runs Agent Web Gateway functions provides to {@code @vgs//agent_gateway}, found with
 * {@link java.util.ServiceLoader} (at most one per class path).
 */
public interface LarkyAgentGateway {

  /**
   * The session of the message the current thread is handling, or empty when it handles none (traffic that is not a
   * reveal session's).
   */
  Optional<AgentGatewaySession> currentSession();
}
