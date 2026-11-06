# Ported from python-xmlsig 0.1.6-0.1.7 (src/ identical), pinned to 0.1.6 (1836ccc): src/xmlsig/algorithms/__init__.py. See larky/src/main/resources/VENDORED.md.
load("@stdlib//larky", larky="larky")

load("@vendor//xmlsig/algorithms/hmac", HMACAlgorithm="HMACAlgorithm")
load("@vendor//xmlsig/algorithms/rsa", RSAAlgorithm="RSAAlgorithm")


algorithms = larky.struct(
    HMACAlgorithm=HMACAlgorithm,
    RSAAlgorithm=RSAAlgorithm,
)