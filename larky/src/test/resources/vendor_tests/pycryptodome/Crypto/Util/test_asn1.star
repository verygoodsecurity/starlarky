# -*- coding: utf-8 -*-
#
#  SelfTest/PublicKey/test_RSA.py: Self-test for the RSA primitive
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

"""Self-test suite for Crypto.Util.asn1"""

load("@stdlib//binascii", hexlify="hexlify", unhexlify="unhexlify")
load("@stdlib//builtins", "builtins")

load("@stdlib//unittest","unittest")
load("@vendor//asserts","asserts")
load("@stdlib//types", "types")
load(
    "@vendor//Crypto/Util/asn1",
    DerObject="DerObject",
    DerSetOf="DerSetOf",
    DerInteger="DerInteger",
    DerBitString="DerBitString",
    DerObjectId="DerObjectId",
    DerNull="DerNull",
    DerOctetString="DerOctetString",
    DerSequence="DerSequence",
)
load("@stdlib//jcrypto", _JCrypto="jcrypto")


b = builtins.b


def _test_DERInteger():
    int_der = DerInteger(9)
    encoded = int_der.encode()
    asserts.eq(hexlify(encoded), "020109")
    asserts.assert_true(types.is_bytes(encoded))

    s = unhexlify(encoded)
    print(len(s))
    int_der = DerInteger()
    int_der.decode(s)
    asserts.eq(str(int_der.value), "9")



def _test_DERSequence():
    obj_der = unhexlify('070102')
    asserts.assert_true(types.is_bytes(obj_der))
    seq_der = DerSequence([4])
    seq_der.append(9)
    asserts.assert_true(types.is_string(obj_der.decode("utf-8")))
    seq_der.append(obj_der.decode("utf-8"))
    asserts.eq(hexlify(seq_der.encode()), "3009020104020109070102")


def testObjInit1():
    # Fail with invalid tag format (must be 1 byte)
    asserts.assert_fails(lambda: DerObject(b(r"\x00\x99")), ".*?ValueError")
    # Fail with invalid implicit tag (must be <0x1F)
    asserts.assert_fails(lambda: DerObject(0x1F), ".*?ValueError")


def testObjEncode1():
    # No payload
    der = DerObject(b("\x02"))
    asserts.assert_that(der.encode()).is_equal_to(b("\x02\x00"))
    # Small payload (primitive)
    der.payload = b("\x45")
    asserts.assert_that(der.encode()).is_equal_to(b("\x02\x01\x45"))
    # Invariant
    asserts.assert_that(der.encode()).is_equal_to(b("\x02\x01\x45"))
    # Initialize with numerical tag
    der = DerObject(0x04)
    der.payload = b("\x45")
    asserts.assert_that(der.encode()).is_equal_to(b("\x04\x01\x45"))
    # Initialize with constructed type
    der = DerObject(b("\x10"), constructed=True)
    asserts.assert_that(der.encode()).is_equal_to(b("\x30\x00"))

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_DERInteger))
    _suite.addTest(unittest.FunctionTestCase(_test_DERSequence))
    _suite.addTest(unittest.FunctionTestCase(testObjInit1))
    # _suite.addTest(unittest.FunctionTestCase(testObjEncode1))
    # _suite.addTest(unittest.FunctionTestCase(testObjEncode2))
    # _suite.addTest(unittest.FunctionTestCase(testObjEncode3))
    # _suite.addTest(unittest.FunctionTestCase(testObjEncode4))
    # _suite.addTest(unittest.FunctionTestCase(testObjEncode5))
    # _suite.addTest(unittest.FunctionTestCase(testObjDecode1))
    # _suite.addTest(unittest.FunctionTestCase(testObjDecode2))
    # _suite.addTest(unittest.FunctionTestCase(testObjDecode3))
    # _suite.addTest(unittest.FunctionTestCase(testObjDecode4))
    # _suite.addTest(unittest.FunctionTestCase(testObjDecode5))
    # _suite.addTest(unittest.FunctionTestCase(testObjDecode6))
    # _suite.addTest(unittest.FunctionTestCase(testObjDecode7))
    # _suite.addTest(unittest.FunctionTestCase(testObjDecode8))
    # _suite.addTest(unittest.FunctionTestCase(testInit1))
    # _suite.addTest(unittest.FunctionTestCase(testEncode1))
    # _suite.addTest(unittest.FunctionTestCase(testEncode2))
    # _suite.addTest(unittest.FunctionTestCase(testEncode3))
    # _suite.addTest(unittest.FunctionTestCase(testEncode4))
    # _suite.addTest(unittest.FunctionTestCase(testDecode1))
    # _suite.addTest(unittest.FunctionTestCase(testDecode2))
    # _suite.addTest(unittest.FunctionTestCase(testDecode3))
    # _suite.addTest(unittest.FunctionTestCase(testDecode5))
    # _suite.addTest(unittest.FunctionTestCase(testDecode6))
    # _suite.addTest(unittest.FunctionTestCase(testDecode7))
    # _suite.addTest(unittest.FunctionTestCase(testStrict1))
    # _suite.addTest(unittest.FunctionTestCase(testErrDecode1))
    # _suite.addTest(unittest.FunctionTestCase(testInit1))
    # _suite.addTest(unittest.FunctionTestCase(testEncode1))
    # _suite.addTest(unittest.FunctionTestCase(testEncode2))
    # _suite.addTest(unittest.FunctionTestCase(testEncode3))
    # _suite.addTest(unittest.FunctionTestCase(testEncode4))
    # _suite.addTest(unittest.FunctionTestCase(testEncode5))
    # _suite.addTest(unittest.FunctionTestCase(testEncode6))
    # _suite.addTest(unittest.FunctionTestCase(testEncode7))
    # _suite.addTest(unittest.FunctionTestCase(testEncode8))
    # _suite.addTest(unittest.FunctionTestCase(testDecode1))
    # _suite.addTest(unittest.FunctionTestCase(testDecode2))
    # _suite.addTest(unittest.FunctionTestCase(testDecode4))
    # _suite.addTest(unittest.FunctionTestCase(testDecode6))
    # _suite.addTest(unittest.FunctionTestCase(testDecode7))
    # _suite.addTest(unittest.FunctionTestCase(testDecode8))
    # _suite.addTest(unittest.FunctionTestCase(testDecode9))
    # _suite.addTest(unittest.FunctionTestCase(testErrDecode1))
    # _suite.addTest(unittest.FunctionTestCase(testErrDecode2))
    # _suite.addTest(unittest.FunctionTestCase(testErrDecode3))
    # _suite.addTest(unittest.FunctionTestCase(test_expected_nr_elements))
    # _suite.addTest(unittest.FunctionTestCase(test_expected_only_integers))
    # _suite.addTest(unittest.FunctionTestCase(testInit1))
    # _suite.addTest(unittest.FunctionTestCase(testEncode1))
    # _suite.addTest(unittest.FunctionTestCase(testDecode1))
    # _suite.addTest(unittest.FunctionTestCase(testDecode2))
    # _suite.addTest(unittest.FunctionTestCase(testErrDecode1))
    # _suite.addTest(unittest.FunctionTestCase(testEncode1))
    # _suite.addTest(unittest.FunctionTestCase(testDecode1))
    # _suite.addTest(unittest.FunctionTestCase(testInit1))
    # _suite.addTest(unittest.FunctionTestCase(testEncode1))
    # _suite.addTest(unittest.FunctionTestCase(testDecode1))
    # _suite.addTest(unittest.FunctionTestCase(testDecode2))
    # _suite.addTest(unittest.FunctionTestCase(testDecode3))
    # _suite.addTest(unittest.FunctionTestCase(testInit1))
    # _suite.addTest(unittest.FunctionTestCase(testInit2))
    # _suite.addTest(unittest.FunctionTestCase(testEncode1))
    # _suite.addTest(unittest.FunctionTestCase(testDecode1))
    # _suite.addTest(unittest.FunctionTestCase(testDecode2))
    # _suite.addTest(unittest.FunctionTestCase(testInit1))
    # _suite.addTest(unittest.FunctionTestCase(testEncode1))
    # _suite.addTest(unittest.FunctionTestCase(testEncode2))
    # _suite.addTest(unittest.FunctionTestCase(testEncode3))
    # _suite.addTest(unittest.FunctionTestCase(testEncode4))
    # _suite.addTest(unittest.FunctionTestCase(testDecode1))
    # _suite.addTest(unittest.FunctionTestCase(testDecode2))
    # _suite.addTest(unittest.FunctionTestCase(testDecode3))
    # _suite.addTest(unittest.FunctionTestCase(testDecode4))
    # _suite.addTest(unittest.FunctionTestCase(testErrDecode1))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
