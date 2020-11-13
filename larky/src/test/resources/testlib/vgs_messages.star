load("@stdlib/larky", "larky")


def _HttpMessage(payload, headers, uri, phase):
    return larky.struct(
    payload=payload,
    headers=headers,
    uri=uri,
    phase=phase
)


def _HttpHeader(key, value):
    return (key, value)


_HttpPhase = larky.struct(
    UNKNOWN = 0,
    REQUEST = 1,
    RESPONSE = 2,
)


http_pb2 = larky.struct(
    HttpMessage=_HttpMessage,
    HttpHeader=_HttpHeader,
    HttpPhase=_HttpPhase,
)