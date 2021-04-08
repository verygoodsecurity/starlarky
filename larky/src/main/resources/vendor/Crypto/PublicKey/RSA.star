# -*- coding: utf-8 -*-
# ===================================================================
#
# Copyright (c) 2016, Legrandin <helderijs@gmail.com>
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
# 1. Redistributions of source code must retain the above copyright
#    notice, this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in
#    the documentation and/or other materials provided with the
#    distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
# ===================================================================

load("@stdlib//binascii", binascii="binascii")
load("@stdlib//larky", larky="larky")
load("@stdlib//struct", struct="struct")
load("@stdlib//sets", "sets")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")
load("@vendor//Crypto/Util/asn1", DerSequence="DerSequence")
load("@vendor//Crypto/IO/PEM", PEM="PEM")
load("@vendor//Crypto/Math/Numbers", Integer="Integer")
load("@vendor//Crypto/Util/number", long_to_bytes="long_to_bytes", bytes_to_long="bytes_to_long")
load("@vendor//Crypto/Math/Primality", test_probable_prime="test_probable_prime", generate_probable_prime="generate_probable_prime", COMPOSITE="COMPOSITE")
load("@vendor//Crypto/PublicKey",
     _expand_subject_public_key_info="expand_subject_public_key_info",
     _create_subject_public_key_info="create_subject_public_key_info",
     _extract_subject_public_key_info="extract_subject_public_key_info")


_WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION


__all__ = ['generate', 'construct', 'import_key',
           'RsaKey', 'oid']


def _RsaKey(**kwargs):
    r"""Class defining an actual RSA key.
    Do not instantiate directly.
    Use :func:`generate`, :func:`construct` or :func:`import_key` instead.

    :ivar n: RSA modulus
    :vartype n: integer

    :ivar e: RSA public exponent
    :vartype e: integer

    :ivar d: RSA private exponent
    :vartype d: integer

    :ivar p: First factor of the RSA modulus
    :vartype p: integer

    :ivar q: Second factor of the RSA modulus
    :vartype q: integer

    :ivar u: Chinese remainder component (:math:`p^{-1} \text{mod } q`)
    :vartype q: integer

    :undocumented: exportKey, publickey
    """

    def __init__(**kwargs):
        """Build an RSA key.

        :Keywords:
          n : integer
            The modulus.
          e : integer
            The public exponent.
          d : integer
            The private exponent. Only required for private keys.
          p : integer
            The first factor of the modulus. Only required for private keys.
          q : integer
            The second factor of the modulus. Only required for private keys.
          u : integer
            The CRT coefficient (inverse of p modulo q). Only required for
            private keys.
        """

        input_set = sets.make(kwargs.keys())
        public_set = sets.make(['n', 'e'])
        private_set = sets.union(public_set, sets.make(['p', 'q', 'd', 'u']))
        if not sets.is_subset(input_set, private_set) and not sets.is_subset(input_set, public_set):
            fail('ValueError: Some RSA components are missing')
        __dict__ = {}
        for component, value in kwargs.items():
            __dict__["_" + component] = Integer(value)
        if sets.is_equal(input_set, private_set):
            __dict__['_dp'] = __dict__['_d'].__int__() % (__dict__['_p'].__int__() - 1)  # = (e⁻¹) mod (p-1)
            __dict__['_dq'] = __dict__['_d'].__int__() % (__dict__['_q'].__int__() - 1)  # = (e⁻¹) mod (q-1)

        return larky.mutablestruct(**__dict__)

    self = __init__(**kwargs)

    def n():
        return self._n.__int__()
    self.n = larky.property(n)

    def e():
        return self._e.__int__()
    self.e = larky.property(e)

    def d():
        if not self.has_private():
            fail('AttributeError("No private exponent available for public keys")')
        return self._d.__int__()
    self.d = larky.property(d)

    def p():
        if not self.has_private():
            fail('AttributeError: No CRT component "p" available for public keys')
        return self._p.__int__()
    self.p = larky.property(p)

    def q():
        if not self.has_private():
            fail('AttributeError: No CRT component "q" available for public keys')
        return self._q.__int__()
    self.q = larky.property(q)

    def u():
        if not self.has_private():
            fail('AttributeError: No CRT component "u" available for public keys')
        return self._u.__int__()
    self.u = larky.property(u)

    def size_in_bits():
        """Size of the RSA modulus in bits"""
        return self._n.size_in_bits()
    self.size_in_bits = size_in_bits

    def size_in_bytes():
        """The minimal amount of bytes that can hold the RSA modulus"""
        return (self._n.size_in_bits() - 1) // 8 + 1
    self.size_in_bytes = size_in_bytes

    def has_private():
        """Whether this is an RSA private key"""
        return hasattr(self, "_d")
    self.has_private = has_private

    def can_encrypt():  # legacy
        return True
    self.can_encrypt = can_encrypt

    def can_sign():     # legacy
        return True
    self.can_sign = can_sign

    def public_key():
        """A matching RSA public key.

        Returns:
            a new :class:`RsaKey` object
        """
        return _RsaKey(n=self._n, e=self._e)
    self.public_key = public_key

    def __eq__(other):
        if self.has_private() != other.has_private():
            return False
        if self.n != other.n or self.e != other.e:
            return False
        if not self.has_private():
            return True
        return self.d == other.d
    self.__eq__ = __eq__

    def __ne__(other):
        return not (self.__eq__(other))
    self.__ne__ = __ne__

    def __repr__():
        if self.has_private():
            extra = ", d=%d, p=%d, q=%d, u=%d" % (int(self._d), int(self._p),
                                                  int(self._q), int(self._u))
        else:
            extra = ""
        return "RsaKey(n=%d, e=%d%s)" % (int(self._n), int(self._e), extra)
    self.__repr__ = __repr__

    def __str__():
        if has_private():
            key_type = "Private"
        else:
            key_type = "Public"
        return "%s RSA key" % key_type
    self.__str__ = __str__

    def _encrypt(plaintext):
        if not (0 <= plaintext) and (plaintext < self.n):
            fail('ValueError: Plaintext too large')
        b = _JCrypto.PublicKey.RSA.encrypt(dict(n=self.n, e=self.e), long_to_bytes(plaintext))
        return bytes_to_long(b)
        #return int(pow(Integer(plaintext), self._e, self._n))
    self._encrypt = _encrypt

    def _decrypt(ciphertext):
        if not (0 <= ciphertext) and (ciphertext < self.n):
            fail('ValueError: Ciphertext too large')
        if not self.has_private():
            fail('TypeError: This is not a private key')

        b = _JCrypto.PublicKey.RSA.decrypt(dict(n=self.n, d=self.d), long_to_bytes(ciphertext))
        result = bytes_to_long(b)
        # Verify no faults occurred
        if ciphertext != pow(result, self.e, self.n):
            fail('ValueError: Fault detected in RSA decryption')
        return result

    self._decrypt = _decrypt

    def export_key(format='PEM', passphrase=None, pkcs=1,
                   protection=None, randfunc=None):
        """Export this RSA key.

        Args:
          format (string):
            The format to use for wrapping the key:

            - *'PEM'*. (*Default*) Text encoding, done according to `RFC1421`_/`RFC1423`_.
            - *'DER'*. Binary encoding.
            - *'OpenSSH'*. Textual encoding, done according to OpenSSH specification.
              Only suitable for public keys (not private keys).

          passphrase (string):
            (*For private keys only*) The pass phrase used for protecting the output.

          pkcs (integer):
            (*For private keys only*) The ASN.1 structure to use for
            serializing the key. Note that even in case of PEM
            encoding, there is an inner ASN.1 DER structure.

            With ``pkcs=1`` (*default*), the private key is encoded in a
            simple `PKCS#1`_ structure (``RSAPrivateKey``).

            With ``pkcs=8``, the private key is encoded in a `PKCS#8`_ structure
            (``PrivateKeyInfo``).

            .. note::
                This parameter is ignored for a public key.
                For DER and PEM, an ASN.1 DER ``SubjectPublicKeyInfo``
                structure is always used.

          protection (string):
            (*For private keys only*)
            The encryption scheme to use for protecting the private key.

            If ``None`` (default), the behavior depends on :attr:`format`:

            - For *'DER'*, the *PBKDF2WithHMAC-SHA1AndDES-EDE3-CBC*
              scheme is used. The following operations are performed:

                1. A 16 byte Triple DES key is derived from the passphrase
                   using :func:`Crypto.Protocol.KDF.PBKDF2` with 8 bytes salt,
                   and 1 000 iterations of :mod:`Crypto.Hash.HMAC`.
                2. The private key is encrypted using CBC.
                3. The encrypted key is encoded according to PKCS#8.

            - For *'PEM'*, the obsolete PEM encryption scheme is used.
              It is based on MD5 for key derivation, and Triple DES for encryption.

            Specifying a value for :attr:`protection` is only meaningful for PKCS#8
            (that is, ``pkcs=8``) and only if a pass phrase is present too.

            The supported schemes for PKCS#8 are listed in the
            :mod:`Crypto.IO.PKCS8` module (see :attr:`wrap_algo` parameter).

          randfunc (callable):
            A function that provides random bytes. Only used for PEM encoding.
            The default is :func:`Crypto.Random.get_random_bytes`.

        Returns:
          byte string: the encoded key

        Raises:
          ValueError:when the format is unknown or when you try to encrypt a private
            key with *DER* format and PKCS#1.

        .. warning::
            If you don't provide a pass phrase, the private key will be
            exported in the clear!

        .. _RFC1421:    http://www.ietf.org/rfc/rfc1421.txt
        .. _RFC1423:    http://www.ietf.org/rfc/rfc1423.txt
        .. _`PKCS#1`:   http://www.ietf.org/rfc/rfc3447.txt
        .. _`PKCS#8`:   http://www.ietf.org/rfc/rfc5208.txt
        """

        if passphrase != None:
            passphrase = tobytes(passphrase)

        if randfunc == None:
            randfunc = Random.get_random_bytes

        if format == 'OpenSSH':
            e_bytes, n_bytes = [x.to_bytes() for x in (self._e, self._n)]
            if bord(e_bytes[0]) & 0x80:
                e_bytes = bytearray([0x00]) + e_bytes
            if bord(n_bytes[0]) & 0x80:
                n_bytes = bytearray([0x00]) + n_bytes
            keyparts = [bytearray([0x73, 0x73, 0x68, 0x2d, 0x72, 0x73, 0x61]), e_bytes, n_bytes]
            keystring = bytearray(r'', encoding='utf-8').join(
                [struct.pack(">I", len(kp)) + kp for kp in keyparts]
            )
            return (
                bytearray([0x73, 0x73, 0x68, 0x2d, 0x72, 0x73, 0x61, 0x20])
                + binascii.b2a_base64(keystring)[:-1]
            )

        # DER format is always used, even in case of PEM, which simply
        # encodes it into BASE64.
        if has_private():
            binary_key = DerSequence([0,
                                      self.n,
                                      self.e,
                                      self.d,
                                      self.p,
                                      self.q,
                                      self.d % (self.p-1),
                                      self.d % (self.q-1),
                                      Integer(self.q).inverse(self.p).__int__(),
                                      ]).encode()
            if pkcs == 1:
                key_type = 'RSA PRIVATE KEY'
                if format == 'DER' and passphrase:
                    fail('ValueError: PKCS#1 private key cannot be encrypted')
            else:  # PKCS#8
                if format == 'PEM' and protection == None:
                    key_type = 'PRIVATE KEY'
                    binary_key = _JCrypto.PublicKey.PKCS8_wrap(binary_key, _oid, None)
                else:
                    key_type = 'ENCRYPTED PRIVATE KEY'
                    if not protection:
                        protection = 'PBKDF2WithHMACSHA1AndDES-EDE3-CBC'
                    binary_key = _JCrypto.PublicKey.PKCS8_wrap(binary_key, _oid,
                                            passphrase, protection)
                    passphrase = None
        else:
            key_type = "PUBLIC KEY"
            #print("IN EXPORT:", _oid, self.n, self.e)
            binary_key = _create_subject_public_key_info(
                _oid, DerSequence([self.n, self.e])
            )


        if format == 'DER':
            return binary_key
        if format == 'PEM':
            pem_str = _JCrypto.PublicKey.PEM_encode(binary_key, key_type, passphrase, randfunc)
            return tobytes(pem_str.strip())

        fail('ValueError: Unknown key format "%s". ' +
             'Cannot export the RSA key.' % format)

    self.export_key = export_key
    # Backward compatibility
    # self.exportKey = export_key
    self.publickey = public_key

    # Methods defined in PyCrypto that we don't support anymore
    def sign(M, K):
        fail('NotImplementedError("Use module Crypto.Signature.pkcs1_15 instead")')
    self.sign = sign

    def verify(M, signature):
        fail('NotImplementedError("Use module Crypto.Signature.pkcs1_15 instead")')
    self.verify = verify

    def encrypt(plaintext, K):
        fail('NotImplementedError("Use module Crypto.Cipher.PKCS1_OAEP instead")')
    self.encrypt = encrypt

    def decrypt(ciphertext):
        fail('NotImplementedError("Use module Crypto.Cipher.PKCS1_OAEP instead")')
    self.decrypt = decrypt

    def blind(M, B):
        fail(" NotImplementedError")
    self.blind = blind

    def unblind(M, B):
        fail(" NotImplementedError")
    self.unblind = unblind

    def size():
        fail(" NotImplementedError")
    self.size = size

    return self


def _generate(bits, randfunc=None, e=65537):
    """Create a new RSA key pair.

    The algorithm closely follows NIST `FIPS 186-4`_ in its
    sections B.3.1 and B.3.3. The modulus is the product of
    two non-strong probable primes.
    Each prime passes a suitable number of Miller-Rabin tests
    with random bases and a single Lucas test.

    Args:
      bits (integer):
        Key length, or size (in bits) of the RSA modulus.
        It must be at least 1024, but **2048 is recommended.**
        The FIPS standard only defines 1024, 2048 and 3072.
      randfunc (callable):
        Function that returns random bytes.
        The default is :func:`Crypto.Random.get_random_bytes`.
      e (integer):
        Public RSA exponent. It must be an odd positive integer.
        It is typically a small number with very few ones in its
        binary representation.
        The FIPS standard requires the public exponent to be
        at least 65537 (the default).

    Returns: an RSA key object (:class:`RsaKey`, with private key).

    .. _FIPS 186-4: http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf
    """

    if bits < 1024:
        fail('ValueError: RSA modulus length must be >= 1024')
    if e % 2 == 0 or e < 3:
        fail('ValueError: RSA public exponent must be a positive, ' +
             'odd integer larger than 2.')

    if randfunc == None:
        # TODO(Larky::Difference): We don't care about passing in a different
        # Random.
        randfunc = Random.get_random_bytes

    rsaObj = _JCrypto.PublicKey.RSA.generate(bits, e)
    return _RsaKey(**rsaObj)


def _construct(rsa_components, consistency_check=True):
    r"""Construct an RSA key from a tuple of valid RSA components.

    The modulus **n** must be the product of two primes.
    The public exponent **e** must be odd and larger than 1.

    In case of a private key, the following equations must apply:

    .. math::

        \begin{align}
        p*q &= n \\
        e*d &\equiv 1 ( \text{mod lcm} [(p-1)(q-1)]) \\
        p*u &\equiv 1 ( \text{mod } q)
        \end{align}

    Args:
        rsa_components (tuple):
            A tuple of integers, with at least 2 and no
            more than 6 items. The items come in the following order:

            1. RSA modulus *n*.
            2. Public exponent *e*.
            3. Private exponent *d*.
               Only required if the key is private.
            4. First factor of *n* (*p*).
               Optional, but the other factor *q* must also be present.
            5. Second factor of *n* (*q*). Optional.
            6. CRT coefficient *q*, that is :math:`p^{-1} \text{mod }q`. Optional.

        consistency_check (boolean):
            If ``True``, the library will verify that the provided components
            fulfil the main RSA properties.

    Raises:
        ValueError: when the key being imported fails the most basic RSA validity checks.

    Returns: An RSA key object (:class:`RsaKey`).
    """
    input_comps = {}
    for (comp, value) in zip(('n', 'e', 'd', 'p', 'q', 'u'), rsa_components):
        input_comps[comp] = Integer(value)

    input_comps = larky.mutablestruct(**input_comps)

    n = input_comps.n
    e = input_comps.e
    if not hasattr(input_comps, 'd'):
        key = _RsaKey(n=n, e=e)
    else:
        d = input_comps.d
        if hasattr(input_comps, 'q'):
            p = input_comps.p
            q = input_comps.q
        else:
            p, q = _JCrypto.PublicKey.RSA.compute_factors(n._value, e._value, d._value)
            p = Integer(p)
            input_comps.p = p
            q = Integer(q)
            input_comps.q = q

        if hasattr(input_comps, 'u'):
            u = input_comps.u
        else:
            u = p.inverse(q)

        # Build key object
        key = _RsaKey(n=n, e=e, d=d, p=p, q=q, u=u)

    # Verify consistency of the key
    if consistency_check:

        # Modulus and public exponent must be coprime
        if e._value <= 1 or e._value >= n._value:
            fail('ValueError: Invalid RSA public exponent')
        if _JCrypto.Math.gcd(n._value, e._value) != 1:
            fail('ValueError: RSA public exponent is not coprime to modulus')

        # For RSA, modulus must be odd
        if not n._value & 1:
            fail('ValueError: RSA modulus is not odd')

        if key.has_private():
            # Modulus and private exponent must be coprime
            if d._value <= 1 or d._value >= n._value:
                fail('ValueError: Invalid RSA private exponent')
            if _JCrypto.Math.gcd(n._value, d._value) != 1:
                fail('ValueError: RSA private exponent is not coprime to modulus')
            # Modulus must be product of 2 primes
            if p._value * q._value != n._value:
                fail('ValueError: RSA factors do not match modulus')
            if test_probable_prime(p._value) == False:
                fail('ValueError: RSA factor p is composite')
            if test_probable_prime(q._value) == False:
                fail('ValueError: RSA factor q is composite')
            # See Carmichael theorem
            phi = (p._value - 1) * (q._value - 1)
            lcm = phi // Integer(p._value - 1).gcd(q._value - 1).__int__()
            if (e._value * d._value % lcm) != 1:
                fail('ValueError: Invalid RSA condition')
            if hasattr(key, 'u'):
                # CRT coefficient
                if u._value <= 1 or u._value >= q._value:
                    fail('ValueError: Invalid RSA component u')
                if (p._value * u._value % q._value) != 1:
                    fail('ValueError: Invalid RSA component u with p')

    return key


def _import_pkcs1_private(encoded, *kwargs):
    # RSAPrivateKey ::= SEQUENCE {
    #           version Version,
    #           modulus INTEGER, -- n
    #           publicExponent INTEGER, -- e
    #           privateExponent INTEGER, -- d
    #           prime1 INTEGER, -- p
    #           prime2 INTEGER, -- q
    #           exponent1 INTEGER, -- d mod (p-1)
    #           exponent2 INTEGER, -- d mod (q-1)
    #           coefficient INTEGER -- (inverse of q) mod p
    # }
    #
    # Version ::= INTEGER
    der = DerSequence().decode(encoded, nr_elements=9, only_ints_expected=True, errors=False)
    # TODO(Hack)...until I introduce safetywrap
    if not der:
        return "failed", None
    if der.__getitem__(0) != 0:
        return 'ValueError: No PKCS#1 encoding of an RSA private key', None
    return None, _construct(der.__getslice__(1,6) + [
            Integer(der.__getitem__(4)).inverse(der.__getitem__(5))
        ])


def _import_pkcs1_public(encoded, *kwargs):
    # RSAPublicKey ::= SEQUENCE {
    #           modulus INTEGER, -- n
    #           publicExponent INTEGER -- e
    # }
    der = DerSequence().decode(encoded, nr_elements=2, only_ints_expected=True, errors=False)
    # TODO(Hack)...until I introduce safetywrap
    if not der:
        return "failed", None
    return None, _construct(der._seq)


def _import_subjectPublicKeyInfo(encoded, *kwargs):
    result = _JCrypto.PublicKey.import_keyDER(encoded, kwargs[0])
    return None, _construct(result)
    print(binascii.hexlify(encoded))
    algoid, encoded_key, params = _expand_subject_public_key_info(encoded)
    if algoid != _oid or params != None:
        return 'ValueError: No RSA subjectPublicKeyInfo', None
    return _import_pkcs1_public(encoded_key)


def _import_x509_cert(encoded, *kwargs):
    result = _JCrypto.PublicKey.import_keyDER(encoded, kwargs[0])
    return None, _construct(result)
    sp_info = _extract_subject_public_key_info(encoded)
    return _import_subjectPublicKeyInfo(sp_info)


def _import_pkcs8(encoded, passphrase):
    k = _JCrypto.PublicKey.PKCS8_unwrap(encoded, passphrase)
    if k[0] != _oid:
        return 'ValueError("No PKCS#8 encoded RSA key")', None
    return _import_keyDER(k[1], passphrase)


def _import_keyDER(extern_key, passphrase):
    """Import an RSA key (public or private half), encoded in DER form."""

    decodings = (_import_pkcs1_private,
                 _import_pkcs1_public,
                 _import_subjectPublicKeyInfo,
                 _import_x509_cert,
                 _import_pkcs8)
    # print("key: " , binascii.hexlify(extern_key))
    for decoding in decodings:
        error, result = decoding(extern_key, passphrase)
        if error != None:
            print(error)
        if result:
            return None, result
    #_JCrypto.PublicKey.import_DER_key(extern_key, passphrase)

    fail('ValueError("RSA key format is not supported")')


def _import_openssh_private_rsa(data, password):
    return _JCrypto.PublicKey.OpenSSH_import(data, password)
    # load("@vendor//_openssh", import_openssh_private_generic="import_openssh_private_generic", read_bytes="read_bytes", read_string="read_string", check_padding="check_padding")

    # ssh_name, decrypted = import_openssh_private_generic(data, password)
    #ssh_name = tostr(data)
#    decrypted = (password != None)
 #   if ssh_name != "ssh-rsa":
#        fail(" ValueError(\"This SSH key is not RSA\")")

    # n, decrypted = read_bytes(decrypted)
    # e, decrypted = read_bytes(decrypted)
    # d, decrypted = read_bytes(decrypted)
    # iqmp, decrypted = read_bytes(decrypted)
    # p, decrypted = read_bytes(decrypted)
    # q, decrypted = read_bytes(decrypted)
    #
    # _, padded = read_string(decrypted)  # Comment
    # check_padding(padded)

    #build = [Integer.from_bytes(x) for x in (n, e, d, q, p, iqmp)]
    #return(build)


def _import_key(extern_key, passphrase=None):
    """Import an RSA key (public or private).

    Args:
      extern_key (string or byte string):
        The RSA key to import.

        The following formats are supported for an RSA **public key**:

        - X.509 certificate (binary or PEM format)
        - X.509 ``subjectPublicKeyInfo`` DER SEQUENCE (binary or PEM
          encoding)
        - `PKCS#1`_ ``RSAPublicKey`` DER SEQUENCE (binary or PEM encoding)
        - An OpenSSH line (e.g. the content of ``~/.ssh/id_ecdsa``, ASCII)

        The following formats are supported for an RSA **private key**:

        - PKCS#1 ``RSAPrivateKey`` DER SEQUENCE (binary or PEM encoding)
        - `PKCS#8`_ ``PrivateKeyInfo`` or ``EncryptedPrivateKeyInfo``
          DER SEQUENCE (binary or PEM encoding)
        - OpenSSH (text format, introduced in `OpenSSH 6.5`_)

        For details about the PEM encoding, see `RFC1421`_/`RFC1423`_.

      passphrase (string or byte string):
        For private keys only, the pass phrase that encrypts the key.

    Returns: An RSA key object (:class:`RsaKey`).

    Raises:
      ValueError/IndexError/TypeError:
        When the given key cannot be parsed (possibly because the pass
        phrase is wrong).

    .. _RFC1421: http://www.ietf.org/rfc/rfc1421.txt
    .. _RFC1423: http://www.ietf.org/rfc/rfc1423.txt
    .. _`PKCS#1`: http://www.ietf.org/rfc/rfc3447.txt
    .. _`PKCS#8`: http://www.ietf.org/rfc/rfc5208.txt
    .. _`OpenSSH 6.5`: https://flak.tedunangst.com/post/new-openssh-key-format-and-bcrypt-pbkdf
    """
    extern_key = tobytes(extern_key)
    if passphrase != None:
        passphrase = tobytes(passphrase)

    if extern_key.startswith(bytes([0x2d, 0x2d, 0x2d, 0x2d, 0x2d, 0x42, 0x45, 0x47, 0x49, 0x4e, 0x20, 0x4f, 0x50, 0x45, 0x4e, 0x53, 0x53, 0x48, 0x20, 0x50, 0x52, 0x49, 0x56, 0x41, 0x54, 0x45, 0x20, 0x4b, 0x45, 0x59])):
        text_encoded = tostr(extern_key)
        # openssh_encoded, marker, enc_flag = PEM.decode(text_encoded, passphrase)
        openssh_encoded = _JCrypto.PublicKey.PEM_decode(text_encoded, passphrase)
        result = _import_openssh_private_rsa(openssh_encoded, passphrase)
        return result

    if extern_key.startswith(bytes([0x2d, 0x2d, 0x2d, 0x2d, 0x2d])):
        # This is probably a PEM encoded key.
        # result = _JCrypto.PublicKey.PEM_decode(tostr(extern_key), passphrase)
        # return _construct(result)
        # [10009650922319323069803079573274165970579185090127568126860948226706532161412468049945146845321486910355660772093076859316010597108858810676816273210356613, 65537, 485384906711183128855977339271942558470797415451201786886248889397948627629708179660048199702198930888752799597253915886594933821519461147610835828000825, 109486538119839518563492128520614562694068174030916365997574385489384951663139, 91423576763046116639172268615227635722663558665590792917584550933135061811767, 19805061107571007563044705551475187361598754225024722988988921457394745450021]

        (der, marker, enc_flag) = PEM.decode(tostr(extern_key), passphrase)
        # print("xxxx: ", der, marker, enc_flag)
        #if enc_flag or "PRIVATE" in marker:
        if enc_flag:
            result = _JCrypto.PublicKey.PEM_decode(tostr(extern_key), passphrase)
            #result = _JCrypto.PublicKey.import_keyDER(der, passphrase)
            return _construct(result)
            # err, result = _import_keyDER(extern_key, passphrase)
            # return result
        else:
            result = _JCrypto.PublicKey.import_keyDER(der, passphrase)
            return _construct(result)

    if extern_key.startswith(bytes([0x73, 0x73, 0x68, 0x2d, 0x72, 0x73, 0x61, 0x20])):
        # This is probably an OpenSSH key
        keystring = binascii.a2b_base64(extern_key.split(bytes([0x20]))[1])
        keyparts = []
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if len(keystring) <= 4:
                break
            length = struct.unpack(">I", keystring[:4])[0]
            keyparts.append(keystring[4:4 + length])
            keystring = keystring[4 + length:]
        e = Integer(0).from_bytes(keyparts[1])
        n = Integer(0).from_bytes(keyparts[2])
        return _construct([n, e])

    if len(extern_key) > 0 and bord(extern_key[0]) == 0x30:
        # This is probably a DER encoded key
        err, res = _import_keyDER(extern_key, passphrase)
        return res

    fail('ValueError: RSA key format is not supported')


# Backward compatibility
importKey = _import_key

#: `Object ID`_ for the RSA encryption algorithm. This OID often indicates
#: a generic RSA key, even when such key will be actually used for digital
#: signatures.
#:
#: .. _`Object ID`: http://www.alvestrand.no/objectid/1.2.840.113549.1.1.1.html
_oid = "1.2.840.113549.1.1.1"


RSA = larky.struct(
    RsaKey=_RsaKey,
    oid=_oid,
    generate=_generate,
    construct=_construct,
    importKey=_import_key,
    import_key=_import_key,
)
