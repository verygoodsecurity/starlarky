load("@stdlib//base64", base64="base64")
load("@stdlib//json", json="json")
load("@vendor//Crypto/Hash/HMAC", HMAC="HMAC")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")

# the following is only for tests (do not put in your yaml in live/prod)
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")


def _test_fd_hmac_sha256_signatur():
    API_KEY = b"1234"
    CLIENT_REQUEST_ID = b"5678"
    TIMESTAMP = b"1628108528"
    PAYLOAD = {"one": 1, "two": 2}
    API_SECRET = b"secretsauce"
    msg = API_KEY + CLIENT_REQUEST_ID + TIMESTAMP + bytes(json.dumps(PAYLOAD), encoding='utf-8')
    m = HMAC.new(API_SECRET, msg=msg, digestmod=SHA256)
    output = m.digest()
    print(output.hex())
    print("base64 encoded: ", base64.b64encode(bytes(output.hex(), encoding='utf-8')))


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_fd_hmac_sha256_signatur))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())