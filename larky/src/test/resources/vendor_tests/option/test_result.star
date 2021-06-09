"""Unit tests for @vendor//result.star

Port of: https://github.com/MaT1g3R/option/blob/master/tests/test_result.py
"""
load("@stdlib//larky", "larky")
load("@stdlib//re", "re")
load("@stdlib//types", types="types")
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
    asserts.assert_fails(lambda: failed.unwrap(), ".*division by zero")
    asserts.assert_true(failed.is_err)


def _test_api():
    f = Result.Ok(1)
    asserts.assert_that(str(f)).is_equal_to("1")
    asserts.assert_that(repr(f)).is_equal_to("Ok{1}")
    asserts.assert_that(f.map(lambda x: x + x).unwrap()).is_equal_to(2)
    asserts.assert_true(f.is_ok)
    asserts.assert_false(f.is_err)

    ##

    d = Result.Error("oh no!")
    asserts.assert_that(str(d)).is_equal_to("oh no!")
    asserts.assert_that(repr(d)).is_equal_to("Error{oh no!}")
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


def test_try_statement_workaround():

    def foo_s_try():
        one_plus_one = 1+1
        two_plus_one = 2+1
        return two_plus_one / one_plus_one

    def foo_s_failz():
        return 1 / 0

    def foo_s_Exception(rval):
        # print(type(rval))
        # print(rval) here has the entire stacktrace too...wow!
        return Result.Error("FAILURE!")

    def foo_s_else(rval):
        # never got here
        # print(rval)
        return rval  # have to make sure a return get here!

    def foo_s_finally(rval):
        # print('hallo')
        return rval  # have to make sure a returns happens!

    r = Result.try_(foo_s_try)\
       .except_(foo_s_Exception)\
       .else_(foo_s_else)\
       .finally_(foo_s_finally)\
       .build()

    asserts.assert_that(r.unwrap()).is_equal_to(1.5)

    r = Result.try_(foo_s_failz)\
       .except_(foo_s_Exception)\
       .else_(foo_s_else)\
       .finally_(foo_s_finally)\
       .build()
    asserts.assert_true(Result.error_is("FAILURE", r))


def test_invalid_transitions():

    def foo_s_try():
        one_plus_one = 1+1
        two_plus_one = 2+1
        return two_plus_one / one_plus_one

    def foo_s_Exception1(rval):
        return rval

    def foo_s_else(rval):
        return rval

    def foo_s_finally(rval):
        return rval  # have to make sure a returns happens!

    def invalid_try_else_transition():
        return Result.try_(foo_s_try)\
            .else_(foo_s_else)\
            .build()

    asserts.assert_fails(
        invalid_try_else_transition,
        "Invalid state transition: TRY => ELSE. The try builder was " +
        "constructed in the wrong order. The next valid state transitions " +
        "allowed are: TRY => EXCEPT, TRY => FINALLY")

    def invalid_try_except_finally_else_transition():
        return Result.try_(foo_s_try)\
            .except_(foo_s_Exception1)\
            .finally_(foo_s_finally)\
            .else_(foo_s_else)\
            .build()

    asserts.assert_fails(
        invalid_try_except_finally_else_transition,
        "Invalid state transition: FINALLY => ELSE.*")

    def invalid_transition_after_build():
        return Result.try_(foo_s_try)\
            .except_(foo_s_Exception1)\
            .build()\
            .finally_(foo_s_finally)

    asserts.assert_fails(
        invalid_transition_after_build,
        ".*value has no field or method 'finally_'")

    def invalid_multiple_elses():
        # only thing that can be done multiple times is except_
        return Result.try_(foo_s_try)\
            .except_(foo_s_Exception1)\
            .except_(foo_s_Exception1)\
            .else_(foo_s_else)\
            .else_(foo_s_else)\
            .build()\

    asserts.assert_fails(
        invalid_multiple_elses,
        "Invalid state transition: ELSE => ELSE.*")

    def invalid_multiple_finally():
        # only thing that can be done multiple times is except_
        return Result.try_(foo_s_try)\
            .except_(foo_s_Exception1)\
            .except_(foo_s_Exception1)\
            .else_(foo_s_else)\
            .finally_(foo_s_finally)\
            .finally_(foo_s_finally)\
            .build()\

    asserts.assert_fails(
        invalid_multiple_finally,
        "Invalid state transition: FINALLY => FINALLY.*")


def test_try_transitions():
    steps = []

    def foo_s_try():
        one_plus_one = 1+1
        two_plus_one = 2+1
        return two_plus_one / one_plus_one

    def foo_s_failz():
        return 1 / 0

    def foo_s_Exception1(rval):
        steps.append('exception')
        return rval

    def foo_s_else(rval):
        steps.append('else')
        return rval

    def foo_s_finally(rval):
        steps.append('finally')
        return rval  # have to make sure a returns happens!

    def _basic_transitions_success():
        """
        Exception will not be called here b/c success will call else => finally
        """
        return Result.try_(foo_s_try)\
            .except_(foo_s_Exception1)\
            .except_(foo_s_Exception1)\
            .else_(foo_s_else)\
            .finally_(foo_s_finally)\
            .build()

    rval = _basic_transitions_success()
    asserts.assert_that(steps).is_equal_to(
        ["else", "finally"]
    )

    steps.clear()

    def _basic_transitions_failure():
        """
        Else will not be called here b/c error will call exception => finally
        """
        return Result.try_(foo_s_failz)\
         .except_(foo_s_Exception1)\
         .except_(foo_s_Exception1)\
         .else_(foo_s_else)\
         .finally_(foo_s_finally)\
         .build()

    rval = _basic_transitions_failure()
    asserts.assert_that(steps).is_equal_to(
        ["exception", "exception", "finally"]
    )


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_invalid_transitions))
    _suite.addTest(unittest.FunctionTestCase(test_try_transitions))
    _suite.addTest(unittest.FunctionTestCase(test_try_statement_workaround))
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
        [
            (Result.Ok(1), str, Result.Ok(1)),
            (Result.Error(1), str, Result.Error("1"))
        ],
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
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
