# -*- coding: utf-8 -*-
#
#  SelfTest/Util/test_number.py: Self-test for parts of the Crypto.Util.number module
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

"""Self-tests for (some of) Crypto.Util.number"""

load("@stdlib//math", math="math")
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@vendor//Crypto/Util/number", number="number")
load("@vendor//Crypto/Util/number", long_to_bytes="long_to_bytes")
load("@vendor//asserts","asserts")
load("@stdlib//builtins","builtins")
load("@stdlib//unittest","unittest")
load("@vendor//escapes", escapes="escapes")


# NB: In some places, we compare tuples instead of just output values so that
# if any inputs cause a test failure, we'll be able to tell which ones.


def test_ceil_div():
    """Util.number.ceil_div"""
    asserts.assert_fails(lambda : number.ceil_div("1", 1), ".*?unsupported comparison")
    asserts.assert_fails(lambda : number.ceil_div(1, 0), ".*?ZeroDivisionError")
    asserts.assert_fails(lambda : number.ceil_div(-1, 0), ".*?ZeroDivisionError")

    # b = 1
    asserts.assert_that(0).is_equal_to(number.ceil_div(0, 1))
    asserts.assert_that(1).is_equal_to(number.ceil_div(1, 1))
    asserts.assert_that(2).is_equal_to(number.ceil_div(2, 1))
    asserts.assert_that(3).is_equal_to(number.ceil_div(3, 1))

    # b = 2
    asserts.assert_that(0).is_equal_to(number.ceil_div(0, 2))
    asserts.assert_that(1).is_equal_to(number.ceil_div(1, 2))
    asserts.assert_that(1).is_equal_to(number.ceil_div(2, 2))
    asserts.assert_that(2).is_equal_to(number.ceil_div(3, 2))
    asserts.assert_that(2).is_equal_to(number.ceil_div(4, 2))
    asserts.assert_that(3).is_equal_to(number.ceil_div(5, 2))

    # b = 3
    asserts.assert_that(0).is_equal_to(number.ceil_div(0, 3))
    asserts.assert_that(1).is_equal_to(number.ceil_div(1, 3))
    asserts.assert_that(1).is_equal_to(number.ceil_div(2, 3))
    asserts.assert_that(1).is_equal_to(number.ceil_div(3, 3))
    asserts.assert_that(2).is_equal_to(number.ceil_div(4, 3))
    asserts.assert_that(2).is_equal_to(number.ceil_div(5, 3))
    asserts.assert_that(2).is_equal_to(number.ceil_div(6, 3))
    asserts.assert_that(3).is_equal_to(number.ceil_div(7, 3))

        # b = 4
    asserts.assert_that(0).is_equal_to(number.ceil_div(0, 4))
    asserts.assert_that(1).is_equal_to(number.ceil_div(1, 4))
    asserts.assert_that(1).is_equal_to(number.ceil_div(2, 4))
    asserts.assert_that(1).is_equal_to(number.ceil_div(3, 4))
    asserts.assert_that(1).is_equal_to(number.ceil_div(4, 4))
    asserts.assert_that(2).is_equal_to(number.ceil_div(5, 4))
    asserts.assert_that(2).is_equal_to(number.ceil_div(6, 4))
    asserts.assert_that(2).is_equal_to(number.ceil_div(7, 4))
    asserts.assert_that(2).is_equal_to(number.ceil_div(8, 4))
    asserts.assert_that(3).is_equal_to(number.ceil_div(9, 4))


# TODO: not supposed to do this?
# def test_getStrongPrime():
#     """Util.number.getStrongPrime"""
#     asserts.assert_fails(lambda : number.getStrongPrime(256), ".*?ValueError")
#     asserts.assert_fails(lambda : number.getStrongPrime(513), ".*?ValueError")
#     bits = 512
#     x = number.getStrongPrime(bits)
#     asserts.assert_that(x % 2).is_not_equal_to(0)
#     asserts.assert_that((x > (1 << bits-1)-1)).is_equal_to(True)
# #    asserts.assert_that((x < (1 << bits))).is_equal_to(True)
#     e = math.pow(2, 16+1)
#     x = number.getStrongPrime(bits, e)
#     asserts.assert_that(number.GCD(x-1, e)).is_equal_to(True)
#     asserts.assert_that(x % 2).is_not_equal_to(0)
#     asserts.assert_that((x > (1 << bits-1)-1)).is_equal_to(True)
#     asserts.assert_that((x < (1 << bits))).is_equal_to(True)
#     e = math.pow(2, 16+2)
#     x = number.getStrongPrime(bits, e)
#     asserts.assert_that(number.GCD((x-1)>>1, e)).is_equal_to(True)
#     asserts.assert_that(x % 2).is_not_equal_to(0)
#     asserts.assert_that((x > (1 << bits-1)-1)).is_equal_to(True)
#     asserts.assert_that((x < (1 << bits))).is_equal_to(True)


def test_isPrime():
    """Util.number.isPrime"""
    asserts.assert_that(number.isPrime(-3)).is_equal_to(False)     # Regression test: negative numbers should not be prime
    asserts.assert_that(number.isPrime(-2)).is_equal_to(False)     # Regression test: negative numbers should not be prime
    asserts.assert_that(number.isPrime(1)).is_equal_to(False)      # Regression test: isPrime(1) caused some versions of PyCrypto to crash.
    asserts.assert_that(number.isPrime(2)).is_equal_to(True)
    asserts.assert_that(number.isPrime(3)).is_equal_to(True)
    asserts.assert_that(number.isPrime(4)).is_equal_to(False)
    asserts.assert_that(number.isPrime(pow(2, 1279)-1)).is_equal_to(True)
    asserts.assert_that(number.isPrime(-pow(2, 1279)-1)).is_equal_to(False)     # Regression test: negative numbers should not be prime
    # test some known gmp pseudo-primes taken from
    # http://www.trnicely.net/misc/mpzspsp.html
    for composite in (43 * 127 * 211, 61 * 151 * 211, 15259 * 30517,
                      346141 * 692281, 1007119 * 2014237, 3589477 * 7178953,
                      4859419 * 9718837, 2730439 * 5460877,
                      245127919 * 490255837, 963939391 * 1927878781,
                      4186358431 * 8372716861, 1576820467 * 3153640933):
        asserts.assert_that(number.isPrime(int(composite))).is_equal_to(False)

def test_size():
    asserts.assert_that(number.size(2)).is_equal_to(2)
    asserts.assert_that(number.size(3)).is_equal_to(2)
    asserts.assert_that(number.size(0xa2)).is_equal_to(8)
    asserts.assert_that(number.size(0xa2ba40)).is_equal_to(8*3)
    asserts.assert_that(number.size(0xa2ba40ee07e3b2bd2f02ce227f36a195024486e49c19cb41bbbdfbba98b22b0e577c2eeaffa20d883a76e65e394c69d4b3c05a1e8fadda27edb2a42bc000fe888b9b32c22d15add0cd76b3e7936e19955b220dd17d4ea904b1ec102b2e4de7751222aa99151024c7cb41cc5ea21d00eeb41f7c800834d2c6e06bce3bce7ea9a5)).is_equal_to(1024)
    asserts.assert_fails(lambda : number.size(-1), ".*?ValueError")


def test1():
    asserts.assert_that(long_to_bytes(0)).is_equal_to(bytes([0x00]))
    asserts.assert_that(long_to_bytes(1)).is_equal_to(bytes([0x01]))
    asserts.assert_that(long_to_bytes(0x100)).is_equal_to(bytes([0x01, 0x00]))
    asserts.assert_that(long_to_bytes(0xFF00000000)).is_equal_to(bytes([0xff, 0x00, 0x00, 0x00, 0x00]))
    asserts.assert_that(long_to_bytes(0x1122334455667788)).is_equal_to(bytes([0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88]))
    asserts.assert_that(long_to_bytes(0x112233445566778899)).is_equal_to(bytes([0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88, 0x99]))


def test2():
    asserts.assert_that(long_to_bytes(0, 1)).is_equal_to(bytes([0x00]))
    asserts.assert_that(long_to_bytes(0, 2)).is_equal_to(bytes([0x00, 0x00]))
    asserts.assert_that(long_to_bytes(1, 3)).is_equal_to(bytes([0x00, 0x00, 0x01]))
    # asserts.assert_that(long_to_bytes(0x100, 1)).is_equal_to(bytes([0x01, 0x00]))
    asserts.assert_that(long_to_bytes(0xFF00000001, 6)).is_equal_to(bytes([0x00, 0xFF, 0x00, 0x00, 0x00, 0x01]))
    asserts.assert_that(long_to_bytes(0xFF00000001, 8)).is_equal_to(bytes([0x00, 0x00, 0x00, 0xFF, 0x00, 0x00, 0x00, 0x01]))
    asserts.assert_that(long_to_bytes(0xFF00000001, 10)).is_equal_to(bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0x00, 0x00, 0x00, 0x01]))
    asserts.assert_that(long_to_bytes(0xFF00000001, 11)).is_equal_to(bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0x00, 0x00, 0x00, 0x01]))


def test_err1():
    asserts.assert_fails(lambda : long_to_bytes(-1), ".*?ValueError")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_ceil_div))
    # _suite.addTest(unittest.FunctionTestCase(test_getStrongPrime))
    _suite.addTest(unittest.FunctionTestCase(test_isPrime))
    _suite.addTest(unittest.FunctionTestCase(test_size))
    _suite.addTest(unittest.FunctionTestCase(test1))
    _suite.addTest(unittest.FunctionTestCase(test2))
    _suite.addTest(unittest.FunctionTestCase(test_err1))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
