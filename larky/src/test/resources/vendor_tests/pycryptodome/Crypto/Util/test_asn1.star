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

"""Self-test suite for Crypto.Util.ASN1"""

load("@stdlib//binascii", hexlify="hexlify", unhexlify="unhexlify")
load("@stdlib//builtins", "builtins")

load("@stdlib//unittest","unittest")
load("@vendor//asserts","asserts")
load("@stdlib//types", "types")
load(
    "@vendor//Crypto/Util/ASN1",
    DerObject="DerObject",
    DerSetOf="DerSetOf",
    DerInteger="DerInteger",
    DerBitString="DerBitString",
    DerObjectId="DerObjectId",
    DerNull="DerNull",
    DerOctetString="DerOctetString",
    DerSequence="DerSequence"
)
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//escapes", "escapes")


def b(s):
    return builtins.bytearray(s, encoding='utf-8')


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



# def _test_DERSequence():
#     obj_der = unhexlify('070102')
#     asserts.assert_true(types.is_bytes(obj_der))
#     seq_der = DerSequence([4])
#     seq_der.append(9)
#     asserts.assert_true(types.is_string(obj_der.decode("utf-8")))
#     seq_der.append(obj_der.decode("utf-8"))
#     asserts.eq(hexlify(seq_der.encode()), "3009020104020109070102")
#
#     # seq_der = _JCrypto.Util.ASN1.DerSequence([4])
#     # seq_der.append(9)
#     # seq_der.append(obj_der.decode("utf-8"))
#     # asserts.eq(hexlify(seq_der.encode()), "3009020104020109070102")



def testObjInit1():
    # Fail with invalid tag format (must be 1 byte)
    asserts.assert_fails(lambda: DerObject(b(r"\x00\x99")), ".*?ValueError")
    # Fail with invalid implicit tag (must be <0x1F)
    asserts.assert_fails(lambda: DerObject(0x1F), ".*?ValueError")


def testObjEncode1():
    # No payload
    der = DerObject(b(r"\x02"))
    asserts.assert_that(der.encode()).is_equal_to(b(r"\x02\x00"))
    # Small payload (primitive)
    der.payload = b(r"\x45")
    asserts.assert_that(der.encode()).is_equal_to(b(r"\x02\x01\x45"))
    # Invariant
    asserts.assert_that(der.encode()).is_equal_to(b(r"\x02\x01\x45"))
    # Initialize with numerical tag
    der = DerObject(0x04)
    der.payload = b(r"\x45")
    asserts.assert_that(der.encode()).is_equal_to(b(r"\x04\x01\x45"))
    # Initialize with constructed type
    der = DerObject(b(r"\x10"), constructed=1)
    asserts.assert_that(der.encode()).is_equal_to(b(r"\x30\x00"))


def testObjEncode2():
    # Initialize with payload
    der = DerObject(0x03, b(r"\x12\x12"))
    asserts.assert_that(der.encode()).is_equal_to(b(r"\x03\x02\x12\x12"))


def testObjEncode3():
    # Long payload
    der = DerObject(b(r"\x10"))
    der.payload = b(r"0") * 128
    asserts.assert_that(der.encode()).is_equal_to(b(r"\x10\x81\x80" + r"0" * 128))


def testObjEncode4():
    # Implicit tags (constructed)
    der = DerObject(0x10, implicit=1, constructed=True)
    der.payload = b(r"ppll")
    asserts.assert_that(der.encode()).is_equal_to(b(r"\xa1\x04ppll"))
    # Implicit tags (primitive)
    der = DerObject(0x02, implicit=0x1E, constructed=False)
    der.payload = b(r"ppll")
    asserts.assert_that(der.encode()).is_equal_to(b(r"\x9E\x04ppll"))


def testObjEncode5():
    # Encode type with explicit tag
    der = DerObject(0x10, explicit=5)
    der.payload = b(r"xxll")
    asserts.assert_that(der.encode()).is_equal_to(b(r"\xa5\x06\x10\x04xxll"))



def testInit1():
    der = _JCrypto.Util.asn1.DerSequence([
        # 1,
        # _JCrypto.Util.ASN1.DerInteger(2),
        b(escapes.Escaper().raw("0").x("00"))
    ])
    #der.append(b(escapes.CEscape().r("0").x("00")))

    print(hexlify(b(escapes.CEscape().r("0").x("00")))) # 3000
    expected = hexlify(b(escapes.CEscape().r("0").x("020").x("00")))
    print(expected) # 30023000 (expected)

    encoded = hexlify(der.encode())
    print("encoded: ", encoded)
    print("encoded ", encoded, "== expected", expected, "?", encoded==expected)
    #print(hexlify(der.encode()), hexlify(der.encode()) == hexlify(b(escapes.CEscape().r("0").x("020").x("00"))))
    der = DerSequence([1, DerInteger(2), b(escapes.Escaper().raw("0").x("00"))])
    expected = b(r"0\x08\x02\x01\x01\x02\x01\x020\x00")
    actual = der.encode()
    print(hexlify(expected))
    print(hexlify(actual))
    asserts.assert_that(actual).is_equal_to(expected)


def testEncode1():
    # Empty sequence
    der = _JCrypto.Util.asn1.DerSequence()
    asserts.assert_that(der.encode()).is_equal_to(b(r"0\x00"))
    asserts.assert_that(der.hasOnlyInts()).is_false()
    # One single-byte integer (zero)
    der.append(0)
    asserts.assert_that(der.encode()).is_equal_to(b(r"0\x03\x02\x01\x00"))
    asserts.assert_that(der.hasInts()).is_equal_to(1)
    asserts.assert_that(der.hasInts(False)).is_equal_to(1)
    asserts.assert_that(der.hasOnlyInts()).is_true()
    asserts.assert_that(der.hasOnlyInts(False)).is_true()
    # Invariant
    asserts.assert_that(der.encode()).is_equal_to(b(r"0\x03\x02\x01\x00"))


def testEncode2():
    # Indexing
    der = _JCrypto.Util.asn1.DerSequence()
    der.append(0)
    der[0] = 1
    asserts.assert_that(len(der)).is_equal_to(1)
    asserts.assert_that(der[0]).is_equal_to(1)
    asserts.assert_that(der[-1]).is_equal_to(1)
    asserts.assert_that(der.encode()).is_equal_to(b(r"0\x03\x02\x01\x01"))
    #
    der = [1]
    asserts.assert_that(len(der)).is_equal_to(1)
    asserts.assert_that(der[0]).is_equal_to(1)
    asserts.assert_that(der.encode()).is_equal_to(b(r"0\x03\x02\x01\x01"))


def testEncode3():
    # One multi-byte integer (non-zero)
    der = _JCrypto.Util.asn1.DerSequence()
    der.append(0x180)
    asserts.assert_that(der.encode()).is_equal_to(b(r"0\x04\x02\x02\x01\x80"))


def testEncode4():
    # One very long integer
    der = _JCrypto.Util.asn1.DerSequence()
    der.append(pow(2, 2048))
    asserts.assert_that(der.encode()).is_equal_to(
        b(r"0\x82\x01\x05")
        + b(r"\x02\x82\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00")
    )


def testEncode5():
    der = _JCrypto.Util.asn1.DerSequence()
    der += 1
    der += b(r"\x30\x00")
    asserts.assert_that(der.encode()).is_equal_to(b(r"\x30\x05\x02\x01\x01\x30\x00"))


def testEncode6():
    # Two positive integers
    der = _JCrypto.Util.asn1.DerSequence()
    der.append(0x180)
    der.append(0xFF)
    asserts.assert_that(der.encode()).is_equal_to(
        b(r"0\x08\x02\x02\x01\x80\x02\x02\x00\xff")
    )
    asserts.assert_that(der.hasOnlyInts()).is_true()
    asserts.assert_that(der.hasOnlyInts(False)).is_true()
    # Two mixed integers
    der = _JCrypto.Util.asn1.DerSequence()
    der.append(2)
    der.append(-2)
    asserts.assert_that(der.encode()).is_equal_to(b(r"0\x06\x02\x01\x02\x02\x01\xFE"))
    asserts.assert_that(der.hasInts()).is_equal_to(1)
    asserts.assert_that(der.hasInts(False)).is_equal_to(2)
    asserts.assert_that(der.hasOnlyInts()).is_false()
    asserts.assert_that(der.hasOnlyInts(False)).is_true()
    #
    der.append(0x01)
    der = der[0] + [9, 8]
    asserts.assert_that(len(der)).is_equal_to(3)
    asserts.assert_that(der[1:]).is_equal_to([9, 8])
    asserts.assert_that(der[1:-1]).is_equal_to([9])
    asserts.assert_that(der.encode()).is_equal_to(
        b(r"0\x09\x02\x01\x02\x02\x01\x09\x02\x01\x08")
    )


def testEncode7():
    # One integer and another type (already encoded)
    der = _JCrypto.Util.asn1.DerSequence()
    der.append(0x180)
    der.append(b(r"0\x03\x02\x01\x05"))
    asserts.assert_that(der.encode()).is_equal_to(
        b(r"0\x09\x02\x02\x01\x800\x03\x02\x01\x05")
    )
    asserts.assert_that(der.hasOnlyInts()).is_false()


def testEncode8():
    # One integer and another type (yet to encode)
    der = _JCrypto.Util.asn1.DerSequence()
    der.append(0x180)
    der.append(_JCrypto.Util.asn1.DerSequence([5]))
    asserts.assert_that(der.encode()).is_equal_to(
        b(r"0\x09\x02\x02\x01\x800\x03\x02\x01\x05")
    )
    asserts.assert_that(der.hasOnlyInts()).is_false()

    ####


def testDecode1():
    # Empty sequence
    der = _JCrypto.Util.asn1.DerSequence()
    der.decode(b(r"0\x00"))
    asserts.assert_that(len(der)).is_equal_to(0)
    # One single-byte integer (zero)
    der.decode(b(r"0\x03\x02\x01\x00"))
    asserts.assert_that(len(der)).is_equal_to(1)
    asserts.assert_that(der[0]).is_equal_to(0)
    # Invariant
    der.decode(b(r"0\x03\x02\x01\x00"))
    asserts.assert_that(len(der)).is_equal_to(1)
    asserts.assert_that(der[0]).is_equal_to(0)


def testDecode2():
    # One single-byte integer (non-zero)
    der = _JCrypto.Util.asn1.DerSequence()
    der.decode(b(r"0\x03\x02\x01\x7f"))
    asserts.assert_that(len(der)).is_equal_to(1)
    asserts.assert_that(der[0]).is_equal_to(127)


def testDecode4():
    # One very long integer
    der = _JCrypto.Util.asn1.DerSequence()
    der.decode(
        b(r"0\x82\x01\x05")
        + b(r"\x02\x82\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00")
        + b(r"\x00\x00\x00\x00\x00\x00\x00\x00\x00")
    )
    asserts.assert_that(len(der)).is_equal_to(1)
    asserts.assert_that(der[0]).is_equal_to(pow(2, 2048))


def testDecode6():
    # Two integers
    der = _JCrypto.Util.asn1.DerSequence()
    der.decode(b(r"0\x08\x02\x02\x01\x80\x02\x02\x00\xff"))
    asserts.assert_that(len(der)).is_equal_to(2)
    asserts.assert_that(der[0]).is_equal_to(0x180)
    asserts.assert_that(der[1]).is_equal_to(0xFF)


def testDecode7():
    # One integer and 2 other types
    der = _JCrypto.Util.asn1.DerSequence()
    der.decode(b(r"0\x0A\x02\x02\x01\x80\x24\x02\xb6\x63\x12\x00"))
    asserts.assert_that(len(der)).is_equal_to(3)
    asserts.assert_that(der[0]).is_equal_to(0x180)
    asserts.assert_that(der[1]).is_equal_to(b(r"\x24\x02\xb6\x63"))
    asserts.assert_that(der[2]).is_equal_to(b(r"\x12\x00"))


def testDecode8():
    # Only 2 other types
    der = _JCrypto.Util.asn1.DerSequence()
    der.decode(b(r"0\x06\x24\x02\xb6\x63\x12\x00"))
    asserts.assert_that(len(der)).is_equal_to(2)
    asserts.assert_that(der[0]).is_equal_to(b(r"\x24\x02\xb6\x63"))
    asserts.assert_that(der[1]).is_equal_to(b(r"\x12\x00"))
    asserts.assert_that(der.hasInts()).is_equal_to(0)
    asserts.assert_that(der.hasInts(False)).is_equal_to(0)
    asserts.assert_that(der.hasOnlyInts()).is_false()
    asserts.assert_that(der.hasOnlyInts(False)).is_false()


def testDecode9():
    # Verify that decode returns itself
    der = _JCrypto.Util.asn1.DerSequence()
    asserts.assert_that(der).is_equal_to(der.decode(b(r"0\x06\x24\x02\xb6\x63\x12\x00")))

    ###


def testErrDecode1():
    # Not a sequence
    der = _JCrypto.Util.asn1.DerSequence()
    asserts.assert_fails(lambda: der.decode(b(r"")), ".*?ValueError")
    asserts.assert_fails(lambda: der.decode(b(r"\x00")), ".*?ValueError")
    asserts.assert_fails(lambda: der.decode(b(r"\x30")), ".*?ValueError")


def testErrDecode2():
    der = _JCrypto.Util.asn1.DerSequence()
    # Too much data
    asserts.assert_fails(lambda: der.decode(b(r"\x30\x00\x00")), ".*?ValueError")


def testErrDecode3():
    # Wrong length format
    der = _JCrypto.Util.asn1.DerSequence()
    # Missing length in sub-item
    asserts.assert_fails(
        lambda: der.decode(b(r"\x30\x04\x02\x01\x01\x00")), ".*?ValueError"
    )
    # Valid BER, but invalid DER length
    asserts.assert_fails(
        lambda: der.decode(b(r"\x30\x81\x03\x02\x01\x01")), ".*?ValueError"
    )
    asserts.assert_fails(
        lambda: der.decode(b(r"\x30\x04\x02\x81\x01\x01")), ".*?ValueError"
    )


def test_expected_nr_elements():
    der_bin = _JCrypto.Util.asn1.DerSequence([1, 2, 3]).encode()

    _JCrypto.Util.asn1.DerSequence().decode(der_bin, nr_elements=3)
    _JCrypto.Util.asn1.DerSequence().decode(der_bin, nr_elements=(2, 3))
    asserts.assert_fails(
        lambda: _JCrypto.Util.asn1.DerSequence().decode(der_bin, nr_elements=1), ".*?ValueError"
    )
    asserts.assert_fails(
        lambda: _JCrypto.Util.asn1.DerSequence().decode(der_bin, nr_elements=(4, 5)), ".*?ValueError"
    )


def test_expected_only_integers():

    der_bin1 = _JCrypto.Util.asn1.DerSequence([1, 2, 3]).encode()
    der_bin2 = _JCrypto.Util.asn1.DerSequence([1, 2, _JCrypto.Util.asn1.DerSequence([3, 4])]).encode()

    _JCrypto.Util.asn1.DerSequence().decode(der_bin1, only_ints_expected=True)
    _JCrypto.Util.asn1.DerSequence().decode(der_bin1, only_ints_expected=False)
    _JCrypto.Util.asn1.DerSequence().decode(der_bin2, only_ints_expected=False)
    asserts.assert_fails(
        lambda: _JCrypto.Util.asn1.DerSequence().decode(der_bin2, only_ints_expected=True), ".*?ValueError"
    )

def _testsuite():
    _suite = unittest.TestSuite()
    # _suite.addTest(unittest.FunctionTestCase(_test_DERInteger))
    # _suite.addTest(unittest.FunctionTestCase(_test_DERSequence))
    # _suite.addTest(unittest.FunctionTestCase(testObjInit1))
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
    _suite.addTest(unittest.FunctionTestCase(testInit1))
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
