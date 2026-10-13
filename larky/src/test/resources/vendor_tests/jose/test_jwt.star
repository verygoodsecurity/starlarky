# Ported from python-jose 3.5.0 (tag 3.5.0, commit
# 018b310ddb8b50dcfd09a0c152117835a21dd656), file tests/test_jwt.py:
# https://github.com/mpdavis/python-jose/blob/018b310ddb8b50dcfd09a0c152117835a21dd656/tests/test_jwt.py
# Translated with `py2star larkify -t` and fixed by hand.
#
# Changes from upstream:
# - pytest fixtures are module constants and parametrized tests loop over
#   their cases.
# - `pytest.raises(JWTError)` is assert_fails(..., JWT_ERROR): Larky errors are
#   strings, and JWTClaimsError/ExpiredSignatureError subclass JWTError
#   upstream.
# - Times are fixed NumericDate integers, and decode() is given now=NOW
#   (Larky scripts cannot read the clock), where upstream uses
#   datetime.now(UTC) +/- timedelta.
# - test_invalid_claims_json/test_invalid_claims replaced jws.verify with a
#   stub; Larky modules are frozen, so the token is signed over the bad
#   payload instead.
# - test_leeway_is_timedelta is not ported: Larky has no timedelta.
# - The tests after "Larky additions" are not upstream.

load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//jose", jws="jws", jwt="jwt")
load("@vendor//jose/jwt", jwt_submodule="jwt")
load("@vendor//jose/utils", base64url_decode="base64url_decode", base64url_encode="base64url_encode")
load("@stdlib//json", json="json")


# Any JWTError, including its upstream subclasses.
JWT_ERROR = "^(JWTError|JWTClaimsError|ExpiredSignatureError): "

# A fixed "current time" (2023-11-14T22:13:20Z).
NOW = 1700000000

# fixtures
claims = {"a": "b"}
key = "secret"
headers = {
    "kid": "my-key-id",
}


def _b64decode_str(s):
    return base64url_decode(bytes(s, encoding="utf-8")).decode("utf-8")


def TestJWT_test_no_alg():
    token = jwt.encode(claims, key, algorithm="HS384")
    b64header, b64payload, b64signature = token.split(".")
    header = json.loads(_b64decode_str(b64header))
    header.pop("alg")
    bad_header_json_bytes = bytes(json.dumps(header), encoding="utf-8")
    bad_b64header = base64url_encode(bad_header_json_bytes).decode("utf-8")
    bad_token = ".".join([bad_b64header, b64payload, b64signature])
    asserts.assert_fails(lambda: jwt.decode(token=bad_token, key=key, algorithms=[]), JWT_ERROR)


def TestJWT_test_numeric_key():
    for key, token in [
        (
            "1234567890",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoidGVzdCJ9.aNBlulVhiYSCzvsh1rTzXZC2eWJmNrMBjINT-0wQz4k",
        ),
        (
            "123456789.0",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoidGVzdCJ9.D8WLFPMi3yKgua2jm3BKThFsParXpgxhIbsUc39zJDw",
        ),
    ]:
        token_info = jwt.decode(token, key)
        asserts.assert_that(token_info).is_equal_to({"name": "test"})


def TestJWT_test_invalid_claims_json():
    token = jws.sign(b'["a", "b"}', "secret", algorithm="HS256")
    asserts.assert_fails(lambda: jwt.decode(token, "secret", ["HS256"]), "^JWTError: Invalid payload string: ")


def TestJWT_test_invalid_claims():
    token = jws.sign(b'["a","b"]', "secret", algorithm="HS256")
    asserts.assert_fails(
        lambda: jwt.decode(token, "secret", ["HS256"]),
        "^JWTError: Invalid payload string: must be a json object",
    )


def TestJWT_test_non_default_alg():
    encoded = jwt.encode(claims, key, algorithm="HS384")
    decoded = jwt.decode(encoded, key, algorithms="HS384")
    asserts.assert_that(claims).is_equal_to(decoded)


def TestJWT_test_non_default_alg_positional_bwcompat():
    encoded = jwt.encode(claims, key, "HS384")
    decoded = jwt.decode(encoded, key, "HS384")
    asserts.assert_that(claims).is_equal_to(decoded)


def TestJWT_test_no_alg_default_headers():
    token = jwt.encode(claims, key, algorithm="HS384")
    b64header, b64payload, b64signature = token.split(".")
    bad_token = b64header + "." + b64payload
    asserts.assert_fails(lambda: jwt.get_unverified_headers(bad_token), JWT_ERROR)


def TestJWT_test_non_default_headers():
    encoded = jwt.encode(claims, key, headers=headers)
    decoded = jwt.decode(encoded, key)
    asserts.assert_that(claims).is_equal_to(decoded)
    all_headers = jwt.get_unverified_headers(encoded)
    for k, v in headers.items():
        asserts.assert_that(all_headers[k]).is_equal_to(v)


def TestJWT_test_deterministic_headers():
    claims = {"a": "b"}
    key = "secret"

    # Larky dicts keep insertion order, like the upstream OrderedDicts.
    headers1 = {"kid": "my-key-id", "another_key": "another_value"}
    encoded1 = jwt.encode(claims, key, algorithm="HS256", headers=headers1)
    encoded_headers1 = encoded1.split(".", 1)[0]

    headers2 = {"another_key": "another_value", "kid": "my-key-id"}
    encoded2 = jwt.encode(claims, key, algorithm="HS256", headers=headers2)
    encoded_headers2 = encoded2.split(".", 1)[0]

    asserts.assert_that(encoded_headers1).is_equal_to(encoded_headers2)

    # manually decode header to compare it to known good
    decoded_headers1 = base64url_decode(bytes(encoded_headers1, encoding="utf-8"))
    asserts.assert_that(decoded_headers1).is_equal_to(
        b'{"alg":"HS256","another_key":"another_value","kid":"my-key-id","typ":"JWT"}'
    )


def TestJWT_test_encode():
    expected = (
        ("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9" + ".eyJhIjoiYiJ9" + ".xNtk2S0CNbCBZX_f67pFgGRugaP1xi2ICfet3nwOSxw"),
        ("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" + ".eyJhIjoiYiJ9" + ".jiMyrsmD8AoHWeQgmxZ5yq8z0lXS67_QGs52AzC8Ru8"),
    )

    encoded = jwt.encode(claims, key)

    asserts.assert_that(encoded in expected).is_true()


def TestJWT_test_decode():
    token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" + ".eyJhIjoiYiJ9" + ".jiMyrsmD8AoHWeQgmxZ5yq8z0lXS67_QGs52AzC8Ru8"

    decoded = jwt.decode(token, key)

    asserts.assert_that(decoded).is_equal_to(claims)


def TestJWT_test_round_trip_with_different_key_types():
    for key in [b"key", "key"]:
        token = jwt.encode({"testkey": "testvalue"}, key, algorithm="HS256")
        verified_data = jwt.decode(token, key, algorithms=["HS256"])
        asserts.assert_that("testkey" in verified_data.keys()).is_true()
        asserts.assert_that(verified_data["testkey"]).is_equal_to("testvalue")


def TestJWT_test_leeway_is_int():
    pass


def TestJWT_test_iat_not_int():
    claims = {"iat": "test"}

    token = jwt.encode(claims, key)

    asserts.assert_fails(lambda: jwt.decode(token, key), JWT_ERROR)


def TestJWT_test_nbf_not_int():
    claims = {"nbf": "test"}

    token = jwt.encode(claims, key)

    asserts.assert_fails(lambda: jwt.decode(token, key, now=NOW), JWT_ERROR)


def TestJWT_test_nbf_datetime():
    nbf = NOW - 5

    claims = {"nbf": nbf}

    token = jwt.encode(claims, key)
    jwt.decode(token, key, now=NOW)


def TestJWT_test_nbf_with_leeway():
    nbf = NOW + 5

    claims = {
        "nbf": nbf,
    }

    options = {"leeway": 10}

    token = jwt.encode(claims, key)
    jwt.decode(token, key, options=options, now=NOW)


def TestJWT_test_nbf_in_future():
    nbf = NOW + 5

    claims = {"nbf": nbf}

    token = jwt.encode(claims, key)

    asserts.assert_fails(lambda: jwt.decode(token, key, now=NOW), JWT_ERROR)


def TestJWT_test_nbf_skip():
    nbf = NOW + 5

    claims = {"nbf": nbf}

    token = jwt.encode(claims, key)

    asserts.assert_fails(lambda: jwt.decode(token, key, now=NOW), JWT_ERROR)

    options = {"verify_nbf": False}

    jwt.decode(token, key, options=options, now=NOW)


def TestJWT_test_exp_not_int():
    claims = {"exp": "test"}

    token = jwt.encode(claims, key)

    asserts.assert_fails(lambda: jwt.decode(token, key, now=NOW), JWT_ERROR)


def TestJWT_test_exp_datetime():
    exp = NOW + 5

    claims = {"exp": exp}

    token = jwt.encode(claims, key)
    jwt.decode(token, key, now=NOW)


def TestJWT_test_exp_with_leeway():
    exp = NOW - 5

    claims = {
        "exp": exp,
    }

    options = {"leeway": 10}

    token = jwt.encode(claims, key)
    jwt.decode(token, key, options=options, now=NOW)


def TestJWT_test_exp_in_past():
    exp = NOW - 5

    claims = {"exp": exp}

    token = jwt.encode(claims, key)

    asserts.assert_fails(lambda: jwt.decode(token, key, now=NOW), JWT_ERROR)


def TestJWT_test_exp_skip():
    exp = NOW - 5

    claims = {"exp": exp}

    token = jwt.encode(claims, key)

    asserts.assert_fails(lambda: jwt.decode(token, key, now=NOW), JWT_ERROR)

    options = {"verify_exp": False}

    jwt.decode(token, key, options=options, now=NOW)


def TestJWT_test_aud_string():
    aud = "audience"

    claims = {"aud": aud}

    token = jwt.encode(claims, key)
    jwt.decode(token, key, audience=aud)


def TestJWT_test_aud_list():
    aud = "audience"

    claims = {"aud": [aud]}

    token = jwt.encode(claims, key)
    jwt.decode(token, key, audience=aud)


def TestJWT_test_aud_list_multiple():
    aud = "audience"

    claims = {"aud": [aud, "another"]}

    token = jwt.encode(claims, key)
    jwt.decode(token, key, audience=aud)


def TestJWT_test_aud_list_is_strings():
    aud = "audience"

    claims = {"aud": [aud, 1]}

    token = jwt.encode(claims, key)
    asserts.assert_fails(lambda: jwt.decode(token, key, audience=aud), JWT_ERROR)


def TestJWT_test_aud_case_sensitive():
    aud = "audience"

    claims = {"aud": [aud]}

    token = jwt.encode(claims, key)
    asserts.assert_fails(lambda: jwt.decode(token, key, audience="AUDIENCE"), JWT_ERROR)


def TestJWT_test_aud_empty_claim():
    aud = "audience"

    token = jwt.encode(claims, key)
    jwt.decode(token, key, audience=aud)


def TestJWT_test_aud_not_string_or_list():
    aud = 1

    claims = {"aud": aud}

    token = jwt.encode(claims, key)
    asserts.assert_fails(lambda: jwt.decode(token, key), JWT_ERROR)


def TestJWT_test_aud_given_number():
    aud = "audience"

    claims = {"aud": aud}

    token = jwt.encode(claims, key)
    asserts.assert_fails(lambda: jwt.decode(token, key, audience=1), JWT_ERROR)


def TestJWT_test_iss_string():
    iss = "issuer"

    claims = {"iss": iss}

    token = jwt.encode(claims, key)
    jwt.decode(token, key, issuer=iss)


def TestJWT_test_iss_list():
    iss = "issuer"

    claims = {"iss": iss}

    token = jwt.encode(claims, key)
    jwt.decode(token, key, issuer=["https://issuer", "issuer"])


def TestJWT_test_iss_tuple():
    iss = "issuer"

    claims = {"iss": iss}

    token = jwt.encode(claims, key)
    jwt.decode(token, key, issuer=("https://issuer", "issuer"))


def TestJWT_test_iss_invalid():
    iss = "issuer"

    claims = {"iss": iss}

    token = jwt.encode(claims, key)
    asserts.assert_fails(lambda: jwt.decode(token, key, issuer="another"), JWT_ERROR)


def TestJWT_test_sub_string():
    sub = "subject"

    claims = {"sub": sub}

    token = jwt.encode(claims, key)
    jwt.decode(token, key)


def TestJWT_test_sub_invalid():
    sub = 1

    claims = {"sub": sub}

    token = jwt.encode(claims, key)
    asserts.assert_fails(lambda: jwt.decode(token, key), JWT_ERROR)


def TestJWT_test_sub_correct():
    sub = "subject"

    claims = {"sub": sub}

    token = jwt.encode(claims, key)
    jwt.decode(token, key, subject=sub)


def TestJWT_test_sub_incorrect():
    sub = "subject"

    claims = {"sub": sub}

    token = jwt.encode(claims, key)
    asserts.assert_fails(lambda: jwt.decode(token, key, subject="another"), JWT_ERROR)


def TestJWT_test_jti_string():
    jti = "JWT ID"

    claims = {"jti": jti}

    token = jwt.encode(claims, key)
    jwt.decode(token, key)


def TestJWT_test_jti_invalid():
    jti = 1

    claims = {"jti": jti}

    token = jwt.encode(claims, key)
    asserts.assert_fails(lambda: jwt.decode(token, key), JWT_ERROR)


def TestJWT_test_at_hash():
    claims = {"a": "b"}
    access_token = "<ACCESS_TOKEN>"
    token = jwt.encode(claims, key, access_token=access_token)
    payload = jwt.decode(token, key, access_token=access_token)
    asserts.assert_that("at_hash" in payload).is_true()


def TestJWT_test_at_hash_invalid():
    claims = {"a": "b"}
    token = jwt.encode(claims, key, access_token="<ACCESS_TOKEN>")
    asserts.assert_fails(lambda: jwt.decode(token, key, access_token="<OTHER_TOKEN>"), JWT_ERROR)


def TestJWT_test_at_hash_missing_access_token():
    claims = {"a": "b"}
    token = jwt.encode(claims, key, access_token="<ACCESS_TOKEN>")
    asserts.assert_fails(lambda: jwt.decode(token, key), JWT_ERROR)


def TestJWT_test_at_hash_missing_claim():
    token = jwt.encode(claims, key)
    payload = jwt.decode(token, key, access_token="<ACCESS_TOKEN>")
    asserts.assert_that("at_hash" not in payload).is_true()


def TestJWT_test_at_hash_unable_to_calculate():
    claims = {"a": "b"}
    token = jwt.encode(claims, key, access_token="<ACCESS_TOKEN>")
    asserts.assert_fails(lambda: jwt.decode(token, key, access_token="â"), JWT_ERROR)


def TestJWT_test_bad_claims():
    bad_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.iOJ5SiNfaNO_pa2J4Umtb3b3zmk5C18-mhTCVNsjnck"
    asserts.assert_fails(lambda: jwt.get_unverified_claims(bad_token), JWT_ERROR)


def TestJWT_test_unverified_claims_string():
    token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.aW52YWxpZCBjbGFpbQ.iOJ5SiNfaNO_pa2J4Umtb3b3zmk5C18-mhTCVNsjnck"
    asserts.assert_fails(lambda: jwt.get_unverified_claims(token), JWT_ERROR)


def TestJWT_test_unverified_claims_list():
    token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.WyJpbnZhbGlkIiwgImNsYWltcyJd.nZvw_Rt1FfUPb5OiVbrSYZGtWSE5c-gdJ6nQnTTBkYo"
    asserts.assert_fails(lambda: jwt.get_unverified_claims(token), JWT_ERROR)


def TestJWT_test_unverified_claims_object():
    token = jwt.encode(claims, key)
    asserts.assert_that(jwt.get_unverified_claims(token)).is_equal_to(claims)


def TestJWT_test_require():
    for claim, value in [
        ("aud", "aud"),
        ("ait", "ait"),
        ("exp", NOW + 3600),
        ("nbf", NOW - 5),
        ("iss", "iss"),
        ("sub", "sub"),
        ("jti", "jti"),
    ]:
        options = {"require_" + claim: True, "verify_" + claim: False}

        token = jwt.encode(claims, key)
        asserts.assert_fails(
            lambda: jwt.decode(token, key, options=options, audience=str(value), now=NOW),
            JWT_ERROR,
        )

        new_claims = dict(claims)
        new_claims[claim] = value
        token = jwt.encode(new_claims, key)
        jwt.decode(token, key, options=options, audience=str(value), now=NOW)


# Larky additions

rsa_public_key = b"-----BEGIN PUBLIC KEY-----\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCQouotdvmxpjokxe5GZxNQbWVQ\n6ip3T7k5xbU37jJheojRJm0wtTSbtIetDXr5vHp0/SnuxzMvEccB7/UrWWGOabVx\nHas4edX2X7Ie9JTDg9G9smkcCDlQLD6cvxgIy7afH5rG//SoBgMPwnAicHXaEuWp\nu+MaeJm1BAg6MND/2QIDAQAB\n-----END PUBLIC KEY-----"
rsa_private_key = b"-----BEGIN RSA PRIVATE KEY-----\nMIICXAIBAAKBgQCQouotdvmxpjokxe5GZxNQbWVQ6ip3T7k5xbU37jJheojRJm0w\ntTSbtIetDXr5vHp0/SnuxzMvEccB7/UrWWGOabVxHas4edX2X7Ie9JTDg9G9smkc\nCDlQLD6cvxgIy7afH5rG//SoBgMPwnAicHXaEuWpu+MaeJm1BAg6MND/2QIDAQAB\nAoGAbJKfD7n7/is2AlzCXP8LNJiqMW9WqXGjLYcIXg/kqd/9zGL4HFQqRafjITi5\nU7b0hdV1INVPysmhhgbHF99kpw3ee31mSQtenn//SGQFJeOn+QE+R63FH8icrod/\nNsXqR4w/61RS07wiYOtfzN/7czczi6WQV4W3xnugG6971vECQQDKCHyJ9W4xuYMl\nKcuQwokFsDKR1s9UpUPOCLeaQJFmW3hCHrWsFic2AH1oWxUrJdaAYL/DFTmtzpXJ\nz+a6heCHAkEAt0V/zqCWjG0+OIZopTaGS6/0Rtp67kAy2GlFLpg9hnIqTqgtHS1+\noeT9D0qQrNKGu5fR14WFQltTPxkWNOAUnwJABR7T8TcsNMxr23xEsYWMrX06uuGD\n3bRWlJk59gne5YY59QsMNbFWCxNWGlf8oFxUJGrPUWVvUc1jlHrVcTLFbwJBAJTD\nYDwMFEgGgMQHLjg1KwuS1tkQjUqJZ/xMbvCkeQSB9R+F2aDehfTJ2DQqVYdDGER7\ntsSXyBSV5tvH9EOVRIcCQFktF/V6TW6X3I3XmQjEuie4W0UT8tpWUcgkwO8slvra\nQ7aSCBe5pLlVT0SorTqH3ywozGy2uzGyaigjtIFbgcM=\n-----END RSA PRIVATE KEY-----"
ecc_public_key = b"-----BEGIN PUBLIC KEY-----\nMFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAE7l9S5znW4VsHl3XXM1cFPXzkJ17QTX+H\nE4gS/P/0EgxJtkpIuKFNwjZnetGKCkJmIjdsVxtzQV3GPOTsi/Btzg==\n-----END PUBLIC KEY-----"
ecc_private_key = b"-----BEGIN EC PRIVATE KEY-----\nMHQCAQEEIPjfg99RGAgLkIWU+Ell5EWzQMXJ99C++CYaZ8DN4wmfoAcGBSuBBAAK\noUQDQgAE7l9S5znW4VsHl3XXM1cFPXzkJ17QTX+HE4gS/P/0EgxJtkpIuKFNwjZn\netGKCkJmIjdsVxtzQV3GPOTsi/Btzg==\n-----END EC PRIVATE KEY-----"

# Generated with python-jose 3.5.0 (cryptography backend):
# jwt.encode({"sub": "1234", "iss": "vgs", "exp": 1700003600}, rsa_private_key, algorithm="RS256")
RS256_UPSTREAM_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0IiwiaXNzIjoidmdzIiwiZXhwIjoxNzAwMDAzNjAwfQ.I9h3I7s_fWnefFHtOrmUobxAcaVsxnbVxmccSjiIzfYYVkb1ntrLGVLNX7cX0nM7VRtceQ-yuVHmdokQehtY9qvx3p3apKhCSXkyiYusXxF9K46c-Uvs8JA-N6p0zuSaHx0RpIFKz_7jZ9x5W4WJr_TrattIiBeVGILiGnNjoIM"

# A P-256 test key pair (trailing newline removed: Larky's Crypto.IO.PEM
# rejects it), and
# jwt.encode({"sub": "1234", "exp": 1700003600}, p256_private_key, algorithm="ES256")
# from python-jose 3.5.0.
p256_private_key = b"-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEINQ8w1Fm5qBANatem6RHkXfEfbgbLCAsEayPnLtkhE/5oAoGCCqGSM49\nAwEHoUQDQgAEsZ2JwgBv6kFQL2bJYJBxNCdSSrvYJW8mw3uXdYs05QPgudNwAkJh\nyIlzepjFstM22VHd1hnc5KjAPMEDpz9iuw==\n-----END EC PRIVATE KEY-----"
p256_public_key = b"-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEsZ2JwgBv6kFQL2bJYJBxNCdSSrvY\nJW8mw3uXdYs05QPgudNwAkJhyIlzepjFstM22VHd1hnc5KjAPMEDpz9iuw==\n-----END PUBLIC KEY-----"
ES256_UPSTREAM_TOKEN = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0IiwiZXhwIjoxNzAwMDAzNjAwfQ.qmd1F_VAPMJgbYOkXVy8hnGjoF_i8ZPmaQcj6jlcma35dRHJJPEUumaqQhkBoJlWfVDZWysc2N7VlD7TJa8LaQ"

# python-jose 3.5.0: jwt.encode({"a": "b"}, "secret", algorithm="HS512")
HS512_UPSTREAM_TOKEN = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJhIjoiYiJ9.NEvauwy77uvgEvKOlGLmLJJEseamUKhAAPaGaWlD5P5qLHkLiHC9eQOy4YwR-L3BjNN1lumBhg8eEHus23CflQ"

# python-jose 3.5.0: jwt.encode({"name": "Doe, John", "note": "a: b"}, "secret")
SEPARATORS_UPSTREAM_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiRG9lLCBKb2huIiwibm90ZSI6ImE6IGIifQ.RAx5gfpUUxgu5nad7qt9Tfy97T8-Fi7MlXuy7vmbX3E"


def _tamper(token, index):
    parts = token.split(".")
    seg = parts[index]
    # flip one base64url character that changes the decoded bytes
    first = "A" if seg[0] != "A" else "B"
    parts[index] = first + seg[1:]
    return ".".join(parts)


def Larky_test_hs512_matches_upstream():
    asserts.assert_that(jwt.encode({"a": "b"}, "secret", algorithm="HS512")).is_equal_to(HS512_UPSTREAM_TOKEN)
    asserts.assert_that(jwt.decode(HS512_UPSTREAM_TOKEN, "secret", algorithms=["HS512"])).is_equal_to({"a": "b"})


def Larky_test_hmac_round_trip():
    for alg in ["HS256", "HS384", "HS512"]:
        payload = {"sub": "1234", "iss": "vgs", "exp": NOW + 60, "iat": NOW}
        token = jwt.encode(payload, key, algorithm=alg)
        asserts.assert_that(jwt.get_unverified_header(token)["alg"]).is_equal_to(alg)
        decoded = jwt.decode(token, key, algorithms=[alg], issuer="vgs", subject="1234", now=NOW)
        asserts.assert_that(decoded).is_equal_to(payload)
        # wrong key
        asserts.assert_fails(lambda: jwt.decode(token, "other", algorithms=[alg], now=NOW),
                             "^JWTError: Signature verification failed")


def Larky_test_rs256_round_trip():
    payload = {"sub": "1234", "exp": NOW + 60}
    token = jwt.encode(payload, rsa_private_key, algorithm="RS256")
    decoded = jwt.decode(token, rsa_public_key, algorithms=["RS256"], now=NOW)
    asserts.assert_that(decoded).is_equal_to(payload)


def Larky_test_rs256_upstream_token():
    decoded = jwt.decode(RS256_UPSTREAM_TOKEN, rsa_public_key, algorithms=["RS256"], issuer="vgs", now=NOW)
    asserts.assert_that(decoded).is_equal_to({"sub": "1234", "iss": "vgs", "exp": 1700003600})
    asserts.assert_fails(
        lambda: jwt.decode(RS256_UPSTREAM_TOKEN, rsa_public_key, algorithms=["RS256"], now=1700003601),
        "^ExpiredSignatureError: Signature has expired.",
    )


def Larky_test_es256_round_trip():
    payload = {"sub": "1234", "exp": NOW + 60}
    token = jwt.encode(payload, ecc_private_key, algorithm="ES256")
    decoded = jwt.decode(token, ecc_public_key, algorithms=["ES256"], now=NOW)
    asserts.assert_that(decoded).is_equal_to(payload)
    token = jwt.encode(payload, p256_private_key, algorithm="ES256")
    decoded = jwt.decode(token, p256_public_key, algorithms=["ES256"], now=NOW)
    asserts.assert_that(decoded).is_equal_to(payload)
    decoded = jwt.decode(ES256_UPSTREAM_TOKEN, p256_public_key, algorithms=["ES256"], now=NOW)
    asserts.assert_that(decoded).is_equal_to({"sub": "1234", "exp": 1700003600})
    asserts.assert_fails(lambda: jwt.decode(_tamper(token, 2), ecc_public_key, algorithms=["ES256"], now=NOW),
                         JWT_ERROR)


def Larky_test_algorithm_not_allowed():
    token = jwt.encode(claims, rsa_private_key, algorithm="RS256")
    asserts.assert_fails(lambda: jwt.decode(token, rsa_public_key, algorithms=["HS256"]),
                         "^JWTError: The specified alg value is not allowed")


def Larky_test_tampered_token():
    token = jwt.encode({"sub": "1234", "admin": False}, key, algorithm="HS256")
    forged_payload = base64url_encode(b'{"admin":true,"sub":"1234"}').decode("utf-8")
    parts = token.split(".")
    forged = ".".join([parts[0], forged_payload, parts[2]])
    asserts.assert_fails(lambda: jwt.decode(forged, key, algorithms=["HS256"]),
                         "^JWTError: Signature verification failed")
    asserts.assert_fails(lambda: jwt.decode(_tamper(token, 2), key, algorithms=["HS256"]),
                         "^JWTError: Signature verification failed")
    asserts.assert_fails(lambda: jwt.decode(_tamper(token, 0), key, algorithms=["HS256"]), JWT_ERROR)


def Larky_test_alg_none_rejected():
    header = base64url_encode(b'{"alg":"none","typ":"JWT"}').decode("utf-8")
    payload = base64url_encode(b'{"sub":"1234"}').decode("utf-8")
    for unsigned in [header + "." + payload + ".", header + "." + payload + ".c2ln"]:
        asserts.assert_fails(lambda: jwt.decode(unsigned, key), "^JWTError: ")
        asserts.assert_fails(lambda: jwt.decode(unsigned, key, algorithms=["none"]), "^JWTError: ")
        asserts.assert_fails(lambda: jwt.decode(unsigned, key, algorithms=["HS256"]), "^JWTError: ")


def Larky_test_malformed_token():
    for bad in ["", "abc", "a.b", "....", "eyJhbGciOiJIUzI1NiJ9.!!!.sig"]:
        asserts.assert_fails(lambda: jwt.decode(bad, key), "^JWTError: ")


def Larky_test_time_claims_need_now():
    exp_token = jwt.encode({"exp": NOW + 60}, key)
    asserts.assert_fails(lambda: jwt.decode(exp_token, key),
                         "^JWTClaimsError: Cannot verify the Expiration Time claim \\(exp\\)")
    jwt.decode(exp_token, key, options={"verify_exp": False})

    nbf_token = jwt.encode({"nbf": NOW - 60}, key)
    asserts.assert_fails(lambda: jwt.decode(nbf_token, key),
                         "^JWTClaimsError: Cannot verify the Not Before claim \\(nbf\\)")
    jwt.decode(nbf_token, key, options={"verify_nbf": False})

    # require_exp turns verify_exp back on, as upstream does
    asserts.assert_fails(
        lambda: jwt.decode(exp_token, key, options={"verify_exp": False, "require_exp": True}),
        "^JWTClaimsError: Cannot verify the Expiration Time claim",
    )

    # iat is only type-checked, so it needs no clock
    jwt.decode(jwt.encode({"iat": NOW}, key), key)

    asserts.assert_fails(lambda: jwt.decode(exp_token, key, now="1700000000"),
                         "^JWTError: now must be a number")
    jwt.decode(exp_token, key, now=NOW + 0.5)


def Larky_test_exp_nbf_boundaries():
    token = jwt.encode({"exp": NOW, "nbf": NOW}, key)
    # exp == now and nbf == now are accepted, as upstream
    jwt.decode(token, key, now=NOW)
    asserts.assert_fails(lambda: jwt.decode(token, key, now=NOW + 1), "^ExpiredSignatureError: Signature has expired.")
    asserts.assert_fails(lambda: jwt.decode(token, key, now=NOW - 1),
                         "^JWTClaimsError: The token is not yet valid \\(nbf\\)")
    jwt.decode(token, key, now=NOW + 1, options={"leeway": 1})


def Larky_test_error_types():
    asserts.assert_fails(lambda: jwt.decode(jwt.encode({"aud": "x"}, key), key, audience="y"),
                         "^JWTClaimsError: Invalid audience")
    asserts.assert_fails(lambda: jwt.decode(jwt.encode({"iss": "x"}, key), key, issuer="y"),
                         "^JWTClaimsError: Invalid issuer")
    asserts.assert_fails(lambda: jwt.decode(jwt.encode({"sub": "x"}, key), key, subject="y"),
                         "^JWTClaimsError: Invalid subject")
    asserts.assert_fails(lambda: jwt.decode(jwt.encode({"exp": NOW - 1}, key), key, now=NOW),
                         "^ExpiredSignatureError: Signature has expired.")
    asserts.assert_fails(lambda: jwt.decode(jwt.encode(claims, key), key, options={"require_jti": True}),
                         "^JWTError: missing required key \"jti\" among claims")
    asserts.assert_fails(lambda: jwt.decode(jwt.encode(claims, key), key, audience=1),
                         "^JWTError: audience must be a string or None")


def Larky_test_at_hash_algorithms():
    for alg in ["HS256", "HS384", "HS512"]:
        token = jwt.encode({"a": "b"}, key, algorithm=alg, access_token="<ACCESS_TOKEN>")
        jwt.decode(token, key, algorithms=[alg], access_token="<ACCESS_TOKEN>")
        asserts.assert_fails(lambda: jwt.decode(token, key, algorithms=[alg], access_token="other"),
                             "^JWTClaimsError: at_hash claim does not match access_token.")


def _unsigned(alg, payload):
    header = base64url_encode(bytes(json.dumps({"alg": alg}), encoding="utf-8")).decode("utf-8")
    body = base64url_encode(bytes(json.dumps(payload), encoding="utf-8")).decode("utf-8")
    return header + "." + body + ".c2ln"


def Larky_test_at_hash_unknown_algorithm():
    # at_hash of "<ACCESS_TOKEN>" under SHA-256; upstream looks up ALGORITHMS.HASHES[alg]
    # without catching KeyError, so an alg it has no hash for raises KeyError (python-jose 3.5.0).
    at_hash = jwt.get_unverified_claims(
        jwt.encode({}, key, algorithm="HS256", access_token="<ACCESS_TOKEN>"))["at_hash"]
    no_sig = {"verify_signature": False}
    for alg in ["none", "PS256", "foo"]:
        token = _unsigned(alg, {"a": "b", "at_hash": at_hash})
        asserts.assert_fails(lambda: jwt.decode(token, key, options=no_sig, access_token="<ACCESS_TOKEN>"),
                             "^KeyError: '%s'$" % alg)
        asserts.assert_fails(lambda: jwt.encode(claims, key, algorithm=alg, access_token="<ACCESS_TOKEN>"),
                             "^KeyError: '%s'$" % alg)
        # where upstream succeeds or raises a jose error, so does Larky
        asserts.assert_fails(lambda: jwt.decode(token, key, options=no_sig),
                             "^JWTClaimsError: No access_token provided to compare against at_hash claim.$")
        asserts.assert_that(jwt.decode(token, key, options={"verify_signature": False, "verify_at_hash": False},
                                       access_token="<ACCESS_TOKEN>")).is_equal_to({"a": "b", "at_hash": at_hash})
        asserts.assert_that(jwt.decode(_unsigned(alg, {"a": "b"}), key, options=no_sig,
                                       access_token="<ACCESS_TOKEN>")).is_equal_to({"a": "b"})
    token = _unsigned("HS256", {"a": "b", "at_hash": at_hash})
    asserts.assert_that(jwt.decode(token, key, options=no_sig, access_token="<ACCESS_TOKEN>")).is_equal_to(
        {"a": "b", "at_hash": at_hash})


def Larky_test_claim_values_are_not_rewritten():
    payload = {"name": "Doe, John", "note": "a: b"}
    token = jwt.encode(payload, key)
    asserts.assert_that(token).is_equal_to(SEPARATORS_UPSTREAM_TOKEN)
    asserts.assert_that(jwt.get_unverified_claims(token)).is_equal_to(payload)
    asserts.assert_that(jwt.decode(token, key)).is_equal_to(payload)
    hdrs = jwt.get_unverified_headers(jwt.encode(claims, key, headers={"kid": "a, b: c"}))
    asserts.assert_that(hdrs["kid"]).is_equal_to("a, b: c")


def Larky_test_load_paths():
    # load("@vendor//jose", "jwt") and load("@vendor//jose/jwt", "jwt") give the same module
    asserts.assert_that(jwt_submodule.encode(claims, key)).is_equal_to(jwt.encode(claims, key))
    asserts.assert_that(jwt_submodule.decode).is_equal_to(jwt.decode)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_no_alg))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_numeric_key))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_invalid_claims_json))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_invalid_claims))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_non_default_alg))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_non_default_alg_positional_bwcompat))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_no_alg_default_headers))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_non_default_headers))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_deterministic_headers))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_encode))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_decode))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_round_trip_with_different_key_types))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_leeway_is_int))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_iat_not_int))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_nbf_not_int))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_nbf_datetime))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_nbf_with_leeway))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_nbf_in_future))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_nbf_skip))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_exp_not_int))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_exp_datetime))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_exp_with_leeway))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_exp_in_past))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_exp_skip))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_aud_string))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_aud_list))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_aud_list_multiple))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_aud_list_is_strings))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_aud_case_sensitive))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_aud_empty_claim))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_aud_not_string_or_list))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_aud_given_number))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_iss_string))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_iss_list))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_iss_tuple))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_iss_invalid))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_sub_string))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_sub_invalid))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_sub_correct))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_sub_incorrect))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_jti_string))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_jti_invalid))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_at_hash))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_at_hash_invalid))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_at_hash_missing_access_token))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_at_hash_missing_claim))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_at_hash_unable_to_calculate))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_bad_claims))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_unverified_claims_string))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_unverified_claims_list))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_unverified_claims_object))
    _suite.addTest(unittest.FunctionTestCase(TestJWT_test_require))
    # Larky additions
    _suite.addTest(unittest.FunctionTestCase(Larky_test_hs512_matches_upstream))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_hmac_round_trip))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_rs256_round_trip))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_rs256_upstream_token))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_es256_round_trip))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_algorithm_not_allowed))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_tampered_token))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_alg_none_rejected))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_malformed_token))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_time_claims_need_now))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_exp_nbf_boundaries))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_error_types))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_at_hash_algorithms))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_at_hash_unknown_algorithm))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_claim_values_are_not_rewritten))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_load_paths))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
