"""
This module is to make python<>larky compatibility easier
"""
load("@stdlib//larky", larky="larky")
load("@vendor//past/builtins", builtins=builtins)


past = larky.struct(
    builtins=builtins
)