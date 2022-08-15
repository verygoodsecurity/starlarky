load("@stdlib//unittest","unittest")
load("@stdlib//base64", "base64")
load("@stdlib//json","json")
load("@stdlib//types", "types")
load("@stdlib//random", "random")

load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Util/Counter", Counter="Counter")
load("@vendor//asserts","asserts")
    
def test_ctr():
    key = b'Sixteen byte key'
    ctr = Counter.new(AES.block_size * 8)
    crypto = AES.new(key, AES.MODE_CTR)
    nonce = base64.b64encode(crypto.nonce).decode('utf-8')
    encrypted = base64.b64encode(crypto.encrypt(b"TEST PAYLOAD!")).decode('utf-8')
    result = json.dumps({"nonce":nonce,"ciphertext":encrypted})

    b64 = json.loads(result)
    nonce = base64.b64decode(b64['nonce'])
    ct = base64.b64decode(b64['ciphertext'])
    cipher = AES.new(key, AES.MODE_CTR, nonce=nonce)
    pt = cipher.decrypt(ct).decode('utf-8')
    asserts.assert_that(pt).is_equal_to("TEST PAYLOAD!")

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_ctr))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
