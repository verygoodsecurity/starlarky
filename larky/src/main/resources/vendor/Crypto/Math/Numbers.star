# ===================================================================
#
# Copyright (c) 2014, Legrandin <helderijs@gmail.com>
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
# 1. Redistributions of source code must retain the above copyright
#    notice, this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in
#    the documentation and/or other materials provided with the
#    distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
# ===================================================================
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//types", types="types")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/Util/number", long_to_bytes="long_to_bytes", bytes_to_long="bytes_to_long")
load("@vendor//Crypto/Util/py3compat", bord="bord", bchr="bchr")
load("@vendor//option/result", Result="Result", Ok="Ok", Error="Error")

_WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION
_implementation = {"library": "jdk", "api": "starlarky"}


def _Integer(value):
    """A class to model a natural integer (including zero)"""
    self = None # will be re-defined

    # Conversions
    def __int__():
        return self._value

    # Only Python 3.x
    def __index__():
        return int(self._value)

    def __str__():
        return str(__int__())

    def __repr__():
        return "%s(_value=%s)" % (self.__name__, self.__index__())

    def to_bytes(block_size=0):
        if self._value < 0:
            fail('ValueError("Conversion only valid for non-negative numbers")')
        result = long_to_bytes(self._value, block_size)
        if (len(result) > block_size) and (block_size > 0):
            fail('ValueError("Value too large to encode")')
        return result

    def from_bytes(byte_string):
        return Integer(bytes_to_long(byte_string))

    # Rich comparisons
    def __eq__(term):
        if term == None:
            return False
        term = operator.index(term)
        return self._value == term

    def __lt__(term):
        if term == None:
            return False
        term = operator.index(term)
        return self._value < term

    def __ne__(term):
        return not __eq__(term)

    def __le__(term):
        return __lt__(term) or __eq__(term)

    def __gt__(term):
        return not __le__(term)

    def __ge__(term):
        return not __lt__(term)

    def __nonzero__():
        return self._value != 0
    __bool__ = __nonzero__

    def is_negative():
        return self._value < 0

    # Arithmetic operations
    def __add__(term):
        return self.__class__(self._value + int(term))

    def __sub__(term):
        return self.__class__(self._value - int(term))

    def __mul__(factor):
        return self.__class__(self._value * int(factor))

    def __floordiv__(divisor):
        return self.__class__(self._value // int(divisor))

    def __mod__(divisor):
        divisor_value = int(divisor)
        if divisor_value < 0:
            return Error("ValueError: Modulus must be positive").unwrap()
        return self.__class__(self._value % divisor_value)

    def inplace_pow(exponent, modulus=None):
        exp_value = int(exponent)
        if exp_value < 0:
            fail('ValueError("Exponent must not be negative")')

        if modulus != None:
            mod_value = modulus.__int__()
            if mod_value < 0:
                fail('ValueError("Modulus must be positive")')
            if mod_value == 0:
                fail('ZeroDivisionError("Modulus cannot be zero")')
        else:
            mod_value = None
        self._value = pow(self._value, exp_value, mod_value)
        return self

    def __pow__(exponent, modulus=None):
        return self.inplace_pow(exponent, modulus)

    def __abs__():
        return abs(self._value)

    def sqrt(modulus=None):
        value = self._value
        if modulus == None:
            if value < 0:
                fail('ValueError("Square root of negative value")')
            # http://stackoverflow.com/questions/15390807/integer-square-root-in-python
            x = value
            y = (x + 1) // 2
            for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
                if y >= x:
                    break
                x = y
                y = (x + value // x) // 2
            result = x
        else:
            if modulus <= 0:
                fail('ValueError("Modulus must be positive")')
            result = self._tonelli_shanks(self % modulus, modulus)
        self._value = result
        return self

    def __iadd__(term):
        term = int(term)
        self._value += term
        return self

    def __isub__(term):
        term = int(term)
        self._value -= term
        return self

    def __imul__(term):
        term = int(term)
        self._value *= term
        return self

    def __imod__(term):
        term = int(term)
        modulus = term
        if modulus == 0:
            fail('ZeroDivisionError("Division by zero")')
        if modulus < 0:
            fail('ValueError("Modulus must be positive")')
        self._value %= modulus
        return self

    # Boolean/bit operations
    def __and__(term):
        # term = term.__int__() if types.is_instance(term, Integer) else int(term)
        return Integer(self._value & int(term))

    def __or__(term):
        # term = term.__int__() if types.is_instance(term, Integer) else int(term)
        return Integer(self._value | int(term))

    def __rshift__(pos):
        res = Result.Ok(self._value).map(lambda x: x >> int(pos))
        if res.is_err:
            if self._value >= 0:
                return 0
            else:
                return -1
        return Integer(res.unwrap())

    def __lshift__(pos):
        res = Result.Ok(self._value).map(lambda x: x << int(pos))
        return Integer(res.unwrap_or("Incorrect shift count"))

    def get_bit(n):
        if self._value < 0:
            fail('ValueError: no bit representation for negative values')
        result = (self._value >> n._value) & 1
        if n._value < 0:
            fail('ValueError: negative bit count')

        # result = (_value >> n) & 1
        # if n < 0:
        #     fail(' ValueError("negative bit count")')
        # except OverflowError:
        #     result = 0
        return result

    # Extra
    def is_odd():
        return (self._value & 1) == 1

    def is_even():
        return (self._value & 1) == 0

    def size_in_bits():
        if self._value < 0:
            fail('ValueError: Conversion only valid for non-negative numbers')

        if self._value == 0:
            return 1

        return _JCrypto.Math.bit_length(self._value)
        # bit_size = 0
        # tmp = self._value
        # for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
        #     if not tmp:
        #         break
        #     tmp >>= 1
        #     bit_size += 1
        #
        # return bit_size

    def size_in_bytes():
        return (size_in_bits() - 1) // 8 + 1

    def is_perfect_square():
        if self._value < 0:
            return False
        if self._value in (0, 1):
            return True

        x = self._value // 2
        square_x = pow(x, 2)
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if square_x <= self._value:
                break
            x = (square_x + self._value) // (2 * x)
            square_x = pow(x, 2)

        return self._value == pow(x, 2)

    def fail_if_divisible_by(small_prime):
        small_prime = int(small_prime)
        if (self._value % small_prime) == 0:
            fail(' ValueError("Value is composite")')

    def multiply_accumulate(a, b):
        self._value += (int(a) * int(b))
        return self

    def set(source):
        self._value = int(source)

    def inplace_inverse(modulus):
        modulus = operator.index(modulus)
        if modulus == 0:
            fail('ZeroDivisionError: Modulus cannot be zero')
        if modulus < 0:
            fail('ValueError: Modulus cannot be negative')
        r_p, r_n = self._value, modulus
        s_p, s_n = 1, 0
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if r_n <= 0:
                break
            q = r_p // r_n
            r_p, r_n = r_n, r_p - q * r_n
            s_p, s_n = s_n, s_p - q * s_n
        if r_p != 1:
            fail('ValueError: No inverse value can be computed: "' + str(r_p))
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if s_p >= 0:
                break
            s_p += modulus
        self._value = s_p
        return self

    def inverse(modulus):
        result = Integer(self)
        result.inplace_inverse(modulus)
        return result

    def gcd(term):
        term = int(term)
        r_p, r_n = abs(self._value), abs(term)
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if r_n <= 0:
                break
            q = r_p // r_n
            r_p, r_n = r_n, r_p - q * r_n
        return Integer(r_p)

    def lcm(term):
        term = int(term)
        if self._value == 0 or term == 0:
            return Integer(0)
        return Integer(_JCrypto.Math.lcm(self._value, term))
        # return Integer(abs((self._value * term) // self.gcd(term)._value))

    def jacobi_symbol(a, n):
        a = int(a)
        n = int(n)

        if n <= 0:
            fail('ValueError: n must be a positive integer')

        if (n & 1) == 0:
            fail('ValueError: n must be even for the Jacobi symbol')
        return _JCrypto.Math.jacobi_number(a, n)
        #
        # # Step 1
        # a = a % n
        # # Step 2
        # if a == 1 or n == 1:
        #     return 1
        # # Step 3
        # if a == 0:
        #     return 0
        # # Step 4
        # e = 0
        # a1 = a
        # for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
        #     if (a1 & 1) != 0:
        #         break
        #     a1 >>= 1
        #     e += 1
        # # Step 5
        # if (e & 1) == 0:
        #     s = 1
        # elif n % 8 in (1, 7):
        #     s = 1
        # else:
        #     s = -1
        # # Step 6
        # if n % 4 == 3 and a1 % 4 == 3:
        #     s = -s
        # # Step 7
        # n1 = n % a1
        # # Step 8
        # return s * self.jacobi_symbol(n1, a1)

    def __init__(value):
        if types.is_float(value):
            fail('ValueError("A floating point type is not a natural number")')
        if hasattr(value, '_value'):
            value = value._value
        return larky.mutablestruct(
            __name__='Integer',
            __class__=_Integer,
            _value=value,
            __int__=__int__,
            __index__=__index__,
            __str__=__str__,
            __repr__=__repr__,
            to_bytes=to_bytes,
            from_bytes=from_bytes,
            __eq__=__eq__,
            __lt__=__lt__,
            __ne__=__ne__,
            __le__=__le__,
            __gt__=__gt__,
            __ge__=__ge__,
            __nonzero__=__nonzero__,
            __bool__=__bool__,
            is_negative=is_negative,
            __add__=__add__,
            __sub__=__sub__,
            __mul__=__mul__,
            __floordiv__=__floordiv__,
            __mod__=__mod__,
            inplace_pow=inplace_pow,
            __pow__=__pow__,
            __abs__=__abs__,
            sqrt=sqrt,
            __iadd__=__iadd__,
            __isub__=__isub__,
            __imul__=__imul__,
            __imod__=__imod__,
            __and__=__and__,
            __or__=__or__,
            __rshift__=__rshift__,
            __lshift__=__lshift__,
            get_bit=get_bit,
            is_odd=is_odd,
            is_even=is_even,
            size_in_bits=size_in_bits,
            size_in_bytes=size_in_bytes,
            is_perfect_square=is_perfect_square,
            fail_if_divisible_by=fail_if_divisible_by,
            multiply_accumulate=multiply_accumulate,
            set=set,
            inplace_inverse=inplace_inverse,
            inverse=inverse,
            gcd=gcd,
            lcm=lcm,
            jacobi_symbol=jacobi_symbol,
        )
    self = __init__(value)
    return self


def _from_bytes(byte_string):
    return _Integer(bytes_to_long(byte_string))


def _random(**kwargs):
    """Generate a random natural integer of a certain size.
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

    exact_bits = kwargs.pop("exact_bits", None)
    max_bits = kwargs.pop("max_bits", None)
    randfunc = kwargs.pop("randfunc", None)

    if randfunc == None:
        randfunc = Random.new().read

    if exact_bits == None and max_bits == None:
        return Error("Either 'exact_bits' or 'max_bits' must be specified").unwrap()

    if exact_bits != None and max_bits != None:
        return Error("'exact_bits' and 'max_bits' are mutually exclusive").unwrap()

    bits = exact_bits or max_bits
    bytes_needed = ((bits - 1) // 8) + 1
    significant_bits_msb = 8 - (bytes_needed * 8 - bits)
    msb = bord(randfunc(1)[0])
    if exact_bits != None:
        msb |= 1 << (significant_bits_msb - 1)
    msb &= (1 << significant_bits_msb) - 1

    return _from_bytes(bchr(msb) + randfunc(bytes_needed - 1))


def _random_range(**kwargs):
    """Generate a random integer within a given internal.
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

    min_inclusive = kwargs.pop("min_inclusive", None)
    max_inclusive = kwargs.pop("max_inclusive", None)
    max_exclusive = kwargs.pop("max_exclusive", None)
    randfunc = kwargs.pop("randfunc", None)

    if kwargs:
        return Error("ValueError: Unknown keywords: " + str(kwargs.keys())).unwrap()
    if None not in (max_inclusive, max_exclusive):
        return Error("ValueError: max_inclusive and max_exclusive cannot be both" +
                     " specified").unwrap()
    if max_exclusive != None:
        max_inclusive = operator.sub(max_exclusive, 1)
    if None in (min_inclusive, max_inclusive):
        return Error("ValueError: Missing keyword to identify the interval").unwrap()

    if randfunc == None:
        randfunc = Random.new().read

    norm_maximum = operator.sub(max_inclusive, min_inclusive)
    bits_needed = _Integer(norm_maximum).size_in_bits()

    norm_candidate = -1
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if (operator.le(0, norm_candidate) and
                operator.le(norm_candidate, norm_maximum)):
            break
        norm_candidate = _random(
                                max_bits=bits_needed,
                                randfunc=randfunc
                                )
    return operator.add(norm_candidate, min_inclusive)


Numbers = larky.struct(
    __name__='Numbers',
    Integer=larky.struct(
        __call__=_Integer,
        __name__='Integer',
        from_bytes=_from_bytes,
        random_range=_random_range,
        random=_random
    )
)

Integer = Numbers.Integer