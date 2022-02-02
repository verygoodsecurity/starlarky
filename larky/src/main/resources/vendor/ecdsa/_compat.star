"""
Common functions for providing cross-python version compatibility.
"""
load("@stdlib//types", types="types")
load("@stdlib//larky", larky="larky")

def str_idx_as_int(string, index):
    """Take index'th byte from string, return as integer"""
    val = string[index]
    if types.is_int(val):
        return val
    return ord(val)

