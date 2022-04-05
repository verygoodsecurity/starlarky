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