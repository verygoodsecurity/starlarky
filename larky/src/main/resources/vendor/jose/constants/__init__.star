# Ported from python-jose jose/constants.py; consistent with its master branch between
# 89c3d41bc6c60ad6f7d135f14dcd963cef633637 and 8572088b76f7c0d31068f38aeca0d4273ae8bc8f
# (no release; the version was not recorded, see jose/__init__.star).

load("@stdlib//larky", larky="larky")
load("@stdlib//sets", sets="sets")
load("@vendor//jose/constants/algorithms", _ALGORITHMS="ALGORITHMS")

ALGORITHMS = _ALGORITHMS
ZIPS = larky.struct(
    DEF="DEF",
    NONE=None,
    SUPPORTED=sets.make(("DEF", None,)),
)
