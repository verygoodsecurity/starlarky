def _mult_gf2(f1, f2):
    """
    Multiply two polynomials in GF(2)
    """
def _div_gf2(a, b):
    """

        Compute division of polynomials over GF(2).
        Given a and b, it finds two polynomials q and r such that:

        a = b*q + r with deg(r)<deg(b)
    
    """
def _Element(object):
    """
    Element of GF(2^128) field
    """
    def __init__(self, encoded_value):
        """
        Initialize the element to a certain value.

                The value passed as parameter is internally encoded as
                a 128-bit integer, where each bit represents a polynomial
                coefficient. The LSB is the constant coefficient.
        
        """
    def __eq__(self, other):
        """
        Return the field element, encoded as a 128-bit integer.
        """
    def encode(self):
        """
        Return the field element, encoded as a 16 byte string.
        """
    def __mul__(self, factor):
        """
         Make sure that f2 is the smallest, to speed up the loop

        """
    def __add__(self, term):
        """
        Return the inverse of this element in GF(2^128).
        """
    def __pow__(self, exponent):
        """
        Shamir's secret sharing scheme.

            A secret is split into ``n`` shares, and it is sufficient to collect
            ``k`` of them to reconstruct the secret.
    
        """
    def split(k, n, secret, ssss=False):
        """
        Split a secret into ``n`` shares.

                The secret can be reconstructed later using just ``k`` shares
                out of the original ``n``.
                Each share must be kept confidential to the person it was
                assigned to.

                Each share is associated to an index (starting from 1).

                Args:
                  k (integer):
                    The sufficient number of shares to reconstruct the secret (``k < n``).
                  n (integer):
                    The number of shares that this method will create.
                  secret (byte string):
                    A byte string of 16 bytes (e.g. the AES 128 key).
                  ssss (bool):
                    If ``True``, the shares can be used with the ``ssss`` utility.
                    Default: ``False``.

                Return (tuples):
                    ``n`` tuples. A tuple is meant for each participant and it contains two items:

                    1. the unique index (an integer)
                    2. the share (a byte string, 16 bytes)
        
        """
        def make_share(user, coeffs, ssss):
            """
            Recombine a secret, if enough shares are presented.

                    Args:
                      shares (tuples):
                        The *k* tuples, each containin the index (an integer) and
                        the share (a byte string, 16 bytes long) that were assigned to
                        a participant.
                      ssss (bool):
                        If ``True``, the shares were produced by the ``ssss`` utility.
                        Default: ``False``.

                    Return:
                        The original secret, as a byte string (16 bytes long).
        
            """
