# HMAC-SHA256 with its inner and outer hashes keyed at load time and copied per call (the usual
# precomputation). Crypto.Hash.HMAC's own copy() is not used: it calls _HMAC without msg.
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")

_KEY = b"0123456789abcdef" + bytes([0] * 48)
_INNER = SHA256.new(bytes([b ^ 0x36 for b in _KEY.elems()]))
_OUTER = SHA256.new(bytes([b ^ 0x5C for b in _KEY.elems()]))

def digest(data):
    inner = _INNER.copy()
    inner.update(data)
    outer = _OUTER.copy()
    outer.update(inner.digest())
    return outer.hexdigest()
