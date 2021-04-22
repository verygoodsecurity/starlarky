load("@stdlib//unittest","unittest")
load("@vendor//asserts", "asserts")
load("@stdlib//larky", larky="larky")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@stdlib//jiso8583", _JISO8583="jiso8583")
load("@vendor//ISO8583/Parser/Parser", Parser="Parser")

def MyTestCase_test_default():
    print(_JCrypto.Random.shuffle)
    decode = Parser.decode(15, 22)
    expected = {"a":15,"b":22}
    asserts.assert_that(expected).is_equal_to(decode)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(MyTestCase_test_default))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
