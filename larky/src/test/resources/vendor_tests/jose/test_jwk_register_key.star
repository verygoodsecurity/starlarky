# Larky tests: jwk.register_key. python-jose adds the key class to
# module-level registries; Larky keeps it for the evaluation that registered
# it (EvaluationDictTest checks that it does not reach the next one).

load("@stdlib//codecs", codecs="codecs")
load("@stdlib//json", json="json")
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//jose/jwe", jwe="jwe")
load("@vendor//jose/jwk", jwk="jwk")
load("@vendor//jose/utils", base64url_encode="base64url_encode")


def NoUnwrapKey(key_data, algorithm):
    """A key that cannot unwrap, like python-jose's Key."""
    def unwrap(wrapped_key, headers=None, enc_alg=None):
        fail("NotImplementedError: NoUnwrapKey")
    return larky.struct(algorithm=algorithm, key_data=key_data, unwrap=unwrap)


def _token(header):
    encoded = codecs.decode(base64url_encode(codecs.encode(json.dumps(header), encoding="utf-8")),
                            encoding="ascii")
    return encoded + ".AAAA.AAAAAAAAAAAAAAAA.AAAA.AAAAAAAAAAAAAAAAAAAAAA"


def Test_unregistered():
    asserts.assert_that(jwk.is_supported("X-NOUNWRAP")).is_false()
    asserts.assert_that(jwk.get_key("X-NOUNWRAP")).is_none()
    asserts.assert_fails(lambda: jwe.decrypt(_token({"alg": "X-NOUNWRAP", "enc": "A128GCM"}), b"k"),
                         "^JWEError: Algorithm not supported\\.$")


def Test_register_key():
    asserts.assert_that(jwk.register_key("X-NOUNWRAP", NoUnwrapKey)).is_true()
    asserts.assert_that(jwk.is_supported("X-NOUNWRAP")).is_true()
    asserts.assert_that(jwk.get_key("X-NOUNWRAP")).is_equal_to(NoUnwrapKey)
    key = jwk.construct(b"k", "X-NOUNWRAP")
    asserts.assert_that(key.algorithm).is_equal_to("X-NOUNWRAP")
    # jwe accepts the algorithm, and reports the key's NotImplementedError
    # as python-jose does.
    asserts.assert_fails(lambda: jwe.decrypt(_token({"alg": "X-NOUNWRAP", "enc": "A128GCM"}), b"k"),
                         "^JWEError: alg X-NOUNWRAP is not implemented$")
    # A registered class replaces the built-in one for its algorithm.
    jwk.register_key("HS256", NoUnwrapKey)
    asserts.assert_that(jwk.construct(b"k", "HS256").key_data).is_equal_to(b"k")


def Test_register_key_errors():
    asserts.assert_fails(lambda: jwk.register_key("X", "not a class"),
                         "^TypeError: Key class is not callable$")
    asserts.assert_fails(lambda: jwk.register_key(["X"], NoUnwrapKey),
                         "^TypeError: algorithm must be a string$")
    asserts.assert_that(jwk.is_supported(["HS256"])).is_false()


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(Test_unregistered))
    _suite.addTest(unittest.FunctionTestCase(Test_register_key))
    _suite.addTest(unittest.FunctionTestCase(Test_register_key_errors))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
