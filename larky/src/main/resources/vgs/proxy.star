"""Proxy control types for scripts running inside a VGS proxy.

    load("@vgs//http/response", "VGSHttpResponse")
    load("@vgs//proxy", "proxy")

    def process(input, ctx):
        return proxy.ShortCircuitResponse(VGSHttpResponse(body=b"blocked", status_code=403))

ShortCircuitResponse wraps the protocol's own response (a VGSHttpResponse for HTTP) and stops
processing the message with it. For HTTP, ShortCircuitResponse(body=..., headers=...,
status_code=...) is the same as wrapping a VGSHttpResponse built from those fields.
"""
load("@stdlib//larky", larky="larky")
load("@vgs//http/response", _protocol_short_circuit="ShortCircuitResponse")
load("@vgs//proxy_native", _proxy_native="proxy_native")


proxy = larky.struct(
    ShortCircuitResponse=_proxy_native.short_circuit_response(_protocol_short_circuit),
)
