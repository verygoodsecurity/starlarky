load("@stdlib//larky", larky="larky")
load("@vendor//ecdsa/ellipticcurve", ellipticcurve="ellipticcurve")
load("@vendor//ecdsa/util", bit_length="bit_length")
load("@vendor//ecdsa/numbertheory", numbertheory="numbertheory")

def Signature(r, s):
    """ECDSA signature."""
    self = larky.mutablestruct(__class__ = Signature, __name__ = "Signature")
    def __init__(r, s):
        self.r = r
        self.s = s
        return self
    self = __init__(r, s)
    return self


def Public_key(generator, point, verify=True):
    """Public key for ECDSA."""
    self = larky.mutablestruct(__class__=Public_key, __name__="Public_key")
    def __init__(generator, point, verify=True):
        """Low level ECDSA public key object.

        :param generator: the Point that generates the group (the base point)
        :param point: the Point that defines the public key
        :param bool verify: if True check if point is valid point on curve

        :raises InvalidPointError: if the point parameters are invalid or
            point does not lay on the curve
        """

        self.curve = generator.curve()
        self.generator = generator
        self.point = point
        n = generator.order()
        p = self.curve.p()
        # if not (0 <= point.x() < p) or not (0 <= point.y() < p):
        #     raise InvalidPointError(
        #         "The public point has x or y out of range."
        #     )
        # if verify and not self.curve.contains_point(point.x(), point.y()):
        #     raise InvalidPointError("Point does not lay on the curve")
        # if not n:
        #     raise InvalidPointError("Generator point must have order.")
        # # for curve parameters with base point with cofactor 1, all points
        # # that are on the curve are scalar multiples of the base point, so
        # # verifying that is not necessary. See Section 3.2.2.1 of SEC 1 v2
        # if (
        #     verify
        #     and self.curve.cofactor() != 1
        #     and not n * point == ellipticcurve.INFINITY
        # ):
        #     raise InvalidPointError("Generator point order is bad.")
        return self
    self = __init__(generator, point, verify)

    return self

def Private_key(public_key, secret_multiplier):
    """Private key for ECDSA."""

    self = larky.mutablestruct(__class__=Private_key, __name__="Private_key")
    def __init__(public_key, secret_multiplier):
        """public_key is of class Public_key;
        secret_multiplier is a large integer.
        """

        self.public_key = public_key
        self.secret_multiplier = secret_multiplier
        return self
    self = __init__(public_key, secret_multiplier)

    def sign(hash, random_k):
        """Return a signature for the provided hash, using the provided
        random nonce.  It is absolutely vital that random_k be an unpredictable
        number in the range [1, self.public_key.point.order()-1].  If
        an attacker can guess random_k, he can compute our private key from a
        single signature.  Also, if an attacker knows a few high-order
        bits (or a few low-order bits) of random_k, he can compute our private
        key from many signatures.  The generation of nonces with adequate
        cryptographic strength is very difficult and far beyond the scope
        of this comment.
        May raise RuntimeError, in which case retrying with a new
        random value k is in order.
        """

        G = self.public_key.generator
        n = G.order()
        k = random_k % n
        # Fix the bit-length of the random nonce,
        # so that it doesn't leak via timing.
        # This does not change that ks = k mod n
        ks = k + n
        kt = ks + n
        if bit_length(ks) == bit_length(n):
            p1 = kt * G
        else:
            p1 = ks * G
        r = p1.x() % n
        if r == 0:
            fail('RSZeroError("amazingly unlucky random number r")')
        s = (
            numbertheory.inverse_mod(k, n)
            * (hash + (self.secret_multiplier * r) % n)
        ) % n
        if s == 0:
            fail('RSZeroError("amazingly unlucky random number s")')
        return Signature(r, s)
    self.sign = sign

    return self


# Certicom secp256-k1
_a = 0x0000000000000000000000000000000000000000000000000000000000000000
_b = 0x0000000000000000000000000000000000000000000000000000000000000007
_p = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F
_Gx = 0x79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798
_Gy = 0x483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8
_r = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141

curve_secp256k1 = ellipticcurve.CurveFp(_p, _a, _b, 1)
generator_secp256k1 = ellipticcurve.PointJacobi(
    curve_secp256k1, _Gx, _Gy, 1, _r, generator=True
)

ecdsa = larky.struct(
    curve_secp256k1=curve_secp256k1,
    generator_secp256k1=generator_secp256k1,
    Public_key=Public_key,
    Private_key=Private_key
)