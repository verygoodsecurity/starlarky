"""Proxy control types for scripts running inside a VGS proxy.

    load("@vgs//proxy", "proxy")
    load("@vgs//http/response", "VGSHttpResponse")

    def process(input, ctx):
        return proxy.ShortCircuitResponse(VGSHttpResponse(body=b"blocked", status_code=403))
"""
load("@stdlib//larky", larky="larky")


def ShortCircuitResponse(response):
    """Return `response` to the client and stop processing the message.

    In the request phase the upstream is not called; in either phase the remaining filters are
    skipped. `response` is the protocol's response type, e.g. a VGSHttpResponse for HTTP.
    """
    if response == None:
        fail("ShortCircuitResponse requires a response")
    return larky.struct(
        __name__="ShortCircuitResponse",
        response=response,
    )


proxy = larky.struct(
    ShortCircuitResponse=ShortCircuitResponse,
)
