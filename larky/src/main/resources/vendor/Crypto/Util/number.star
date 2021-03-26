def ceil_div(n, d):
    """
    Return ceil(n/d), that is, the smallest integer r such that r*d >= n
    """
def size (N):
    """
    Returns the size of the number N in bits.
    """
def getRandomInteger(N, randfunc=None):
    """
    Return a random number at most N bits long.

        If :data:`randfunc` is omitted, then :meth:`Random.get_random_bytes` is used.

        .. deprecated:: 3.0
            This function is for internal use only and may be renamed or removed in
            the future. Use :func:`Crypto.Random.random.getrandbits` instead.

    """
def getRandomRange(a, b, randfunc=None):
    """
    Return a random number *n* so that *a <= n < b*.

        If :data:`randfunc` is omitted, then :meth:`Random.get_random_bytes` is used.

        .. deprecated:: 3.0
            This function is for internal use only and may be renamed or removed in
            the future. Use :func:`Crypto.Random.random.randrange` instead.

    """
def getRandomNBitInteger(N, randfunc=None):
    """
    Return a random number with exactly N-bits,
        i.e. a random number between 2**(N-1) and (2**N)-1.

        If :data:`randfunc` is omitted, then :meth:`Random.get_random_bytes` is used.

        .. deprecated:: 3.0
            This function is for internal use only and may be renamed or removed in
            the future.

    """
def GCD(x,y):
    """
    Greatest Common Denominator of :data:`x` and :data:`y`.

    """
def inverse(u, v):
    """
    The inverse of :data:`u` *mod* :data:`v`.
    """
def getPrime(N, randfunc=None):
    """
    Return a random N-bit prime number.

        If randfunc is omitted, then :meth:`Random.get_random_bytes` is used.

    """
def _rabinMillerTest(n, rounds, randfunc=None):
    """
    _rabinMillerTest(n:long, rounds:int, randfunc:callable):int
        Tests if n is prime.
        Returns 0 when n is definitely composite.
        Returns 1 when n is probably prime.
        Returns 2 when n is definitely prime.

        If randfunc is omitted, then Random.new().read is used.

        This function is for internal use only and may be renamed or removed in
        the future.

    """
def getStrongPrime(N, e=0, false_positive_prob=1e-6, randfunc=None):
    r"""
        Return a random strong *N*-bit prime number.
        In this context, *p* is a strong prime if *p-1* and *p+1* have at
        least one large prime factor.

        Args:
            N (integer): the exact length of the strong prime.
              It must be a multiple of 128 and > 512.
            e (integer): if provided, the returned prime (minus 1)
              will be coprime to *e* and thus suitable for RSA where
              *e* is the public exponent.
            false_positive_prob (float):
              The statistical probability for the result not to be actually a
              prime. It defaults to 10\ :sup:`-6`.
              Note that the real probability of a false-positive is far less. This is
              just the mathematically provable limit.
            randfunc (callable):
              A function that takes a parameter *N* and that returns
              a random byte string of such length.
              If omitted, :func:`Crypto.Random.get_random_bytes` is used.
        Return:
            The new strong prime.

        .. deprecated:: 3.0
            This function is for internal use only and may be renamed or removed in
            the future.

    """
def isPrime(N, false_positive_prob=1e-6, randfunc=None):
    r"""Test if a number *N* is a prime.

        Args:
            false_positive_prob (float):
              The statistical probability for the result not to be actually a
              prime. It defaults to 10\ :sup:`-6`.
              Note that the real probability of a false-positive is far less.
              This is just the mathematically provable limit.
            randfunc (callable):
              A function that takes a parameter *N* and that returns
              a random byte string of such length.
              If omitted, :func:`Crypto.Random.get_random_bytes` is used.

        Return:
            `True` is the input is indeed prime.

    """
def long_to_bytes(n, blocksize=0):
    r"""
    Convert an integer to a byte string.

        In Python 3.2+, use the native method instead::

            >>> n.to_bytes(blocksize, 'big')

        For instance::

            >>> n = 80
            >>> n.to_bytes(2, 'big')
            b'\x00P'

        If the optional :data:`blocksize` is provided and greater than zero,
        the byte string is padded with binary zeros (on the front) so that
        the total length of the output is a multiple of blocksize.

        If :data:`blocksize` is zero or not provided, the byte string will
        be of minimal length.

    """
def bytes_to_long(s):
    r"""
    Convert a byte string to a long integer (big endian).

        In Python 3.2+, use the native method instead::

            >>> int.from_bytes(s, 'big')

        For instance::

            >>> int.from_bytes(b'\x00P', 'big')
            80

        This is (essentially) the inverse of :func:`long_to_bytes`.

    """
def long2str(n, blocksize=0):
    """
    long2str() has been replaced by long_to_bytes()
    """
def str2long(s):
    """
    str2long() has been replaced by bytes_to_long()
    """
