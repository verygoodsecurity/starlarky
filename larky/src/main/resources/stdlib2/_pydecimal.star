def DecimalException(ArithmeticError):
    """
    Base exception class.

        Used exceptions derive from this.
        If an exception derives from another exception besides this (such as
        Underflow (Inexact, Rounded, Subnormal) that indicates that it is only
        called if the others are present.  This isn't actually used for
        anything, though.

        handle  -- Called when context._raise_error is called and the
                   trap_enabler is not set.  First argument is self, second is the
                   context.  More arguments can be given, those being after
                   the explanation in _raise_error (For example,
                   context._raise_error(NewError, '(-x)!', self._sign) would
                   call NewError().handle(context, self._sign).)

        To define a new exception, it should be sufficient to have it derive
        from DecimalException.
    
    """
    def handle(self, context, *args):
        """
        Exponent of a 0 changed to fit bounds.

            This occurs and signals clamped if the exponent of a result has been
            altered in order to fit the constraints of a specific concrete
            representation.  This may occur when the exponent of a zero result would
            be outside the bounds of a representation, or when a large normal
            number would have an encoded exponent that cannot be represented.  In
            this latter case, the exponent is reduced to fit and the corresponding
            number of zero digits are appended to the coefficient ("fold-down").
    
        """
def InvalidOperation(DecimalException):
    """
    An invalid operation was performed.

        Various bad things cause this:

        Something creates a signaling NaN
        -INF + INF
        0 * (+-)INF
        (+-)INF / (+-)INF
        x % 0
        (+-)INF % x
        x._rescale( non-integer )
        sqrt(-x) , x > 0
        0 ** 0
        x ** (non-integer)
        x ** (+-)INF
        An operand is invalid

        The result of the operation after these is a quiet positive NaN,
        except when the cause is a signaling NaN, in which case the result is
        also a quiet NaN, but with the original sign, and an optional
        diagnostic information.
    
    """
    def handle(self, context, *args):
        """
        'n'
        """
def ConversionSyntax(InvalidOperation):
    """
    Trying to convert badly formed string.

        This occurs and signals invalid-operation if a string is being
        converted to a number and it does not conform to the numeric string
        syntax.  The result is [0,qNaN].
    
    """
    def handle(self, context, *args):
        """
        Division by 0.

            This occurs and signals division-by-zero if division of a finite number
            by zero was attempted (during a divide-integer or divide operation, or a
            power operation with negative right-hand operand), and the dividend was
            not zero.

            The result of the operation is [sign,inf], where sign is the exclusive
            or of the signs of the operands for divide, or is 1 for an odd power of
            -0, for power.
    
        """
    def handle(self, context, sign, *args):
        """
        Cannot perform the division adequately.

            This occurs and signals invalid-operation if the integer result of a
            divide-integer or remainder operation had too many digits (would be
            longer than precision).  The result is [0,qNaN].
    
        """
    def handle(self, context, *args):
        """
        Undefined result of division.

            This occurs and signals invalid-operation if division by zero was
            attempted (during a divide-integer, divide, or remainder operation), and
            the dividend is also zero.  The result is [0,qNaN].
    
        """
    def handle(self, context, *args):
        """
        Had to round, losing information.

            This occurs and signals inexact whenever the result of an operation is
            not exact (that is, it needed to be rounded and any discarded digits
            were non-zero), or if an overflow or underflow condition occurs.  The
            result in all cases is unchanged.

            The inexact signal may be tested (or trapped) to determine if a given
            operation (or sequence of operations) was inexact.
    
        """
def InvalidContext(InvalidOperation):
    """
    Invalid context.  Unknown rounding, for example.

        This occurs and signals invalid-operation if an invalid context was
        detected during an operation.  This can occur if contexts are not checked
        on creation and either the precision exceeds the capability of the
        underlying concrete representation or an unknown or unsupported rounding
        was specified.  These aspects of the context need only be checked when
        the values are required to be used.  The result is [0,qNaN].
    
    """
    def handle(self, context, *args):
        """
        Number got rounded (not  necessarily changed during rounding).

            This occurs and signals rounded whenever the result of an operation is
            rounded (that is, some zero or non-zero digits were discarded from the
            coefficient), or if an overflow or underflow condition occurs.  The
            result in all cases is unchanged.

            The rounded signal may be tested (or trapped) to determine if a given
            operation (or sequence of operations) caused a loss of precision.
    
        """
def Subnormal(DecimalException):
    """
    Exponent < Emin before rounding.

        This occurs and signals subnormal whenever the result of a conversion or
        operation is subnormal (that is, its adjusted exponent is less than
        Emin, before any rounding).  The result in all cases is unchanged.

        The subnormal signal may be tested (or trapped) to determine if a given
        or operation (or sequence of operations) yielded a subnormal result.
    
    """
def Overflow(Inexact, Rounded):
    """
    Numerical overflow.

        This occurs and signals overflow if the adjusted exponent of a result
        (from a conversion or from an operation that is not an attempt to divide
        by zero), after rounding, would be greater than the largest value that
        can be handled by the implementation (the value Emax).

        The result depends on the rounding mode:

        For round-half-up and round-half-even (and for round-half-down and
        round-up, if implemented), the result of the operation is [sign,inf],
        where sign is the sign of the intermediate result.  For round-down, the
        result is the largest finite number that can be represented in the
        current precision, with the sign of the intermediate result.  For
        round-ceiling, the result is the same as for round-down if the sign of
        the intermediate result is 1, or is [0,inf] otherwise.  For round-floor,
        the result is the same as for round-down if the sign of the intermediate
        result is 0, or is [1,inf] otherwise.  In all cases, Inexact and Rounded
        will also be raised.
    
    """
    def handle(self, context, sign, *args):
        """
        '9'
        """
def Underflow(Inexact, Rounded, Subnormal):
    """
    Numerical underflow with result rounded to 0.

        This occurs and signals underflow if a result is inexact and the
        adjusted exponent of the result would be smaller (more negative) than
        the smallest value that can be handled by the implementation (the value
        Emin).  That is, the result is both inexact and subnormal.

        The result after an underflow will be a subnormal number rounded, if
        necessary, so that its exponent is not less than Etiny.  This may result
        in 0 with the sign of the intermediate result and an exponent of Etiny.

        In all cases, Inexact, Rounded, and Subnormal will also be raised.
    
    """
def FloatOperation(DecimalException, TypeError):
    """
    Enable stricter semantics for mixing floats and Decimals.

        If the signal is not trapped (default), mixing floats and Decimals is
        permitted in the Decimal() constructor, context.create_decimal() and
        all comparison operators. Both conversion and comparisons are exact.
        Any occurrence of a mixed operation is silently recorded by setting
        FloatOperation in the context flags.  Explicit conversions with
        Decimal.from_float() or context.create_decimal_from_float() do not
        set the flag.

        Otherwise (the signal is trapped), only equality comparisons and explicit
        conversions are silent. All other mixed operations raise FloatOperation.
    
    """
def getcontext():
    """
    Returns this thread's context.

        If this thread does not yet have a context, returns
        a new context and sets this thread's context.
        New contexts are copies of DefaultContext.
    
    """
def setcontext(context):
    """
    Set this thread's context to context.
    """
def localcontext(ctx=None):
    """
    Return a context manager for a copy of the supplied context

        Uses a copy of the current context if no context is specified
        The returned context manager creates a local decimal context
        in a with statement:
            def sin(x):
                 with localcontext() as ctx:
                     ctx.prec += 2
                     # Rest of sin calculation algorithm
                     # uses a precision 2 greater than normal
                 return +s  # Convert result to normal precision

             def sin(x):
                 with localcontext(ExtendedContext):
                     # Rest of sin calculation algorithm
                     # uses the Extended Context from the
                     # General Decimal Arithmetic Specification
                 return +s  # Convert result to normal context

        >>> setcontext(DefaultContext)
        >>> print(getcontext().prec)
        28
        >>> with localcontext():
        ...     ctx = getcontext()
        ...     ctx.prec += 2
        ...     print(ctx.prec)
        ...
        30
        >>> with localcontext(ExtendedContext):
        ...     print(getcontext().prec)
        ...
        9
        >>> print(getcontext().prec)
        28
    
    """
def Decimal(object):
    """
    Floating point class for decimal arithmetic.
    """
    def __new__(cls, value="0", context=None):
        """
        Create a decimal point instance.

                >>> Decimal('3.14')              # string input
                Decimal('3.14')
                >>> Decimal((0, (3, 1, 4), -2))  # tuple (sign, digit_tuple, exponent)
                Decimal('3.14')
                >>> Decimal(314)                 # int
                Decimal('314')
                >>> Decimal(Decimal(314))        # another decimal instance
                Decimal('314')
                >>> Decimal('  3.14  \\n')        # leading and trailing whitespace okay
                Decimal('3.14')
        
        """
    def from_float(cls, f):
        """
        Converts a float to a decimal number, exactly.

                Note that Decimal.from_float(0.1) is not the same as Decimal('0.1').
                Since 0.1 is not exactly representable in binary floating point, the
                value is stored as the nearest representable value which is
                0x1.999999999999ap-4.  The exact equivalent of the value in decimal
                is 0.1000000000000000055511151231257827021181583404541015625.

                >>> Decimal.from_float(0.1)
                Decimal('0.1000000000000000055511151231257827021181583404541015625')
                >>> Decimal.from_float(float('nan'))
                Decimal('NaN')
                >>> Decimal.from_float(float('inf'))
                Decimal('Infinity')
                >>> Decimal.from_float(-float('inf'))
                Decimal('-Infinity')
                >>> Decimal.from_float(-0.0)
                Decimal('-0')

        
        """
    def _isnan(self):
        """
        Returns whether the number is not actually one.

                0 if a number
                1 if NaN
                2 if sNaN
        
        """
    def _isinfinity(self):
        """
        Returns whether the number is infinite

                0 if finite or not a number
                1 if +INF
                -1 if -INF
        
        """
    def _check_nans(self, other=None, context=None):
        """
        Returns whether the number is not actually one.

                if self, other are sNaN, signal
                if self, other are NaN return nan
                return 0

                Done before operations.
        
        """
    def _compare_check_nans(self, other, context):
        """
        Version of _check_nans used for the signaling comparisons
                compare_signal, __le__, __lt__, __ge__, __gt__.

                Signal InvalidOperation if either self or other is a (quiet
                or signaling) NaN.  Signaling NaNs take precedence over quiet
                NaNs.

                Return 0 if neither operand is a NaN.

        
        """
    def __bool__(self):
        """
        Return True if self is nonzero; otherwise return False.

                NaNs and infinities are considered nonzero.
        
        """
    def _cmp(self, other):
        """
        Compare the two non-NaN decimal instances self and other.

                Returns -1 if self < other, 0 if self == other and 1
                if self > other.  This routine is for internal use only.
        """
    def __eq__(self, other, context=None):
        """
        Compare self to other.  Return a decimal value:

                a or b is a NaN ==> Decimal('NaN')
                a < b           ==> Decimal('-1')
                a == b          ==> Decimal('0')
                a > b           ==> Decimal('1')
        
        """
    def __hash__(self):
        """
        x.__hash__() <==> hash(x)
        """
    def as_tuple(self):
        """
        Represents the number as a triple tuple.

                To show the internals exactly as they are.
        
        """
    def as_integer_ratio(self):
        """
        Express a finite Decimal instance in the form n / d.

                Returns a pair (n, d) of integers.  When called on an infinity
                or NaN, raises OverflowError or ValueError respectively.

                >>> Decimal('3.14').as_integer_ratio()
                (157, 50)
                >>> Decimal('-123e5').as_integer_ratio()
                (-12300000, 1)
                >>> Decimal('0.00').as_integer_ratio()
                (0, 1)

        
        """
    def __repr__(self):
        """
        Represents the number as an instance of Decimal.
        """
    def __str__(self, eng=False, context=None):
        """
        Return string representation of the number in scientific notation.

                Captures all of the information in the underlying representation.
        
        """
    def to_eng_string(self, context=None):
        """
        Convert to a string, using engineering notation if an exponent is needed.

                Engineering notation has an exponent which is a multiple of 3.  This
                can leave up to 3 digits to the left of the decimal place and may
                require the addition of either one or two trailing zeros.
        
        """
    def __neg__(self, context=None):
        """
        Returns a copy with the sign switched.

                Rounds, if it has reason.
        
        """
    def __pos__(self, context=None):
        """
        Returns a copy, unless it is a sNaN.

                Rounds the number (if more than precision digits)
        
        """
    def __abs__(self, round=True, context=None):
        """
        Returns the absolute value of self.

                If the keyword argument 'round' is false, do not round.  The
                expression self.__abs__(round=False) is equivalent to
                self.copy_abs().
        
        """
    def __add__(self, other, context=None):
        """
        Returns self + other.

                -INF + INF (or the reverse) cause InvalidOperation errors.
        
        """
    def __sub__(self, other, context=None):
        """
        Return self - other
        """
    def __rsub__(self, other, context=None):
        """
        Return other - self
        """
    def __mul__(self, other, context=None):
        """
        Return self * other.

                (+-) INF * 0 (or its reverse) raise InvalidOperation.
        
        """
    def __truediv__(self, other, context=None):
        """
        Return self / other.
        """
    def _divide(self, other, context):
        """
        Return (self // other, self % other), to context.prec precision.

                Assumes that neither self nor other is a NaN, that self is not
                infinite and that other is nonzero.
        
        """
    def __rtruediv__(self, other, context=None):
        """
        Swaps self/other and returns __truediv__.
        """
    def __divmod__(self, other, context=None):
        """

                Return (self // other, self % other)
        
        """
    def __rdivmod__(self, other, context=None):
        """
        Swaps self/other and returns __divmod__.
        """
    def __mod__(self, other, context=None):
        """

                self % other
        
        """
    def __rmod__(self, other, context=None):
        """
        Swaps self/other and returns __mod__.
        """
    def remainder_near(self, other, context=None):
        """

                Remainder nearest to 0-  abs(remainder-near) <= other/2
        
        """
    def __floordiv__(self, other, context=None):
        """
        self // other
        """
    def __rfloordiv__(self, other, context=None):
        """
        Swaps self/other and returns __floordiv__.
        """
    def __float__(self):
        """
        Float representation.
        """
    def __int__(self):
        """
        Converts self to an int, truncating if necessary.
        """
    def real(self):
        """
        Decapitate the payload of a NaN to fit the context
        """
    def _fix(self, context):
        """
        Round if it is necessary to keep self within prec precision.

                Rounds and fixes the exponent.  Does not raise on a sNaN.

                Arguments:
                self - Decimal instance
                context - context used.
        
        """
    def _round_down(self, prec):
        """
        Also known as round-towards-0, truncate.
        """
    def _round_up(self, prec):
        """
        Rounds away from 0.
        """
    def _round_half_up(self, prec):
        """
        Rounds 5 up (away from 0)
        """
    def _round_half_down(self, prec):
        """
        Round 5 down
        """
    def _round_half_even(self, prec):
        """
        Round 5 to even, rest to nearest.
        """
    def _round_ceiling(self, prec):
        """
        Rounds up (not away from 0 if negative.)
        """
    def _round_floor(self, prec):
        """
        Rounds down (not towards 0 if negative)
        """
    def _round_05up(self, prec):
        """
        Round down unless digit prec-1 is 0 or 5.
        """
    def __round__(self, n=None):
        """
        Round self to the nearest integer, or to a given precision.

                If only one argument is supplied, round a finite Decimal
                instance self to the nearest integer.  If self is infinite or
                a NaN then a Python exception is raised.  If self is finite
                and lies exactly halfway between two integers then it is
                rounded to the integer with even last digit.

                >>> round(Decimal('123.456'))
                123
                >>> round(Decimal('-456.789'))
                -457
                >>> round(Decimal('-3.0'))
                -3
                >>> round(Decimal('2.5'))
                2
                >>> round(Decimal('3.5'))
                4
                >>> round(Decimal('Inf'))
                Traceback (most recent call last):
                  ...
                OverflowError: cannot round an infinity
                >>> round(Decimal('NaN'))
                Traceback (most recent call last):
                  ...
                ValueError: cannot round a NaN

                If a second argument n is supplied, self is rounded to n
                decimal places using the rounding mode for the current
                context.

                For an integer n, round(self, -n) is exactly equivalent to
                self.quantize(Decimal('1En')).

                >>> round(Decimal('123.456'), 0)
                Decimal('123')
                >>> round(Decimal('123.456'), 2)
                Decimal('123.46')
                >>> round(Decimal('123.456'), -2)
                Decimal('1E+2')
                >>> round(Decimal('-Infinity'), 37)
                Decimal('NaN')
                >>> round(Decimal('sNaN123'), 0)
                Decimal('NaN123')

        
        """
    def __floor__(self):
        """
        Return the floor of self, as an integer.

                For a finite Decimal instance self, return the greatest
                integer n such that n <= self.  If self is infinite or a NaN
                then a Python exception is raised.

        
        """
    def __ceil__(self):
        """
        Return the ceiling of self, as an integer.

                For a finite Decimal instance self, return the least integer n
                such that n >= self.  If self is infinite or a NaN then a
                Python exception is raised.

        
        """
    def fma(self, other, third, context=None):
        """
        Fused multiply-add.

                Returns self*other+third with no rounding of the intermediate
                product self*other.

                self and other are multiplied together, with no rounding of
                the result.  The third operand is then added to the result,
                and a single final rounding is performed.
        
        """
    def _power_modulo(self, other, modulo, context=None):
        """
        Three argument version of __pow__
        """
    def _power_exact(self, other, p):
        """
        Attempt to compute self**other exactly.

                Given Decimals self and other and an integer p, attempt to
                compute an exact result for the power self**other, with p
                digits of precision.  Return None if self**other is not
                exactly representable in p digits.

                Assumes that elimination of special cases has already been
                performed: self and other must both be nonspecial; self must
                be positive and not numerically equal to 1; other must be
                nonzero.  For efficiency, other._exp should not be too large,
                so that 10**abs(other._exp) is a feasible calculation.
        """
    def __pow__(self, other, modulo=None, context=None):
        """
        Return self ** other [ % modulo].

                With two arguments, compute self**other.

                With three arguments, compute (self**other) % modulo.  For the
                three argument form, the following restrictions on the
                arguments hold:

                 - all three arguments must be integral
                 - other must be nonnegative
                 - either self or other (or both) must be nonzero
                 - modulo must be nonzero and must have at most p digits,
                   where p is the context precision.

                If any of these restrictions is violated the InvalidOperation
                flag is raised.

                The result of pow(self, other, modulo) is identical to the
                result that would be obtained by computing (self**other) %
                modulo with unbounded precision, but is computed more
                efficiently.  It is always exact.
        
        """
    def __rpow__(self, other, context=None):
        """
        Swaps self/other and returns __pow__.
        """
    def normalize(self, context=None):
        """
        Normalize- strip trailing 0s, change anything equal to 0 to 0e0
        """
    def quantize(self, exp, rounding=None, context=None):
        """
        Quantize self so its exponent is the same as that of exp.

                Similar to self._rescale(exp._exp) but with error checking.
        
        """
    def same_quantum(self, other, context=None):
        """
        Return True if self and other have the same exponent; otherwise
                return False.

                If either operand is a special value, the following rules are used:
                   * return True if both operands are infinities
                   * return True if both operands are NaNs
                   * otherwise, return False.
        
        """
    def _rescale(self, exp, rounding):
        """
        Rescale self so that the exponent is exp, either by padding with zeros
                or by truncating digits, using the given rounding mode.

                Specials are returned without change.  This operation is
                quiet: it raises no flags, and uses no information from the
                context.

                exp = exp to scale to (an integer)
                rounding = rounding mode
        
        """
    def _round(self, places, rounding):
        """
        Round a nonzero, nonspecial Decimal to a fixed number of
                significant figures, using the given rounding mode.

                Infinities, NaNs and zeros are returned unaltered.

                This operation is quiet: it raises no flags, and uses no
                information from the context.

        
        """
    def to_integral_exact(self, rounding=None, context=None):
        """
        Rounds to a nearby integer.

                If no rounding mode is specified, take the rounding mode from
                the context.  This method raises the Rounded and Inexact flags
                when appropriate.

                See also: to_integral_value, which does exactly the same as
                this method except that it doesn't raise Inexact or Rounded.
        
        """
    def to_integral_value(self, rounding=None, context=None):
        """
        Rounds to the nearest integer, without raising inexact, rounded.
        """
    def sqrt(self, context=None):
        """
        Return the square root of self.
        """
    def max(self, other, context=None):
        """
        Returns the larger value.

                Like max(self, other) except if one is not a number, returns
                NaN (and signals if one is sNaN).  Also rounds.
        
        """
    def min(self, other, context=None):
        """
        Returns the smaller value.

                Like min(self, other) except if one is not a number, returns
                NaN (and signals if one is sNaN).  Also rounds.
        
        """
    def _isinteger(self):
        """
        Returns whether self is an integer
        """
    def _iseven(self):
        """
        Returns True if self is even.  Assumes self is an integer.
        """
    def adjusted(self):
        """
        Return the adjusted exponent of self
        """
    def canonical(self):
        """
        Returns the same Decimal object.

                As we do not have different encodings for the same number, the
                received object already is in its canonical form.
        
        """
    def compare_signal(self, other, context=None):
        """
        Compares self to the other operand numerically.

                It's pretty much like compare(), but all NaNs signal, with signaling
                NaNs taking precedence over quiet NaNs.
        
        """
    def compare_total(self, other, context=None):
        """
        Compares self to other using the abstract representations.

                This is not like the standard compare, which use their numerical
                value. Note that a total ordering is defined for all possible abstract
                representations.
        
        """
    def compare_total_mag(self, other, context=None):
        """
        Compares self to other using abstract repr., ignoring sign.

                Like compare_total, but with operand's sign ignored and assumed to be 0.
        
        """
    def copy_abs(self):
        """
        Returns a copy with the sign set to 0. 
        """
    def copy_negate(self):
        """
        Returns a copy with the sign inverted.
        """
    def copy_sign(self, other, context=None):
        """
        Returns self with the sign of other.
        """
    def exp(self, context=None):
        """
        Returns e ** self.
        """
    def is_canonical(self):
        """
        Return True if self is canonical; otherwise return False.

                Currently, the encoding of a Decimal instance is always
                canonical, so this method returns True for any Decimal.
        
        """
    def is_finite(self):
        """
        Return True if self is finite; otherwise return False.

                A Decimal instance is considered finite if it is neither
                infinite nor a NaN.
        
        """
    def is_infinite(self):
        """
        Return True if self is infinite; otherwise return False.
        """
    def is_nan(self):
        """
        Return True if self is a qNaN or sNaN; otherwise return False.
        """
    def is_normal(self, context=None):
        """
        Return True if self is a normal number; otherwise return False.
        """
    def is_qnan(self):
        """
        Return True if self is a quiet NaN; otherwise return False.
        """
    def is_signed(self):
        """
        Return True if self is negative; otherwise return False.
        """
    def is_snan(self):
        """
        Return True if self is a signaling NaN; otherwise return False.
        """
    def is_subnormal(self, context=None):
        """
        Return True if self is subnormal; otherwise return False.
        """
    def is_zero(self):
        """
        Return True if self is a zero; otherwise return False.
        """
    def _ln_exp_bound(self):
        """
        Compute a lower bound for the adjusted exponent of self.ln().
                In other words, compute r such that self.ln() >= 10**r.  Assumes
                that self is finite and positive and that self != 1.
        
        """
    def ln(self, context=None):
        """
        Returns the natural (base e) logarithm of self.
        """
    def _log10_exp_bound(self):
        """
        Compute a lower bound for the adjusted exponent of self.log10().
                In other words, find r such that self.log10() >= 10**r.
                Assumes that self is finite and positive and that self != 1.
        
        """
    def log10(self, context=None):
        """
        Returns the base 10 logarithm of self.
        """
    def logb(self, context=None):
        """
         Returns the exponent of the magnitude of self's MSD.

                The result is the integer which is the exponent of the magnitude
                of the most significant digit of self (as though it were truncated
                to a single digit while maintaining the value of that digit and
                without limiting the resulting exponent).
        
        """
    def _islogical(self):
        """
        Return True if self is a logical operand.

                For being logical, it must be a finite number with a sign of 0,
                an exponent of 0, and a coefficient whose digits must all be
                either 0 or 1.
        
        """
    def _fill_logical(self, context, opa, opb):
        """
        '0'
        """
    def logical_and(self, other, context=None):
        """
        Applies an 'and' operation between self and other's digits.
        """
    def logical_invert(self, context=None):
        """
        Invert all its digits.
        """
    def logical_or(self, other, context=None):
        """
        Applies an 'or' operation between self and other's digits.
        """
    def logical_xor(self, other, context=None):
        """
        Applies an 'xor' operation between self and other's digits.
        """
    def max_mag(self, other, context=None):
        """
        Compares the values numerically with their sign ignored.
        """
    def min_mag(self, other, context=None):
        """
        Compares the values numerically with their sign ignored.
        """
    def next_minus(self, context=None):
        """
        Returns the largest representable number smaller than itself.
        """
    def next_plus(self, context=None):
        """
        Returns the smallest representable number larger than itself.
        """
    def next_toward(self, other, context=None):
        """
        Returns the number closest to self, in the direction towards other.

                The result is the closest representable number to self
                (excluding self) that is in the direction towards other,
                unless both have the same value.  If the two operands are
                numerically equal, then the result is a copy of self with the
                sign set to be the same as the sign of other.
        
        """
    def number_class(self, context=None):
        """
        Returns an indication of the class of self.

                The class is one of the following strings:
                  sNaN
                  NaN
                  -Infinity
                  -Normal
                  -Subnormal
                  -Zero
                  +Zero
                  +Subnormal
                  +Normal
                  +Infinity
        
        """
    def radix(self):
        """
        Just returns 10, as this is Decimal, :)
        """
    def rotate(self, other, context=None):
        """
        Returns a rotated copy of self, value-of-other times.
        """
    def scaleb(self, other, context=None):
        """
        Returns self operand after adding the second value to its exp.
        """
    def shift(self, other, context=None):
        """
        Returns a shifted copy of self, value-of-other times.
        """
    def __reduce__(self):
        """
         I'm immutable; therefore I am my own clone
        """
    def __deepcopy__(self, memo):
        """
         My components are also immutable
        """
    def __format__(self, specifier, context=None, _localeconv=None):
        """
        Format a Decimal instance according to the given specifier.

                The specifier should be a standard format specifier, with the
                form described in PEP 3101.  Formatting types 'e', 'E', 'f',
                'F', 'g', 'G', 'n' and '%' are supported.  If the formatting
                type is omitted it defaults to 'g' or 'G', depending on the
                value of context.capitals.
        
        """
def _dec_from_triple(sign, coefficient, exponent, special=False):
    """
    Create a decimal instance directly, without any validation,
        normalization (e.g. removal of leading zeros) or argument
        conversion.

        This function is for *internal use only*.
    
    """
def _ContextManager(object):
    """
    Context manager class to support localcontext().

          Sets a copy of the supplied context in __enter__() and restores
          the previous decimal context in __exit__()
    
    """
    def __init__(self, new_context):
        """
        Contains the context for a Decimal instance.

            Contains:
            prec - precision (for use in rounding, division, square roots..)
            rounding - rounding type (how you round)
            traps - If traps[exception] = 1, then the exception is
                            raised when it is caused.  Otherwise, a value is
                            substituted in.
            flags  - When an exception is caused, flags[exception] is set.
                     (Whether or not the trap_enabler is set)
                     Should be reset by user of Decimal instance.
            Emin -   Minimum exponent
            Emax -   Maximum exponent
            capitals -      If 1, 1*10^1 is printed as 1E+1.
                            If 0, printed as 1e1
            clamp -  If 1, change exponents if too high (Default 0)
    
        """
2021-03-02 20:54:29,435 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:29,435 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, prec=None, rounding=None, Emin=None, Emax=None,
                       capitals=None, clamp=None, flags=None, traps=None,
                       _ignored_flags=None):
        """
         Set defaults; for everything except flags and _ignored_flags,
         inherit from DefaultContext.

        """
    def _set_integer_check(self, name, value, vmin, vmax):
        """
        %s must be an integer
        """
    def _set_signal_dict(self, name, d):
        """
        %s must be a signal dict
        """
    def __setattr__(self, name, value):
        """
        'prec'
        """
    def __delattr__(self, name):
        """
        %s cannot be deleted
        """
    def __reduce__(self):
        """
        Show the current context.
        """
    def clear_flags(self):
        """
        Reset all flags to zero
        """
    def clear_traps(self):
        """
        Reset all traps to zero
        """
    def _shallow_copy(self):
        """
        Returns a shallow copy from self.
        """
    def copy(self):
        """
        Returns a deep copy from self.
        """
    def _raise_error(self, condition, explanation = None, *args):
        """
        Handles an error

                If the flag is in _ignored_flags, returns the default response.
                Otherwise, it sets the flag, then, if the corresponding
                trap_enabler is set, it reraises the exception.  Otherwise, it returns
                the default value after setting the flag.
        
        """
    def _ignore_all_flags(self):
        """
        Ignore all flags, if they are raised
        """
    def _ignore_flags(self, *flags):
        """
        Ignore the flags, if they are raised
        """
    def _regard_flags(self, *flags):
        """
        Stop ignoring the flags, if they are raised
        """
    def Etiny(self):
        """
        Returns Etiny (= Emin - prec + 1)
        """
    def Etop(self):
        """
        Returns maximum exponent (= Emax - prec + 1)
        """
    def _set_rounding(self, type):
        """
        Sets the rounding type.

                Sets the rounding type, and returns the current (previous)
                rounding type.  Often used like:

                context = context.copy()
                # so you don't change the calling context
                # if an error occurs in the middle.
                rounding = context._set_rounding(ROUND_UP)
                val = self.__sub__(other, context=context)
                context._set_rounding(rounding)

                This will make it round up for that operation.
        
        """
    def create_decimal(self, num='0'):
        """
        Creates a new Decimal instance but using self as context.

                This method implements the to-number operation of the
                IBM Decimal specification.
        """
    def create_decimal_from_float(self, f):
        """
        Creates a new Decimal instance from a float but rounding using self
                as the context.

                >>> context = Context(prec=5, rounding=ROUND_DOWN)
                >>> context.create_decimal_from_float(3.1415926535897932)
                Decimal('3.1415')
                >>> context = Context(prec=5, traps=[Inexact])
                >>> context.create_decimal_from_float(3.1415926535897932)
                Traceback (most recent call last):
                    ...
                decimal.Inexact: None

        
        """
    def abs(self, a):
        """
        Returns the absolute value of the operand.

                If the operand is negative, the result is the same as using the minus
                operation on the operand.  Otherwise, the result is the same as using
                the plus operation on the operand.

                >>> ExtendedContext.abs(Decimal('2.1'))
                Decimal('2.1')
                >>> ExtendedContext.abs(Decimal('-100'))
                Decimal('100')
                >>> ExtendedContext.abs(Decimal('101.5'))
                Decimal('101.5')
                >>> ExtendedContext.abs(Decimal('-101.5'))
                Decimal('101.5')
                >>> ExtendedContext.abs(-1)
                Decimal('1')
        
        """
    def add(self, a, b):
        """
        Return the sum of the two operands.

                >>> ExtendedContext.add(Decimal('12'), Decimal('7.00'))
                Decimal('19.00')
                >>> ExtendedContext.add(Decimal('1E+2'), Decimal('1.01E+4'))
                Decimal('1.02E+4')
                >>> ExtendedContext.add(1, Decimal(2))
                Decimal('3')
                >>> ExtendedContext.add(Decimal(8), 5)
                Decimal('13')
                >>> ExtendedContext.add(5, 5)
                Decimal('10')
        
        """
    def _apply(self, a):
        """
        Returns the same Decimal object.

                As we do not have different encodings for the same number, the
                received object already is in its canonical form.

                >>> ExtendedContext.canonical(Decimal('2.50'))
                Decimal('2.50')
        
        """
    def compare(self, a, b):
        """
        Compares values numerically.

                If the signs of the operands differ, a value representing each operand
                ('-1' if the operand is less than zero, '0' if the operand is zero or
                negative zero, or '1' if the operand is greater than zero) is used in
                place of that operand for the comparison instead of the actual
                operand.

                The comparison is then effected by subtracting the second operand from
                the first and then returning a value according to the result of the
                subtraction: '-1' if the result is less than zero, '0' if the result is
                zero or negative zero, or '1' if the result is greater than zero.

                >>> ExtendedContext.compare(Decimal('2.1'), Decimal('3'))
                Decimal('-1')
                >>> ExtendedContext.compare(Decimal('2.1'), Decimal('2.1'))
                Decimal('0')
                >>> ExtendedContext.compare(Decimal('2.1'), Decimal('2.10'))
                Decimal('0')
                >>> ExtendedContext.compare(Decimal('3'), Decimal('2.1'))
                Decimal('1')
                >>> ExtendedContext.compare(Decimal('2.1'), Decimal('-3'))
                Decimal('1')
                >>> ExtendedContext.compare(Decimal('-3'), Decimal('2.1'))
                Decimal('-1')
                >>> ExtendedContext.compare(1, 2)
                Decimal('-1')
                >>> ExtendedContext.compare(Decimal(1), 2)
                Decimal('-1')
                >>> ExtendedContext.compare(1, Decimal(2))
                Decimal('-1')
        
        """
    def compare_signal(self, a, b):
        """
        Compares the values of the two operands numerically.

                It's pretty much like compare(), but all NaNs signal, with signaling
                NaNs taking precedence over quiet NaNs.

                >>> c = ExtendedContext
                >>> c.compare_signal(Decimal('2.1'), Decimal('3'))
                Decimal('-1')
                >>> c.compare_signal(Decimal('2.1'), Decimal('2.1'))
                Decimal('0')
                >>> c.flags[InvalidOperation] = 0
                >>> print(c.flags[InvalidOperation])
                0
                >>> c.compare_signal(Decimal('NaN'), Decimal('2.1'))
                Decimal('NaN')
                >>> print(c.flags[InvalidOperation])
                1
                >>> c.flags[InvalidOperation] = 0
                >>> print(c.flags[InvalidOperation])
                0
                >>> c.compare_signal(Decimal('sNaN'), Decimal('2.1'))
                Decimal('NaN')
                >>> print(c.flags[InvalidOperation])
                1
                >>> c.compare_signal(-1, 2)
                Decimal('-1')
                >>> c.compare_signal(Decimal(-1), 2)
                Decimal('-1')
                >>> c.compare_signal(-1, Decimal(2))
                Decimal('-1')
        
        """
    def compare_total(self, a, b):
        """
        Compares two operands using their abstract representation.

                This is not like the standard compare, which use their numerical
                value. Note that a total ordering is defined for all possible abstract
                representations.

                >>> ExtendedContext.compare_total(Decimal('12.73'), Decimal('127.9'))
                Decimal('-1')
                >>> ExtendedContext.compare_total(Decimal('-127'),  Decimal('12'))
                Decimal('-1')
                >>> ExtendedContext.compare_total(Decimal('12.30'), Decimal('12.3'))
                Decimal('-1')
                >>> ExtendedContext.compare_total(Decimal('12.30'), Decimal('12.30'))
                Decimal('0')
                >>> ExtendedContext.compare_total(Decimal('12.3'),  Decimal('12.300'))
                Decimal('1')
                >>> ExtendedContext.compare_total(Decimal('12.3'),  Decimal('NaN'))
                Decimal('-1')
                >>> ExtendedContext.compare_total(1, 2)
                Decimal('-1')
                >>> ExtendedContext.compare_total(Decimal(1), 2)
                Decimal('-1')
                >>> ExtendedContext.compare_total(1, Decimal(2))
                Decimal('-1')
        
        """
    def compare_total_mag(self, a, b):
        """
        Compares two operands using their abstract representation ignoring sign.

                Like compare_total, but with operand's sign ignored and assumed to be 0.
        
        """
    def copy_abs(self, a):
        """
        Returns a copy of the operand with the sign set to 0.

                >>> ExtendedContext.copy_abs(Decimal('2.1'))
                Decimal('2.1')
                >>> ExtendedContext.copy_abs(Decimal('-100'))
                Decimal('100')
                >>> ExtendedContext.copy_abs(-1)
                Decimal('1')
        
        """
    def copy_decimal(self, a):
        """
        Returns a copy of the decimal object.

                >>> ExtendedContext.copy_decimal(Decimal('2.1'))
                Decimal('2.1')
                >>> ExtendedContext.copy_decimal(Decimal('-1.00'))
                Decimal('-1.00')
                >>> ExtendedContext.copy_decimal(1)
                Decimal('1')
        
        """
    def copy_negate(self, a):
        """
        Returns a copy of the operand with the sign inverted.

                >>> ExtendedContext.copy_negate(Decimal('101.5'))
                Decimal('-101.5')
                >>> ExtendedContext.copy_negate(Decimal('-101.5'))
                Decimal('101.5')
                >>> ExtendedContext.copy_negate(1)
                Decimal('-1')
        
        """
    def copy_sign(self, a, b):
        """
        Copies the second operand's sign to the first one.

                In detail, it returns a copy of the first operand with the sign
                equal to the sign of the second operand.

                >>> ExtendedContext.copy_sign(Decimal( '1.50'), Decimal('7.33'))
                Decimal('1.50')
                >>> ExtendedContext.copy_sign(Decimal('-1.50'), Decimal('7.33'))
                Decimal('1.50')
                >>> ExtendedContext.copy_sign(Decimal( '1.50'), Decimal('-7.33'))
                Decimal('-1.50')
                >>> ExtendedContext.copy_sign(Decimal('-1.50'), Decimal('-7.33'))
                Decimal('-1.50')
                >>> ExtendedContext.copy_sign(1, -2)
                Decimal('-1')
                >>> ExtendedContext.copy_sign(Decimal(1), -2)
                Decimal('-1')
                >>> ExtendedContext.copy_sign(1, Decimal(-2))
                Decimal('-1')
        
        """
    def divide(self, a, b):
        """
        Decimal division in a specified context.

                >>> ExtendedContext.divide(Decimal('1'), Decimal('3'))
                Decimal('0.333333333')
                >>> ExtendedContext.divide(Decimal('2'), Decimal('3'))
                Decimal('0.666666667')
                >>> ExtendedContext.divide(Decimal('5'), Decimal('2'))
                Decimal('2.5')
                >>> ExtendedContext.divide(Decimal('1'), Decimal('10'))
                Decimal('0.1')
                >>> ExtendedContext.divide(Decimal('12'), Decimal('12'))
                Decimal('1')
                >>> ExtendedContext.divide(Decimal('8.00'), Decimal('2'))
                Decimal('4.00')
                >>> ExtendedContext.divide(Decimal('2.400'), Decimal('2.0'))
                Decimal('1.20')
                >>> ExtendedContext.divide(Decimal('1000'), Decimal('100'))
                Decimal('10')
                >>> ExtendedContext.divide(Decimal('1000'), Decimal('1'))
                Decimal('1000')
                >>> ExtendedContext.divide(Decimal('2.40E+6'), Decimal('2'))
                Decimal('1.20E+6')
                >>> ExtendedContext.divide(5, 5)
                Decimal('1')
                >>> ExtendedContext.divide(Decimal(5), 5)
                Decimal('1')
                >>> ExtendedContext.divide(5, Decimal(5))
                Decimal('1')
        
        """
    def divide_int(self, a, b):
        """
        Divides two numbers and returns the integer part of the result.

                >>> ExtendedContext.divide_int(Decimal('2'), Decimal('3'))
                Decimal('0')
                >>> ExtendedContext.divide_int(Decimal('10'), Decimal('3'))
                Decimal('3')
                >>> ExtendedContext.divide_int(Decimal('1'), Decimal('0.3'))
                Decimal('3')
                >>> ExtendedContext.divide_int(10, 3)
                Decimal('3')
                >>> ExtendedContext.divide_int(Decimal(10), 3)
                Decimal('3')
                >>> ExtendedContext.divide_int(10, Decimal(3))
                Decimal('3')
        
        """
    def divmod(self, a, b):
        """
        Return (a // b, a % b).

                >>> ExtendedContext.divmod(Decimal(8), Decimal(3))
                (Decimal('2'), Decimal('2'))
                >>> ExtendedContext.divmod(Decimal(8), Decimal(4))
                (Decimal('2'), Decimal('0'))
                >>> ExtendedContext.divmod(8, 4)
                (Decimal('2'), Decimal('0'))
                >>> ExtendedContext.divmod(Decimal(8), 4)
                (Decimal('2'), Decimal('0'))
                >>> ExtendedContext.divmod(8, Decimal(4))
                (Decimal('2'), Decimal('0'))
        
        """
    def exp(self, a):
        """
        Returns e ** a.

                >>> c = ExtendedContext.copy()
                >>> c.Emin = -999
                >>> c.Emax = 999
                >>> c.exp(Decimal('-Infinity'))
                Decimal('0')
                >>> c.exp(Decimal('-1'))
                Decimal('0.367879441')
                >>> c.exp(Decimal('0'))
                Decimal('1')
                >>> c.exp(Decimal('1'))
                Decimal('2.71828183')
                >>> c.exp(Decimal('0.693147181'))
                Decimal('2.00000000')
                >>> c.exp(Decimal('+Infinity'))
                Decimal('Infinity')
                >>> c.exp(10)
                Decimal('22026.4658')
        
        """
    def fma(self, a, b, c):
        """
        Returns a multiplied by b, plus c.

                The first two operands are multiplied together, using multiply,
                the third operand is then added to the result of that
                multiplication, using add, all with only one final rounding.

                >>> ExtendedContext.fma(Decimal('3'), Decimal('5'), Decimal('7'))
                Decimal('22')
                >>> ExtendedContext.fma(Decimal('3'), Decimal('-5'), Decimal('7'))
                Decimal('-8')
                >>> ExtendedContext.fma(Decimal('888565290'), Decimal('1557.96930'), Decimal('-86087.7578'))
                Decimal('1.38435736E+12')
                >>> ExtendedContext.fma(1, 3, 4)
                Decimal('7')
                >>> ExtendedContext.fma(1, Decimal(3), 4)
                Decimal('7')
                >>> ExtendedContext.fma(1, 3, Decimal(4))
                Decimal('7')
        
        """
    def is_canonical(self, a):
        """
        Return True if the operand is canonical; otherwise return False.

                Currently, the encoding of a Decimal instance is always
                canonical, so this method returns True for any Decimal.

                >>> ExtendedContext.is_canonical(Decimal('2.50'))
                True
        
        """
    def is_finite(self, a):
        """
        Return True if the operand is finite; otherwise return False.

                A Decimal instance is considered finite if it is neither
                infinite nor a NaN.

                >>> ExtendedContext.is_finite(Decimal('2.50'))
                True
                >>> ExtendedContext.is_finite(Decimal('-0.3'))
                True
                >>> ExtendedContext.is_finite(Decimal('0'))
                True
                >>> ExtendedContext.is_finite(Decimal('Inf'))
                False
                >>> ExtendedContext.is_finite(Decimal('NaN'))
                False
                >>> ExtendedContext.is_finite(1)
                True
        
        """
    def is_infinite(self, a):
        """
        Return True if the operand is infinite; otherwise return False.

                >>> ExtendedContext.is_infinite(Decimal('2.50'))
                False
                >>> ExtendedContext.is_infinite(Decimal('-Inf'))
                True
                >>> ExtendedContext.is_infinite(Decimal('NaN'))
                False
                >>> ExtendedContext.is_infinite(1)
                False
        
        """
    def is_nan(self, a):
        """
        Return True if the operand is a qNaN or sNaN;
                otherwise return False.

                >>> ExtendedContext.is_nan(Decimal('2.50'))
                False
                >>> ExtendedContext.is_nan(Decimal('NaN'))
                True
                >>> ExtendedContext.is_nan(Decimal('-sNaN'))
                True
                >>> ExtendedContext.is_nan(1)
                False
        
        """
    def is_normal(self, a):
        """
        Return True if the operand is a normal number;
                otherwise return False.

                >>> c = ExtendedContext.copy()
                >>> c.Emin = -999
                >>> c.Emax = 999
                >>> c.is_normal(Decimal('2.50'))
                True
                >>> c.is_normal(Decimal('0.1E-999'))
                False
                >>> c.is_normal(Decimal('0.00'))
                False
                >>> c.is_normal(Decimal('-Inf'))
                False
                >>> c.is_normal(Decimal('NaN'))
                False
                >>> c.is_normal(1)
                True
        
        """
    def is_qnan(self, a):
        """
        Return True if the operand is a quiet NaN; otherwise return False.

                >>> ExtendedContext.is_qnan(Decimal('2.50'))
                False
                >>> ExtendedContext.is_qnan(Decimal('NaN'))
                True
                >>> ExtendedContext.is_qnan(Decimal('sNaN'))
                False
                >>> ExtendedContext.is_qnan(1)
                False
        
        """
    def is_signed(self, a):
        """
        Return True if the operand is negative; otherwise return False.

                >>> ExtendedContext.is_signed(Decimal('2.50'))
                False
                >>> ExtendedContext.is_signed(Decimal('-12'))
                True
                >>> ExtendedContext.is_signed(Decimal('-0'))
                True
                >>> ExtendedContext.is_signed(8)
                False
                >>> ExtendedContext.is_signed(-8)
                True
        
        """
    def is_snan(self, a):
        """
        Return True if the operand is a signaling NaN;
                otherwise return False.

                >>> ExtendedContext.is_snan(Decimal('2.50'))
                False
                >>> ExtendedContext.is_snan(Decimal('NaN'))
                False
                >>> ExtendedContext.is_snan(Decimal('sNaN'))
                True
                >>> ExtendedContext.is_snan(1)
                False
        
        """
    def is_subnormal(self, a):
        """
        Return True if the operand is subnormal; otherwise return False.

                >>> c = ExtendedContext.copy()
                >>> c.Emin = -999
                >>> c.Emax = 999
                >>> c.is_subnormal(Decimal('2.50'))
                False
                >>> c.is_subnormal(Decimal('0.1E-999'))
                True
                >>> c.is_subnormal(Decimal('0.00'))
                False
                >>> c.is_subnormal(Decimal('-Inf'))
                False
                >>> c.is_subnormal(Decimal('NaN'))
                False
                >>> c.is_subnormal(1)
                False
        
        """
    def is_zero(self, a):
        """
        Return True if the operand is a zero; otherwise return False.

                >>> ExtendedContext.is_zero(Decimal('0'))
                True
                >>> ExtendedContext.is_zero(Decimal('2.50'))
                False
                >>> ExtendedContext.is_zero(Decimal('-0E+2'))
                True
                >>> ExtendedContext.is_zero(1)
                False
                >>> ExtendedContext.is_zero(0)
                True
        
        """
    def ln(self, a):
        """
        Returns the natural (base e) logarithm of the operand.

                >>> c = ExtendedContext.copy()
                >>> c.Emin = -999
                >>> c.Emax = 999
                >>> c.ln(Decimal('0'))
                Decimal('-Infinity')
                >>> c.ln(Decimal('1.000'))
                Decimal('0')
                >>> c.ln(Decimal('2.71828183'))
                Decimal('1.00000000')
                >>> c.ln(Decimal('10'))
                Decimal('2.30258509')
                >>> c.ln(Decimal('+Infinity'))
                Decimal('Infinity')
                >>> c.ln(1)
                Decimal('0')
        
        """
    def log10(self, a):
        """
        Returns the base 10 logarithm of the operand.

                >>> c = ExtendedContext.copy()
                >>> c.Emin = -999
                >>> c.Emax = 999
                >>> c.log10(Decimal('0'))
                Decimal('-Infinity')
                >>> c.log10(Decimal('0.001'))
                Decimal('-3')
                >>> c.log10(Decimal('1.000'))
                Decimal('0')
                >>> c.log10(Decimal('2'))
                Decimal('0.301029996')
                >>> c.log10(Decimal('10'))
                Decimal('1')
                >>> c.log10(Decimal('70'))
                Decimal('1.84509804')
                >>> c.log10(Decimal('+Infinity'))
                Decimal('Infinity')
                >>> c.log10(0)
                Decimal('-Infinity')
                >>> c.log10(1)
                Decimal('0')
        
        """
    def logb(self, a):
        """
         Returns the exponent of the magnitude of the operand's MSD.

                The result is the integer which is the exponent of the magnitude
                of the most significant digit of the operand (as though the
                operand were truncated to a single digit while maintaining the
                value of that digit and without limiting the resulting exponent).

                >>> ExtendedContext.logb(Decimal('250'))
                Decimal('2')
                >>> ExtendedContext.logb(Decimal('2.50'))
                Decimal('0')
                >>> ExtendedContext.logb(Decimal('0.03'))
                Decimal('-2')
                >>> ExtendedContext.logb(Decimal('0'))
                Decimal('-Infinity')
                >>> ExtendedContext.logb(1)
                Decimal('0')
                >>> ExtendedContext.logb(10)
                Decimal('1')
                >>> ExtendedContext.logb(100)
                Decimal('2')
        
        """
    def logical_and(self, a, b):
        """
        Applies the logical operation 'and' between each operand's digits.

                The operands must be both logical numbers.

                >>> ExtendedContext.logical_and(Decimal('0'), Decimal('0'))
                Decimal('0')
                >>> ExtendedContext.logical_and(Decimal('0'), Decimal('1'))
                Decimal('0')
                >>> ExtendedContext.logical_and(Decimal('1'), Decimal('0'))
                Decimal('0')
                >>> ExtendedContext.logical_and(Decimal('1'), Decimal('1'))
                Decimal('1')
                >>> ExtendedContext.logical_and(Decimal('1100'), Decimal('1010'))
                Decimal('1000')
                >>> ExtendedContext.logical_and(Decimal('1111'), Decimal('10'))
                Decimal('10')
                >>> ExtendedContext.logical_and(110, 1101)
                Decimal('100')
                >>> ExtendedContext.logical_and(Decimal(110), 1101)
                Decimal('100')
                >>> ExtendedContext.logical_and(110, Decimal(1101))
                Decimal('100')
        
        """
    def logical_invert(self, a):
        """
        Invert all the digits in the operand.

                The operand must be a logical number.

                >>> ExtendedContext.logical_invert(Decimal('0'))
                Decimal('111111111')
                >>> ExtendedContext.logical_invert(Decimal('1'))
                Decimal('111111110')
                >>> ExtendedContext.logical_invert(Decimal('111111111'))
                Decimal('0')
                >>> ExtendedContext.logical_invert(Decimal('101010101'))
                Decimal('10101010')
                >>> ExtendedContext.logical_invert(1101)
                Decimal('111110010')
        
        """
    def logical_or(self, a, b):
        """
        Applies the logical operation 'or' between each operand's digits.

                The operands must be both logical numbers.

                >>> ExtendedContext.logical_or(Decimal('0'), Decimal('0'))
                Decimal('0')
                >>> ExtendedContext.logical_or(Decimal('0'), Decimal('1'))
                Decimal('1')
                >>> ExtendedContext.logical_or(Decimal('1'), Decimal('0'))
                Decimal('1')
                >>> ExtendedContext.logical_or(Decimal('1'), Decimal('1'))
                Decimal('1')
                >>> ExtendedContext.logical_or(Decimal('1100'), Decimal('1010'))
                Decimal('1110')
                >>> ExtendedContext.logical_or(Decimal('1110'), Decimal('10'))
                Decimal('1110')
                >>> ExtendedContext.logical_or(110, 1101)
                Decimal('1111')
                >>> ExtendedContext.logical_or(Decimal(110), 1101)
                Decimal('1111')
                >>> ExtendedContext.logical_or(110, Decimal(1101))
                Decimal('1111')
        
        """
    def logical_xor(self, a, b):
        """
        Applies the logical operation 'xor' between each operand's digits.

                The operands must be both logical numbers.

                >>> ExtendedContext.logical_xor(Decimal('0'), Decimal('0'))
                Decimal('0')
                >>> ExtendedContext.logical_xor(Decimal('0'), Decimal('1'))
                Decimal('1')
                >>> ExtendedContext.logical_xor(Decimal('1'), Decimal('0'))
                Decimal('1')
                >>> ExtendedContext.logical_xor(Decimal('1'), Decimal('1'))
                Decimal('0')
                >>> ExtendedContext.logical_xor(Decimal('1100'), Decimal('1010'))
                Decimal('110')
                >>> ExtendedContext.logical_xor(Decimal('1111'), Decimal('10'))
                Decimal('1101')
                >>> ExtendedContext.logical_xor(110, 1101)
                Decimal('1011')
                >>> ExtendedContext.logical_xor(Decimal(110), 1101)
                Decimal('1011')
                >>> ExtendedContext.logical_xor(110, Decimal(1101))
                Decimal('1011')
        
        """
    def max(self, a, b):
        """
        max compares two values numerically and returns the maximum.

                If either operand is a NaN then the general rules apply.
                Otherwise, the operands are compared as though by the compare
                operation.  If they are numerically equal then the left-hand operand
                is chosen as the result.  Otherwise the maximum (closer to positive
                infinity) of the two operands is chosen as the result.

                >>> ExtendedContext.max(Decimal('3'), Decimal('2'))
                Decimal('3')
                >>> ExtendedContext.max(Decimal('-10'), Decimal('3'))
                Decimal('3')
                >>> ExtendedContext.max(Decimal('1.0'), Decimal('1'))
                Decimal('1')
                >>> ExtendedContext.max(Decimal('7'), Decimal('NaN'))
                Decimal('7')
                >>> ExtendedContext.max(1, 2)
                Decimal('2')
                >>> ExtendedContext.max(Decimal(1), 2)
                Decimal('2')
                >>> ExtendedContext.max(1, Decimal(2))
                Decimal('2')
        
        """
    def max_mag(self, a, b):
        """
        Compares the values numerically with their sign ignored.

                >>> ExtendedContext.max_mag(Decimal('7'), Decimal('NaN'))
                Decimal('7')
                >>> ExtendedContext.max_mag(Decimal('7'), Decimal('-10'))
                Decimal('-10')
                >>> ExtendedContext.max_mag(1, -2)
                Decimal('-2')
                >>> ExtendedContext.max_mag(Decimal(1), -2)
                Decimal('-2')
                >>> ExtendedContext.max_mag(1, Decimal(-2))
                Decimal('-2')
        
        """
    def min(self, a, b):
        """
        min compares two values numerically and returns the minimum.

                If either operand is a NaN then the general rules apply.
                Otherwise, the operands are compared as though by the compare
                operation.  If they are numerically equal then the left-hand operand
                is chosen as the result.  Otherwise the minimum (closer to negative
                infinity) of the two operands is chosen as the result.

                >>> ExtendedContext.min(Decimal('3'), Decimal('2'))
                Decimal('2')
                >>> ExtendedContext.min(Decimal('-10'), Decimal('3'))
                Decimal('-10')
                >>> ExtendedContext.min(Decimal('1.0'), Decimal('1'))
                Decimal('1.0')
                >>> ExtendedContext.min(Decimal('7'), Decimal('NaN'))
                Decimal('7')
                >>> ExtendedContext.min(1, 2)
                Decimal('1')
                >>> ExtendedContext.min(Decimal(1), 2)
                Decimal('1')
                >>> ExtendedContext.min(1, Decimal(29))
                Decimal('1')
        
        """
    def min_mag(self, a, b):
        """
        Compares the values numerically with their sign ignored.

                >>> ExtendedContext.min_mag(Decimal('3'), Decimal('-2'))
                Decimal('-2')
                >>> ExtendedContext.min_mag(Decimal('-3'), Decimal('NaN'))
                Decimal('-3')
                >>> ExtendedContext.min_mag(1, -2)
                Decimal('1')
                >>> ExtendedContext.min_mag(Decimal(1), -2)
                Decimal('1')
                >>> ExtendedContext.min_mag(1, Decimal(-2))
                Decimal('1')
        
        """
    def minus(self, a):
        """
        Minus corresponds to unary prefix minus in Python.

                The operation is evaluated using the same rules as subtract; the
                operation minus(a) is calculated as subtract('0', a) where the '0'
                has the same exponent as the operand.

                >>> ExtendedContext.minus(Decimal('1.3'))
                Decimal('-1.3')
                >>> ExtendedContext.minus(Decimal('-1.3'))
                Decimal('1.3')
                >>> ExtendedContext.minus(1)
                Decimal('-1')
        
        """
    def multiply(self, a, b):
        """
        multiply multiplies two operands.

                If either operand is a special value then the general rules apply.
                Otherwise, the operands are multiplied together
                ('long multiplication'), resulting in a number which may be as long as
                the sum of the lengths of the two operands.

                >>> ExtendedContext.multiply(Decimal('1.20'), Decimal('3'))
                Decimal('3.60')
                >>> ExtendedContext.multiply(Decimal('7'), Decimal('3'))
                Decimal('21')
                >>> ExtendedContext.multiply(Decimal('0.9'), Decimal('0.8'))
                Decimal('0.72')
                >>> ExtendedContext.multiply(Decimal('0.9'), Decimal('-0'))
                Decimal('-0.0')
                >>> ExtendedContext.multiply(Decimal('654321'), Decimal('654321'))
                Decimal('4.28135971E+11')
                >>> ExtendedContext.multiply(7, 7)
                Decimal('49')
                >>> ExtendedContext.multiply(Decimal(7), 7)
                Decimal('49')
                >>> ExtendedContext.multiply(7, Decimal(7))
                Decimal('49')
        
        """
    def next_minus(self, a):
        """
        Returns the largest representable number smaller than a.

                >>> c = ExtendedContext.copy()
                >>> c.Emin = -999
                >>> c.Emax = 999
                >>> ExtendedContext.next_minus(Decimal('1'))
                Decimal('0.999999999')
                >>> c.next_minus(Decimal('1E-1007'))
                Decimal('0E-1007')
                >>> ExtendedContext.next_minus(Decimal('-1.00000003'))
                Decimal('-1.00000004')
                >>> c.next_minus(Decimal('Infinity'))
                Decimal('9.99999999E+999')
                >>> c.next_minus(1)
                Decimal('0.999999999')
        
        """
    def next_plus(self, a):
        """
        Returns the smallest representable number larger than a.

                >>> c = ExtendedContext.copy()
                >>> c.Emin = -999
                >>> c.Emax = 999
                >>> ExtendedContext.next_plus(Decimal('1'))
                Decimal('1.00000001')
                >>> c.next_plus(Decimal('-1E-1007'))
                Decimal('-0E-1007')
                >>> ExtendedContext.next_plus(Decimal('-1.00000003'))
                Decimal('-1.00000002')
                >>> c.next_plus(Decimal('-Infinity'))
                Decimal('-9.99999999E+999')
                >>> c.next_plus(1)
                Decimal('1.00000001')
        
        """
    def next_toward(self, a, b):
        """
        Returns the number closest to a, in direction towards b.

                The result is the closest representable number from the first
                operand (but not the first operand) that is in the direction
                towards the second operand, unless the operands have the same
                value.

                >>> c = ExtendedContext.copy()
                >>> c.Emin = -999
                >>> c.Emax = 999
                >>> c.next_toward(Decimal('1'), Decimal('2'))
                Decimal('1.00000001')
                >>> c.next_toward(Decimal('-1E-1007'), Decimal('1'))
                Decimal('-0E-1007')
                >>> c.next_toward(Decimal('-1.00000003'), Decimal('0'))
                Decimal('-1.00000002')
                >>> c.next_toward(Decimal('1'), Decimal('0'))
                Decimal('0.999999999')
                >>> c.next_toward(Decimal('1E-1007'), Decimal('-100'))
                Decimal('0E-1007')
                >>> c.next_toward(Decimal('-1.00000003'), Decimal('-10'))
                Decimal('-1.00000004')
                >>> c.next_toward(Decimal('0.00'), Decimal('-0.0000'))
                Decimal('-0.00')
                >>> c.next_toward(0, 1)
                Decimal('1E-1007')
                >>> c.next_toward(Decimal(0), 1)
                Decimal('1E-1007')
                >>> c.next_toward(0, Decimal(1))
                Decimal('1E-1007')
        
        """
    def normalize(self, a):
        """
        normalize reduces an operand to its simplest form.

                Essentially a plus operation with all trailing zeros removed from the
                result.

                >>> ExtendedContext.normalize(Decimal('2.1'))
                Decimal('2.1')
                >>> ExtendedContext.normalize(Decimal('-2.0'))
                Decimal('-2')
                >>> ExtendedContext.normalize(Decimal('1.200'))
                Decimal('1.2')
                >>> ExtendedContext.normalize(Decimal('-120'))
                Decimal('-1.2E+2')
                >>> ExtendedContext.normalize(Decimal('120.00'))
                Decimal('1.2E+2')
                >>> ExtendedContext.normalize(Decimal('0.00'))
                Decimal('0')
                >>> ExtendedContext.normalize(6)
                Decimal('6')
        
        """
    def number_class(self, a):
        """
        Returns an indication of the class of the operand.

                The class is one of the following strings:
                  -sNaN
                  -NaN
                  -Infinity
                  -Normal
                  -Subnormal
                  -Zero
                  +Zero
                  +Subnormal
                  +Normal
                  +Infinity

                >>> c = ExtendedContext.copy()
                >>> c.Emin = -999
                >>> c.Emax = 999
                >>> c.number_class(Decimal('Infinity'))
                '+Infinity'
                >>> c.number_class(Decimal('1E-10'))
                '+Normal'
                >>> c.number_class(Decimal('2.50'))
                '+Normal'
                >>> c.number_class(Decimal('0.1E-999'))
                '+Subnormal'
                >>> c.number_class(Decimal('0'))
                '+Zero'
                >>> c.number_class(Decimal('-0'))
                '-Zero'
                >>> c.number_class(Decimal('-0.1E-999'))
                '-Subnormal'
                >>> c.number_class(Decimal('-1E-10'))
                '-Normal'
                >>> c.number_class(Decimal('-2.50'))
                '-Normal'
                >>> c.number_class(Decimal('-Infinity'))
                '-Infinity'
                >>> c.number_class(Decimal('NaN'))
                'NaN'
                >>> c.number_class(Decimal('-NaN'))
                'NaN'
                >>> c.number_class(Decimal('sNaN'))
                'sNaN'
                >>> c.number_class(123)
                '+Normal'
        
        """
    def plus(self, a):
        """
        Plus corresponds to unary prefix plus in Python.

                The operation is evaluated using the same rules as add; the
                operation plus(a) is calculated as add('0', a) where the '0'
                has the same exponent as the operand.

                >>> ExtendedContext.plus(Decimal('1.3'))
                Decimal('1.3')
                >>> ExtendedContext.plus(Decimal('-1.3'))
                Decimal('-1.3')
                >>> ExtendedContext.plus(-1)
                Decimal('-1')
        
        """
    def power(self, a, b, modulo=None):
        """
        Raises a to the power of b, to modulo if given.

                With two arguments, compute a**b.  If a is negative then b
                must be integral.  The result will be inexact unless b is
                integral and the result is finite and can be expressed exactly
                in 'precision' digits.

                With three arguments, compute (a**b) % modulo.  For the
                three argument form, the following restrictions on the
                arguments hold:

                 - all three arguments must be integral
                 - b must be nonnegative
                 - at least one of a or b must be nonzero
                 - modulo must be nonzero and have at most 'precision' digits

                The result of pow(a, b, modulo) is identical to the result
                that would be obtained by computing (a**b) % modulo with
                unbounded precision, but is computed more efficiently.  It is
                always exact.

                >>> c = ExtendedContext.copy()
                >>> c.Emin = -999
                >>> c.Emax = 999
                >>> c.power(Decimal('2'), Decimal('3'))
                Decimal('8')
                >>> c.power(Decimal('-2'), Decimal('3'))
                Decimal('-8')
                >>> c.power(Decimal('2'), Decimal('-3'))
                Decimal('0.125')
                >>> c.power(Decimal('1.7'), Decimal('8'))
                Decimal('69.7575744')
                >>> c.power(Decimal('10'), Decimal('0.301029996'))
                Decimal('2.00000000')
                >>> c.power(Decimal('Infinity'), Decimal('-1'))
                Decimal('0')
                >>> c.power(Decimal('Infinity'), Decimal('0'))
                Decimal('1')
                >>> c.power(Decimal('Infinity'), Decimal('1'))
                Decimal('Infinity')
                >>> c.power(Decimal('-Infinity'), Decimal('-1'))
                Decimal('-0')
                >>> c.power(Decimal('-Infinity'), Decimal('0'))
                Decimal('1')
                >>> c.power(Decimal('-Infinity'), Decimal('1'))
                Decimal('-Infinity')
                >>> c.power(Decimal('-Infinity'), Decimal('2'))
                Decimal('Infinity')
                >>> c.power(Decimal('0'), Decimal('0'))
                Decimal('NaN')

                >>> c.power(Decimal('3'), Decimal('7'), Decimal('16'))
                Decimal('11')
                >>> c.power(Decimal('-3'), Decimal('7'), Decimal('16'))
                Decimal('-11')
                >>> c.power(Decimal('-3'), Decimal('8'), Decimal('16'))
                Decimal('1')
                >>> c.power(Decimal('3'), Decimal('7'), Decimal('-16'))
                Decimal('11')
                >>> c.power(Decimal('23E12345'), Decimal('67E189'), Decimal('123456789'))
                Decimal('11729830')
                >>> c.power(Decimal('-0'), Decimal('17'), Decimal('1729'))
                Decimal('-0')
                >>> c.power(Decimal('-23'), Decimal('0'), Decimal('65537'))
                Decimal('1')
                >>> ExtendedContext.power(7, 7)
                Decimal('823543')
                >>> ExtendedContext.power(Decimal(7), 7)
                Decimal('823543')
                >>> ExtendedContext.power(7, Decimal(7), 2)
                Decimal('1')
        
        """
    def quantize(self, a, b):
        """
        Returns a value equal to 'a' (rounded), having the exponent of 'b'.

                The coefficient of the result is derived from that of the left-hand
                operand.  It may be rounded using the current rounding setting (if the
                exponent is being increased), multiplied by a positive power of ten (if
                the exponent is being decreased), or is unchanged (if the exponent is
                already equal to that of the right-hand operand).

                Unlike other operations, if the length of the coefficient after the
                quantize operation would be greater than precision then an Invalid
                operation condition is raised.  This guarantees that, unless there is
                an error condition, the exponent of the result of a quantize is always
                equal to that of the right-hand operand.

                Also unlike other operations, quantize will never raise Underflow, even
                if the result is subnormal and inexact.

                >>> ExtendedContext.quantize(Decimal('2.17'), Decimal('0.001'))
                Decimal('2.170')
                >>> ExtendedContext.quantize(Decimal('2.17'), Decimal('0.01'))
                Decimal('2.17')
                >>> ExtendedContext.quantize(Decimal('2.17'), Decimal('0.1'))
                Decimal('2.2')
                >>> ExtendedContext.quantize(Decimal('2.17'), Decimal('1e+0'))
                Decimal('2')
                >>> ExtendedContext.quantize(Decimal('2.17'), Decimal('1e+1'))
                Decimal('0E+1')
                >>> ExtendedContext.quantize(Decimal('-Inf'), Decimal('Infinity'))
                Decimal('-Infinity')
                >>> ExtendedContext.quantize(Decimal('2'), Decimal('Infinity'))
                Decimal('NaN')
                >>> ExtendedContext.quantize(Decimal('-0.1'), Decimal('1'))
                Decimal('-0')
                >>> ExtendedContext.quantize(Decimal('-0'), Decimal('1e+5'))
                Decimal('-0E+5')
                >>> ExtendedContext.quantize(Decimal('+35236450.6'), Decimal('1e-2'))
                Decimal('NaN')
                >>> ExtendedContext.quantize(Decimal('-35236450.6'), Decimal('1e-2'))
                Decimal('NaN')
                >>> ExtendedContext.quantize(Decimal('217'), Decimal('1e-1'))
                Decimal('217.0')
                >>> ExtendedContext.quantize(Decimal('217'), Decimal('1e-0'))
                Decimal('217')
                >>> ExtendedContext.quantize(Decimal('217'), Decimal('1e+1'))
                Decimal('2.2E+2')
                >>> ExtendedContext.quantize(Decimal('217'), Decimal('1e+2'))
                Decimal('2E+2')
                >>> ExtendedContext.quantize(1, 2)
                Decimal('1')
                >>> ExtendedContext.quantize(Decimal(1), 2)
                Decimal('1')
                >>> ExtendedContext.quantize(1, Decimal(2))
                Decimal('1')
        
        """
    def radix(self):
        """
        Just returns 10, as this is Decimal, :)

                >>> ExtendedContext.radix()
                Decimal('10')
        
        """
    def remainder(self, a, b):
        """
        Returns the remainder from integer division.

                The result is the residue of the dividend after the operation of
                calculating integer division as described for divide-integer, rounded
                to precision digits if necessary.  The sign of the result, if
                non-zero, is the same as that of the original dividend.

                This operation will fail under the same conditions as integer division
                (that is, if integer division on the same two operands would fail, the
                remainder cannot be calculated).

                >>> ExtendedContext.remainder(Decimal('2.1'), Decimal('3'))
                Decimal('2.1')
                >>> ExtendedContext.remainder(Decimal('10'), Decimal('3'))
                Decimal('1')
                >>> ExtendedContext.remainder(Decimal('-10'), Decimal('3'))
                Decimal('-1')
                >>> ExtendedContext.remainder(Decimal('10.2'), Decimal('1'))
                Decimal('0.2')
                >>> ExtendedContext.remainder(Decimal('10'), Decimal('0.3'))
                Decimal('0.1')
                >>> ExtendedContext.remainder(Decimal('3.6'), Decimal('1.3'))
                Decimal('1.0')
                >>> ExtendedContext.remainder(22, 6)
                Decimal('4')
                >>> ExtendedContext.remainder(Decimal(22), 6)
                Decimal('4')
                >>> ExtendedContext.remainder(22, Decimal(6))
                Decimal('4')
        
        """
    def remainder_near(self, a, b):
        """
        Returns to be "a - b * n", where n is the integer nearest the exact
                value of "x / b" (if two integers are equally near then the even one
                is chosen).  If the result is equal to 0 then its sign will be the
                sign of a.

                This operation will fail under the same conditions as integer division
                (that is, if integer division on the same two operands would fail, the
                remainder cannot be calculated).

                >>> ExtendedContext.remainder_near(Decimal('2.1'), Decimal('3'))
                Decimal('-0.9')
                >>> ExtendedContext.remainder_near(Decimal('10'), Decimal('6'))
                Decimal('-2')
                >>> ExtendedContext.remainder_near(Decimal('10'), Decimal('3'))
                Decimal('1')
                >>> ExtendedContext.remainder_near(Decimal('-10'), Decimal('3'))
                Decimal('-1')
                >>> ExtendedContext.remainder_near(Decimal('10.2'), Decimal('1'))
                Decimal('0.2')
                >>> ExtendedContext.remainder_near(Decimal('10'), Decimal('0.3'))
                Decimal('0.1')
                >>> ExtendedContext.remainder_near(Decimal('3.6'), Decimal('1.3'))
                Decimal('-0.3')
                >>> ExtendedContext.remainder_near(3, 11)
                Decimal('3')
                >>> ExtendedContext.remainder_near(Decimal(3), 11)
                Decimal('3')
                >>> ExtendedContext.remainder_near(3, Decimal(11))
                Decimal('3')
        
        """
    def rotate(self, a, b):
        """
        Returns a rotated copy of a, b times.

                The coefficient of the result is a rotated copy of the digits in
                the coefficient of the first operand.  The number of places of
                rotation is taken from the absolute value of the second operand,
                with the rotation being to the left if the second operand is
                positive or to the right otherwise.

                >>> ExtendedContext.rotate(Decimal('34'), Decimal('8'))
                Decimal('400000003')
                >>> ExtendedContext.rotate(Decimal('12'), Decimal('9'))
                Decimal('12')
                >>> ExtendedContext.rotate(Decimal('123456789'), Decimal('-2'))
                Decimal('891234567')
                >>> ExtendedContext.rotate(Decimal('123456789'), Decimal('0'))
                Decimal('123456789')
                >>> ExtendedContext.rotate(Decimal('123456789'), Decimal('+2'))
                Decimal('345678912')
                >>> ExtendedContext.rotate(1333333, 1)
                Decimal('13333330')
                >>> ExtendedContext.rotate(Decimal(1333333), 1)
                Decimal('13333330')
                >>> ExtendedContext.rotate(1333333, Decimal(1))
                Decimal('13333330')
        
        """
    def same_quantum(self, a, b):
        """
        Returns True if the two operands have the same exponent.

                The result is never affected by either the sign or the coefficient of
                either operand.

                >>> ExtendedContext.same_quantum(Decimal('2.17'), Decimal('0.001'))
                False
                >>> ExtendedContext.same_quantum(Decimal('2.17'), Decimal('0.01'))
                True
                >>> ExtendedContext.same_quantum(Decimal('2.17'), Decimal('1'))
                False
                >>> ExtendedContext.same_quantum(Decimal('Inf'), Decimal('-Inf'))
                True
                >>> ExtendedContext.same_quantum(10000, -1)
                True
                >>> ExtendedContext.same_quantum(Decimal(10000), -1)
                True
                >>> ExtendedContext.same_quantum(10000, Decimal(-1))
                True
        
        """
    def scaleb (self, a, b):
        """
        Returns the first operand after adding the second value its exp.

                >>> ExtendedContext.scaleb(Decimal('7.50'), Decimal('-2'))
                Decimal('0.0750')
                >>> ExtendedContext.scaleb(Decimal('7.50'), Decimal('0'))
                Decimal('7.50')
                >>> ExtendedContext.scaleb(Decimal('7.50'), Decimal('3'))
                Decimal('7.50E+3')
                >>> ExtendedContext.scaleb(1, 4)
                Decimal('1E+4')
                >>> ExtendedContext.scaleb(Decimal(1), 4)
                Decimal('1E+4')
                >>> ExtendedContext.scaleb(1, Decimal(4))
                Decimal('1E+4')
        
        """
    def shift(self, a, b):
        """
        Returns a shifted copy of a, b times.

                The coefficient of the result is a shifted copy of the digits
                in the coefficient of the first operand.  The number of places
                to shift is taken from the absolute value of the second operand,
                with the shift being to the left if the second operand is
                positive or to the right otherwise.  Digits shifted into the
                coefficient are zeros.

                >>> ExtendedContext.shift(Decimal('34'), Decimal('8'))
                Decimal('400000000')
                >>> ExtendedContext.shift(Decimal('12'), Decimal('9'))
                Decimal('0')
                >>> ExtendedContext.shift(Decimal('123456789'), Decimal('-2'))
                Decimal('1234567')
                >>> ExtendedContext.shift(Decimal('123456789'), Decimal('0'))
                Decimal('123456789')
                >>> ExtendedContext.shift(Decimal('123456789'), Decimal('+2'))
                Decimal('345678900')
                >>> ExtendedContext.shift(88888888, 2)
                Decimal('888888800')
                >>> ExtendedContext.shift(Decimal(88888888), 2)
                Decimal('888888800')
                >>> ExtendedContext.shift(88888888, Decimal(2))
                Decimal('888888800')
        
        """
    def sqrt(self, a):
        """
        Square root of a non-negative number to context precision.

                If the result must be inexact, it is rounded using the round-half-even
                algorithm.

                >>> ExtendedContext.sqrt(Decimal('0'))
                Decimal('0')
                >>> ExtendedContext.sqrt(Decimal('-0'))
                Decimal('-0')
                >>> ExtendedContext.sqrt(Decimal('0.39'))
                Decimal('0.624499800')
                >>> ExtendedContext.sqrt(Decimal('100'))
                Decimal('10')
                >>> ExtendedContext.sqrt(Decimal('1'))
                Decimal('1')
                >>> ExtendedContext.sqrt(Decimal('1.0'))
                Decimal('1.0')
                >>> ExtendedContext.sqrt(Decimal('1.00'))
                Decimal('1.0')
                >>> ExtendedContext.sqrt(Decimal('7'))
                Decimal('2.64575131')
                >>> ExtendedContext.sqrt(Decimal('10'))
                Decimal('3.16227766')
                >>> ExtendedContext.sqrt(2)
                Decimal('1.41421356')
                >>> ExtendedContext.prec
                9
        
        """
    def subtract(self, a, b):
        """
        Return the difference between the two operands.

                >>> ExtendedContext.subtract(Decimal('1.3'), Decimal('1.07'))
                Decimal('0.23')
                >>> ExtendedContext.subtract(Decimal('1.3'), Decimal('1.30'))
                Decimal('0.00')
                >>> ExtendedContext.subtract(Decimal('1.3'), Decimal('2.07'))
                Decimal('-0.77')
                >>> ExtendedContext.subtract(8, 5)
                Decimal('3')
                >>> ExtendedContext.subtract(Decimal(8), 5)
                Decimal('3')
                >>> ExtendedContext.subtract(8, Decimal(5))
                Decimal('3')
        
        """
    def to_eng_string(self, a):
        """
        Convert to a string, using engineering notation if an exponent is needed.

                Engineering notation has an exponent which is a multiple of 3.  This
                can leave up to 3 digits to the left of the decimal place and may
                require the addition of either one or two trailing zeros.

                The operation is not affected by the context.

                >>> ExtendedContext.to_eng_string(Decimal('123E+1'))
                '1.23E+3'
                >>> ExtendedContext.to_eng_string(Decimal('123E+3'))
                '123E+3'
                >>> ExtendedContext.to_eng_string(Decimal('123E-10'))
                '12.3E-9'
                >>> ExtendedContext.to_eng_string(Decimal('-123E-12'))
                '-123E-12'
                >>> ExtendedContext.to_eng_string(Decimal('7E-7'))
                '700E-9'
                >>> ExtendedContext.to_eng_string(Decimal('7E+1'))
                '70'
                >>> ExtendedContext.to_eng_string(Decimal('0E+1'))
                '0.00E+3'

        
        """
    def to_sci_string(self, a):
        """
        Converts a number to a string, using scientific notation.

                The operation is not affected by the context.
        
        """
    def to_integral_exact(self, a):
        """
        Rounds to an integer.

                When the operand has a negative exponent, the result is the same
                as using the quantize() operation using the given operand as the
                left-hand-operand, 1E+0 as the right-hand-operand, and the precision
                of the operand as the precision setting; Inexact and Rounded flags
                are allowed in this operation.  The rounding mode is taken from the
                context.

                >>> ExtendedContext.to_integral_exact(Decimal('2.1'))
                Decimal('2')
                >>> ExtendedContext.to_integral_exact(Decimal('100'))
                Decimal('100')
                >>> ExtendedContext.to_integral_exact(Decimal('100.0'))
                Decimal('100')
                >>> ExtendedContext.to_integral_exact(Decimal('101.5'))
                Decimal('102')
                >>> ExtendedContext.to_integral_exact(Decimal('-101.5'))
                Decimal('-102')
                >>> ExtendedContext.to_integral_exact(Decimal('10E+5'))
                Decimal('1.0E+6')
                >>> ExtendedContext.to_integral_exact(Decimal('7.89E+77'))
                Decimal('7.89E+77')
                >>> ExtendedContext.to_integral_exact(Decimal('-Inf'))
                Decimal('-Infinity')
        
        """
    def to_integral_value(self, a):
        """
        Rounds to an integer.

                When the operand has a negative exponent, the result is the same
                as using the quantize() operation using the given operand as the
                left-hand-operand, 1E+0 as the right-hand-operand, and the precision
                of the operand as the precision setting, except that no flags will
                be set.  The rounding mode is taken from the context.

                >>> ExtendedContext.to_integral_value(Decimal('2.1'))
                Decimal('2')
                >>> ExtendedContext.to_integral_value(Decimal('100'))
                Decimal('100')
                >>> ExtendedContext.to_integral_value(Decimal('100.0'))
                Decimal('100')
                >>> ExtendedContext.to_integral_value(Decimal('101.5'))
                Decimal('102')
                >>> ExtendedContext.to_integral_value(Decimal('-101.5'))
                Decimal('-102')
                >>> ExtendedContext.to_integral_value(Decimal('10E+5'))
                Decimal('1.0E+6')
                >>> ExtendedContext.to_integral_value(Decimal('7.89E+77'))
                Decimal('7.89E+77')
                >>> ExtendedContext.to_integral_value(Decimal('-Inf'))
                Decimal('-Infinity')
        
        """
def _WorkRep(object):
    """
    'sign'
    """
    def __init__(self, value=None):
        """
         assert isinstance(value, tuple)

        """
    def __repr__(self):
        """
        (%r, %r, %r)
        """
def _normalize(op1, op2, prec = 0):
    """
    Normalizes op1, op2 to have the same exp and length of coefficient.

        Done during addition.
    
    """
def _decimal_lshift_exact(n, e):
    """
     Given integers n and e, return n * 10**e if it's an integer, else None.

        The computation is designed to avoid computing large powers of 10
        unnecessarily.

        >>> _decimal_lshift_exact(3, 4)
        30000
        >>> _decimal_lshift_exact(300, -999999999)  # returns None

    
    """
def _sqrt_nearest(n, a):
    """
    Closest integer to the square root of the positive integer n.  a is
        an initial approximation to the square root.  Any positive integer
        will do for a, but the closer a is to the square root of n the
        faster convergence will be.

    
    """
def _rshift_nearest(x, shift):
    """
    Given an integer x and a nonnegative integer shift, return closest
        integer to x / 2**shift; use round-to-even in case of a tie.

    
    """
def _div_nearest(a, b):
    """
    Closest integer to a/b, a and b positive integers; rounds to even
        in the case of a tie.

    
    """
def _ilog(x, M, L = 8):
    """
    Integer approximation to M*log(x/M), with absolute error boundable
        in terms only of x/M.

        Given positive integers x and M, return an integer approximation to
        M * log(x/M).  For L = 8 and 0.1 <= x/M <= 10 the difference
        between the approximation and the exact result is at most 22.  For
        L = 8 and 1.0 <= x/M <= 10.0 the difference is at most 15.  In
        both cases these are upper bounds on the error; it will usually be
        much smaller.
    """
def _dlog10(c, e, p):
    """
    Given integers c, e and p with c > 0, p >= 0, compute an integer
        approximation to 10**p * log10(c*10**e), with an absolute error of
        at most 1.  Assumes that c*10**e is not exactly 1.
    """
def _dlog(c, e, p):
    """
    Given integers c, e and p with c > 0, compute an integer
        approximation to 10**p * log(c*10**e), with an absolute error of
        at most 1.  Assumes that c*10**e is not exactly 1.
    """
def _Log10Memoize(object):
    """
    Class to compute, store, and allow retrieval of, digits of the
        constant log(10) = 2.302585....  This constant is needed by
        Decimal.ln, Decimal.log10, Decimal.exp and Decimal.__pow__.
    """
    def __init__(self):
        """
        23025850929940456840179914546843642076011014886
        """
    def getdigits(self, p):
        """
        Given an integer p >= 0, return floor(10**p)*log(10).

                For example, self.getdigits(3) returns 2302.
        
        """
def _iexp(x, M, L=8):
    """
    Given integers x and M, M > 0, such that x/M is small in absolute
        value, compute an integer approximation to M*exp(x/M).  For 0 <=
        x/M <= 2.4, the absolute error in the result is bounded by 60 (and
        is usually much smaller).
    """
def _dexp(c, e, p):
    """
    Compute an approximation to exp(c*10**e), with p decimal places of
        precision.

        Returns integers d, f such that:

          10**(p-1) <= d <= 10**p, and
          (d-1)*10**f < exp(c*10**e) < (d+1)*10**f

        In other words, d*10**f is an approximation to exp(c*10**e) with p
        digits of precision, and with an error in d of at most 1.  This is
        almost, but not quite, the same as the error being < 1ulp: when d
        = 10**(p-1) the error could be up to 10 ulp.
    """
def _dpower(xc, xe, yc, ye, p):
    """
    Given integers xc, xe, yc and ye representing Decimals x = xc*10**xe and
        y = yc*10**ye, compute x**y.  Returns a pair of integers (c, e) such that:

          10**(p-1) <= c <= 10**p, and
          (c-1)*10**e < x**y < (c+1)*10**e

        in other words, c*10**e is an approximation to x**y with p digits
        of precision, and with an error in c of at most 1.  (This is
        almost, but not quite, the same as the error being < 1ulp: when c
        == 10**(p-1) we can only guarantee error < 10ulp.)

        We assume that: x is positive and not equal to 1, and y is nonzero.
    
    """
2021-03-02 20:54:29,466 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:29,467 : INFO : tokenize_signature : --> do i ever get here?
def _log10_lb(c, correction = {
        '1': 100, '2': 70, '3': 53, '4': 40, '5': 31,
        '6': 23, '7': 16, '8': 10, '9': 5}):
    """
    Compute a lower bound for 100*log10(c) for a positive integer c.
    """
def _convert_other(other, raiseit=False, allow_float=False):
    """
    Convert other to Decimal.

        Verifies that it's ok to use in an implicit construction.
        If allow_float is true, allow conversion from float;  this
        is used in the comparison methods (__eq__ and friends).

    
    """
def _convert_for_comparison(self, other, equality_op=False):
    """
    Given a Decimal instance self and a Python object other, return
        a pair (s, o) of Decimal instances such that "s op o" is
        equivalent to "self op other" for any of the 6 comparison
        operators "op".

    
    """
def _parse_format_specifier(format_spec, _localeconv=None):
    """
    Parse and validate a format specifier.

        Turns a standard numeric format specifier into a dict, with the
        following entries:

          fill: fill character to pad field to minimum width
          align: alignment type, either '<', '>', '=' or '^'
          sign: either '+', '-' or ' '
          minimumwidth: nonnegative integer giving minimum width
          zeropad: boolean, indicating whether to pad with zeros
          thousands_sep: string to use as thousands separator, or ''
          grouping: grouping for thousands separators, in format
            used by localeconv
          decimal_point: string to use for decimal point
          precision: nonnegative integer giving precision, or None
          type: one of the characters 'eEfFgG%', or None

    
    """
def _format_align(sign, body, spec):
    """
    Given an unpadded, non-aligned numeric string 'body' and sign
        string 'sign', add padding and alignment conforming to the given
        format specifier dictionary 'spec' (as produced by
        parse_format_specifier).

    
    """
def _group_lengths(grouping):
    """
    Convert a localeconv-style grouping into a (possibly infinite)
        iterable of integers representing group lengths.

    
    """
def _insert_thousands_sep(digits, spec, min_width=1):
    """
    Insert thousands separators into a digit string.

        spec is a dictionary whose keys should include 'thousands_sep' and
        'grouping'; typically it's the result of parsing the format
        specifier using _parse_format_specifier.

        The min_width keyword argument gives the minimum length of the
        result, which will be padded on the left with zeros if necessary.

        If necessary, the zero padding adds an extra '0' on the left to
        avoid a leading thousands separator.  For example, inserting
        commas every three digits in '123456', with min_width=8, gives
        '0,123,456', even though that has length 9.

    
    """
def _format_sign(is_negative, spec):
    """
    Determine sign character.
    """
def _format_number(is_negative, intpart, fracpart, exp, spec):
    """
    Format a number, given the following data:

        is_negative: true if the number is negative, else false
        intpart: string of digits that must appear before the decimal point
        fracpart: string of digits that must come after the point
        exp: exponent, as an integer
        spec: dictionary resulting from parsing the format specifier

        This function uses the information in spec to:
          insert separators (decimal separator and thousands separators)
          format the sign
          format the exponent
          add trailing '%' for the '%' type
          zero-pad if necessary
          fill and align if necessary
    
    """
