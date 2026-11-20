# Ported from multidict 4.7.6-5.1.0, pinned to 5.1.0 (c3eed83): multidict/_multidict_py.py. See larky/src/main/resources/VENDORED.md.

"""Public multidict module with its existing exports."""

load("@stdlib//larky", "larky")
load("@stdlib//operator", "operator")
load("@stdlib//types", "types")
load("@vendor//option/result", "Error", "Ok")
load(
    "@vendor//_multidict_core",
    "CIMultiDict",
    "CIMultiDictProxy",
    "MultiDict",
    "MultiDictProxy",
    "getversion",
    "istr",
    "multidict",
)
