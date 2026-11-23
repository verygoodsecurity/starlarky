"""Proxy control types for scripts running inside a VGS proxy.

    load("@vgs//http/response", "VGSHttpResponse")
    load("@vgs//proxy", "proxy")

    def process(input, ctx):
        return proxy.ShortCircuitResponse(VGSHttpResponse(body=b"blocked", status_code=403))

ShortCircuitResponse decorates the protocol's own response (a VGSHttpResponse for HTTP) to stop
processing the message with it: the result shares that response's state.
"""
load("@stdlib//larky", larky="larky")
load("@vgs//jproxy", _jproxy="jproxy")


proxy = larky.struct(
    ShortCircuitResponse=_jproxy.ShortCircuitResponse,
)
