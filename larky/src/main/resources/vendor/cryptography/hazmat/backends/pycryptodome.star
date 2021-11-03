# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//enum", enum="enum")
load("@stdlib//larky", larky="larky")
load("@vendor//Crypto/Hash", Hash="Hash")
load("@vendor//cryptography/hazmat/primitives/_hashes",
     HashContext="HashContext",
     )

# From: https://github.com/openssl/openssl/blob/master/include/openssl/obj_mac.h
_OpenSSL_Constants = enum.Enum('OpenSSL_Constant', [
    ("EVP_PKEY_DH", 28),
    ("EVP_PKEY_DSA", 116),
    ("EVP_PKEY_EC", 408),
    ("EVP_PKEY_RSA", 6),
    # EVP_PKEY_DHX => NID_dhpublicnumber
    # https://github.com/openssl/openssl/blob/c6fcd88fa030da8322cf27aff95376512f41faff/include/openssl/evp.h#L68
    # NID_dhpublicnumber =>
    # https://github.com/openssl/openssl/blob/a596d38a8cddca4af3416b2664e120028d96e6a9/include/openssl/obj_mac.h#L5011
    ("EVP_PKEY_DHX", 920),
    ("RAND_SEED_LENGTH_IN_BYTES", 1024),
    ("SSL_MODE_HANDSHAKE_CUTTHROUGH", 64),
    ("SSL_OP_NO_COMPRESSION", 131072),
    ("SSL_OP_NO_SESSION_RESUMPTION_ON_RENEGOTIATION", 65536),
    ("SSL_OP_NO_SSLv3", 33554432),
    ("SSL_OP_NO_TICKET", 16384),
    ("SSL_OP_NO_TLSv1", 67108864),
    ("SSL_OP_NO_TLSv1_1", 268435456),
    ("SSL_OP_NO_TLSv1_2", 134217728),
    ("SSL_VERIFY_FAIL_IF_NO_PEER_CERT", 2),
    ("SSL_VERIFY_NONE", 0),
    ("SSL_VERIFY_PEER", 1),
])


def _register_default_ciphers(self):
    pass


def _register_x509_ext_parsers(self):
    pass


def _register_x509_encoders(self):
    pass


def _HashContext(backend, algorithm, ctx=None):
    # type: (Backend, HashAlgorithm, _HashContext) -> _HashContext
    self = HashContext(algorithm)

    def __init__(backend, algorithm, ctx=None):
        self._backend = backend
        self._algorithm = algorithm
        if not ctx:
            hash_algo, _, bits = algorithm.name.upper().partition('-')
            ctx = getattr(Hash, hash_algo, None)
            if not ctx:
                fail("{} is not a supported hash on this backend.".format(
                                        algorithm.name
                                    ))
            self._ctx = ctx.new()
        else:
            self._ctx = ctx
        return self
    self = __init__(backend, algorithm, ctx)

    def update(data):
        # type: (bytes) -> None
        self._ctx.update(data)
    self.update = update

    def finalize():
        # type: () -> bytes
        dig = self._ctx.digest()
        self._ctx = None
        return dig
    self.finalize = finalize

    def copy():
        # type: () -> _HashContext
        return _HashContext(self._backend, self._algorithm, self._ctx.copy())
    self.copy = copy
    return self


def pycryptodome():
    """
    PyCryptodome API interface implementation of `Cryptography`'s
    `backend/interfaces:Backend`
    """
    self = larky.mutablestruct(__name__='pycryptodome', __class__=pycryptodome)
    self.name = "pycryptodome"

    def __init__():
        self._cipher_registry = {}
        _register_default_ciphers(self)
        _register_x509_ext_parsers(self)
        _register_x509_encoders(self)
        # from https://github.com/openssl/openssl/blob/9dddcd90a1350fa63486cbf3226c3eee79f9aff5/crypto/evp/p_lib.c#L907-L925
        # The EVP_PKEY_DH type is used for dh parameter generation types:
        #  - named safe prime groups related to ffdhe and modp
        #  - safe prime generator
        #
        # The type EVP_PKEY_DHX is used for dh parameter generation types
        #  - fips186-4 and fips186-2
        #  - rfc5114 named groups.
        #
        # The EVP_PKEY_DH type is used to save PKCS#3 data than can be stored
        # without a q value.
        # The EVP_PKEY_DHX type is used to save X9.42 data that requires the
        # q value to be stored.
        self._dh_types = [_OpenSSL_Constants.EVP_PKEY_DH]
        self._dh_types.append(_OpenSSL_Constants.EVP_PKEY_DHX)
        return self
    self = __init__()

    def create_hash_ctx(algorithm):
        return _HashContext(self, algorithm)

    return self


backend = pycryptodome