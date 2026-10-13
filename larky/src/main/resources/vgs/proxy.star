"""Proxy control types for scripts running inside a VGS proxy.

    load("@vgs//proxy", "proxy")
    load("@vgs//http/response", "VGSHttpResponse")

    def process(input, ctx):
        return proxy.ShortCircuitResponse(VGSHttpResponse(body=b"blocked", status_code=403))
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@vgs//http/response", "VGSHttpResponse")


# The protocol response types a script can short-circuit with. Add a protocol's response type here
# (e.g. a TCP response) when a host proxy can send it back to the sender.
_RESPONSE_TYPES = (VGSHttpResponse,)


def _type_name(value):
    return getattr(value, "__name__", None) or type(value)


def ShortCircuitResponse(response):
    """Return `response` to the client and stop processing the message.

    In the request phase the upstream is not called; in either phase the remaining filters are
    skipped. `response` is the protocol's response type, e.g. a VGSHttpResponse for HTTP.
    """
    if response == None:
        fail("ShortCircuitResponse requires a response")
    if not [t for t in _RESPONSE_TYPES if types.is_instance(response, t)]:
        fail("ShortCircuitResponse must wrap one of [%s], got %s" % (
            ", ".join([larky.impl_function_name(t) for t in _RESPONSE_TYPES]),
            _type_name(response),
        ))
    return larky.struct(
        __name__="ShortCircuitResponse",
        response=response,
    )


proxy = larky.struct(
    ShortCircuitResponse=ShortCircuitResponse,
)
