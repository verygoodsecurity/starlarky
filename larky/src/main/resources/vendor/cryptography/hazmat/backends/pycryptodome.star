# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//enum", enum="enum")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//jopenssl", _JOpenSSL="jopenssl")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//Crypto/Hash", Hash="Hash")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//Crypto/Signature/pkcs1_15", pkcs1_15="pkcs1_15")
load("@vendor//Crypto/Signature/pss", pss="pss")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes")
load("@vendor//cryptography/hazmat/_oid", oid="oid")
load("@vendor//cryptography/hazmat/primitives/_serialization", serialization="serialization")
load("@vendor//cryptography/hazmat/primitives/_hashes", hashes="hashes")
# load("@vendor//cryptography/hazmat/primitives/asymmetric/utils", asym_utils="utils")
load("@vendor//cryptography/x509/_base", X509Version="Version")
load("@vendor//cryptography/utils", utils="utils")

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


def _rsa_sig_verify(backend, padding, algorithm, public_key, signature, data):
    # public_key.verify(
            #     b64decode(signature_value), data, padding.PKCS1v15(), digest()
            # )
    pkey_ctx = _rsa_sig_setup(
        backend,
        padding,
        algorithm,
        public_key,
        None,
    )

    hashctx = backend.create_hash_ctx(algorithm)
    hashctx.update(data)
    pkey_ctx.new(public_key._evp_pkey).verify(hashctx._ctx, signature)


def _rsa_sig_determine_padding(backend, key, padding, algorithm):
    if type(padding) == 'PKCS1v15':
        # Hash algorithm is ignored for PKCS1v15-padding, may be None.
        return pkcs1_15
    elif type(padding) == 'PSS':
        if not type(padding._mgf) == 'MGF1':
            fail("Only MGF1 is supported by this backend.")
        # PSS padding requires a hash algorithm
        if not builtins.isinstance(algorithm, hashes.HashAlgorithm):
            fail("Expected instance of hashes.HashAlgorithm.")

        pkey_size = key.bits
        # Size of key in bytes - 2 is the maximum
        # PSS signature length (salt length is checked later)
        if pkey_size - algorithm.digest_size - 2 < 0:
            fail(
                "Digest too large for key size. Use a larger " +
                "key or different digest."
            )

        return pss
    fail("{} is not supported by this backend.".format(padding.name))


# Hash algorithm can be absent (None) to initialize the context without setting
# any message digest algorithm. This is currently only valid for the PKCS1v15
# padding type, where it means that the signature data is encoded/decoded
# as provided, without being wrapped in a DigestInfo structure.
def _rsa_sig_setup(backend, padding, algorithm, key, init_func):
    padding_cls = _rsa_sig_determine_padding(backend, key, padding, algorithm)
    if type(padding) != 'PKCS1v15':
        fail("Unsupported")
        # TODO(mahmoudimus): support pss
        # mask_func = kwargs.pop("mask_func", None)
        # salt_len = kwargs.pop("salt_bytes", None)
        # rand_func = kwargs.pop("rand_func", None)
    return padding_cls


def _rsa_sig_sign(backend, padding, algorithm, private_key, data):
    pkey_ctx = _rsa_sig_setup(
        backend,
        padding,
        algorithm,
        private_key,
        None
    )
    hashctx = backend.create_hash_ctx(algorithm)
    hashctx.update(data)
    return (pkey_ctx
            .new(private_key._evp_pkey)
            .sign(hashctx._ctx))


def _calculate_digest_and_algorithm(backend, data, algorithm):
    if not type(algorithm) == 'Prehashed':
        hash_ctx = hashes.Hash(algorithm, backend)
        hash_ctx.update(data)
        data = hash_ctx.finalize()
    else:
        algorithm = algorithm._algorithm

    if len(data) != algorithm.digest_size:
        fail(
            "The provided data must be the same length as the hash " +
            "algorithm's digest size."
        )

    return (data, algorithm)


def RSAPrivateKey(backend, rsa_cdata, evp_pkey):
    self = larky.mutablestruct(__name__='RSAPrivateKey',
                               __class__=RSAPrivateKey)

    def __init__(backend, rsa_cdata, evp_pkey):
        self._backend = backend
        self._rsa_cdata = rsa_cdata
        self._evp_pkey = evp_pkey
        return self
    self = __init__(backend, rsa_cdata, evp_pkey)

    def decrypt(ciphertext, padding):
        """
        Decrypts the provided ciphertext.
        """
    self.decrypt = decrypt

    def key_size():
        """
        The bit length of the public modulus.
        """
        return self._rsa_cdata.bits
    self.key_size = key_size

    def public_key():
        """
        The RSAPublicKey associated with this private key.
        """
        return RSAPublicKey(self._backend, self._evp_pkey.public_key())
    self.public_key = public_key

    def sign(
        data,  # type: bytes
        padding, # type: AsymmetricPadding
        algorithm,  # type: Union[Prehashed, hashes.HashAlgorithm]
    ):
        """
        Signs the data.
        """
        return _rsa_sig_sign(self._backend, padding, algorithm, self, data)
    self.sign = sign

    def private_numbers():
        """
        Returns an RSAPrivateNumbers.
        """
    self.private_numbers = private_numbers

    def private_bytes(
        encoding,
        format,
        encryption_algorithm,
    ):
        """
        Returns the key serialized as bytes.
        """
    self.private_bytes = private_bytes
    return self


def RSAPublicKey(backend, evp_pkey):
    self = larky.mutablestruct(__name__='RSAPublicKey', __class__=RSAPublicKey)

    def __init__(backend, evp_pkey):
        self._backend = backend
        self._evp_pkey = evp_pkey
        return self
    self = __init__(backend, evp_pkey)
    def encrypt(plaintext, padding):
        """
        Encrypts the given plaintext.
        """
    self.encrypt = encrypt

    def key_size():
        """
        The bit length of the public modulus.
        """
    self.key_size = key_size

    def public_numbers():
        """
        Returns an RSAPublicNumbers
        """
    self.public_numbers = public_numbers

    def public_bytes(
        encoding,
        format,
    ):
        """
        Returns the key serialized as bytes.
        """
    self.public_bytes = public_bytes

    def verify(
        signature,
        data,
        padding,
        algorithm,
    ):
        """
        Verifies the signature of the data.
        """
        data, algorithm = _calculate_digest_and_algorithm(
            self._backend, data, algorithm
        )
        return _rsa_sig_verify(
            self._backend, padding, algorithm, self, signature, data
        )
    self.verify = verify

    def recover_data_from_signature(
        signature,
        padding,
        algorithm,
    ):
        """
        Recovers the original data from the signature.
        """
    self.recover_data_from_signature = recover_data_from_signature
    return self


def RSAPrivateNumbers(p,
    q,
    d,
    dmp1,
    dmq1,
    iqmp,
    public_numbers,
):
    self = larky.mutablestruct(__name__='RSAPrivateNumbers', __class__=RSAPrivateNumbers)
    def __init__(
        p,
        q,
        d,
        dmp1,
        dmq1,
        iqmp,
        public_numbers,
    ):
        if (
            not types.is_int(p)
            or not types.is_int(q)
            or not types.is_int(d)
            or not types.is_int(dmp1)
            or not types.is_int(dmq1)
            or not types.is_int(iqmp)
        ):
            fail("TypeError: " + ("RSAPrivateNumbers p, q, d, dmp1, dmq1, iqmp arguments must" +
                " all be an integers.")
            )

        if not hasattr(public_numbers, 'public_key'):
            fail("TypeError: RSAPrivateNumbers public_numbers must be " +
                 "an RSAPublicNumbers instance.")

        self._p = p
        self._q = q
        self._d = d
        self._dmp1 = dmp1
        self._dmq1 = dmq1
        self._iqmp = iqmp
        self._public_numbers = public_numbers
        return self
    self = __init__(p, q, d, dmp1, dmq1, iqmp, public_numbers)

    self.p = larky.property(lambda: self._p)
    self.q = larky.property(lambda: self._q)
    self.d = larky.property(lambda: self._d)
    self.dmp1 = larky.property(lambda: self._dmp1)
    self.dmq1 = larky.property(lambda: self._dmq1)
    self.iqmp = larky.property(lambda: self._iqmp)
    self.public_numbers = larky.property(lambda: self._public_numbers)

    def private_key(backend = None):
        # backend = _get_backend(backend)
        return backend.load_rsa_private_numbers(self)
    self.private_key = private_key

    def __eq__(other):
        if not builtins.isinstance(other, RSAPrivateNumbers):
            return builtins.NotImplemented

        return (
            self.p == other.p
            and self.q == other.q
            and self.d == other.d
            and self.dmp1 == other.dmp1
            and self.dmq1 == other.dmq1
            and self.iqmp == other.iqmp
            and self.public_numbers == other.public_numbers
        )
    self.__eq__ = __eq__

    def __ne__(other):
        return not self.__eq__(other)
    self.__ne__ = __ne__

    def __hash__():
        return hash(
            (
                self.p,
                self.q,
                self.d,
                self.dmp1,
                self.dmq1,
                self.iqmp,
                self.public_numbers,
            )
        )
    self.__hash__ = __hash__
    return self



def RSAPublicNumbers(e, n):

    self = larky.mutablestruct(
        __name__='RSAPublicNumbers',
        __class__=RSAPublicNumbers
    )

    def __init__(e, n):
        if not types.is_int(e) or not types.is_int(n):
            fail("TypeError: RSAPublicNumbers arguments must be integers.")
        self._e = e
        self._n = n
        return self
    self = __init__(e, n)

    self.e = larky.property(lambda: self._e)
    self.n = larky.property(lambda: self._n)

    def public_key(backend = None):
        # backend = _get_backend(backend)
        return pycryptodome().load_rsa_public_numbers(self)
    self.public_key = public_key

    def __repr__():
        return "<RSAPublicNumbers(e={}, n={})>".format(self._e, self._n)
    self.__repr__ = __repr__

    def __eq__(other):
        if not builtins.isinstance(other, RSAPublicNumbers):
            return builtins.NotImplemented

        return self.e == other.e and self.n == other.n
    self.__eq__ = __eq__

    def __ne__(other):
        return not self == other
    self.__ne__ = __ne__

    def __hash__():
        return hash((self.e, self.n,))
    self.__hash__ = __hash__
    return self


def _HashContext(algorithm, backend, ctx=None):
    # type: (HashAlgorithm, Backend, _HashContext) -> _HashContext
    self = hashes.HashContext(algorithm)

    def __init__(algorithm, backend, ctx=None):
        self._algorithm = algorithm
        self._backend = backend
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
    self = __init__(algorithm, backend, ctx)

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
        return _HashContext(self._algorithm, self._backend, self._ctx.copy())
    self.copy = copy
    return self


def _asn1_string_to_bytes(*args):
    print(*args)
    return args[1]


def _decode_x509_name(*args):
    print(*args)
    return args[0]


def _Certificate(backend, x509_cert):
    self = larky.mutablestruct(__name__='Certificate', __class__=_Certificate)

    def __init__(backend, x509_cert):
        self._backend = backend
        self._x509 = x509_cert
        # Keep-alive reference used by OCSP
        self._ocsp_resp_ref = None

        # version = self._backend.X509_get_version(self._x509)
        version = self._x509.version()
        if version == 1:
            self._version = X509Version.v1
        elif version == 3:
            self._version = X509Version.v3
        else:
            fail("{} is not a valid X509 version".format(version))
        return self
    self = __init__(backend, x509_cert)

    def __repr__():
        return "<Certificate(subject={}, ...)>".format(self.subject)
    self.__repr__ = __repr__

    def __eq__(other):
        if not builtins.isinstance(other, self.__class__):
            return builtins.NotImplemented
        return self._x509 == other._x509

    self.__eq__ = __eq__

    def __ne__(other):
        return not self.__eq__(other)
    self.__ne__ = __ne__
#
    def __hash__():
        return hash(self.public_bytes(serialization.Encoding.DER))
    self.__hash__ = __hash__

    def fingerprint(algorithm):
        return self._x509.fingerprint(algorithm.name)
        # ctx = self._backend.create_hash_ctx(algorithm)
        # ctx.update(self.public_bytes(serialization.Encoding.DER))
        # return ctx.finalize()
    self.fingerprint = fingerprint

    self.version = larky.property(lambda: self._version)

    # def serial_number():
    #     return self._x509.serial_number
    # self.serial_number = serial_number
    self.serial_number = larky.property(self._x509.serial_number)

    def public_key():
        # fail("ValueError: Certificate public key is of an unknown type")
        print(self._x509.public_key())
        return RSAPublicKey(self, self._x509.public_key())
        # return self._backend._evp_pkey_to_public_key(self._x509.public_key())
    self.public_key = public_key
#
#     def not_valid_before():
#         asn1_time = self._backend._lib.X509_get0_notBefore(self._x509)
#         return _parse_asn1_time(self._backend, asn1_time)
    self.not_valid_before = larky.property(self._x509.not_valid_before)
#     not_valid_before = property(not_valid_before)
#
#     def not_valid_after():
#         asn1_time = self._backend._lib.X509_get0_notAfter(self._x509)
#         return _parse_asn1_time(self._backend, asn1_time)
#     self.not_valid_after = not_valid_after
    self.not_valid_after = larky.property(self._x509.not_valid_after)
    self.issuer = larky.property(lambda: _decode_x509_name(self._x509.issuer()))
    self.subject = larky.property(lambda: _decode_x509_name(self._x509.subject()))

    def signature_hash_algorithm():
        oid = self.signature_algorithm_oid
        roid = oid._SIG_OIDS_TO_HASH.get(oid, None)
        print(self._x509.signature_hash_algorithm())
        if roid == None:
            fail("Signature algorithm OID:{} not recognized".format(oid))
        return oid
    self.signature_hash_algorithm = larky.property(signature_hash_algorithm)

    def signature_algorithm_oid():
        # alg = self._backend._ffi.new("X509_ALGOR **")
        # self._backend._lib.X509_get0_signature(
        #     self._backend._ffi.NULL, alg, self._x509
        # )
        # self._backend.openssl_assert(alg[0] != self._backend._ffi.NULL)
        # oid = _obj2txt(self._backend, alg[0].algorithm)
        return oid.ObjectIdentifier(self._x509.signature_algorithm_oid())
    self.signature_algorithm_oid = larky.property(signature_algorithm_oid)

    def extensions():
        return self._x509.extensions()
    self.extensions = extensions

    def signature():
        # type: () -> bytes
        """
        Returns the signature bytes.
        """
        return self._x509.signature()
    self.signature = larky.property(signature)

    def tbs_certificate_bytes():
        # type: () -> bytes
        """
        Returns the tbsCertificate payload bytes as defined in RFC 5280.
        """
        return self._x509.tbs_certificate_bytes()
    self.tbs_certificate_bytes = larky.property(tbs_certificate_bytes)

    def public_bytes(encoding):
        return self._x509.public_bytes(encoding)
    self.public_bytes = public_bytes
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
        return _HashContext(algorithm, self)
    self.create_hash_ctx = create_hash_ctx

    def _evp_pkey_to_private_key(evp_pkey):
        """
        Return the appropriate type of PrivateKey given an evp_pkey cdata
        pointer.
        """
        key_type = evp_pkey.key_type
        if key_type == 'RSA':
            return RSAPrivateKey(self, evp_pkey, RSA.import_key(evp_pkey.private_key()))

        # elif key_type == self._lib.EVP_PKEY_DSA:
        #     dsa_cdata = self._lib.EVP_PKEY_get1_DSA(evp_pkey)
        #     self.openssl_assert(dsa_cdata != self._ffi.NULL)
        #     dsa_cdata = self._ffi.gc(dsa_cdata, self._lib.DSA_free)
        #     return _DSAPrivateKey(self, dsa_cdata, evp_pkey)
        # elif key_type == self._lib.EVP_PKEY_EC:
        #     ec_cdata = self._lib.EVP_PKEY_get1_EC_KEY(evp_pkey)
        #     self.openssl_assert(ec_cdata != self._ffi.NULL)
        #     ec_cdata = self._ffi.gc(ec_cdata, self._lib.EC_KEY_free)
        #     return _EllipticCurvePrivateKey(self, ec_cdata, evp_pkey)
        # elif key_type in self._dh_types:
        #     dh_cdata = self._lib.EVP_PKEY_get1_DH(evp_pkey)
        #     self.openssl_assert(dh_cdata != self._ffi.NULL)
        #     dh_cdata = self._ffi.gc(dh_cdata, self._lib.DH_free)
        #     return _DHPrivateKey(self, dh_cdata, evp_pkey)
        # elif key_type == getattr(self._lib, "EVP_PKEY_ED25519", None):
        #     # EVP_PKEY_ED25519 is not present in OpenSSL < 1.1.1
        #     return _Ed25519PrivateKey(self, evp_pkey)
        # elif key_type == getattr(self._lib, "EVP_PKEY_X448", None):
        #     # EVP_PKEY_X448 is not present in OpenSSL < 1.1.1
        #     return _X448PrivateKey(self, evp_pkey)
        # elif key_type == getattr(self._lib, "EVP_PKEY_X25519", None):
        #     # EVP_PKEY_X25519 is not present in OpenSSL < 1.1.0
        #     return _X25519PrivateKey(self, evp_pkey)
        # elif key_type == getattr(self._lib, "EVP_PKEY_ED448", None):
        #     # EVP_PKEY_ED448 is not present in OpenSSL < 1.1.1
        #     return _Ed448PrivateKey(self, evp_pkey)
        # else:
        fail("UnsupportedAlgorithm: Unsupported key type: %s" % key_type)
    self._evp_pkey_to_private_key = _evp_pkey_to_private_key

    def _evp_pkey_to_public_key(pkey):
        """
        Return the appropriate type of PublicKey given an pkey cdata
        """

        key_type = pkey.key_type
        if key_type == 'RSA':
            return RSAPublicKey(self, RSA.import_key(pkey.private_key()))
        fail("UnsupportedAlgorithm: Unsupported key type: %s" % key_type)

        #
        # if key_type == self._lib.EVP_PKEY_RSA:
        #     rsa_cdata = self._lib.EVP_PKEY_get1_RSA(evp_pkey)
        #     self.openssl_assert(rsa_cdata != self._ffi.NULL)
        #     rsa_cdata = self._ffi.gc(rsa_cdata, self._lib.RSA_free)
        #     return _RSAPublicKey(self, rsa_cdata, evp_pkey)
        # elif key_type == self._lib.EVP_PKEY_DSA:
        #     dsa_cdata = self._lib.EVP_PKEY_get1_DSA(evp_pkey)
        #     self.openssl_assert(dsa_cdata != self._ffi.NULL)
        #     dsa_cdata = self._ffi.gc(dsa_cdata, self._lib.DSA_free)
        #     return _DSAPublicKey(self, dsa_cdata, evp_pkey)
        # elif key_type == self._lib.EVP_PKEY_EC:
        #     ec_cdata = self._lib.EVP_PKEY_get1_EC_KEY(evp_pkey)
        #     self.openssl_assert(ec_cdata != self._ffi.NULL)
        #     ec_cdata = self._ffi.gc(ec_cdata, self._lib.EC_KEY_free)
        #     return _EllipticCurvePublicKey(self, ec_cdata, evp_pkey)
        # elif key_type in self._dh_types:
        #     dh_cdata = self._lib.EVP_PKEY_get1_DH(evp_pkey)
        #     self.openssl_assert(dh_cdata != self._ffi.NULL)
        #     dh_cdata = self._ffi.gc(dh_cdata, self._lib.DH_free)
        #     return _DHPublicKey(self, dh_cdata, evp_pkey)
        # elif key_type == getattr(self._lib, "EVP_PKEY_ED25519", None):
        #     # EVP_PKEY_ED25519 is not present in OpenSSL < 1.1.1
        #     return _Ed25519PublicKey(self, evp_pkey)
        # elif key_type == getattr(self._lib, "EVP_PKEY_X448", None):
        #     # EVP_PKEY_X448 is not present in OpenSSL < 1.1.1
        #     return _X448PublicKey(self, evp_pkey)
        # elif key_type == getattr(self._lib, "EVP_PKEY_X25519", None):
        #     # EVP_PKEY_X25519 is not present in OpenSSL < 1.1.0
        #     return _X25519PublicKey(self, evp_pkey)
        # elif key_type == getattr(self._lib, "EVP_PKEY_ED448", None):
        #     # EVP_PKEY_X25519 is not present in OpenSSL < 1.1.1
        #     return _Ed448PublicKey(self, evp_pkey)
        # else:
        #     raise UnsupportedAlgorithm("Unsupported key type.")
    self._evp_pkey_to_public_key = _evp_pkey_to_public_key

    def load_key_and_certificates_from_pkcs12(data, password):
        if password != None:
            utils._check_byteslike("password", password)
        # fail("ValueError: Could not deserialize PKCS12 data")
        # fail("Invalid password or PKCS12 data")
        # # In OpenSSL < 3.0.0 PKCS12 parsing reverses the order of the
        # # certificates.
        # cert = None
        # key = None
        # additional_certificates = []
        key, cert, additional_certificates = _JOpenSSL.OpenSSL.load_key_and_certificates_from_pkcs12(tobytes(data), password)
        return RSAPrivateKey(self, key, RSA.import_key(key.private_key())), _Certificate(self, cert), [_Certificate(self, c) for c in additional_certificates]
    self.load_key_and_certificates_from_pkcs12 = load_key_and_certificates_from_pkcs12

    def load_rsa_private_numbers(private_numbers):
        return
    self.load_rsa_private_numbers = load_rsa_private_numbers

    def load_pem_private_key(data, password):
        if password != None:
            utils._check_byteslike("password", password)
        # fail("TypeError: Password was not given but private key is encrypted")
        evp_pkey = _JCrypto.IO.PEM.load_privatekey(data, password)
        return self._evp_pkey_to_private_key(evp_pkey)

    self.load_pem_private_key = load_pem_private_key
    return self


backend = pycryptodome