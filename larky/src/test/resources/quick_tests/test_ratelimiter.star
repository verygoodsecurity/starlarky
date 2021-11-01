load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//ratelimiter", "ratelimiter")

# for local test
def Test_ratelimiter():

    # would get: 'ratelimiter response: True'
    print('ratelimiter response:', ratelimiter.is_limited("rl", {"ip":"123", "tenant": "tnt321"}, 5, "MINUTE"))


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(Test_ratelimiter))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
