load("@vendor//luhn", luhn="luhn")
load("@stdlib//unittest","unittest")
load("@vendor//asserts","asserts")

def test_valid():
    asserts.assert_that(luhn.verify('356938035643809')).is_equal_to(True)

def test_invalid():
    asserts.assert_that(luhn.verify('4222222222222222')).is_equal_to(False)

def test_generate():
    asserts.assert_that(luhn.generate('7992739871')).is_equal_to(3)

def test_append():
    asserts.assert_that(luhn.append('53461861341123')).is_equal_to('534618613411234')

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_valid))
    _suite.addTest(unittest.FunctionTestCase(test_invalid))
    _suite.addTest(unittest.FunctionTestCase(test_generate))
    _suite.addTest(unittest.FunctionTestCase(test_append))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
