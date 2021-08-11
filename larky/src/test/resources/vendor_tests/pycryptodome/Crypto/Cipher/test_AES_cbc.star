load("@stdlib//base64", "base64")
load("@stdlib//builtins","builtins")
load("@stdlib//unittest","unittest")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Util/Padding", pad="pad")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes")
load("@vendor//asserts","asserts")
load("@vendor//Crypto/Hash/SHAKE128", SHAKE128="SHAKE128")


def get_tag_random(tag, length):
    return SHAKE128.new(data=tobytes(tag)).read(length)

key_128 = get_tag_random("key_128", 16)

def b(s):
    return builtins.bytes(s, encoding="utf-8")

eq = asserts.eq

def Test_AES_cbc_loopback_128():
    iv  = b("0000000000000000")
    cipher = AES.new(key_128, AES.MODE_CBC, iv)
    pt = get_tag_random("plaintext", 16 * 100)
    ct = cipher.encrypt(pt)

    cipher = AES.new(key_128, AES.MODE_CBC, iv)
    pt2 = cipher.decrypt(ct)
    eq(pt, pt2)


def Test_AES_cbc_128_pkcs7():
    iv  = b("0000000000000000")
    cipher = AES.new(b("testtesttesttest"), AES.MODE_CBC, iv)
    pt = pad(b("1234123412341234"), 16)
    ct = cipher.encrypt(pt)
    eq(base64.b64encode(ct), b('CcP0yurcHgtVlLlSxNjqzRoJmTdtEo9dac49kXDGHOQ='))


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(Test_AES_cbc_loopback_128))
    _suite.addTest(unittest.FunctionTestCase(Test_AES_cbc_128_pkcs7))
    _suite.addTest(unittest.FunctionTestCase(_testsuite))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
