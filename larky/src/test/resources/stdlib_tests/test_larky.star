"""Unit tests for larky.star."""
load("@stdlib/larky", "larky")

load("@stdlib/asserts",  "asserts")
load("@stdlib/unittest", "unittest")


def _test_namespace_exposes_larky_builtins():
    """
    This unit test really is testing to make sure that we are tracking
    that everything we need exported from `@stdlib/larky` to ensure that
    we do not break dependencies.

    :return: None
    """
    items = sorted(dir(larky))
    asserts.assert_that(items).is_length(5)
    asserts.assert_that(items).is_equal_to(sorted([
        "bytearray",
        "struct",
        "mutablestruct",
        "partial",
        "property"
    ]))


def _suite():
    _suite = unittest.TestSuite()
    for t in [
        _test_namespace_exposes_larky_builtins,
    ]:
        _suite.addTest(unittest.FunctionTestCase(t))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())