# A hash object built at load time and updated by every call: the module global's native state
# changes after the module is frozen.
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")

_RUNNING = SHA256.new()

def digest(data):
    _RUNNING.update(data)
    return _RUNNING.hexdigest()
