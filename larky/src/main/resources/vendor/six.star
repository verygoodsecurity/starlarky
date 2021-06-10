"""
This module is not really needed but it's here for posterity sake
"""
load("@stdlib//larky", larky="larky")
load("@vendor//Crypto/Util/py3compat",
     tobytes="tobytes",
     tostr="tostr")


six = larky.struct(
    ensure_binary=tobytes,
    ensure_str=tostr,
    int2byte=chr,
    byte2int=ord,
)