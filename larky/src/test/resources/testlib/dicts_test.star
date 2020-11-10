"""Unit tests for dicts.star."""

load("dicts", "dicts")
load("unittest", "asserts", "unittest")


def _add_test(ctx):
    """Unit tests for dicts.add."""
    env = unittest.begin(ctx)

    # Test zero- and one-argument behavior.
    asserts.equals(env, {}, dicts.add())
    asserts.equals(env, {"a": 1}, dicts.add({"a": 1}))
    asserts.equals(env, {"a": 1}, dicts.add(a = 1))
    asserts.equals(env, {"a": 1, "b": 2}, dicts.add({"a": 1}, b = 2))

    # Test simple two-argument behavior.
    asserts.equals(env, {"a": 1, "b": 2}, dicts.add({"a": 1}, {"b": 2}))
    asserts.equals(env, {"a": 1, "b": 2, "c": 3}, dicts.add({"a": 1}, {"b": 2}, c = 3))

    # Test simple more-than-two-argument behavior.
    asserts.equals(
        env,
        {"a": 1, "b": 2, "c": 3, "d": 4},
        dicts.add({"a": 1}, {"b": 2}, {"c": 3}, {"d": 4}),
    )
    asserts.equals(
        env,
        {"a": 1, "b": 2, "c": 3, "d": 4, "e": 5},
        dicts.add({"a": 1}, {"b": 2}, {"c": 3}, {"d": 4}, e = 5),
    )

    # Test same-key overriding.
    asserts.equals(env, {"a": 100}, dicts.add({"a": 1}, {"a": 100}))
    asserts.equals(env, {"a": 100}, dicts.add({"a": 1}, a = 100))
    asserts.equals(env, {"a": 10}, dicts.add({"a": 1}, {"a": 100}, {"a": 10}))
    asserts.equals(env, {"a": 10}, dicts.add({"a": 1}, {"a": 100}, a = 10))
    asserts.equals(
        env,
        {"a": 100, "b": 10},
        dicts.add({"a": 1}, {"a": 100}, {"b": 10}),
    )
    asserts.equals(env, {"a": 10}, dicts.add({"a": 1}, {}, {"a": 10}))
    asserts.equals(env, {"a": 10}, dicts.add({"a": 1}, {}, a = 10))
    asserts.equals(
        env,
        {"a": 10, "b": 5},
        dicts.add({"a": 1}, {"a": 10, "b": 5}),
    )
    asserts.equals(
        env,
        {"a": 10, "b": 5},
        dicts.add({"a": 1}, a = 10, b = 5),
    )

    # Test some other boundary cases.
    asserts.equals(env, {"a": 1}, dicts.add({"a": 1}, {}))

    # Since dictionaries are passed around by reference, make sure that the
    # result of dicts.add is always a *copy* by modifying it afterwards and
    # ensuring that the original argument doesn't also reflect the change. We do
    # this to protect against someone who might attempt to optimize the function
    # by returning the argument itself in the one-argument case.
    original = {"a": 1}
    result = dicts.add(original)
    result["a"] = 2
    asserts.equals(env, 1, original["a"])

    return unittest.end(env)

add_test = unittest.make(_add_test)

def dicts_test_suite():
    """Creates the test targets and test suite for dicts.bzl tests."""
    unittest.suite(
        "dicts_tests",
        add_test,
    )

def testSomething():
    something = makeSomething()
    assert something.name is not None
    # ...


testcase = unittest.make(testSomething,
                         setUp=makeSomethingDB,
                         tearDown=deleteSomethingDB)