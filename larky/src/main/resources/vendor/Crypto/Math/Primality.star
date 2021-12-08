load("@stdlib//jcrypto", _JCrypto="jcrypto")

COMPOSITE = 1

def miller_rabin_test(candidate, iterations, randfunc=None):
    r"""
    Perform a Miller-Rabin primality test on an integer.

        The test is specified in Section C.3.1 of `FIPS PUB 186-4`__.

        :Parameters:
          candidate : integer
            The number to test for primality.
          iterations : integer
            The maximum number of iterations to perform before
            declaring a candidate a probable prime.
          randfunc : callable
            An RNG function where bases are taken from.

        :Returns:
          ``Primality.COMPOSITE`` or ``Primality.PROBABLY_PRIME``.

        .. __: http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf

    """
def lucas_test(candidate):
    r"""
    Perform a Lucas primality test on an integer.

        The test is specified in Section C.3.3 of `FIPS PUB 186-4`__.

        :Parameters:
          candidate : integer
            The number to test for primality.

        :Returns:
          ``Primality.COMPOSITE`` or ``Primality.PROBABLY_PRIME``.

        .. __: http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf

    """
    def alternate():
        r"""
         Found D. P=1 and Q=(1-D)/4 (note that Q is guaranteed to be an integer)

         Step 3
         This is \delta(n) = n - jacobi(D/n)

        """
def test_probable_prime(candidate, randfunc=None):
    r"""
    Test if a number is prime.

        A number is qualified as prime if it passes a certain
        number of Miller-Rabin tests (dependent on the size
        of the number, but such that probability of a false
        positive is less than 10^-30) and a single Lucas test.

        For instance, a 1024-bit candidate will need to pass
        4 Miller-Rabin tests.

        :Parameters:
          candidate : integer
            The number to test for primality.
          randfunc : callable
            The routine to draw random bytes from to select Miller-Rabin bases.
        :Returns:
          ``PROBABLE_PRIME`` if the number if prime with very high probability.
          ``COMPOSITE`` if the number is a composite.
          For efficiency reasons, ``COMPOSITE`` is also returned for small primes.

    """
    return _JCrypto.Math.is_prime(candidate, 1e-30)

def generate_probable_prime(**kwargs):
    r"""
    Generate a random probable prime.

        The prime will not have any specific properties
        (e.g. it will not be a *strong* prime).

        Random numbers are evaluated for primality until one
        passes all tests, consisting of a certain number of
        Miller-Rabin tests with random bases followed by
        a single Lucas test.

        The number of Miller-Rabin iterations is chosen such that
        the probability that the output number is a non-prime is
        less than 1E-30 (roughly 2^{-100}).

        This approach is compliant to `FIPS PUB 186-4`__.

        :Keywords:
          exact_bits : integer
            The desired size in bits of the probable prime.
            It must be at least 160.
          randfunc : callable
            An RNG function where candidate primes are taken from.
          prime_filter : callable
            A function that takes an Integer as parameter and returns
            True if the number can be passed to further primality tests,
            False if it should be immediately discarded.

        :Return:
            A probable prime in the range 2^exact_bits > p > 2^(exact_bits-1).

        .. __: http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf

    """
def generate_probable_safe_prime(**kwargs):
    r"""
    Generate a random, probable safe prime.

        Note this operation is much slower than generating a simple prime.

        :Keywords:
          exact_bits : integer
            The desired size in bits of the probable safe prime.
          randfunc : callable
            An RNG function where candidate primes are taken from.

        :Return:
            A probable safe prime in the range
            2^exact_bits > p > 2^(exact_bits-1).

    """
