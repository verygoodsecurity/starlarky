load("@stdlib//base64", base64="base64")
load("@stdlib//json", json="json")
load("@vendor//Crypto/Hash/HMAC", HMAC="HMAC")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")

# the following is only for tests (do not put in your yaml in live/prod)
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")



def _test_fd_hmac_sha256_signature():
    API_KEY = b"1234"
    CLIENT_REQUEST_ID = b"5678"
    TIMESTAMP = b"1628108528"
    PAYLOAD = {"one": 1, "two": 2}
    API_SECRET = b"secretsauce"
    msg = API_KEY + CLIENT_REQUEST_ID + TIMESTAMP + bytes(json.dumps(PAYLOAD), encoding='utf-8')
    m = HMAC.new(API_SECRET, msg=msg, digestmod=SHA256)
    output = m.digest()
    hexOutput = output.hex()
    asserts.assert_that(hexOutput).is_equal_to("ae41813900fc644c3ec8d448cfb068b82cd9ba5f495a7e28b75a5a8ec92a9668")
    asserts.assert_that(
        base64.b64encode(bytes(hexOutput, encoding='utf-8'))
    ).is_equal_to(b"YWU0MTgxMzkwMGZjNjQ0YzNlYzhkNDQ4Y2ZiMDY4YjgyY2Q5YmE1ZjQ5NWE3ZTI4Yjc1YTVhOGVjOTJhOTY2OA==")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_fd_hmac_sha256_signature))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())