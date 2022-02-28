load("@stdlib//larky", larky="larky")
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@vendor//ecdsa/ecdsa", ecdsa="ecdsa")
load("@vendor//ecdsa/der", "der")
load("@vendor//ecdsa/curves", "curves")
load("@vendor//ecdsa/rfc6979", "rfc6979")
load("@vendor//Crypto/Hash/SHA1", SHA1="SHA1")
load("@vendor//Crypto/Random/random", randrange="randrange")
load("@vendor//ecdsa/util", string_to_number="string_to_number", bit_length="bit_length", sigencode_string="sigencode_string")
load("@vendor//ecdsa/ellipticcurve", ellipticcurve="ellipticcurve")
load("@vendor//ecdsa/_compat", normalise_bytes="normalise_bytes", str_idx_as_int="str_idx_as_int")

def _truncate_and_convert_digest(digest, curve, allow_truncate):
    """Truncates and converts digest to an integer."""
    if not allow_truncate:
        if len(digest) > curve.baselen:
            fail("BadDigestError(this curve %s is too short for the length of your digest %d)" % (curve.name, 8 * len(digest)))
    else:
        digest = digest[: curve.baselen]
    number = string_to_number(digest)
    if allow_truncate:
        max_length = bit_length(curve.order)
        # we don't use bit_length(number) as that truncates leading zeros
        length = len(digest) * 8

        # See NIST FIPS 186-4:
        #
        # When the length of the output of the hash function is greater
        # than N (i.e., the bit length of q), then the leftmost N bits of
        # the hash function output block shall be used in any calculation
        # using the hash function output during the generation or
        # verification of a digital signature.
        #
        # as such, we need to shift-out the low-order bits:
        number >>= max(0, length - max_length)

    return number

def VerifyingKey(_error__please_use_generate=None):
    """
    Class for handling keys that can verify signatures (public keys).
    :ivar ecdsa.curves.Curve curve: The Curve over which all the cryptographic
        operations will take place
    :ivar default_hashfunc: the function that will be used for hashing the
        data. Should implement the same API as hashlib.sha1
    :vartype default_hashfunc: callable
    :ivar pubkey: the actual public key
    :vartype pubkey: ecdsa.ecdsa.Public_key
    """
    self = larky.mutablestruct(__class__=VerifyingKey, __name__="VerifyingKey")

    def __init__(_error__please_use_generate=None):
        """Unsupported, please use one of the classmethods to initialise."""
        if not _error__please_use_generate:
            fail('TypeError("Please use VerifyingKey.generate() to construct me"')
        self.curve = None
        self.default_hashfunc = None
        self.pubkey = None
        return self
    self = __init__(_error__please_use_generate)

    # @classmethod
    def from_public_point(point, curve, hashfunc=SHA1, validate_point=True):
        """
        Initialise the object from a Point object.
        This is a low-level method, generally you will not want to use it.
        :param point: The point to wrap around, the actual public key
        :type point: ecdsa.ellipticcurve.Point
        :param curve: The curve on which the point needs to reside, defaults
            to NIST192p
        :type curve: ecdsa.curves.Curve
        :param hashfunc: The default hash function that will be used for
            verification, needs to implement the same interface
            as hashlib.sha1
        :type hashfunc: callable
        :type bool validate_point: whether to check if the point lays on curve
            should always be used if the public point is not a result
            of our own calculation
        :raises MalformedPointError: if the public point does not lay on the
            curve
        :return: Initialised VerifyingKey object
        :rtype: VerifyingKey
        """
        # self = cls(_error__please_use_generate=True)
        self = __init__(True)
        # if isinstance(curve.curve, CurveEdTw):
        #     fail('ValueError("Method incompatible with Edwards curves")')
        # if point.__class__ != ellipticcurve.PointJacobi:
        #     point = ellipticcurve.PointJacobi.from_affine(point)
        self.curve = curve
        self.default_hashfunc = hashfunc
        # try:
        #     self.pubkey = ecdsa.Public_key(
        #         curve.generator, point, validate_point
        #     )
        # except ecdsa.InvalidPointError:
        #     raise MalformedPointError("Point does not lay on the curve")
        self.pubkey = ecdsa.Public_key(curve.generator, point, validate_point)
        self.pubkey.order = curve.order
        return self
    self.from_public_point = from_public_point

    return self


def SigningKey(_error__please_use_generate=None):
    """
    Class for handling keys that can create signatures (private keys).
    :ivar ecdsa.curves.Curve curve: The Curve over which all the cryptographic
        operations will take place
    :ivar default_hashfunc: the function that will be used for hashing the
        data. Should implement the same API as hashlib.sha1
    :ivar int baselen: the length of a :term:`raw encoding` of private key
    :ivar ecdsa.keys.VerifyingKey verifying_key: the public key
        associated with this private key
    :ivar ecdsa.ecdsa.Private_key privkey: the actual private key
    """
    self = larky.mutablestruct(__class__=SigningKey, __name__="SigningKey")
    def __init__(_error__please_use_generate=None):
        """Unsupported, please use one of the classmethods to initialise."""
        if not _error__please_use_generate:
            fail('TypeError("Please use SigningKey.generate() to construct me")')
        self.curve = None
        self.default_hashfunc = None
        self.baselen = None
        self.verifying_key = None
        self.privkey = None
        return self
    self = __init__(_error__please_use_generate)

    def _weierstrass_keygen(curve, entropy, hashfunc):
        """Generate a private key on a Weierstrass curve."""
        secexp = randrange(curve.order, entropy)
        return self.from_secret_exponent(secexp, curve, hashfunc)
    self._weierstrass_keygen = _weierstrass_keygen

    def generate(curve, entropy=None, hashfunc=SHA1):
        """
        Generate a random private key.
        :param curve: The curve on which the point needs to reside, defaults
            to NIST192p
        :type curve: ecdsa.curves.Curve
        :param entropy: Source of randomness for generating the private keys,
            should provide cryptographically secure random numbers if the keys
            need to be secure. Uses os.urandom() by default.
        :type entropy: callable
        :param hashfunc: The default hash function that will be used for
            signing, needs to implement the same interface
            as hashlib.sha1
        :type hashfunc: callable
        :return: Initialised SigningKey object
        :rtype: SigningKey
        """
        # if isinstance(curve.curve, CurveEdTw):
        #     return cls._twisted_edwards_keygen(curve, entropy)
        return self._weierstrass_keygen(curve, entropy, hashfunc)
    self.generate = generate
    
    # @classmethod
    def from_secret_exponent(secexp, curve, hashfunc=SHA1):
        """
        Create a private key from a random integer.
        Note: it's a low level method, it's recommended to use the
        :func:`~SigningKey.generate` method to create private keys.
        :param int secexp: secret multiplier (the actual private key in ECDSA).
            Needs to be an integer between 1 and the curve order.
        :param curve: The curve on which the point needs to reside
        :type curve: ecdsa.curves.Curve
        :param hashfunc: The default hash function that will be used for
            signing, needs to implement the same interface
            as hashlib.sha1
        :type hashfunc: callable
        :raises MalformedPointError: when the provided secexp is too large
            or too small for the curve selected
        :raises RuntimeError: if the generation of public key from private
            key failed
        :return: Initialised SigningKey object
        :rtype: SigningKey
        """
        # currently we implement ecdsa only for secp256k1 which uses weierstrass curve. 
        # if isinstance(curve.curve, CurveEdTw):
        #     fail('ValueError("Edwards keys don\'t support setting the secret scalar (exponent) directly")')
        self = __init__(True)
        self.curve = curve
        self.default_hashfunc = hashfunc
        self.baselen = curve.baselen
        n = curve.order
        if not (1 <= secexp and secexp < n):
            fail('MalformedPointError("Invalid value for secexp, expected integer between 1 and %s"' % n)
        # Frozen struct err occurs
        pubkey_point = curve.generator * secexp
        if hasattr(pubkey_point, "scale"):
            pubkey_point = pubkey_point.scale()
        self.verifying_key = VerifyingKey().from_public_point(
            pubkey_point, curve, hashfunc, False
        )
        pubkey = self.verifying_key.pubkey
        self.privkey = ecdsa.Private_key(pubkey, secexp)
        self.privkey.order = n
        return self
    self.from_secret_exponent = from_secret_exponent

    #  @classmethod
    def from_string(string, curve, hashfunc=SHA1):
        """
        Decode the private key from :term:`raw encoding`.
        Note: the name of this method is a misnomer coming from days of
        Python 2, when binary strings and character strings shared a type.
        In Python 3, the expected type is `bytes`.
        :param string: the raw encoding of the private key
        :type string: bytes like object
        :param curve: The curve on which the point needs to reside
        :type curve: ecdsa.curves.Curve
        :param hashfunc: The default hash function that will be used for
            signing, needs to implement the same interface
            as hashlib.sha1
        :type hashfunc: callable
        :raises MalformedPointError: if the length of encoding doesn't match
            the provided curve or the encoded values is too large
        :raises RuntimeError: if the generation of public key from private
            key failed
        :return: Initialised SigningKey object
        :rtype: SigningKey
        """
        string = normalise_bytes(string)

        if len(string) != curve.baselen:
            fail('MalformedPointError("Invalid length of private key, received {0}, expected {1}")'.format(len(string), curve.baselen))
        # if isinstance(curve.curve, CurveEdTw):
        #     self = cls(_error__please_use_generate=True)
        #     self.curve = curve
        #     self.default_hashfunc = None  # Ignored for EdDSA
        #     self.baselen = curve.baselen
        #     self.privkey = eddsa.PrivateKey(curve.generator, string)
        #     self.verifying_key = VerifyingKey.from_string(
        #         self.privkey.public_key().public_key(), curve
        #     )
        #     return self
        secexp = string_to_number(string)
        return self.from_secret_exponent(secexp, curve, hashfunc)
    self.from_string = from_string

    # @classmethod
    def from_der(string, hashfunc=SHA1, valid_curve_encodings=None):
        """
        Initialise from key stored in :term:`DER` format.
        The DER formats supported are the un-encrypted RFC5915
        (the ssleay format) supported by OpenSSL, and the more common
        un-encrypted RFC5958 (the PKCS #8 format).
        Both formats contain an ASN.1 object following the syntax specified
        in RFC5915::
            ECPrivateKey ::= SEQUENCE {
              version        INTEGER { ecPrivkeyVer1(1) }} (ecPrivkeyVer1),
              privateKey     OCTET STRING,
              parameters [0] ECParameters {{ NamedCurve }} OPTIONAL,
              publicKey  [1] BIT STRING OPTIONAL
            }
        `publicKey` field is ignored completely (errors, if any, in it will
        be undetected).
        Two formats are supported for the `parameters` field: the named
        curve and the explicit encoding of curve parameters.
        In the legacy ssleay format, this implementation requires the optional
        `parameters` field to get the curve name. In PKCS #8 format, the curve
        is part of the PrivateKeyAlgorithmIdentifier.
        The PKCS #8 format includes an ECPrivateKey object as the `privateKey`
        field within a larger structure:
            OneAsymmetricKey ::= SEQUENCE {
                version                   Version,
                privateKeyAlgorithm       PrivateKeyAlgorithmIdentifier,
                privateKey                PrivateKey,
                attributes            [0] Attributes OPTIONAL,
                ...,
                [[2: publicKey        [1] PublicKey OPTIONAL ]],
                ...
            }
        The `attributes` and `publicKey` fields are completely ignored; errors
        in them will not be detected.
        :param string: binary string with DER-encoded private ECDSA key
        :type string: bytes like object
        :param valid_curve_encodings: list of allowed encoding formats
            for curve parameters. By default (``None``) all are supported:
            ``named_curve`` and ``explicit``.
            Ignored for EdDSA.
        :type valid_curve_encodings: :term:`set-like object`
        :raises MalformedPointError: if the length of encoding doesn't match
            the provided curve or the encoded values is too large
        :raises RuntimeError: if the generation of public key from private
            key failed
        :raises UnexpectedDER: if the encoding of the DER file is incorrect
        :return: Initialised SigningKey object
        :rtype: SigningKey
        """
        s = normalise_bytes(string)
        curve = None

        s, empty = der.remove_sequence(s)
        if empty != b'':
            fail('der.UnexpectedDER("trailing junk after DER privkey: %s")' % hexlify(empty))
        
        version, s = der.remove_integer(s)

        # At this point, PKCS #8 has a sequence containing the algorithm
        # identifier and the curve identifier. The ssleay format instead has
        # an octet string containing the key data, so this is how we can
        # distinguish the two formats.
        if der.is_sequence(s):
            fail('ValueError("Only SECP256k1 supported")')
            # if version not in (0, 1):
            #     fail('der.UnexpectedDER("expected version 0 or 1 at start of privkey, got %d")' % version)

            # sequence, s = der.remove_sequence(s)
            # algorithm_oid, algorithm_identifier = der.remove_object(sequence)

            # if algorithm_oid in (Ed25519.oid, Ed448.oid):
            #     if algorithm_identifier:
            #         raise der.UnexpectedDER(
            #             "Non NULL parameters for a EdDSA key"
            #         )
            #     key_str_der, s = der.remove_octet_string(s)

            #     # As RFC5958 describe, there are may be optional Attributes
            #     # and Publickey. Don't raise error if something after
            #     # Privatekey

            #     # TODO parse attributes or validate publickey
            #     # if s:
            #     #     raise der.UnexpectedDER(
            #     #         "trailing junk inside the privateKey"
            #     #     )
            #     key_str, s = der.remove_octet_string(key_str_der)
            #     if s:
            #         raise der.UnexpectedDER(
            #             "trailing junk after the encoded private key"
            #         )

            #     if algorithm_oid == Ed25519.oid:
            #         curve = Ed25519
            #     else:
            #         assert algorithm_oid == Ed448.oid
            #         curve = Ed448

            #     return cls.from_string(key_str, curve, None)

            # if algorithm_oid not in (oid_ecPublicKey, oid_ecDH, oid_ecMQV):
            #     fail('der.UnexpectedDER( "unexpected algorithm identifier %s")' % (algorithm_oid,))

            # curve = curves.from_der(algorithm_identifier, valid_curve_encodings)

            # if empty != b"":
            #     fail('der.UnexpectedDER("unexpected data after algorithm identifier: %s"' % hexlify(empty))

            # # Up next is an octet string containing an ECPrivateKey. Ignore
            # # the optional "attributes" and "publicKey" fields that come after.
            # s, _ = der.remove_octet_string(s)

            # # Unpack the ECPrivateKey to get to the key data octet string,
            # # and rejoin the ssleay parsing path.
            # s, empty = der.remove_sequence(s)
            # if empty != b"":
            #     fail('der.UnexpectedDER("trailing junk after DER privkey: %s")' % binascii.hexlify(empty))

            # version, s = der.remove_integer(s)

        # The version of the ECPrivateKey must be 1.
        if version != 1:
            fail('der.UnexpectedDER("expected version 1 at start of DER privkey, got %d")' % version)

        privkey_str, s = der.remove_octet_string(s)

        if not curve:
            tag, curve_oid_str, s = der.remove_constructed(s)
            if tag != 0:
                fail('der.UnexpectedDER("expected tag 0 in DER privkey, got %d")' % tag)
            curve = curves.from_der(curve_oid_str, valid_curve_encodings)

        # we don't actually care about the following fields
        #
        # tag, pubkey_bitstring, s = der.remove_constructed(s)
        # if tag != 1:
        #     raise der.UnexpectedDER("expected tag 1 in DER privkey, got %d"
        #                             % tag)
        # pubkey_str = der.remove_bitstring(pubkey_bitstring, 0)
        # if empty != "":
        #     raise der.UnexpectedDER("trailing junk after DER privkey "
        #                             "pubkeystr: %s"
        #                             % binascii.hexlify(empty))

        # our from_string method likes fixed-length privkey strings
        if len(privkey_str) < curve.baselen:
            privkey_str = (
                b"\x00" * (curve.baselen - len(privkey_str)) + privkey_str
            )
        return self.from_string(privkey_str, curve, hashfunc)
    self.from_der = from_der

    def sign_deterministic(
        data,
        hashfunc=None,
        sigencode=sigencode_string,
        extra_entropy=b"",
    ):
        """
        Create signature over data.
        For Weierstrass curves it uses the deterministic RFC6979 algorithm.
        For Edwards curves it uses the standard EdDSA algorithm.
        For ECDSA the data will be hashed using the `hashfunc` function before
        signing.
        For EdDSA the data will be hashed with the hash associated with the
        curve (SHA-512 for Ed25519 and SHAKE-256 for Ed448).
        This is the recommended method for performing signatures when hashing
        of data is necessary.
        :param data: data to be hashed and computed signature over
        :type data: bytes like object
        :param hashfunc: hash function to use for computing the signature,
            if unspecified, the default hash function selected during
            object initialisation will be used (see
            `VerifyingKey.default_hashfunc`). The object needs to implement
            the same interface as hashlib.sha1.
            Ignored with EdDSA.
        :type hashfunc: callable
        :param sigencode: function used to encode the signature.
            The function needs to accept three parameters: the two integers
            that are the signature and the order of the curve over which the
            signature was computed. It needs to return an encoded signature.
            See `ecdsa.util.sigencode_string` and `ecdsa.util.sigencode_der`
            as examples of such functions.
            Ignored with EdDSA.
        :type sigencode: callable
        :param extra_entropy: additional data that will be fed into the random
            number generator used in the RFC6979 process. Entirely optional.
            Ignored with EdDSA.
        :type extra_entropy: bytes like object
        :return: encoded signature over `data`
        :rtype: bytes or sigencode function dependent type
        """
        hashfunc = hashfunc or self.default_hashfunc
        data = normalise_bytes(data)

        # if isinstance(self.curve.curve, CurveEdTw):
        #     return self.privkey.sign(data)

        extra_entropy = normalise_bytes(extra_entropy)
        digest = hashfunc(data).digest()

        return self.sign_digest_deterministic(
            digest,
            hashfunc=hashfunc,
            sigencode=sigencode,
            extra_entropy=extra_entropy,
            allow_truncate=True,
        )
    self.sign_deterministic = sign_deterministic

    def sign_digest_deterministic(
        digest,
        hashfunc=None,
        sigencode=sigencode_string,
        extra_entropy=b"",
        allow_truncate=False,
    ):
        """
        Create signature for digest using the deterministic RFC6979 algorithm.
        `digest` should be the output of cryptographically secure hash function
        like SHA256 or SHA-3-256.
        This is the recommended method for performing signatures when no
        hashing of data is necessary.
        :param digest: hash of data that will be signed
        :type digest: bytes like object
        :param hashfunc: hash function to use for computing the random "k"
            value from RFC6979 process,
            if unspecified, the default hash function selected during
            object initialisation will be used (see
            `VerifyingKey.default_hashfunc`). The object needs to implement
            the same interface as hashlib.sha1.
        :type hashfunc: callable
        :param sigencode: function used to encode the signature.
            The function needs to accept three parameters: the two integers
            that are the signature and the order of the curve over which the
            signature was computed. It needs to return an encoded signature.
            See `ecdsa.util.sigencode_string` and `ecdsa.util.sigencode_der`
            as examples of such functions.
        :type sigencode: callable
        :param extra_entropy: additional data that will be fed into the random
            number generator used in the RFC6979 process. Entirely optional.
        :type extra_entropy: bytes like object
        :param bool allow_truncate: if True, the provided digest can have
            bigger bit-size than the order of the curve, the extra bits (at
            the end of the digest) will be truncated. Use it when signing
            SHA-384 output using NIST256p or in similar situations.
        :return: encoded signature for the `digest` hash
        :rtype: bytes or sigencode function dependent type
        """
        # if isinstance(self.curve.curve, CurveEdTw):
        #     raise ValueError("Method unsupported for Edwards curves")
        secexp = self.privkey.secret_multiplier
        hashfunc = hashfunc or self.default_hashfunc
        digest = normalise_bytes(digest)
        extra_entropy = normalise_bytes(extra_entropy)

        def simple_r_s(r, s, order):
            return r, s, order

        retry_gen = 0
        # TODO
        # while True:
        k = rfc6979.generate_k(
            self.curve.generator.order(),
            secexp,
            hashfunc,
            digest,
            retry_gen=retry_gen,
            extra_entropy=extra_entropy,
        )
            # try:
        r, s, order = self.sign_digest(
            digest,
            sigencode=simple_r_s,
            k=k,
            allow_truncate=allow_truncate,
        )
            #     break
            # except RSZeroError:
            #     retry_gen += 1

        return sigencode(r, s, order)
    self.sign_digest_deterministic = sign_digest_deterministic

    def sign_digest(
        digest,
        entropy=None,
        sigencode=sigencode_string,
        k=None,
        allow_truncate=False,
    ):
        """
        Create signature over digest using the probabilistic ECDSA algorithm.
        This method uses the standard ECDSA algorithm that requires a
        cryptographically secure random number generator.
        This method does not hash the input.
        It's recommended to use the
        :func:`~SigningKey.sign_digest_deterministic` method
        instead of this one.
        :param digest: hash value that will be signed
        :type digest: bytes like object
        :param callable entropy: randomness source, os.urandom by default
        :param sigencode: function used to encode the signature.
            The function needs to accept three parameters: the two integers
            that are the signature and the order of the curve over which the
            signature was computed. It needs to return an encoded signature.
            See `ecdsa.util.sigencode_string` and `ecdsa.util.sigencode_der`
            as examples of such functions.
        :type sigencode: callable
        :param int k: a pre-selected nonce for calculating the signature.
            In typical use cases, it should be set to None (the default) to
            allow its generation from an entropy source.
        :param bool allow_truncate: if True, the provided digest can have
            bigger bit-size than the order of the curve, the extra bits (at
            the end of the digest) will be truncated. Use it when signing
            SHA-384 output using NIST256p or in similar situations.
        :raises RSZeroError: in the unlikely event when "r" parameter or
            "s" parameter of the created signature is equal 0, as that would
            leak the key. Caller should try a better entropy source, retry with
            different 'k', or use the
            :func:`~SigningKey.sign_digest_deterministic` in such case.
        :return: encoded signature for the `digest` hash
        :rtype: bytes or sigencode function dependent type
        """
        # if isinstance(self.curve.curve, CurveEdTw):
        #     raise ValueError("Method unsupported for Edwards curves")
        digest = normalise_bytes(digest)
        number = _truncate_and_convert_digest(
            digest, self.curve, allow_truncate,
        )
        r, s = self.sign_number(number, entropy, k)
        return sigencode(r, s, self.privkey.order)
    self.sign_digest = sign_digest

    def sign_number(number, entropy=None, k=None):
        """
        Sign an integer directly.
        Note, this is a low level method, usually you will want to use
        :func:`~SigningKey.sign_deterministic` or
        :func:`~SigningKey.sign_digest_deterministic`.
        :param int number: number to sign using the probabilistic ECDSA
            algorithm.
        :param callable entropy: entropy source, os.urandom by default
        :param int k: pre-selected nonce for signature operation. If unset
            it will be selected at random using the entropy source.
        :raises RSZeroError: in the unlikely event when "r" parameter or
            "s" parameter of the created signature is equal 0, as that would
            leak the key. Caller should try a better entropy source, retry with
            different 'k', or use the
            :func:`~SigningKey.sign_digest_deterministic` in such case.
        :return: the "r" and "s" parameters of the signature
        :rtype: tuple of ints
        # """
        # if isinstance(self.curve.curve, CurveEdTw):
        #     raise ValueError("Method unsupported for Edwards curves")
        order = self.privkey.order

        if k != None:
            _k = k
        else:
            _k = randrange(order, entropy)

        # assert 1 <= _k < order
        if (_k >= 1) and (_k < order):
            fail('AssertError()')
        sig = self.privkey.sign(number, _k)
        return sig.r, sig.s
    self.sign_number = sign_number

    return self


keys = larky.struct(
    SigningKey=SigningKey,
    VerifyingKey=VerifyingKey
)