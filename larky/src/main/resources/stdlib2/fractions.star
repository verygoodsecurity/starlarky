def gcd(a, b):
    """
    Calculate the Greatest Common Divisor of a and b.

        Unless b==0, the result will have the same sign as b (so that when
        b is divided by it, the result comes out positive).
    
    """
def _gcd(a, b):
    """
     Supports non-integers for backward compatibility.

    """
def Fraction(numbers.Rational):
    """
    This class implements rational numbers.

        In the two-argument form of the constructor, Fraction(8, 6) will
        produce a rational number equivalent to 4/3. Both arguments must
        be Rational. The numerator defaults to 0 and the denominator
        defaults to 1 so that Fraction(3) == 3 and Fraction() == 0.

        Fractions can also be constructed from:

          - numeric strings similar to those accepted by the
            float constructor (for example, '-2.3' or '1e10')

          - strings of the form '123/456'

          - float and Decimal instances

          - other Rational instances (including integers)

    
    """
    def __new__(cls, numerator=0, denominator=None, *, _normalize=True):
        """
        Constructs a Rational.

                Takes a string like '3/2' or '1.5', another Rational instance, a
                numerator/denominator pair, or a float.

                Examples
                --------

                >>> Fraction(10, -8)
                Fraction(-5, 4)
                >>> Fraction(Fraction(1, 7), 5)
                Fraction(1, 35)
                >>> Fraction(Fraction(1, 7), Fraction(2, 3))
                Fraction(3, 14)
                >>> Fraction('314')
                Fraction(314, 1)
                >>> Fraction('-35/4')
                Fraction(-35, 4)
                >>> Fraction('3.1415') # conversion from numeric string
                Fraction(6283, 2000)
                >>> Fraction('-47e-2') # string may include a decimal exponent
                Fraction(-47, 100)
                >>> Fraction(1.47)  # direct construction from float (exact conversion)
                Fraction(6620291452234629, 4503599627370496)
                >>> Fraction(2.25)
                Fraction(9, 4)
                >>> Fraction(Decimal('1.47'))
                Fraction(147, 100)

        
        """
    def from_float(cls, f):
        """
        Converts a finite float to a rational number, exactly.

                Beware that Fraction.from_float(0.3) != Fraction(3, 10).

        
        """
    def from_decimal(cls, dec):
        """
        Converts a finite Decimal instance to a rational number, exactly.
        """
    def as_integer_ratio(self):
        """
        Return the integer ratio as a tuple.

                Return a tuple of two integers, whose ratio is equal to the
                Fraction and with a positive denominator.
        
        """
    def limit_denominator(self, max_denominator=1000000):
        """
        Closest Fraction to self with denominator at most max_denominator.

                >>> Fraction('3.141592653589793').limit_denominator(10)
                Fraction(22, 7)
                >>> Fraction('3.141592653589793').limit_denominator(100)
                Fraction(311, 99)
                >>> Fraction(4321, 8765).limit_denominator(10000)
                Fraction(4321, 8765)

        
        """
    def numerator(a):
        """
        repr(self)
        """
    def __str__(self):
        """
        str(self)
        """
    def _operator_fallbacks(monomorphic_operator, fallback_operator):
        """
        Generates forward and reverse operators given a purely-rational
                operator and a function from the operator module.

                Use this like:
                __op__, __rop__ = _operator_fallbacks(just_rational_op, operator.op)

                In general, we want to implement the arithmetic operations so
                that mixed-mode operations either call an implementation whose
                author knew about the types of both arguments, or convert both
                to the nearest built in type and do the operation there. In
                Fraction, that means that we define __add__ and __radd__ as:

                    def __add__(self, other):
                        # Both types have numerators/denominator attributes,
                        # so do the operation directly
                        if isinstance(other, (int, Fraction)):
                            return Fraction(self.numerator * other.denominator +
                                            other.numerator * self.denominator,
                                            self.denominator * other.denominator)
                        # float and complex don't have those operations, but we
                        # know about those types, so special case them.
                        elif isinstance(other, float):
                            return float(self) + other
                        elif isinstance(other, complex):
                            return complex(self) + other
                        # Let the other type take over.
                        return NotImplemented

                    def __radd__(self, other):
                        # radd handles more types than add because there's
                        # nothing left to fall back to.
                        if isinstance(other, numbers.Rational):
                            return Fraction(self.numerator * other.denominator +
                                            other.numerator * self.denominator,
                                            self.denominator * other.denominator)
                        elif isinstance(other, Real):
                            return float(other) + float(self)
                        elif isinstance(other, Complex):
                            return complex(other) + complex(self)
                        return NotImplemented


                There are 5 different cases for a mixed-type addition on
                Fraction. I'll refer to all of the above code that doesn't
                refer to Fraction, float, or complex as "boilerplate". 'r'
                will be an instance of Fraction, which is a subtype of
                Rational (r : Fraction <: Rational), and b : B <:
                Complex. The first three involve 'r + b':

                    1. If B <: Fraction, int, float, or complex, we handle
                       that specially, and all is well.
                    2. If Fraction falls back to the boilerplate code, and it
                       were to return a value from __add__, we'd miss the
                       possibility that B defines a more intelligent __radd__,
                       so the boilerplate should return NotImplemented from
                       __add__. In particular, we don't handle Rational
                       here, even though we could get an exact answer, in case
                       the other type wants to do something special.
                    3. If B <: Fraction, Python tries B.__radd__ before
                       Fraction.__add__. This is ok, because it was
                       implemented with knowledge of Fraction, so it can
                       handle those instances before delegating to Real or
                       Complex.

                The next two situations describe 'b + r'. We assume that b
                didn't know about Fraction in its implementation, and that it
                uses similar boilerplate code:

                    4. If B <: Rational, then __radd_ converts both to the
                       builtin rational type (hey look, that's us) and
                       proceeds.
                    5. Otherwise, __radd__ tries to find the nearest common
                       base ABC, and fall back to its builtin type. Since this
                       class doesn't subclass a concrete type, there's no
                       implementation to fall back to, so we need to try as
                       hard as possible to return an actual value, or the user
                       will get a TypeError.

        
        """
        def forward(a, b):
            """
            '__'
            """
        def reverse(b, a):
            """
             Includes ints.

            """
    def _add(a, b):
        """
        a + b
        """
    def _sub(a, b):
        """
        a - b
        """
    def _mul(a, b):
        """
        a * b
        """
    def _div(a, b):
        """
        a / b
        """
    def _floordiv(a, b):
        """
        a // b
        """
    def _divmod(a, b):
        """
        (a // b, a % b)
        """
    def _mod(a, b):
        """
        a % b
        """
    def __pow__(a, b):
        """
        a ** b

                If b is not an integer, the result will be a float or complex
                since roots are generally irrational. If b is an integer, the
                result will be rational.

        
        """
    def __rpow__(b, a):
        """
        a ** b
        """
    def __pos__(a):
        """
        +a: Coerces a subclass instance to Fraction
        """
    def __neg__(a):
        """
        -a
        """
    def __abs__(a):
        """
        abs(a)
        """
    def __trunc__(a):
        """
        trunc(a)
        """
    def __floor__(a):
        """
        math.floor(a)
        """
    def __ceil__(a):
        """
        math.ceil(a)
        """
    def __round__(self, ndigits=None):
        """
        round(self, ndigits)

                Rounds half toward even.
        
        """
    def __hash__(self):
        """
        hash(self)
        """
    def __eq__(a, b):
        """
        a == b
        """
    def _richcmp(self, other, op):
        """
        Helper for comparison operators, for internal use only.

                Implement comparison between a Rational instance `self`, and
                either another Rational instance or a float `other`.  If
                `other` is not a Rational instance or a float, return
                NotImplemented. `op` should be one of the six standard
                comparison operators.

        
        """
    def __lt__(a, b):
        """
        a < b
        """
    def __gt__(a, b):
        """
        a > b
        """
    def __le__(a, b):
        """
        a <= b
        """
    def __ge__(a, b):
        """
        a >= b
        """
    def __bool__(a):
        """
        a != 0
        """
    def __reduce__(self):
        """
         I'm immutable; therefore I am my own clone
        """
    def __deepcopy__(self, memo):
        """
         My components are also immutable
        """
