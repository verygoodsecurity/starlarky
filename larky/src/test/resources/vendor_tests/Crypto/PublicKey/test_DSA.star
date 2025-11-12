# -*- coding: utf-8 -*-
#
#  SelfTest/PublicKey/test_DSA.py: Self-test for the DSA primitive
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
"""Self-test suite for Crypto.PublicKey.DSA"""
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//types", types="types")
load("@stdlib//operator", operator="operator")
load("@stdlib//re", re="re")
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/Math/Primality", Primality="Primality")
load("@vendor//Crypto/Math/Numbers", Integer="Integer")
load("@vendor//Crypto/PublicKey/DSA", DSA="DSA")
load("@vendor//Crypto/st_common", a2b_hex="a2b_hex", b2a_hex="b2a_hex")
load("@vendor//Crypto/Util/number", bytes_to_long="bytes_to_long", inverse="inverse", size="size")
load("@vendor//Crypto/Util/py3compat", b="b", bchr="bchr", bstr="bstr", byte_string="byte_string", is_bytes="is_bytes", iter_range="iter_range", _copy_bytes="copy_bytes", tobytes="tobytes", bord="bord", tostr="tostr")
load("@vendor//asserts", asserts="asserts")

def _sws(s):
    """Remove whitespace from a text or byte string"""
    if types.is_string(s):
        return bytes(re.sub(r'(\s|\x0B|\r?\n)+', '', s), encoding='utf-8')
    else:
        b = bytes('', encoding='utf-8')
        return b.join(s.split())

# Test vector from "Appendix 5. Example of the DSA" of
# "Digital Signature Standard (DSS)",
# U.S. Department of Commerce/National Institute of Standards and Technology
# FIPS 186-2 (+Change Notice), 2000 January 27.
# http://csrc.nist.gov/publications/fips/fips186-2/fips186-2-change1.pdf

y = _sws("""19131871 d75b1612 a819f29d 78d1b0d7 346f7aa7 7bb62a85
                9bfd6c56 75da9d21 2d3a36ef 1672ef66 0b8c7c25 5cc0ec74
                858fba33 f44c0669 9630a76b 030ee333""")

g = _sws("""626d0278 39ea0a13 413163a5 5b4cb500 299d5522 956cefcb
                3bff10f3 99ce2c2e 71cb9de5 fa24babf 58e5b795 21925c9c
                c42e9f6f 464b088c c572af53 e6d78802""")

p = _sws("""8df2a494 492276aa 3d25759b b06869cb eac0d83a fb8d0cf7
                cbb8324f 0d7882e5 d0762fc5 b7210eaf c2e9adac 32ab7aac
                49693dfb f83724c2 ec0736ee 31c80291""")

q = _sws("""c773218c 737ec8ee 993b4f2d ed30f48e dace915f""")

x = _sws("""2070b322 3dba372f de1c0ffc 7b2e3b49 8b260614""")

k = _sws("""358dad57 1462710f 50e254cf 1a376b2b deaadfbf""")
k_inverse = _sws("""0d516729 8202e49b 4116ac10 4fc3f415 ae52f917""")
m = b2a_hex(b("abc"))
m_hash = _sws("""a9993e36 4706816a ba3e2571 7850c26c 9cd0d89d""")
r = _sws("""8bac1ab6 6410435c b7181f95 b16ab97c 92b341c0""")
s = _sws("""41e2345f 1f56df24 58f426d1 55b4ba2d b6dcd8c8""")

dsa = DSA

def DSATest_test_generate_1arg():
    """DSA (default implementation) generated key (1 argument)"""
    dsaObj = dsa.generate(1024)
    _check_private_key(dsaObj)
    pub = dsaObj.public_key()
    _check_public_key(pub)

def DSATest_test_generate_2arg():
    """DSA (default implementation) generated key (2 arguments)"""
    dsaObj = dsa.generate(1024, Random.new().read)
    _check_private_key(dsaObj)
    pub = dsaObj.public_key()
    _check_public_key(pub)

def DSATest_test_construct_4tuple():
    """DSA (default implementation) constructed key (4-tuple)"""
    (_y, _g, _p, _q) = [bytes_to_long(a2b_hex(param)) for param in (y, g, p, q)]
    dsaObj = dsa.construct((_y, _g, _p, _q))
    _test_verification(dsaObj)

def DSATest_test_construct_5tuple():
    """DSA (default implementation) constructed key (5-tuple)"""
    (_y, _g, _p, _q, _x) = [bytes_to_long(a2b_hex(param)) for param in (y, g, p, q, x)]
    dsaObj = dsa.construct((_y, _g, _p, _q, _x))
    _test_signing(dsaObj)
    _test_verification(dsaObj)

def DSATest_test_construct_bad_key4():
    (_y, _g, _p, _q) = [bytes_to_long(a2b_hex(param)) for param in (y, g, p, q)]
    tup = (_y, _g, _p+1, _q)
    asserts.assert_fails(lambda: dsa.construct(tup), ".*?ValueError")

    tup = (_y, _g, _p, _q+1)
    asserts.assert_fails(lambda: dsa.construct(tup), ".*?ValueError")

    tup = (_y, 1, _p, _q)
    asserts.assert_fails(lambda: dsa.construct(tup), ".*?ValueError")

def DSATest_test_construct_bad_key5():
    (_y, _g, _p, _q, _x) = [bytes_to_long(a2b_hex(param)) for param in (y, g, p, q, x)]
    tup = (_y, _g, _p, _q, _x+1)
    asserts.assert_fails(lambda: dsa.construct(tup), ".*?ValueError")

    tup =  (_y, _g, _p, _q, _q+10)
    asserts.assert_fails(lambda: dsa.construct(tup), ".*?ValueError")

def _check_private_key(dsaObj):
    # Check capabilities
    asserts.assert_that(True).is_equal_to(dsaObj.has_private())
    asserts.assert_that(True).is_equal_to(dsaObj.can_sign())
    asserts.assert_that(False).is_equal_to(dsaObj.can_encrypt())

    # Sanity check key data
    asserts.assert_that(True).is_equal_to(dsaObj.p > dsaObj.q)            # p > q
    asserts.assert_that(160).is_equal_to(size(dsaObj.q))               # size(q) == 160 bits
    asserts.assert_that(0).is_equal_to((dsaObj.p - 1) % dsaObj.q)      # q is a divisor of p-1
    asserts.assert_that(dsaObj.y).is_equal_to(pow(dsaObj.g, dsaObj.x, dsaObj.p))     # y == g**x mod p
    asserts.assert_that(True).is_equal_to((0 < dsaObj.x) and (dsaObj.x < dsaObj.q))       # 0 < x < q

def _check_public_key(dsaObj):
    _k = bytes_to_long(a2b_hex(k))
    _m_hash = bytes_to_long(a2b_hex(m_hash))

    # Check capabilities
    asserts.assert_that(False).is_equal_to(dsaObj.has_private())
    asserts.assert_that(True).is_equal_to(dsaObj.can_sign())
    asserts.assert_that(False).is_equal_to(dsaObj.can_encrypt())

    # Check that private parameters are all missing
    asserts.assert_that(False).is_equal_to(hasattr(dsaObj, 'x'))

    # Sanity check key data
    asserts.assert_that(True).is_equal_to(dsaObj.p > dsaObj.q)            # p > q
    asserts.assert_that(160).is_equal_to(size(dsaObj.q))               # size(q) == 160 bits
    asserts.assert_that(0).is_equal_to((dsaObj.p - 1) % dsaObj.q)      # q is a divisor of p-1

    # Public-only key objects should raise an error when .sign() is called
    asserts.assert_fails(lambda: dsaObj._sign(_m_hash, _k), ".*?TypeError")

    # Check __eq__ and __ne__
    asserts.assert_that(dsaObj.public_key() == dsaObj.public_key()).is_equal_to(True) # assert_
    asserts.assert_that(dsaObj.public_key() != dsaObj.public_key()).is_equal_to(False) # assertFalse

    asserts.assert_that(dsaObj.public_key()).is_equal_to(dsaObj.publickey())

def _test_signing(dsaObj):
    _k = bytes_to_long(a2b_hex(k))
    _m_hash = bytes_to_long(a2b_hex(m_hash))
    _r = bytes_to_long(a2b_hex(r))
    _s = bytes_to_long(a2b_hex(s))
    (r_out, s_out) = dsaObj._sign(_m_hash, _k)
    asserts.assert_that((_r, _s)).is_equal_to((r_out, s_out))

def _test_verification(dsaObj):
    _m_hash = bytes_to_long(a2b_hex(m_hash))
    _r = bytes_to_long(a2b_hex(r))
    _s = bytes_to_long(a2b_hex(s))
    asserts.assert_that(dsaObj._verify(_m_hash, (_r, _s))).is_true()
    asserts.assert_that(dsaObj._verify(_m_hash + 1, (_r, _s))).is_false()

def DSATest_test_repr():
    (_y, _g, _p, _q) = [bytes_to_long(a2b_hex(param)) for param in (y, g, p, q)]
    dsaObj = dsa.construct((_y, _g, _p, _q))
    repr(dsaObj)

def DSADomainTest_test_domain1():
    """Verify we can generate new keys in a given domain"""
    dsa_key_1 = DSA.generate(1024)
    domain_params = dsa_key_1.domain()

    dsa_key_2 = DSA.generate(1024, domain=domain_params)
    asserts.assert_that(dsa_key_1.p).is_equal_to(dsa_key_2.p)
    asserts.assert_that(dsa_key_1.q).is_equal_to(dsa_key_2.q)
    asserts.assert_that(dsa_key_1.g).is_equal_to(dsa_key_2.g)

    asserts.assert_that(dsa_key_1.domain()).is_equal_to(dsa_key_2.domain())

def _get_weak_domain():
    p = Integer(4)
    for _while_ in larky.while_true():
        if not (p.size_in_bits() != 1024 or Primality.test_probable_prime(p) != Primality.PROBABLY_PRIME):
            break
        q1 = Integer.random(exact_bits=80)
        q2 = Integer.random(exact_bits=80)
        q = q1 * q2
        z = Integer.random(exact_bits=1024-160)
        p = z * q + 1

    h = Integer(2)
    g = 1
    for _while_ in larky.while_true():
        if g != 1:
            break
        g = pow(int(h), int(z), int(p))
        h += 1

    return (p, q, g)


def DSADomainTest_test_error_weak_domain():
    asserts.assert_fails(lambda: _get_weak_domain(), "Iteration limit exceeded!")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(DSATest_test_generate_1arg))
    _suite.addTest(unittest.FunctionTestCase(DSATest_test_generate_2arg))
    _suite.addTest(unittest.FunctionTestCase(DSATest_test_construct_4tuple))
    _suite.addTest(unittest.FunctionTestCase(DSATest_test_construct_5tuple))
    _suite.addTest(unittest.FunctionTestCase(DSATest_test_construct_bad_key4))
    _suite.addTest(unittest.FunctionTestCase(DSATest_test_construct_bad_key5))
    _suite.addTest(unittest.FunctionTestCase(DSATest_test_repr))
    _suite.addTest(unittest.FunctionTestCase(DSADomainTest_test_domain1))
    _suite.addTest(unittest.FunctionTestCase(DSADomainTest_test_error_weak_domain))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
