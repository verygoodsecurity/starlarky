# Ported from pydukpt 0.1.0 (3778da71): tests/test_pinblock.py. See larky/src/main/resources/VENDORED.md.
#
# MIT License, Copyright (c) 2026 Mahmoud Abdelkader; see
# larky/src/main/resources/vendor/pydukpt/__init__.star for the full notice.
#
# pydukpt NOTICE (test vectors): the ANSI X9.24-1:2009 Annex A.4.2 values in
# tests/test_published_vectors.py are as transcribed in moov-io/dukpt
# (https://github.com/moov-io/dukpt, Apache License 2.0),
# pkg/des/des_test.go at commit 9cb72e64a3eab8ec2dd1b9824b9f8ebba2b5ad4d.
# tests/fixtures/vectors.json was computed with moov-io/dukpt and
# cross-checked with varhenn/dukpt_des_pinblock
# (https://github.com/varhenn/dukpt_des_pinblock, BSD 2-Clause License,
# Copyright (c) 2022, varhenn) and dukpt 1.0.1; neither is included.
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//pydukpt", "decode_pin_block", "decrypt_pin", "encode_pin_block", "encrypt_pin")
load("./conftest", Examples="Examples", h="h")

PAN = "4012345678909"
KEY = h("042666B49184CFA368DE9628D0397BC9")


def _xor(a, b):
    return bytes([x ^ y for x, y in zip(a, b)])


def test_clear_block_example():
    # PIN field 041234FFFFFFFFFF XOR PAN field 0000401234567890.
    asserts.assert_that(encode_pin_block("1234", PAN)).is_equal_to(h("041274EDCBA9876F"))
    asserts.assert_that(decode_pin_block(h("041274EDCBA9876F"), PAN)).is_equal_to("1234")


def test_pin_length_is_one_hex_nibble():
    block = encode_pin_block("123456789012", "0000000000000")
    asserts.assert_that(block).is_equal_to(h("0C123456789012FF"))


def test_short_pan_is_zero_padded():
    # 8 digits: the 7 digits before the check digit, right-justified.
    asserts.assert_that(encode_pin_block("1234", "12345678")).is_equal_to(
        _xor(h("041234FFFFFFFFFF"), h("0000000001234567")))


def test_long_pan_uses_rightmost_12_before_check_digit():
    asserts.assert_that(encode_pin_block("1234", "9994012345678909")).is_equal_to(
        encode_pin_block("1234", PAN))


def test_round_trip():
    # @given(text(digits, 4, 12), text(digits, 8, 19), binary(16)); Hypothesis'
    # default is 100 examples.
    ex = Examples(4)
    for _ in range(100):
        pin = ex.text("0123456789", 4, 12)
        pan = ex.text("0123456789", 8, 19)
        key = ex.binary(16)
        asserts.assert_that(decode_pin_block(encode_pin_block(pin, pan), pan)).is_equal_to(pin)
        asserts.assert_that(decrypt_pin(key, encrypt_pin(key, pin, pan), pan)).is_equal_to(pin)


def test_bad_pin(pin):
    asserts.assert_fails(lambda: encode_pin_block(pin, PAN),
                         ".*?ValueError: pin must be 4 to 12 decimal digits")


def test_bad_pan(pan):
    asserts.assert_fails(lambda: encode_pin_block("1234", pan),
                         ".*?ValueError: pan must be 8 to 19 decimal digits")


def test_types():
    asserts.assert_fails(lambda: encode_pin_block(1234, PAN), ".*?TypeError: ")
    asserts.assert_fails(lambda: encode_pin_block("1234", 4012345678909), ".*?TypeError: ")
    asserts.assert_fails(lambda: decode_pin_block("041274EDCBA9876F", PAN), ".*?TypeError: ")


_DECODE_REJECTS = [
    ("141234FFFFFFFFFF", "format 0"),  # control nibble 1
    ("031234FFFFFFFFFF", "PIN length"),
    ("0D1234FFFFFFFFFF", "PIN length"),
    ("04123AFFFFFFFFFF", "malformed"),  # non-digit PIN nibble
    ("041234FFFFFFFFFE", "malformed"),  # fill nibble not F
]


def test_decode_rejects(clear, message):
    pan_field = h("0000401234567890")
    block = _xor(h(clear), pan_field)
    asserts.assert_fails(lambda: decode_pin_block(block, PAN), ".*?ValueError: .*?" + message)


def test_decrypt_with_wrong_pan_or_key_fails():
    block = encrypt_pin(KEY, "1234", PAN)
    asserts.assert_fails(lambda: decrypt_pin(KEY, block, "4012345678917"), ".*?ValueError: .*?PIN block")
    asserts.assert_fails(lambda: decrypt_pin(bytes([0] * 16), block, PAN), ".*?ValueError: .*?PIN block")


def test_block_length():
    asserts.assert_fails(lambda: decrypt_pin(KEY, b"\0" * 7, PAN), ".*?ValueError: .*?8 bytes")
    asserts.assert_fails(lambda: decode_pin_block(b"\0" * 9, PAN), ".*?ValueError: .*?8 bytes")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_clear_block_example))
    _suite.addTest(unittest.FunctionTestCase(test_pin_length_is_one_hex_nibble))
    _suite.addTest(unittest.FunctionTestCase(test_short_pan_is_zero_padded))
    _suite.addTest(unittest.FunctionTestCase(test_long_pan_uses_rightmost_12_before_check_digit))
    _suite.addTest(unittest.FunctionTestCase(test_round_trip))
    larky.parametrize(_suite.addTest, unittest.FunctionTestCase, "pin",
                      ["123", "1234567890123", "12a4", "", "１２３４", " 1234"])(test_bad_pin)
    larky.parametrize(_suite.addTest, unittest.FunctionTestCase, "pan",
                      ["1234567", "1" * 20, "40123456789O9", ""])(test_bad_pan)
    _suite.addTest(unittest.FunctionTestCase(test_types))
    larky.parametrize(_suite.addTest, unittest.FunctionTestCase,
                      "clear,message", _DECODE_REJECTS)(test_decode_rejects)
    _suite.addTest(unittest.FunctionTestCase(test_decrypt_with_wrong_pan_or_key_fails))
    _suite.addTest(unittest.FunctionTestCase(test_block_length))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
