"""Unit tests for dicts.star."""

load("@stdlib/asserts",  "asserts")
load("@stdlib/dicts", "dicts")
load("@stdlib/unittest", "unittest")


def _add_test():
    """Unit tests for dicts.add."""

    # Test zero- and one-argument behavior.
    asserts.assert_that({}).is_equal_to(dicts.add())
    asserts.assert_that({"a": 1}).is_equal_to(dicts.add({"a": 1}))
    asserts.assert_that({"a": 1}).is_equal_to(dicts.add(a = 1))
    asserts.assert_that({"a": 1, "b": 2}).is_equal_to(dicts.add({"a": 1}, b = 2))

    # Test simple two-argument behavior.
    asserts.assert_that({"a": 1, "b": 2}).is_equal_to(dicts.add({"a": 1}, {"b": 2}))
    asserts.assert_that({"a": 1, "b": 2, "c": 3}).is_equal_to(dicts.add({"a": 1}, {"b": 2}, c = 3))

    # Test simple more-than-two-argument behavior.
    asserts.assert_that(
        {"a": 1, "b": 2, "c": 3, "d": 4}
    ).is_equal_to(
        dicts.add({"a": 1}, {"b": 2}, {"c": 3}, {"d": 4}),
    )
    asserts.assert_that(
        {"a": 1, "b": 2, "c": 3, "d": 4, "e": 5}
    ).is_equal_to(
        dicts.add({"a": 1}, {"b": 2}, {"c": 3}, {"d": 4}, e = 5),
    )

    # Test same-key overriding.
    asserts.assert_that({"a": 100}).is_equal_to(dicts.add({"a": 1}, {"a": 100}))
    asserts.assert_that({"a": 100}).is_equal_to(dicts.add({"a": 1}, a = 100))
    asserts.assert_that({"a": 10}).is_equal_to(dicts.add({"a": 1}, {"a": 100}, {"a": 10}))
    asserts.assert_that({"a": 10}).is_equal_to(dicts.add({"a": 1}, {"a": 100}, a = 10))
    asserts.assert_that(
        {"a": 100, "b": 10}
    ).is_equal_to(
        dicts.add({"a": 1}, {"a": 100}, {"b": 10}),
    )
    asserts.assert_that({"a": 10}).is_equal_to(dicts.add({"a": 1}, {}, {"a": 10}))
    asserts.assert_that({"a": 10}).is_equal_to(dicts.add({"a": 1}, {}, a = 10))
    asserts.assert_that(
        {"a": 10, "b": 5}
    ).is_equal_to(
        dicts.add({"a": 1}, {"a": 10, "b": 5}),
    )
    asserts.assert_that(
        {"a": 10, "b": 5}
    ).is_equal_to(
        dicts.add({"a": 1}, a = 10, b = 5),
    )

    # Test some other boundary cases.
    asserts.assert_that({"a": 1}).is_equal_to(dicts.add({"a": 1}, {}))

    # Since dictionaries are passed around by reference, make sure that the
    # result of dicts.add is always a *copy* by modifying it afterwards and
    # ensuring that the original argument doesn't also reflect the change. We do
    # this to protect against someone who might attempt to optimize the function
    # by returning the argument itself in the one-argument case.
    original = {"a": 1}
    result = dicts.add(original)
    result["a"] = 2
    asserts.assert_that(1).is_equal_to(original["a"])


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_add_test))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())