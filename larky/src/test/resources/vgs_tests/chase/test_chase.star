load("@stdlib//unittest","unittest")
load("@stdlib//json", "json")
load("@stdlib//base64", "base64")
load("@vendor//asserts","asserts")

load("@vgs//chase/jwk", get_public_keys="get_public_keys", decrypt="decrypt")

def test_get_keys():
    """
    A dedicated static test set should be generated to compare against
    """
    get_public_keys()

def test_byte_string_decryption():
    jwe_string = '{"cardInfo": {"name": "Josh", "cardNumber": 4111111111111111, "approved": true}}'
    s = bytes(jwe_string, 'utf-8')
    decrypted = decrypt(s)
    d = json.loads(decrypted)
    asserts.assert_that(d['cardInfo']['cardNumber']).is_equal_to(4111111111111111)

def test_string_decryption():
    jwe_string = '{"cardInfo": {"name": "Josh", "cardNumber": 4111111111111111, "approved": true}}'
    decrypted = decrypt(jwe_string)
    d = json.loads(decrypted)
    asserts.assert_that(d['cardInfo']['cardNumber']).is_equal_to(4111111111111111)

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_byte_string_decryption))
    _suite.addTest(unittest.FunctionTestCase(test_string_decryption))
    _suite.addTest(unittest.FunctionTestCase(test_get_keys))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
