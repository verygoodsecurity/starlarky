load("@stdlib//builtins", builtins="builtins")
load("@vendor//Crypto/Hash/HMAC", HMAC="HMAC")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")

load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")


def b(s):
    return builtins.bytes(s, encoding="UTF8")


def test_aws_v4_sign():
    expected_signature = "20a7ab2bfce9848a117546ad4f701270683e0f86e72703872841f7343a1a1a7e"
    key = "this-is-a-key"
    date = "1959-02-03"
    region = "us-west-2"
    service_name = "some-service"
    kDate = HMAC.new(b("AWS4" + key), b(date), digestmod=SHA256).digest()
    kRegion = HMAC.new(kDate, b(region), digestmod=SHA256).digest()
    kService = HMAC.new(kRegion, b(service_name), digestmod=SHA256).digest()
    kSigning = HMAC.new(kService, b("aws4_request"), digestmod=SHA256).hexdigest()
    asserts.assert_that(kSigning).is_equal_to(expected_signature)

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_aws_v4_sign))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
