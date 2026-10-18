# Larky tests: jwe.encrypt and jwe.decrypt called the way scripts wrap
# fallible calls, inside option/result's safe() and inside Result.try_.
# safe() cannot be nested (its wrapper reports "called recursively"), so
# jose must not call safe() itself.

load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//jose/jwe", jwe="jwe")
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


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(Test_inside_safe))
    _suite.addTest(unittest.FunctionTestCase(Test_inside_result_try))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
