"""Equality and hashing of larky.struct, checked against CPython 3 classes.

A Python class that defines __eq__ without __hash__ is unhashable; one that
defines __hash__ is hashed by it; one that defines neither compares and
hashes by identity.
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")


def _test_plain_struct_is_identity_hashed():
    s = larky.struct(v=1)
    asserts.assert_that(s == s).is_true()
    asserts.assert_that(s == larky.struct(v=1)).is_false()
    asserts.assert_that({s: 1}[s]).is_equal_to(1)
    asserts.assert_that(len(set([s, larky.struct(v=1)]))).is_equal_to(2)


def _test_eq_without_hash_is_unhashable():
    s1 = larky.struct(v=1, __eq__=lambda o: True)
    s2 = larky.struct(v=2, __eq__=lambda o: True)
    asserts.assert_that(s1 == s2).is_true()
    asserts.assert_fails(lambda: {s1: "hit"}, "unhashable type")
    asserts.assert_fails(lambda: {"k": 1}.get(s1), "unhashable type")
    asserts.assert_fails(lambda: set([s1, s2]), "unhashable type")


def _test_eq_with_hash_uses_hash():
    s1 = larky.struct(v=1, __eq__=lambda o: True, __hash__=lambda: 7)
    s2 = larky.struct(v=2, __eq__=lambda o: True, __hash__=lambda: 7)
    asserts.assert_that({s1: "hit"}.get(s2)).is_equal_to("hit")
    asserts.assert_that(len(set([s1, s2]))).is_equal_to(1)


def _test_hash_without_eq_is_identity_equal():
    h1 = larky.struct(__hash__=lambda: 7)
    h2 = larky.struct(__hash__=lambda: 7)
    asserts.assert_that(h1 == h1).is_true()
    asserts.assert_that(h1 == h2).is_false()
    asserts.assert_that(len(set([h1, h2]))).is_equal_to(2)
    asserts.assert_that({h1: 1}[h1]).is_equal_to(1)


def _test_hash_must_return_int():
    x = larky.struct(__hash__=lambda: "x")
    asserts.assert_fails(lambda: {x: 1}, "__hash__ method should return an integer")
    b = larky.struct(__hash__=lambda: True)
    asserts.assert_that({b: 1}[b]).is_equal_to(1)


def _test_hash_error_propagates():
    def _boom():
        fail("hash boom")
    x = larky.struct(__hash__=_boom)
    asserts.assert_fails(lambda: {x: 1}, "hash boom")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_plain_struct_is_identity_hashed))
    _suite.addTest(unittest.FunctionTestCase(_test_eq_without_hash_is_unhashable))
    _suite.addTest(unittest.FunctionTestCase(_test_eq_with_hash_uses_hash))
    _suite.addTest(unittest.FunctionTestCase(_test_hash_without_eq_is_identity_equal))
    _suite.addTest(unittest.FunctionTestCase(_test_hash_must_return_int))
    _suite.addTest(unittest.FunctionTestCase(_test_hash_error_propagates))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
