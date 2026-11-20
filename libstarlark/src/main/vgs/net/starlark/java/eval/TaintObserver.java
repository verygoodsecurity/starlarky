package net.starlark.java.eval;

/**
 * Receives one event per blocked or would-be-blocked output. Implementations count events; they
 * never see the value, by construction. Tenant and route come from the host.
 */
@FunctionalInterface
public interface TaintObserver {
  TaintObserver NOOP = (sink, reason) -> {};

  void onHit(SecretTaint.Sink sink, SecretTaint.Reason reason);
}
