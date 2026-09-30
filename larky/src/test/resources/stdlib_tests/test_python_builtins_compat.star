"""Python 3 compatibility of numeric and bytes builtins.

Every expected value below was computed with CPython 3.10. Error cases use
anchored regexes so that a leaked Java exception (whose message would be
"...Exception thrown during Starlark evaluation") cannot match.
"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")


_BIG = int("100000000000000000000")  # 10**20
_HUGE = int("1" + "0" * 400)  # 10**400, too large for a float
_INF = float("inf")
_NAN = float("nan")


def _test_divmod_int_floors():
    asserts.assert_that(divmod(7, 2)).is_equal_to((3, 1))
    asserts.assert_that(divmod(-7, 2)).is_equal_to((-4, 1))
    asserts.assert_that(divmod(7, -2)).is_equal_to((-4, -1))
    asserts.assert_that(divmod(-7, -2)).is_equal_to((3, -1))
    asserts.assert_that(divmod(-1, _BIG)).is_equal_to((-1, int("99999999999999999999")))
    asserts.assert_that(divmod(_BIG, -7)).is_equal_to((int("-14285714285714285715"), -5))


def _test_divmod_float():
    asserts.assert_that(divmod(-7.5, 2)).is_equal_to((-4.0, 0.5))
    asserts.assert_that(divmod(7.5, -2)).is_equal_to((-4.0, -0.5))
    asserts.assert_that(divmod(-7, 2.0)).is_equal_to((-4.0, 1.0))
    asserts.assert_that(divmod(7, -2.5)).is_equal_to((-3.0, -0.5))
    asserts.assert_that(divmod(5, 0.5)).is_equal_to((10.0, 0.0))
    asserts.assert_that(type(divmod(5, 0.5)[0])).is_equal_to("float")
    # Python's divmod is not floor(a / b): 1 / 0.1 rounds up to 10.0.
    asserts.assert_that(divmod(1, 0.1)).is_equal_to((9.0, 0.09999999999999995))
    asserts.assert_that(divmod(-1, 0.1)).is_equal_to((-10.0, 5.551115123125783e-17))
    asserts.assert_that(str(divmod(0.0, -3))).is_equal_to("(-0.0, -0.0)")
    asserts.assert_that(str(divmod(-0.0, 3))).is_equal_to("(-0.0, 0.0)")
    asserts.assert_that(divmod(3, _INF)).is_equal_to((0.0, 3.0))
    asserts.assert_that(divmod(-3, _INF)).is_equal_to((-1.0, _INF))
    asserts.assert_that(str(divmod(_INF, 3))).is_equal_to("(nan, nan)")


def _test_divmod_errors():
    asserts.assert_fails(lambda: divmod(1, 0), "^integer division or modulo by zero$")
    asserts.assert_fails(lambda: divmod(1, 0.0), "^floating-point division or modulo by zero$")
    asserts.assert_fails(lambda: divmod(1.0, 0), "^floating-point division or modulo by zero$")
    asserts.assert_fails(lambda: divmod(_HUGE, 2.0), "^int too large to convert to float$")
    asserts.assert_fails(lambda: divmod("a", 2), "want 'int or float'")


def _test_hex_bin_sign():
    asserts.assert_that(hex(-255)).is_equal_to("-0xff")
    asserts.assert_that(hex(-1)).is_equal_to("-0x1")
    asserts.assert_that(hex(0)).is_equal_to("0x0")
    asserts.assert_that(hex(255)).is_equal_to("0xff")
    asserts.assert_that(hex(-(1 << 70))).is_equal_to("-0x400000000000000000")
    asserts.assert_that(bin(-255)).is_equal_to("-0b11111111")
    asserts.assert_that(bin(0)).is_equal_to("0b0")


def _test_pow_int():
    asserts.assert_that(pow(0, 0)).is_equal_to(1)
    asserts.assert_that(type(pow(3, 2))).is_equal_to("int")
    asserts.assert_that(pow(2, 1024)).is_equal_to(pow(2, 512) * pow(2, 512))
    asserts.assert_that(type(pow(2, 1024))).is_equal_to("int")
    # A negative exponent gives a float.
    asserts.assert_that(pow(2, -1)).is_equal_to(0.5)
    asserts.assert_that(pow(2, -2)).is_equal_to(0.25)
    asserts.assert_that(pow(-8, -1)).is_equal_to(-0.125)
    asserts.assert_that(pow(10, -400)).is_equal_to(0.0)
    asserts.assert_that(pow(7, -10000000000)).is_equal_to(0.0)
    asserts.assert_fails(lambda: pow(0, -1), "^ZeroDivisionError: 0.0 cannot be raised to a negative power$")
    asserts.assert_fails(lambda: pow(_HUGE, -1), "^int too large to convert to float$")
    # Python would run out of memory; Larky refuses up front.
    asserts.assert_fails(lambda: pow(2, 10000000000), "^OverflowError: pow\\(\\) exponent too large: 10000000000$")


def _test_pow_float():
    asserts.assert_that(pow(2.0, 3)).is_equal_to(8.0)
    asserts.assert_that(type(pow(2.0, 3))).is_equal_to("float")
    asserts.assert_that(type(pow(-2, 3.0))).is_equal_to("float")
    asserts.assert_that(pow(-2, 3.0)).is_equal_to(-8.0)
    asserts.assert_that(pow(2.5, 2)).is_equal_to(6.25)
    asserts.assert_that(pow(2, 0.5)).is_equal_to(1.4142135623730951)
    asserts.assert_that(pow(1.5, -2)).is_equal_to(0.4444444444444444)
    asserts.assert_that(pow(0.0, 0)).is_equal_to(1.0)
    asserts.assert_that(pow(2, -1074)).is_equal_to(5e-324)
    asserts.assert_that(pow(2, -1075)).is_equal_to(0.0)
    asserts.assert_that(str(pow(-0.0, 3))).is_equal_to("-0.0")
    asserts.assert_that(pow(_INF, -1)).is_equal_to(0.0)
    asserts.assert_that(pow(-_INF, 3)).is_equal_to(-_INF)
    asserts.assert_that(str(pow(-_INF, -3))).is_equal_to("-0.0")
    asserts.assert_that(pow(_NAN, 0)).is_equal_to(1.0)
    asserts.assert_that(pow(1, _NAN)).is_equal_to(1.0)
    asserts.assert_that(pow(-1, _INF)).is_equal_to(1.0)
    asserts.assert_fails(lambda: pow(1e300, 2), "^OverflowError: pow\\(\\) result too large$")
    asserts.assert_fails(lambda: pow(2.0, 1024), "^OverflowError: pow\\(\\) result too large$")
    asserts.assert_fails(lambda: pow(10, 309.0), "^OverflowError: pow\\(\\) result too large$")
    asserts.assert_fails(lambda: pow(0.0, -1), "^ZeroDivisionError: 0.0 cannot be raised to a negative power$")
    asserts.assert_fails(lambda: pow(0, -1.0), "^ZeroDivisionError: 0.0 cannot be raised to a negative power$")
    # Python returns a complex number; Starlark has none.
    asserts.assert_fails(lambda: pow(-2, 0.5), "^ValueError: negative number cannot be raised to a fractional power$")
    asserts.assert_fails(lambda: pow(_HUGE, 0.5), "^int too large to convert to float$")


def _test_pow_mod():
    asserts.assert_that(pow(3, -1, 7)).is_equal_to(5)
    asserts.assert_that(pow(3, 2, -7)).is_equal_to(-5)
    asserts.assert_that(pow(3, -1, -7)).is_equal_to(-2)
    asserts.assert_that(pow(-3, 3, 7)).is_equal_to(1)
    asserts.assert_that(pow(-2, 3, -5)).is_equal_to(-3)
    asserts.assert_that(pow(2, 3, 1)).is_equal_to(0)
    asserts.assert_that(pow(2, 0, 1)).is_equal_to(0)
    asserts.assert_that(pow(2, int("1" + "0" * 100), 3)).is_equal_to(1)
    asserts.assert_that(pow(2, -int("1" + "0" * 100), 5)).is_equal_to(1)
    asserts.assert_fails(lambda: pow(2, -1, 4), "^base is not invertible for the given modulus$")
    asserts.assert_fails(lambda: pow(3, 2, 0), "^pow\\(\\) 3rd argument cannot be 0$")
    msg = "^TypeError: pow\\(\\) 3rd argument not allowed unless all arguments are integers$"
    asserts.assert_fails(lambda: pow(2.0, 2, 3), msg)
    asserts.assert_fails(lambda: pow(2, 2.0, 3), msg)
    asserts.assert_fails(lambda: pow(2, 3, 5.0), "want 'int or NoneType'")


def _test_chr_range():
    asserts.assert_that(chr(97)).is_equal_to("a")
    asserts.assert_that(ord(chr(0x10FFFF))).is_equal_to(0x10FFFF)
    # A lone surrogate is a one-element string, as in Python.
    asserts.assert_that(len(chr(0xD800))).is_equal_to(1)
    asserts.assert_that(ord(chr(0xD800))).is_equal_to(0xD800)
    msg = "^ValueError: chr\\(\\) arg not in range\\(0x110000\\)$"
    asserts.assert_fails(lambda: chr(-1), msg)
    asserts.assert_fails(lambda: chr(0x110000), msg)
    asserts.assert_fails(lambda: chr(1 << 40), msg)


def _test_bytes_join_rejects_non_bytes():
    asserts.assert_fails(
        lambda: b",".join([b"a", "b"]),
        "^sequence item 1: expected a bytes-like object, string found$")
    asserts.assert_fails(
        lambda: b",".join([1]),
        "^sequence item 0: expected a bytes-like object, int found$")
    asserts.assert_fails(
        lambda: bytearray(b",").join([b"a", "b"]),
        "^sequence item 1: expected a bytes-like object, string found$")
    asserts.assert_that(b",".join([b"a", bytearray(b"b")])).is_equal_to(b"a,b")
    asserts.assert_that(b",".join((b"a", b"b"))).is_equal_to(b"a,b")
    joined = bytearray(b",").join([b"a", bytearray(b"b")])
    asserts.assert_that(type(joined)).is_equal_to("bytearray")
    asserts.assert_that(joined).is_equal_to(bytearray(b"a,b"))


def _test_hash_bytearray_unhashable():
    asserts.assert_fails(lambda: hash(bytearray(b"a")), "^unhashable type: 'bytearray'$")
    asserts.assert_that(hash(b"a")).is_equal_to(hash(b"a"))
    asserts.assert_that(hash("a")).is_equal_to(97)


def _power_of_two(bits):
    # 1 << bits, built with shifts under Starlark's 512 limit.
    x = 1
    for _ in range(bits // 500):
        x = x << 500
    return x << (bits % 500)


def _test_pow_size_limits():
    # Ints are limited to 65,536 bits and a modular power's modulus and exponent to 16,384 bits,
    # checked before computing (IntLimits). Values from CPython.
    asserts.assert_that(pow(2, 65535) % 1000003).is_equal_to(762552)  # 65,536 bits: fits
    asserts.assert_that(pow(-2, 65535) < 0).is_true()
    asserts.assert_fails(lambda: pow(2, 65536), "int too large: pow would make an int")
    asserts.assert_fails(lambda: pow(3, 10000000), "int too large: pow would make an int")
    asserts.assert_that(pow(1, 10000000)).is_equal_to(1)
    asserts.assert_that(pow(-1, 10000001)).is_equal_to(-1)
    m = _power_of_two(16383) + 1  # 16,384 bits
    asserts.assert_that(pow(3, 65537, m) % 1000003).is_equal_to(342305)
    asserts.assert_fails(lambda: pow(3, 65537, _power_of_two(16384) + 1),
                         "int too large: pow\\(\\) with a 17-bit exponent and a 16385-bit modulus")
    asserts.assert_fails(lambda: pow(3, _power_of_two(16384), 1000003),
                         "int too large: pow\\(\\) with a 16385-bit exponent")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_divmod_int_floors))
    _suite.addTest(unittest.FunctionTestCase(_test_divmod_float))
    _suite.addTest(unittest.FunctionTestCase(_test_divmod_errors))
    _suite.addTest(unittest.FunctionTestCase(_test_hex_bin_sign))
    _suite.addTest(unittest.FunctionTestCase(_test_pow_int))
    _suite.addTest(unittest.FunctionTestCase(_test_pow_float))
    _suite.addTest(unittest.FunctionTestCase(_test_pow_mod))
    _suite.addTest(unittest.FunctionTestCase(_test_chr_range))
    _suite.addTest(unittest.FunctionTestCase(_test_bytes_join_rejects_non_bytes))
    _suite.addTest(unittest.FunctionTestCase(_test_hash_bytearray_unhashable))
    _suite.addTest(unittest.FunctionTestCase(_test_pow_size_limits))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
