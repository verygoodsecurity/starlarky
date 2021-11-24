load("@stdlib//builtins", "builtins")
load("@stdlib//larky", "larky")
load("@stdlib//types", "types")
load("@stdlib//operator", operator="operator")
load("@stdlib//unittest", "unittest")
load("@stdlib//xml/dom", dom="dom")
load("@vendor//asserts", "asserts")

# test imports..
load("@stdlib//xml/dom/domreg", getDOMImplementation="getDOMImplementation")
load("@stdlib//xml/dom/minicompat", NodeList="NodeList")
load("@stdlib//xml/dom/NodeFilter", NodeFilter="NodeFilter")
load("@stdlib//xml/dom/xmlbuilder",
     DOMImplementationLS="DOMImplementationLS",
     DocumentLS="DocumentLS")
# load("@stdlib//xml/dom/minidom", Node="Node")


def _test_dom():
    pass


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_dom))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
