# Ported from pydukpt 0.1.0 (3778da71): tests/test_keys.py. See larky/src/main/resources/VENDORED.md.
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
load("@vendor//Crypto/Cipher/DES", DES="DES")
load("@vendor//pydukpt", pydukpt="pydukpt")
load("./conftest", Examples="Examples", h="h")

BDK = h("0123456789ABCDEFFEDCBA9876543210")
KSN = h("FFFF9876543210E00000")
IPEK = h("6AC292FAA1315B4D858AB3A3D7D5933A")


def _xor(a, b):
    return bytes([x ^ y for x, y in zip(a, b)])


def test_variant_masks():
    key = bytes([0] * 16)
    asserts.assert_that(pydukpt.pin_key(key)).is_equal_to(pydukpt.PIN_VARIANT)
    asserts.assert_that(pydukpt.mac_request_key(key)).is_equal_to(pydukpt.MAC_REQUEST_VARIANT)
    asserts.assert_that(pydukpt.mac_response_key(key)).is_equal_to(pydukpt.MAC_RESPONSE_VARIANT)
    variants = [
        pydukpt.PIN_VARIANT,
        pydukpt.MAC_REQUEST_VARIANT,
        pydukpt.MAC_RESPONSE_VARIANT,
        pydukpt.DATA_REQUEST_VARIANT,
        pydukpt.DATA_RESPONSE_VARIANT,
    ]
    asserts.assert_that(len(set(variants))).is_equal_to(5)
    for v in variants:
        asserts.assert_that(v[:8]).is_equal_to(v[8:])
        asserts.assert_that(len([b for b in v.elems() if b != 0])).is_equal_to(2)


def test_data_keys_are_not_bare_variants():
    key = h("042666B49184CFA368DE9628D0397BC9")
    bare = _xor(key, pydukpt.DATA_REQUEST_VARIANT)
    asserts.assert_that(pydukpt.data_request_key(key)).is_not_equal_to(bare)
    asserts.assert_that(pydukpt.data_request_key(key)).is_not_equal_to(pydukpt.data_response_key(key))


def test_accepts_bytes_like():
    # Larky has no memoryview; bytearray is the other bytes-like type.
    ksn = h("FFFF9876543210E00001")
    expected = pydukpt.derive_key(IPEK, ksn)
    asserts.assert_that(pydukpt.derive_key(bytearray(IPEK), bytearray(ksn))).is_equal_to(expected)
    asserts.assert_that(pydukpt.derive_ipek(bytearray(BDK), bytearray(ksn))).is_equal_to(IPEK)
    asserts.assert_that(type(pydukpt.derive_key(bytearray(IPEK), ksn))).is_equal_to("bytes")


_ARGUMENT_ERRORS = [
    (pydukpt.derive_ipek, (BDK[:15], KSN), "ValueError", "bdk must be 16 bytes"),
    (pydukpt.derive_ipek, (BDK + b"\0" * 8, KSN), "ValueError", "bdk must be 16 bytes"),
    (pydukpt.derive_ipek, (BDK, KSN[:8]), "ValueError", "ksn must be 10 bytes"),
    (pydukpt.derive_ipek, (BDK.hex(), KSN), "TypeError", "bdk must be bytes-like"),
    (pydukpt.derive_key, (IPEK[:8], KSN), "ValueError", "ipek must be 16 bytes"),
    (pydukpt.derive_key, (IPEK, KSN + b"\0"), "ValueError", "ksn must be 10 bytes"),
    (pydukpt.derive_key, (IPEK, None), "TypeError", "ksn must be bytes-like"),
    (pydukpt.pin_key, (IPEK[:8],), "ValueError", "key must be 16 bytes"),
    (pydukpt.data_request_key, ([0] * 16,), "TypeError", "key must be bytes-like"),
]


def test_argument_errors(fn, args, exc, match):
    asserts.assert_fails(lambda: fn(*args), ".*?%s: %s" % (exc, match))


def test_equal_halves_bdk():
    # pycryptodome's DES3 rejects such keys; the derivation is still defined
    # (and matches moov-io/dukpt, see the generated vectors).
    bdk = h("0123456789ABCDEF0123456789ABCDEF")
    asserts.assert_that(len(pydukpt.derive_ipek(bdk, KSN))).is_equal_to(16)


def test_counter_zero_and_ipek_independent_of_counter():
    # @settings(max_examples=50) @given(binary(16), binary(10))
    ex = Examples(1)
    for _ in range(50):
        bdk, ksn = ex.binary(16), ex.binary(10)
        zero = pydukpt.ksn_with_counter(ksn, 0)
        ipek = pydukpt.derive_ipek(bdk, zero)
        asserts.assert_that(pydukpt.derive_ipek(bdk, ksn)).is_equal_to(ipek)
        asserts.assert_that(pydukpt.derive_key(ipek, zero)).is_equal_to(ipek)


def test_derive_key_chain():
    # @settings(max_examples=50) @given(binary(16), integers(1, 0x1FFFFF))
    # The device-side view: the key for a counter is the one-step derivation
    # from the key of the counter with its lowest 1 bit cleared. The two halves
    # of each step are checked by recomputing that step from the definition.
    ex = Examples(2)
    for _ in range(50):
        ipek, counter = ex.binary(16), ex.integers(1, 0x1FFFFF)
        ksn = pydukpt.ksn_with_counter(KSN, counter)
        parent = pydukpt.ksn_with_counter(KSN, counter & (counter - 1))
        asserts.assert_that(pydukpt.derive_key(ipek, ksn)).is_equal_to(
            _step(pydukpt.derive_key(ipek, parent), ksn))


def _step(key, ksn):
    reg = ksn[2:]
    mask = h("C0C0C0C000000000C0C0C0C000000000")

    def half(k):
        r = _xor(k[8:], reg)
        e = DES.new(k[:8], DES.MODE_ECB).encrypt(r)
        return _xor(e, k[8:])

    return half(_xor(key, mask)) + half(key)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_variant_masks))
    _suite.addTest(unittest.FunctionTestCase(test_data_keys_are_not_bare_variants))
    _suite.addTest(unittest.FunctionTestCase(test_accepts_bytes_like))
    larky.parametrize(_suite.addTest, unittest.FunctionTestCase,
                      "fn,args,exc,match", _ARGUMENT_ERRORS)(test_argument_errors)
    _suite.addTest(unittest.FunctionTestCase(test_equal_halves_bdk))
    _suite.addTest(unittest.FunctionTestCase(test_counter_zero_and_ipek_independent_of_counter))
    _suite.addTest(unittest.FunctionTestCase(test_derive_key_chain))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
