# Larky tests: jose behaviour that differed from python-jose at the commit it
# is ported from (8572088b). Expected values come from parity_fixtures.star,
# made by larky/src/test/tools/jose_parity/gen_fixtures.py.

load("@stdlib//codecs", codecs="codecs")
load("@stdlib//json", json="json")
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//zlib", zlib="zlib")
load("@vendor//asserts", asserts="asserts")
load("@vendor//jose/jwe", jwe="jwe")
load("@vendor//jose/jwk", jwk="jwk")
load("@vendor//jose/jws", jws="jws")
load("@vendor//jose/utils", base64url_decode="base64url_decode", base64url_encode="base64url_encode")
load("./parity_fixtures",
     "PLAINTEXT", "K16", "K24", "K32", "RSA_PRIVATE_PEM", "RSA_PUBLIC_PEM",
     "PYJOSE_ZIP_TOKEN", "JOSERFC_ZIP_TOKEN", "JOSERFC_GCMKW_TOKENS",
     "PYJOSE_HS256_JWK_TOKEN", "PYJOSE_RSA_PUBLIC_PKCS1")

GCMKW_KEYS = {"A128GCMKW": K16, "A192GCMKW": K24, "A256GCMKW": K32}


def _b64(data):
    return codecs.decode(base64url_encode(data), encoding="ascii")


def _unb64(text):
    return base64url_decode(codecs.encode(text, encoding="ascii"))


def _segments(token):
    return codecs.decode(token, encoding="ascii").split(".") if type(token) == "bytes" else token.split(".")


def _with_segment(token, index, value):
    parts = _segments(token)
    parts[index] = value
    return ".".join(parts)


def Test_get_unverified_header():
    token = jwe.encrypt(b"x", K16, algorithm="dir", encryption="A128GCM", cty="text/plain")
    asserts.assert_that(jwe.get_unverified_header(token)).is_equal_to(
        {"alg": "dir", "enc": "A128GCM", "cty": "text/plain"})
    asserts.assert_that(jwe.get_unverified_header(PYJOSE_ZIP_TOKEN)).is_equal_to(
        {"alg": "dir", "enc": "A128GCM", "zip": "DEF"})
    asserts.assert_fails(lambda: jwe.get_unverified_header(_b64(b"[1]") + ".a.b.c.d"),
                         ".*?JWEParseError: Invalid header string: must be a json object")


def Test_zip_def():
    # python-jose's zlib data and joserfc's raw DEFLATE both decompress.
    asserts.assert_that(jwe.decrypt(PYJOSE_ZIP_TOKEN, K16)).is_equal_to(PLAINTEXT)
    asserts.assert_that(jwe.decrypt(JOSERFC_ZIP_TOKEN, K16)).is_equal_to(PLAINTEXT)
    for alg, key in [("dir", K16), ("A128KW", K16), ("A128GCMKW", K16)]:
        token = jwe.encrypt(PLAINTEXT, key, algorithm=alg, encryption="A128GCM", zip="DEF")
        asserts.assert_that(jwe.get_unverified_header(token)["zip"]).is_equal_to("DEF")
        # Encrypted as python-jose does: zlib data, shorter than the plaintext.
        asserts.assert_that(len(_unb64(_segments(token)[3]))).is_less_than(len(PLAINTEXT))
        asserts.assert_that(jwe.decrypt(token, key)).is_equal_to(PLAINTEXT)


def Test_zip_errors():
    asserts.assert_fails(
        lambda: jwe.encrypt(b"x", K16, algorithm="dir", encryption="A128GCM", zip="XYZ"),
        ".*?NotImplementedError: ZIP XYZ is not supported!")
    token = jwe.encrypt(b"x", K16, algorithm="dir", encryption="A128GCM",
                        extra_headers={"zip": "XYZ"})
    asserts.assert_fails(lambda: jwe.decrypt(token, K16),
                         ".*?NotImplementedError: ZIP XYZ is not supported!")
    # More than 250 KiB after decompression is refused (python-jose 3.5.0).
    bomb = jwe.encrypt(b"\0" * (250 * 1024 + 1), K16, algorithm="dir", encryption="A128GCM", zip="DEF")
    asserts.assert_fails(lambda: jwe.decrypt(bomb, K16),
                         ".*?JWEError: Decompressed JWE string exceeds 256000 bytes")
    exact = jwe.encrypt(b"\0" * (250 * 1024), K16, algorithm="dir", encryption="A128GCM", zip="DEF")
    asserts.assert_that(len(jwe.decrypt(exact, K16))).is_equal_to(250 * 1024)


def Test_gcmkw_from_joserfc():
    for alg, _, token in JOSERFC_GCMKW_TOKENS:
        asserts.assert_that(jwe.decrypt(token, GCMKW_KEYS[alg])).is_equal_to(PLAINTEXT)


def Test_gcmkw_round_trip():
    for alg, key in GCMKW_KEYS.items():
        for enc in ["A128GCM", "A256CBC-HS512"]:
            token = jwe.encrypt(PLAINTEXT, key, algorithm=alg, encryption=enc, kid="k1")
            header = jwe.get_unverified_header(token)
            asserts.assert_that(sorted(header.keys())).is_equal_to(["alg", "enc", "iv", "kid", "tag"])
            asserts.assert_that(len(_unb64(header["iv"]))).is_equal_to(12)
            asserts.assert_that(len(_unb64(header["tag"]))).is_equal_to(16)
            asserts.assert_that(jwe.decrypt(token, key)).is_equal_to(PLAINTEXT)


def Test_gcmkw_errors():
    token = jwe.encrypt(PLAINTEXT, K16, algorithm="A128GCMKW", encryption="A128GCM")
    # A changed key wrap tag is refused; jwe goes on with a random CEK, so
    # the content tag check fails.
    header = jwe.get_unverified_header(token)
    header["tag"] = _b64(b"\0" * 16)
    forged = _with_segment(token, 0, _b64(codecs.encode(json.dumps(header), encoding="utf-8")))
    asserts.assert_fails(lambda: jwe.decrypt(forged, K16), ".*?MAC check failed")
    asserts.assert_fails(
        lambda: jwe.encrypt(b"x", K16, algorithm="A128GCMKW", encryption="A128GCM",
                            extra_headers={"iv": "AAAA"}),
        ".*?JWEError: A\\*GCMKW generates the iv and tag headers")
    asserts.assert_fails(lambda: jwe.encrypt(b"x", K24, algorithm="A128GCMKW", encryption="A128GCM"),
                         ".*?JWKError: Key must be 128 bits for alg A128GCMKW")


def Test_hmac_jwk():
    oct_jwk = {"kty": "oct", "k": _b64(b"secret")}
    asserts.assert_that(jws.sign(b"payload", oct_jwk, algorithm="HS256")).is_equal_to(PYJOSE_HS256_JWK_TOKEN)
    asserts.assert_that(jws.verify(PYJOSE_HS256_JWK_TOKEN, oct_jwk, "HS256")).is_equal_to(b"payload")
    # The algorithm can come from the JWK.
    key = jwk.construct({"kty": "oct", "k": _b64(b"s"), "alg": "HS256"})
    asserts.assert_that(key.to_dict()).is_equal_to({"alg": "HS256", "k": "cw", "kty": "oct"})
    asserts.assert_fails(lambda: jwk.construct({"kty": "RSA", "k": "cw"}, "HS256"),
                         ".*?JWKError: Incorrect key type. Expected: 'oct', Received: RSA")


def Test_rsa_jwk_errors():
    d = jwk.construct(RSA_PRIVATE_PEM, "RS256").to_dict()
    wrong_kty = dict(d)
    wrong_kty["kty"] = "EC"
    asserts.assert_fails(lambda: jwk.construct(wrong_kty, "RS256"),
                         ".*?JWKError: Incorrect key type. Expected: 'RSA', Received: EC")
    incomplete = dict(d)
    incomplete.pop("qi")
    asserts.assert_fails(lambda: jwk.construct(incomplete, "RS256"),
                         ".*?JWKError: Precomputed private key parameters are incomplete.")
    # A complete JWK still works.
    token = jws.sign(b"payload", d, algorithm="RS256")
    asserts.assert_that(jws.verify(token, RSA_PUBLIC_PEM, "RS256")).is_equal_to(b"payload")


def Test_rsa_to_pem():
    public = jwk.construct(RSA_PUBLIC_PEM, "RS256")
    asserts.assert_that(codecs.decode(public.to_pem(pem_format="PKCS1"), encoding="ascii").strip()).is_equal_to(
        PYJOSE_RSA_PUBLIC_PKCS1.strip())
    asserts.assert_that(codecs.decode(public.to_pem(), encoding="ascii").strip()).is_equal_to(
        RSA_PUBLIC_PEM.strip())
    asserts.assert_fails(lambda: public.to_pem(pem_format="X"),
                         ".*?ValueError: Invalid pem format specified: \"X\"")


def Test_aes_key_size():
    asserts.assert_fails(lambda: jwk.construct(b"short", "A128KW"),
                         ".*?JWKError: Key must be 128 bits for alg A128KW")
    asserts.assert_fails(lambda: jwk.construct(K16, "A192KW"),
                         ".*?JWKError: Key must be 192 bits for alg A192KW")
    asserts.assert_fails(lambda: jwk.construct(K16, "A256GCM"),
                         ".*?JWKError: Key must be 256 bits for alg A256GCM")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(Test_get_unverified_header))
    _suite.addTest(unittest.FunctionTestCase(Test_zip_def))
    _suite.addTest(unittest.FunctionTestCase(Test_zip_errors))
    _suite.addTest(unittest.FunctionTestCase(Test_gcmkw_from_joserfc))
    _suite.addTest(unittest.FunctionTestCase(Test_gcmkw_round_trip))
    _suite.addTest(unittest.FunctionTestCase(Test_gcmkw_errors))
    _suite.addTest(unittest.FunctionTestCase(Test_hmac_jwk))
    _suite.addTest(unittest.FunctionTestCase(Test_rsa_jwk_errors))
    _suite.addTest(unittest.FunctionTestCase(Test_rsa_to_pem))
    _suite.addTest(unittest.FunctionTestCase(Test_aes_key_size))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
