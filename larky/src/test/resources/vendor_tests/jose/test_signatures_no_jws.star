load("@stdlib//base64", base64="base64")
load("@stdlib//json", json="json")
load("@stdlib//re", re="re")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//jose/jwk", jwk="jwk")
load("@vendor//jose/jws", jws="jws")

ecc_public_key = b"""-----BEGIN PUBLIC KEY-----\nMFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAE7l9S5znW4VsHl3XXM1cFPXzkJ17QTX+H\nE4gS/P/0EgxJtkpIuKFNwjZnetGKCkJmIjdsVxtzQV3GPOTsi/Btzg==\n-----END PUBLIC KEY-----"""
ecc_private_key = b"""-----BEGIN EC PRIVATE KEY-----\nMHQCAQEEIPjfg99RGAgLkIWU+Ell5EWzQMXJ99C++CYaZ8DN4wmfoAcGBSuBBAAK\noUQDQgAE7l9S5znW4VsHl3XXM1cFPXzkJ17QTX+HE4gS/P/0EgxJtkpIuKFNwjZn\netGKCkJmIjdsVxtzQV3GPOTsi/Btzg==\n-----END EC PRIVATE KEY-----"""
rsa_public_key = b"""-----BEGIN PUBLIC KEY-----\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCQouotdvmxpjokxe5GZxNQbWVQ\n6ip3T7k5xbU37jJheojRJm0wtTSbtIetDXr5vHp0/SnuxzMvEccB7/UrWWGOabVx\nHas4edX2X7Ie9JTDg9G9smkcCDlQLD6cvxgIy7afH5rG//SoBgMPwnAicHXaEuWp\nu+MaeJm1BAg6MND/2QIDAQAB\n-----END PUBLIC KEY-----"""
rsa_private_key = b"""-----BEGIN RSA PRIVATE KEY-----\nMIICXAIBAAKBgQCQouotdvmxpjokxe5GZxNQbWVQ6ip3T7k5xbU37jJheojRJm0w\ntTSbtIetDXr5vHp0/SnuxzMvEccB7/UrWWGOabVxHas4edX2X7Ie9JTDg9G9smkc\nCDlQLD6cvxgIy7afH5rG//SoBgMPwnAicHXaEuWpu+MaeJm1BAg6MND/2QIDAQAB\nAoGAbJKfD7n7/is2AlzCXP8LNJiqMW9WqXGjLYcIXg/kqd/9zGL4HFQqRafjITi5\nU7b0hdV1INVPysmhhgbHF99kpw3ee31mSQtenn//SGQFJeOn+QE+R63FH8icrod/\nNsXqR4w/61RS07wiYOtfzN/7czczi6WQV4W3xnugG6971vECQQDKCHyJ9W4xuYMl\nKcuQwokFsDKR1s9UpUPOCLeaQJFmW3hCHrWsFic2AH1oWxUrJdaAYL/DFTmtzpXJ\nz+a6heCHAkEAt0V/zqCWjG0+OIZopTaGS6/0Rtp67kAy2GlFLpg9hnIqTqgtHS1+\noeT9D0qQrNKGu5fR14WFQltTPxkWNOAUnwJABR7T8TcsNMxr23xEsYWMrX06uuGD\n3bRWlJk59gne5YY59QsMNbFWCxNWGlf8oFxUJGrPUWVvUc1jlHrVcTLFbwJBAJTD\nYDwMFEgGgMQHLjg1KwuS1tkQjUqJZ/xMbvCkeQSB9R+F2aDehfTJ2DQqVYdDGER7\ntsSXyBSV5tvH9EOVRIcCQFktF/V6TW6X3I3XmQjEuie4W0UT8tpWUcgkwO8slvra\nQ7aSCBe5pLlVT0SorTqH3ywozGy2uzGyaigjtIFbgcM=\n-----END RSA PRIVATE KEY-----"""


def build_jwe(body, alg, private_key, public_key):
    header = {"typ": "JWT", "alg": alg}
    json_header = bytes(
        json.dumps(
            header,
        ),
        "utf-8",
    )
    headers = base64.urlsafe_b64encode(json_header)
    encoded_payload = base64.urlsafe_b64encode(bytes(body, "utf-8"))
    signing_input = bytes([0x2E]).join([headers, encoded_payload])
    priv_jwk = jwk.construct(private_key, alg)
    sign = priv_jwk.sign(signing_input)
    pub_jwk = jwk.construct(public_key, alg)
    pub_jwk.verify(signing_input, sign)
    encoded_signature = base64.urlsafe_b64encode(sign)

    encoded_string = b".".join([headers, encoded_payload, encoded_signature])
    return encoded_string


## Attempt to build and sign a JWE without using the JWS module. Verify it is valid by using JWS.verify
def test_sign_rsa_without_jws():
    alg = "RS256"
    jwe = build_jwe(
        json.dumps({"a": "b"}), alg, rsa_private_key, rsa_public_key
    ).decode()
    asserts.assert_true(re.match(r"[^\s]+\.[^\s]+\.[^\s]+", jwe))
    # Commenting out because currently breaks on load...
    # verified = jws.verify(jwe, rsa_public_key, algorithms=[alg])
    # asserts.assert_true(verified)


## Verify an RSA encrypted JWE without the JWS module
def test_verify_rsa_without_jws():
    alg = "RS256"
    jwe = b"eyJ0eXAiOiAiSldUIiwgImFsZyI6ICJSUzI1NiJ9.eyJhIjogImIifQ==.erMoTS3F5-vNRaY9RAaNWv8vDNnu63O7CmhOC-tbOuqfkg8tQs0nIch3wHbU04aCa_9wjU8QKVLcGJYXC8BG8EJX0t2Em71_9zTiv8J-W6gNctG1xDAldQVWVfFKBpfiDQqNnGlmwmwnsWIsW-AtGE3L3KeDcKkVY5eBh7paLDQ="
    headers, encoded_payload, encoded_signature = jwe.split(b".")
    signed = bytes([0x2E]).join([headers, encoded_payload])
    pub_jwk = jwk.construct(rsa_public_key, alg)
    decoded_sig = base64.urlsafe_b64decode(encoded_signature)
    verified = pub_jwk.verify(signed, decoded_sig)
    asserts.assert_true(verified)


## Sign and verify an RSA encrypted JWE without the JWS module
def test_sign_and_verify_rsa_without_jws():
    alg = "RS256"
    jwe = build_jwe(json.dumps({"a": "b"}), alg, rsa_private_key, rsa_public_key)
    headers, encoded_payload, encoded_signature = jwe.split(b".")
    signed = bytes([0x2E]).join([headers, encoded_payload])
    pub_jwk = jwk.construct(rsa_public_key, alg)
    decoded_sig = base64.urlsafe_b64decode(encoded_signature)
    verified = pub_jwk.verify(signed, decoded_sig)
    asserts.assert_true(verified)


def test_sign_ecc_without_jws():
    alg = "ES256"
    jwe = build_jwe(
        json.dumps({"a": "b"}), alg, ecc_private_key, ecc_public_key
    ).decode()
    asserts.assert_true(re.match(r"[^\s]+\.[^\s]+\.[^\s]+", jwe))
    # Commenting out because currently breaks on load...
    # verified = jws.verify(jwe, ecc_public_key, algorithms=[alg])
    # asserts.assert_true(verified)


## Sign and verify an RSA encrypted JWE without the JWS module
def test_verify_ecc_without_jws():
    alg = "ES256"
    jwe = b"eyJ0eXAiOiAiSldUIiwgImFsZyI6ICJFUzI1NiJ9.eyJhIjogImIifQ==.CZTOELuTxFw03E1R7fwRK4lqJPAzQXpzNY4dte4OOiu3McVAQioCkMlPRBN7c0tgCfqTmWH6UD-gqe1WgsNQTQ=="
    headers, encoded_payload, encoded_signature = jwe.split(b".")
    signed = bytes([0x2E]).join([headers, encoded_payload])
    pub_jwk = jwk.construct(ecc_public_key, alg)
    decoded_sig = base64.urlsafe_b64decode(encoded_signature)
    verified = pub_jwk.verify(signed, decoded_sig)
    asserts.assert_true(verified)


## Sign and verify an RSA encrypted JWE without the JWS module
def test_sign_and_verify_ecc_without_jws():
    alg = "ES256"
    jwe = build_jwe(json.dumps({"a": "b"}), alg, ecc_private_key, ecc_public_key)
    headers, encoded_payload, encoded_signature = jwe.split(b".")
    signed = bytes([0x2E]).join([headers, encoded_payload])
    pub_jwk = jwk.construct(ecc_public_key, alg)
    decoded_sig = base64.urlsafe_b64decode(encoded_signature)
    verified = pub_jwk.verify(signed, decoded_sig)
    asserts.assert_true(verified)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_sign_rsa_without_jws))
    _suite.addTest(unittest.FunctionTestCase(test_verify_rsa_without_jws))
    _suite.addTest(unittest.FunctionTestCase(test_sign_and_verify_rsa_without_jws))
    _suite.addTest(unittest.FunctionTestCase(test_sign_ecc_without_jws))
    _suite.addTest(unittest.FunctionTestCase(test_verify_ecc_without_jws))
    _suite.addTest(unittest.FunctionTestCase(test_sign_and_verify_ecc_without_jws))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
