"""Tests of request_validation.star, the signed-token request check (issue #196).

The keys below are a throwaway 2048-bit RSA pair generated for this test. The
public key is stored in the test vault (DefaultVault, the in-memory vault the
test resources register), as a real route would keep it, and the example
reveals it by alias.
"""

load("@stdlib//hashlib", hashlib="hashlib")
load("@stdlib//json", json="json")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//Crypto/Hash/HMAC", HMAC="HMAC")
load("@vendor//jose", jwt="jwt")
load("@vendor//jose/utils", base64url_encode="base64url_encode")
load("@vgs//http/request", "VGSHttpRequest")
load("@vgs//vault", "vault")
load(
    "request_validation",
    "AUDIENCE",
    "ISSUER",
    "REQUEST_TIME_HEADER",
    "check_request",
    "process",
)

PRIVATE_KEY = "\n".join([
    # https://verygoodsecurity.atlassian.net/browse/SI-203
    # nosemgrep: secrets.misc.generic_private_key.generic_private_key
    "-----BEGIN RSA PRIVATE KEY-----",
    "MIIEogIBAAKCAQEAxUjsYL5oo2PDNTMFiEJgFPhRFzo+OBrN5pFxpJQo8J4bcSwL",
    "hzCTY/GrLm3SJGWkd9S+t4+n+GcVSBzF5qcvG3FqIPxqPWJ2S51K3jKWlkFe3y2y",
    "cvU05r9WFoeZnNtT/fRZevtlLibpIvpFe1HAvqLstigF/MaqxNqGMHRi2jPnts3V",
    "cUsre/6L9UP6WRRaOiQp+K26/vhi1NP5gNSl8jtYEcflA6KHEAHeY3lR83FboF8d",
    "WLfquita6UXDAzHW5K1cQx9VXLL3XKf2uUdVin0cFKC2W8saLfQUOeWewQCE34fZ",
    "joo2KJUw7sF/qY9Az0vwDb0Zc5Jvp9V6G5Y89QIDAQABAoIBAAG5L9tufmKDAhcK",
    "YY99qPI18xtlVlrOHSNY/Omno63qa6ny7Qzax0bVux2WNwI7YxLq6FmZJyn72Is/",
    "ORlZN4ehV4B/Q/xSSl9BugAz5+3bF1WSxB7N9o7XeAyNVbGxlsu+oTN6U7lx/JJi",
    "Jk47+heQO8ohkl/Rand6wzZUJfLrAjjgOgKqk5LL6ZoqVnVeiVqQb3us7lzapqPk",
    "YruBudo5BnY4DQW66eNd2zrC5jMwIOZtZO21EzMl/hUdqq0y3MH2ZyEEcsUXTGKq",
    "LiORrVAmk6NdHDBKcfQl/uZHCi/jS7xsNQK3vuqrNU/p70eGRNVP1L95xJ2Q7Wxp",
    "SePiiEECgYEA8ahCrsCHMoJV9XgkcnNX3w/JJKaOtgL0GIs5sCgKiyT+J0941c0s",
    "9vyWZFXDxRnFjCkO5pBM8zT1HkikUuRwvLr0F4OZS5WwJNfLfJtxEUh94lpo82U4",
    "KOCttx3UyMrE5wkZM1mKy79znAKwVl9hy4Z9g2RlWFYNgv8wSX4l9KECgYEA0P53",
    "+Vn6MguNIfflWC6gNc9z0S1EyZult9KlaOt6IDDJhETf1PTBTcVH4mnEUudi4jjN",
    "e9Lsr21tImuH0albZHhaNK+dYr61QBcNwKgXmtxRJoau6B80tKtszFSoqbmmbsm2",
    "rA3BpbKiJjHv/CUKpKW+E4oIyiH4vnMdV3DB09UCgYAnKrxUEoRXHWTNOkG+Aw/U",
    "6XwSYqRyv5vMdpRNBtBeqQpAWNMe4/hLt7iwXm4yffDUvLrbh7vRF147IE7x2HCl",
    "Ri/Vt9CavZIlBozGj3MN+A/fJgQontkxVukQ6tbYDHVJdOIOvo6OY2Nbf9wQw7jY",
    "Ad5J83WxkZDzEGVFJBsGwQKBgFI22JFh4PeQ7bsgHrt61BF3A3OVIEtDDITsu58z",
    "iv5g0tVSls7aXbSvV+DshIusdIzTlZoRVoSIQndY37wNqGM0qR+yufIfXYPRzdot",
    "BeQoqU/DicjMhdpQ2WLkJtllq+BHP7IdU0FQrVLjMCn7lF1Cyiw7j8ucY5LgC/na",
    "5JTlAoGAGRF3sjwQo9fpaZISqXJhlDZjlEkl2hy6nA4mxCh1E4q5AVcousD9fx/p",
    "/tuOpWwMJmtboEv/y1pef/p2LyJO7nwk+MVeNOrTNgd6hMMiRfFsnRye4fZxHPP/",
    "vmXyhpZNJQXJ5GHQ7frEyle4eDX7nDm+6+mL6ebQbqofTjfDw+A=",
    "-----END RSA PRIVATE KEY-----",
])
PUBLIC_KEY = "\n".join([
    "-----BEGIN PUBLIC KEY-----",
    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxUjsYL5oo2PDNTMFiEJg",
    "FPhRFzo+OBrN5pFxpJQo8J4bcSwLhzCTY/GrLm3SJGWkd9S+t4+n+GcVSBzF5qcv",
    "G3FqIPxqPWJ2S51K3jKWlkFe3y2ycvU05r9WFoeZnNtT/fRZevtlLibpIvpFe1HA",
    "vqLstigF/MaqxNqGMHRi2jPnts3VcUsre/6L9UP6WRRaOiQp+K26/vhi1NP5gNSl",
    "8jtYEcflA6KHEAHeY3lR83FboF8dWLfquita6UXDAzHW5K1cQx9VXLL3XKf2uUdV",
    "in0cFKC2W8saLfQUOeWewQCE34fZjoo2KJUw7sF/qY9Az0vwDb0Zc5Jvp9V6G5Y8",
    "9QIDAQAB",
    "-----END PUBLIC KEY-----",
])

# The time the (assumed) host proxy received the request: 2023-11-14T22:13:20Z.
NOW = 1700000000

KEY_ALIAS = vault.redact(PUBLIC_KEY)


def _claims(**overrides):
    claims = {
        "sub": "user-1234",
        "aud": AUDIENCE,
        "iss": ISSUER,
        "iat": NOW - 10,
        "exp": NOW + 300,
    }
    claims.update(overrides)
    return claims


def _token(claims=None, algorithm="RS256", key=PRIVATE_KEY):
    return jwt.encode(claims or _claims(), key, algorithm=algorithm)


def _request(authorization=None, now=NOW):
    headers = {"Content-Type": "application/json"}
    if authorization != None:
        headers["Authorization"] = authorization
    if now != None:
        headers[REQUEST_TIME_HEADER] = str(now)
    return VGSHttpRequest(
        "https://api.example.com/v1/orders",
        data=b'{"amount": 100}',
        headers=headers,
        method="POST",
    )


def _assert_passes(request):
    result = check_request(request, KEY_ALIAS)
    asserts.assert_that(result).is_equal_to(request)


def _assert_401(request, public_key_alias=KEY_ALIAS):
    result = check_request(request, public_key_alias)
    asserts.assert_that(result.__name__).is_equal_to("ShortCircuitResponse")
    asserts.assert_that(result.response.status_code).is_equal_to(401)
    asserts.assert_that(json.loads(result.response.body.decode("utf-8"))).is_equal_to(
        {"error": "unauthorized"})
    asserts.assert_that(result.response.headers["WWW-Authenticate"]).is_equal_to(
        'Bearer error="invalid_token"')


def _tamper_signature(token):
    header, payload, signature = token.split(".")
    flipped = "A" if signature[0] != "A" else "B"
    return ".".join([header, payload, flipped + signature[1:]])


def test_valid_token_passes():
    request = _request("Bearer " + _token())
    _assert_passes(request)
    # The scheme is case-insensitive.
    _assert_passes(_request("bearer " + _token()))
    # So is extra whitespace around the token.
    _assert_passes(_request("Bearer   " + _token() + " "))
    # The request is returned unchanged, headers and body included.
    result = check_request(request, KEY_ALIAS)
    asserts.assert_that(result.body).is_equal_to(b'{"amount": 100}')


def test_now_argument_overrides_header():
    request = _request("Bearer " + _token(), now=None)
    asserts.assert_that(check_request(request, KEY_ALIAS, now=NOW)).is_equal_to(request)


def test_expired_token():
    # exp is 1 hour ago; leeway is 30 seconds.
    _assert_401(_request("Bearer " + _token(_claims(exp=NOW - 3600))))
    # Within the leeway it still passes.
    _assert_passes(_request("Bearer " + _token(_claims(exp=NOW - 20))))


def test_not_yet_valid_token():
    _assert_401(_request("Bearer " + _token(_claims(nbf=NOW + 3600))))


def test_missing_exp():
    claims = _claims()
    claims.pop("exp")
    _assert_401(_request("Bearer " + _token(claims)))


def test_wrong_audience():
    _assert_401(_request("Bearer " + _token(_claims(aud="https://other.example.com"))))
    claims = _claims()
    claims.pop("aud")
    _assert_401(_request("Bearer " + _token(claims)))


def test_wrong_issuer():
    _assert_401(_request("Bearer " + _token(_claims(iss="https://other-issuer.example.com"))))


def test_tampered_signature():
    _assert_401(_request("Bearer " + _tamper_signature(_token())))


def test_tampered_payload():
    header, _, signature = _token().split(".")
    forged = base64url_encode(bytes(json.dumps(_claims(sub="admin")), encoding="utf-8")).decode("utf-8")
    _assert_401(_request("Bearer " + ".".join([header, forged, signature])))


def test_alg_none():
    header = base64url_encode(b'{"alg":"none","typ":"JWT"}').decode("utf-8")
    payload = base64url_encode(bytes(json.dumps(_claims()), encoding="utf-8")).decode("utf-8")
    _assert_401(_request("Bearer " + header + "." + payload + "."))


def _b64(data):
    return base64url_encode(data).decode("utf-8")


def test_hs256_signed_with_public_key():
    # Algorithm confusion: an HS256 token whose HMAC secret is the issuer's
    # (public) PEM. jwt.encode refuses such a key, so the token is built by
    # hand. Only RS256 is allowed, so it is rejected.
    header = _b64(b'{"alg":"HS256","typ":"JWT"}')
    payload = _b64(bytes(json.dumps(_claims()), encoding="utf-8"))
    signing_input = bytes(header + "." + payload, encoding="utf-8")
    mac = HMAC.new(bytes(PUBLIC_KEY, encoding="utf-8"), signing_input, hashlib.sha256()).digest()
    _assert_401(_request("Bearer " + header + "." + payload + "." + _b64(mac)))


def test_token_signed_by_another_key():
    other = jwt.encode(_claims(), "not-the-issuer", algorithm="HS256")
    _assert_401(_request("Bearer " + other))


def test_missing_or_malformed_header():
    _assert_401(_request(None))
    _assert_401(_request(""))
    _assert_401(_request("Bearer"))
    _assert_401(_request("Bearer "))
    _assert_401(_request("Basic dXNlcjpwYXNz"))
    _assert_401(_request(_token()))
    _assert_401(_request("Bearer " + _token() + " extra"))
    _assert_401(_request("Bearer not-a-jwt"))


def test_missing_or_bad_request_time():
    # Without a trusted time the token's exp cannot be checked: fail closed.
    _assert_401(_request("Bearer " + _token(), now=None))
    request = _request("Bearer " + _token(), now=None)
    request.headers[REQUEST_TIME_HEADER] = "yesterday"
    _assert_401(request)


def test_unknown_key_alias():
    # DefaultVault.reveal returns an unknown alias unchanged; it is not a key.
    _assert_401(_request("Bearer " + _token()), public_key_alias="tok_sandbox_unknown")


def test_process_with_placeholder_alias():
    # process() uses PUBLIC_KEY_ALIAS, which is not in the test vault.
    result = process(_request("Bearer " + _token()), {})
    asserts.assert_that(result.response.status_code).is_equal_to(401)


def _suite():
    _suite = unittest.TestSuite()
    for test in [
        test_valid_token_passes,
        test_now_argument_overrides_header,
        test_expired_token,
        test_not_yet_valid_token,
        test_missing_exp,
        test_wrong_audience,
        test_wrong_issuer,
        test_tampered_signature,
        test_tampered_payload,
        test_alg_none,
        test_hs256_signed_with_public_key,
        test_token_signed_by_another_key,
        test_missing_or_malformed_header,
        test_missing_or_bad_request_time,
        test_unknown_key_alias,
        test_process_with_placeholder_alias,
    ]:
        _suite.addTest(unittest.FunctionTestCase(test))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
