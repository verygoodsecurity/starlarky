#
#  SelfTest/Util/test_strxor.py: Self-test for XORing
#
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

load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//builtins", "builtins")
load("@stdlib//unittest", "unittest")
load("@vendor//Crypto/Util/strxor", strxor="strxor", strxor_c="strxor_c")
load("@vendor//asserts", "asserts")

bytearray = builtins.bytearray
bytes = builtins.bytes

#
# def test1():
#     term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
#     term2 = unhexlify(bytes("383d4ba020573314395b", encoding="utf-8"))
#     result = unhexlify(bytes("c70ed123c59a7fcb6f12", encoding="utf-8"))
#     asserts.assert_that(strxor(term1, term2)).is_equal_to(result)
#     asserts.assert_that(strxor(term2, term1)).is_equal_to(result)
#
# def test2():
#     es = builtins.bytes(r"", encoding='utf-8')
#     asserts.assert_that(strxor(es, es)).is_equal_to(es)
#
# def test3():
#     term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
#     all_zeros = builtins.bytes(r"\x00", encoding='utf-8') * len(term1)
#     asserts.assert_that(strxor(term1, term1)).is_equal_to(all_zeros)

def test_wrong_length():
    term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
    term2 = unhexlify(bytes("ff339a83e5cd4cdf564990", encoding="utf-8"))
    asserts.assert_fails(lambda : strxor(term1, term2), ".*?ValueError")

def test_bytearray():
    term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
    term1_ba = bytearray(term1)
    term2 = unhexlify(bytes("383d4ba020573314395b", encoding="utf-8"))
    result = unhexlify(bytes("c70ed123c59a7fcb6f12", encoding="utf-8"))

    asserts.assert_that(strxor(term1_ba, term2)).is_equal_to(result)

def test_memoryview():
    term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
    term1_mv = builtins.bytearray(term1)
    term2 = unhexlify(bytes("383d4ba020573314395b", encoding="utf-8"))
    result = unhexlify(bytes("c70ed123c59a7fcb6f12", encoding="utf-8"))

    asserts.assert_that(strxor(term1_mv, term2)).is_equal_to(result)

def test_output_bytearray():
    """Verify result can be stored in pre-allocated memory"""

    term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
    term2 = unhexlify(bytes("383d4ba020573314395b", encoding="utf-8"))
    original_term1 = term1[:]
    original_term2 = term2[:]
    expected_xor = unhexlify(bytes("c70ed123c59a7fcb6f12", encoding="utf-8"))
    output = bytearray(len(term1))

    result = strxor(term1, term2, output=output)

    asserts.assert_that(result).is_equal_to(None)
    asserts.assert_that(output).is_equal_to(expected_xor)
    asserts.assert_that(term1).is_equal_to(original_term1)
    asserts.assert_that(term2).is_equal_to(original_term2)

def test_output_memoryview():
    """Verify result can be stored in pre-allocated memory"""

    term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
    term2 = unhexlify(bytes("383d4ba020573314395b", encoding="utf-8"))
    original_term1 = term1[:]
    original_term2 = term2[:]
    expected_xor = unhexlify(bytes("c70ed123c59a7fcb6f12", encoding="utf-8"))
    output = builtins.bytearray(bytearray(len(term1)))

    result = strxor(term1, term2, output=output)

    asserts.assert_that(result).is_equal_to(None)
    asserts.assert_that(output).is_equal_to(expected_xor)
    asserts.assert_that(term1).is_equal_to(original_term1)
    asserts.assert_that(term2).is_equal_to(original_term2)

def test_output_overlapping_bytearray():
    """Verify result can be stored in overlapping memory"""

    term1 = bytearray(unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8")))
    term2 = unhexlify(bytes("383d4ba020573314395b", encoding="utf-8"))
    original_term2 = term2[:]
    expected_xor = unhexlify(bytes("c70ed123c59a7fcb6f12", encoding="utf-8"))

    result = strxor(term1, term2, output=term1)

    asserts.assert_that(result).is_equal_to(None)
    asserts.assert_that(term1).is_equal_to(expected_xor)
    asserts.assert_that(term2).is_equal_to(original_term2)

def test_output_overlapping_memoryview():
    """Verify result can be stored in overlapping memory"""

    term1 = builtins.bytearray(bytearray(unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))))
    term2 = unhexlify(bytes("383d4ba020573314395b", encoding="utf-8"))
    original_term2 = term2[:]
    expected_xor = unhexlify(bytes("c70ed123c59a7fcb6f12", encoding="utf-8"))

    result = strxor(term1, term2, output=term1)

    asserts.assert_that(result).is_equal_to(None)
    asserts.assert_that(term1).is_equal_to(expected_xor)
    asserts.assert_that(term2).is_equal_to(original_term2)

def test_output_ro_bytes():
    """Verify result cannot be stored in read-only memory"""

    term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
    term2 = unhexlify(bytes("383d4ba020573314395b", encoding="utf-8"))

    asserts.assert_fails(lambda : strxor(term1, term2, output=term1), ".*?TypeError")

def test_output_ro_memoryview():
    """Verify result cannot be stored in read-only memory"""

    term1 = builtins.bytearray(unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8")))
    term2 = unhexlify(bytes("383d4ba020573314395b", encoding="utf-8"))

    asserts.assert_fails(lambda : strxor(term1, term2, output=term1), ".*?TypeError")

def test_output_incorrect_length():
    """Verify result cannot be stored in memory of incorrect length"""

    term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
    term2 = unhexlify(bytes("383d4ba020573314395b", encoding="utf-8"))
    output = bytearray(len(term1) - 1)

    asserts.assert_fails(lambda : strxor(term1, term2, output=output), ".*?ValueError")


#
# def test1():
#     term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
#     result = unhexlify(bytes("be72dbc2a48c0d9e1708", encoding="utf-8"))
#     asserts.assert_that(strxor_c(term1, 65)).is_equal_to(result)
#
# def test2():
#     term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
#     asserts.assert_that(strxor_c(term1, 0)).is_equal_to(term1)
#
# def test3():
#     asserts.assert_that(strxor_c(builtins.bytes(r"", encoding='utf-8'), 90)).is_equal_to(builtins.bytes(r"", encoding='utf-8'))

def test_wrong_range():
    term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
    asserts.assert_fails(lambda : strxor_c(term1, -1), ".*?ValueError")
    asserts.assert_fails(lambda : strxor_c(term1, 256), ".*?ValueError")

def test_bytearray():
    term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
    term1_ba = bytearray(term1)
    result = unhexlify(bytes("be72dbc2a48c0d9e1708", encoding="utf-8"))

    asserts.assert_that(strxor_c(term1_ba, 65)).is_equal_to(result)

def test_memoryview():
    term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
    term1_mv = builtins.bytearray(term1)
    result = unhexlify(bytes("be72dbc2a48c0d9e1708", encoding="utf-8"))

    asserts.assert_that(strxor_c(term1_mv, 65)).is_equal_to(result)

def test_output_bytearray():
    term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
    original_term1 = term1[:]
    expected_result = unhexlify(bytes("be72dbc2a48c0d9e1708", encoding="utf-8"))
    output = bytearray(len(term1))

    result = strxor_c(term1, 65, output=output)

    asserts.assert_that(result).is_equal_to(None)
    asserts.assert_that(output).is_equal_to(expected_result)
    asserts.assert_that(term1).is_equal_to(original_term1)

def test_output_memoryview():
    term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
    original_term1 = term1[:]
    expected_result = unhexlify(bytes("be72dbc2a48c0d9e1708", encoding="utf-8"))
    output = builtins.bytearray(bytearray(len(term1)))

    result = strxor_c(term1, 65, output=output)

    asserts.assert_that(result).is_equal_to(None)
    asserts.assert_that(output).is_equal_to(expected_result)
    asserts.assert_that(term1).is_equal_to(original_term1)

def test_output_overlapping_bytearray():
    """Verify result can be stored in overlapping memory"""

    term1 = bytearray(unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8")))
    expected_xor = unhexlify(bytes("be72dbc2a48c0d9e1708", encoding="utf-8"))

    result = strxor_c(term1, 65, output=term1)

    asserts.assert_that(result).is_equal_to(None)
    asserts.assert_that(term1).is_equal_to(expected_xor)

def test_output_overlapping_memoryview():
    """Verify result can be stored in overlapping memory"""

    term1 = builtins.bytearray(bytearray(unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))))
    expected_xor = unhexlify(bytes("be72dbc2a48c0d9e1708", encoding="utf-8"))

    result = strxor_c(term1, 65, output=term1)

    asserts.assert_that(result).is_equal_to(None)
    asserts.assert_that(term1).is_equal_to(expected_xor)

def test_output_ro_bytes():
    """Verify result cannot be stored in read-only memory"""

    term1 = bytes(unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"), encoding="utf-8"))

    asserts.assert_fails(lambda : strxor_c(term1, 65, output=term1), ".*?TypeError")

def test_output_ro_memoryview():
    """Verify result cannot be stored in read-only memory"""

    term1 = builtins.bytes(unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8")))
    term2 = unhexlify(bytes("383d4ba020573314395b", encoding="utf-8"))

    asserts.assert_fails(lambda : strxor_c(term1, 65, output=term1), ".*?TypeError")

def test_output_incorrect_length():
    """Verify result cannot be stored in memory of incorrect length"""

    term1 = unhexlify(bytes("ff339a83e5cd4cdf5649", encoding="utf-8"))
    output = bytearray(len(term1) - 1)

    asserts.assert_fails(lambda : strxor_c(term1, 65, output=output), ".*?ValueError")


def _testsuite():
    _suite = unittest.TestSuite()
    # _suite.addTest(unittest.FunctionTestCase(test1))
    # _suite.addTest(unittest.FunctionTestCase(test2))
    # _suite.addTest(unittest.FunctionTestCase(test3))
    _suite.addTest(unittest.FunctionTestCase(test_wrong_length))
    _suite.addTest(unittest.FunctionTestCase(test_bytearray))
    _suite.addTest(unittest.FunctionTestCase(test_memoryview))
    _suite.addTest(unittest.FunctionTestCase(test_output_bytearray))
    _suite.addTest(unittest.FunctionTestCase(test_output_memoryview))
    _suite.addTest(unittest.FunctionTestCase(test_output_overlapping_bytearray))
    _suite.addTest(unittest.FunctionTestCase(test_output_overlapping_memoryview))
    _suite.addTest(unittest.FunctionTestCase(test_output_ro_bytes))
    _suite.addTest(unittest.FunctionTestCase(test_output_ro_memoryview))
    _suite.addTest(unittest.FunctionTestCase(test_output_incorrect_length))
    # _suite.addTest(unittest.FunctionTestCase(test1))
    # _suite.addTest(unittest.FunctionTestCase(test2))
    # _suite.addTest(unittest.FunctionTestCase(test3))
    _suite.addTest(unittest.FunctionTestCase(test_wrong_range))
    _suite.addTest(unittest.FunctionTestCase(test_bytearray))
    _suite.addTest(unittest.FunctionTestCase(test_memoryview))
    _suite.addTest(unittest.FunctionTestCase(test_output_bytearray))
    _suite.addTest(unittest.FunctionTestCase(test_output_memoryview))
    _suite.addTest(unittest.FunctionTestCase(test_output_overlapping_bytearray))
    _suite.addTest(unittest.FunctionTestCase(test_output_overlapping_memoryview))
    _suite.addTest(unittest.FunctionTestCase(test_output_ro_bytes))
    _suite.addTest(unittest.FunctionTestCase(test_output_ro_memoryview))
    _suite.addTest(unittest.FunctionTestCase(test_output_incorrect_length))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
