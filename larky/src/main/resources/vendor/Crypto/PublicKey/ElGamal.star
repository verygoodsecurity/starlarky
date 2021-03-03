def generate(bits, randfunc):
    """
    Randomly generate a fresh, new ElGamal key.

        The key will be safe for use for both encryption and signature
        (although it should be used for **only one** purpose).

        Args:
          bits (int):
            Key length, or size (in bits) of the modulus *p*.
            The recommended value is 2048.
          randfunc (callable):
            Random number generation function; it should accept
            a single integer *N* and return a string of random
            *N* random bytes.

        Return:
            an :class:`ElGamalKey` object
    
    """
def construct(tup):
    """
    r"""Construct an ElGamal key from a tuple of valid ElGamal components.

        The modulus *p* must be a prime.
        The following conditions must apply:

        .. math::

            \begin{align}
            &1 < g < p-1 \\
            &g^{p-1} = 1 \text{ mod } 1 \\
            &1 < x < p-1 \\
            &g^x = y \text{ mod } p
            \end{align}

        Args:
          tup (tuple):
            A tuple with either 3 or 4 integers,
            in the following order:

            1. Modulus (*p*).
            2. Generator (*g*).
            3. Public key (*y*).
            4. Private key (*x*). Optional.

        Raises:
            ValueError: when the key being imported fails the most basic ElGamal validity checks.

        Returns:
            an :class:`ElGamalKey` object
    
    """
def ElGamalKey(object):
    """
    r"""Class defining an ElGamal key.
        Do not instantiate directly.
        Use :func:`generate` or :func:`construct` instead.

        :ivar p: Modulus
        :vartype d: integer

        :ivar g: Generator
        :vartype e: integer

        :ivar y: Public key component
        :vartype y: integer

        :ivar x: Private key component
        :vartype x: integer
    
    """
    def __init__(self, randfunc=None):
        """
        'x'
        """
    def _sign(self, M, K):
        """
        'x'
        """
    def _verify(self, M, sig):
        """
        Whether this is an ElGamal private key
        """
    def can_encrypt(self):
        """
        A matching ElGamal public key.

                Returns:
                    a new :class:`ElGamalKey` object
        
        """
    def __eq__(self, other):
        """
         ElGamal key is not pickable

        """
