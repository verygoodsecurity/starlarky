"""Unit tests for stripe.star"""
load("@stdlib//base64", base64="base64")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vgs//http/request", VGSHttpRequest="VGSHttpRequest")
load("@vgs//stripe", stripe="stripe")

ALLOWED_KEYS = ["pk_test_allowed"]
ALLOWED = "pk_test_allowed"
DISALLOWED = "pk_test_disallowed"
FORBIDDEN_BODY = b'{"error":"psp_key_not_allowed"}'
FORBIDDEN_ACCOUNT_BODY = b'{"error":"psp_account_not_allowed"}'
ALLOWED_ACCOUNT = "acct_allowed"
DISALLOWED_ACCOUNT = "acct_disallowed"


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


def _assert_account_forbidden(result):
    asserts.assert_that(result.__name__).is_equal_to("ShortCircuitResponse")
    asserts.assert_that(result.response.status_code).is_equal_to(403)
    asserts.assert_that(result.response.body).is_equal_to(FORBIDDEN_ACCOUNT_BODY)
    asserts.assert_that(result.response.headers["Content-Type"]).is_equal_to("application/json")


def _basic_auth_header(username, password):
    token = base64.b64encode(builtins.bytes(username + ":" + password))
    return "Basic " + str(token)


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
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_form_allowed_bearer_disallowed_returns_403():
    request = _form_request(
        "key=" + ALLOWED,
        headers={"Authorization": "Bearer " + DISALLOWED},
    )
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_duplicate_form_keys_one_disallowed_returns_403():
    request = _form_request("key=" + ALLOWED + "&key=" + DISALLOWED)
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_query_disallowed_with_form_allowed_returns_403():
    request = _form_request(
        "key=" + ALLOWED,
        headers={},
    )
    request = VGSHttpRequest(
        "https://api.stripe.com/v1/payment_intents/pi_123/confirm?key=" + DISALLOWED,
        data=request.body,
        headers=request.headers,
        method="POST",
    )
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_basic_auth_disallowed_with_form_allowed_returns_403():
    request = _form_request(
        "key=" + ALLOWED,
        headers={"Authorization": _basic_auth_header(DISALLOWED, "secret")},
    )
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_basic_auth_allowed_username_alone_returns_none():
    request = VGSHttpRequest(
        "https://api.stripe.com/v1/payment_intents/pi_123/confirm",
        data=None,
        headers={"Authorization": _basic_auth_header(ALLOWED, "secret")},
        method="POST",
    )
    asserts.assert_that(stripe.require_publishable_key(request, ALLOWED_KEYS)).is_none()


def _test_stripe_account_forbidden_when_not_in_allowlist():
    request = _form_request(
        "key=" + ALLOWED,
        headers={"Stripe-Account": DISALLOWED_ACCOUNT},
    )
    _assert_account_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))

    request = _form_request(
        "key=" + ALLOWED,
        headers={"Stripe-Account": ALLOWED_ACCOUNT},
    )
    asserts.assert_that(
        stripe.require_publishable_key(
            request,
            ALLOWED_KEYS,
            allowed_accounts=[ALLOWED_ACCOUNT],
        )
    ).is_none()


def _test_form_and_bearer_same_allowed_key_returns_none():
    request = _form_request(
        "key=" + ALLOWED,
        headers={"Authorization": "Bearer " + ALLOWED},
    )
    asserts.assert_that(stripe.require_publishable_key(request, ALLOWED_KEYS)).is_none()


def _request_with_authorization_headers(first, second):
    request = _form_request("key=" + ALLOWED)
    request.headers.add("Authorization", first)
    request.headers.add("Authorization", second)
    return request


def _test_two_authorization_headers_returns_403():
    request = _request_with_authorization_headers(
        "Bearer " + ALLOWED,
        "Bearer " + DISALLOWED,
    )
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _request_with_stripe_account_headers(first, second):
    request = _form_request("key=" + ALLOWED)
    request.headers.add("Stripe-Account", first)
    request.headers.add("Stripe-Account", second)
    return request


def _test_two_stripe_account_headers_returns_403():
    request = _request_with_stripe_account_headers(ALLOWED_ACCOUNT, ALLOWED_ACCOUNT)
    _assert_account_forbidden(
        stripe.require_publishable_key(
            request,
            ALLOWED_KEYS,
            allowed_accounts=[ALLOWED_ACCOUNT],
        )
    )


def _test_basic_with_appended_junk_returns_403():
    token = str(base64.b64encode(builtins.bytes(ALLOWED + ":"))) + "!!"
    request = VGSHttpRequest(
        "https://api.stripe.com/v1/payment_intents/pi_123/confirm",
        data=None,
        headers={"Authorization": "Basic " + token},
        method="POST",
    )
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_basic_with_bad_padding_returns_403():
    request = VGSHttpRequest(
        "https://api.stripe.com/v1/payment_intents/pi_123/confirm",
        data=None,
        headers={"Authorization": "Basic YQ"},
        method="POST",
    )
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _prod_message(uri, header_pairs, body_bytes):
    # mirrors LarkySanitizedHttpMessage (uri/headers/raw_body/body/phase methods)
    return larky.struct(
        uri=lambda: uri,
        headers=lambda: header_pairs,
        raw_body=lambda: body_bytes,
        body=lambda charset="UTF-8": str(body_bytes),
        phase=lambda: "REQUEST",
    )


def _test_prod_origin_form_allowed_form_key_returns_none():
    body = builtins.bytes("key=" + ALLOWED)
    message = _prod_message(
        "/v1/payment_intents/pi_123/confirm",
        [("Content-Type", "application/x-www-form-urlencoded")],
        body,
    )
    asserts.assert_that(stripe.require_publishable_key(message, ALLOWED_KEYS)).is_none()


def _test_prod_origin_form_query_key_disallowed_returns_403():
    body = builtins.bytes("key=" + ALLOWED)
    message = _prod_message(
        "/v1/x?key=" + DISALLOWED,
        [("Content-Type", "application/x-www-form-urlencoded")],
        body,
    )
    _assert_forbidden(stripe.require_publishable_key(message, ALLOWED_KEYS))


def _test_prod_duplicate_authorization_pairs_returns_403():
    body = builtins.bytes("key=" + ALLOWED)
    message = _prod_message(
        "/v1/payment_intents/pi_123/confirm",
        [
            ("Content-Type", "application/x-www-form-urlencoded"),
            ("Authorization", "Bearer " + ALLOWED),
            ("Authorization", "Bearer " + DISALLOWED),
        ],
        body,
    )
    _assert_forbidden(stripe.require_publishable_key(message, ALLOWED_KEYS))


def _test_prod_stripe_account_not_allowed_returns_403():
    body = builtins.bytes("key=" + ALLOWED)
    message = _prod_message(
        "/v1/payment_intents/pi_123/confirm",
        [
            ("Content-Type", "application/x-www-form-urlencoded"),
            ("Stripe-Account", DISALLOWED_ACCOUNT),
        ],
        body,
    )
    _assert_account_forbidden(stripe.require_publishable_key(message, ALLOWED_KEYS))


def _test_basic_token_decoding_to_invalid_utf8_vgs_returns_403():
    request = VGSHttpRequest(
        "https://api.stripe.com/v1/payment_intents/pi_123/confirm",
        data=None,
        headers={"Authorization": "Basic /w=="},
        method="POST",
    )
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_basic_token_decoding_to_invalid_utf8_prod_returns_403():
    message = _prod_message(
        "/v1/payment_intents/pi_123/confirm",
        [("Authorization", "Basic /w==")],
        builtins.bytes(""),
    )
    _assert_forbidden(stripe.require_publishable_key(message, ALLOWED_KEYS))


def _utf16le_bytes(text):
    units = []
    for i in range(len(text)):
        units.append(ord(text[i]))
        units.append(0)
    return builtins.bytes(units)


def _bytes_with_suffix(text, suffix_ints):
    units = []
    for i in range(len(text)):
        units.append(ord(text[i]))
    units.extend(suffix_ints)
    return builtins.bytes(units)


def _form_request_bytes(body_bytes, headers):
    return VGSHttpRequest(
        "https://api.stripe.com/v1/payment_intents/pi_123/confirm",
        data=body_bytes,
        headers=headers,
        method="POST",
    )


def _test_utf16_form_body_with_bearer_allowed_is_refused():
    body = _utf16le_bytes("key=" + DISALLOWED)
    for content_type in [
        "application/x-www-form-urlencoded",
        "application/x-www-form-urlencoded; charset=UTF-16LE",
    ]:
        request = _form_request_bytes(
            body,
            {"Content-Type": content_type, "Authorization": "Bearer " + ALLOWED},
        )
        _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))

        message = _prod_message(
            "/v1/payment_intents/pi_123/confirm",
            [
                ("Content-Type", content_type),
                ("Authorization", "Bearer " + ALLOWED),
            ],
            body,
        )
        _assert_forbidden(stripe.require_publishable_key(message, ALLOWED_KEYS))


def _test_form_body_with_nul_is_refused():
    request = _form_request_bytes(
        _bytes_with_suffix("key=" + ALLOWED + "&x=", [0]),
        {"Content-Type": "application/x-www-form-urlencoded"},
    )
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_form_body_with_invalid_utf8_is_refused():
    request = _form_request_bytes(
        _bytes_with_suffix("key=" + ALLOWED + "&x=", [0xff]),
        {"Content-Type": "application/x-www-form-urlencoded"},
    )
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_form_charset_iso_8859_1_is_refused():
    request = _form_request(
        "key=" + ALLOWED,
        headers={"Content-Type": "application/x-www-form-urlencoded; charset=ISO-8859-1"},
    )
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_form_charset_utf8_quoted_is_accepted():
    request = _form_request(
        "key=" + ALLOWED,
        headers={"Content-Type": "application/x-www-form-urlencoded; charset=\"UTF-8\""},
    )
    asserts.assert_that(stripe.require_publishable_key(request, ALLOWED_KEYS)).is_none()


def _bearer_request(token, extra_pairs):
    request = VGSHttpRequest(
        "https://api.stripe.com/v1/payment_intents/pi_123/confirm",
        data=None,
        headers={"Authorization": "Bearer " + token},
        method="POST",
    )
    for pair in extra_pairs:
        request.headers.add(pair[0], pair[1])
    return request


def _bearer_prod_message(token, extra_pairs):
    return _prod_message(
        "/v1/payment_intents/pi_123/confirm",
        [("Authorization", "Bearer " + token)] + extra_pairs,
        builtins.bytes(""),
    )


def _test_stripe_context_not_allowed_is_refused():
    for name in ["Stripe-Context", "stripe-context"]:
        pairs = [(name, DISALLOWED_ACCOUNT)]
        for request in [_bearer_request(ALLOWED, pairs), _bearer_prod_message(ALLOWED, pairs)]:
            _assert_account_forbidden(
                stripe.require_publishable_key(
                    request,
                    ALLOWED_KEYS,
                    allowed_accounts=[ALLOWED_ACCOUNT],
                )
            )
            _assert_account_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))


def _test_stripe_context_allowed_continues():
    for name in ["Stripe-Context", "stripe-context"]:
        pairs = [(name, ALLOWED_ACCOUNT)]
        for request in [_bearer_request(ALLOWED, pairs), _bearer_prod_message(ALLOWED, pairs)]:
            asserts.assert_that(
                stripe.require_publishable_key(
                    request,
                    ALLOWED_KEYS,
                    allowed_accounts=[ALLOWED_ACCOUNT],
                )
            ).is_none()


def _test_duplicate_stripe_context_is_refused():
    pairs = [("Stripe-Context", ALLOWED_ACCOUNT), ("stripe-context", ALLOWED_ACCOUNT)]
    for request in [_bearer_request(ALLOWED, pairs), _bearer_prod_message(ALLOWED, pairs)]:
        _assert_account_forbidden(
            stripe.require_publishable_key(
                request,
                ALLOWED_KEYS,
                allowed_accounts=[ALLOWED_ACCOUNT],
            )
        )


def _test_secret_key_listed_in_allowed_keys_is_refused():
    for request in [_bearer_request("sk_test_x", []), _bearer_prod_message("sk_test_x", [])]:
        _assert_forbidden(stripe.require_publishable_key(request, ["sk_test_x"]))


def _test_org_key_is_refused():
    org_key = "sk_org_test_x"
    for request in [_bearer_request(org_key, []), _bearer_prod_message(org_key, [])]:
        _assert_forbidden(stripe.require_publishable_key(request, [org_key]))
    _assert_forbidden(stripe.require_publishable_key(_form_request("key=" + org_key), [org_key]))


def _test_duplicate_content_type_is_refused():
    body = builtins.bytes("key=" + DISALLOWED)
    request = _form_request_bytes(
        body,
        {"Content-Type": "text/plain", "Authorization": "Bearer " + ALLOWED},
    )
    request.headers.add("content-type", "application/x-www-form-urlencoded")
    _assert_forbidden(stripe.require_publishable_key(request, ALLOWED_KEYS))

    message = _prod_message(
        "/v1/payment_intents/pi_123/confirm",
        [
            ("Content-Type", "text/plain"),
            ("Authorization", "Bearer " + ALLOWED),
            ("content-type", "application/x-www-form-urlencoded"),
        ],
        body,
    )
    _assert_forbidden(stripe.require_publishable_key(message, ALLOWED_KEYS))


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_allowed_form_key_returns_none))
    _suite.addTest(unittest.FunctionTestCase(_test_disallowed_form_key_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_missing_key_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_bearer_allowed_returns_none))
    _suite.addTest(unittest.FunctionTestCase(_test_bearer_disallowed_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_form_key_wins_over_bearer_header))
    _suite.addTest(unittest.FunctionTestCase(_test_form_allowed_bearer_disallowed_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_duplicate_form_keys_one_disallowed_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_query_disallowed_with_form_allowed_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_basic_auth_disallowed_with_form_allowed_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_basic_auth_allowed_username_alone_returns_none))
    _suite.addTest(unittest.FunctionTestCase(_test_stripe_account_forbidden_when_not_in_allowlist))
    _suite.addTest(unittest.FunctionTestCase(_test_form_and_bearer_same_allowed_key_returns_none))
    _suite.addTest(unittest.FunctionTestCase(_test_two_authorization_headers_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_two_stripe_account_headers_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_basic_with_appended_junk_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_basic_with_bad_padding_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_prod_origin_form_allowed_form_key_returns_none))
    _suite.addTest(unittest.FunctionTestCase(_test_prod_origin_form_query_key_disallowed_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_prod_duplicate_authorization_pairs_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_prod_stripe_account_not_allowed_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_basic_token_decoding_to_invalid_utf8_vgs_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_basic_token_decoding_to_invalid_utf8_prod_returns_403))
    _suite.addTest(unittest.FunctionTestCase(_test_utf16_form_body_with_bearer_allowed_is_refused))
    _suite.addTest(unittest.FunctionTestCase(_test_form_body_with_nul_is_refused))
    _suite.addTest(unittest.FunctionTestCase(_test_form_body_with_invalid_utf8_is_refused))
    _suite.addTest(unittest.FunctionTestCase(_test_form_charset_iso_8859_1_is_refused))
    _suite.addTest(unittest.FunctionTestCase(_test_form_charset_utf8_quoted_is_accepted))
    _suite.addTest(unittest.FunctionTestCase(_test_stripe_context_not_allowed_is_refused))
    _suite.addTest(unittest.FunctionTestCase(_test_stripe_context_allowed_continues))
    _suite.addTest(unittest.FunctionTestCase(_test_duplicate_stripe_context_is_refused))
    _suite.addTest(unittest.FunctionTestCase(_test_secret_key_listed_in_allowed_keys_is_refused))
    _suite.addTest(unittest.FunctionTestCase(_test_org_key_is_refused))
    _suite.addTest(unittest.FunctionTestCase(_test_duplicate_content_type_is_refused))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
