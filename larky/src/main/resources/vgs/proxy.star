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


def ShortCircuitResponse(response):
    """Stop processing the message with `response`, the protocol's response (a VGSHttpResponse for HTTP)."""
    return _jproxy.ShortCircuitResponse(ShortCircuitResponse, response)


proxy = larky.struct(
    ShortCircuitResponse=ShortCircuitResponse,
)
