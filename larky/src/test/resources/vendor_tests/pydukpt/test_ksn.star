# Ported from pydukpt 0.1.0 (3778da71): tests/test_ksn.py. See larky/src/main/resources/VENDORED.md.
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
load("@vendor//pydukpt", "KSNExhaustedError", "ksn_counter", "ksn_with_counter", "next_ksn", pydukpt="pydukpt")
load("./conftest", Examples="Examples", VECTORS="VECTORS", h="h")

KSN = h("FFFF9876543210E00000")
_EXHAUSTED = ".*?KSNExhaustedError: KSN counter exhausted at "


def naive_next(counter):
    for c in range(counter + 1, 0x1FFFFF + 1):
        if bin(c).count("1") <= 10:
            return c
    return None


def test_counter_and_replace():
    asserts.assert_that(ksn_counter(h("FFFF9876543210E00001"))).is_equal_to(1)
    asserts.assert_that(ksn_counter(h("FFFF9876543210FFFFFF"))).is_equal_to(0x1FFFFF)
    asserts.assert_that(ksn_counter(KSN)).is_equal_to(0)
    # Only the low 5 bits of byte 7 belong to the counter.
    asserts.assert_that(ksn_with_counter(KSN, 0x1FFFFF)).is_equal_to(h("FFFF9876543210FFFFFF"))
    asserts.assert_that(ksn_with_counter(h("FFFF9876543210FFFFFF"), 0)).is_equal_to(KSN)
    asserts.assert_that(ksn_with_counter(bytearray(KSN), 5)).is_equal_to(h("FFFF9876543210E00005"))


def test_next_ksn_matches_varhenn(case):
    if case["next"] == None:
        asserts.assert_fails(lambda: next_ksn(h(case["ksn"])), _EXHAUSTED)
    else:
        asserts.assert_that(next_ksn(h(case["ksn"]))).is_equal_to(h(case["next"]))


_NEXT_KSN_EXAMPLES = [
    # varhenn/dukpt_des_pinblock tests @74befccb
    ("ffff9876543210e00000", "ffff9876543210e00001"),
    ("ffff9876543210e3f0f0", "ffff9876543210e3f100"),
    ("ffff9876543210e0ffcf", "ffff9876543210e10000"),
    ("ffff9876543210effc00", "ffff9876543210f00000"),
    ("ffff9876543210e007fe", "ffff9876543210e00800"),  # 0x7FF has 11 one bits
    ("ffff9876543210fff7ff", "ffff9876543210fff800"),
]


def test_next_ksn_examples(ksn, expected):
    asserts.assert_that(next_ksn(h(ksn))).is_equal_to(h(expected))


def test_next_ksn_exhausted(counter):
    asserts.assert_fails(lambda: next_ksn(ksn_with_counter(KSN, counter)), _EXHAUSTED + "%#x" % counter)
    # issubclass(KSNExhaustedError, ValueError): Larky has no classes; the
    # failure names the exception, and calling it fails the same way.
    asserts.assert_fails(lambda: KSNExhaustedError("x"), ".*?KSNExhaustedError: x")


def test_max_counter_is_last_valid():
    asserts.assert_that(bin(pydukpt.MAX_COUNTER).count("1")).is_equal_to(10)
    asserts.assert_that(naive_next(pydukpt.MAX_COUNTER)).is_none()


def test_next_ksn_keeps_non_counter_bits():
    ksn = h("0123456789ABCDEFFFFF")  # counter 0x0FFFFF, top 3 bits of byte 7 = 111
    nxt = next_ksn(ksn)
    asserts.assert_that(nxt[:7]).is_equal_to(ksn[:7])
    asserts.assert_that(nxt[7] & 0xE0).is_equal_to(ksn[7] & 0xE0)


def test_device_lifetime_is_2_pow_20_minus_1_transactions():
    # @pytest.mark.slow (about 10 s here). Larky cannot catch the
    # KSNExhaustedError that ends the Python loop, so this makes exactly
    # 2**20 - 1 next_ksn calls, each of which must succeed, and then checks
    # that the next one fails.
    ksn = KSN
    for _ in range((1 << 20) - 1):
        ksn = next_ksn(ksn)
    asserts.assert_that(ksn_counter(ksn)).is_equal_to(pydukpt.MAX_COUNTER)
    asserts.assert_fails(lambda: next_ksn(ksn), _EXHAUSTED)


def test_next_ksn_property():
    # @given(integers(0, 0x1FFFFF), binary(10)); Hypothesis' default is 100
    # examples.
    ex = Examples(3)
    for _ in range(100):
        counter, base = ex.integers(0, 0x1FFFFF), ex.binary(10)
        ksn = ksn_with_counter(base, counter)
        expected = naive_next(counter)
        if expected == None:
            asserts.assert_fails(lambda: next_ksn(ksn), _EXHAUSTED)
        else:
            asserts.assert_that(next_ksn(ksn)).is_equal_to(ksn_with_counter(base, expected))


def test_ksn_length(bad):
    for fn in (ksn_counter, next_ksn):
        asserts.assert_fails(lambda: fn(bad), ".*?ValueError: ksn must be 10 bytes")


def test_ksn_type():
    asserts.assert_fails(lambda: ksn_counter("FFFF9876543210E00000"), ".*?TypeError: ")


def test_ksn_with_counter_range(counter):
    asserts.assert_fails(lambda: ksn_with_counter(KSN, counter), ".*?ValueError: counter")


def test_ksn_with_counter_type(counter):
    asserts.assert_fails(lambda: ksn_with_counter(KSN, counter), ".*?TypeError: ")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_counter_and_replace))
    larky.parametrize(_suite.addTest, unittest.FunctionTestCase,
                      "case", VECTORS["next_ksn"])(test_next_ksn_matches_varhenn)
    larky.parametrize(_suite.addTest, unittest.FunctionTestCase,
                      "ksn,expected", _NEXT_KSN_EXAMPLES)(test_next_ksn_examples)
    larky.parametrize(_suite.addTest, unittest.FunctionTestCase,
                      "counter", [pydukpt.MAX_COUNTER, 0x1FF801, 0x1FFC00, 0x1FFFFF])(test_next_ksn_exhausted)
    _suite.addTest(unittest.FunctionTestCase(test_max_counter_is_last_valid))
    _suite.addTest(unittest.FunctionTestCase(test_next_ksn_keeps_non_counter_bits))
    _suite.addTest(unittest.FunctionTestCase(test_device_lifetime_is_2_pow_20_minus_1_transactions))
    _suite.addTest(unittest.FunctionTestCase(test_next_ksn_property))
    larky.parametrize(_suite.addTest, unittest.FunctionTestCase,
                      "bad", [b"", b"\0" * 9, b"\0" * 11])(test_ksn_length)
    _suite.addTest(unittest.FunctionTestCase(test_ksn_type))
    larky.parametrize(_suite.addTest, unittest.FunctionTestCase,
                      "counter", [-1, 0x200000])(test_ksn_with_counter_range)
    larky.parametrize(_suite.addTest, unittest.FunctionTestCase,
                      "counter", [True, 1.0, "1"])(test_ksn_with_counter_type)
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
