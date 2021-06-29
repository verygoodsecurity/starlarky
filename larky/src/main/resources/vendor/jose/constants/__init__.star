load("@stdlib//larky", larky="larky")
load("@stdlib//sets", sets="sets")
load("@vendor//jose/constants/algorithms", _ALGORITHMS="ALGORITHMS")

ALGORITHMS = _ALGORITHMS
ZIPS = larky.struct(
    DEF="DEF",
    NONE=None,
    SUPPORTED=sets.make(("DEF", None,)),
)
