load("@stdlib//larky", larky="larky")
load("@vendor//ecdsa/ellipticcurve", ellipticcurve="ellipticcurve")

def Private_key(public_key, secret_multiplier):
    """Private key for ECDSA."""

    self = larky.mutablestruct(__class__="Private_key", __name__="Private_key")
    def __init__(public_key, secret_multiplier):
        """public_key is of class Public_key;
        secret_multiplier is a large integer.
        """

        self.public_key = public_key
        self.secret_multiplier = secret_multiplier
        return self
    self = __init__(public_key, secret_multiplier)

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
