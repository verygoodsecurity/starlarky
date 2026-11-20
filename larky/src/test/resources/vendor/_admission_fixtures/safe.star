# Shareable: functions, frozen data, and a factory that makes a fresh hash per call.
# init_counter is a host binding the test passes in, counting how often this top level runs.
load("@vendor//Crypto/Hash/SHA256", "SHA256")

init_counter.count()

TABLE = {"a": [1, 2, 3], "b": ("x", "y")}

def digest(data):
    h = SHA256.new()
    h.update(data)
    return h.hexdigest()

def lookup(k):
    return TABLE[k]
