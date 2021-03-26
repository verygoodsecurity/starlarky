# -*- coding: utf-8 -*-
#
#  Random/random.py : Strong alternative for the standard 'random' module
#
# Written in 2008 by Dwayne C. Litzenberger <dlitz@dlitz.net>
#
# ===================================================================
# The contents of this file are dedicated to the public domain.  To
# the extent that dedication to the public domain is not available,
# everyone is granted a worldwide, perpetual, royalty-free,
# non-exclusive license to exercise all rights associated with the
# contents of this file for any purpose whatsoever.
# No rights are reserved.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
# BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
# ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
# ===================================================================

__all__ = ['StrongRandom', 'getrandbits', 'randrange', 'randint', 'choice', 'shuffle', 'sample']

load("@vendor//Crypto", Random="Random")

load("@vendor//Crypto/Util/py3compat", is_native_int="is_native_int")
def StrongRandom():
    def __init__(rng=None, randfunc=None):
        if randfunc == None and rng == None:
            _randfunc = None
        elif randfunc != None and rng == None:
            _randfunc = randfunc
        elif randfunc == None and rng != None:
            _randfunc = rng.read
        else:
            fail(" ValueError(\"Cannot specify both 'rng' and 'randfunc'\")")

    def getrandbits(k):
        """Return an integer with k random bits."""

        if _randfunc == None:
            _randfunc = Random.new().read
        mask = (1 << k) - 1
        return mask & bytes_to_long(_randfunc(ceil_div(k, 8)))

    def randrange(*args):
        """randrange([start,] stop[, step]):
        Return a randomly-selected element from range(start, stop, step)."""
        if len(args) == 3:
            (start, stop, step) = args
        elif len(args) == 2:
            (start, stop) = args
            step = 1
        elif len(args) == 1:
            (stop,) = args
            start = 0
            step = 1
        else:
            fail(" TypeError(\"randrange expected at most 3 arguments, got %d\" % (len(args),))")
        if (not is_native_int(start) or not is_native_int(stop) or not
                is_native_int(step)):
            fail(" TypeError(\"randrange requires integer arguments\")")
        if step == 0:
            fail(" ValueError(\"randrange step argument must not be zero\")")

        num_choices = ceil_div(stop - start, step)
        if num_choices < 0:
            num_choices = 0
        if num_choices < 1:
            fail(" ValueError(\"empty range for randrange(%r, %r, %r)\" % (start, stop, step))")

        # Pick a random number in the range of possible numbers
        r = num_choices
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if r < num_choices:
                break
            r = getrandbits(size(num_choices))

        return start + (step * r)

    def randint(a, b):
        """Return a random integer N such that a <= N <= b."""
        if not is_native_int(a) or not is_native_int(b):
            fail(" TypeError(\"randint requires integer arguments\")")
        N = randrange(a, b+1)
        assert (a <= N) and (N <= b)
        return N

    def choice(seq):
        """Return a random element from a (non-empty) sequence.

        If the seqence is empty, raises IndexError.
        """
        if len(seq) == 0:
            fail(" IndexError(\"empty sequence\")")
        return seq[randrange(len(seq))]

    def shuffle(x):
        """Shuffle the sequence in place."""
        # Fisher-Yates shuffle.  O(n)
        # See http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
        # Working backwards from the end of the array, we choose a random item
        # from the remaining items until all items have been chosen.
        for i in range(len(x)-1, 0, -1):   # iterate from len(x)-1 downto 1
            j = randrange(0, i+1)      # choose random j such that 0 <= j <= i
            x[i], x[j] = x[j], x[i]         # exchange x[i] and x[j]

    def sample(population, k):
        """Return a k-length list of unique elements chosen from the population sequence."""

        num_choices = len(population)
        if k > num_choices:
            fail(" ValueError(\"sample larger than population\")")

        retval = []
        selected = {}  # we emulate a set using a dict here
        for i in range(k):
            r = None
            for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
                if not r == None or r in selected:
                    break
                r = randrange(num_choices)
            retval.append(population[r])
            selected[r] = 1
        return retval

_r = StrongRandom()
getrandbits = _r.getrandbits
randrange = _r.randrange
randint = _r.randint
choice = _r.choice
shuffle = _r.shuffle
sample = _r.sample

# These are at the bottom to avoid problems with recursive imports
load("@vendor//Crypto/Util/number", ceil_div="ceil_div", bytes_to_long="bytes_to_long", long_to_bytes="long_to_bytes", size="size")

# vim:set ts=4 sw=4 sts=4 expandtab:

