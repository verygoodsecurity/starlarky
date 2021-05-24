"""Unit tests for @vendor//result.star"""
load("@stdlib//larky", "larky")
load("@stdlib//re", "re")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//option/result", Result="Result")


def _test_api():
    f = Result.Ok(1)
    asserts.assert_that(str(f)).is_equal_to("Ok{1}")
    asserts.assert_that(f.map(lambda x: x + x).value()).is_equal_to(2)
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
    asserts.assert_that(res.value()).is_equal_to(val)


def _test_factory_err(err):
    res = Result.Error(err)
    asserts.assert_false(res.is_ok)
    asserts.assert_true(res.is_err)
    asserts.assert_fails(lambda: res.unwrap(), re.escape("%s" % err))
    # asserts.assert_that(res.error()).is_not_none()
    # asserts.assert_that(res.error()).is_equal_to(err)


def _test_map(obj, call, exp):
    asserts.assert_that(obj.map(call)).is_equal_to(exp)


def _test_map_err(obj, call, exp):
    asserts.assert_that(obj.map_err(call)).is_equal_to(exp)


def _test_unwrap(obj, exp, ok):
    if ok:
        asserts.assert_that(obj.unwrap()).is_equal_to(exp)
    else:
        asserts.assert_fails(obj.unwrap, ".*%s" % exp)  # assert fails with msg


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_api))
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

    # _suite.addTest(unittest.FunctionTestCase(_test_factory_err))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
