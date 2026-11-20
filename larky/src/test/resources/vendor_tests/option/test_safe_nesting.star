"""safe() wrappers can be nested; real recursion is still refused."""
load("@stdlib//csv", "csv")
load("@stdlib//io", "io")
load("@stdlib//types", "types")
load("@stdlib//unittest", "unittest")
load("@stdlib//urllib/parse", "parse")
load("@vendor//asserts", "asserts")
load("@vendor//option/result", Error="Error", Ok="Ok", safe="safe")


def _half(n):
    return n // 2


def _fails(n):
    return n // 0


def _calls_safe_ok(n):
    return safe(_half)(n).unwrap() + 1


def _calls_safe_failing(n):
    return safe(_fails)(n).is_err


def _test_nested_safe_returns_ok():
    asserts.assert_that(safe(_calls_safe_ok)(4)).is_equal_to(Ok(3))


def _test_inner_failure_is_the_inner_error():
    asserts.assert_that(safe(_calls_safe_failing)(4)).is_equal_to(Ok(True))


def _test_same_wrapper_twice():
    w = safe(_half)
    asserts.assert_that(w(4)).is_equal_to(Ok(2))
    asserts.assert_that(w(6)).is_equal_to(Ok(3))


def _reenters():
    return safe(_reenters)().unwrap()


def _test_recursion_is_still_refused():
    r = safe(_reenters)()
    asserts.assert_true(r.is_err)
    asserts.assert_that(str(r)).contains("called recursively")


def _test_library_calls_that_use_safe():
    q = safe(parse.urlencode)({"a": ["1", "2"], "b": "3"}, doseq=True)
    asserts.assert_that(q).is_equal_to(Ok("a=1&a=2&b=3"))
    rows = safe(lambda: list(csv.reader(io.StringIO("a,b\nc,d\n"))))()
    asserts.assert_that(rows).is_equal_to(Ok([["a", "b"], ["c", "d"]]))


def _test_wrapper_is_callable():
    w = safe(_half)
    asserts.assert_true(types.is_callable(w))
    asserts.assert_that(type(w)).is_equal_to("function")
    asserts.assert_that(repr(w)).is_equal_to("<function safe(_half)>")
    asserts.assert_that(str(w)).is_equal_to("<function safe(_half)>")
    asserts.assert_that(repr(safe(lambda: 1))).is_equal_to("<function safe(lambda)>")


def _testsuite():
    _suite = unittest.TestSuite()
    for t in [
        _test_nested_safe_returns_ok,
        _test_inner_failure_is_the_inner_error,
        _test_same_wrapper_twice,
        _test_recursion_is_still_refused,
        _test_library_calls_that_use_safe,
        _test_wrapper_is_callable,
    ]:
        _suite.addTest(unittest.FunctionTestCase(t))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
