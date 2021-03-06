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

"""Self-test suite for Crypto.PublicKey.RSA"""
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//binascii", binascii="binascii")
load("@stdlib//types", types="types")
load("@stdlib//re", re="re")
load("@vendor//asserts", asserts="asserts")
load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//Crypto/Math/Numbers", Integer="Integer")
load("@vendor//Crypto/Util/number", bytes_to_long="bytes_to_long",
     inverse="inverse")

# Test vectors from "RSA-OAEP and RSA-PSS test vectors (.zip file)"
#   ftp://ftp.rsasecurity.com/pub/pkcs/pkcs-1/pkcs-1v2-1-vec.zip
# See RSADSI's PKCS#1 page at
#   http://www.rsa.com/rsalabs/node.asp?id=2125

# from oaep-int.txt

# TODO: PyCrypto treats the message as starting *after* the leading "00"
# TODO: That behaviour should probably be changed in the future.
plaintext = """
           eb 7a 19 ac e9 e3 00 63 50 e3 29 50 4b 45 e2
        ca 82 31 0b 26 dc d8 7d 5c 68 f1 ee a8 f5 52 67
        c3 1b 2e 8b b4 25 1f 84 d7 e0 b2 c0 46 26 f5 af
        f9 3e dc fb 25 c9 c2 b3 ff 8a e1 0e 83 9a 2d db
        4c dc fe 4f f4 77 28 b4 a1 b7 c1 36 2b aa d2 9a
        b4 8d 28 69 d5 02 41 21 43 58 11 59 1b e3 92 f9
        82 fb 3e 87 d0 95 ae b4 04 48 db 97 2f 3a c1 4f
        7b c2 75 19 52 81 ce 32 d2 f1 b7 6d 4d 35 3e 2d
    """

ciphertext = """
        12 53 e0 4d c0 a5 39 7b b4 4a 7a b8 7e 9b f2 a0
        39 a3 3d 1e 99 6f c8 2a 94 cc d3 00 74 c9 5d f7
        63 72 20 17 06 9e 52 68 da 5d 1c 0b 4f 87 2c f6
        53 c1 1d f8 23 14 a6 79 68 df ea e2 8d ef 04 bb
        6d 84 b1 c3 1d 65 4a 19 70 e5 78 3b d6 eb 96 a0
        24 c2 ca 2f 4a 90 fe 9f 2e f5 c9 c1 40 e5 bb 48
        da 95 36 ad 87 00 c8 4f c9 13 0a de a7 4e 55 8d
        51 a7 4d df 85 d8 b5 0d e9 68 38 d6 06 3e 09 55
    """

modulus = """
        bb f8 2f 09 06 82 ce 9c 23 38 ac 2b 9d a8 71 f7
        36 8d 07 ee d4 10 43 a4 40 d6 b6 f0 74 54 f5 1f
        b8 df ba af 03 5c 02 ab 61 ea 48 ce eb 6f cd 48
        76 ed 52 0d 60 e1 ec 46 19 71 9d 8a 5b 8b 80 7f
        af b8 e0 a3 df c7 37 72 3e e6 b4 b7 d9 3a 25 84
        ee 6a 64 9d 06 09 53 74 88 34 b2 45 45 98 39 4e
        e0 aa b1 2d 7b 61 a5 1f 52 7a 9a 41 f6 c1 68 7f
        e2 53 72 98 ca 2a 8f 59 46 f8 e5 fd 09 1d bd cb
    """

e = 0x11  # public exponent

prime_factor = """
        c9 7f b1 f0 27 f4 53 f6 34 12 33 ea aa d1 d9 35
        3f 6c 42 d0 88 66 b1 d0 5a 0f 20 35 02 8b 9d 86
        98 40 b4 16 66 b4 2e 92 ea 0d a3 b4 32 04 b5 cf
        ce 33 52 52 4d 04 16 a5 a4 41 e7 00 af 46 15 03
    """


def strip_whitespace(s):
    """Remove whitespace from a text or byte string"""
    if types.is_string(s):
        return bytes(re.sub(r'(\s|\x0B|\r?\n)+', '', s), encoding='utf-8')
    else:
        b = bytes('', encoding='utf-8')
        return b.join(s.split())


def a2b_hex(s):
    return binascii.a2b_hex(strip_whitespace(s))


def b2a_hex(s):
    return binascii.b2a_hex(s)


n = bytes_to_long(a2b_hex(modulus))
p = bytes_to_long(a2b_hex(prime_factor))

# Compute q, d, and u from n, e, and p
q = n // p
d = inverse(e, (p - 1) * (q - 1))
u = inverse(p, q)  # u = e**-1 (mod q)

rsa = RSA


def _check_private_key(rsaObj):
    # Check capabilities
    asserts.assert_that(True).is_equal_to(rsaObj.has_private())
    # Sanity check key data
    asserts.assert_that(rsaObj.n).is_equal_to(rsaObj.p * rsaObj.q)  # n = pq
    asserts.assert_that(True).is_equal_to((rsaObj.p > 1))  # p > 1
    asserts.assert_that(True).is_equal_to((rsaObj.q > 1))  # q > 1
    asserts.assert_that(True).is_equal_to((rsaObj.e > 1))  # e > 1
    asserts.assert_that(True).is_equal_to((rsaObj.d > 1))  # d > 1
    asserts.assert_that(1).is_equal_to(
        rsaObj.p * rsaObj.u % rsaObj.q)  # pu = 1 (mod q)
    lcm = Integer(rsaObj.p - 1).lcm(rsaObj.q - 1).__int__()
    asserts.assert_that(1).is_equal_to(
        rsaObj.d * rsaObj.e % lcm)  # ed = 1 (mod LCM(p-1, q-1))


def _check_public_key(rsaObj):
    _ciphertext = a2b_hex(ciphertext)

    # Check capabilities
    asserts.assert_that(False).is_equal_to(rsaObj.has_private())

    # Check rsaObj.[ne] -> rsaObj.[ne] mapping
    asserts.assert_that(rsaObj.n).is_equal_to(rsaObj.n)
    asserts.assert_that(rsaObj.e).is_equal_to(rsaObj.e)

    # Check that private parameters are all missing
    asserts.assert_that(False).is_equal_to(hasattr(rsaObj, '_d'))
    asserts.assert_that(False).is_equal_to(hasattr(rsaObj, '_p'))
    asserts.assert_that(False).is_equal_to(hasattr(rsaObj, '_q'))
    asserts.assert_that(False).is_equal_to(hasattr(rsaObj, '_u'))

    # Sanity check key data
    asserts.assert_that(True).is_equal_to((rsaObj.e > 1))  # e > 1

    # Public keys should not be able to sign or decrypt
    asserts.assert_fails(lambda: rsaObj._decrypt(bytes_to_long(_ciphertext)),
                         ".*?TypeError")

    # Check __eq__ and __ne__
    asserts.assert_that(
        rsaObj.public_key().__eq__(rsaObj.public_key())).is_equal_to(
        True)  # assert_
    asserts.assert_that(
        rsaObj.public_key().__ne__(rsaObj.public_key())).is_equal_to(
        False)  # failIf

    asserts.assert_true(rsaObj.publickey().__eq__(rsaObj.public_key()))


def _exercise_primitive(rsaObj):
    # Since we're using a randomly-generated key, we can't check the test
    # vector, but we can make sure encryption and decryption are inverse
    # operations.
    _ciphertext = bytes_to_long(a2b_hex(ciphertext))

    # Test decryption
    plaintext = rsaObj._decrypt(_ciphertext)

    # Test encryption (2 arguments)
    new_ciphertext2 = rsaObj._encrypt(plaintext)
    asserts.assert_that(_ciphertext).is_equal_to(new_ciphertext2)


def _exercise_public_primitive(rsaObj):
    _plaintext = a2b_hex(plaintext)

    # Test encryption (2 arguments)
    new_ciphertext2 = rsaObj._encrypt(bytes_to_long(_plaintext))


def _check_encryption(rsaObj):
    _plaintext = a2b_hex(plaintext)
    _ciphertext = a2b_hex(ciphertext)

    # Test encryption
    new_ciphertext2 = rsaObj._encrypt(bytes_to_long(_plaintext))
    asserts.assert_that(bytes_to_long(_ciphertext)).is_equal_to(new_ciphertext2)


def _check_decryption(rsaObj):
    _plaintext = bytes_to_long(a2b_hex(plaintext))
    _ciphertext = bytes_to_long(a2b_hex(ciphertext))

    # Test plain decryption
    new_plaintext = rsaObj._decrypt(_ciphertext)
    asserts.assert_that(_plaintext).is_equal_to(new_plaintext)


def RSATest_test_generate_1arg():
    """RSA (default implementation) generated key (1 argument)"""
    rsaObj = rsa.generate(1024)
    _check_private_key(rsaObj)
    _exercise_primitive(rsaObj)
    pub = rsaObj.public_key()
    _check_public_key(pub)
    _exercise_public_primitive(rsaObj)


def RSATest_test_generate_2arg():
    """RSA (default implementation) generated key (2 arguments)"""
    rsaObj = rsa.generate(1024, Random.new().read)
    _check_private_key(rsaObj)
    _exercise_primitive(rsaObj)
    pub = rsaObj.public_key()
    _check_public_key(pub)
    _exercise_public_primitive(rsaObj)


def RSATest_test_generate_3args():
    rsaObj = rsa.generate(1024, Random.new().read, e=65537)
    _check_private_key(rsaObj)
    _exercise_primitive(rsaObj)
    pub = rsaObj.public_key()
    _check_public_key(pub)
    _exercise_public_primitive(rsaObj)
    asserts.assert_that(65537).is_equal_to(rsaObj.e)


def RSATest_test_construct_2tuple():
    """RSA (default implementation) constructed key (2-tuple)"""
    pub = rsa.construct((n, e))
    _check_public_key(pub)
    _check_encryption(pub)


def RSATest_test_construct_3tuple():
    """RSA (default implementation) constructed key (3-tuple)"""
    rsaObj = rsa.construct((n, e, d))
    _check_encryption(rsaObj)
    _check_decryption(rsaObj)


def RSATest_test_construct_4tuple():
    """RSA (default implementation) constructed key (4-tuple)"""
    rsaObj = rsa.construct((n, e, d, p))
    _check_encryption(rsaObj)
    _check_decryption(rsaObj)


def RSATest_test_construct_5tuple():
    """RSA (default implementation) constructed key (5-tuple)"""
    rsaObj = rsa.construct((n, e, d, p, q))
    _check_private_key(rsaObj)
    _check_encryption(rsaObj)
    _check_decryption(rsaObj)


def RSATest_test_construct_6tuple():
    """RSA (default implementation) constructed key (6-tuple)"""
    rsaObj = rsa.construct((n, e, d, p, q, u))
    _check_private_key(rsaObj)
    _check_encryption(rsaObj)
    _check_decryption(rsaObj)


def RSATest_test_construct_bad_key2():
    tup = (n, 1)
    asserts.assert_fails(lambda : rsa.construct(tup), ".*?ValueError")

        # An even modulus is wrong
    tup = (n+1, e)
    asserts.assert_fails(lambda : rsa.construct(tup), ".*?ValueError")


def RSATest_test_construct_bad_key3():
    tup = (n, e, d+1)
    asserts.assert_fails(lambda : rsa.construct(tup), ".*?ValueError")


def RSATest_test_construct_bad_key5():
    tup = (n, e, d, p, p)
    asserts.assert_fails(lambda : rsa.construct(tup), ".*?ValueError")

    tup = (p*p, e, p, p)
    asserts.assert_fails(lambda : rsa.construct(tup), ".*?ValueError")

    tup = (p*p, 3, p, q)
    asserts.assert_fails(lambda : rsa.construct(tup), ".*?ValueError")


def RSATest_test_construct_bad_key6():
    tup = (n, e, d, p, q, 10)
    asserts.assert_fails(lambda : rsa.construct(tup), ".*?ValueError")

    tup = (n, e, d, p, q, inverse(q, p))
    asserts.assert_fails(lambda : rsa.construct(tup), ".*?ValueError")


def RSATest_test_factoring():
    rsaObj = rsa.construct([n, e, d])
    asserts.assert_that((rsaObj.p==p or rsaObj.p==q)).is_true()
    asserts.assert_that((rsaObj.q==p or rsaObj.q==q)).is_true()
    asserts.assert_that((rsaObj.q*rsaObj.p == n)).is_true()

    asserts.assert_fails(lambda : rsa.construct([n, e, n-1]), ".*?ValueError")


def RSATest_test_repr():
    rsaObj = rsa.construct((n, e, d, p, q))
    asserts.assert_that(repr(rsaObj)).is_instance_of(str)


def RSATest_test_raw_rsa_boundary():
    # The argument of every RSA raw operation (encrypt/decrypt) must be
    # non-negative and no larger than the modulus
    rsa_obj = rsa.generate(1024)

    asserts.assert_fails(lambda : rsa_obj._decrypt(rsa_obj.n), ".*?ValueError")
    asserts.assert_fails(lambda : rsa_obj._encrypt(rsa_obj.n), ".*?ValueError")

    asserts.assert_fails(lambda : rsa_obj._decrypt(-1), ".*?ValueError")
    asserts.assert_fails(lambda : rsa_obj._encrypt(-1), ".*?ValueError")


def RSATest_test_size():
    pub = rsa.construct((n, e))
    asserts.assert_that(pub.size_in_bits()).is_equal_to(1024)
    asserts.assert_that(pub.size_in_bytes()).is_equal_to(128)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_generate_1arg))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_generate_2arg))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_generate_3args))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_generate_2arg))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_generate_3args))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_construct_2tuple))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_construct_3tuple))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_construct_4tuple))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_construct_5tuple))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_construct_6tuple))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_construct_bad_key2))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_construct_bad_key3))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_construct_bad_key5))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_construct_bad_key6))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_factoring))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_repr))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_raw_rsa_boundary))
    _suite.addTest(unittest.FunctionTestCase(RSATest_test_size))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
