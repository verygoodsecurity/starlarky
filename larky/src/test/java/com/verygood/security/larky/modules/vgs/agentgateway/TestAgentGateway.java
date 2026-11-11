package com.verygood.security.larky.modules.vgs.agentgateway;

import com.verygood.security.larky.modules.vgs.agentgateway.spi.AgentGatewaySession;
import com.verygood.security.larky.modules.vgs.agentgateway.spi.LarkyAgentGateway;
import java.util.Optional;

/** The test host: the session a test sets for its thread (registered with ServiceLoader in test resources). */
public class TestAgentGateway implements LarkyAgentGateway {

  static final ThreadLocal<AgentGatewaySession> CURRENT = new ThreadLocal<>();

  @Override
  public Optional<AgentGatewaySession> currentSession() {
    return Optional.ofNullable(CURRENT.get());
  }
}
