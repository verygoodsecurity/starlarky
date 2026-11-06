# Ported from python-xmlsig 0.1.6-0.1.7, pinned to 0.1.6 (1836ccc) via vendor/xmlsig: src/xmlsig/algorithms/__init__.py. See larky/src/main/resources/VENDORED.md.
load("@stdlib//larky", larky="larky")

load("@vgs//xmlsig-java-compatible/algorithms/hmac", HMACAlgorithm="HMACAlgorithm")
load("@vgs//xmlsig-java-compatible/algorithms/rsa", RSAAlgorithm="RSAAlgorithm")


algorithms = larky.struct(
    HMACAlgorithm=HMACAlgorithm,
    RSAAlgorithm=RSAAlgorithm,
)