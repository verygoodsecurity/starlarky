# Ported from pycryptodome 3.10.0-3.10.1, pinned to 3.10.1 (339259c): lib/Crypto/Signature/__init__.py. See larky/src/main/resources/VENDORED.md.
load("@stdlib//larky", larky="larky")

load("@vendor//Crypto/Signature/DSS", DSS="DSS")
load("@vendor//Crypto/Signature/PKCS1_v1_5", PKCS1_v1_5="PKCS1_v1_5")
load("@vendor//Crypto/Signature/pkcs1_15", pkcs1_15="pkcs1_15")
load("@vendor//Crypto/Signature/pss", pss="pss")


Signature = larky.struct(
    DSS=DSS,
    PKCS1_v1_5=PKCS1_v1_5,
    pkcs1_15=pkcs1_15,
    pss=pss,
)