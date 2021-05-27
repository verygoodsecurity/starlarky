load("@stdlib//larky", "larky")
load("@stdlib//builtins", "builtins")
load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")

def test_sum_len_1():
    asserts.assert_that(builtins.sum([1])).is_equal_to(1)

def test_sum_len_2():
    asserts.assert_that(builtins.sum([1,2])).is_equal_to(3)

def test_sum_len_3():
    asserts.assert_that(builtins.sum([1,2,3])).is_equal_to(6)

def test_sum_len_10():
    asserts.assert_that(builtins.sum([1,1,1,1,1,1,1,1,1,1])).is_equal_to(10)

def test_sum_kwstart():
    asserts.assert_that(builtins.sum([1,2,3,4,5],start=3)).is_equal_to(18)

def test_sum_start():
    asserts.assert_that(builtins.sum([1,2,3],4)).is_equal_to(10)

def test_too_many_args():
    asserts.assert_fails(lambda: builtins.sum([1,2,3],4,5), ".*?TypeError")

def test_too_many_args2():
    asserts.assert_fails(lambda: builtins.sum([1,2,3],4,start=5), ".*?TypeError")

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_sum_len_1))
    _suite.addTest(unittest.FunctionTestCase(test_sum_len_2))
    _suite.addTest(unittest.FunctionTestCase(test_sum_len_3))
    _suite.addTest(unittest.FunctionTestCase(test_sum_len_10))
    _suite.addTest(unittest.FunctionTestCase(test_sum_start))
    _suite.addTest(unittest.FunctionTestCase(test_sum_kwstart))
    _suite.addTest(unittest.FunctionTestCase(test_too_many_args))
    _suite.addTest(unittest.FunctionTestCase(test_too_many_args2))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
