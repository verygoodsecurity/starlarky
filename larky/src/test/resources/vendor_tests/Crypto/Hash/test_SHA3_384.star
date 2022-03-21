# -*- coding: utf-8 -*-
#
# SelfTest/Hash/test_SHA3_384.py: Self-test for the SHA-3/384 hash function
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

"""Self-test suite for Crypto.Hash.SHA3_384"""
load("@stdlib//binascii", hexlify="hexlify")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//Crypto/Hash", SHA3="SHA3_384")
load("@vendor//Crypto/Util/py3compat", b="b")
load("@vendor//asserts", asserts="asserts")

def APITest_test_update_after_digest():
    msg=b("rrrrttt")

    # Normally, update() cannot be done after digest()
    h = SHA3.new(data=msg[:4])
    dig1 = h.digest()
    asserts.assert_fails(lambda: h.update(msg[4:]), ".*?TypeError")
    dig2 = SHA3.new(data=msg).digest()

    # With the proper flag, it is allowed
    h = SHA3.new(data=msg[:4], update_after_digest=True)
    asserts.assert_that(h.digest()).is_equal_to(dig1)
    # ... and the subsequent digest applies to the entire message
    # up to that point
    h.update(msg[4:])
    # asserts.assert_that(h.digest()).is_equal_to(dig2)
    

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(APITest_test_update_after_digest))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())