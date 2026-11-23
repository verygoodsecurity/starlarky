"""Proxy control types for scripts running inside a VGS proxy.

    load("@vgs//http/response", "VGSHttpResponse")
    load("@vgs//proxy", "proxy")

    def process(input, ctx):
        return proxy.ShortCircuitResponse(VGSHttpResponse(body=b"blocked", status_code=403))

ShortCircuitResponse decorates the protocol's own response (a VGSHttpResponse for HTTP) to stop
processing the message with it: the result shares that response's state. Given only keywords,
ShortCircuitResponse(body=..., headers=..., status_code=...) builds the protocol's short-circuit
response from them directly (for HTTP, @vgs//http/response's ShortCircuitResponse), without a
response to decorate.
"""
load("@stdlib//larky", larky="larky")
load("@vgs//http/response", _protocol_short_circuit="ShortCircuitResponse")
load("@vgs//jproxy", _jproxy="jproxy")


proxy = larky.struct(
    ShortCircuitResponse=_jproxy.short_circuit_response(_protocol_short_circuit),
)
