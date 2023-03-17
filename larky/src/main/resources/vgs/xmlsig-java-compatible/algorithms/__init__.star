load("@stdlib//larky", larky="larky")

load("@vgs//xmlsig-java-compatible/algorithms/hmac", HMACAlgorithm="HMACAlgorithm")
load("@vgs//xmlsig-java-compatible/algorithms/rsa", RSAAlgorithm="RSAAlgorithm")


algorithms = larky.struct(
    HMACAlgorithm=HMACAlgorithm,
    RSAAlgorithm=RSAAlgorithm,
)