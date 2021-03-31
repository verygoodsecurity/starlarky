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
load("@stdlib//binascii", "a2b_base64")
load("@stdlib//struct", struct="struct")
load("@stdlib//larky", larky="larky")

load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")
load("@vendor//Crypto/Util/asn1", DerSequence="DerSequence")

load("@vendor//Crypto/Math/Numbers", Integer="Integer")
load("@vendor//Crypto/Math/Primality", test_probable_prime="test_probable_prime", generate_probable_prime="generate_probable_prime", COMPOSITE="COMPOSITE")

#load("@vendor//Crypto/PublicKey", _expand_subject_public_key_info="expand_subject_public_key_info", _create_subject_public_key_info="create_subject_public_key_info", _extract_subject_public_key_info="extract_subject_public_key_info")
load("@stdlib//builtins", "builtins")


RSA = larky.struct(
    generate=int,
    construct=int,
    import_key=int,
    RsaKey=int,
    oid=int
)

#
# __all__ = ['generate', 'construct', 'import_key',
#            'RsaKey', 'oid']
#
# def RsaKey():
#     r"""Class defining an actual RSA key.
#     Do not instantiate directly.
#     Use :func:`generate`, :func:`construct` or :func:`import_key` instead.
#
#     :ivar n: RSA modulus
#     :vartype n: integer
#
#     :ivar e: RSA public exponent
#     :vartype e: integer
#
#     :ivar d: RSA private exponent
#     :vartype d: integer
#
#     :ivar p: First factor of the RSA modulus
#     :vartype p: integer
#
#     :ivar q: Second factor of the RSA modulus
#     :vartype q: integer
#
#     :ivar u: Chinese remainder component (:math:`p^{-1} \text{mod } q`)
#     :vartype q: integer
#
#     :undocumented: exportKey, publickey
#     """
#
#     def __init__(**kwargs):
#         """Build an RSA key.
#
#         :Keywords:
#           n : integer
#             The modulus.
#           e : integer
#             The public exponent.
#           d : integer
#             The private exponent. Only required for private keys.
#           p : integer
#             The first factor of the modulus. Only required for private keys.
#           q : integer
#             The second factor of the modulus. Only required for private keys.
#           u : integer
#             The CRT coefficient (inverse of p modulo q). Only required for
#             private keys.
#         """
#
#         input_set = set(kwargs.keys())
#         public_set = set(('n', 'e'))
#         private_set = public_set | set(('p', 'q', 'd', 'u'))
#         if input_set not in (private_set, public_set):
#             fail(" ValueError(\"Some RSA components are missing\")")
#         for component, value in kwargs.items():
#             setattr(self, "_" + component, value)
#         if input_set == private_set:
#             _dp = _d % (_p - 1)  # = (e⁻¹) mod (p-1)
#             _dq = _d % (_q - 1)  # = (e⁻¹) mod (q-1)
#
#     @property
#     def n():
#         return int(_n)
#
#     @property
#     def e():
#         return int(_e)
#
#     @property
#     def d():
#         if not has_private():
#             fail(" AttributeError(\"No private exponent available for public keys\")")
#         return int(_d)
#
#     @property
#     def p():
#         if not has_private():
#             fail(" AttributeError(\"No CRT component 'p' available for public keys\")")
#         return int(_p)
#
#     @property
#     def q():
#         if not has_private():
#             fail(" AttributeError(\"No CRT component 'q' available for public keys\")")
#         return int(_q)
#
#     @property
#     def u():
#         if not has_private():
#             fail(" AttributeError(\"No CRT component 'u' available for public keys\")")
#         return int(_u)
#
#     def size_in_bits():
#         """Size of the RSA modulus in bits"""
#         return _n.size_in_bits()
#
#     def size_in_bytes():
#         """The minimal amount of bytes that can hold the RSA modulus"""
#         return (_n.size_in_bits() - 1) // 8 + 1
#
#     def _encrypt(plaintext):
#         if not (0 <= plaintext) and (plaintext < _n):
#             fail(" ValueError(\"Plaintext too large\")")
#         return int(pow(Integer(plaintext), _e, _n))
#
#     def _decrypt(ciphertext):
#         if not (0 <= ciphertext) and (ciphertext < _n):
#             fail(" ValueError(\"Ciphertext too large\")")
#         if not has_private():
#             fail(" TypeError(\"This is not a private key\")")
#
#         # Blinded RSA decryption (to prevent timing attacks):
#         # Step 1: Generate random secret blinding factor r,
#         # such that 0 < r < n-1
#         r = Integer.random_range(min_inclusive=1, max_exclusive=_n)
#         # Step 2: Compute c' = c * r**e mod n
#         cp = Integer(ciphertext) * pow(r, _e, _n) % _n
#         # Step 3: Compute m' = c'**d mod n       (normal RSA decryption)
#         m1 = pow(cp, _dp, _p)
#         m2 = pow(cp, _dq, _q)
#         h = ((m2 - m1) * _u) % _q
#         mp = h * _p + m1
#         # Step 4: Compute m = m**(r-1) mod n
#         result = (r.inverse(_n) * mp) % _n
#         # Verify no faults occurred
#         if ciphertext != pow(result, _e, _n):
#             fail(" ValueError(\"Fault detected in RSA decryption\")")
#         return result
#
#     def has_private():
#         """Whether this is an RSA private key"""
#
#         return hasattr(self, "_d")
#
#     def can_encrypt():  # legacy
#         return True
#
#     def can_sign():     # legacy
#         return True
#
#     def public_key():
#         """A matching RSA public key.
#
#         Returns:
#             a new :class:`RsaKey` object
#         """
#         return RsaKey(n=_n, e=_e)
#
#     def __eq__(other):
#         if has_private() != other.has_private():
#             return False
#         if n != other.n or e != other.e:
#             return False
#         if not has_private():
#             return True
#         return (d == other.d)
#
#     def __ne__(other):
#         return not (self == other)
#
#     def __getstate__():
#         # RSA key is not pickable
#         load("@stdlib//pickle", PicklingError="PicklingError")
#         fail(" PicklingError")
#
#     def __repr__():
#         if has_private():
#             extra = ", d=%d, p=%d, q=%d, u=%d" % (int(_d), int(_p),
#                                                   int(_q), int(_u))
#         else:
#             extra = ""
#         return "RsaKey(n=%d, e=%d%s)" % (int(_n), int(_e), extra)
#
#     def __str__():
#         if has_private():
#             key_type = "Private"
#         else:
#             key_type = "Public"
#         return "%s RSA key at 0x%X" % (key_type, id(self))
#
#     def export_key(format='PEM', passphrase=None, pkcs=1,
#                    protection=None, randfunc=None):
#         """Export this RSA key.
#
#         Args:
#           format (string):
#             The format to use for wrapping the key:
#
#             - *'PEM'*. (*Default*) Text encoding, done according to `RFC1421`_/`RFC1423`_.
#             - *'DER'*. Binary encoding.
#             - *'OpenSSH'*. Textual encoding, done according to OpenSSH specification.
#               Only suitable for public keys (not private keys).
#
#           passphrase (string):
#             (*For private keys only*) The pass phrase used for protecting the output.
#
#           pkcs (integer):
#             (*For private keys only*) The ASN.1 structure to use for
#             serializing the key. Note that even in case of PEM
#             encoding, there is an inner ASN.1 DER structure.
#
#             With ``pkcs=1`` (*default*), the private key is encoded in a
#             simple `PKCS#1`_ structure (``RSAPrivateKey``).
#
#             With ``pkcs=8``, the private key is encoded in a `PKCS#8`_ structure
#             (``PrivateKeyInfo``).
#
#             .. note::
#                 This parameter is ignored for a public key.
#                 For DER and PEM, an ASN.1 DER ``SubjectPublicKeyInfo``
#                 structure is always used.
#
#           protection (string):
#             (*For private keys only*)
#             The encryption scheme to use for protecting the private key.
#
#             If ``None`` (default), the behavior depends on :attr:`format`:
#
#             - For *'DER'*, the *PBKDF2WithHMAC-SHA1AndDES-EDE3-CBC*
#               scheme is used. The following operations are performed:
#
#                 1. A 16 byte Triple DES key is derived from the passphrase
#                    using :func:`Crypto.Protocol.KDF.PBKDF2` with 8 bytes salt,
#                    and 1 000 iterations of :mod:`Crypto.Hash.HMAC`.
#                 2. The private key is encrypted using CBC.
#                 3. The encrypted key is encoded according to PKCS#8.
#
#             - For *'PEM'*, the obsolete PEM encryption scheme is used.
#               It is based on MD5 for key derivation, and Triple DES for encryption.
#
#             Specifying a value for :attr:`protection` is only meaningful for PKCS#8
#             (that is, ``pkcs=8``) and only if a pass phrase is present too.
#
#             The supported schemes for PKCS#8 are listed in the
#             :mod:`Crypto.IO.PKCS8` module (see :attr:`wrap_algo` parameter).
#
#           randfunc (callable):
#             A function that provides random bytes. Only used for PEM encoding.
#             The default is :func:`Crypto.Random.get_random_bytes`.
#
#         Returns:
#           byte string: the encoded key
#
#         Raises:
#           ValueError:when the format is unknown or when you try to encrypt a private
#             key with *DER* format and PKCS#1.
#
#         .. warning::
#             If you don't provide a pass phrase, the private key will be
#             exported in the clear!
#
#         .. _RFC1421:    http://www.ietf.org/rfc/rfc1421.txt
#         .. _RFC1423:    http://www.ietf.org/rfc/rfc1423.txt
#         .. _`PKCS#1`:   http://www.ietf.org/rfc/rfc3447.txt
#         .. _`PKCS#8`:   http://www.ietf.org/rfc/rfc5208.txt
#         """
#
#         if passphrase != None:
#             passphrase = tobytes(passphrase)
#
#         if randfunc == None:
#             randfunc = Random.get_random_bytes
#
#         if format == 'OpenSSH':
#             e_bytes, n_bytes = [x.to_bytes() for x in (_e, _n)]
#             if bord(e_bytes[0]) & 0x80:
#                 e_bytes = builtins.bytes(r'\x00', encoding='utf-8') + e_bytes
#             if bord(n_bytes[0]) & 0x80:
#                 n_bytes = builtins.bytes(r'\x00', encoding='utf-8') + n_bytes
#             keyparts = [builtins.bytes(r'ssh-rsa', encoding='utf-8'), e_bytes, n_bytes]
#             keystring = builtins.bytes(r'', encoding='utf-8').join([struct.pack(">I", len(kp)) + kp for kp in keyparts])
#             return builtins.bytes(r'ssh-rsa ', encoding='utf-8') + binascii.b2a_base64(keystring)[:-1]
#
#         # DER format is always used, even in case of PEM, which simply
#         # encodes it into BASE64.
#         if has_private():
#             binary_key = DerSequence([0,
#                                       n,
#                                       e,
#                                       d,
#                                       p,
#                                       q,
#                                       d % (p-1),
#                                       d % (q-1),
#                                       Integer(q).inverse(p)
#                                       ]).encode()
#             if pkcs == 1:
#                 key_type = 'RSA PRIVATE KEY'
#                 if format == 'DER' and passphrase:
#                     fail(" ValueError(\"PKCS#1 private key cannot be encrypted\")")
#             else:  # PKCS#8
#                 load("@vendor//Crypto/IO", PKCS8="PKCS8")
#
#                 if format == 'PEM' and protection == None:
#                     key_type = 'PRIVATE KEY'
#                     binary_key = PKCS8.wrap(binary_key, oid, None)
#                 else:
#                     key_type = 'ENCRYPTED PRIVATE KEY'
#                     if not protection:
#                         protection = 'PBKDF2WithHMAC-SHA1AndDES-EDE3-CBC'
#                     binary_key = PKCS8.wrap(binary_key, oid,
#                                             passphrase, protection)
#                     passphrase = None
#         else:
#             key_type = "PUBLIC KEY"
#             binary_key = _create_subject_public_key_info(oid,
#                                                          DerSequence([n,
#                                                                       e])
#                                                          )
#
#         if format == 'DER':
#             return binary_key
#         if format == 'PEM':
#             load("@vendor//Crypto/IO", PEM="PEM")
#
#             pem_str = PEM.encode(binary_key, key_type, passphrase, randfunc)
#             return tobytes(pem_str)
#
#         fail(" ValueError(\"Unknown key format '%s'. Cannot export the RSA key.\" % format)")
#
#     # Backward compatibility
#     exportKey = export_key
#     publickey = public_key
#
#     # Methods defined in PyCrypto that we don't support anymore
#     def sign(M, K):
#         fail(" NotImplementedError(\"Use module Crypto.Signature.pkcs1_15 instead\")")
#
#     def verify(M, signature):
#         fail(" NotImplementedError(\"Use module Crypto.Signature.pkcs1_15 instead\")")
#
#     def encrypt(plaintext, K):
#         fail(" NotImplementedError(\"Use module Crypto.Cipher.PKCS1_OAEP instead\")")
#
#     def decrypt(ciphertext):
#         fail(" NotImplementedError(\"Use module Crypto.Cipher.PKCS1_OAEP instead\")")
#
#     def blind(M, B):
#         fail(" NotImplementedError")
#
#     def unblind(M, B):
#         fail(" NotImplementedError")
#
#     def size():
#         fail(" NotImplementedError")
#

def generate(bits, randfunc=None, e=65537):
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
        fail('ValueError("RSA modulus length must be >= 1024")')
    if e % 2 == 0 or e < 3:
        fail('ValueError("RSA public exponent must be a positive, odd integer' +
             ' larger than 2.")')

    if randfunc == None:
        randfunc = Random.get_random_bytes

    d = n = Integer(1)
    e = Integer(e)
    for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
        if not n.size_in_bits() != bits and d < (1 << (bits // 2)):
            break
        # Generate the prime factors of n: p and q.
        # By construciton, their product is always
        # 2^{bits-1} < p*q < 2^bits.
        size_q = bits // 2
        size_p = bits - size_q

        min_p = min_q = (Integer(1) << (2 * size_q - 1)).sqrt()
        if size_q != size_p:
            min_p = (Integer(1) << (2 * size_p - 1)).sqrt()

        def filter_p(candidate):
            return candidate > min_p and (candidate - 1).gcd(e) == 1

        p = generate_probable_prime(exact_bits=size_p,
                                    randfunc=randfunc,
                                    prime_filter=filter_p)

        min_distance = Integer(1) << (bits // 2 - 100)

        def filter_q(candidate):
            return (candidate > min_q and
                    (candidate - 1).gcd(e) == 1 and
                    abs(candidate - p) > min_distance)

        q = generate_probable_prime(exact_bits=size_q,
                                    randfunc=randfunc,
                                    prime_filter=filter_q)

        n = p * q
        lcm = (p - 1).lcm(q - 1)
        d = e.inverse(lcm)

    if p > q:
        p, q = q, p

    u = p.inverse(q)

    return RsaKey(n=n, e=e, d=d, p=p, q=q, u=u)


# def construct(rsa_components, consistency_check=True):
#     r"""Construct an RSA key from a tuple of valid RSA components.
#
#     The modulus **n** must be the product of two primes.
#     The public exponent **e** must be odd and larger than 1.
#
#     In case of a private key, the following equations must apply:
#
#     .. math::
#
#         \begin{align}
#         p*q &= n \\
#         e*d &\equiv 1 ( \text{mod lcm} [(p-1)(q-1)]) \\
#         p*u &\equiv 1 ( \text{mod } q)
#         \end{align}
#
#     Args:
#         rsa_components (tuple):
#             A tuple of integers, with at least 2 and no
#             more than 6 items. The items come in the following order:
#
#             1. RSA modulus *n*.
#             2. Public exponent *e*.
#             3. Private exponent *d*.
#                Only required if the key is private.
#             4. First factor of *n* (*p*).
#                Optional, but the other factor *q* must also be present.
#             5. Second factor of *n* (*q*). Optional.
#             6. CRT coefficient *q*, that is :math:`p^{-1} \text{mod }q`. Optional.
#
#         consistency_check (boolean):
#             If ``True``, the library will verify that the provided components
#             fulfil the main RSA properties.
#
#     Raises:
#         ValueError: when the key being imported fails the most basic RSA validity checks.
#
#     Returns: An RSA key object (:class:`RsaKey`).
#     """
#     def InputComps():
#         pass
#
#     input_comps = InputComps()
#     for (comp, value) in zip(('n', 'e', 'd', 'p', 'q', 'u'), rsa_components):
#         setattr(input_comps, comp, Integer(value))
#
#     n = input_comps.n
#     e = input_comps.e
#     if not hasattr(input_comps, 'd'):
#         key = RsaKey(n=n, e=e)
#     else:
#         d = input_comps.d
#         if hasattr(input_comps, 'q'):
#             p = input_comps.p
#             q = input_comps.q
#         else:
#             # Compute factors p and q from the private exponent d.
#             # We assume that n has no more than two factors.
#             # See 8.2.2(i) in Handbook of Applied Cryptography.
#             ktot = d * e - 1
#             # The quantity d*e-1 is a multiple of phi(n), even,
#             # and can be represented as t*2^s.
#             t = ktot
#             for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
#                 if t % 2 != 0:
#                     break
#                 t //= 2
#             # Cycle through all multiplicative inverses in Zn.
#             # The algorithm is non-deterministic, but there is a 50% chance
#             # any candidate a leads to successful factoring.
#             # See "Digitalized Signatures and Public Key Functions as Intractable
#             # as Factorization", M. Rabin, 1979
#             spotted = False
#             a = Integer(2)
#             for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
#                 if not not spotted and a < 100:
#                     break
#                 k = Integer(t)
#                 for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
#                     if k >= ktot:
#                         break
#                     cand = pow(a, k, n)
#                     # Check if a^k is a non-trivial root of unity (mod n)
#                     if cand != 1 and cand != (n - 1) and pow(cand, 2, n) == 1:
#                         # We have found a number such that (cand-1)(cand+1)=0 (mod n).
#                         # Either of the terms divides n.
#                         p = Integer(n).gcd(cand + 1)
#                         spotted = True
#                         break
#                     k *= 2
#                 # This value was not any good... let's try another!
#                 a += 2
#             if not spotted:
#                 fail(" ValueError(\"Unable to compute factors p and q from exponent d.\")")
#             # Found !
#             assert ((n % p) == 0)
#             q = n // p
#
#         if hasattr(input_comps, 'u'):
#             u = input_comps.u
#         else:
#             u = p.inverse(q)
#
#         # Build key object
#         key = RsaKey(n=n, e=e, d=d, p=p, q=q, u=u)
#
#     # Verify consistency of the key
#     if consistency_check:
#
#         # Modulus and public exponent must be coprime
#         if e <= 1 or e >= n:
#             fail(" ValueError(\"Invalid RSA public exponent\")")
#         if Integer(n).gcd(e) != 1:
#             fail(" ValueError(\"RSA public exponent is not coprime to modulus\")")
#
#         # For RSA, modulus must be odd
#         if not n & 1:
#             fail(" ValueError(\"RSA modulus is not odd\")")
#
#         if key.has_private():
#             # Modulus and private exponent must be coprime
#             if d <= 1 or d >= n:
#                 fail(" ValueError(\"Invalid RSA private exponent\")")
#             if Integer(n).gcd(d) != 1:
#                 fail(" ValueError(\"RSA private exponent is not coprime to modulus\")")
#             # Modulus must be product of 2 primes
#             if p * q != n:
#                 fail(" ValueError(\"RSA factors do not match modulus\")")
#             if test_probable_prime(p) == COMPOSITE:
#                 fail(" ValueError(\"RSA factor p is composite\")")
#             if test_probable_prime(q) == COMPOSITE:
#                 fail(" ValueError(\"RSA factor q is composite\")")
#             # See Carmichael theorem
#             phi = (p - 1) * (q - 1)
#             lcm = phi // (p - 1).gcd(q - 1)
#             if (e * d % int(lcm)) != 1:
#                 fail(" ValueError(\"Invalid RSA condition\")")
#             if hasattr(key, 'u'):
#                 # CRT coefficient
#                 if u <= 1 or u >= q:
#                     fail(" ValueError(\"Invalid RSA component u\")")
#                 if (p * u % q) != 1:
#                     fail(" ValueError(\"Invalid RSA component u with p\")")
#
#     return key
#
#
# def _import_pkcs1_private(encoded, *kwargs):
#     # RSAPrivateKey ::= SEQUENCE {
#     #           version Version,
#     #           modulus INTEGER, -- n
#     #           publicExponent INTEGER, -- e
#     #           privateExponent INTEGER, -- d
#     #           prime1 INTEGER, -- p
#     #           prime2 INTEGER, -- q
#     #           exponent1 INTEGER, -- d mod (p-1)
#     #           exponent2 INTEGER, -- d mod (q-1)
#     #           coefficient INTEGER -- (inverse of q) mod p
#     # }
#     #
#     # Version ::= INTEGER
#     der = DerSequence().decode(encoded, nr_elements=9, only_ints_expected=True)
#     if der[0] != 0:
#         fail(" ValueError(\"No PKCS#1 encoding of an RSA private key\")")
#     return construct(der[1:6] + [Integer(der[4]).inverse(der[5])])
#
#
# def _import_pkcs1_public(encoded, *kwargs):
#     # RSAPublicKey ::= SEQUENCE {
#     #           modulus INTEGER, -- n
#     #           publicExponent INTEGER -- e
#     # }
#     der = DerSequence().decode(encoded, nr_elements=2, only_ints_expected=True)
#     return construct(der)
#
#
# def _import_subjectPublicKeyInfo(encoded, *kwargs):
#
#     algoid, encoded_key, params = _expand_subject_public_key_info(encoded)
#     if algoid != oid or params != None:
#         fail(" ValueError(\"No RSA subjectPublicKeyInfo\")")
#     return _import_pkcs1_public(encoded_key)
#
#
# def _import_x509_cert(encoded, *kwargs):
#
#     sp_info = _extract_subject_public_key_info(encoded)
#     return _import_subjectPublicKeyInfo(sp_info)
#
#
# def _import_pkcs8(encoded, passphrase):
#     load("@vendor//Crypto/IO", PKCS8="PKCS8")
#
#     k = PKCS8.unwrap(encoded, passphrase)
#     if k[0] != oid:
#         fail(" ValueError(\"No PKCS#8 encoded RSA key\")")
#     return _import_keyDER(k[1], passphrase)
#
#
# def _import_keyDER(extern_key, passphrase):
#     """Import an RSA key (public or private half), encoded in DER form."""
#
#     decodings = (_import_pkcs1_private,
#                  _import_pkcs1_public,
#                  _import_subjectPublicKeyInfo,
#                  _import_x509_cert,
#                  _import_pkcs8)
#
#     for decoding in decodings:
#         try:
#             return decoding(extern_key, passphrase)
#         except ValueError:
#             pass
#
#     fail(" ValueError(\"RSA key format is not supported\")")
#
#
# def _import_openssh_private_rsa(data, password):
#
#     load("@vendor//_openssh", import_openssh_private_generic="import_openssh_private_generic", read_bytes="read_bytes", read_string="read_string", check_padding="check_padding")
#
#     ssh_name, decrypted = import_openssh_private_generic(data, password)
#
#     if ssh_name != "ssh-rsa":
#         fail(" ValueError(\"This SSH key is not RSA\")")
#
#     n, decrypted = read_bytes(decrypted)
#     e, decrypted = read_bytes(decrypted)
#     d, decrypted = read_bytes(decrypted)
#     iqmp, decrypted = read_bytes(decrypted)
#     p, decrypted = read_bytes(decrypted)
#     q, decrypted = read_bytes(decrypted)
#
#     _, padded = read_string(decrypted)  # Comment
#     check_padding(padded)
#
#     build = [Integer.from_bytes(x) for x in (n, e, d, q, p, iqmp)]
#     return construct(build)
#
#
# def import_key(extern_key, passphrase=None):
#     """Import an RSA key (public or private).
#
#     Args:
#       extern_key (string or byte string):
#         The RSA key to import.
#
#         The following formats are supported for an RSA **public key**:
#
#         - X.509 certificate (binary or PEM format)
#         - X.509 ``subjectPublicKeyInfo`` DER SEQUENCE (binary or PEM
#           encoding)
#         - `PKCS#1`_ ``RSAPublicKey`` DER SEQUENCE (binary or PEM encoding)
#         - An OpenSSH line (e.g. the content of ``~/.ssh/id_ecdsa``, ASCII)
#
#         The following formats are supported for an RSA **private key**:
#
#         - PKCS#1 ``RSAPrivateKey`` DER SEQUENCE (binary or PEM encoding)
#         - `PKCS#8`_ ``PrivateKeyInfo`` or ``EncryptedPrivateKeyInfo``
#           DER SEQUENCE (binary or PEM encoding)
#         - OpenSSH (text format, introduced in `OpenSSH 6.5`_)
#
#         For details about the PEM encoding, see `RFC1421`_/`RFC1423`_.
#
#       passphrase (string or byte string):
#         For private keys only, the pass phrase that encrypts the key.
#
#     Returns: An RSA key object (:class:`RsaKey`).
#
#     Raises:
#       ValueError/IndexError/TypeError:
#         When the given key cannot be parsed (possibly because the pass
#         phrase is wrong).
#
#     .. _RFC1421: http://www.ietf.org/rfc/rfc1421.txt
#     .. _RFC1423: http://www.ietf.org/rfc/rfc1423.txt
#     .. _`PKCS#1`: http://www.ietf.org/rfc/rfc3447.txt
#     .. _`PKCS#8`: http://www.ietf.org/rfc/rfc5208.txt
#     .. _`OpenSSH 6.5`: https://flak.tedunangst.com/post/new-openssh-key-format-and-bcrypt-pbkdf
#     """
#
#     load("@vendor//Crypto/IO", PEM="PEM")
#
#     extern_key = tobytes(extern_key)
#     if passphrase != None:
#         passphrase = tobytes(passphrase)
#
#     if extern_key.startswith(builtins.bytes(r'-----BEGIN OPENSSH PRIVATE KEY', encoding='utf-8')):
#         text_encoded = tostr(extern_key)
#         openssh_encoded, marker, enc_flag = PEM.decode(text_encoded, passphrase)
#         result = _import_openssh_private_rsa(openssh_encoded, passphrase)
#         return result
#
#     if extern_key.startswith(builtins.bytes(r'-----', encoding='utf-8')):
#         # This is probably a PEM encoded key.
#         (der, marker, enc_flag) = PEM.decode(tostr(extern_key), passphrase)
#         if enc_flag:
#             passphrase = None
#         return _import_keyDER(der, passphrase)
#
#     if extern_key.startswith(builtins.bytes(r'ssh-rsa ', encoding='utf-8')):
#         # This is probably an OpenSSH key
#         keystring = binascii.a2b_base64(extern_key.split(builtins.bytes(r' ', encoding='utf-8'))[1])
#         keyparts = []
#         for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
#             if len(keystring) <= 4:
#                 break
#             length = struct.unpack(">I", keystring[:4])[0]
#             keyparts.append(keystring[4:4 + length])
#             keystring = keystring[4 + length:]
#         e = Integer.from_bytes(keyparts[1])
#         n = Integer.from_bytes(keyparts[2])
#         return construct([n, e])
#
#     if len(extern_key) > 0 and bord(extern_key[0]) == 0x30:
#         # This is probably a DER encoded key
#         return _import_keyDER(extern_key, passphrase)
#
#     fail(" ValueError(\"RSA key format is not supported\")")
#
#
# # Backward compatibility
# importKey = import_key
#
# #: `Object ID`_ for the RSA encryption algorithm. This OID often indicates
# #: a generic RSA key, even when such key will be actually used for digital
# #: signatures.
# #:
# #: .. _`Object ID`: http://www.alvestrand.no/objectid/1.2.840.113549.1.1.1.html
# oid = "1.2.840.113549.1.1.1"

