# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//types", types="types")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//math", gcd="gcd")
load("@vendor//cryptography/hazmat/backends", backends="backends")
load("@vendor//cryptography/hazmat/primitives/hashes", hashes="hashes")
load("@vendor//option/result", Error="Error")


_get_backend = backends._get_backend


def RSAPrivateKey():
    self = larky.mutablestruct(__name__='RSAPrivateKey',
                               __class__=RSAPrivateKey)

    def signer(
        padding, algorithm
    ):
        """
        Returns an AsymmetricSignatureContext used for signing data.
        """
    self.signer = signer

    def decrypt(ciphertext, padding):
        """
        Decrypts the provided ciphertext.
        """
    self.decrypt = decrypt

    def key_size():
        """
        The bit length of the public modulus.
        """
    self.key_size = key_size

    def public_key():
        """
        The RSAPublicKey associated with this private key.
        """
    self.public_key = public_key

    def sign(
        data,
        padding,
        algorithm,
    ):
        """
        Signs the data.
        """
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


RSAPrivateKeyWithSerialization = RSAPrivateKey


def RSAPublicKey():
    self = larky.mutablestruct(__name__='RSAPublicKey', __class__=RSAPublicKey)

    def verifier(
        signature,
        padding,
        algorithm,
    ):
        """
        Returns an AsymmetricVerificationContext used for verifying signatures.
        """
    self.verifier = verifier

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


RSAPublicKeyWithSerialization = RSAPublicKey


def generate_private_key(
    public_exponent,
    key_size,
    backend = None,
):
    backend = _get_backend(backend)
    if not hasattr(backend, 'generate_rsa_private_key'):
        fail("UnsupportedAlgorithm: Backend object does not implement RSABackend.",
            )

    _verify_rsa_parameters(public_exponent, key_size)
    return backend.generate_rsa_private_key(public_exponent, key_size)


def _verify_rsa_parameters(public_exponent, key_size):
    if public_exponent not in (3, 65537):
        fail("ValueError: " + ("public_exponent must be either 3 (for legacy compatibility) or " +
            "65537. Almost everyone should choose 65537 here!")
        )

    if key_size < 512:
        fail("ValueError: key_size must be at least 512-bits.")


def _check_private_key_components(
    p,
    q,
    private_exponent,
    dmp1,
    dmq1,
    iqmp,
    public_exponent,
    modulus,
):
    if modulus < 3:
        fail("ValueError: modulus must be >= 3.")

    if p >= modulus:
        fail("ValueError: p must be < modulus.")

    if q >= modulus:
        fail("ValueError: q must be < modulus.")

    if dmp1 >= modulus:
        fail("ValueError: dmp1 must be < modulus.")

    if dmq1 >= modulus:
        fail("ValueError: dmq1 must be < modulus.")

    if iqmp >= modulus:
        fail("ValueError: iqmp must be < modulus.")

    if private_exponent >= modulus:
        fail("ValueError: private_exponent must be < modulus.")

    if public_exponent < 3 or public_exponent >= modulus:
        fail("ValueError: public_exponent must be >= 3 and < modulus.")

    if public_exponent & 1 == 0:
        fail("ValueError: public_exponent must be odd.")

    if dmp1 & 1 == 0:
        fail("ValueError: dmp1 must be odd.")

    if dmq1 & 1 == 0:
        fail("ValueError: dmq1 must be odd.")

    if p * q != modulus:
        fail("ValueError: p*q must equal modulus.")


def _check_public_key_components(e, n):
    if n < 3:
        fail("ValueError: n must be >= 3.")

    if e < 3 or e >= n:
        fail("ValueError: e must be >= 3 and < n.")

    if e & 1 == 0:
        fail("ValueError: e must be odd.")


def _modinv(e, m):
    """
    Modular Multiplicative Inverse. Returns x such that: (x*e) mod m == 1
    """
    x1, x2 = 1, 0
    a, b = e, m
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if b <= 0:
            break
        q, r = divmod(a, b)
        xn = x1 - q * x2
        a, b, x1, x2 = b, r, x2, xn
    return x1 % m


def rsa_crt_iqmp(p, q):
    """
    Compute the CRT (q ** -1) % p value from RSA primes p and q.
    """
    return _modinv(q, p)


def rsa_crt_dmp1(private_exponent, p):
    """
    Compute the CRT private_exponent % (p - 1) value from the RSA
    private_exponent (d) and p.
    """
    return private_exponent % (p - 1)


def rsa_crt_dmq1(private_exponent, q):
    """
    Compute the CRT private_exponent % (q - 1) value from the RSA
    private_exponent (d) and q.
    """
    return private_exponent % (q - 1)


# Controls the number of iterations rsa_recover_prime_factors will perform
# to obtain the prime factors. Each iteration increments by 2 so the actual
# maximum attempts is half this number.
_MAX_RECOVERY_ATTEMPTS = 1000


def rsa_recover_prime_factors(n, e, d):
    """
    Compute factors p and q from the private exponent d. We assume that n has
    no more than two factors. This function is adapted from code in PyCrypto.
    """
    # See 8.2.2(i) in Handbook of Applied Cryptography.
    ktot = d * e - 1
    # The quantity d*e-1 is a multiple of phi(n), even,
    # and can be represented as t*2^s.
    t = ktot
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if t % 2 != 0:
            break
        t = t // 2
    # Cycle through all multiplicative inverses in Zn.
    # The algorithm is non-deterministic, but there is a 50% chance
    # any candidate a leads to successful factoring.
    # See "Digitalized Signatures and Public Key Functions as Intractable
    # as Factorization", M. Rabin, 1979
    spotted = False
    a = 2
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        # not spotted and a < _MAX_RECOVERY_ATTEMPTS
        if not (not spotted and a < _MAX_RECOVERY_ATTEMPTS):
            break
        k = t
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if k >= ktot:
                break
            cand = pow(a, k, n)
            # Check if a^k is a non-trivial root of unity (mod n)
            if cand != 1 and cand != (n - 1) and pow(cand, 2, n) == 1:
                # We have found a number such that (cand-1)(cand+1)=0 (mod n).
                # Either of the terms divides n.
                p = gcd(cand + 1, n)
                spotted = True
                break
            k *= 2
        # This value was not any good... let's try another!
        a += 2
    if not spotted:
        fail("ValueError: Unable to compute factors p and q from exponent d.")
    # Found !
    q, r = divmod(n, p)
    if not (r == 0):
        fail("assert r == 0 failed!")
    p, q = sorted((p, q,), reverse=True)
    return (p, q)


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
        backend = _get_backend(backend)
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
        backend = _get_backend(backend)
        return backend.load_rsa_public_numbers(self)
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


rsa = larky.struct(
    __name__='rsa',
    RSAPrivateKey=RSAPrivateKey,
    RSAPrivateKeyWithSerialization=RSAPrivateKeyWithSerialization,
    RSAPublicKey=RSAPublicKey,
    RSAPublicKeyWithSerialization=RSAPublicKeyWithSerialization,
    generate_private_key=generate_private_key,
    _verify_rsa_parameters=_verify_rsa_parameters,
    _check_private_key_components=_check_private_key_components,
    _check_public_key_components=_check_public_key_components,
    _modinv=_modinv,
    rsa_crt_iqmp=rsa_crt_iqmp,
    rsa_crt_dmp1=rsa_crt_dmp1,
    rsa_crt_dmq1=rsa_crt_dmq1,
    _MAX_RECOVERY_ATTEMPTS=_MAX_RECOVERY_ATTEMPTS,
    rsa_recover_prime_factors=rsa_recover_prime_factors,
    RSAPrivateNumbers=RSAPrivateNumbers,
    RSAPublicNumbers=RSAPublicNumbers,
)