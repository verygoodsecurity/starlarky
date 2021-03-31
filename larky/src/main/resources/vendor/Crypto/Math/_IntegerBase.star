def IntegerBase(ABC):
    """
     Conversions

    """
    def __int__(self):
        """
         Relations

        """
    def __eq__(self, term):
        """
         Arithmetic operations

        """
    def __add__(self, term):
        """
         Boolean/bit operations

        """
    def __and__(self, term):
        """
         Extra

        """
    def is_odd(self):
        """
        Tonelli-shanks algorithm for computing the square root
                of n modulo a prime p.

                n must be in the range [0..p-1].
                p must be at least even.

                The return value r is the square root of modulo p. If non-zero,
                another solution will also exist (p-r).

                Note we cannot assume that p is really a prime: if it's not,
                we can either raise an exception or return the correct value.
        
        """
    def random(cls, **kwargs):
        """
        Generate a random natural integer of a certain size.

                :Keywords:
                  exact_bits : positive integer
                    The length in bits of the resulting random Integer number.
                    The number is guaranteed to fulfil the relation:

                        2^bits > result >= 2^(bits - 1)

                  max_bits : positive integer
                    The maximum length in bits of the resulting random Integer number.
                    The number is guaranteed to fulfil the relation:

                        2^bits > result >=0

                  randfunc : callable
                    A function that returns a random byte string. The length of the
                    byte string is passed as parameter. Optional.
                    If not provided (or ``None``), randomness is read from the system RNG.

                :Return: a Integer object
        
        """
    def random_range(cls, **kwargs):
        """
        Generate a random integer within a given internal.

                :Keywords:
                  min_inclusive : integer
                    The lower end of the interval (inclusive).
                  max_inclusive : integer
                    The higher end of the interval (inclusive).
                  max_exclusive : integer
                    The higher end of the interval (exclusive).
                  randfunc : callable
                    A function that returns a random byte string. The length of the
                    byte string is passed as parameter. Optional.
                    If not provided (or ``None``), randomness is read from the system RNG.
                :Returns:
                    An Integer randomly taken in the given interval.
        
        """
