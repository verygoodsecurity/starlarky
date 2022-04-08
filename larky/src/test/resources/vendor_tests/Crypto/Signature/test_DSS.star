load("@vendor//asserts", asserts="asserts")
load("@stdlib//unittest", unittest="unittest")

# load("@vendor//Crypto/Signature/DSS", DSS="DSS")
load("@vendor//Crypto/PublicKey/DSA", DSA="DSA")


def test_DSS():
    key = DSA.generate(2048)
    print(key)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_DSS))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
