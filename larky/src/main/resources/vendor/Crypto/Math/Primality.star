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

"""Functions to create and test prime numbers.

:undocumented: __package__
"""

load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@stdlib//sets", sets="sets")
load("@stdlib//types", types="types")
load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/Math/Numbers", Integer="Integer")
load("@vendor//Crypto/Util/number", _sieve_base_large="sieve_base")
load("@vendor//Crypto/Util/py3compat", iter_range="iter_range")
load("@vendor//option/result", safe="safe", Result="Result", Error="Error")

COMPOSITE = 0
PROBABLY_PRIME = 1

map = builtins.map


def miller_rabin_test(candidate, iterations, randfunc=None):
    """Perform a Miller-Rabin primality test on an integer.

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
    # return _JCrypto.Math.is_prime(int(candidate), 1e-30)
    return _JCrypto.Math.is_prime(int(candidate), 1e-30)
    if not builtins.isinstance(candidate, Integer):
        candidate = Integer(candidate)

    if candidate in (1, 2, 3, 5):
        return PROBABLY_PRIME

    if candidate.is_even():
        return COMPOSITE

    one = Integer(1)
    minus_one = Integer(candidate - 1)

    if randfunc == None:
        randfunc = Random.new().read

    # Step 1 and 2
    m = Integer(minus_one)
    a = 0
    for _while_ in larky.while_true():
        if not m.is_even():
            break
        m >>= 1
        a += 1

    # Skip step 3

    # Step 4
    for i in iter_range(iterations):

        # Step 4.1-2
        base = 1
        for _while_ in larky.while_true():
            if base not in (one, minus_one):
                break
            base = Integer.random_range(min_inclusive=2,
                    max_inclusive=candidate - 2,
                    randfunc=randfunc)
            if not ((2 <= base) and (base <= candidate - 2)):
                fail("assert(2 <= base) and (base <= candidate - 2) failed!")

        # Step 4.3-4.4
        z = pow(base, m, candidate)
        if z in (one, minus_one):
            continue

        # Step 4.5
        _or_else = True
        for j in iter_range(1, a):
            z = pow(z, 2, candidate)
            if z == minus_one:
                _or_else = False
                break
            if z == one:
                return COMPOSITE

        if _or_else:
            return COMPOSITE

    # Step 5
    return PROBABLY_PRIME


def lucas_test(candidate):
    """Perform a Lucas primality test on an integer.

    The test is specified in Section C.3.3 of `FIPS PUB 186-4`__.

    :Parameters:
      candidate : integer
        The number to test for primality.

    :Returns:
      ``Primality.COMPOSITE`` or ``Primality.PROBABLY_PRIME``.

    .. __: http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf
    """

    if not builtins.isinstance(candidate, Integer):
        candidate = Integer(candidate)

    # Step 1
    if candidate in (1, 2, 3, 5):
        return PROBABLY_PRIME
    if candidate.is_even() or candidate.is_perfect_square():
        return COMPOSITE

    # Step 2
    def alternate(idx, *args, **kwargs):
        if idx == 0:
            kwargs['memo']['value'] = 5
            return Result.Ok(kwargs['memo']['value'])

        if idx == WHILE_LOOP_EMULATION_ITERATION:
            return StopIteration()

        value = kwargs['memo']['value']
        if value > 0:
            value += 2
        else:
            value -= 2
        kwargs['memo']['value'] = -value
        return Result.Ok(kwargs['memo']['value'])

    candidate_as_int = int(candidate)
    for D in larky.DeterministicGenerator(alternate, memo={}):
        if candidate_as_int in (D, -D):
            continue
        js = Integer.jacobi_symbol(D, candidate)
        if js == 0:
            return COMPOSITE
        if js == -1:
            break

    # Found D. P=1 and Q=(1-D)/4 (note that Q is guaranteed to be an integer)

    # Step 3
    # This is \delta(n) = n - jacobi(D/n)
    K = candidate + 1
    # Step 4
    r = K.size_in_bits() - 1
    # Step 5
    # U_1=1 and V_1=P
    U_i = Integer(1)
    V_i = Integer(1)
    U_temp = Integer(0)
    V_temp = Integer(0)
    # Step 6
    for i in iter_range(r - 1, -1, -1):
        # Square
        # U_temp = U_i * V_i % candidate
        U_temp.set(U_i)
        U_temp *= V_i
        U_temp %= candidate
        # V_temp = (((V_i ** 2 + (U_i ** 2 * D)) * K) >> 1) % candidate
        V_temp.set(U_i)
        V_temp *= U_i
        V_temp *= D
        V_temp.multiply_accumulate(V_i, V_i)
        if V_temp.is_odd():
            V_temp += candidate
        V_temp >>= 1
        V_temp %= candidate
        # Multiply
        if K.get_bit(i):
            # U_i = (((U_temp + V_temp) * K) >> 1) % candidate
            U_i.set(U_temp)
            U_i += V_temp
            if U_i.is_odd():
                U_i += candidate
            U_i >>= 1
            U_i %= candidate
            # V_i = (((V_temp + U_temp * D) * K) >> 1) % candidate
            V_i.set(V_temp)
            V_i.multiply_accumulate(U_temp, D)
            if V_i.is_odd():
                V_i += candidate
            V_i >>= 1
            V_i %= candidate
        else:
            U_i.set(U_temp)
            V_i.set(V_temp)
    # Step 7
    if U_i == 0:
        return PROBABLY_PRIME
    return COMPOSITE

## The optimal number of small primes to use for the sieve
## is probably dependent on the platform and the candidate size
_sieve_base = sets.Set(_sieve_base_large[:100])


def test_probable_prime(candidate, randfunc=None):
    """Test if a number is prime.

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
    return _JCrypto.Math.is_prime(int(candidate), 1e-30)

    if randfunc == None:
        randfunc = Random.new().read

    if not builtins.isinstance(candidate, Integer):
        candidate = Integer(candidate)

    # First, check trial division by the smallest primes
    if int(candidate) in _sieve_base:
        return PROBABLY_PRIME

    for sieve_base_entry in _sieve_base:
        res = Result.Ok(sieve_base_entry).map(candidate.fail_if_divisible_by)
        if res.is_err:
            return COMPOSITE

    # These are the number of Miller-Rabin iterations s.t. p(k, t) < 1E-30,
    # with p(k, t) being the probability that a randomly chosen k-bit number
    # is composite but still survives t MR iterations.
    mr_ranges = ((220, 30), (280, 20), (390, 15), (512, 10),
                 (620, 7), (740, 6), (890, 5), (1200, 4),
                 (1700, 3), (3700, 2))

    bit_size = candidate.size_in_bits()
    mr_iterations_lt_bit_sz = [mr for mr in mr_ranges if bit_size < mr[0]]
    mr_iterations = 1
    if mr_iterations_lt_bit_sz and len(mr_iterations_lt_bit_sz[0]) >= 2:
        mr_iterations = mr_iterations_lt_bit_sz[0][1]

    if miller_rabin_test(candidate, mr_iterations, randfunc=randfunc) == COMPOSITE:
        return COMPOSITE

    if lucas_test(candidate) == COMPOSITE:
        return COMPOSITE

    return PROBABLY_PRIME


def generate_probable_prime(**kwargs):
    """Generate a random probable prime.

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

    exact_bits = kwargs.pop("exact_bits", None)
    randfunc = kwargs.pop("randfunc", None)
    prime_filter = kwargs.pop("prime_filter", lambda x: True)
    if kwargs:
        fail("ValueError: " + "Unknown parameters: " + kwargs.keys())

    if exact_bits == None:
        fail("ValueError: Missing exact_bits parameter")
    if exact_bits < 160:
        fail("ValueError: Prime number is not big enough.")

    if randfunc == None:
        randfunc = Random.new().read

    result = COMPOSITE
    for _while_ in larky.while_true():
        if result != COMPOSITE:
            break
        candidate = Integer.random(exact_bits=exact_bits,
                                   randfunc=randfunc) | 1
        if not prime_filter(candidate):
            continue
        result = test_probable_prime(candidate, randfunc)
    return candidate


def generate_probable_safe_prime(**kwargs):
    """Generate a random, probable safe prime.

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

    exact_bits = kwargs.pop("exact_bits", None)
    randfunc = kwargs.pop("randfunc", None)
    if kwargs:
        fail("ValueError: " + "Unknown parameters: " + kwargs.keys())

    if randfunc == None:
        randfunc = Random.new().read

    result = COMPOSITE
    for _while_ in larky.while_true():
        if result != COMPOSITE:
            break
        q = generate_probable_prime(exact_bits=exact_bits - 1, randfunc=randfunc)
        candidate = q * 2 + 1
        if candidate.size_in_bits() != exact_bits:
            continue
        result = test_probable_prime(candidate, randfunc=randfunc)
    return candidate


Primality = larky.struct(
    __name__='Primality',
    COMPOSITE=COMPOSITE,
    PROBABLY_PRIME=PROBABLY_PRIME,
    generate_probable_prime=generate_probable_prime,
    generate_probable_safe_prime=generate_probable_safe_prime,
    lucas_test=lucas_test,
    miller_rabin_test=miller_rabin_test,
    test_probable_prime=test_probable_prime,
)