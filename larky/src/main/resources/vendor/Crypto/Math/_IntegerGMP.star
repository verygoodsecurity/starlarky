    def _MPZ(Structure):
    """
    '_mp_alloc'
    """
    def new_mpz():
        """
         We are using CFFI

        """
    def new_mpz():
        """
        MPZ*
        """
def _GMP(object):
    """
    mpz_
    """
def IntegerGMP(IntegerBase):
    """
    A fast, arbitrary precision integer
    """
    def __init__(self, value):
        """
        Initialize the integer to the given value.
        """
    def __int__(self):
        """
         buf will contain the integer encoded in decimal plus the trailing
         zero, and possibly the negative sign.
         dig10(x) < log10(x) + 1 = log2(x)/log2(10) + 1 < log2(x)/3 + 1

        """
    def __str__(self):
        """
        Integer(%s)
        """
    def __hex__(self):
        """
         Only Python 3.x

        """
    def __index__(self):
        """
        Convert the number into a byte string.

                This method encodes the number in network order and prepends
                as many zero bytes as required. It only works for non-negative
                values.

                :Parameters:
                  block_size : integer
                    The exact size the output byte string must have.
                    If zero, the string has the minimal length.
                :Returns:
                  A byte string.
                :Raise ValueError:
                  If the value is negative or if ``block_size`` is
                  provided and the length of the byte string would exceed it.
        
        """
    def from_bytes(byte_string):
        """
        Convert a byte string into a number.

                :Parameters:
                  byte_string : byte string
                    The input number, encoded in network order.
                    It can only be non-negative.
                :Return:
                  The ``Integer`` object carrying the same value as the input.
        
        """
    def _apply_and_return(self, func, term):
        """
         Arithmetic operations

        """
    def __add__(self, term):
        """
        Division by zero
        """
    def __mod__(self, divisor):
        """
        Division by zero
        """
    def inplace_pow(self, exponent, modulus=None):
        """
        Exponent must not be negative
        """
    def __pow__(self, exponent, modulus=None):
        """
        Return the largest Integer that does not
                exceed the square root
        """
    def __iadd__(self, term):
        """
        Division by zero
        """
    def __and__(self, term):
        """
        negative shift count
        """
    def __irshift__(self, pos):
        """
        negative shift count
        """
    def __lshift__(self, pos):
        """
        Incorrect shift count
        """
    def __ilshift__(self, pos):
        """
        Incorrect shift count
        """
    def get_bit(self, n):
        """
        Return True if the n-th bit is set to 1.
                Bit 0 is the least significant.
        """
    def is_odd(self):
        """
        Return the minimum number of bits that can encode the number.
        """
    def size_in_bytes(self):
        """
        Return the minimum number of bytes that can encode the number.
        """
    def is_perfect_square(self):
        """
        Raise an exception if the small prime is a divisor.
        """
    def multiply_accumulate(self, a, b):
        """
        Increment the number by the product of a and b.
        """
    def set(self, source):
        """
        Set the Integer to have the given value
        """
    def inplace_inverse(self, modulus):
        """
        Compute the inverse of this number in the ring of
                modulo integers.

                Raise an exception if no inverse exists.
        
        """
    def inverse(self, modulus):
        """
        Compute the greatest common denominator between this
                number and another term.
        """
    def lcm(self, term):
        """
        Compute the least common multiplier between this
                number and another term.
        """
    def jacobi_symbol(a, n):
        """
        Compute the Jacobi symbol
        """
