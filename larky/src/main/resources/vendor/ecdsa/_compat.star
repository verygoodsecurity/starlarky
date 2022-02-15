"""
Common functions for providing cross-python version compatibility.
"""
load("@stdlib//types", types="types")
load("@stdlib//larky", larky="larky")
load("@stdlib//builtins", "builtins")

def hmac_compat(data):
    return data

def normalise_bytes(buffer_object):
    """Cast the input into array of bytes."""
    # return memoryview(buffer_object).cast("B")
    return bytes(buffer_object)

def str_idx_as_int(string, index):
    """Take index'th byte from string, return as integer"""
    val = string[index]
    if types.is_int(val):
        return val
    return ord(val)

_compat=larky.struct(
    normalise_bytes=normalise_bytes,
    str_idx_as_int=str_idx_as_int,
    hmac_compat=hmac_compat
)

