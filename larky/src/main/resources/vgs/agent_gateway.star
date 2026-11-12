"""Agent Web Gateway API for adapter functions.

    load("@vgs//agent_gateway", "session")

`session` is the reveal session of the message being handled: session.card(label=None), session.alias(label),
session.context(key, default=None), session.get(key) and session.put(key, value). Aliases are session aliases
(stand-ins); reveal them through @vgs//vault. Off reveal-session traffic every method fails.
"""
load("@vgs//native_agent_gateway", _agent_gateway="native_agent_gateway")

session = _agent_gateway.session
