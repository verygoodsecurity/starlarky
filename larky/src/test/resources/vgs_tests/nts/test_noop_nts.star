"""Unit tests for VaultModule.java using NoopVault API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//nts", "nts")


def _test_get_network_token():
    asserts.assert_fails(
        lambda: nts.get_network_token(pan="MOCK_PAN_ALIAS", cvv="MOCK_CVV", amount="123.45", currency_code="USD"),
        "nts.get_network_token operation must be overridden")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
