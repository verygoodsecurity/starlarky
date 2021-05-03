
load("@stdlib//larky", larky="larky")
load("@stdlib//struct", struct="struct")


def _redact(secret):
    return "tok_123"

def _put(secret):
    return "tok_123"

def _get(alias):
    return "4111111111111111"

vault = larky.struct(
    put = _put,
    get = _get,
)