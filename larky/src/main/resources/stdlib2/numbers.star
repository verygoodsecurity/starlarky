def Number(metadef=ABCMeta):
    """
    All numbers inherit from this class.

        If you just want to check if an argument x is a number, without
        caring what kind, use isinstance(x, Number).
    
    """
def Complex(Number):
    """
    Complex defines the operations that work on the builtin complex type.

        In short, those are: a conversion to complex, .real, .imag, +, -,
        *, /, abs(), .conjugate, ==, and !=.

        If it is given heterogeneous arguments, and doesn't have special
        knowledge about them, it should fall back to the builtin complex
        type as described below.
    
    """
    def __complex__(self):
        """
        Return a builtin complex instance. Called for complex(self).
        """
    def __bool__(self):
        """
        True if self != 0. Called for bool(self).
        """
    def real(self):
        """
        Retrieve the real component of this number.

                This should subclass Real.
        
        """
    def imag(self):
        """
        Retrieve the imaginary component of this number.

                This should subclass Real.
        
        """
    def __add__(self, other):
        """
        self + other
        """
    def __radd__(self, other):
        """
        other + self
        """
    def __neg__(self):
        """
        -self
        """
    def __pos__(self):
        """
        +self
        """
    def __sub__(self, other):
        """
        self - other
        """
    def __rsub__(self, other):
        """
        other - self
        """
    def __mul__(self, other):
        """
        self * other
        """
    def __rmul__(self, other):
        """
        other * self
        """
    def __truediv__(self, other):
        """
        self / other: Should promote to float when necessary.
        """
    def __rtruediv__(self, other):
        """
        other / self
        """
    def __pow__(self, exponent):
        """
        self**exponent; should promote to float or complex when necessary.
        """
    def __rpow__(self, base):
        """
        base ** self
        """
    def __abs__(self):
        """
        Returns the Real distance from 0. Called for abs(self).
        """
    def conjugate(self):
        """
        (x+y*i).conjugate() returns (x-y*i).
        """
    def __eq__(self, other):
        """
        self == other
        """
def Real(Complex):
    """
    To Complex, Real adds the operations that work on real numbers.

        In short, those are: a conversion to float, trunc(), divmod,
        %, <, <=, >, and >=.

        Real also provides defaults for the derived operations.
    
    """
    def __float__(self):
        """
        Any Real can be converted to a native float object.

                Called for float(self).
        """
    def __trunc__(self):
        """
        trunc(self): Truncates self to an Integral.

                Returns an Integral i such that:
                  * i>0 iff self>0;
                  * abs(i) <= abs(self);
                  * for any Integral j satisfying the first two conditions,
                    abs(i) >= abs(j) [i.e. i has "maximal" abs among those].
                i.e. "truncate towards 0".
        
        """
    def __floor__(self):
        """
        Finds the greatest Integral <= self.
        """
    def __ceil__(self):
        """
        Finds the least Integral >= self.
        """
    def __round__(self, ndigits=None):
        """
        Rounds self to ndigits decimal places, defaulting to 0.

                If ndigits is omitted or None, returns an Integral, otherwise
                returns a Real. Rounds half toward even.
        
        """
    def __divmod__(self, other):
        """
        divmod(self, other): The pair (self // other, self % other).

                Sometimes this can be computed faster than the pair of
                operations.
        
        """
    def __rdivmod__(self, other):
        """
        divmod(other, self): The pair (self // other, self % other).

                Sometimes this can be computed faster than the pair of
                operations.
        
        """
    def __floordiv__(self, other):
        """
        self // other: The floor() of self/other.
        """
    def __rfloordiv__(self, other):
        """
        other // self: The floor() of other/self.
        """
    def __mod__(self, other):
        """
        self % other
        """
    def __rmod__(self, other):
        """
        other % self
        """
    def __lt__(self, other):
        """
        self < other

                < on Reals defines a total ordering, except perhaps for NaN.
        """
    def __le__(self, other):
        """
        self <= other
        """
    def __complex__(self):
        """
        complex(self) == complex(float(self), 0)
        """
    def real(self):
        """
        Real numbers are their real component.
        """
    def imag(self):
        """
        Real numbers have no imaginary component.
        """
    def conjugate(self):
        """
        Conjugate is a no-op for Reals.
        """
def Rational(Real):
    """
    .numerator and .denominator should be in lowest terms.
    """
    def numerator(self):
        """
         Concrete implementation of Real's conversion to float.

        """
    def __float__(self):
        """
        float(self) = self.numerator / self.denominator

                It's important that this conversion use the integer's "true"
                division rather than casting one side to float before dividing
                so that ratios of huge integers convert without overflowing.

        
        """
def Integral(Rational):
    """
    Integral adds a conversion to int and the bit-string operations.
    """
    def __int__(self):
        """
        int(self)
        """
    def __index__(self):
        """
        Called whenever an index is needed, such as in slicing
        """
    def __pow__(self, exponent, modulus=None):
        """
        self ** exponent % modulus, but maybe faster.

                Accept the modulus argument if you want to support the
                3-argument version of pow(). Raise a TypeError if exponent < 0
                or any argument isn't Integral. Otherwise, just implement the
                2-argument version described in Complex.
        
        """
    def __lshift__(self, other):
        """
        self << other
        """
    def __rlshift__(self, other):
        """
        other << self
        """
    def __rshift__(self, other):
        """
        self >> other
        """
    def __rrshift__(self, other):
        """
        other >> self
        """
    def __and__(self, other):
        """
        self & other
        """
    def __rand__(self, other):
        """
        other & self
        """
    def __xor__(self, other):
        """
        self ^ other
        """
    def __rxor__(self, other):
        """
        other ^ self
        """
    def __or__(self, other):
        """
        self | other
        """
    def __ror__(self, other):
        """
        other | self
        """
    def __invert__(self):
        """
        ~self
        """
    def __float__(self):
        """
        float(self) == float(int(self))
        """
    def numerator(self):
        """
        Integers are their own numerators.
        """
    def denominator(self):
        """
        Integers have a denominator of 1.
        """
