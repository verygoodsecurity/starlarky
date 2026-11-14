"""Struct callbacks that Java reaches without a thread parameter.

repr/str inside containers, sorted(), min()/max(), bool(), __getattr__ and
larky.property reads are run by the evaluation that uses the struct, on that
evaluation's thread (CurrentEvaluation), not on a thread the struct stored.
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")


def _ordered(n):
    return larky.struct(n=n, __lt__=lambda o: n < o.n, __gt__=lambda o: n > o.n)


def _test_sorted_uses_dunder_lt_and_gt():
    s = sorted([_ordered(3), _ordered(1), _ordered(2)])
    asserts.assert_that([x.n for x in s]).is_equal_to([1, 2, 3])
    asserts.assert_that(min([_ordered(3), _ordered(1)]).n).is_equal_to(1)
    asserts.assert_that(max([_ordered(3), _ordered(1)]).n).is_equal_to(3)


def _test_repr_and_str_inside_containers():
    s = larky.struct(__repr__=lambda: "REPR", __str__=lambda: "STR")
    asserts.assert_that(repr([s])).is_equal_to("[REPR]")
    asserts.assert_that(repr({"k": s})).is_equal_to('{"k": REPR}')
    asserts.assert_that(str(s)).is_equal_to("STR")


def _test_truth_uses_bool_and_len():
    asserts.assert_that(bool(larky.struct(__bool__=lambda: False))).is_false()
    asserts.assert_that(bool(larky.struct(__len__=lambda: 0))).is_false()
    asserts.assert_that(bool(larky.struct(__len__=lambda: 2))).is_true()
    asserts.assert_that([x for x in [larky.struct(__bool__=lambda: False)] if x]).is_equal_to([])


def _test_getattr_and_property_reads():
    s = larky.mutablestruct(
        computed=larky.property(lambda: [1, 2]),
        __getattr__=lambda name: "attr:" + name,
    )
    asserts.assert_that(s.computed).is_equal_to([1, 2])
    asserts.assert_that(s.other).is_equal_to("attr:other")
    asserts.assert_that(getattr(s, "x")).is_equal_to("attr:x")


def _test_property_setter_runs_on_the_current_thread():
    seen = []
    s = larky.mutablestruct(
        v=larky.property(lambda: len(seen), lambda x: seen.append(x)),
    )
    s.v = 5
    asserts.assert_that(seen).is_equal_to([5])
    asserts.assert_that(s.v).is_equal_to(1)


def _test_struct_equality_and_hash_in_dict_and_set():
    a = larky.struct(__eq__=lambda o: True, __hash__=lambda: 1)
    b = larky.struct(__eq__=lambda o: True, __hash__=lambda: 1)
    asserts.assert_that({a: "x"}[b]).is_equal_to("x")
    asserts.assert_that(len(set([a, b]))).is_equal_to(1)
    asserts.assert_that(a in [b]).is_true()


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_sorted_uses_dunder_lt_and_gt))
    _suite.addTest(unittest.FunctionTestCase(_test_repr_and_str_inside_containers))
    _suite.addTest(unittest.FunctionTestCase(_test_truth_uses_bool_and_len))
    _suite.addTest(unittest.FunctionTestCase(_test_getattr_and_property_reads))
    _suite.addTest(unittest.FunctionTestCase(_test_property_setter_runs_on_the_current_thread))
    _suite.addTest(unittest.FunctionTestCase(_test_struct_equality_and_hash_in_dict_and_set))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
