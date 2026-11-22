"""Proxy control types for scripts running inside a VGS proxy.

    load("@vgs//proxy", "proxy")

    def process(input, ctx):
        return proxy.ShortCircuitResponse(body=b"blocked", status_code=403)
"""
load("@stdlib//larky", larky="larky")
load("@vgs//http/response", "ShortCircuitResponse")


proxy = larky.struct(
    ShortCircuitResponse=ShortCircuitResponse,
)
