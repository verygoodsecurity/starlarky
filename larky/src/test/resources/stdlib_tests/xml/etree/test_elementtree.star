load("@stdlib//xml/etree/ElementTree", Element="Element", iselement="iselement")

load("@stdlib//builtins", "builtins")
load("@stdlib//codecs", "codecs")
load("@stdlib//larky", "larky")
load("@stdlib//types", "types")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")


def _suite():
    _suite = unittest.TestSuite()
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
