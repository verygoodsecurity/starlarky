"""
This module is not really needed but it's here for posterity sake
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//struct", struct="struct")
load("@vendor//Crypto/Util/py3compat",
     tobytes="tobytes",
     tostr="tostr")

def _int2byte(x):
    return struct.pack(">B", x)

six = larky.struct(
    ensure_binary=tobytes,
    ensure_str=tostr,
    int2byte=_int2byte,
    byte2int=operator.itemgetter(0),
)