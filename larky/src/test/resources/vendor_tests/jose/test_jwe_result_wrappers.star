# Larky tests: jwe.encrypt and jwe.decrypt called the way scripts wrap
# fallible calls, inside option/result's safe() and inside Result.try_.
# safe() cannot be nested (its wrapper reports "called recursively"), so
# jose must not call safe() itself.
#
# The Test_*_error tests and the Test_decrypt_* tests after them reach the
# error branch of each Result.Ok(x).map(f) in jwe.star and AESKey.encrypt,
# and check that the failure is the same inside safe().

load("@stdlib//codecs", codecs="codecs")
load("@stdlib//json", json="json")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//jose/jwe", jwe="jwe")
load("@vendor//jose/jwk", jwk="jwk")
load("@vendor//jose/utils", base64url_encode="base64url_encode")
load("@vendor//option/result", Result="Result", safe="safe")
load("./ecdh_es_fixtures", KEYS="KEYS")

PLAINTEXT = b"wrapped call"

ERR_TAG = "JWEError: Invalid JWE Auth Tag"
ERR_MAC = "ValueError: MAC check failed"

# (alg, enc, key, wrong key, error for a tampered tag). The alg values reach
# each key management path of jwe.star: AES key wrap (key.unwrap on decrypt,
# _get_cek on encrypt), direct encryption, and ECDH-ES (+A*KW).
CASES = [
    ("A128KW", "A128GCM", b"0123456789abcdef", b"fedcba9876543210", ERR_MAC),
    ("A256KW", "A256CBC-HS512", b"0123456789abcdef0123456789abcdef",
     b"fedcba9876543210fedcba9876543210", ERR_TAG),
    ("dir", "A256GCM", b"0123456789abcdef0123456789abcdef",
     b"fedcba9876543210fedcba9876543210", ERR_MAC),
    ("ECDH-ES", "A256GCM", KEYS["x25519_a"], KEYS["x25519_b"], ERR_TAG),
    ("ECDH-ES+A128KW", "A128CBC-HS256", KEYS["p256"], KEYS["x25519_a"], None),
]


def _keys(key, wrong):
    """(encryption key, decryption key, wrong decryption key)."""
    if type(key) == "dict":
        return key["public_pem"], key["private_pem"], wrong["private_pem"]
    return key, key, wrong


def _tamper(token):
    parts = token.decode("utf-8").split(".")
    tag = parts[4]
    parts[4] = ("B" if tag[0] == "A" else "A") + tag[1:]
    return ".".join(parts)


def _try(fn):
    return Result.try_(fn).except_(lambda e: e).build()


def _direct_error(fn):
    """The message of the failure fn raises, caught without safe()."""
    res = Result.Ok(fn).map(lambda f: f())
    asserts.assert_that(res.is_err).is_true()
    return res._val


def Test_inside_safe():
    for alg, enc, key, wrong, tag_err in CASES:
        enc_key, dec_key, wrong_key = _keys(key, wrong)
        res = safe(jwe.encrypt)(PLAINTEXT, enc_key, encryption=enc, algorithm=alg)
        asserts.assert_that(res.is_ok).is_true()
        token = res.unwrap()
        res = safe(jwe.decrypt)(token, dec_key)
        asserts.assert_that(res.is_ok).is_true()
        asserts.assert_that(res.unwrap()).is_equal_to(PLAINTEXT)

        for bad_token, bad_key in ((_tamper(token), dec_key), (token, wrong_key)):
            res = safe(jwe.decrypt)(bad_token, bad_key)
            asserts.assert_that(res.is_err).is_true()
            # The same failure as an unwrapped call.
            asserts.assert_that(res._val).is_equal_to(
                _direct_error(lambda: jwe.decrypt(bad_token, bad_key)))
        if tag_err:
            asserts.assert_fails(lambda: safe(jwe.decrypt)(_tamper(token), dec_key).unwrap(),
                                 "^%s$" % tag_err)


def Test_inside_result_try():
    for alg, enc, key, wrong, tag_err in CASES:
        enc_key, dec_key, wrong_key = _keys(key, wrong)
        res = _try(lambda: jwe.encrypt(PLAINTEXT, enc_key, encryption=enc, algorithm=alg))
        asserts.assert_that(res.is_ok).is_true()
        token = res.unwrap()
        res = _try(lambda: jwe.decrypt(token, dec_key))
        asserts.assert_that(res.is_ok).is_true()
        asserts.assert_that(res.unwrap()).is_equal_to(PLAINTEXT)

        for bad_token, bad_key in ((_tamper(token), dec_key), (token, wrong_key)):
            handled = []
            res = Result.try_(lambda: jwe.decrypt(bad_token, bad_key))\
                .except_(lambda e: handled.append(e) or e)\
                .build()
            asserts.assert_that(res.is_err).is_true()
            asserts.assert_that(handled).is_equal_to(
                [_direct_error(lambda: jwe.decrypt(bad_token, bad_key))])
        if tag_err:
            res = _try(lambda: jwe.decrypt(_tamper(token), dec_key))
            asserts.assert_that(res._val).is_equal_to(tag_err)


def _assert_fails_alike(fn, error):
    """fn fails with error, and inside safe() it is an Err with the same message."""
    asserts.assert_fails(fn, error)
    res = safe(fn)()
    asserts.assert_that(res.is_err).is_true()
    asserts.assert_that(res._val).is_equal_to(_direct_error(fn))


def _with_header(token, header):
    """token with its protected header replaced by header."""
    encoded = codecs.decode(base64url_encode(codecs.encode(json.dumps(header), encoding="utf-8")),
                            encoding="ascii")
    return ".".join([encoded] + token.decode("utf-8").split(".")[1:])


def Test_decrypt_unwrap_error():
    # key.unwrap fails with a JWEError (here, the "iv" an A128GCMKW header
    # needs), which decrypt raises as is.
    key = b"0123456789abcdef"
    token = jwe.encrypt(PLAINTEXT, key, encryption="A128GCM", algorithm="A128KW")
    bad = _with_header(token, {"alg": "A128GCMKW", "enc": "A128GCM"})
    _assert_fails_alike(lambda: jwe.decrypt(bad, key), '^JWEError: Missing "iv"$')
    bad = _with_header(token, {"alg": "A128GCMKW", "enc": "A128GCM", "iv": "AAAAAAAAAAAAAAAA"})
    _assert_fails_alike(lambda: jwe.decrypt(bad, key), '^JWEError: Missing "tag"$')


def Test_decrypt_segment_error():
    # A segment that is not base64url ("A" is too short) fails with the
    # message for that segment.
    key = b"0123456789abcdef"
    token = jwe.encrypt(PLAINTEXT, key, encryption="A128GCM", algorithm="A128KW")
    for index, error in ((1, "JWEParseError: Invalid encrypted key"),
                         (2, "JWEParseError: Invalid IV"),
                         (3, "JWEParseError: Invalid cyphertext"),
                         (4, "JWEParseError: Invalid auth tag")):
        parts = token.decode("utf-8").split(".")
        parts[index] = "A"
        bad = ".".join(parts)
        _assert_fails_alike(lambda: jwe.decrypt(bad, key), "^%s$" % error)


def Test_encrypt_cek_error():
    # _get_cek fails. An HMAC key cannot wrap a CEK, which python-jose
    # reports as not implemented; other errors are raised as they are.
    _assert_fails_alike(
        lambda: jwe.encrypt(PLAINTEXT, b"0123456789abcdef", encryption="A128GCM", algorithm="HS256"),
        "^JWEError: alg HS256 is not implemented$")
    _assert_fails_alike(
        lambda: jwe.encrypt(PLAINTEXT, b"0123456789abcdef", encryption="A128GCM", algorithm="A256KW"),
        "^JWKError: Key must be 256 bits for alg A256KW$")


def Test_decrypt_alg_not_implemented():
    key = b"0123456789abcdef"
    token = jwe.encrypt(PLAINTEXT, key, encryption="A128GCM", algorithm="A128KW")
    bad = _with_header(token, {"alg": "HS256", "enc": "A128GCM"})
    _assert_fails_alike(lambda: jwe.decrypt(bad, key), "^JWEError: alg HS256 is not implemented$")


def Test_decrypt_unwrap_failure_uses_random_cek():
    # RFC 7516, Section 11.5: when the encrypted key does not unwrap,
    # decrypt goes on with a random CEK, so a wrong key, a truncated
    # encrypted key and a changed one all fail like a changed tag.
    for alg, enc, key, wrong, tag_err in CASES:
        if alg not in ("A128KW", "A256KW"):
            continue
        token = jwe.encrypt(PLAINTEXT, key, encryption=enc, algorithm=alg)
        parts = token.decode("utf-8").split(".")
        short = list(parts)
        short[1] = "AAAAAAAAAAA"
        changed = list(parts)
        changed[1] = ("B" if parts[1][0] == "A" else "A") + parts[1][1:]
        for bad_token, bad_key in ((token, wrong), (".".join(short), key), (".".join(changed), key)):
            _assert_fails_alike(lambda: jwe.decrypt(bad_token, bad_key), "^%s$" % tag_err)


def Test_aes_key_encrypt_error():
    # AESKey.encrypt fails: GCM needs bytes for the additional data.
    key = jwk.construct(b"0123456789abcdef", "A128GCM")
    _assert_fails_alike(lambda: key.encrypt(PLAINTEXT),
                        "^TypeError: assoc_data is not byte-like$")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(Test_inside_safe))
    _suite.addTest(unittest.FunctionTestCase(Test_inside_result_try))
    _suite.addTest(unittest.FunctionTestCase(Test_decrypt_unwrap_error))
    _suite.addTest(unittest.FunctionTestCase(Test_decrypt_segment_error))
    _suite.addTest(unittest.FunctionTestCase(Test_encrypt_cek_error))
    _suite.addTest(unittest.FunctionTestCase(Test_decrypt_alg_not_implemented))
    _suite.addTest(unittest.FunctionTestCase(Test_decrypt_unwrap_failure_uses_random_cek))
    _suite.addTest(unittest.FunctionTestCase(Test_aes_key_encrypt_error))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
