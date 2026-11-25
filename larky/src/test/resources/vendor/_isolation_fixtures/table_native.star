# A top-level dict (frozen with the module) holding a native hash object that calls update.
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")

_TABLE = {"running": SHA256.new()}

def digest(data):
    h = _TABLE["running"]
    h.update(data)
    return h.hexdigest()
