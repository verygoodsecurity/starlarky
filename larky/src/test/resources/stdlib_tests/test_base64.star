"""Unit tests for re.star"""

load("@stdlib/asserts", "asserts")
load("@stdlib/unittest", "unittest")
load("@stdlib/base64", "base64")


def _test_encodebytes():
    asserts.assert_that(base64.encodebytes(b"www.python.org")).is_equal_to(b"d3d3LnB5dGhvbi5vcmc=\n")


def _test_failing():
    asserts.assert_that(0).is_equal_to(1)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_encodebytes))
    _suite.addTest(unittest.FunctionTestCase(_test_failing))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_suite())
