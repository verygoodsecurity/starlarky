load("@stdlib//larky", "larky")
load("@stdlib//builtins", "builtins")
load("@stdlib//struct", "struct")
load("@stdlib//unittest", "unittest")
load("@vendor//escapes", "escapes")
load("@vendor//asserts", "asserts")


_bstr = escapes.CEscape().x("00").x("01").x("00").x("02")
_b = builtins.bytes(_bstr)


def _test_pack():
    # test string format string
    result = struct.pack(">HH", 1, 2)
    asserts.assert_that(result).is_equal_to(_b)

    # test bytes format string
    result = struct.pack(builtins.bytes(">HH"), 1, 2)
    asserts.assert_that(result).is_equal_to(_b)


def _test_unpack():
    # test bytes/string combination
    a, b = struct.unpack(builtins.bytes(">HH"), _b)
    asserts.eq(a, 1)
    asserts.eq(b, 2)

    # test string/bytes combination
    a, b = struct.unpack(">HH", _b)
    asserts.eq(a, 1)
    asserts.eq(b, 2)


_z01 = escapes.CEscape().x("00").x("01")
_bz = builtins.bytes(_z01)


def _test_unpack_from():
    # test string format string
    (a,) = struct.unpack_from('>H', _bz)
    asserts.eq(a, 1)

    # test bytes format string
    (a,) = struct.unpack_from(builtins.bytes('>H'), _bz)
    asserts.eq(a, 1)


def _test_pack_into():
    # todo(mahmoudimus) uncomment when we have implemented array module
    # test string format string
    # result = array.array('b', [0, 0])
    # struct.pack_into('>H', result, 0, 0xABCD)
    # self.assertSequenceEqual(result, array.array('b', b"\xAB\xCD"))
    #
    # # test bytes format string
    # result = array.array('b', [0, 0])
    # struct.pack_into(b'>H', result, 0, 0xABCD)
    # self.assertSequenceEqual(result, array.array('b', b"\xAB\xCD"))

    # test bytearray
    result = bytearray()
    r = struct.pack_into('>H', result, 0, 0xABCD)
    asserts.eq(r, builtins.bytes([171, 205]))
    asserts.eq(result, bytearray([171, 205]))


def _testsuite():
    _suite = unittest.TestSuite()

    _suite.addTest(unittest.FunctionTestCase(_test_pack))
    _suite.addTest(unittest.FunctionTestCase(_test_unpack))
    _suite.addTest(unittest.FunctionTestCase(_test_unpack_from))
    _suite.addTest(unittest.FunctionTestCase(_test_pack_into))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
