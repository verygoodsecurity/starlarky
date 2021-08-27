load("@stdlib//csv", csv="csv")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//io/StringIO", StringIO="StringIO")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//option/result", Result="Result", Error="Error")


def _test_simple_testcase():
    asserts.assert_that(True).is_equal_to(True)
    repr(csv.QuoteMinimalStrategy(csv.excel()))
    asserts.assert_that(csv.list_dialects()).is_equal_to(["excel"])


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_simple_testcase))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
