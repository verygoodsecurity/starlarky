# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//types", types="types")
load("@stdlib//larky", larky="larky")
load("@vendor//cryptography/hazmat/primitives", hashes="hashes")
load("@vendor//cryptography/hazmat/primitives/asymmetric/rsa", rsa="rsa")
load("@vendor//option/result", Error="Error")


def PKCS1v15():
    self = larky.mutablestruct(__name__='PKCS1v15', __class__=PKCS1v15)
    self.name = "EMSA-PKCS1-v1_5"
    return self


def PSS(mgf, salt_length):
    self = larky.mutablestruct(__name__='PSS', __class__=PSS)
    self.MAX_LENGTH = larky.SENTINEL
    self.name = "EMSA-PSS"

    def __init__(mgf, salt_length):
        self._mgf = mgf

        if not types.is_int(salt_length) and salt_length != self.MAX_LENGTH:
            fail("TypeError: salt_length must be an integer.")

        if salt_length != self.MAX_LENGTH and salt_length < 0:
            fail("ValueError: salt_length must be zero or greater.")

        self._salt_length = salt_length
        return self

    self = __init__(mgf, salt_length)
    return self


def OAEP(mgf,
         algorithm,
         label,
         ):
    self = larky.mutablestruct(__name__='OAEP', __class__=OAEP)
    self.name = "EME-OAEP"

    def __init__(
            mgf,
            algorithm,
            label,
    ):
        if not builtins.isinstance(algorithm, hashes.HashAlgorithm):
            fail("TypeError: Expected instance of hashes.HashAlgorithm.")

        self._mgf = mgf
        self._algorithm = algorithm
        self._label = label
        return self

    self = __init__(mgf, algorithm, label)
    return self


def MGF1(algorithm):
    self = larky.mutablestruct(__name__='MGF1', __class__=MGF1)
    self.MAX_LENGTH = larky.SENTINEL

    def __init__(algorithm):
        if not builtins.isinstance(algorithm, hashes.HashAlgorithm):
            fail("TypeError: Expected instance of hashes.HashAlgorithm.")

        self._algorithm = algorithm
        return self

    self = __init__(algorithm)
    return self


def calculate_max_pss_salt_length(
        key,
        hash_algorithm,
):
    if not any([
        builtins.isinstance(key, x)
        for x in (rsa.RSAPrivateKey, rsa.RSAPublicKey,)
    ]):
        fail("TypeError: key must be an RSA public or private key")
    # bit length - 1 per RFC 3447
    emlen = (key.key_size + 6) // 8
    salt_length = emlen - hash_algorithm.digest_size - 2
    if not (salt_length >= 0):
        fail("assert salt_length >= 0 failed!")
    return salt_length


padding = larky.struct(
    __name__='padding',
    PKCS1v15=PKCS1v15,
    PSS=PSS,
    OAEP=OAEP,
    MGF1=MGF1,
    calculate_max_pss_salt_length=calculate_max_pss_salt_length,
)
