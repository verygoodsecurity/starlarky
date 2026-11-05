# Larky tests (not upstream): the optional 'parameters' and 'publicKey'
# fields of an RFC 5915 ECPrivateKey, alone (SEC1) or inside PKCS#8, as
# _import_private_der handles them since pycryptodome 3.21.0. The keys are
# the variants of ec_der_fixtures.star; OpenSSL 3.6.5 loads the two that
# import here and rejects the other two.

load("@stdlib//binascii", hexlify="hexlify", unhexlify="unhexlify")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//Crypto/PublicKey/ECC", ECC="ECC")
load("./ec_der_fixtures", EC_DER_FIXTURES="EC_DER_FIXTURES",
     P256_D_HEX="P256_D_HEX", P256_POINT_HEX="P256_POINT_HEX")


def _import(name):
    return ECC.import_key(unhexlify(EC_DER_FIXTURES[name]))


def _assert_is_test_key(key):
    asserts.assert_that(key.curve).is_equal_to("NIST P-256")
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(int(key.d)).is_equal_to(int(P256_D_HEX, 16))
    asserts.assert_that(
        hexlify(key.public_key().export_key(format="raw")).decode("ascii")
    ).is_equal_to(P256_POINT_HEX)


def Larky_test_sec1_without_public_key():
    # 'parameters' only: the public key is computed from the private one.
    _assert_is_test_key(_import("sec1_no_public"))


def Larky_test_pkcs8_without_parameters_or_public_key():
    # The curve comes from the PKCS#8 AlgorithmIdentifier.
    _assert_is_test_key(_import("pkcs8_no_params_no_public"))


def Larky_test_public_key_not_on_curve_is_ignored():
    # As 3.21.0: a 'publicKey' that does not decode to a point is skipped
    # and the public key is computed from the private one.
    _assert_is_test_key(_import("sec1_off_curve_public"))


def Larky_test_pkcs8_curve_mismatch():
    # P-384 'parameters' inside a P-256 PKCS#8 key. 3.21.0 imports it as
    # P-256; Larky refuses it, as OpenSSL does.
    asserts.assert_fails(lambda: _import("pkcs8_curve_mismatch"),
                         ".*ValueError: Not an ECC DER key")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(Larky_test_sec1_without_public_key))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_pkcs8_without_parameters_or_public_key))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_public_key_not_on_curve_is_ignored))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_pkcs8_curve_mismatch))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
