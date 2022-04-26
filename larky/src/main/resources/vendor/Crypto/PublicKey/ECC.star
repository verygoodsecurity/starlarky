# ===================================================================
#
# Copyright (c) 2015, Legrandin <helderijs@gmail.com>
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
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//collections", namedtuple="namedtuple")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//re", re="re")
load("@stdlib//struct", struct="struct")
load("@vendor//Crypto/IO", PEM="PEM")
load("@vendor//Crypto/IO", PKCS8="PKCS8")
load("@vendor//Crypto/Math/Numbers", Integer="Integer")
load("@vendor//Crypto/PublicKey", _expand_subject_public_key_info="expand_subject_public_key_info", _create_subject_public_key_info="create_subject_public_key_info", _extract_subject_public_key_info="extract_subject_public_key_info")
load("@vendor//Crypto/PublicKey/_openssh", import_openssh_private_generic="import_openssh_private_generic", read_bytes="read_bytes", read_string="read_string", check_padding="check_padding")
load("@vendor//Crypto/Random", get_random_bytes="get_random_bytes")
load("@vendor//Crypto/Random/random", getrandbits="getrandbits")
load("@vendor//Crypto/Util/asn1", DerObjectId="DerObjectId", DerOctetString="DerOctetString", DerSequence="DerSequence", DerBitString="DerBitString")
load("@vendor//Crypto/Util/number", bytes_to_long="bytes_to_long", long_to_bytes="long_to_bytes")
load("@vendor//Crypto/Util/py3compat", bord="bord", tobytes="tobytes", tostr="tostr", bchr="bchr", is_string="is_string")
load("@vendor//option/result", Error="Error", Ok="Ok", Result="Result")

load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@stdlib//binascii", hexlify="hexlify", unhexlify="unhexlify")

#
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
_Curve = namedtuple("_Curve", "p b order Gx Gy G modulus_bits oid context desc openssh")
_curves = {}


p256_names = ["p256", "NIST P-256", "P-256", "prime256v1", "secp256r1",
              "nistp256"]


def _init_p256():
    p = 0xffffffff00000001000000000000000000000000ffffffffffffffffffffffff # bc <-Q
    b = 0x5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b # bc <- b
    order = 0xffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551 # n
    Gx = 0x6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296
    Gy = 0x4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5

    p256_modulus = long_to_bytes(p, 32)
    p256_b = long_to_bytes(b, 32)
    p256_order = long_to_bytes(order, 32)
#
#     ec_p256_context = VoidPointer()
#     result = _ec_lib.ec_ws_new_context(ec_p256_context.address_of(),
#                                        c_uint8_ptr(p256_modulus),
#                                        c_uint8_ptr(p256_b),
#                                        c_uint8_ptr(p256_order),
#                                        c_size_t(len(p256_modulus)),
#                                        c_ulonglong(getrandbits(64))
#                                        )
#     if result:
#         return Error("ImportError: " + "Error %d initializing P-256 context" % result)
#
#     context = SmartPointer(ec_p256_context.get(), _ec_lib.ec_free_context)
    context = _JCrypto.PublicKey.ECC.P256R1Curve()
    p256 = _Curve(Integer(p),
                  Integer(b),
                  Integer(order),
                  Integer(Gx),
                  Integer(Gy),
                  None,
                  256,
                  "1.2.840.10045.3.1.7",    # ANSI X9.62
                  context,
                  "NIST P-256",
                  "ecdsa-sha2-nistp256")

    _curves.update({k: p256 for k in p256_names})


_init_p256()


p384_names = ["p384", "NIST P-384", "P-384", "prime384v1", "secp384r1",
              "nistp384"]


def _init_p384():
    p = 0xfffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffeffffffff0000000000000000ffffffff
    b = 0xb3312fa7e23ee7e4988e056be3f82d19181d9c6efe8141120314088f5013875ac656398d8a2ed19d2a85c8edd3ec2aef
    order = 0xffffffffffffffffffffffffffffffffffffffffffffffffc7634d81f4372ddf581a0db248b0a77aecec196accc52973
    Gx = 0xaa87ca22be8b05378eb1c71ef320ad746e1d3b628ba79b9859f741e082542a385502f25dbf55296c3a545e3872760aB7
    Gy = 0x3617de4a96262c6f5d9e98bf9292dc29f8f41dbd289a147ce9da3113b5f0b8c00a60b1ce1d7e819d7a431d7c90ea0e5F

    p384_modulus = long_to_bytes(p, 48)
    p384_b = long_to_bytes(b, 48)
    p384_order = long_to_bytes(order, 48)

    # ec_p384_context = VoidPointer()
    # result = _ec_lib.ec_ws_new_context(ec_p384_context.address_of(),
    #                                    c_uint8_ptr(p384_modulus),
    #                                    c_uint8_ptr(p384_b),
    #                                    c_uint8_ptr(p384_order),
    #                                    c_size_t(len(p384_modulus)),
    #                                    c_ulonglong(getrandbits(64))
    #                                    )
    # if result:
    #     return Error("ImportError: " + "Error %d initializing P-384 context" % result)

    # context = SmartPointer(ec_p384_context.get(), _ec_lib.ec_free_context)
    context = _JCrypto.PublicKey.ECC.P384R1Curve()
    p384 = _Curve(Integer(p),
                  Integer(b),
                  Integer(order),
                  Integer(Gx),
                  Integer(Gy),
                  None,
                  384,
                  "1.3.132.0.34",   # SEC 2
                  context,
                  "NIST P-384",
                  "ecdsa-sha2-nistp384")
    _curves.update({k: p384 for k in p384_names})


_init_p384()


p521_names = ["p521", "NIST P-521", "P-521", "prime521v1", "secp521r1",
              "nistp521"]


def _init_p521():
    p = 0x000001ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
    b = 0x00000051953eb9618e1c9a1f929a21a0b68540eea2da725b99b315f3b8b489918ef109e156193951ec7e937b1652c0bd3bb1bf073573df883d2c34f1ef451fd46b503f00
    order = 0x000001fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffa51868783bf2f966b7fcc0148f709a5d03bb5c9b8899c47aebb6fb71e91386409
    Gx = 0x000000c6858e06b70404e9cd9e3ecb662395b4429c648139053fb521f828af606b4d3dbaa14b5e77efe75928fe1dc127a2ffa8de3348b3c1856a429bf97e7e31c2e5bd66
    Gy = 0x0000011839296a789a3bc0045c8a5fb42c7d1bd998f54449579b446817afbd17273e662c97ee72995ef42640c550b9013fad0761353c7086a272c24088be94769fd16650

    p521_modulus = long_to_bytes(p, 66)
    p521_b = long_to_bytes(b, 66)
    p521_order = long_to_bytes(order, 66)

    # ec_p521_context = VoidPointer()
    # result = _ec_lib.ec_ws_new_context(ec_p521_context.address_of(),
    #                                    c_uint8_ptr(p521_modulus),
    #                                    c_uint8_ptr(p521_b),
    #                                    c_uint8_ptr(p521_order),
    #                                    c_size_t(len(p521_modulus)),
    #                                    c_ulonglong(getrandbits(64))
    #                                    )
    # if result:
    #     return Error("ImportError: " + "Error %d initializing P-521 context" % result)
    #
    # context = SmartPointer(ec_p521_context.get(), _ec_lib.ec_free_context)
    # context = larky.struct(__name__="TODO")
    context = _JCrypto.PublicKey.ECC.P521R1Curve()
    p521 = _Curve(Integer(p),
                  Integer(b),
                  Integer(order),
                  Integer(Gx),
                  Integer(Gy),
                  None,
                  521,
                  "1.3.132.0.35",   # SEC 2
                  context,
                  "NIST P-521",
                  "ecdsa-sha2-nistp521")

    _curves.update({k: p521 for k in p521_names})

_init_p521()


def UnsupportedEccFeature(msg):
    return Error("UnsupportedEccFeature: " + msg)


def EccPoint(x, y, curve="p256"):
    """A class to abstract a point over an Elliptic Curve.

    The class support special methods for:

    * Adding two points: ``R = S + T``
    * In-place addition: ``S += T``
    * Negating a point: ``R = -T``
    * Comparing two points: ``if S == T: ...``
    * Multiplying a point by a scalar: ``R = S*k``
    * In-place multiplication by a scalar: ``T *= k``

    :ivar x: The affine X-coordinate of the ECC point
    :vartype x: integer

    :ivar y: The affine Y-coordinate of the ECC point
    :vartype y: integer

    :ivar xy: The tuple with X- and Y- coordinates
    """
    self = larky.mutablestruct(__name__='EccPoint', __class__=EccPoint)

    def set(point):
        self._point = point._point
        return self
    self.set = set

    def __eq__(point):
        return self._point == point._point

    self.__eq__ = __eq__

    def __neg__():
        np = self.copy()
        np._point.negate()
        return np
    self.__neg__ = __neg__

    def copy():
        """Return a copy of this point."""
        x, y = self.xy
        np = EccPoint(x, y, self._curve_name)
        return np
    self.copy = copy

    def is_point_at_infinity():
        """``True`` if this is the point-at-infinity."""
        return self._point.is_infinity()
    self.is_point_at_infinity = is_point_at_infinity

    def point_at_infinity():
        """Return the point-at-infinity for the curve this point is on."""
        return EccPoint(0, 0, self._curve_name)
    self.point_at_infinity = point_at_infinity

    def _x():
        return self.xy[0]
    self.x = larky.property(_x)

    def _y():
        return self.xy[1]
    self.y = larky.property(_y)

    def _xy():
        xb, yb = self._point.as_tuple()
        return Integer(xb), Integer(yb)
    self.xy = larky.property(_xy)

    def size_in_bytes():
        """Size of each coordinate, in bytes."""
        return (self.size_in_bits() + 7) // 8
    self.size_in_bytes = size_in_bytes

    def size_in_bits():
        """Size of each coordinate, in bits."""
        return self._curve.modulus_bits
    self.size_in_bits = size_in_bits

    def double():
        """Double this point (in-place operation).

        :Return:
            :class:`EccPoint` : this same object (to enable chaining)
        """
        self._point.twice()
        return self
    self.double = double

    def __iadd__(point):
        """Add a second point to this one"""
        self._point.add(point._point)
        return self
    self.__iadd__ = __iadd__

    def __add__(point):
        """Return a new point, the addition of this one and another"""
        np = self.copy()
        np.__iadd__(point)
        return np
    self.__add__ = __add__

    def __imul__(scalar):
        """Multiply this point by a scalar"""
        if operator.lt(scalar, 0):
            return Error("ValueError: Scalar multiplication is only defined for non-negative integers").unwrap()
        self._point.multiply(operator.index(scalar))
        return self
    self.__imul__ = __imul__

    def __mul__(scalar):
        """Return a new point, the scalar product of this one"""
        np = self.copy()
        np.__imul__(scalar)
        return np
    self.__mul__ = __mul__

    def __rmul__(left_hand):
        return self.__mul__(left_hand)
    self.__rmul__ = __rmul__

    def __init__(x, y, curve):
        if curve not in _curves:
            return Error("ValueError: Unknown curve name %s" % curve).unwrap()

        self._curve = _curves[curve]
        self._curve_name = curve

        modulus_bytes = self.size_in_bytes()
        context = self._curve.context
        xb = long_to_bytes(x, modulus_bytes)
        yb = long_to_bytes(y, modulus_bytes)
        if len(xb) != modulus_bytes or len(yb) != modulus_bytes:
            return Error("ValueError: Incorrect coordinate length").unwrap()

        if operator.eq(x, 0) and operator.eq(y, 0):
            self._point = context.infinity()
        else:
            self._point = context.point(operator.index(x), operator.index(y))
        return self
    self = __init__(x, y, curve)
    return self


# Last piece of initialization
_p256_G = EccPoint(_curves['p256'].Gx, _curves['p256'].Gy, "p256")
_p256 = _curves['p256']._replace(G=_p256_G)
_curves.update({key: _p256 for key in p256_names})
# del p256_G, p256, p256_names

_p384_G = EccPoint(_curves['p384'].Gx, _curves['p384'].Gy, "p384")
_p384 = _curves['p384']._replace(G=_p384_G)
_curves.update({key: _p384 for key in p384_names})
# del p384_G, p384, p384_names

_p521_G = EccPoint(_curves['p521'].Gx, _curves['p521'].Gy, "p521")
_p521 = _curves['p521']._replace(G=_p521_G)
_curves.update({key: _p521 for key in p521_names})
# del p521_G, p521, p521_names

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

    def __init__(kwargs):
        """Create a new ECC key

        Keywords:
          curve : string
            It must be *"p256"*, *"P-256"*, *"prime256v1"* or *"secp256r1"*.
          d : integer
            Only for a private key. It must be in the range ``[1..order-1]``.
          point : EccPoint
            Mandatory for a public key. If provided for a private key,
            the implementation will NOT check whether it matches ``d``.
        """

        kwargs_ = dict(kwargs)
        curve_name = kwargs_.pop("curve", None)
        self._d = kwargs_.pop("d", None)
        self._point = kwargs_.pop("point", None)
        if kwargs_:
            return Error("TypeError: Unknown parameters: " + str(kwargs_)).unwrap()

        if curve_name not in _curves:
            return Error("ValueError: Unsupported curve (%s)" % curve_name).unwrap()
        self._curve = _curves[curve_name]

        if self._d == None:
            if self._point == None:
                return Error("ValueError: Either private or public ECC component must be specified, not both").unwrap()
        else:
            self._d = Integer(self._d)
            if not (operator.le(1, self._d) and operator.lt(self._d, self._curve.order)):
                return Error("ValueError: Invalid ECC private component").unwrap()

        self.curve = self._curve.desc
        return self
    self = __init__(kwargs)

    def __eq__(other):
        if other.has_private() != self.has_private():
            return False

        return other.pointQ == self.pointQ
    self.__eq__ = __eq__

    def __repr__():
        if self.has_private():
            extra = ", d=%d" % operator.index(self._d)
        else:
            extra = ""
        x, y = self.pointQ.xy
        return "EccKey(curve='%s', point_x=%d, point_y=%d%s)" % (self._curve.desc, operator.index(x), operator.index(y), extra)
    self.__repr__ = __repr__

    def has_private():
        """``True`` if this key can be used for making signatures or decrypting data."""
        return self._d != None
    self.has_private = has_private

    def _sign(z, k):
        if not ((0 < k) and (k < self._curve.order)):
            fail("assert (0 < k) and (k < self._curve.order) failed!")

        order = self._curve.order
        blind = Integer.random_range(min_inclusive=1,
                                     max_exclusive=order)

        blind_d = self._d * blind
        inv_blind_k = (blind * k).inverse(order)

        r = (self._curve.G * k).x % order
        s = inv_blind_k * (blind * z + blind_d * r) % order
        return (r, s)
    self._sign = _sign

    def _verify(z, rs):
        order = self._curve.order
        sinv = rs[1].inverse(order)
        point1 = self._curve.G * ((sinv * z) % order)
        point2 = self.pointQ * ((sinv * rs[0]) % order)
        return (point1 + point2).x == rs[0]
    self._verify = _verify

    def __d():
        if not self.has_private():
            return Error("ValueError: This is not a private ECC key").unwrap()
        return self._d
    self.d = larky.property(__d)

    def _pointQ():
        if self._point == None:
            # print("_pointQ")
            # print(self._curve.G, type(self._curve.G))
            # print(self._d, type(self._d))
            # print("END_pointQ")
            self._point = operator.mul(self._curve.G, self._d)
        return self._point
    self.pointQ = larky.property(_pointQ)

    def public_key():
        """A matching ECC public key.

        Returns:
            a new :class:`EccKey` object
        """

        return EccKey(curve=self._curve.desc, point=self.pointQ)
    self.public_key = public_key

    def _export_subjectPublicKeyInfo(compress):

        # See 2.2 in RFC5480 and 2.3.3 in SEC1
        # The first byte is:
        # - 0x02:   compressed, only X-coordinate, Y-coordinate is even
        # - 0x03:   compressed, only X-coordinate, Y-coordinate is odd
        # - 0x04:   uncompressed, X-coordinate is followed by Y-coordinate
        #
        # PAI is in theory encoded as 0x00.

        modulus_bytes = self.pointQ.size_in_bytes()

        if compress:
            first_byte = 2 + self.pointQ.y.is_odd()
            public_key = (bchr(first_byte) +
                          self.pointQ.x.to_bytes(modulus_bytes))
        else:
            public_key = (b'\x04' +
                          self.pointQ.x.to_bytes(modulus_bytes) +
                          self.pointQ.y.to_bytes(modulus_bytes))

        unrestricted_oid = "1.2.840.10045.2.1"
        return _create_subject_public_key_info(unrestricted_oid,
                                               public_key,
                                               DerObjectId(self._curve.oid))
    self._export_subjectPublicKeyInfo = _export_subjectPublicKeyInfo

    def _export_private_der(include_ec_params=True):
        if not (self.has_private()):
            fail("assert self.has_private() failed!")

        # ECPrivateKey ::= SEQUENCE {
        #           version        INTEGER { ecPrivkeyVer1(1) } (ecPrivkeyVer1),
        #           privateKey     OCTET STRING,
        #           parameters [0] ECParameters {{ NamedCurve }} OPTIONAL,
        #           publicKey  [1] BIT STRING OPTIONAL
        #    }

        # Public key - uncompressed form
        modulus_bytes = self.pointQ.size_in_bytes()
        public_key = (b'\x04' +
                      self.pointQ.x.to_bytes(modulus_bytes) +
                      self.pointQ.y.to_bytes(modulus_bytes))

        seq = [1,
               DerOctetString(self.d.to_bytes(modulus_bytes)),
               DerObjectId(self._curve.oid, explicit=0),
               DerBitString(public_key, explicit=1)]

        if not include_ec_params:
            operator.delitem(seq, 2)

        return DerSequence(seq).encode()
    self._export_private_der = _export_private_der

    def _export_pkcs8(**kwargs):
        if kwargs.get('passphrase', None) != None and 'protection' not in kwargs:
            return Error("ValueError: At least the 'protection' parameter should be present").unwrap()

        unrestricted_oid = "1.2.840.10045.2.1"
        private_key = self._export_private_der(include_ec_params=False)
        result = PKCS8.wrap(private_key,
                            unrestricted_oid,
                            key_params=DerObjectId(self._curve.oid),
                            **kwargs)
        return result
    self._export_pkcs8 = _export_pkcs8

    def _export_public_pem(compress):
        encoded_der = self._export_subjectPublicKeyInfo(compress)
        return PEM.encode(encoded_der, "PUBLIC KEY")
    self._export_public_pem = _export_public_pem

    def _export_private_pem(passphrase, **kwargs):
        encoded_der = self._export_private_der()
        return PEM.encode(encoded_der, "EC PRIVATE KEY", passphrase, **kwargs)
    self._export_private_pem = _export_private_pem

    def _export_private_clear_pkcs8_in_clear_pem():
        encoded_der = self._export_pkcs8()
        return PEM.encode(encoded_der, "PRIVATE KEY")
    self._export_private_clear_pkcs8_in_clear_pem = _export_private_clear_pkcs8_in_clear_pem

    def _export_private_encrypted_pkcs8_in_clear_pem(passphrase, **kwargs):
        if not passphrase:
            fail("assert passphrase failed!")
        if 'protection' not in kwargs:
            return Error("ValueError: At least the 'protection' parameter should be present").unwrap()
        encoded_der = self._export_pkcs8(passphrase=passphrase, **kwargs)
        return PEM.encode(encoded_der, "ENCRYPTED PRIVATE KEY")
    self._export_private_encrypted_pkcs8_in_clear_pem = _export_private_encrypted_pkcs8_in_clear_pem

    def _export_openssh(compress):
        if self.has_private():
            return Error("ValueError: Cannot export OpenSSH private keys").unwrap()

        desc = self._curve.openssh
        modulus_bytes = self.pointQ.size_in_bytes()

        if compress:
            first_byte = 2 + self.pointQ.y.is_odd()
            public_key = (bchr(first_byte) +
                          self.pointQ.x.to_bytes(modulus_bytes))
        else:
            public_key = (b'\x04' +
                          self.pointQ.x.to_bytes(modulus_bytes) +
                          self.pointQ.y.to_bytes(modulus_bytes))

        middle = desc.split("-")[2]
        comps = (tobytes(desc), tobytes(middle), public_key)
        blob = b"".join([struct.pack(">I", len(x)) + x for x in comps])
        return desc + " " + tostr(binascii.b2a_base64(blob))
    self._export_openssh = _export_openssh

    def export_key(**kwargs):
        """Export this ECC key.

        Args:
          format (string):
            The format to use for encoding the key:

            - ``'DER'``. The key will be encoded in ASN.1 DER format (binary).
              For a public key, the ASN.1 ``subjectPublicKeyInfo`` structure
              defined in `RFC5480`_ will be used.
              For a private key, the ASN.1 ``ECPrivateKey`` structure defined
              in `RFC5915`_ is used instead (possibly within a PKCS#8 envelope,
              see the ``use_pkcs8`` flag below).
            - ``'PEM'``. The key will be encoded in a PEM_ envelope (ASCII).
            - ``'OpenSSH'``. The key will be encoded in the OpenSSH_ format
              (ASCII, public keys only).

          passphrase (byte string or string):
            The passphrase to use for protecting the private key.

          use_pkcs8 (boolean):
            Only relevant for private keys.

            If ``True`` (default and recommended), the `PKCS#8`_ representation
            will be used.

            If ``False``, the much weaker `PEM encryption`_ mechanism will be used.

          protection (string):
            When a private key is exported with password-protection
            and PKCS#8 (both ``DER`` and ``PEM`` formats), this parameter MUST be
            present and be a valid algorithm supported by :mod:`Crypto.IO.PKCS8`.
            It is recommended to use ``PBKDF2WithHMAC-SHA1AndAES128-CBC``.

          compress (boolean):
            If ``True``, a more compact representation of the public key
            with the X-coordinate only is used.

            If ``False`` (default), the full public key will be exported.

        .. warning::
            If you don't provide a passphrase, the private key will be
            exported in the clear!

        .. note::
            When exporting a private key with password-protection and `PKCS#8`_
            (both ``DER`` and ``PEM`` formats), any extra parameters
            to ``export_key()`` will be passed to :mod:`Crypto.IO.PKCS8`.

        .. _PEM:        http://www.ietf.org/rfc/rfc1421.txt
        .. _`PEM encryption`: http://www.ietf.org/rfc/rfc1423.txt
        .. _`PKCS#8`:   http://www.ietf.org/rfc/rfc5208.txt
        .. _OpenSSH:    http://www.openssh.com/txt/rfc5656.txt
        .. _RFC5480:    https://tools.ietf.org/html/rfc5480
        .. _RFC5915:    http://www.ietf.org/rfc/rfc5915.txt

        Returns:
            A multi-line string (for PEM and OpenSSH) or bytes (for DER) with the encoded key.
        """

        args = dict(**kwargs)
        ext_format = args.pop("format")
        if ext_format not in ("PEM", "DER", "OpenSSH"):
            return Error("ValueError: Unknown format '%s'" % ext_format).unwrap()

        compress = args.pop("compress", False)

        if self.has_private():
            passphrase = args.pop("passphrase", None)
            if is_string(passphrase):
                passphrase = tobytes(passphrase)
                if not passphrase:
                    return Error("ValueError: Empty passphrase").unwrap()
            use_pkcs8 = args.pop("use_pkcs8", True)
            if ext_format == "PEM":
                if use_pkcs8:
                    if passphrase:
                        return self._export_private_encrypted_pkcs8_in_clear_pem(passphrase, **args)
                    else:
                        return self._export_private_clear_pkcs8_in_clear_pem()
                else:
                    return self._export_private_pem(passphrase, **args)
            elif ext_format == "DER":
                # DER
                if passphrase and not use_pkcs8:
                    return Error("ValueError: Private keys can only be encrpyted with DER using PKCS#8").unwrap()
                if use_pkcs8:
                    return self._export_pkcs8(passphrase=passphrase, **args)
                else:
                    return self._export_private_der()
            else:
                return Error("ValueError: Private keys cannot be exported in OpenSSH format").unwrap()
        else:  # Public key
            if args:
                return Error("ValueError: " + "Unexpected parameters: '%s'" % args).unwrap()
            if ext_format == "PEM":
                return self._export_public_pem(compress)
            elif ext_format == "DER":
                return self._export_subjectPublicKeyInfo(compress)
            else:
                return self._export_openssh(compress)
    self.export_key = export_key
    return self


def generate(**kwargs):
    """Generate a new private key on the given curve.

    Args:

      curve (string):
        Mandatory. It must be a curve name defined in :numref:`curve_names`.

      randfunc (callable):
        Optional. The RNG to read randomness from.
        If ``None``, :func:`Crypto.Random.get_random_bytes` is used.
    """

    curve_name = kwargs.pop("curve")
    curve = _curves[curve_name]
    randfunc = kwargs.pop("randfunc", get_random_bytes)
    if kwargs:
        return Error("TypeError: Unknown parameters: " + str(kwargs)).unwrap()

    d = Integer.random_range(min_inclusive=1,
                             max_exclusive=curve.order,
                             randfunc=randfunc)

    return EccKey(curve=curve_name, d=d)


def _construct(**kwargs):
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

    curve_name = kwargs["curve"]
    curve = _curves[curve_name]
    point_x = kwargs.pop("point_x", None)
    point_y = kwargs.pop("point_y", None)

    if "point" in kwargs:
        return Error("TypeError: Unknown keyword: point")

    if None not in (point_x, point_y):
        # ValueError is raised if the point is not on the curve
        kwargs["point"] = EccPoint(point_x, point_y, curve_name)

    # Validate that the private key matches the public one
    d = kwargs.get("d", None)
    if d != None and "point" in kwargs:
        pub_key = operator.mul(curve.G, d)
        if operator.ne(pub_key.x, point_x) or operator.ne(pub_key.y, point_y):
            return Error("ValueError: Private and public ECC keys do not match")

    return Ok(EccKey(**kwargs))


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
    return _construct(**kwargs).unwrap()


def _import_public_der(curve_oid, ec_point):
    """Convert an encoded EC point into an EccKey object

    curve_name: string with the OID of the curve
    ec_point: byte string with the EC point (not DER encoded)

    """
    _larky_forelse_run_else = True
    for curve_name, curve in _curves.items():
        if curve.oid == curve_oid:
            _larky_forelse_run_else = False
            break

    if _larky_forelse_run_else:
        return UnsupportedEccFeature("Unsupported ECC curve (OID: %s)" % curve_oid)

    # See 2.2 in RFC5480 and 2.3.3 in SEC1
    # The first byte is:
    # - 0x02:   compressed, only X-coordinate, Y-coordinate is even
    # - 0x03:   compressed, only X-coordinate, Y-coordinate is odd
    # - 0x04:   uncompressed, X-coordinate is followed by Y-coordinate
    #
    # PAI is in theory encoded as 0x00.

    # noinspection PyUnboundLocalVariable
    modulus_bytes = curve.p.size_in_bytes()
    point_type = bord(ec_point[0])

    # Uncompressed point
    if point_type == 0x04:
        if len(ec_point) != (1 + 2 * modulus_bytes):
            return Error("ValueError: Incorrect EC point length").unwrap()
        x = Integer.from_bytes(ec_point[1:modulus_bytes+1])
        y = Integer.from_bytes(ec_point[modulus_bytes+1:])
    # Compressed point
    elif point_type in (0x02, 0x3):
        if len(ec_point) != (1 + modulus_bytes):
            return Error("ValueError: Incorrect EC point length").unwrap()
        x = Integer.from_bytes(ec_point[1:])
        y = (pow(x, 3) - x*3 + curve.b).sqrt(curve.p)    # Short Weierstrass
        if point_type == 0x02 and y.is_odd():
            y = curve.p - y
        if point_type == 0x03 and y.is_even():
            y = curve.p - y
    else:
        return Error("ValueError: Incorrect EC point encoding")

    # noinspection PyUnboundLocalVariable
    return _construct(curve=curve_name, point_x=x, point_y=y)


def _import_subjectPublicKeyInfo(encoded, *kwargs):
    """Convert a subjectPublicKeyInfo into an EccKey object"""

    # See RFC5480

    # Parse the generic subjectPublicKeyInfo structure
    oid, ec_point, params = _expand_subject_public_key_info(encoded)

    # ec_point must be an encoded OCTET STRING
    # params is encoded ECParameters

    # We accept id-ecPublicKey, id-ecDH, id-ecMQV without making any
    # distiction for now.

    # Restrictions can be captured in the key usage certificate
    # extension
    unrestricted_oid = "1.2.840.10045.2.1"
    ecdh_oid = "1.3.132.1.12"
    ecmqv_oid = "1.3.132.1.13"

    if oid not in (unrestricted_oid, ecdh_oid, ecmqv_oid):
        return UnsupportedEccFeature("Unsupported ECC purpose (OID: %s)" % oid)

    # Parameters are mandatory for all three types
    if not params:
        return Error("ValueError: Missing ECC parameters")

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

    # See RFC5915 https://tools.ietf.org/html/rfc5915
    #
    # ECPrivateKey ::= SEQUENCE {
    #           version        INTEGER { ecPrivkeyVer1(1) } (ecPrivkeyVer1),
    #           privateKey     OCTET STRING,
    #           parameters [0] ECParameters {{ NamedCurve }} OPTIONAL,
    #           publicKey  [1] BIT STRING OPTIONAL
    #    }

    private_key = DerSequence().decode(encoded)
    if private_key[0] != 1:
        return Error("ValueError: Incorrect ECC private key version")

    # This might have been passed like "[0]1.3.132.0.35" instead of 
    # DER Encoded OID "1.3.132.0.35"
    if type(private_key[2]) == 'string':
        parameters = private_key[2].split(']')[-1]
    else:
        parameters = DerObjectId(explicit=0).decode(private_key[2]).value
    if curve_oid != None and parameters != curve_oid:
        return Error("ValueError: Curve mismatch")
    curve_oid = parameters

    if curve_oid == None:
        return Error("ValueError: No curve found")

    _larky_forelse_run_else = True
    for curve_name, curve in _curves.items():
        if curve.oid == curve_oid:
            _larky_forelse_run_else = False
            break
    if _larky_forelse_run_else:
        return UnsupportedEccFeature("Unsupported ECC curve (OID: %s)" % curve_oid)

    """
    If this was passed as a string instead of bytes, it was already parsed and 
    begins with a '#' followed by the hexlified string of the bytes
    """
    if type(private_key[1]) == 'string':
        scalar_bytes = unhexlify(''.join(private_key[1].split('#')))
    elif type(private_key[1]) == 'bytes':
        scalar_bytes = DerOctetString().decode(private_key[1]).payload
    modulus_bytes = curve.p.size_in_bytes()
    if len(scalar_bytes) != modulus_bytes:
        return Error("ValueError: Private key is too small")
    d = Integer.from_bytes(scalar_bytes)

    # Decode public key (if any)
    if len(private_key) == 4:
        if type(private_key[3]) == 'string':
            pk3 = unhexlify(private_key[3].split('#')[-1])
            public_key_enc = DerBitString().decode(pk3).value
        else:
            public_key_enc = DerBitString(explicit=1).decode(private_key[3]).value
        public_key = _import_public_der(curve_oid, public_key_enc)
        print(public_key)
        print(public_key.pointQ)
        #point_x = public_key.pointQ.x
        #point_y = public_key.pointQ.y
    else:
        point_x = None
        point_y = point_x

    return _construct(curve=curve_name, d=d, point_x=point_x, point_y=point_y)


def _import_pkcs8(encoded, passphrase):

    # From RFC5915, Section 1:
    #
    # Distributing an EC private key with PKCS#8 [RFC5208] involves including:
    # a) id-ecPublicKey, id-ecDH, or id-ecMQV (from [RFC5480]) with the
    #    namedCurve as the parameters in the privateKeyAlgorithm field; and
    # b) ECPrivateKey in the PrivateKey field, which is an OCTET STRING.

    algo_oid, private_key, params = PKCS8.unwrap(encoded, passphrase)

    # We accept id-ecPublicKey, id-ecDH, id-ecMQV without making any
    # distinction for now.
    unrestricted_oid = "1.2.840.10045.2.1"
    ecdh_oid = "1.3.132.1.12"
    ecmqv_oid = "1.3.132.1.13"

    if algo_oid not in (unrestricted_oid, ecdh_oid, ecmqv_oid):
        return UnsupportedEccFeature("Unsupported ECC purpose (OID: %s)" % algo_oid)

    curve_oid = DerObjectId().decode(params).value

    return _import_private_der(private_key, passphrase, curve_oid)


def _import_x509_cert(encoded, *kwargs):

    sp_info = _extract_subject_public_key_info(encoded)
    return _import_subjectPublicKeyInfo(sp_info)


def _import_der(encoded, passphrase):

    # try:
    #     return _import_subjectPublicKeyInfo(encoded, passphrase)
    # except UnsupportedEccFeature as err:
    #     raise err
    # except (ValueError, TypeError, IndexError):
    #     pass
    #
    #  ↕↕↕↕↕↕ LARKY MIGRATED ↕↕↕↕↕↕↕

    # THIS TRY BLOCK NEEDS TO BE REFACTORED

    """
    res = _import_subjectPublicKeyInfo(encoded, passphrase)
    if res.is_err:
        if Result.error_is("UnsupportedEccFeature", res) != None:
            res.unwrap()
    else:
        return res.unwrap()


    res = _import_x509_cert(encoded, passphrase)
    if res.is_err:
        if Result.error_is("UnsupportedEccFeature", res) != None:
            res.unwrap()
    else:
        return res.unwrap()

    res = _import_private_der(encoded, passphrase)
    if res.is_err:
        if Result.error_is("UnsupportedEccFeature", res) != None:
            res.unwrap()
    else:
        return res.unwrap()
    """
    res = _import_pkcs8(encoded, passphrase)
    if res.is_err:
        if Result.error_is("UnsupportedEccFeature", res) != None:
            res.unwrap()
    else:
        return res.unwrap()

    return Error("ValueError: Not an ECC DER key").unwrap()


def _import_openssh_public(encoded):
    keystring = binascii.a2b_base64(encoded.split(b' ')[1])

    keyparts = []
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if len(keystring) <= 4:
            break
        lk = struct.unpack(">I", keystring[:4])[0]
        keyparts.append(keystring[4:4 + lk])
        keystring = keystring[4 + lk:]

    _larky_forelse_run_else = True
    for curve_name, curve in _curves.items():
        middle = tobytes(curve.openssh.split("-")[2])
        if keyparts[1] == middle:
            _larky_forelse_run_else = False
            break
    if _larky_forelse_run_else:
        return Error("ValueError: Unsupported ECC curve")

    return _import_public_der(curve.oid, keyparts[2])


def _import_openssh_private_ecc(data, password):

    ssh_name, decrypted = import_openssh_private_generic(data, password)
    name, decrypted = read_string(decrypted)
    if name not in _curves:
        return UnsupportedEccFeature("Unsupported ECC curve %s" % name)
    curve = _curves[name]
    modulus_bytes = (curve.modulus_bits + 7) // 8

    public_key, decrypted = read_bytes(decrypted)

    if bord(public_key[0]) != 4:
        return Error("ValueError: Only uncompressed OpenSSH EC keys are supported")
    if len(public_key) != 2 * modulus_bytes + 1:
        return Error("ValueError: Incorrect public key length")

    point_x = Integer.from_bytes(public_key[1:1+modulus_bytes])
    point_y = Integer.from_bytes(public_key[1+modulus_bytes:])
    point = EccPoint(point_x, point_y, curve=name)

    private_key, decrypted = read_bytes(decrypted)
    d = Integer.from_bytes(private_key)

    _, padded = read_string(decrypted)  # Comment
    check_padding(padded)

    return Ok(EccKey(curve=name, d=d, point=point))


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
    encoded = tobytes(encoded)
    if passphrase != None:
        passphrase = tobytes(passphrase)

    # PEM
    if encoded.startswith(b'-----BEGIN OPENSSH PRIVATE KEY'):
        text_encoded = tostr(encoded)
        openssh_encoded, marker, enc_flag = PEM.decode(text_encoded)
        result = _import_openssh_private_ecc(openssh_encoded, passphrase).unwrap()
        return result

    elif encoded.startswith(b'-----'):

        text_encoded = tostr(encoded)

        # Remove any EC PARAMETERS section
        # Ignore its content because the curve type must be already given in
        # the key
        ecparams_start = "-----BEGIN EC PARAMETERS-----"
        ecparams_end = "-----END EC PARAMETERS-----"
        text_encoded = re.sub(ecparams_start + ".*?" + ecparams_end, "",
                                text_encoded,
                                flags=re.DOTALL)

        der_encoded, marker, enc_flag = PEM.decode(text_encoded)
        if enc_flag:
            passphrase = None
        result = _import_der(der_encoded, passphrase)
        if result.is_err:
            if Result.error_is("UnsupportedEccFeature", result) != None:
                result.unwrap()
            if Result.error_is("ValueError", result) != None:
                return Error("ValueError: Invalid DER encoding inside the PEM file").unwrap()
        else:
            return result.unwrap()

    # OpenSSH
    if encoded.startswith(b'ecdsa-sha2-'):
        return _import_openssh_public(encoded).unwrap()

    # DER
    if len(encoded) > 0 and bord(encoded[0]) == 0x30:
        return _import_der(encoded, passphrase).unwrap()

    return Error("ValueError: ECC key format is not supported").unwrap()


curves = _curves
ECC = larky.struct(
    construct=construct,
    generate=generate,
    import_key=import_key,
    curves=_curves,
    EccPoint=EccPoint,
    EccKey=EccKey,
)