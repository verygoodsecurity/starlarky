load("@stdlib//base64", b64encode="b64encode", b64decode="b64decode")
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//larky", larky="larky")
load("@stdlib//sets", sets="sets")
load("@stdlib//struct", struct="struct")
load("@stdlib//operator", operator="operator")
load("@stdlib//types", types="types")
load("@stdlib//math", math="math")

load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/Cipher/PKCS1_v1_5", PKCS1_v1_5_Cipher="PKCS1_v1_5_Cipher")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Cipher/PKCS1_OAEP", PKCS1_OAEP="PKCS1_OAEP")
load("@vendor//Crypto/Hash/HMAC", HMAC="HMAC")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")
load("@vendor//Crypto/Hash/SHA384", SHA384="SHA384")
load("@vendor//Crypto/Hash/SHA512", SHA512="SHA512")
load("@vendor//Crypto/Hash/SHA1", SHA1="SHA1")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//Crypto/PublicKey/ECC", ECC="ECC")
load("@vendor//Crypto/Signature/DSS", DSS="DSS")
load("@vendor//Crypto/Signature/PKCS1_v1_5", PKCS1_v1_5_Signature="PKCS1_v1_5")
load("@vendor//Crypto/Util/asn1", DerSequence="DerSequence")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", tostr="tostr")
load("@vendor//Crypto/Util/number", bytes_to_long="bytes_to_long")


load("@vendor//cryptography/hazmat/primitives/serialization/base", load_pem_public_key="load_pem_public_key", load_pem_private_key="load_pem_private_key")
load("@vendor//cryptography/hazmat/primitives/asymmetric/utils", decode_dss_signature="decode_dss_signature",
     encode_dss_signature="encode_dss_signature")
load("@vendor//cryptography/utils", int_to_bytes="int_to_bytes")
# load("@vendor//jose/backends/_asn1", rsa_public_key_pkcs8_to_pkcs1="rsa_public_key_pkcs8_to_pkcs1")
load("@vendor//jose/backends/base", Key="Key")
load("@vendor//jose/constants", ALGORITHMS="ALGORITHMS")
load("@vendor//jose/exceptions", JWKError="JWKError", JWEError="JWEError", JWEAlgorithmUnsupportedError="JWEAlgorithmUnsupportedError")
load("@vendor//jose/utils",
     base64url_encode="base64url_encode",
     base64url_decode="base64url_decode",
     base64_to_long="base64_to_long",
     long_to_base64="long_to_base64")

load("@vendor//option/result", Ok="Ok", Error="Error", safe="safe")
load("@vendor//six", six="six")

ceil = math.ceil

# We default to using PyCryptodome, however, if PyCrypto is installed, it is
# used instead. This is so that environments that require the use of PyCrypto
# are still supported.
_RSAKey = RSA.RsaKey


def get_random_bytes(num_bytes):
    return bytes(Random.new().read(num_bytes))


def _der_to_pem(der_key, marker):
    """
    Perform a simple DER to PEM conversion.
    """
    pem_key_chunks = [codecs.encode(('-----BEGIN %s-----' % marker), encoding='utf-8')]

    # Limit base64 output lines to 64 characters by limiting input lines to 48 characters.
    for chunk_start in range(0, len(der_key), 48):
        pem_key_chunks.append(b64encode(der_key[chunk_start:chunk_start + 48]))

    pem_key_chunks.append(codecs.encode('-----END %s-----' % marker, encoding='utf-8'))

    return b'\n'.join(pem_key_chunks)


def ECKey(key, algorithm):
    self = larky.mutablestruct(__class__=ECKey, __name__='ECKey',
                               SHA256=SHA256,
                               SHA384=SHA384,
                               SHA512=SHA512)
    self.SHA256 = SHA256
    self.SHA384 = SHA384
    self.SHA512 = SHA512

    def __init__(key, algorithm):
        if not operator.contains(ALGORITHMS.EC, algorithm):
            return Error("JWKError: %s is not a valid hash algorithm" % algorithm)
        self.hash_alg = {
            ALGORITHMS.ES256: self.SHA256,
            ALGORITHMS.ES384: self.SHA384,
            ALGORITHMS.ES512: self.SHA512,
        }.get(algorithm)
        self._algorithm = algorithm
        if builtins.isinstance(key, ECC.EccKey):
            self.prepared_key = key
            return self

        if types.is_dict(key):
            self.prepared_key = self._process_jwk(key)

        if types.is_string(key):
            key = tobytes(key)

        if types.is_bytelike(key):
            self.prepared_key = ECC.import_key(key)
            return self

        fail('JWKError: Unable to parse an ECKey from key: %s' % key)
    self = __init__(key, algorithm)

    def _process_jwk(jwk_dict):
        if not jwk_dict.get("kty") == "EC":
            return Error("Incorrect key type. Expected: 'EC', Received: %s" % jwk_dict.get("kty"))
        if not all([k in jwk_dict for k in ["x", "y", "crv"]]):
            return Error("Mandatory parameters are missing")
        x = base64_to_long(jwk_dict.get("x"))
        y = base64_to_long(jwk_dict.get("y"))
        curve = {
            "P-256": "secp256r1",
            "P-384": "secp384r1",
            "P-521": "secp521r1",
        }[jwk_dict["crv"]]
        if "d" in jwk_dict:
            # private key
            d = base64_to_long(jwk_dict.get("d"))
            return ECC.construct(point_x=x, point_y=y, d=d, curve=curve)
        else:
            return ECC.construct(point_x=x, point_y=y, curve=curve)
    self._process_jwk = _process_jwk

    def sign(msg):
        hashed = self.hash_alg.new(msg)
        signer = DSS.new(self.prepared_key, 'fips-186-3')
        return signer.sign(hashed)
    self.sign = sign

    def verify(msg, sig):
        hashed = self.hash_alg.new(msg)
        signer = DSS.new(self.prepared_key, 'fips-186-3')
        result = Ok(signer.verify).map(lambda v: v(hashed, sig))
        return True if result.is_ok else False
    self.verify = verify

    return self


def RSAKey(key, algorithm):
    """
    Performs signing and verification operations using
    RSASSA-PKCS-v1_5 and the specified hash function.
    This class requires PyCrypto package to be installed.
    This is based off of the implementation in PyJWT 0.3.2
    """

    self = larky.mutablestruct(__class__=RSAKey,
                               __name__='RSAKey',
                               SHA256=SHA256,
                               SHA384=SHA384,
                               SHA512=SHA512,
                               SHA1=SHA1)

    def __init__(key, algorithm):
        if not operator.contains(ALGORITHMS.RSA, algorithm):
            return Error("JWKError: hash_alg: %s is not a valid hash algorithm" % algorithm).unwrap()

        self.hash_alg = {
            ALGORITHMS.RS256: self.SHA256,
            ALGORITHMS.RS384: self.SHA384,
            ALGORITHMS.RS512: self.SHA512,
            ALGORITHMS.RSA1_5: self.SHA1,
            ALGORITHMS.RSA_OAEP: self.SHA1,
            ALGORITHMS.RSA_OAEP_256: self.SHA256,
        }.get(algorithm)
        self._algorithm = algorithm

        if types.is_instance(key, _RSAKey):
            self.prepared_key = key
            return self

        if types.is_dict(key):
            self._process_jwk(key)
            return self

        if types.is_string(key):
            key = codecs.encode(key, encoding='utf-8')

        if types.is_bytelike(key):
            self.prepared_key = RSA.importKey(key)
            return self

        return Error("JWKError: Unable to parse an RSA_JWK from key: %s" % key).unwrap()
    self = __init__(key, algorithm)

    def _process_jwk(jwk_dict):
        if not jwk_dict.get('kty') == 'RSA':
            return Error("JWKError: Incorrect key type. Expected: 'RSA', Received: %s" % jwk_dict.get('kty'))

        e = base64_to_long(jwk_dict.get('e', 256))
        n = base64_to_long(jwk_dict.get('n'))
        params = (n, e)

        if 'd' in jwk_dict:
            params += (base64_to_long(jwk_dict.get('d')),)

            extra_params = ['p', 'q', 'dp', 'dq', 'qi']

            if any([k in jwk_dict for k in extra_params]):
                # Precomputed private key parameters are available.
                if not all([k in jwk_dict for k in extra_params]):
                    # These values must be present when 'p' is according to
                    # Section 6.3.2 of RFC7518, so if they are not we raise
                    # an error.
                    return Error("JWKError: Precomputed private key parameters are incomplete.")

                p = base64_to_long(jwk_dict.get('p'))
                q = base64_to_long(jwk_dict.get('q'))
                qi = base64_to_long(jwk_dict.get('qi'))

                # PyCrypto does not take the dp and dq as arguments, so we do
                # not pass them. Furthermore, the parameter qi specified in
                # the JWK is the inverse of q modulo p, whereas PyCrypto
                # takes the inverse of p modulo q. We therefore switch the
                # parameters to make the third parameter the inverse of the
                # second parameter modulo the first parameter.
                params += (q, p, qi)

        self.prepared_key = RSA.construct(params)

        return self.prepared_key
    self._process_jwk = _process_jwk

    def _process_cert(key):
        pemLines = key.replace(b' ', b'').split()
        certDer = base64url_decode(b''.join(pemLines[1:-1]))
        certSeq = DerSequence()
        certSeq.decode(certDer)
        tbsSeq = DerSequence()
        tbsSeq.decode(certSeq[0])
        self.prepared_key = RSA.importKey(tbsSeq[6])
        return
    self._process_cert = _process_cert

    def sign(msg):
        signature = PKCS1_v1_5_Signature.new(self.prepared_key)
        res = Ok(signature.sign).map(lambda sign: sign(self.hash_alg.new(msg)))
        return res.unwrap()
    self.sign = sign

    def verify(msg, sig):
        if not self.is_public():
            print(
                "Attempting to verify a message with a private key. " +
                "This is not recommended.")
        _pkcs_inst = PKCS1_v1_5_Signature.new(self.prepared_key)
        _res = (Ok(_pkcs_inst.verify)
                .map(lambda v: v(self.hash_alg.new(msg), sig)))
        return _res.unwrap_or(False)
    self.verify = verify

    def is_public():
        return not self.prepared_key.has_private()
    self.is_public = is_public

    def public_key():
        if self.is_public():
            return self
        return self.__class__(self.prepared_key.publickey(), self._algorithm)
    self.public_key = public_key

    def to_pem(pem_format='PKCS8'):
        if pem_format == 'PKCS8':
            pkcs = 8
        elif pem_format == 'PKCS1':
            pkcs = 1
        else:
            return Error("ValueError: Invalid pem format specified: %r" % (pem_format,))

        if self.is_public():
            # PyCrypto/dome always export public keys as PKCS8
            if pkcs == 8:
                pem = self.prepared_key.exportKey('PEM')
            else:
                pkcs8_der = self.prepared_key.exportKey('DER')
                pkcs1_der = pkcs8_der # rsa_public_key_pkcs8_to_pkcs1(pkcs8_der)
                pem = _der_to_pem(pkcs1_der, 'RSA PUBLIC KEY')
            return pem
        else:
            pem = self.prepared_key.exportKey('PEM', pkcs=pkcs)
        return pem
    self.to_pem = to_pem

    def to_dict():
        data = {
            'alg': self._algorithm,
            'kty': 'RSA',
            'n': codecs.decode(long_to_base64(self.prepared_key.n), encoding='ASCII'),
            'e': codecs.decode(long_to_base64(self.prepared_key.e), encoding='ASCII'),
        }

        if not self.is_public():
            # Section 6.3.2 of RFC7518 prescribes that when we include the
            # optional parameters p and q, we must also include the values of
            # dp and dq, which are not readily available from PyCrypto - so we
            # calculate them. Moreover, PyCrypto stores the inverse of p
            # modulo q rather than the inverse of q modulo p, so we switch
            # p and q. As far as I can tell, this is OK - RFC7518 only
            # asserts that p is the 'first factor', but does not specify
            # what 'first' means in this case.
            dp = self.prepared_key.d % (self.prepared_key.p - 1)
            dq = self.prepared_key.d % (self.prepared_key.q - 1)
            data.update({
                'd': long_to_base64(self.prepared_key.d).decode('ASCII'),
                'p': long_to_base64(self.prepared_key.q).decode('ASCII'),
                'q': long_to_base64(self.prepared_key.p).decode('ASCII'),
                'dp': long_to_base64(dq).decode('ASCII'),
                'dq': long_to_base64(dp).decode('ASCII'),
                'qi': long_to_base64(self.prepared_key.u).decode('ASCII'),
            })

        return data
    self.to_dict = to_dict

    def wrap(key_data, enc_alg=None, headers=None):
        return self.wrap_key(key_data)
    self.wrap = wrap

    def wrap_key(key_data):
        if self._algorithm == ALGORITHMS.RSA1_5:
            cipher = PKCS1_v1_5_Cipher.new(self.prepared_key)
        else:
            cipher = PKCS1_OAEP.new(self.prepared_key, self.hash_alg)
        wrapped_key = cipher.encrypt(key_data)
        return wrapped_key
    self.wrap_key = wrap_key

    def unwrap(wrapped_key, headers=None, enc_alg=None):
        return self.unwrap_key(wrapped_key)
    self.unwrap = unwrap

    def unwrap_key(wrapped_key):
        if self._algorithm == ALGORITHMS.RSA1_5:
            sentinel = Random.new().read(32)
            cipher = PKCS1_v1_5_Cipher.new(self.prepared_key)
            plain_text = cipher.decrypt(wrapped_key, sentinel)
        else:
            cipher = PKCS1_OAEP.new(self.prepared_key, self.hash_alg)
            plain_text = cipher.decrypt(wrapped_key)
        return plain_text
    self.unwrap_key = unwrap_key
    return self

# code ported from this commit:
# https://github.com/mpdavis/python-jose/commit/54417da04edbf00cf985fee283166255ab249a8a
def AESKey(key, algorithm):
    self = larky.mutablestruct(__class__=AESKey, __name__='AESKey')

    self.ALG_128 = (ALGORITHMS.A128GCM, ALGORITHMS.A128CBC_HS256, ALGORITHMS.A128GCMKW, ALGORITHMS.A128KW)
    self.ALG_192 = (ALGORITHMS.A192GCM, ALGORITHMS.A192CBC_HS384, ALGORITHMS.A192GCMKW, ALGORITHMS.A192KW)
    self.ALG_256 = (ALGORITHMS.A256GCM, ALGORITHMS.A256CBC_HS512, ALGORITHMS.A256GCMKW, ALGORITHMS.A256KW)

    self.AES_KW_ALGS = (ALGORITHMS.A128KW, ALGORITHMS.A192KW, ALGORITHMS.A256KW)

    self.MODES = {
        ALGORITHMS.A128CBC_HS256: AES.MODE_CBC,
        ALGORITHMS.A192CBC_HS384: AES.MODE_CBC,
        ALGORITHMS.A256CBC_HS512: AES.MODE_CBC,
        ALGORITHMS.A128CBC: AES.MODE_CBC,
        ALGORITHMS.A192CBC: AES.MODE_CBC,
        ALGORITHMS.A256CBC: AES.MODE_CBC,
        ALGORITHMS.A128KW: AES.MODE_ECB,
        ALGORITHMS.A192KW: AES.MODE_ECB,
        ALGORITHMS.A256KW: AES.MODE_ECB,
        #  pycryptdome supports GCM
        ALGORITHMS.A128GCMKW: AES.MODE_GCM,
        ALGORITHMS.A192GCMKW: AES.MODE_GCM,
        ALGORITHMS.A256GCMKW: AES.MODE_GCM,
        ALGORITHMS.A128GCM: AES.MODE_GCM,
        ALGORITHMS.A192GCM: AES.MODE_GCM,
        ALGORITHMS.A256GCM: AES.MODE_GCM,
    }


    def __init__(key, algorithm):
        if not operator.contains(ALGORITHMS.AES, algorithm):
            return Error("JWKError: %s is not a valid AES algorithm" % algorithm)
        if not operator.contains(
                ALGORITHMS.SUPPORTED.union(ALGORITHMS.AES_PSEUDO),
                algorithm
        ):
            return Error("JWKError: %s is not a supported algorithm" % algorithm)

        self._algorithm = algorithm
        self._mode = self.MODES.get(self._algorithm)
        if self._mode == None:
            return Error("JWEAlgorithmUnsupportedError: AES Mode is not supported by cryptographic library")

        if algorithm in self.ALG_128 and len(key) != 16:
            return Error("128 bit algo: %s's key (size %s) is not size 16" % (algorithm, len(key)))
        elif algorithm in self.ALG_192 and len(key) != 24:
            return Error("192 bit algo: %s's key (size %s) is not size 24" % (algorithm, len(key)))
        elif algorithm in self.ALG_256 and len(key) != 32:
            return Error("256 bit algo: %s's key (size %s) is not size 32" % (algorithm, len(key)))

        self._key = six.ensure_binary(key)
        self._key_size = len(key) * 8
        return Ok(self)
    self = __init__(key, algorithm).unwrap()

    def to_dict():
        data = {
            'alg': self._algorithm,
            'kty': 'oct',
            'k': self._key
        }
        return data
    self.to_dict = to_dict

    def encrypt(plain_text, aad=None):
        plain_text = six.ensure_binary(plain_text)
        def _try_encrypt(self, plain_text, aad):
            iv = get_random_bytes(AES.block_size)
            cipher = AES.new(self._key, self._mode, iv)
            if self._mode == AES.MODE_CBC:
                padded_plain_text = self._pad(AES.block_size, plain_text)
                cipher_text = cipher.encrypt(padded_plain_text)
                auth_tag = None
            else:
                cipher.update(aad)
                cipher_text, auth_tag = cipher.encrypt_and_digest(plain_text)
            return iv, cipher_text, auth_tag
        return safe(_try_encrypt)(self, plain_text, aad).unwrap()
    self.encrypt = encrypt

    def decrypt(cipher_text, iv=None, aad=None, tag=None):
        cipher_text = six.ensure_binary(cipher_text)
        cipher = AES.new(self._key, self._mode, iv)
        if self._mode == AES.MODE_CBC:
            padded_plain_text = cipher.decrypt(cipher_text)
            plain_text = self._unpad(padded_plain_text)
        else:
            cipher.update(aad)
            plain_text = cipher.decrypt_and_verify(cipher_text, tag)
            return plain_text # .expect("JWEError: Invalid JWE Auth Tag")
        return plain_text
    self.decrypt = decrypt

    def _pad(block_size, unpadded):
        padding_bytes = block_size - len(unpadded) % block_size
        padding = bytes(bytearray([padding_bytes]) * padding_bytes)
        return unpadded + padding
    self._pad = _pad

    def _unpad(padded):
        padded = six.ensure_binary(padded)
        padding_byte = padded[-1]
        padding_byte = ord(padding_byte)
        if padded[-padding_byte:] != bytearray([padding_byte]) * padding_byte:
            return Error("ValueError: Invalid padding!")
        return padded[:-padding_byte]
    self._unpad = _unpad

    def wrap(key_data, enc_alg, headers=None):
        if not headers:
            headers = {}
        if self._mode == AES.MODE_GCM:
            algo = AESGCMAlgorithm(self._key_size)
        else:
            algo = AESAlgorithm(self._key_size)

        wrapped = algo.wrap(enc_alg, headers, self._key, key_data)
        return wrapped['ek']
    self.wrap = wrap

    def wrap_key(key_data):
        return self.wrap(key_data, enc_alg=None)
    self.wrap_key = wrap_key

    def unwrap(wrapped_key, headers, enc_alg):
        if self._mode == AES.MODE_GCM:
            algo = AESGCMAlgorithm(self._key_size)
        else:
            algo = AESAlgorithm(self._key_size)
        cek = algo.unwrap(enc_alg, wrapped_key, headers, self._key)
        return cek
    self.unwrap = unwrap

    def unwrap_key(wrapped_key, headers=None):
        if not headers:
            headers = {}
        return self.unwrap(wrapped_key, headers, enc_alg=None)
    self.unwrap_key = unwrap_key

    return self


def AESAlgorithm(key_size):

    self = larky.mutablestruct(__name__='AESAlgorithm', __class__=AESAlgorithm)

    self.DEFAULT_IV = unhexlify("A6A6A6A6A6A6A6A6")
    self.IV_SIZE = 96

    def __init__(key_size):
        self.name = 'A{}KW'.format(key_size)
        self.description = 'AES Key Wrap using {}-bit key'.format(key_size)
        self.key_size = key_size
        return self
    self = __init__(key_size)

    def _check_key(key):
        if len(key) * 8 != self.key_size:
            fail('A key of size %s bits is required.' % self.key_size)
    self._check_key = _check_key

    def _wrap_core(wrapping_key, a, r):
        # RFC 3394 Key Wrap - 2.2.1 (index method)
        encryptor = AES.new(wrapping_key, AES.MODE_ECB).encrypt
        n = len(r)
        for j in range(6):
            for i in range(n):
                # every encryption operation is a discrete 16 byte chunk (because
                # AES has a 128-bit block size) and since we're using ECB it is
                # safe to reuse the encryptor for the entire operation
                b = encryptor(a + r[i])
                # pack/unpack are safe as these are always 64-bit chunks
                a = struct.pack(
                    ">Q", struct.unpack(">Q", b[:8])[0] ^ ((n * j) + i + 1)
                )
                r[i] = b[-8:]

        return a + b"".join(r)
    self._wrap_core = _wrap_core

    def aes_key_wrap(wrapping_key, key_to_wrap):
        if len(wrapping_key) not in [16, 24, 32]:
            return Error("ValueError: The wrapping key must be a valid AES key length").unwrap()

        if len(key_to_wrap) < 16:
            return Error("ValueError: The key to wrap must be at least 16 bytes").unwrap()

        if len(key_to_wrap) % 8 != 0:
            return Error("ValueError: The key to wrap must be a multiple of 8 bytes").unwrap()

        a = b"\xa6\xa6\xa6\xa6\xa6\xa6\xa6\xa6"
        r = [key_to_wrap[i : i + 8] for i in range(0, len(key_to_wrap), 8)]
        return self._wrap_core(wrapping_key, a, r)
    self.aes_key_wrap = aes_key_wrap

    def _unwrap_core(wrapping_key, a, r):
        # Implement RFC 3394 Key Unwrap - 2.2.2 (index method)
        decryptor = AES.new(wrapping_key, AES.MODE_ECB).decrypt
        n = len(r)
        for j in reversed(range(6)):
            for i in reversed(range(n)):
                # pack/unpack are safe as these are always 64-bit chunks
                atr = (
                    struct.pack(
                        ">Q", struct.unpack(">Q", a)[0] ^ ((n * j) + i + 1)
                    )
                    + r[i]
                )
                # every decryption operation is a discrete 16 byte chunk so
                # it is safe to reuse the decryptor for the entire operation
                b = decryptor(atr)
                a = b[:8]
                r[i] = b[-8:]

        return a, r
    self._unwrap_core = _unwrap_core

    def aes_key_wrap_with_padding(wrapping_key, key_to_wrap):
        if len(wrapping_key) not in [16, 24, 32]:
            return Error("ValueError: The wrapping key must be a valid AES key length").unwrap()

        aiv = b"\xA6\x59\x59\xA6" + struct.pack(">i", len(key_to_wrap))
        # pad the key to wrap if necessary
        pad = (8 - (len(key_to_wrap) % 8)) % 8
        key_to_wrap = key_to_wrap + b"\x00" * pad
        if len(key_to_wrap) == 8:
            # RFC 5649 - 4.1 - exactly 8 octets after padding
            encryptor = AES.new(wrapping_key, AES.MODE_ECB).encrypt
            b = encryptor(aiv + key_to_wrap)
            return b

        r = [key_to_wrap[i : i + 8] for i in range(0, len(key_to_wrap), 8)]
        return self._wrap_core(wrapping_key, aiv, r)
    self.aes_key_wrap_with_padding = aes_key_wrap_with_padding

    def aes_key_unwrap_with_padding(wrapping_key, wrapped_key):
        if len(wrapped_key) < 16:
            return Error("InvalidUnwrap: Must be at least 16 bytes").unwrap()

        if len(wrapping_key) not in [16, 24, 32]:
            return Error("ValueError: The wrapping key must be a valid AES key length").unwrap()

        if len(wrapped_key) == 16:
            # RFC 5649 - 4.2 - exactly two 64-bit blocks
            decryptor = AES.new(wrapping_key, AES.MODE_ECB).decrypt
            b = decryptor(wrapped_key)
            a = b[:8]
            data = b[8:]
            n = 1
        else:
            r = [wrapped_key[i : i + 8] for i in range(0, len(wrapped_key), 8)]
            encrypted_aiv = r.pop(0)
            n = len(r)
            a, r = self._unwrap_core(wrapping_key, encrypted_aiv, r)
            data = b"".join(r)

        # 1) Check that MSB(32,A) = A65959A6.
        # 2) Check that 8*(n-1) < LSB(32,A) <= 8*n.  If so, let
        #    MLI = LSB(32,A).
        # 3) Let b = (8*n)-MLI, and then check that the rightmost b octets of
        #    the output data are zero.
        (mli,) = struct.unpack(">I", a[4:])
        b = (8 * n) - mli
        if (
            not HMAC.compare_digest(a[:4], b"\xa6\x59\x59\xa6")
            or not (8 * (n - 1) < mli) and (mli <= 8 * n)
            or (b != 0 and not HMAC.compare_digest(data[-b:], b"\x00" * b))
        ):
            return Error("Invalid unwrap!").unwrap()

        if b == 0:
            return data

        return data[:-b]
    self.aes_key_unwrap_with_padding = aes_key_unwrap_with_padding

    def aes_key_unwrap(wrapping_key, wrapped_key):
        if len(wrapped_key) < 24:
            return Error("InvalidUnwrap: Must be at least 24 bytes").unwrap()

        if len(wrapped_key) % 8 != 0:
            return Error("InvalidUnwrap: The wrapped key must be a multiple of 8 bytes").unwrap()

        if len(wrapping_key) not in [16, 24, 32]:
            return Error("ValueError: The wrapping key must be a valid AES key length").unwrap()

        aiv = b"\xa6\xa6\xa6\xa6\xa6\xa6\xa6\xa6"
        r = [wrapped_key[i : i + 8] for i in range(0, len(wrapped_key), 8)]
        a = r.pop(0)
        a, r = self._unwrap_core(wrapping_key, a, r)
        if not HMAC.compare_digest(a, aiv):
            print(a.hex())
            print(aiv.hex())
            return Error("Invalid unwrap!").unwrap()

        return b"".join(r)
    self.aes_key_unwrap = aes_key_unwrap

    def wrap(enc_alg, headers, wrapping_key, key_to_wrap=None):
        if key_to_wrap == None:
            key_to_wrap = get_random_bytes(self.key_size // 8)
        if not headers.get("with_padding"):
            cipher_text = self.aes_key_wrap(wrapping_key, key_to_wrap)
        else:
            cipher_text = self.aes_key_wrap_with_padding(wrapping_key, key_to_wrap)
        return {"ek": cipher_text, "cek": key_to_wrap}  # IV, cipher text, auth tag
    self.wrap = wrap

    def unwrap(enc_alg, wrapped_key, headers, wrapping_key):
        wrapped_key = bytearray(six.ensure_binary(wrapped_key))
        if not headers.get("with_padding"):
            return self.aes_key_unwrap(wrapping_key, wrapped_key)
        else:
            return self.aes_key_unwrap_with_padding(wrapping_key, wrapped_key)
    self.unwrap = unwrap

    def _most_significant_bits(number_of_bits, _bytes):
        number_of_bytes = number_of_bits // 8
        msb = _bytes[:number_of_bytes]
        return msb
    self._most_significant_bits = _most_significant_bits

    def _least_significant_bits(number_of_bits, _bytes):
        number_of_bytes = number_of_bits // 8
        lsb = _bytes[-number_of_bytes:]
        return lsb
    self._least_significant_bits = _least_significant_bits
    return self


def AESGCMAlgorithm(key_size):
    EXTRA_HEADERS = sets.Set(['iv', 'tag'])
    self = larky.mutablestruct(__name__='AESGCMAlgorithm', __class__=AESGCMAlgorithm)

    def __init__(key_size):
        self.name = 'A{}GCMKW'.format(key_size)
        self.description = 'Key wrapping with AES GCM using {}-bit key'.format(key_size)
        self.key_size = key_size
        return self
    self = __init__(key_size)

    def _check_key(key):
        if len(key) * 8 != self.key_size:
            fail('A key of size %s bits is required.' % self.key_size)
    self._check_key = _check_key

    def wrap(enc_alg, headers, key, cek=None):
        if cek == None:
            cek = get_random_bytes(self.key_size // 8)

        #: https://tools.ietf.org/html/rfc7518#section-4.7.1.1
        #: The "iv" (initialization vector) Header Parameter value is the
        #: base64url-encoded representation of the 96-bit IV value
        iv_size = 96
        iv = get_random_bytes(iv_size // 8)

        cipher = AES.new(key, AES.MODE_GCM, nonce=iv)
        ek, tag = cipher.encrypt_and_digest(cek)
        h = {
            'iv': tostr(base64url_encode(iv)),
            'tag': tostr(base64url_encode(tag))
        }
        return {'ek': ek, 'cek': cek, 'header': h}
    self.wrap = wrap

    def unwrap(enc_alg, ek, headers, key):
        iv = headers.get('iv')
        if not iv:
            fail('JWEError: Missing "iv"')

        tag = headers.get('tag')
        if not tag:
            fail('JWEError: Missing "tag"')

        iv = base64url_decode(tobytes(iv))
        tag = base64url_decode(tobytes(tag))

        cipher = AES.new(key, AES.MODE_GCM, nonce=iv, mac_len=len(tag))
        cek = cipher.decrypt(ek)
        if len(cek) not in AES.key_size:
            fail('JWEError: Invalid "cek" length')
        return cek
    self.unwrap = unwrap
    return self

