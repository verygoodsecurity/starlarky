load("@vendor//asserts", asserts="asserts")
load("@stdlib//unittest", unittest="unittest")

load("@vendor//Crypto/Signature/DSS", DSS="DSS")
load("@vendor//Crypto/PublicKey/DSA", DSA="DSA")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")


def test_DSS():
    key = DSA.generate(2048)
    message = b"Hello"
    hash_obj = SHA256.new(message)
    signer = DSS.new(key, 'fips-186-3')
    signature = signer.sign(hash_obj)
    # this is correct because signer.verify() returns False to as per
    # pycryptodome contract to keep the API identical to pycrypto.
    asserts.assert_that(signer.verify(hash_obj, signature)).is_false()


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_DSS))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
