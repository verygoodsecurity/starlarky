"""Unit tests for stripe.star"""
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vgs//http/request", VGSHttpRequest="VGSHttpRequest")
load("@vgs//stripe", stripe="stripe")

ALLOWED_KEYS = ["pk_test_allowed"]
ALLOWED = "pk_test_allowed"
DISALLOWED = "pk_test_disallowed"
FORBIDDEN_BODY = b'{"error":"psp_key_not_allowed"}'


def _form_request(body_str, headers={}):
    merged = dict(headers)
    if "Content-Type" not in merged and "content-type" not in merged:
        merged["Content-Type"] = "application/x-www-form-urlencoded"
    return VGSHttpRequest(
        "https://api.stripe.com/v1/payment_intents/pi_123/confirm",
        data=builtins.bytes(body_str),
        headers=merged,
        method="POST",
    )


def _assert_forbidden(result):
    asserts.assert_that(result.__name__).is_equal_to("ShortCircuitResponse")
    asserts.assert_that(result.response.status_code).is_equal_to(403)
    asserts.assert_that(result.response.body).is_equal_to(FORBIDDEN_BODY)
    asserts.assert_that(result.response.headers["Content-Type"]).is_equal_to("application/json")


def _test_allowed_form_key_returns_none():
    request = _form_request("key=" + ALLOWED)
    asserts.assert_that(stripe.require_publishable_key(request, ALLOWED_KEYS)).is_none()


def _test_disallowed_form_key_returns_403():
    request = _form_request("key=" + DISALLOWED)
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_missing_key_returns_403():
    request = VGSHttpRequest(
        "https://api.stripe.com/v1/payment_intents/pi_123/confirm",
        data=builtins.bytes(""),
        headers={},
        method="POST",
    )
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_bearer_allowed_returns_none():
    request = VGSHttpRequest(
        "https://api.stripe.com/v1/payment_intents/pi_123/confirm",
        data=None,
        headers={"Authorization": "Bearer " + ALLOWED},
        method="POST",
    )
    asserts.assert_that(stripe.require_publishable_key(request, ALLOWED_KEYS)).is_none()


def _test_bearer_disallowed_returns_403():
    request = VGSHttpRequest(
        "https://api.stripe.com/v1/payment_intents/pi_123/confirm",
        data=None,
        headers={"Authorization": "Bearer " + DISALLOWED},
        method="POST",
    )
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_form_key_wins_over_bearer_header():
    request = _form_request(
        "key=" + DISALLOWED,
        headers={"Authorization": "Bearer " + ALLOWED},
    )
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))

    request = _form_request(
        "key=" + ALLOWED,
        headers={"Authorization": "Bearer " + DISALLOWED},
    )
    asserts.assert_that(stripe.require_publishable_key(request, ALLOWED_KEYS)).is_none()


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_allowed_form_key_returns_none))
    _suite.addTest(unittest.FunctionTestCase(_test_disallowed_form_key_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_missing_key_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_bearer_allowed_returns_none))
    _suite.addTest(unittest.FunctionTestCase(_test_bearer_disallowed_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_form_key_wins_over_bearer_header))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
