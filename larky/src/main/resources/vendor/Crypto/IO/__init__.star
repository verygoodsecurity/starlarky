load("@stdlib//larky", larky="larky")
load("@vendor//Crypto/IO/PEM", _PEM="PEM")
load("@vendor//Crypto/IO/PKCS8", _PKCS8="PKCS8")

PEM = _PEM
PKCS8 = _PKCS8

IO = larky.struct(
    PEM=PEM,
    PKCS8=PKCS8
)