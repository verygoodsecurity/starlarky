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


def reraise(tp, value, tb=None):
    if value == None:
       value = tp()
    if tb != None and getattr(value, '__traceback__', None) == tb:
       fail(value.with_traceback(tb))
    fail(value)


six = larky.struct(
    ensure_binary=tobytes,
    ensure_str=tostr,
    int2byte=_int2byte,
    byte2int=operator.itemgetter(0),
    reraise=reraise
)