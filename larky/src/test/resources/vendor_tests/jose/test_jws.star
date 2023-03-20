load("@stdlib//re", re="re")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//jose/jws", jws="jws")


ecc_public_key = b"-----BEGIN PUBLIC KEY-----\nMFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAE7l9S5znW4VsHl3XXM1cFPXzkJ17QTX+H\nE4gS/P/0EgxJtkpIuKFNwjZnetGKCkJmIjdsVxtzQV3GPOTsi/Btzg==\n-----END PUBLIC KEY-----"
ecc_private_key = b"-----BEGIN EC PRIVATE KEY-----\nMHQCAQEEIPjfg99RGAgLkIWU+Ell5EWzQMXJ99C++CYaZ8DN4wmfoAcGBSuBBAAK\noUQDQgAE7l9S5znW4VsHl3XXM1cFPXzkJ17QTX+HE4gS/P/0EgxJtkpIuKFNwjZn\netGKCkJmIjdsVxtzQV3GPOTsi/Btzg==\n-----END EC PRIVATE KEY-----"
rsa_public_key = b"-----BEGIN PUBLIC KEY-----\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCQouotdvmxpjokxe5GZxNQbWVQ\n6ip3T7k5xbU37jJheojRJm0wtTSbtIetDXr5vHp0/SnuxzMvEccB7/UrWWGOabVx\nHas4edX2X7Ie9JTDg9G9smkcCDlQLD6cvxgIy7afH5rG//SoBgMPwnAicHXaEuWp\nu+MaeJm1BAg6MND/2QIDAQAB\n-----END PUBLIC KEY-----"
rsa_private_key = b"-----BEGIN RSA PRIVATE KEY-----\nMIICXAIBAAKBgQCQouotdvmxpjokxe5GZxNQbWVQ6ip3T7k5xbU37jJheojRJm0w\ntTSbtIetDXr5vHp0/SnuxzMvEccB7/UrWWGOabVxHas4edX2X7Ie9JTDg9G9smkc\nCDlQLD6cvxgIy7afH5rG//SoBgMPwnAicHXaEuWpu+MaeJm1BAg6MND/2QIDAQAB\nAoGAbJKfD7n7/is2AlzCXP8LNJiqMW9WqXGjLYcIXg/kqd/9zGL4HFQqRafjITi5\nU7b0hdV1INVPysmhhgbHF99kpw3ee31mSQtenn//SGQFJeOn+QE+R63FH8icrod/\nNsXqR4w/61RS07wiYOtfzN/7czczi6WQV4W3xnugG6971vECQQDKCHyJ9W4xuYMl\nKcuQwokFsDKR1s9UpUPOCLeaQJFmW3hCHrWsFic2AH1oWxUrJdaAYL/DFTmtzpXJ\nz+a6heCHAkEAt0V/zqCWjG0+OIZopTaGS6/0Rtp67kAy2GlFLpg9hnIqTqgtHS1+\noeT9D0qQrNKGu5fR14WFQltTPxkWNOAUnwJABR7T8TcsNMxr23xEsYWMrX06uuGD\n3bRWlJk59gne5YY59QsMNbFWCxNWGlf8oFxUJGrPUWVvUc1jlHrVcTLFbwJBAJTD\nYDwMFEgGgMQHLjg1KwuS1tkQjUqJZ/xMbvCkeQSB9R+F2aDehfTJ2DQqVYdDGER7\ntsSXyBSV5tvH9EOVRIcCQFktF/V6TW6X3I3XmQjEuie4W0UT8tpWUcgkwO8slvra\nQ7aSCBe5pLlVT0SorTqH3ywozGy2uzGyaigjtIFbgcM=\n-----END RSA PRIVATE KEY-----"

JWS_PATTERN = r"[-\w]+\.[-\w]+\.[-\w]+"

def test_sign_rsa():
    algorithm = "RS256"
    payload = {"a": "b"}
    jwe = jws.sign(payload, rsa_private_key, algorithm=algorithm)
    asserts.assert_true(re.match(JWS_PATTERN, jwe))


def test_verify_rsa():
    algorithm = "RS256"
    jwe = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYiJ9.cfJ0k9bqQ8d36WJb3CSexeVWcBGAJKx7AmEis7G1sb162ehtEs2ZRe3S5LEKzKbBaI_wQKXHonMv8bQrzrjDZeAYkeW8nc0gyUkeyqw42lNdmj2BFu8f_jkSYqTqlD31MsGafSyfvxW1lYOO_porBQ3WWDt3-G-QUEw1y-x7lGg"
    verified = jws.verify(jwe, rsa_public_key, algorithms=[algorithm])
    asserts.assert_true(verified)


def test_sign_and_verify_rsa():
    algorithm = "RS256"
    payload = {"a": "b"}
    jwe = jws.sign(payload, rsa_private_key, algorithm=algorithm)
    asserts.assert_true(re.match(JWS_PATTERN, jwe))
    verified = jws.verify(jwe, rsa_public_key, algorithms=[algorithm])
    asserts.assert_true(verified)


def test_sign_ecc():
    algorithm = "ES256"
    payload = {"a": "b"}
    jwe = jws.sign(payload, ecc_private_key, algorithm=algorithm)
    asserts.assert_true(re.match(JWS_PATTERN, jwe))


def test_verify_ecc():
    algorithm = "ES256"
    jwe = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYiJ9.aag1Do5YHBFQHnW2UaUblLR5BxhSdORo2pWIvr3mKr3MyFoZIZnRrKEgwETfcT0W-Nk2zD7y5xz0dpWD2ZPSnw"
    verified = jws.verify(jwe, ecc_public_key, algorithms=[algorithm])
    asserts.assert_true(verified)


def test_sign_and_verify_ecc():
    algorithm = "ES256"
    payload = {"a": "b"}
    jwe = jws.sign(payload, ecc_private_key, algorithm=algorithm)
    print(jwe)
    asserts.assert_true(re.match(JWS_PATTERN, jwe))
    verified = jws.verify(jwe, ecc_public_key, algorithms=[algorithm])
    asserts.assert_true(verified)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_sign_rsa))
    _suite.addTest(unittest.FunctionTestCase(test_verify_rsa))
    _suite.addTest(unittest.FunctionTestCase(test_sign_and_verify_rsa))
    _suite.addTest(unittest.FunctionTestCase(test_sign_ecc))
    _suite.addTest(unittest.FunctionTestCase(test_verify_ecc))
    _suite.addTest(unittest.FunctionTestCase(test_sign_and_verify_ecc))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
