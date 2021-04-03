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
    "@vendor//Crypto/Util/asn1",
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
    asserts.assert_true(types.is_bytelike(encoded))


# def _test_DERSequence():
#     obj_der = unhexlify('070102')
#     asserts.assert_true(types.is_bytes(obj_der))
#     seq_der = DerSequence([4])
#     seq_der.append(9)
#     asserts.assert_true(types.is_string(obj_der.decode("utf-8")))
#     seq_der.append(obj_der.decode("utf-8"))
#     asserts.eq(hexlify(seq_der.encode()), "3009020104020109070102")


def test_JDERSequence():
    obj_der = unhexlify('070102')
    seq_der = _JCrypto.Util.ASN1.DerSequence([4])
    seq_der.append(9)
    seq_der.append(obj_der.decode("utf-8"))
    asserts.eq(hexlify(seq_der.encode()), "3009020104020109070102")


def DerObjectTests_testObjInit1():
    # Fail with invalid tag format (must be 1 byte)
    asserts.assert_fails(lambda: DerObject(b(r"\x00\x99")), ".*?ValueError")
    # Fail with invalid implicit tag (must be <0x1F)
    asserts.assert_fails(lambda: DerObject(0x1F), ".*?ValueError")


def DerObjectTests_testObjEncode1():
    # No payload
    der = DerObject(b(r"\x02"))
    asserts.assert_that(der.encode(der)).is_equal_to(b(r"\x02\x00"))
    # Small payload (primitive)
    der.payload = b(r"\x45")
    asserts.assert_that(der.encode(der)).is_equal_to(b(r"\x02\x01\x45"))
    # Invariant
    asserts.assert_that(der.encode(der)).is_equal_to(b(r"\x02\x01\x45"))
    # Initialize with numerical tag
    der = DerObject(0x04)
    der.payload = b(r"\x45")
    asserts.assert_that(der.encode(der)).is_equal_to(b(r"\x04\x01\x45"))
    # Initialize with constructed type
    der = DerObject(b(r"\x10"), constructed=1)
    asserts.assert_that(der.encode(der)).is_equal_to(b(r"\x30\x00"))


def DerObjectTests_testObjEncode2():
    # Initialize with payload
    der = DerObject(0x03, b(r"\x12\x12"))
    asserts.assert_that(der.encode(der)).is_equal_to(b(r"\x03\x02\x12\x12"))


def DerObjectTests_testObjEncode3():
    # Long payload
    der = DerObject(b(r"\x10"))
    der.payload = bytearray([0]) * 128
    expected = b([0x10, 0x81, 0x80] + ([0x00] * 128))
    encoded = der.encode()
    asserts.assert_that(encoded).is_equal_to(expected)


def DerObjectTests_testObjEncode4():
    # Implicit tags (constructed)
    der = DerObject(0x10, implicit=1, constructed=1)
    der.payload = b(r"ppll")
    expected = bytearray([0xa1, 0x04]) + b(r"ppll")
    asserts.assert_that(der.encode()).is_equal_to(expected)
    # Implicit tags (primitive)
    der = DerObject(0x02, implicit=0x1E, constructed=0)
    der.payload = b(r"ppll")
    encoded = der.encode()
    expected = bytearray([0x9E, 0x04]) + b(r"ppll")
    asserts.assert_that(encoded).is_equal_to(expected)


def DerObjectTests_testObjEncode5():
    # Encode type with explicit tag
    der = DerObject(0x10, explicit=5)
    der.payload = b(r"xxll")
    expected = bytearray([0xa5, 0x06, 0x10, 0x04]) + b(r"xxll")
    asserts.assert_that(der.encode()).is_equal_to(expected)


def DerObjectTests_testObjDecode1():
        # Decode short payload
    der = DerObject(0x02)
    der.decode(der, bytes([0x02, 0x02, 0x01, 0x02]))
    asserts.assert_that(der.payload).is_equal_to(bytes([0x01, 0x02]))
    asserts.assert_that(der._tag_octet).is_equal_to(0x02)

def DerObjectTests_testObjDecode2():
        # Decode long payload
    der = DerObject(0x02)
    der.decode(der, bytearray([0x02, 0x81, 0x80]) + bytearray("1", encoding="utf-8") * 128)
    asserts.assert_that(der.payload).is_equal_to(bytes([0x31])*128)
    asserts.assert_that(der._tag_octet).is_equal_to(0x02)

def DerObjectTests_testObjDecode3():
        # Decode payload with too much data gives error
    der = DerObject(0x02)
    asserts.assert_fails(lambda : der.decode(der, bytes([0x02, 0x02, 0x01, 0x02, 0xff])), ".*?ValueError")
        # Decode payload with too little data gives error
    der = DerObject(0x02)
    asserts.assert_fails(lambda : der.decode(der, bytes([0x02, 0x02, 0x01])), ".*?ValueError")

def DerObjectTests_testObjDecode4():
        # Decode implicit tag (primitive)
    der = DerObject(0x02, constructed=0, implicit=0xF)
    asserts.assert_fails(lambda : der.decode(der, bytes([0x02, 0x02, 0x01, 0x02])), ".*?ValueError")
    der.decode(der, bytes([0x8f, 0x01, 0x00]))
    asserts.assert_that(der.payload).is_equal_to(bytes([0x00]))
        # Decode implicit tag (constructed)
    der = DerObject(0x02, constructed=1, implicit=0xF)
    asserts.assert_fails(lambda : der.decode(der, bytes([0x02, 0x02, 0x01, 0x02])), ".*?ValueError")
    der.decode(der, bytes([0xaf, 0x01, 0x00]))
    asserts.assert_that(der.payload).is_equal_to(bytes([0x00]))

def DerObjectTests_testObjDecode5():
        # Decode payload with unexpected tag gives error
    der = DerObject(0x02)
    asserts.assert_fails(lambda : der.decode(der, bytes([0x03, 0x02, 0x01, 0x02])), ".*?ValueError")

def DerObjectTests_testObjDecode6():
        # Arbitrary DER object
    der = DerObject()
    der.decode(der, bytes([0x65, 0x01, 0x88]))
    asserts.assert_that(der._tag_octet).is_equal_to(0x65)
    asserts.assert_that(der.payload).is_equal_to(bytes([0x88]))

def DerObjectTests_testObjDecode7():
        # Decode explicit tag
    der = DerObject(0x10, explicit=5)
    der.decode(der, bytes([0xa5, 0x06, 0x10, 0x04, 0x78, 0x78, 0x6c, 0x6c]))
    asserts.assert_that(der._inner_tag_octet).is_equal_to(0x10)
    asserts.assert_that(der.payload).is_equal_to(bytes([0x78, 0x78, 0x6c, 0x6c]))

        # Explicit tag may be 0
    der = DerObject(0x10, explicit=0)
    der.decode(der, bytes([0xa0, 0x06, 0x10, 0x04, 0x78, 0x78, 0x6c, 0x6c]))
    asserts.assert_that(der._inner_tag_octet).is_equal_to(0x10)
    asserts.assert_that(der.payload).is_equal_to(bytes([0x78, 0x78, 0x6c, 0x6c]))

def DerObjectTests_testObjDecode8():
        # Verify that decode returns the object
    der = DerObject(0x02)
    asserts.assert_that(der).is_equal_to(der.decode(der, bytes([0x02, 0x02, 0x01, 0x02])))


def DerIntegerTests_testInit1():
    der = DerInteger(1)
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x02, 0x01, 0x01]))

def DerIntegerTests_testEncode1():
        # Single-byte integers
        # Value 0
    der = DerInteger(0)
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x02, 0x01, 0x00]))
        # Value 1
    der = DerInteger(1)
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x02, 0x01, 0x01]))
        # Value 127
    der = DerInteger(127)
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x02, 0x01, 0x7f]))


def DerIntegerTests_testEncode2():
    # Multi-byte integers
    # Value 128
    der = DerInteger(128)
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x02, 0x02, 0x00, 0x80]))
    # Value 0x180
    der = DerInteger(0x180)
    encoded = der.encode()
    expected = bytes([0x02, 0x02, 0x01, 0x80])
    # print('encoded:', hexlify(encoded), ' expected: ', hexlify(expected))
    asserts.assert_that(encoded).is_equal_to(expected)
    # One very long integer
    der = DerInteger(pow(2, 2048))
    # tests to make sure that integer encodes propertly (using Java)
    encoded = der.encode()
    expected = bytearray([
      0x02, 0x82, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    ])
    # print(hexlify(encoded) == hexlify(expected))
    asserts.assert_that(encoded).is_equal_to(expected)


def DerIntegerTests_testEncode3():
    # Negative integers
    # Value -1
    der = DerInteger(-1)
    encoded = der.encode()
    expected = bytes([0x02, 0x01, 0xff])
    # print('encoded:', hexlify(encoded), ' expected: ', hexlify(expected))
    # asserts.assert_that(hexlify(encoded)).is_equal_to(hexlify(expected))
    asserts.assert_that(encoded).is_equal_to(expected)
    # Value -128
    der = DerInteger(-128)
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x02, 0x01, 0x80]))
    # Value
    der = DerInteger(-87873)
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x02, 0x03, 0xfe, 0xa8, 0xbf]))


def DerIntegerTests_testEncode4():
    # Explicit encoding
    number = DerInteger(0x34, explicit=3)
    asserts.assert_that(number.encode()).is_equal_to(bytes([0xa3, 0x03, 0x02, 0x01, 0x34]))

# -----

def DerIntegerTests_testDecode1():
    # Single-byte integer
    der = DerInteger()
    # Value 0
    der.decode(bytes([0x02, 0x01, 0x00]))
    asserts.assert_that(der.value).is_equal_to(0)
    # Value 1
    der.decode(bytes([0x02, 0x01, 0x01]))
    asserts.assert_that(der.value).is_equal_to(1)
    # Value 127
    der.decode(bytes([0x02, 0x01, 0x7f]))
    asserts.assert_that(der.value).is_equal_to(127)

def DerIntegerTests_testDecode2():
    # Multi-byte integer
    der = DerInteger()
    # Value 0x180L
    der.decode(bytes([0x02, 0x02, 0x01, 0x80]))
    asserts.assert_that(der.value).is_equal_to(0x180)
    # One very long integer
    der.decode(
        bytearray([
              0x02, 0x82, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
            ]))
    asserts.assert_that(der.value).is_equal_to(pow(2,2048))

def DerIntegerTests_testDecode3():
    # Negative integer
    der = DerInteger()
    # Value -1
    der.decode(bytes([0x02, 0x01, 0xff]))
    asserts.assert_that(der.value).is_equal_to(-1)
    # Value -32768
    der.decode(bytes([0x02, 0x02, 0x80, 0x00]))
    asserts.assert_that(der.value).is_equal_to(-32768)

def DerIntegerTests_testDecode5():
    # We still accept BER integer format
    der = DerInteger()
    # Redundant leading zeroes
    der.decode(bytes([0x02, 0x02, 0x00, 0x01]))
    asserts.assert_that(der.value).is_equal_to(1)
    # Redundant leading 0xFF
    der.decode(bytes([0x02, 0x02, 0xff, 0xff]))
    asserts.assert_that(der.value).is_equal_to(-1)
    # Empty payload
    der.decode(bytes([0x02, 0x00]))
    asserts.assert_that(der.value).is_equal_to(0)

def DerIntegerTests_testDecode6():
    # Explicit encoding
    number = DerInteger(explicit=3)
    number.decode(bytes([0xa3, 0x03, 0x02, 0x01, 0x34]))
    asserts.assert_that(number.value).is_equal_to(0x34)

def DerIntegerTests_testDecode7():
    # Verify decode returns the DerInteger
    der = DerInteger()
    asserts.assert_that(der).is_equal_to(der.decode(bytes([0x02, 0x01, 0x7f])))

    ###

def DerIntegerTests_testStrict1():
    number = DerInteger()

    number.decode(bytes([0x02, 0x02, 0x00, 0x01]))
    number.decode(bytes([0x02, 0x02, 0x00, 0x7f]))
    asserts.assert_fails(lambda : number.decode(bytes([0x02, 0x02, 0x00, 0x01]), strict=True), ".*?ValueError")
    asserts.assert_fails(lambda : number.decode(bytes([0x02, 0x02, 0x00, 0x7f]), strict=True), ".*?ValueError")

    ###

def DerIntegerTests_testErrDecode1():
    # Wide length field
    der = DerInteger()
    asserts.assert_fails(lambda : der.decode(bytes([0x02, 0x81, 0x01, 0x01])), ".*?ValueError")


def DerSequenceTests_testInit1():
    # I believe that pycryptodome asn1.py is broken for this test.
    # This can be verified by pyasn1 and bouncycastle. There is no
    # tagless encoding in the ASN1 specification if you use a DERSequence...
    der = DerSequence([1, DerInteger(2), bytes([0x30, 0x00])])
    # expected = bytes([0x30, 0x08, 0x02, 0x01, 0x01, 0x02, 0x01, 0x02, 0x30, 0x00])
    expected = bytes([0x30, 0x0a, 0x02, 0x01, 0x01, 0x02, 0x01, 0x02, 0x04, 0x02, 0x30, 0x00])
    actual = der.encode()
    asserts.assert_that(actual).is_equal_to(expected)


def DerSequenceTests_testEncode1():
    # Empty sequence
    der = DerSequence()
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x30, 0x00]))
    asserts.assert_that(der.hasOnlyInts()).is_false()
    # One single-byte integer (zero)
    der.append(0)
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x30, 0x03, 0x02, 0x01, 0x00]))
    asserts.assert_that(der.hasInts()).is_equal_to(1)
    asserts.assert_that(der.hasInts(False)).is_equal_to(1)
    asserts.assert_that(der.hasOnlyInts()).is_true()
    asserts.assert_that(der.hasOnlyInts(False)).is_true()
    # Invariant
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x30, 0x03, 0x02, 0x01, 0x00]))


def DerSequenceTests_testEncode2():
    # Indexing
    der = DerSequence()
    der.append(0)
    der.__setitem__(0, 1)
    asserts.assert_that(der.__len__()).is_equal_to(1)
    asserts.assert_that(der.__getitem__(0)).is_equal_to(1)
    asserts.assert_that(der.__getitem__(-1)).is_equal_to(1)
    asserts.assert_that(der.encode()).is_equal_to(b(r"0\x03\x02\x01\x01"))
    #
    der.__setslice__(0, der.__len__(), [1])
    asserts.assert_that(der.__len__()).is_equal_to(1)
    asserts.assert_that(der.__getitem__(0)).is_equal_to(1)
    asserts.assert_that(der.encode()).is_equal_to(b(r"0\x03\x02\x01\x01"))


def DerSequenceTests_testEncode3():
    # One multi-byte integer (non-zero)
    der = DerSequence()
    der.append(0x180)
    expected = bytearray([0x30, 0x04, 0x02, 0x02, 0x01, 0x80])
    asserts.assert_that(der.encode()).is_equal_to(expected)


def DerSequenceTests_testEncode4():
    # One very long integer
    der = DerSequence()
    der.append(pow(2, 2048))
    expected = bytearray([0x30, 0x82, 0x01, 0x05, 0x02, 0x82, 0x01, 0x01, 0x01] +
                ([0x00] * 256))
    encoded = der.encode()
    asserts.assert_that(encoded).is_equal_to(expected)


def DerSequenceTests_testEncode5():
    der = DerSequence()
    der.__iadd__(1)
    der.__iadd__(b([0x30, 0x00]))
    encoded = der.encode()
    expected = b([0x30, 0x07, 0x02, 0x01, 0x01, 0x04, 0x02, 0x30, 0x00])
    #        pyasn: 300702010104023000
    # pycryptodome: 3005020101    3000
    # bouncycastle: 300702010104023000
    asserts.assert_that(hexlify(encoded)).is_equal_to(hexlify(expected))


def DerSequenceTests_testEncode6():
    # Two positive integers
    der = DerSequence()
    der.append(0x180)
    der.append(0xFF)
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x30, 0x08, 0x02, 0x02, 0x01, 0x80, 0x02, 0x02, 0x00, 0xff]))
    asserts.assert_that(der.hasOnlyInts()).is_true()
    asserts.assert_that(der.hasOnlyInts(False)).is_true()
    # Two mixed integers
    der = DerSequence()
    der.append(2)
    der.append(-2)
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x30, 0x06, 0x02, 0x01, 0x02, 0x02, 0x01, 0xfe]))
    asserts.assert_that(der.hasInts()).is_equal_to(1)
    asserts.assert_that(der.hasInts(False)).is_equal_to(2)
    asserts.assert_that(der.hasOnlyInts()).is_false()
    asserts.assert_that(der.hasOnlyInts(False)).is_true()
    #
    der.append(0x01)
    der.__setslice__(1, der.__len__(), [9, 8])
    asserts.assert_that(der.__len__()).is_equal_to(3)
    asserts.assert_that(der.__getslice__(1, der.__len__())).is_equal_to([9, 8])
    asserts.assert_that(der.__getslice__(1, -1)).is_equal_to([9])
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x30, 0x09, 0x02, 0x01, 0x02, 0x02, 0x01, 0x09, 0x02, 0x01, 0x08]))



def DerSequenceTests_testEncode7():
    # One integer and another type (already encoded)
    der = DerSequence()
    der.append(0x180)
    der.append(bytes([0x30, 0x03, 0x02, 0x01, 0x05]))
    asserts.assert_that(
        der.encode()
    ).is_equal_to(
        bytes([0x30, 0x0b, 0x02, 0x02, 0x01, 0x80, 0x04, 0x05, 0x30, 0x03, 0x02, 0x01, 0x05]))
# pyasn:          [0x30, 0x0b, 0x02, 0x02, 0x01, 0x80, 0x04, 0x05, 0x30, 0x03, 0x02, 0x01, 0x05]
# pycrypto: bytes([0x30, 0x09, 0x02, 0x02, 0x01, 0x80, 0x30, 0x03, 0x02, 0x01, 0x05]))
    asserts.assert_that(der.hasOnlyInts()).is_false()


# Test will not pass since we do not enable recursion in Larky
def DerSequenceTests_testEncode8():
    # One integer and another type (yet to encode)
    der = DerSequence()
    der.append(0x180)
    der.append(DerSequence([5]))
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x30, 0x09, 0x02, 0x02, 0x01, 0x80, 0x30, 0x03, 0x02, 0x01, 0x05]))
    asserts.assert_that(der.hasOnlyInts()).is_false()

    ####


def DerSequenceTests_testDecode1():
    # Empty sequence
    der = DerSequence()
    der.decode(bytearray([0x30, 0x00]))
    asserts.assert_that(der.__len__()).is_equal_to(0)
    # One single-byte integer (zero)
    der.decode(bytes([0x30, 0x03, 0x02, 0x01, 0x00]))
    asserts.assert_that(der.__len__()).is_equal_to(1)
    asserts.assert_that(der.__getitem__(0)).is_equal_to(0)
    # Invariant
    der.decode(bytes([0x30, 0x03, 0x02, 0x01, 0x00]))
    asserts.assert_that(der.__len__()).is_equal_to(1)
    asserts.assert_that(der.__getitem__(0)).is_equal_to(0)


def DerSequenceTests_testDecode2():
    # One single-byte integer (non-zero)
    der = DerSequence()
    der.decode(bytearray([0x30, 0x03, 0x02, 0x01, 0x7f]))
    asserts.assert_that(der.__len__()).is_equal_to(1)
    asserts.assert_that(der.__getitem__(0)).is_equal_to(127)


def DerSequenceTests_testDecode4():
    # One very long integer
    der = DerSequence()
    der.decode(
        bytearray([0x30, 0x82, 0x01, 0x05])
           + bytearray([0x02, 0x82, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
           + bytearray([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]))
    asserts.assert_that(der.__len__()).is_equal_to(1)
    asserts.assert_that(der.__getitem__(0)).is_equal_to(pow(2, 2048))


def DerSequenceTests_testDecode6():
    # Two integers
    der = DerSequence()
    der.decode(bytearray([0x30, 0x08, 0x02, 0x02, 0x01, 0x80, 0x02, 0x02, 0x00, 0xff]))
    asserts.assert_that(der.__len__()).is_equal_to(2)
    asserts.assert_that(der.__getitem__(0)).is_equal_to(0x180)
    asserts.assert_that(der.__getitem__(1)).is_equal_to(0xFF)


def DerSequenceTests_testDecode7():
    # One integer and 2 other types
    der = DerSequence()
    der.decode(bytearray([0x30, 0x0a, 0x02, 0x02, 0x01, 0x80, 0x24, 0x02, 0xb6, 0x63, 0x12, 0x00]))
    asserts.assert_that(der.__len__()).is_equal_to(3)
    asserts.assert_that(der.__getitem__(0)).is_equal_to(0x180)
    asserts.assert_that(der.__getitem__(1)).is_equal_to(bytes([0x24, 0x02, 0xb6, 0x63]))
    asserts.assert_that(der.__getitem__(2)).is_equal_to(bytes([0x12, 0x00]))


def DerSequenceTests_testDecode8():
    # This test fails on both pyasn and bouncycastle, (again a bug in pycrypto)
    # Only 2 other types
    der = DerSequence()
    der.decode(bytearray([0x30, 0x06, 0x24, 0x02, 0xb6, 0x63, 0x12, 0x00]))
    asserts.assert_that(der.__len__()).is_equal_to(2)
    asserts.assert_that(der.__getitem__(0)).is_equal_to(bytes([0x24, 0x02, 0xb6, 0x63]))
    asserts.assert_that(der.__getitem__(1)).is_equal_to(bytes([0x12, 0x00]))
    asserts.assert_that(der.hasInts()).is_equal_to(0)
    asserts.assert_that(der.hasInts(False)).is_equal_to(0)
    asserts.assert_that(der.hasOnlyInts()).is_false()
    asserts.assert_that(der.hasOnlyInts(False)).is_false()


def DerSequenceTests_testDecode9():
    # Verify that decode returns itself
    der = DerSequence()
    asserts.assert_that(der).is_equal_to(der.decode(bytes([0x30, 0x03, 0x02, 0x01, 0x00])))

    ###


def DerSequenceTests_testErrDecode1():
    # Not a sequence
    der = DerSequence()
    asserts.assert_fails(lambda: der.decode(b(r"")), ".*?ValueError")
    asserts.assert_fails(lambda: der.decode(bytes([0x00])), ".*?ValueError")
    asserts.assert_fails(lambda: der.decode(bytes([0x30])), ".*?ValueError")


def DerSequenceTests_testErrDecode2():
    der = DerSequence()
    # Too much data
    asserts.assert_fails(lambda: der.decode(bytes([0x30, 0x00, 0x00])), ".*?ValueError")


def DerSequenceTests_testErrDecode3():
    # Wrong length format
    der = DerSequence()
    # Missing length in sub-item
    asserts.assert_fails(lambda : der.decode(bytes([0x30, 0x04, 0x02, 0x01, 0x01, 0x00])), ".*?ValueError")
    # Valid BER, but invalid DER length
    # I don't know how to do this with pycryptodome or pyasn..
    # asserts.assert_fails(lambda : der.decode(bytes([0x30, 0x81, 0x03, 0x02, 0x01, 0x01])), ".*?ValueError")
    # asserts.assert_fails(lambda : der.decode(bytes([0x30, 0x04, 0x02, 0x81, 0x01, 0x01])), ".*?ValueError")


def DerSequenceTests_test_expected_nr_elements():
    der_bin = DerSequence([1, 2, 3]).encode()

    DerSequence().decode(der_bin, nr_elements=3)
    DerSequence().decode(der_bin, nr_elements=(2, 3))
    asserts.assert_fails(
        lambda: DerSequence().decode(der_bin, nr_elements=1), ".*?ValueError"
    )
    asserts.assert_fails(
        lambda: DerSequence().decode(der_bin, nr_elements=(4, 5)), ".*?ValueError"
    )


def DerSequenceTests_test_expected_only_integers():

    der_bin1 = DerSequence([1, 2, 3]).encode()
    in_seq1 = DerSequence([3, 4]).encode()
    der_bin2 = DerSequence([1, 2, in_seq1]).encode()

    DerSequence().decode(der_bin1, only_ints_expected=True)
    DerSequence().decode(der_bin1, only_ints_expected=False)
    DerSequence().decode(der_bin2, only_ints_expected=False)
    asserts.assert_fails(
        lambda: DerSequence().decode(der_bin2, only_ints_expected=True), ".*?ValueError"
    )


def DerOctetStringTests_testInit1():
    der = DerOctetString(bytes([0xff]))
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x04, 0x01, 0xff]))

def DerOctetStringTests_testEncode1():
        # Empty sequence
    der = DerOctetString()
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x04, 0x00]))
        # Small payload
    der.payload = bytes([0x01, 0x02])
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x04, 0x02, 0x01, 0x02]))

    ####

def DerOctetStringTests_testDecode1():
        # Empty sequence
    der = DerOctetString()
    der.decode(bytes([0x04, 0x00]))
    asserts.assert_that(der.payload).is_equal_to(b(''))
        # Small payload
    der.decode(bytes([0x04, 0x02, 0x01, 0x02]))
    asserts.assert_that(der.payload).is_equal_to(bytes([0x01, 0x02]))

def DerOctetStringTests_testDecode2():
        # Verify that decode returns the object
    der = DerOctetString()
    asserts.assert_that(der).is_equal_to(der.decode(bytes([0x04, 0x00])))

def DerOctetStringTests_testErrDecode1():
        # No leftovers allowed
    der = DerOctetString()
    asserts.assert_fails(lambda : der.decode(bytes([0x04, 0x01, 0x01, 0xff])), ".*?ValueError")


def DerNullTests_testEncode1():
    der = DerNull()
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x05, 0x00]))

    ####

def DerNullTests_testDecode1():
    # Empty sequence
    der = DerNull()
    asserts.assert_that(der).is_equal_to(der.decode(bytes([0x05, 0x00])))


def DerObjectIdTests_testInit1():
    der = DerObjectId("1.1")
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x06, 0x01, 0x29]))

def DerObjectIdTests_testEncode1():
    der = DerObjectId('1.2.840.113549.1.1.1')
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01]))
    #
    der = DerObjectId()
    der.value = '1.2.840.113549.1.1.1'
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01]))


def DerObjectIdTests_testDecode1():
        # Empty sequence
    der = DerObjectId()
    der.decode(bytes([0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01]))
    asserts.assert_that(der.value).is_equal_to('1.2.840.113549.1.1.1')

def DerObjectIdTests_testDecode2():
        # Verify that decode returns the object
    der = DerObjectId()
    asserts.assert_that(der).is_equal_to(
            der.decode(bytes([0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01])))

def DerObjectIdTests_testDecode3():
    der = DerObjectId()
    der.decode(bytes([0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x00, 0x01]))
    asserts.assert_that(der.value).is_equal_to('1.2.840.113549.1.0.1')



def DerBitStringTests_testInit1():
    der = DerBitString(bytes([0xff]))
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x03, 0x02, 0x00, 0xff]))

def DerBitStringTests_testInit2():
    der = DerBitString(DerInteger(1))
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x03, 0x04, 0x00, 0x02, 0x01, 0x01]))

def DerBitStringTests_testEncode1():
        # Empty sequence
    der = DerBitString()
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x03, 0x01, 0x00]))
        # Small payload
    der = DerBitString(bytes([0x01, 0x02]))
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x03, 0x03, 0x00, 0x01, 0x02]))
        # Small payload
    der = DerBitString()
    der.value = bytes([0x01, 0x02])
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x03, 0x03, 0x00, 0x01, 0x02]))

    ####

def DerBitStringTests_testDecode1():
        # Empty sequence
    der = DerBitString()
    der.decode(bytes([0x03, 0x00]))
    asserts.assert_that(der.value).is_equal_to(b(''))
        # Small payload
    der.decode(bytes([0x03, 0x03, 0x00, 0x01, 0x02]))
    asserts.assert_that(der.value).is_equal_to(bytes([0x01, 0x02]))

def DerBitStringTests_testDecode2():
        # Verify that decode returns the object
    der = DerBitString()
    asserts.assert_that(der).is_equal_to(der.decode(bytes([0x03, 0x00])))



def DerSetOfTests_testInit1():
    der = DerSetOf([DerInteger(1), DerInteger(2)])
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x31, 0x06, 0x02, 0x01, 0x01, 0x02, 0x01, 0x02]))

def DerSetOfTests_testEncode1():
        # Empty set
    der = DerSetOf()
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x31, 0x00]))
        # One single-byte integer (zero)
    der.add(0)
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x31, 0x03, 0x02, 0x01, 0x00]))
        # Invariant
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x31, 0x03, 0x02, 0x01, 0x00]))

def DerSetOfTests_testEncode2():
        # Two integers
    der = DerSetOf()
    der.add(0x180)
    der.add(0xFF)
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x31, 0x08, 0x02, 0x02, 0x00, 0xff, 0x02, 0x02, 0x01, 0x80]))
        # Initialize with integers
    der = DerSetOf([0x180, 0xFF])
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x31, 0x08, 0x02, 0x02, 0x00, 0xff, 0x02, 0x02, 0x01, 0x80]))

def DerSetOfTests_testEncode3():
        # One integer and another type (no matter what it is)
    der = DerSetOf()
    der.add(0x180)
    asserts.assert_fails(lambda : der.add(bytes([0x00, 0x02, 0x00, 0x00])), ".*?ValueError")

def DerSetOfTests_testEncode4():
        # Only non integers
    der = DerSetOf()
    der.add(bytes([0x01, 0x00]))
    der.add(bytes([0x01, 0x01, 0x01]))
    asserts.assert_that(der.encode()).is_equal_to(bytes([0x31, 0x05, 0x01, 0x00, 0x01, 0x01, 0x01]))

    ####

def DerSetOfTests_testDecode1():
        # Empty sequence
    der = DerSetOf()
    der.decode(bytes([0x31, 0x00]))
    asserts.assert_that(len(der)).is_equal_to(0)
        # One single-byte integer (zero)
    der.decode(bytes([0x31, 0x03, 0x02, 0x01, 0x00]))
    asserts.assert_that(len(der)).is_equal_to(1)
    asserts.assert_that(list(der)).is_equal_to([0])

def DerSetOfTests_testDecode2():
        # Two integers
    der = DerSetOf()
    der.decode(bytes([0x31, 0x08, 0x02, 0x02, 0x01, 0x80, 0x02, 0x02, 0x00, 0xff]))
    asserts.assert_that(len(der)).is_equal_to(2)
    l = list(der)
    asserts.assert_that((0x180 in l)).is_true()
    asserts.assert_that((0xFF in l)).is_true()

def DerSetOfTests_testDecode3():
        # One integer and 2 other types
    der = DerSetOf()
        #import pdb; pdb.set_trace()
    asserts.assert_fails(lambda : der.decode(bytes([0x30, 0x0a, 0x02, 0x02, 0x01, 0x80, 0x24, 0x02, 0xb6, 0x63, 0x12, 0x00])), ".*?ValueError")

def DerSetOfTests_testDecode4():
        # Verify that decode returns the object
    der = DerSetOf()
    asserts.assert_that(der).is_equal_to(
            der.decode(bytes([0x31, 0x08, 0x02, 0x02, 0x01, 0x80, 0x02, 0x02, 0x00, 0xff])))

    ###

def DerSetOfTests_testErrDecode1():
        # No leftovers allowed
    der = DerSetOf()
    asserts.assert_fails(lambda : der.decode(bytes([0x31, 0x08, 0x02, 0x02, 0x01, 0x80, 0x02, 0x02, 0x00, 0xff, 0xaa])), ".*?ValueError")


def _testsuite():
    _suite = unittest.TestSuite()
    #
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjInit1))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjEncode1))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjEncode2))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjEncode3))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjEncode4))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjEncode5))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjDecode1))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjDecode2))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjDecode3))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjDecode4))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjDecode5))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjDecode6))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjDecode7))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectTests_testObjDecode8))
    #
    # _suite.addTest(unittest.FunctionTestCase(_test_DERInteger))
    # _suite.addTest(unittest.FunctionTestCase(DerIntegerTests_testInit1))
    # _suite.addTest(unittest.FunctionTestCase(DerIntegerTests_testEncode1))
    # _suite.addTest(unittest.FunctionTestCase(DerIntegerTests_testEncode2))
    # _suite.addTest(unittest.FunctionTestCase(DerIntegerTests_testEncode3))
    # # _suite.addTest(unittest.FunctionTestCase(DerIntegerTests_testEncode4))
    # _suite.addTest(unittest.FunctionTestCase(DerIntegerTests_testDecode1))
    # _suite.addTest(unittest.FunctionTestCase(DerIntegerTests_testDecode2))
    # _suite.addTest(unittest.FunctionTestCase(DerIntegerTests_testDecode3))
    # _suite.addTest(unittest.FunctionTestCase(DerIntegerTests_testDecode5))
    # _suite.addTest(unittest.FunctionTestCase(DerIntegerTests_testDecode6))
    # _suite.addTest(unittest.FunctionTestCase(DerIntegerTests_testDecode7))
    # _suite.addTest(unittest.FunctionTestCase(DerIntegerTests_testStrict1))
    # _suite.addTest(unittest.FunctionTestCase(DerIntegerTests_testErrDecode1))
    #
    #
    # # _suite.addTest(unittest.FunctionTestCase(_test_DERSequence))
    # # _suite.addTest(unittest.FunctionTestCase(test_JDERSequence))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testInit1))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testEncode1))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testEncode2))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testEncode3))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testEncode4))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testEncode5))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testEncode6))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testEncode7))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testEncode8))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testDecode1))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testDecode2))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testDecode4))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testDecode6))
    # _suite.addTest(
    #     # Error in decode: DEF length 99 object truncated by 99
    #     # Pyasn + pycryptodome ...
    #     unittest.expectedFailure(
    #         unittest.FunctionTestCase(DerSequenceTests_testDecode7)))
    # _suite.addTest(
    #     # On pyasn, this test fails with: SubstrateUnderrunError: 99-octet short
    #     # On bouncycastle, this test fails with: DEF length 99 object truncated by 99
    #     # I think it's a bug in pycryptodome
    #     unittest.expectedFailure(unittest.FunctionTestCase(DerSequenceTests_testDecode8))
    # )
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testDecode9))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testErrDecode1))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testErrDecode2))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_testErrDecode3))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_test_expected_nr_elements))
    # _suite.addTest(unittest.FunctionTestCase(DerSequenceTests_test_expected_only_integers))
    #
    # _suite.addTest(unittest.FunctionTestCase(DerNullTests_testEncode1))
    # _suite.addTest(unittest.FunctionTestCase(DerNullTests_testDecode1))
    #
    # _suite.addTest(unittest.FunctionTestCase(DerObjectIdTests_testInit1))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectIdTests_testEncode1))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectIdTests_testDecode1))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectIdTests_testDecode2))
    # _suite.addTest(unittest.FunctionTestCase(DerObjectIdTests_testDecode3))
    #
    # _suite.addTest(unittest.FunctionTestCase(DerOctetStringTests_testInit1))
    # _suite.addTest(unittest.FunctionTestCase(DerOctetStringTests_testEncode1))
    # _suite.addTest(unittest.FunctionTestCase(DerOctetStringTests_testDecode1))
    # _suite.addTest(unittest.FunctionTestCase(DerOctetStringTests_testDecode2))
    # _suite.addTest(unittest.FunctionTestCase(DerOctetStringTests_testErrDecode1))

    _suite.addTest(unittest.FunctionTestCase(DerBitStringTests_testInit1))
    _suite.addTest(unittest.FunctionTestCase(DerBitStringTests_testInit2))
    _suite.addTest(unittest.FunctionTestCase(DerBitStringTests_testEncode1))
    _suite.addTest(unittest.FunctionTestCase(DerBitStringTests_testDecode1))
    _suite.addTest(unittest.FunctionTestCase(DerBitStringTests_testDecode2))

    ###

    # _suite.addTest(unittest.FunctionTestCase(DerSetOfTests_testInit1))
    # _suite.addTest(unittest.FunctionTestCase(DerSetOfTests_testEncode1))
    # _suite.addTest(unittest.FunctionTestCase(DerSetOfTests_testEncode2))
    # _suite.addTest(unittest.FunctionTestCase(DerSetOfTests_testEncode3))
    # _suite.addTest(unittest.FunctionTestCase(DerSetOfTests_testEncode4))
    # _suite.addTest(unittest.FunctionTestCase(DerSetOfTests_testDecode1))
    # _suite.addTest(unittest.FunctionTestCase(DerSetOfTests_testDecode2))
    # _suite.addTest(unittest.FunctionTestCase(DerSetOfTests_testDecode3))
    # _suite.addTest(unittest.FunctionTestCase(DerSetOfTests_testDecode4))
    # _suite.addTest(unittest.FunctionTestCase(DerSetOfTests_testErrDecode1))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
