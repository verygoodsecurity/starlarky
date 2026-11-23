"""Proxy control types for scripts running inside a VGS proxy.

    load("@vgs//http/response", "VGSHttpResponse")
    load("@vgs//proxy", "proxy")

    def process(input, ctx):
        return proxy.ShortCircuitResponse(VGSHttpResponse(body=b"blocked", status_code=403))

ShortCircuitResponse takes the protocol's own response (a VGSHttpResponse for HTTP) and stops
processing the message with it. The result cannot be changed; the response it holds, `.response`,
can.
"""
load("@stdlib//larky", larky="larky")
load("@vgs//jproxy", _jproxy="jproxy")


proxy = larky.struct(
    ShortCircuitResponse=_jproxy.ShortCircuitResponse,
)
