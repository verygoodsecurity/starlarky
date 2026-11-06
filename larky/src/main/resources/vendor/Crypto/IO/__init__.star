# Ported from pycryptodome 3.10.0-3.10.1, pinned to 3.10.1 (339259c): lib/Crypto/IO/__init__.py. See larky/src/main/resources/VENDORED.md.
load("@stdlib//larky", larky="larky")
load("@vendor//Crypto/IO/PEM", _PEM="PEM")
load("@vendor//Crypto/IO/PKCS8", _PKCS8="PKCS8")

PEM = _PEM
PKCS8 = _PKCS8

IO = larky.struct(
    PEM=PEM,
    PKCS8=PKCS8
)