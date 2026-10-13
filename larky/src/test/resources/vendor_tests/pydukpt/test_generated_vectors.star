# Ported from pydukpt 0.1.0 (3778da71): tests/test_generated_vectors.py. See larky/src/main/resources/VENDORED.md.
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
"""Vectors from moov-io/dukpt, cross-checked with varhenn/dukpt_des_pinblock.

See fixtures/vectors.star (VECTORS["provenance"]) and pydukpt's tools/vectors/.
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//pydukpt", pydukpt="pydukpt")
load("./conftest", VECTORS="VECTORS", h="h")
load("./helpers", retail_mac="retail_mac", tdes_cbc_encrypt="tdes_cbc_encrypt")

CASES = VECTORS["cases"]


def test_fixture_covers_full_counter_range():
    counters = {pydukpt.ksn_counter(h(c["ksn"])): None for c in CASES}
    for c in [0, 1, 0x200, 0x7FF, 0x100000, 0x1FF800, 0x1FFFFF]:
        asserts.assert_that(c in counters).is_true()
    asserts.assert_that(any([h(c["bdk"])[:8] == h(c["bdk"])[8:] for c in CASES])).is_true()


def test_case(case):
    bdk, ksn = h(case["bdk"]), h(case["ksn"])
    ipek = pydukpt.derive_ipek(bdk, ksn)
    asserts.assert_that(ipek).is_equal_to(h(case["ipek"]))
    key = pydukpt.derive_key(ipek, ksn)
    asserts.assert_that(key).is_equal_to(h(case["key"]))
    asserts.assert_that(pydukpt.encrypt_pin(key, case["pin"], case["pan"])).is_equal_to(h(case["pin_block"]))
    asserts.assert_that(pydukpt.decrypt_pin(key, h(case["pin_block"]), case["pan"])).is_equal_to(case["pin"])
    data = h(case["data"])
    asserts.assert_that(tdes_cbc_encrypt(pydukpt.data_request_key(key), data)).is_equal_to(h(case["data_request_enc"]))
    asserts.assert_that(tdes_cbc_encrypt(pydukpt.data_response_key(key), data)).is_equal_to(h(case["data_response_enc"]))
    asserts.assert_that(retail_mac(pydukpt.mac_request_key(key), data)).is_equal_to(h(case["mac_request"]))
    asserts.assert_that(retail_mac(pydukpt.mac_response_key(key), data)).is_equal_to(h(case["mac_response"]))


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_fixture_covers_full_counter_range))
    larky.parametrize(_suite.addTest, unittest.FunctionTestCase, "case", CASES)(test_case)
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
