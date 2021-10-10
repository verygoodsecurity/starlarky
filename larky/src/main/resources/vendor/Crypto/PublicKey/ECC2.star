load("@stdlib//binascii", binascii="binascii")
load("@stdlib//larky", larky="larky")
load("@stdlib//struct", struct="struct")
load("@stdlib//sets", "sets")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@stdlib//types", types="types")
load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")
load("@vendor//Crypto/Util/asn1", DerSequence="DerSequence", DerOctetString="DerOctetString", DerObjectId="DerObjectId", DerBitString="DerBitString")
load("@vendor//Crypto/IO/PEM", PEM="PEM")
load("@vendor//Crypto/Math/Numbers", Integer="Integer")
load("@vendor//Crypto/Util/number", long_to_bytes="long_to_bytes", bytes_to_long="bytes_to_long", getRandomRange="getRandomRange")
load("@vendor//Crypto/Math/Primality", test_probable_prime="test_probable_prime", generate_probable_prime="generate_probable_prime", COMPOSITE="COMPOSITE")
load("@vendor//Crypto/PublicKey",
     _expand_subject_public_key_info="expand_subject_public_key_info",
     _create_subject_public_key_info="create_subject_public_key_info",
     _extract_subject_public_key_info="extract_subject_public_key_info")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//operator", operator="operator")
load("@vendor//option/result", Error="Error")


# _ec_lib = load_pycryptodome_raw_lib("Crypto.PublicKey._ec_ws", """
# typedef void EcContext;
# typedef void EcPoint;
# int ec_ws_new_context(EcContext **pec_ctx,
#                       const uint8_t *modulus,
#                       const uint8_t *b,
#                       const uint8_t *order,
#                       size_t len,
#                       uint64_t seed);
# void ec_free_context(EcContext *ec_ctx);
# int ec_ws_new_point(EcPoint **pecp,
#                     const uint8_t *x,
#                     const uint8_t *y,
#                     size_t len,
#                     const EcContext *ec_ctx);
# void ec_free_point(EcPoint *ecp);
# int ec_ws_get_xy(uint8_t *x,
#                  uint8_t *y,
#                  size_t len,
#                  const EcPoint *ecp);
# int ec_ws_double(EcPoint *p);
# int ec_ws_add(EcPoint *ecpa, EcPoint *ecpb);
# int ec_ws_scalar(EcPoint *ecp,
#                  const uint8_t *k,
#                  size_t len,
#                  uint64_t seed);
# int ec_ws_clone(EcPoint **pecp2, const EcPoint *ecp);
# int ec_ws_copy(EcPoint *ecp1, const EcPoint *ecp2);
# int ec_ws_cmp(const EcPoint *ecp1, const EcPoint *ecp2);
# int ec_ws_neg(EcPoint *p);
# int ec_ws_normalize(EcPoint *ecp);
# int ec_ws_is_pai(EcPoint *ecp);
# """)
#
# # _Curve = namedtuple("_Curve", "p b order Gx Gy G modulus_bits oid context desc openssh")
# _curves = {}
#
#
# # p256_names = ["p256", "NIST P-256", "P-256", "prime256v1", "secp256r1",
# #               "nistp256"]
#
#
# def init_p256():
#     p = 0xffffffff00000001000000000000000000000000ffffffffffffffffffffffff
#     b = 0x5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b
#     order = 0xffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551
#     Gx = 0x6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296
#     Gy = 0x4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5
#
#     p256_modulus = long_to_bytes(p, 32)
#     p256_b = long_to_bytes(b, 32)
#     p256_order = long_to_bytes(order, 32)
#
#     # ec_p256_context = VoidPointer()
#     # result = _ec_lib.ec_ws_new_context(ec_p256_context.address_of(),
#     #                                    c_uint8_ptr(p256_modulus),
#     #                                    c_uint8_ptr(p256_b),
#     #                                    c_uint8_ptr(p256_order),
#     #                                    c_size_t(len(p256_modulus)),
#     #                                    c_ulonglong(getrandbits(64))
#     #                                    )
#     # if result:
#     #     return Error("ImportError: " + "Error %d initializing P-256 context" % result)
#
#     # context = SmartPointer(ec_p256_context.get(), _ec_lib.ec_free_context)
#     # context = _JCrypto.PublicKey
#     # p256 = _Curve(Integer(p),
#     #               Integer(b),
#     #               Integer(order),
#     #               Integer(Gx),
#     #               Integer(Gy),
#     #               None,
#     #               256,
#     #               "1.2.840.10045.3.1.7",    # ANSI X9.62
#     #             #   context,
#     #               "NIST P-256",
#     #               "ecdsa-sha2-nistp256")
#     kwargs = {'p': Integer(p),
#           'b': Integer(b),
#           'order': Integer(order),
#           'Gx': Integer(Gx),
#           'Gy': Integer(Gy),
#           'G': None,
#           'modulus_bits': 256,
#           'oid': "1.2.840.10045.3.1.7",
#           'context': None,
#           'desc': "NIST P-256",
#           'openssh': "ecdsa-sha2-nistp256"}
#     p256 = larky.mutablestruct(__name__="_Curve", **kwargs)
#     # global p256_names
#     # _curves.update(dict.fromkeys(p256_names, p256))
#     _curves["p256"] = p256
#     _curves["NIST P-256"] = p256
#     _curves["P-256"] = p256
#     _curves["prime256v1"] = p256
#     _curves["secp256r1"] = p256
#     _curves["nistp256"] = p256
#
#
# init_p256()
# del init_p256


# p384_names = ["p384", "NIST P-384", "P-384", "prime384v1", "secp384r1",
#               "nistp384"]


# def init_p384():
#     p = 0xfffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffeffffffff0000000000000000ffffffff
#     b = 0xb3312fa7e23ee7e4988e056be3f82d19181d9c6efe8141120314088f5013875ac656398d8a2ed19d2a85c8edd3ec2aef
#     order = 0xffffffffffffffffffffffffffffffffffffffffffffffffc7634d81f4372ddf581a0db248b0a77aecec196accc52973
#     Gx = 0xaa87ca22be8b05378eb1c71ef320ad746e1d3b628ba79b9859f741e082542a385502f25dbf55296c3a545e3872760aB7
#     Gy = 0x3617de4a96262c6f5d9e98bf9292dc29f8f41dbd289a147ce9da3113b5f0b8c00a60b1ce1d7e819d7a431d7c90ea0e5F

#     p384_modulus = long_to_bytes(p, 48)
#     p384_b = long_to_bytes(b, 48)
#     p384_order = long_to_bytes(order, 48)

#     ec_p384_context = VoidPointer()
#     result = _ec_lib.ec_ws_new_context(ec_p384_context.address_of(),
#                                        c_uint8_ptr(p384_modulus),
#                                        c_uint8_ptr(p384_b),
#                                        c_uint8_ptr(p384_order),
#                                        c_size_t(len(p384_modulus)),
#                                        c_ulonglong(getrandbits(64))
#                                        )
#     if result:
#         return Error("ImportError: " + "Error %d initializing P-384 context" % result)

#     context = SmartPointer(ec_p384_context.get(), _ec_lib.ec_free_context)
#     p384 = _Curve(Integer(p),
#                   Integer(b),
#                   Integer(order),
#                   Integer(Gx),
#                   Integer(Gy),
#                   None,
#                   384,
#                   "1.3.132.0.34",   # SEC 2
#                   context,
#                   "NIST P-384",
#                   "ecdsa-sha2-nistp384")
#     global p384_names
#     _curves.update(dict.fromkeys(p384_names, p384))


# init_p384()
# del init_p384


# p521_names = ["p521", "NIST P-521", "P-521", "prime521v1", "secp521r1",
#               "nistp521"]


# def init_p521():
#     p = 0x000001ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
#     b = 0x00000051953eb9618e1c9a1f929a21a0b68540eea2da725b99b315f3b8b489918ef109e156193951ec7e937b1652c0bd3bb1bf073573df883d2c34f1ef451fd46b503f00
#     order = 0x000001fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffa51868783bf2f966b7fcc0148f709a5d03bb5c9b8899c47aebb6fb71e91386409
#     Gx = 0x000000c6858e06b70404e9cd9e3ecb662395b4429c648139053fb521f828af606b4d3dbaa14b5e77efe75928fe1dc127a2ffa8de3348b3c1856a429bf97e7e31c2e5bd66
#     Gy = 0x0000011839296a789a3bc0045c8a5fb42c7d1bd998f54449579b446817afbd17273e662c97ee72995ef42640c550b9013fad0761353c7086a272c24088be94769fd16650

#     p521_modulus = long_to_bytes(p, 66)
#     p521_b = long_to_bytes(b, 66)
#     p521_order = long_to_bytes(order, 66)

#     ec_p521_context = VoidPointer()
#     result = _ec_lib.ec_ws_new_context(ec_p521_context.address_of(),
#                                        c_uint8_ptr(p521_modulus),
#                                        c_uint8_ptr(p521_b),
#                                        c_uint8_ptr(p521_order),
#                                        c_size_t(len(p521_modulus)),
#                                        c_ulonglong(getrandbits(64))
#                                        )
#     if result:
#         return Error("ImportError: " + "Error %d initializing P-521 context" % result)

#     context = SmartPointer(ec_p521_context.get(), _ec_lib.ec_free_context)
#     p521 = _Curve(Integer(p),
#                   Integer(b),
#                   Integer(order),
#                   Integer(Gx),
#                   Integer(Gy),
#                   None,
#                   521,
#                   "1.3.132.0.35",   # SEC 2
#                   context,
#                   "NIST P-521",
#                   "ecdsa-sha2-nistp521")
#     global p521_names
#     _curves.update(dict.fromkeys(p521_names, p521))


# init_p521()
# del init_p521

#
# def UnsupportedEccFeature(ValueError):
#     pass
#
# def EccPoint(x, y, curve="p256"):
#     """
#     A class to abstract a point over an Elliptic Curve.
#
#         The class support special methods for:
#
#         * Adding two points: ``R = S + T``
#         * In-place addition: ``S += T``
#         * Negating a point: ``R = -T``
#         * Comparing two points: ``if S == T: ...``
#         * Multiplying a point by a scalar: ``R = S*k``
#         * In-place multiplication by a scalar: ``T *= k``
#
#         :ivar x: The affine X-coordinate of the ECC point
#         :vartype x: integer
#
#         :ivar y: The affine Y-coordinate of the ECC point
#         :vartype y: integer
#
#         :ivar xy: The tuple with X- and Y- coordinates
#
#     """
#     self = larky.mutablestruct(__class__=EccPoint, __name__="EccPoint")
#
#     def set(self, point):
#         """
#         Error %d while cloning an EC point
#         """
#     def __eq__(self, point):
#         """
#         Error %d while inverting an EC point
#         """
#     def copy(self):
#         """
#         Return a copy of this point.
#         """
#     def is_point_at_infinity(self):
#         """
#         ``True`` if this is the point-at-infinity.
#         """
#     def point_at_infinity(self):
#         """
#         Return the point-at-infinity for the curve this point is on.
#         """
#     def x():
#         """
#         Error %d while encoding an EC point
#         """
#         return self.xy[0]
#     self.x = x
#
#     def y():
#         return self.xy[1]
#     self.y = y
#
#     # @property
#     def xy():
#         modulus_bytes = self.size_in_bytes()
#         xb = bytearray(modulus_bytes)
#         yb = bytearray(modulus_bytes)
#         # result = _ec_lib.ec_ws_get_xy(c_uint8_ptr(xb),
#         #                               c_uint8_ptr(yb),
#         #                               c_size_t(modulus_bytes),
#         #                               self._point.get())
#         # if result:
#         #     raise ValueError("Error %d while encoding an EC point" % result)
#
#         return (Integer(bytes_to_long(xb)), Integer(bytes_to_long(yb)))
#     self.xy = xy
#
#     def size_in_bytes():
#         """
#         Size of each coordinate, in bytes.
#         """
#         return (self.size_in_bits() + 7) // 8
#     self.size_in_bytes = size_in_bytes
#
#     def size_in_bits(self):
#         """
#         Size of each coordinate, in bits.
#         """
#         return self._curve.modulus_bits
#     self.size_in_bits = size_in_bits
#
#     def double(self):
#         """
#         Double this point (in-place operation).
#
#                 :Return:
#                     :class:`EccPoint` : this same object (to enable chaining)
#
#         """
#     def __iadd__(self, point):
#         """
#         Add a second point to this one
#         """
#     def __add__(self, point):
#         """
#         Return a new point, the addition of this one and another
#         """
#     def __imul__(self, scalar):
#         """
#         Multiply this point by a scalar
#         """
#     def __mul__(self, scalar):
#         """
#         Return a new point, the scalar product of this one
#         """
#     def __rmul__(self, left_hand):
#         """
#          Last piece of initialization
#
#         """
#
#     def __init__(self, x, y, curve="p256"):
#         """
#         Unknown curve name %s
#         """
#         if curve in _curves:
#             self._curve = _curves[curve]
#         else:
#             fail('ValueError("Unknown curve name %s")', str(curve))
#         self._curve_name = curve
#
#         modulus_bytes = self.size_in_bytes()
#         context = self._curve.context
#
#         xb = long_to_bytes(x, modulus_bytes)
#         yb = long_to_bytes(y, modulus_bytes)
#         if len(xb) != modulus_bytes or len(yb) != modulus_bytes:
#             fail('ValueError("Incorrect coordinate length")')
#
#         # self._point = VoidPointer()
#         # result = _ec_lib.ec_ws_new_point(self._point.address_of(),
#         #                                  c_uint8_ptr(xb),
#         #                                  c_uint8_ptr(yb),
#         #                                  c_size_t(modulus_bytes),
#         #                                  context.get())
#         # if result:
#         #     if result == 15:
#         #         fail('ValueError("The EC point does not belong to the curve")')
#         #     fail('ValueError("Error %d while instantiating an EC point")', result)
#
#         # Ensure that object disposal of this Python object will (eventually)
#         # free the memory allocated by the raw library for the EC point
#         # self._point = SmartPointer(self._point.get(),
#         #                            _ec_lib.ec_free_point)
#         self._point = _JCrypto.PublicKey
#         return self
#     self = __init__(self, x, y, curve)
#     return self
#
# p256_G = EccPoint(_curves['p256'].Gx, _curves['p256'].Gy, "p256")
# _curves['p256'].G = p256_G
# p256 = _curves['p256']
# # _curves.update(dict.fromkeys(p256_names, p256))
# _curves["NIST P-256"] = p256
# _curves["P-256"] = p256
# _curves["prime256v1"] = p256
# _curves["secp256r1"] = p256
# _curves["nistp256"] = p256

def EccKey(**kwargs):
    r"""Class defining an ECC key.
    Do not instantiate directly.
    Use :func:`generate`, :func:`construct` or :func:`import_key` instead.
    :ivar curve: The name of the ECC as defined in :numref:`curve_names`.
    :vartype curve: string
    :ivar pointQ: an ECC point representating the public component
    :vartype pointQ: :class:`EccPoint`
    :ivar d: A scalar representating the private component
    :vartype d: integer
    """
    self = larky.mutablestruct(__name__='EccKey', __class__=EccKey)
    #
    # def __init__(**kwargs):
    #     """Create a new ECC key
    #     Keywords:
    #       curve : string
    #         It must be *"p256"*, *"P-256"*, *"prime256v1"* or *"secp256r1"*.
    #       d : integer
    #         Only for a private key. It must be in the range ``[1..order-1]``.
    #       point : EccPoint
    #         Mandatory for a public key. If provided for a private key,
    #         the implementation will NOT check whether it matches ``d``.
    #     """
    #
    #     kwargs_ = dict(kwargs)
    #     curve_name = kwargs_.pop("curve", None)
    #     self._d = kwargs_.pop("d", None)
    #     self._point = kwargs_.pop("point", None)
    #     if kwargs_:
    #         return fail("TypeError: Unknown parameters: %s", str(kwargs_))
    #
    #     if curve_name not in _curves:
    #         return fail("ValueError: Unsupported curve (%s)", curve_names)
    #     self._curve = _curves[curve_name] # a mutablestruct _Curve
    #
    #     if self._d == None:
    #         if self._point == None:
    #             return fail("ValueError: Either private or public ECC component must be specified, not both")
    #     else:
    #         self._d = Integer(self._d)
    #         if not (1 <= self._d) and (self._d < self._curve.order):
    #             return fail("ValueError: Invalid ECC private component")
    #
    #     self.curve = self._curve.desc
    #     return self
    # self = __init__(kwargs)
    #
    # def __eq__(self, other):
    #     """
    #     , d=%d
    #     """
    # def has_private():
    #     """
    #     ``True`` if this key can be used for making signatures or decrypting data.
    #     """
    #     return self._d != None
    # self.has_private = has_private
    #
    # def _sign(self, z, k):
    #     """
    #     This is not a private ECC key
    #     """
    #
    # def d():
    #     if not self.has_private():
    #         fail('ValueError("This is not a private ECC key")')
    #     return self._d
    # self.d = d
    #
    # # @property
    # def pointQ():
    #     if self._point == None:
    #         # for ref: self._curve.G = EccPoint(_curves['p256'].Gx, _curves['p256'].Gy, "p256")
    #         self._point = self._curve.G * self._d
    #     return self._point
    # self.pointQ = pointQ
    #
    # def public_key():
    #     """
    #     A matching ECC public key.
    #
    #             Returns:
    #                 a new :class:`EccKey` object
    #
    #     """
    # def _export_subjectPublicKeyInfo(compress):
    #     """
    #      See 2.2 in RFC5480 and 2.3.3 in SEC1
    #      The first byte is:
    #      - 0x02:   compressed, only X-coordinate, Y-coordinate is even
    #      - 0x03:   compressed, only X-coordinate, Y-coordinate is odd
    #      - 0x04:   uncompressed, X-coordinate is followed by Y-coordinate
    #
    #      PAI is in theory encoded as 0x00.
    #
    #
    #     """
    #     modulus_bytes = self.pointQ.size_in_bytes()
    #
    #     if compress:
    #         first_byte = 2 + self.pointQ.y().is_odd()
    #         public_key = (bchr(first_byte) +
    #                       self.pointQ.x().to_bytes(modulus_bytes))
    #     else:
    #         public_key = (b'\x04' +
    #                       self.pointQ.x().to_bytes(modulus_bytes) +
    #                       self.pointQ.y().to_bytes(modulus_bytes))
    #
    #     unrestricted_oid = "1.2.840.10045.2.1"
    #     return _create_subject_public_key_info(unrestricted_oid,
    #                                            public_key,
    #                                            DerObjectId(self._curve.oid))
    # self._export_subjectPublicKeyInfo = _export_subjectPublicKeyInfo
    #
    # def _export_private_der(include_ec_params=True):
    #     if not (self.has_private()):
    #         fail("assert self.has_private() failed!")
    #
    #     # ECPrivateKey ::= SEQUENCE {
    #     #           version        INTEGER { ecPrivkeyVer1(1) } (ecPrivkeyVer1),
    #     #           privateKey     OCTET STRING,
    #     #           parameters [0] ECParameters {{ NamedCurve }} OPTIONAL,
    #     #           publicKey  [1] BIT STRING OPTIONAL
    #     #    }
    #
    #     # Public key - uncompressed form
    #     # modulus_bytes = self.pointQ.size_in_bytes()
    #     self.pointQ = self.pointQ()
    #     modulus_bytes = self.pointQ.size_in_bytes()
    #     public_key = (b'\x04' +
    #                 self.pointQ.x().to_bytes(modulus_bytes) +
    #                 self.pointQ.y().to_bytes(modulus_bytes))
    #
    #     seq = [1,
    #         DerOctetString(self.d().to_bytes(modulus_bytes)),
    #         DerObjectId(self._curve.oid, explicit=0),
    #         DerBitString(public_key, explicit=1)]
    #
    #     if not include_ec_params:
    #         operator.delitem(seq, 2)
    #
    #     return codecs.encode(DerSequence(seq), encoding="utf-8")
    # self._export_private_der = _export_private_der
    #
    # def _export_pkcs8(self, **kwargs):
    #     """
    #     'passphrase'
    #     """
    # def _export_public_pem(compress):
    #     """
    #     PUBLIC KEY
    #     """
    #     encoded_der = self._export_subjectPublicKeyInfo(compress)
    #     return PEM.encode(encoded_der, "PUBLIC KEY")
    # self._export_public_pem = _export_public_pem
    #
    # def _export_private_pem(passphrase, **kwargs):
    #     """
    #     EC PRIVATE KEY
    #     """
    #     encoded_der = self._export_private_der()
    #     return PEM.encode(encoded_der, "EC PRIVATE KEY", passphrase, **kwargs)
    # self._export_private_pem = _export_private_pem
    #
    # def _export_private_clear_pkcs8_in_clear_pem(self):
    #     """
    #     PRIVATE KEY
    #     """
    # def _export_private_encrypted_pkcs8_in_clear_pem(self, passphrase, **kwargs):
    #     """
    #     'protection'
    #     """
    # # def _export_openssh(self, compress):
    #
    #
    # def _export_openssh(compress):
    #     """
    #     Cannot export OpenSSH private keys
    #     """
    #     if self.has_private():
    #         return fail("ValueError: Cannot export OpenSSH private keys")
    #
    #     desc = self._curve.openssh
    #     modulus_bytes = self.pointQ.size_in_bytes()
    #
    #     if compress:
    #         first_byte = 2 + self.pointQ.y().is_odd()
    #         public_key = (bchr(first_byte) +
    #                     self.pointQ.x().to_bytes(modulus_bytes))
    #     else:
    #         public_key = (b'\x04' +
    #                     self.pointQ.x().to_bytes(modulus_bytes) +
    #                     self.pointQ.y().to_bytes(modulus_bytes))
    #
    #     middle = desc.split("-")[2]
    #     comps = (tobytes(desc), tobytes(middle), public_key)
    #     blob = b"".join([struct.pack(">I", len(x)) + x for x in comps])
    #     return desc + " " + tostr(binascii.b2a_base64(blob))
    # self._export_openssh = _export_openssh
    #
    # def export_key(**kwargs):
    #     """
    #     Export this ECC key.
    #
    #             Args:
    #               format (string):
    #                 The format to use for encoding the key:
    #
    #                 - ``'DER'``. The key will be encoded in ASN.1 DER format (binary).
    #                   For a public key, the ASN.1 ``subjectPublicKeyInfo`` structure
    #                   defined in `RFC5480`_ will be used.
    #                   For a private key, the ASN.1 ``ECPrivateKey`` structure defined
    #                   in `RFC5915`_ is used instead (possibly within a PKCS#8 envelope,
    #                   see the ``use_pkcs8`` flag below).
    #                 - ``'PEM'``. The key will be encoded in a PEM_ envelope (ASCII).
    #                 - ``'OpenSSH'``. The key will be encoded in the OpenSSH_ format
    #                   (ASCII, public keys only).
    #
    #               passphrase (byte string or string):
    #                 The passphrase to use for protecting the private key.
    #
    #               use_pkcs8 (boolean):
    #                 Only relevant for private keys.
    #
    #                 If ``True`` (default and recommended), the `PKCS#8`_ representation
    #                 will be used.
    #
    #                 If ``False``, the much weaker `PEM encryption`_ mechanism will be used.
    #
    #               protection (string):
    #                 When a private key is exported with password-protection
    #                 and PKCS#8 (both ``DER`` and ``PEM`` formats), this parameter MUST be
    #                 present and be a valid algorithm supported by :mod:`Crypto.IO.PKCS8`.
    #                 It is recommended to use ``PBKDF2WithHMAC-SHA1AndAES128-CBC``.
    #
    #               compress (boolean):
    #                 If ``True``, a more compact representation of the public key
    #                 with the X-coordinate only is used.
    #
    #                 If ``False`` (default), the full public key will be exported.
    #
    #             .. warning::
    #                 If you don't provide a passphrase, the private key will be
    #                 exported in the clear!
    #
    #             .. note::
    #                 When exporting a private key with password-protection and `PKCS#8`_
    #                 (both ``DER`` and ``PEM`` formats), any extra parameters
    #                 to ``export_key()`` will be passed to :mod:`Crypto.IO.PKCS8`.
    #
    #             .. _PEM:        http://www.ietf.org/rfc/rfc1421.txt
    #             .. _`PEM encryption`: http://www.ietf.org/rfc/rfc1423.txt
    #             .. _`PKCS#8`:   http://www.ietf.org/rfc/rfc5208.txt
    #             .. _OpenSSH:    http://www.openssh.com/txt/rfc5656.txt
    #             .. _RFC5480:    https://tools.ietf.org/html/rfc5480
    #             .. _RFC5915:    http://www.ietf.org/rfc/rfc5915.txt
    #
    #             Returns:
    #                 A multi-line string (for PEM and OpenSSH) or bytes (for DER) with the encoded key.
    #
    #     """
    #
    #     args = kwargs.copy()
    #     ext_format = args.pop("format")
    #     if ext_format not in ("PEM", "DER", "OpenSSH"):
    #         return fail("ValueError: Unknown format '%s'", ext_format)
    #
    #     compress = args.pop("compress", False)
    #
    #     if self.has_private():
    #         passphrase = args.pop("passphrase", None)
    #         if types.is_string(passphrase):
    #             passphrase = tobytes(passphrase)
    #             if not passphrase:
    #                 return fail("ValueError: Empty passphrase")
    #         use_pkcs8 = args.pop("use_pkcs8", True)
    #         if ext_format == "PEM":
    #             if use_pkcs8:
    #                 fail('ValueError: pkcs8 is not supported currently')
    #                 # if passphrase:
    #                 #     return self._export_private_encrypted_pkcs8_in_clear_pem(passphrase, **args)
    #                 # else:
    #                 #     return self._export_private_clear_pkcs8_in_clear_pem()
    #             else:
    #                 return self._export_private_pem(passphrase, **args)
    #         elif ext_format == "DER":
    #             # DER
    #             if passphrase and not use_pkcs8:
    #                 return fail("ValueError: Private keys can only be encrpyted with DER using PKCS#8")
    #             if use_pkcs8:
    #                 # return self._export_pkcs8(passphrase=passphrase, **args)
    #                 fail('ValueError: pkcs8 is not supported currently')
    #             else:
    #                 return self._export_private_der()
    #         else:
    #             return fail("ValueError: Private keys cannot be exported in OpenSSH format")
    #     else:  # Public key
    #         if args:
    #             return fail("ValueError: Unexpected parameters: '%s'", args)
    #         if ext_format == "PEM":
    #             return self._export_public_pem(compress)
    #         elif ext_format == "DER":
    #             return self._export_subjectPublicKeyInfo(compress)
    #         else:
    #             return self._export_openssh(compress)
    # self.export_key = export_key
    #
    # return self

def generate(**kwargs):
    """Generate a new private key on the given curve.
    Args:
      curve (string):
        Mandatory. It must be a curve name defined in :numref:`curve_names`.
      randfunc (callable):
        Optional. The RNG to read randomness from.
        If ``None``, :func:`Crypto.Random.get_random_bytes` is used.
    """

    # curve_name = kwargs.pop("curve")
    # curve = _curves[curve_name]
    # randfunc = kwargs.pop("randfunc", Random.get_random_bytes)
    # if kwargs:
    #     fail('Error("TypeError: Unknown parameters: %s")'% str(kwargs))
    #
    # # d = Integer.random_range(min_inclusive=1,
    # d = getRandomRange(min_inclusive=1,
    #                          max_exclusive=curve.order,
    #                          randfunc=randfunc)
    #
    # return EccKey(curve=curve_name, d=d)


def construct(**kwargs):
    """Build a new ECC key (private or public) starting
    from some base components.
    Args:
      curve (string):
        Mandatory. It must be a curve name defined in :numref:`curve_names`.
      d (integer):
        Only for a private key. It must be in the range ``[1..order-1]``.
      point_x (integer):
        Mandatory for a public key. X coordinate (affine) of the ECC point.
      point_y (integer):
        Mandatory for a public key. Y coordinate (affine) of the ECC point.
    Returns:
      :class:`EccKey` : a new ECC key object
    """
    #
    # curve_name = kwargs["curve"]
    # curve = _curves[curve_name]
    # point_x = kwargs.pop("point_x", None)
    # point_y = kwargs.pop("point_y", None)
    #
    # if "point" in kwargs:
    #     return Error("TypeError: Unknown keyword: point")
    #
    # if None not in (point_x, point_y):
    #     # ValueError is raised if the point is not on the curve
    #     kwargs["point"] = EccPoint(point_x, point_y, curve_name)
    #
    # # Validate that the private key matches the public one
    # d = kwargs.get("d", None)
    # if d != None and "point" in kwargs:
    #     pub_key = curve.G * d
    #     if pub_key.xy != (point_x, point_y):
    #         return Error("ValueError: Private and public ECC keys do not match")
    #
    # return EccKey(**kwargs)


def _import_public_der(curve_oid, ec_point):
    """Convert an encoded EC point into an EccKey object
    curve_name: string with the OID of the curve
    ec_point: byte string with the EC point (not DER encoded)
    """
    #
    # for curve_name, curve in _curves.items():
    #     if curve.oid == curve_oid:
    #         break
    # else:
    #     return Error("UnsupportedEccFeature: " + "Unsupported ECC curve (OID: %s)" % curve_oid)
    #
    # # See 2.2 in RFC5480 and 2.3.3 in SEC1
    # # The first byte is:
    # # - 0x02:   compressed, only X-coordinate, Y-coordinate is even
    # # - 0x03:   compressed, only X-coordinate, Y-coordinate is odd
    # # - 0x04:   uncompressed, X-coordinate is followed by Y-coordinate
    # #
    # # PAI is in theory encoded as 0x00.
    #
    # modulus_bytes = curve.p.size_in_bytes()
    # point_type = bord(ec_point[0])
    #
    # # Uncompressed point
    # if point_type == 0x04:
    #     if len(ec_point) != (1 + 2 * modulus_bytes):
    #         return Error("ValueError: Incorrect EC point length")
    #     x = Integer.from_bytes(ec_point[1:modulus_bytes+1])
    #     y = Integer.from_bytes(ec_point[modulus_bytes+1:])
    # # Compressed point
    # elif point_type in (0x02, 0x3):
    #     if len(ec_point) != (1 + modulus_bytes):
    #         return Error("ValueError: Incorrect EC point length")
    #     x = Integer.from_bytes(ec_point[1:])
    #     y = (pow(x, 3) - x*3 + curve.b).sqrt(curve.p)    # Short Weierstrass
    #     if point_type == 0x02 and y.is_odd():
    #         y = curve.p - y
    #     if point_type == 0x03 and y.is_even():
    #         y = curve.p - y
    # else:
    #     return Error("ValueError: Incorrect EC point encoding")
    #
    # return construct(curve=curve_name, point_x=x, point_y=y)



def _import_subjectPublicKeyInfo(encoded, *kwargs):
    """
    Convert a subjectPublicKeyInfo into an EccKey object
    """
    unrestricted_oid = "1.2.840.10045.2.1"
    ecdh_oid = "1.3.132.1.12"
    ecmqv_oid = "1.3.132.1.13"

    if oid not in (unrestricted_oid, ecdh_oid, ecmqv_oid):
        fail('UnsupportedEccFeature("Unsupported ECC purpose (OID: %s)")', oid)

    # Parameters are mandatory for all three types
    if not params:
        fail('ValueError("Missing ECC parameters")')

    # ECParameters ::= CHOICE {
    #   namedCurve         OBJECT IDENTIFIER
    #   -- implicitCurve   NULL
    #   -- specifiedCurve  SpecifiedECDomain
    # }
    #
    # implicitCurve and specifiedCurve are not supported (as per RFC)
    curve_oid = DerObjectId().decode(params).value
    return _import_public_der(curve_oid, ec_point)

def _import_private_der(encoded, passphrase, curve_oid=None):
    """
     See RFC5915 https://tools.ietf.org/html/rfc5915

     ECPrivateKey ::= SEQUENCE {
               version        INTEGER { ecPrivkeyVer1(1) } (ecPrivkeyVer1),
               privateKey     OCTET STRING,
               parameters [0] ECParameters {{ NamedCurve }} OPTIONAL,
               publicKey  [1] BIT STRING OPTIONAL
        }


    """

def _import_pkcs8(encoded, passphrase):
    """
     From RFC5915, Section 1:

     Distributing an EC private key with PKCS#8 [RFC5208] involves including:
     a) id-ecPublicKey, id-ecDH, or id-ecMQV (from [RFC5480]) with the
        namedCurve as the parameters in the privateKeyAlgorithm field; and
     b) ECPrivateKey in the PrivateKey field, which is an OCTET STRING.


    """

def _import_x509_cert(encoded, *kwargs):
    """
    Not an ECC DER key
    """
    sp_info = _extract_subject_public_key_info(encoded)
    return _import_subjectPublicKeyInfo(sp_info)

def _import_der(encoded, passphrase):
    return _import_x509_cert(encoded, passphrase)
    # try:
    #     return _import_subjectPublicKeyInfo(encoded, passphrase)
    # except UnsupportedEccFeature as err:
    #     raise err
    # except (ValueError, TypeError, IndexError):
    #     pass

    # try:
        # return _import_x509_cert(encoded, passphrase)
    # except UnsupportedEccFeature as err:
    #     raise err
    # except (ValueError, TypeError, IndexError):
    #     pass

    # try:
    #     return _import_private_der(encoded, passphrase)
    # except UnsupportedEccFeature as err:
    #     raise err
    # except (ValueError, TypeError, IndexError):
    #     pass

    # try:
    #     return _import_pkcs8(encoded, passphrase)
    # except UnsupportedEccFeature as err:
    #     raise err
    # except (ValueError, TypeError, IndexError):
    #     pass

    # raise ValueError("Not an ECC DER key")

def _import_openssh_public(encoded):
    """
    b' '
    """

def _import_openssh_private_ecc(data, password):
    """
    Unsupported ECC curve %s
    """

def import_key(encoded, passphrase=None):
    """Import an ECC key (public or private).
    Args:
      encoded (bytes or multi-line string):
        The ECC key to import.
        An ECC **public** key can be:
        - An X.509 certificate, binary (DER) or ASCII (PEM)
        - An X.509 ``subjectPublicKeyInfo``, binary (DER) or ASCII (PEM)
        - An OpenSSH line (e.g. the content of ``~/.ssh/id_ecdsa``, ASCII)
        An ECC **private** key can be:
        - In binary format (DER, see section 3 of `RFC5915`_ or `PKCS#8`_)
        - In ASCII format (PEM or `OpenSSH 6.5+`_)
        Private keys can be in the clear or password-protected.
        For details about the PEM encoding, see `RFC1421`_/`RFC1423`_.
      passphrase (byte string):
        The passphrase to use for decrypting a private key.
        Encryption may be applied protected at the PEM level or at the PKCS#8 level.
        This parameter is ignored if the key in input is not encrypted.
    Returns:
      :class:`EccKey` : a new ECC key object
    Raises:
      ValueError: when the given key cannot be parsed (possibly because
        the pass phrase is wrong).
    .. _RFC1421: http://www.ietf.org/rfc/rfc1421.txt
    .. _RFC1423: http://www.ietf.org/rfc/rfc1423.txt
    .. _RFC5915: http://www.ietf.org/rfc/rfc5915.txt
    .. _`PKCS#8`: http://www.ietf.org/rfc/rfc5208.txt
    .. _`OpenSSH 6.5+`: https://flak.tedunangst.com/post/new-openssh-key-format-and-bcrypt-pbkdf
    """
    # encoded = tobytes(encoded)
    # if passphrase != None:
    #     passphrase = tobytes(passphrase)
    #
    # # PEM
    # if encoded.startswith(b'-----BEGIN OPENSSH PRIVATE KEY'):
    #     text_encoded = tostr(encoded)
    #     openssh_encoded, marker, enc_flag = codecs.decode(PEM, encoding=text_encoded)
    #     result = _import_openssh_private_ecc(openssh_encoded, passphrase)
    #     return result
    #
    # elif encoded.startswith(b'-----'):
    #
    #     text_encoded = tostr(encoded)
    #
    #     # Remove any EC PARAMETERS section
    #     # Ignore its content because the curve type must be already given in the key
    #     ecparams_start = "-----BEGIN EC PARAMETERS-----"
    #     ecparams_end = "-----END EC PARAMETERS-----"
    #     text_encoded = re.sub(ecparams_start + ".*?" + ecparams_end, "",
    #                             text_encoded,
    #                             flags=re.DOTALL)
    #
    #     der_encoded, marker, enc_flag = codecs.decode(PEM, encoding=text_encoded)
    #     if enc_flag:
    #         passphrase = None
    #     result = _import_der(der_encoded, passphrase)
    #     # result = _import_x509_cert(der_encoded, passphrase)
    #     return result
    #     # try:
    #     #     result = _import_der(der_encoded, passphrase)
    #     # except UnsupportedEccFeature as uef:
    #     #     # PY2LARKY: pay attention to this!
    #     #     return uef
    #     # except ValueError:
    #     #     return Error("ValueError: Invalid DER encoding inside the PEM file")
    #
    # # OpenSSH
    # if encoded.startswith(b'ecdsa-sha2-'):
    #     return _import_openssh_public(encoded)
    #
    # # DER
    # if len(encoded) > 0 and bord(encoded[0]) == 0x30:
    #     return _import_der(encoded, passphrase)
    #
    # return Error("ValueError: ECC key format is not supported")
    #


ECC = larky.struct(
    generate=generate,
    import_key=import_key,
)