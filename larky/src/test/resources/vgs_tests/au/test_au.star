load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//au", "gen_auth_token")


def _test_get_xxx():
    resp = gen_auth_token(
        client_id="AC5RA6rLA-calm-CaWsf",
        client_secret="4624b52c-8906-4890-8a35-345168d7c8d7",
    )
    print(resp)
    pass
    # asserts.assert_fails(
    #     lambda: nts.get_network_token(pan="MOCK_PAN_ALIAS", cvv="MOCK_CVV", amount="123.45", currency_code="USD"),
    #     "nts.get_network_token operation must be overridden")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_get_xxx))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
