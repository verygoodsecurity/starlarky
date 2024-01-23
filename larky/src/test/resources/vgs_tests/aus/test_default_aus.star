load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//aus", "aus")


def _test_lookup_card_with_exp_updates():
    card = aus.lookup_card(
        pan="4111111111111111",
        exp_year=25,
        exp_month=11,
        name="Jane Doe",
        client_id="AC5RA6rLA-calm-CaWsf",
        client_secret="4624b52c-8906-4890-8a35-345168d7c8d7",
    )
    asserts.assert_that(card["number"]).is_equal_to("4111111111111111")
    asserts.assert_that(card["exp_year"]).is_equal_to(27)
    asserts.assert_that(card["exp_month"]).is_equal_to(10)

def _test_lookup_card_with_number_updates():
    card = aus.lookup_card(
        pan="4242424242424242",
        exp_year=25,
        exp_month=11,
        name="Jane Doe",
        client_id="AC5RA6rLA-calm-CaWsf",
        client_secret="4624b52c-8906-4890-8a35-345168d7c8d7",
    )
    asserts.assert_that(card["number"]).is_equal_to("4242424242424243")
    asserts.assert_that(card["exp_year"]).is_equal_to(27)
    asserts.assert_that(card["exp_month"]).is_equal_to(12)


def _test_lookup_card_with_not_existing_card():
    card = aus.lookup_card(
        pan="999999999999",
        exp_year=25,
        exp_month=11,
        name="Jane Doe",
        client_id="AC5RA6rLA-calm-CaWsf",
        client_secret="4624b52c-8906-4890-8a35-345168d7c8d7",
    )
    asserts.assert_that(card).is_none()



def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_lookup_card_with_exp_updates))
    _suite.addTest(unittest.FunctionTestCase(_test_lookup_card_with_number_updates))
    _suite.addTest(unittest.FunctionTestCase(_test_lookup_card_with_not_existing_card))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
