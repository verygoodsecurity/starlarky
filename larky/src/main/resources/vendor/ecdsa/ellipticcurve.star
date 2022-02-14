
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//larky", larky="larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION")
load("@vendor//asserts", "asserts")
load("@vendor//ecdsa/numbertheory", "numbertheory")
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


def AbstractPoint():
    """Class for common methods of elliptic curve points."""

    self = larky.mutablestruct(__class__=AbstractPoint, __name__="AbstractPoint")
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

def Point(curve, x, y, order=None):
    """A point on a short Weierstrass elliptic curve. Altering x and y is
    forbidden, but they can be read by the x() and y() methods."""

    def __init__(self, curve, x, y, order=None):
        """curve, x, y, order; order (optional) is the order of this point."""
        # super(Point, self).__init__()
        self = AbstractPoint()
        self.__class__ = Point
        self.__name__ = "Point"
        self.__curve = curve
        self.__x = x
        self.__y = y
        self.__order = order
        # self.curve is allowed to be None only for INFINITY:
        # if self.__curve:
        #     assert self.__curve.contains_point(x, y)
        # for curves with cofactor 1, all points that are on the curve are
        # scalar multiples of the base point, so performing multiplication is
        # not necessary to verify that. See Section 3.2.2.1 of SEC 1 v2
        # if curve and curve.cofactor() != 1 and order:
        #     assert self * order == INFINITY
        return self
    self = __init__(curve, x, y, order)
  
    return self

INFINITY = Point(None, None, None)

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

    def _maybe_precompute():
        if not self.__generator or self.__precompute:
            return
        # since this code will execute just once, and it's fully deterministic,
        # depend on atomicity of the last assignment to switch from empty
        # self.__precompute to filled one and just ignore the unlikely
        # situation when two threads execute it at the same time (as it won't
        # lead to inconsistent __precompute)
        order = self.__order
        # assert order
        precompute = []
        i = 1
        order *= 2
        coord_x, coord_y, coord_z = self.__coords
        doubler = PointJacobi(self.__curve, coord_x, coord_y, coord_z, order)
        order *= 2
        precompute.append((doubler.x(), doubler.y()))

        for __while__ in range(WHILE_LOOP_EMULATION_ITERATION):
            if i >= order:
                break
            i *= 2
            doubler = doubler.double().scale()
            precompute.append((doubler.x(), doubler.y()))

        self.__precompute = precompute
    self._maybe_precompute = _maybe_precompute

    def x():
        """
        Return affine x coordinate.

        This method should be used only when the 'y' coordinate is not needed.
        It's computationally more efficient to use `to_affine()` and then
        call x() and y() on the returned instance. Or call `scale()`
        and then x() and y() on the returned instance.
        """
        x, _, z = self.__coords
        if z == 1:
            return x
        p = self.__curve.p()
        z = numbertheory.inverse_mod(z, p)
        # return x * z ** 2 % p
        return x * pow(z, 2) % p
    self.x = x

    def y():
        """
        Return affine y coordinate.

        This method should be used only when the 'x' coordinate is not needed.
        It's computationally more efficient to use `to_affine()` and then
        call x() and y() on the returned instance. Or call `scale()`
        and then x() and y() on the returned instance.
        """
        _, y, z = self.__coords
        if z == 1:
            return y
        p = self.__curve.p()
        z = numbertheory.inverse_mod(z, p)
        # return y * z ** 3 % p
        return y * pow(z, 3) % p
    self.x = x

    def order():
        """Return the order of the point.

        None if it is undefined.
        """
        return self.__order
    self.order = order

    def scale():
        """
        Return point scaled so that z == 1.

        Modifies point in place, returns self.
        """
        x, y, z = self.__coords
        if z == 1:
            return self

        # scaling is deterministic, so even if two threads execute the below
        # code at the same time, they will set __coords to the same value
        p = self.__curve.p()
        z_inv = numbertheory.inverse_mod(z, p)
        zz_inv = z_inv * z_inv % p
        x = x * zz_inv % p
        y = y * zz_inv * z_inv % p
        self.__coords = (x, y, 1)
        return self
    self.scale = scale

    def double():
        """Add a point to itself."""
        X1, Y1, Z1 = self.__coords

        if not Y1:
            return INFINITY

        p, a = self.__curve.p(), self.__curve.a()

        X3, Y3, Z3 = self._double(X1, Y1, Z1, p, a)

        if not Y3 or not Z3:
            return INFINITY
        return PointJacobi(self.__curve, X3, Y3, Z3, self.__order)
    self.double = double

    def _add_with_z_1(X1, Y1, X2, Y2, p):
        """add points when both Z1 and Z2 equal 1"""
        # after:
        # http://hyperelliptic.org/EFD/g1p/auto-shortw-jacobian.html#addition-mmadd-2007-bl
        H = X2 - X1
        HH = H * H
        I = 4 * HH % p
        J = H * I
        r = 2 * (Y2 - Y1)
        if not H and not r:
            return self._double_with_z_1(X1, Y1, p, self.__curve.a())
        V = X1 * I
        # X3 = (r ** 2 - J - 2 * V) % p
        X3 = (pow(r, 2) - J - 2 * V) % p
        Y3 = (r * (V - X3) - 2 * Y1 * J) % p
        Z3 = 2 * H % p
        return X3, Y3, Z3
    self._add_with_z_1 = _add_with_z_1

    def _add_with_z_eq(X1, Y1, Z1, X2, Y2, p):
        """add points when Z1 == Z2"""
        # after:
        # http://hyperelliptic.org/EFD/g1p/auto-shortw-jacobian.html#addition-zadd-2007-m
        # A = (X2 - X1) ** 2 % p
        A = pow((X2 - X1), 2) % p
        B = X1 * A % p
        C = X2 * A
        # D = (Y2 - Y1) ** 2 % p
        D = pow((Y2 - Y1), 2) % p
        if not A and not D:
            return self._double(X1, Y1, Z1, p, self.__curve.a())
        X3 = (D - B - C) % p
        Y3 = ((Y2 - Y1) * (B - X3) - Y1 * (C - B)) % p
        Z3 = Z1 * (X2 - X1) % p
        return X3, Y3, Z3
    self._add_with_z_eq = _add_with_z_eq

    def _add_with_z2_1(X1, Y1, Z1, X2, Y2, p):
        """add points when Z2 == 1"""
        # after:
        # http://hyperelliptic.org/EFD/g1p/auto-shortw-jacobian.html#addition-madd-2007-bl
        Z1Z1 = Z1 * Z1 % p
        U2, S2 = X2 * Z1Z1 % p, Y2 * Z1 * Z1Z1 % p
        H = (U2 - X1) % p
        HH = H * H % p
        I = 4 * HH % p
        J = H * I
        r = 2 * (S2 - Y1) % p
        if not r and not H:
            return self._double_with_z_1(X2, Y2, p, self.__curve.a())
        V = X1 * I
        X3 = (r * r - J - 2 * V) % p
        Y3 = (r * (V - X3) - 2 * Y1 * J) % p
        # Z3 = ((Z1 + H) ** 2 - Z1Z1 - HH) % p
        Z3 = (pow((Z1+ H), 2) - Z1Z1 - HH) % p
        return X3, Y3, Z3
    self._add_with_z2_1 = _add_with_z2_1

    def _add_with_z_ne(X1, Y1, Z1, X2, Y2, Z2, p):
        """add points with arbitrary z"""
        # after:
        # http://hyperelliptic.org/EFD/g1p/auto-shortw-jacobian.html#addition-add-2007-bl
        Z1Z1 = Z1 * Z1 % p
        Z2Z2 = Z2 * Z2 % p
        U1 = X1 * Z2Z2 % p
        U2 = X2 * Z1Z1 % p
        S1 = Y1 * Z2 * Z2Z2 % p
        S2 = Y2 * Z1 * Z1Z1 % p
        H = U2 - U1
        I = 4 * H * H % p
        J = H * I % p
        r = 2 * (S2 - S1) % p
        if not H and not r:
            return self._double(X1, Y1, Z1, p, self.__curve.a())
        V = U1 * I
        X3 = (r * r - J - 2 * V) % p
        Y3 = (r * (V - X3) - 2 * S1 * J) % p
        # Z3 = ((Z1 + Z2) ** 2 - Z1Z1 - Z2Z2) * H % p
        Z3 = (pow((Z1 + Z2), 2) - Z1Z1 - Z2Z2) * H % p

        return X3, Y3, Z3
    self._add_with_z_ne = _add_with_z_ne

    def _add(X1, Y1, Z1, X2, Y2, Z2, p):
        """add two points, select fastest method."""
        if not Y1 or not Z1:
            return X2, Y2, Z2
        if not Y2 or not Z2:
            return X1, Y1, Z1
        if Z1 == Z2:
            if Z1 == 1:
                return self._add_with_z_1(X1, Y1, X2, Y2, p)
            return self._add_with_z_eq(X1, Y1, Z1, X2, Y2, p)
        if Z1 == 1:
            return self._add_with_z2_1(X2, Y2, Z2, X1, Y1, p)
        if Z2 == 1:
            return self._add_with_z2_1(X1, Y1, Z1, X2, Y2, p)
        return self._add_with_z_ne(X1, Y1, Z1, X2, Y2, Z2, p)
    self._add = _add

    def _mul_precompute(other):
        """Multiply point by integer with precomputation table."""
        X3, Y3, Z3, p = 0, 0, 1, self.__curve.p()
        _add = self._add
        for X2, Y2 in self.__precompute:
            if other % 2:
                if other % 4 >= 2:
                    other = (other + 1) // 2
                    X3, Y3, Z3 = _add(X3, Y3, Z3, X2, -Y2, 1, p)
                else:
                    other = (other - 1) // 2
                    X3, Y3, Z3 = _add(X3, Y3, Z3, X2, Y2, 1, p)
            else:
                other //= 2

        if not Y3 or not Z3:
            return INFINITY
        return PointJacobi(self.__curve, X3, Y3, Z3, self.__order)
    self._mul_precompute = _mul_precompute

    def _double(X1, Y1, Z1, T1, p, a):
        """Double the point, assume sane parameters."""
        # after "dbl-2008-hwcd"
        # from https://hyperelliptic.org/EFD/g1p/auto-twisted-extended.html
        # NOTE: there are more efficient formulas for Z1 == 1
        A = X1 * X1 % p
        B = Y1 * Y1 % p
        C = 2 * Z1 * Z1 % p
        D = a * A % p
        E = ((X1 + Y1) * (X1 + Y1) - A - B) % p
        G = D + B
        F = G - C
        H = D - B
        X3 = E * F % p
        Y3 = G * H % p
        T3 = E * H % p
        Z3 = F * G % p

        return X3, Y3, Z3, T3
    self._double = _double

    def __mul__(other):
        """Multiply point by an integer."""
        if not self.__coords[1] or not other:
            return INFINITY
        if other == 1:
            return self
        if self.__order:
            # order*2 as a protection for Minerva
            other = other % (self.__order * 2)
        self._maybe_precompute()
        if self.__precompute:
            return self._mul_precompute(other)
        # self = self.scale()
        # X2, Y2, _ = self.__coords
        # X3, Y3, Z3 = 0, 0, 1
        # p, a = self.__curve.p(), self.__curve.a()
        # _double = self._double
        # _add = self._add
        # # since adding points when at least one of them is scaled
        # # is quicker, reverse the NAF order
        # for i in reversed(self._naf(other)):
        #     X3, Y3, Z3 = _double(X3, Y3, Z3, p, a)
        #     if i < 0:
        #         X3, Y3, Z3 = _add(X3, Y3, Z3, X2, -Y2, 1, p)
        #     elif i > 0:
        #         X3, Y3, Z3 = _add(X3, Y3, Z3, X2, Y2, 1, p)

        # if not Y3 or not Z3:
        #     return INFINITY

        # return PointJacobi(self.__curve, X3, Y3, Z3, self.__order)
    self.__mul__ = __mul__

    return self


ellipticcurve=larky.struct(
    AbstractPoint=AbstractPoint,
    PointJacobi=PointJacobi,
    CurveFp=CurveFp
)