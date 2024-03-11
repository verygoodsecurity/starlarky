"""Unit tests for AccountUpdateModule.java using NoopVault API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//aus", "aus")


def _test_lookup_card():
    asserts.assert_fails(
        lambda: aus.lookup_card(
            pan="4242424242424242",
            exp_year=25,
            exp_month=11,
            name="Jane Doe",
            client_id="AC5RA6rLA-calm-CaWsf",
            client_secret="4624b52c-8906-4890-8a35-345168d7c8d7",
        ),
        "aus.lookup_card operation must be overridden")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_lookup_card))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
