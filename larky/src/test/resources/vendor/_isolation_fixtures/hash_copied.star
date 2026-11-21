# A hash object built at load time and only copied afterwards (read-only use of a module global).
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")

_PREFIXED = SHA256.new(b"prefix:")

def digest(data):
    h = _PREFIXED.copy()
    h.update(data)
    return h.hexdigest()
