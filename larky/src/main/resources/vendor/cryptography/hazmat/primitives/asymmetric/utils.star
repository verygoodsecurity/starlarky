# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", larky="larky")
load("@vendor//cryptography/hazmat/_der", DERReader="DERReader", INTEGER="INTEGER", SEQUENCE="SEQUENCE", encode_der="encode_der", encode_der_integer="encode_der_integer")
load("@vendor//cryptography/hazmat/primitives", hashes="hashes")
load("@vendor//option/result", Error="Error")


def decode_dss_signature(signature):
    reader = DERReader(signature)
    reader_ctx = reader.__enter__()
    seq = reader_ctx.read_single_element(SEQUENCE)
    r = seq.read_element(INTEGER).as_integer()
    s = seq.read_element(INTEGER).as_integer()
    reader.__exit__()
    return r, s


def encode_dss_signature(r, s):
    return encode_der(
        SEQUENCE,
        encode_der(INTEGER, encode_der_integer(r)),
        encode_der(INTEGER, encode_der_integer(s)),
    )


def Prehashed(algorithm):
    self = larky.mutablestruct(__name__='Prehashed', __class__=Prehashed)
    def __init__(algorithm):
        if not builtins.isinstance(algorithm, hashes.HashAlgorithm):
            fail("TypeError: Expected instance of HashAlgorithm, not %s", type(algorithm))

        self._algorithm = algorithm
        self._digest_size = algorithm.digest_size
        return self
    self = __init__(algorithm)

    self.digest_size = larky.property(lambda: self._digest_size)
    return self


utils = larky.struct(
    __name__='utils',
    decode_dss_signature=decode_dss_signature,
    encode_dss_signature=encode_dss_signature,
    Prehashed=Prehashed,
)