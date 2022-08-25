load("@stdlib//unittest","unittest")
load("@stdlib//base64", "base64")
load("@stdlib//json","json")
load("@stdlib//types", "types")
load("@stdlib//random", "random")

load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Util/Counter", Counter="Counter")
load("@vendor//asserts","asserts")
load("@vendor//Crypto/Hash", SHAKE128="SHAKE128")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bchr="bchr")


def get_tag_random(tag, length):
    return SHAKE128.new(data=tobytes(tag)).read(length)

key_128 = get_tag_random("key_128", 16)
key_192 = get_tag_random("key_192", 24)
nonce_32 = get_tag_random("nonce_32", 4)
nonce_64 = get_tag_random("nonce_64", 8)
ctr_64 = Counter.new(32, prefix=nonce_32)
ctr_128 = Counter.new(64, prefix=nonce_64)


def test_ctr():
    key = b'Sixteen byte key'
    ctr = Counter.new(AES.block_size * 8)
    crypto = AES.new(key, AES.MODE_CTR)
    nonce = base64.b64encode(crypto.nonce).decode('utf-8')
    encrypted = base64.b64encode(crypto.encrypt(b"TEST PAYLOAD")).decode('utf-8')
    result = json.dumps({"nonce":nonce,"ciphertext":encrypted})

    b64 = json.loads(result)
    nonce = base64.b64decode(b64['nonce'])
    ct = base64.b64decode(b64['ciphertext'])
    cipher = AES.new(key, AES.MODE_CTR, nonce=nonce)
    pt = cipher.decrypt(ct).decode('utf-8')
    asserts.assert_that(pt).is_equal_to("TEST PAYLOAD")

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_ctr))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())