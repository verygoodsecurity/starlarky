"""Unit tests for @vendor//result.star

Port of: https://github.com/MaT1g3R/option/blob/master/tests/test_result.py
"""
load("@stdlib//larky", "larky")
load("@stdlib//re", "re")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//option/result",
     Result="Result",
     Ok="Ok",
     Error="Error",
     safe="safe")


def _function(number):
    return number / number


def _test_safe_success():
    """Ensures that safe decorator works correctly for Success case."""
    asserts.assert_that(safe(_function)(1)).is_equal_to(Ok(1.0))
    asserts.assert_that(safe(_function)(1)).is_not_equal_to(Error(1.0))


def _test_safe_failure():
    """Ensures that safe decorator works correctly for Failure case."""
    failed = safe(_function)(0)
    asserts.assert_fails(lambda: failed.unwrap(), ".*ZeroDivisionError")

    # from returns.result import Success, safe
    #
    #
    # @safe
    # def _function(number: int) -> float:
    #     return number / number
    #
    #
    # def test_safe_success():
    #     """Ensures that safe decorator works correctly for Success case."""
    #     assert _function(1) == Success(1.0)
    #
    #
    # def test_safe_failure():
    #     """Ensures that safe decorator works correctly for Failure case."""
    #     failed = _function(0)
    #     assert isinstance(failed.failure(), ZeroDivisionError)
    pass


def _test_api():
    f = Result.Ok(1)
    asserts.assert_that(str(f)).is_equal_to("Ok{1}")
    asserts.assert_that(f.map(lambda x: x + x).unwrap()).is_equal_to(2)
    asserts.assert_true(f.is_ok)
    asserts.assert_false(f.is_err)

    ##

    d = Result.Error("oh no!")
    asserts.assert_that(str(d)).is_equal_to("Error{oh no!}")
    asserts.assert_false(d.is_ok)
    asserts.assert_true(d.is_err)


def _test_factory_ok(val):
    res = Result.Ok(val)
    asserts.assert_true(res.is_ok)
    asserts.assert_that(res._val).is_equal_to(val)


def _test_factory_err(err):
    res = Result.Error(err)
    asserts.assert_false(res.is_ok)
    asserts.assert_true(res.is_err)
    asserts.assert_fails(lambda: res.unwrap(), re.escape("%s" % err))

def _test_map(obj, call, exp):
    asserts.assert_that(obj.map(call)).is_equal_to(exp)


def _test_map_err(obj, call, exp):
    asserts.assert_that(obj.map_err(call)).is_equal_to(exp)


def _test_unwrap(obj, exp, ok):
    if ok:
        asserts.assert_that(obj.unwrap()).is_equal_to(exp)
    else:
        asserts.assert_fails(obj.unwrap, ".*%s" % exp)  # assert fails with msg


def _test_unwrap_or(obj, optb, exp):
    asserts.assert_that(obj.unwrap_or(optb)).is_equal_to(exp)


def test_unwrap_or_else(obj, op, exp):
    asserts.assert_that(obj.unwrap_or_else(op)).is_equal_to(exp)


def test_except(obj, ok, exp):
    if ok:
        asserts.assert_that(obj.expect('')).is_equal_to(exp)
    else:
        asserts.assert_fails(lambda:  obj.expect('ValueError'), ".*ValueError")


def test_unwrap_expect_err(obj, err, exp):
    if err:
        asserts.assert_that(obj.unwrap_err()).is_equal_to(exp)
        asserts.assert_that(obj.expect_err('')).is_equal_to(exp)
    else:
        asserts.assert_fails(lambda:  obj.unwrap_err(), ".*")
        asserts.assert_fails(lambda:  obj.expect_err(''), ".*")


def test_eq(o1, o2):

    asserts.assert_that(o1).is_equal_to(o2)
    asserts.assert_that(o1).is_not_equal_to(o1._val)
    asserts.assert_that(o1._val).is_not_equal_to(o1)


def test_neq(o1, o2):
    asserts.assert_that(o1).is_not_equal_to(o2)
    asserts.assert_that(o1).is_not_equal_to(o1._val)
    asserts.assert_that(o1._val).is_not_equal_to(o1)


def test_lt_gt(o1, o2):
    asserts.assert_true(o1 < o2)
    asserts.assert_true(o1 <= o2)
    asserts.assert_true(o2 > o1)
    asserts.assert_true(o2 >= o1)


def test_le_ge(o1, o2):
    asserts.assert_true(o1 <= o2)
    asserts.assert_true(o1 >= o2)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_api))

    _suite.addTest(unittest.FunctionTestCase(_test_safe_success))
    _suite.addTest(unittest.FunctionTestCase(_test_safe_failure))

    # very verbose way of writing:
    # @parametrize('val', [0, None, '', [], False])
    # def _test_factory_err(val):
    larky.parametrize(
        _suite.addTest, unittest.FunctionTestCase, "val", [0, None, {}, [], False]
    )(_test_factory_ok)

    larky.parametrize(
        _suite.addTest, unittest.FunctionTestCase, "err", [0, None, "", [], False]
    )(_test_factory_err)

    larky.parametrize(
        _suite.addTest,
        unittest.FunctionTestCase,
        "obj,call,exp",
        [(Result.Ok(1), str, Result.Ok("1")), (Result.Error(1), str, Result.Error(1))],
    )(_test_map)

    larky.parametrize(
        _suite.addTest,
        unittest.FunctionTestCase,
        "obj,call,exp",
        [(Result.Ok(1), str, Result.Ok(1)), (Result.Error(1), str, Result.Error("1"))],
    )(_test_map_err)

    larky.parametrize(
        _suite.addTest,
        unittest.FunctionTestCase,
        "obj,exp,ok",
        [
            (Result.Ok(1), 1, True),
            (Result.Ok(None), None, True),
            (Result.Error(1), 1, False),
        ],
    )(_test_unwrap)

    larky.parametrize(
        _suite.addTest,
        unittest.FunctionTestCase,
        "obj,optb,exp",
        [
            (Result.Ok(0), 11, 0),
            (Result.Error(11), 0, 0),
        ],
    )(_test_unwrap_or)

    larky.parametrize(
        _suite.addTest,
        unittest.FunctionTestCase,
        'obj,op,exp',
        [
            (Ok('asd'), len, 'asd'),
            (Result.Error('asd'), len, 3),
        ],
    )(test_unwrap_or_else)

    larky.parametrize(
        _suite.addTest,
        unittest.FunctionTestCase,
        'obj,ok,exp',
        [
            (Ok(1), True, 1),
            (Result.Error(1), False, ''),
            (Ok(None), True, None)
        ],
    )(test_except)

    larky.parametrize(
        _suite.addTest,
        unittest.FunctionTestCase,
        'obj,err,exp',
        [
            (Ok(1), False, ''),
            (Result.Error(None), True, None),
        ],
    )(test_unwrap_expect_err)

    # Starlark does not support hashes except for string or byte values
    # larky.parametrize(
    #     _suite.addTest,
    #     unittest.FunctionTestCase,
    #     'obj1,obj2,eq',
    #     [
    #         (Ok(1), Ok(1), True),
    #         (Result.Error(1), Result.Error(1), True),
    #         (Ok(1), Result.Error(1), False)
    #     ],
    # )(test_hash)

    larky.parametrize(
        _suite.addTest,
        unittest.FunctionTestCase,
        'o1,o2',
        [
            (Ok(''), Ok('')),
            (Ok([]), Ok([])),
            (Result.Error('aa'), Result.Error('aa')),
            (Result.Error({}), Result.Error({}))
        ],
    )(test_eq)

    larky.parametrize(
        _suite.addTest,
        unittest.FunctionTestCase,
        'o1,o2',
        [
            (Ok(''), Result.Error('')),
            (Result.Error([]), Ok([])),
            (Ok({}), Result.Error({})),
            (Ok(1), Ok(2)),
            (Result.Error(2), Result.Error(3))
        ],
    )(test_neq)

    larky.parametrize(
        _suite.addTest,
        unittest.FunctionTestCase,
        'o1,o2',
        [
            (Ok(2), Result.Error(1)),
            (Ok(1), Ok(2)),
            (Error(1), Result.Error(2))
        ],
    )(test_lt_gt)

    larky.parametrize(
        _suite.addTest,
        unittest.FunctionTestCase,
        'o1,o2',
        [
            (Ok(1), Ok(1)),
            (Error(1), Error(1))
        ],
    )(test_le_ge)

    # _suite.addTest(unittest.FunctionTestCase(_test_factory_err))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
