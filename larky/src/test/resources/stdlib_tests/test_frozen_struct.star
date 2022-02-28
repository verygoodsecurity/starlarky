load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//larky", larky="larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION")
load("@vendor//asserts", "asserts")
load("@vendor//ecdsa/numbertheory", "numbertheory")
load("@vendor//ecdsa/util", string_to_number="string_to_number")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")


def CurveFp(p, a, b, h=None):
    """Short Weierstrass Elliptic Curve over a prime field."""

    self = larky.mutablestruct(__name__="CurveFp", __class__=CurveFp)
    def __init__(p, a, b, h=None):
        """
        The curve of points satisfying y^2 = x^3 + a*x + b (mod p).

        h is an integer that is the cofactor of the elliptic curve domain
        parameters; it is the number of points satisfying the elliptic
        curve equation divided by the order of the base point. It is used
        for selection of efficient algorithm for public point verification.
        """
        self.__p = p
        self.__a = a
        self.__b = b
        self.__h = h
        return self
    self = __init__(p, a, b, h)

    def p():
        return self.__p
    self.p = p

    def a():
        return self.__a
    self.a = a

    def b():
        return self.__b
    self.b = b

    def cofactor():
        return self.__h
    self.cofactor = cofactor

    return self

def PointJacobi(curve, x, y, z, order=None, generator=False):
    """
    Point on a short Weierstrass elliptic curve. Uses Jacobi coordinates.
    In Jacobian coordinates, there are three parameters, X, Y and Z.
    They correspond to affine parameters 'x' and 'y' like so:
    x = X / Z²
    y = Y / Z³
    """
    self = larky.mutablestruct(__name__ = "PointJacobi", __class__=PointJacobi)
    def __init__(curve, x, y, z, order=None, generator=False):
        """
        Initialise a point that uses Jacobi representation internally.
        :param CurveFp curve: curve on which the point resides
        :param int x: the X parameter of Jacobi representation (equal to x when
          converting from affine coordinates
        :param int y: the Y parameter of Jacobi representation (equal to y when
          converting from affine coordinates
        :param int z: the Z parameter of Jacobi representation (equal to 1 when
          converting from affine coordinates
        :param int order: the point order, must be non zero when using
          generator=True
        :param bool generator: the point provided is a curve generator, as
          such, it will be commonly used with scalar multiplication. This will
          cause to precompute multiplication table generation for it
        """
        self.__curve = curve
        self.__coords = (x, y, z)
        self.__order = order
        self.__generator = generator
        self.__precompute = []
        return self
    self = __init__(curve, x, y, z, order, generator)

    def _maybe_precompute():
        if not self.__generator or self.__precompute:
            return
        # since this code will execute just once, and it's fully deterministic,
        # depend on atomicity of the last assignment to switch from empty
        # self.__precompute to filled one and just ignore the unlikely
        # situation when two threads execute it at the same time (as it won't
        # lead to inconsistent __precompute)
        order = self.__order
        order *= 2
        coord_x, coord_y, coord_z = self.__coords
        print('below would trigger frozen struct')
        doubler = PointJacobi(self.__curve, coord_x, coord_y, coord_z, order)
    self._maybe_precompute = _maybe_precompute

    def __mul__(other):
        """Multiply point by an integer."""
        if other == 1:
            return self
        if self.__order:
            # order*2 as a protection for Minerva
            other = other % (self.__order * 2)
        self._maybe_precompute()
    self.__mul__ = __mul__

    return self


# Certicom secp256-k1
_a = 0x0000000000000000000000000000000000000000000000000000000000000000
_b = 0x0000000000000000000000000000000000000000000000000000000000000007
_p = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F
_Gx = 0x79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798
_Gy = 0x483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8
_r = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141

curve_secp256k1 = CurveFp(_p, _a, _b, 1)
generator_secp256k1 = PointJacobi(
    curve_secp256k1, _Gx, _Gy, 1, _r, generator=True
)

dss = larky.struct(
    curve_secp256k1=curve_secp256k1,
    generator_secp256k1=generator_secp256k1,
)

def Curve(name=None, curve=None, generator=None, oid=None, openssl_name=None):

    self = larky.mutablestruct(__class__=Curve, __name__="Curve")
    
    def __init__(name, curve, generator, oid, openssl_name=None):
        self.name = name
        self.openssl_name = openssl_name  # maybe None
        self.curve = curve
        self.generator = generator
        self.oid = oid

        return self
    self = __init__(name, curve, generator, oid, openssl_name)
    
    return self

SECP256k1 = Curve(
    "SECP256k1",
    dss.curve_secp256k1,
    dss.generator_secp256k1,
    (1, 3, 132, 0, 10),
    "secp256k1",
)


def SigningKey(_error__please_use_generate=None):
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

    def from_secret_exponent(secexp, curve):
        self = __init__(True)
        self.curve = curve
        # Frozen struct err occurs
        pubkey_point = curve.generator * secexp
        return self
    self.from_secret_exponent = from_secret_exponent

    def from_string(string, curve):
        secexp = 3
        return self.from_secret_exponent(secexp, curve)
    self.from_string = from_string

    def from_der(string):
        privkey_str = 'some string'
        return self.from_string(privkey_str, SECP256k1)
    self.from_der = from_der

    return self


def _test_frozen_struct():
    print('test frozen struct')
    # pubkey_point = SECP256k1.generator * 3
    sk = SigningKey(True).from_der('key bytes')

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_frozen_struct))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())