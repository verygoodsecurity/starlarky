load("@stdlib//larky", larky="larky")

load("@vendor//xmlsec/algorithms/hmac", HMACAlgorithm="HMACAlgorithm")
load("@vendor//xmlsec/algorithms/rsa", RSAAlgorithm="RSAAlgorithm")


algorithms = larky.struct(
    HMACAlgorithm=HMACAlgorithm,
    RSAAlgorithm=RSAAlgorithm,
)