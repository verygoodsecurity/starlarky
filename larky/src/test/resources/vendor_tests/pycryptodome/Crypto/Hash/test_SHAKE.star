# ===================================================================
#
# Copyright (c) 2015, Legrandin <helderijs@gmail.com>
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

"""Self-test suite for Crypto.Hash.SHAKE128 and SHAKE256"""

load("@stdlib//binascii", hexlify="hexlify", unhexlify="unhexlify")
load("@vendor//Crypto/Hash/SHAKE128", SHAKE128="SHAKE128")
load("@vendor//Crypto/Util/py3compat", b="b", bchr="bchr", bord="bord", tobytes="tobytes")
load("@vendor//asserts","asserts")
load("@stdlib//unittest","unittest")
load("@stdlib//types", "types")

shake = SHAKE128

def SHAKETest_test_new_positive():

    xof1 = shake.new()
    xof2 = shake.new(data=b("90"))
    xof3 = shake.new().update(b("90"))

    asserts.assert_that(xof1.read(10)).is_not_equal_to(xof2.read(10))
    xof3.read(10)
    asserts.assert_that(xof2.read(10)).is_equal_to(xof3.read(10))

def SHAKETest_test_update():
    pieces = [bytearray(bchr(10) * 200), bytearray(bchr(20) * 300)]
    h = shake.new()
    h.update(pieces[0]).update(pieces[1])
    digest = h.read(10)
    h = shake.new()
    h.update(pieces[0] + pieces[1])
    asserts.assert_that(h.read(10)).is_equal_to(digest)

def SHAKETest_test_update_negative():
    h = shake.new()
    asserts.assert_fails(lambda : h.update("string"), ".*?TypeError")

def SHAKETest_test_digest():
    h = shake.new()
    digest = h.read(90)

    # read returns a byte string of the right length
    asserts.assert_that(types.is_instance(digest, type(b("digest")))).is_true()
    asserts.assert_that(len(digest)).is_equal_to(90)

def SHAKETest_test_update_after_read():
    mac = shake.new()
    mac.update(b("rrrr"))
    mac.read(90)
    asserts.assert_fails(lambda : mac.update(b("ttt")), ".*?TypeError")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(SHAKETest_test_new_positive))
    _suite.addTest(unittest.FunctionTestCase(SHAKETest_test_update))
    _suite.addTest(unittest.FunctionTestCase(SHAKETest_test_update_negative))
    _suite.addTest(unittest.FunctionTestCase(SHAKETest_test_digest))
    _suite.addTest(unittest.FunctionTestCase(SHAKETest_test_update_after_read))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
