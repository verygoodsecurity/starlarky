load("@stdlib//base64", b64decode="b64decode")
load("@vendor//Crypto/Hash/HMAC", HMAC="HMAC")

load("@vendor//xmlsig/algorithms/base", Algorithm="Algorithm")


def _HMACAlgorithm():
    self = Algorithm()
    self.__name__ = 'HMACAlgorithm'
    self.__class__ = _HMACAlgorithm

    def sign(data, private_key, digest):
        return HMAC.new(private_key, data, digestmod=digest).digest()
    self.sign = sign

    def verify(signature_value, data, public_key, digest):
        h = HMAC.new(public_key, data, digestmod=digest)
        h.verify(b64decode(signature_value))
    self.verify = verify
    return self


HMACAlgorithm = _HMACAlgorithm()