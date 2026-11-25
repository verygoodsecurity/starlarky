# Plain Starlark state at top level: a dict, a list and a mutable struct. Each function writes to
# one of them; freezing the loaded module must reject the write, with the cache on or off.
load("@stdlib//larky", "larky")

_COUNTS = {"n": 0}
_SEEN = []
_STATE = larky.mutablestruct(n=0)

def write_dict(data):
    _COUNTS["n"] += 1
    return str(_COUNTS["n"])

def write_list(data):
    _SEEN.append(data)
    return str(len(_SEEN))

def write_struct(data):
    _STATE.n += 1
    return str(_STATE.n)
