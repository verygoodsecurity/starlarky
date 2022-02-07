
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//larky", larky="larky")
load("@vendor//asserts", "asserts")
load("@vendor//ecdsa/util", string_to_number="string_to_number")

# @python_2_unicode_compatible
def CurveFp(p, a, b, h=None):
    """Short Weierstrass Elliptic Curve over a prime field."""

    # if GMPY:  # pragma: no branch

    #     def __init__(self, p, a, b, h=None):
    #         """
    #         The curve of points satisfying y^2 = x^3 + a*x + b (mod p).
    #         h is an integer that is the cofactor of the elliptic curve domain
    #         parameters; it is the number of points satisfying the elliptic
    #         curve equation divided by the order of the base point. It is used
    #         for selection of efficient algorithm for public point verification.
    #         """
    #         self.__p = mpz(p)
    #         self.__a = mpz(a)
    #         self.__b = mpz(b)
    #         # h is not used in calculations and it can be None, so don't use
    #         # gmpy with it
    #         self.__h = h

    # else:  # pragma: no branch
    self = larky.mustablestruct(__name__="CurveFp", __class__=CurveFp)
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

    return self


def AbstractPoint():
    """Class for common methods of elliptic curve points."""

    self = larky.mustablestruct(__class__=AbstractPoint, __name__="AbstractPoint")
    # @staticmethod
    def _from_raw_encoding(data, raw_encoding_length):
        """
        Decode public point from :term:`raw encoding`.
        :term:`raw encoding` is the same as the :term:`uncompressed` encoding,
        but without the 0x04 byte at the beginning.
        """
        # real assert, from_bytes() should not call us with different length
        asserts.eq(len(data), raw_encoding_length)
        
        xs = data[: raw_encoding_length // 2]
        ys = data[raw_encoding_length // 2 :]
        # real assert, raw_encoding_length is calculated by multiplying an
        # integer by two so it will always be even
        asserts.eq(len(xs), raw_encoding_length // 2)
        asserts.eq(len(ys), raw_encoding_length // 2)
        coord_x = string_to_number(xs)
        coord_y = string_to_number(ys)

        return coord_x, coord_y
    self._from_raw_encoding=_from_raw_encoding

    return self

def PointJacobi(curve, x, y, z, order=None, generator=False):
    """
    Point on a short Weierstrass elliptic curve. Uses Jacobi coordinates.
    In Jacobian coordinates, there are three parameters, X, Y and Z.
    They correspond to affine parameters 'x' and 'y' like so:
    x = X / Z²
    y = Y / Z³
    """

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
        # super(PointJacobi, self).__init__()
        self = AbstractPoint()
        self.__name__ = "PointJacobi"
        self.__class__ = PointJacobi
        self.__curve = curve
        # if GMPY:  # pragma: no branch
        #     self.__coords = (mpz(x), mpz(y), mpz(z))
        #     self.__order = order and mpz(order)
        # else:  # pragma: no branch
        self.__coords = (x, y, z)
        self.__order = order
        self.__generator = generator
        self.__precompute = []
        return self
    self = __init__(curve, x, y, z, order, generator)

    return self


ellpticcurve=larky.struct(
    AbstractPoint=AbstractPoint,
    PointJacobi=PointJacobi
)