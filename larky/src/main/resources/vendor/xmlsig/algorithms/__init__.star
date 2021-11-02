load("@stdlib//larky", larky="larky")

load("@vendor//xmlsig/algorithms/hmac", HMACAlgorithm="HMACAlgorithm")
load("@vendor//xmlsig/algorithms/rsa", RSAAlgorithm="RSAAlgorithm")


algorithms = larky.struct(
    HMACAlgorithm=HMACAlgorithm,
    RSAAlgorithm=RSAAlgorithm,
)