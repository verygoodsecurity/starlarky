load("@stdlib//builtins","builtins")
load("@stdlib//base64", "base64")
load("@stdlib//unittest","unittest")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//asserts","asserts")

def b(s):
    return builtins.bytes(s, encoding="utf-8")

eq = asserts.eq

def CfbTests_test():
    pt = b("secret")
    cipher = AES.new(b("testtesttesttest"), AES.MODE_CFB, iv=b("0000000000000000"))
    ct = cipher.encrypt(pt)
    # print("aes cfb ciphertext:", base64.b64encode(ct))
    eq(base64.b64encode(ct), b("R5WM2ZTL"))

    cipher = AES.new(b("testtesttesttest"), AES.MODE_CFB, iv=b("0000000000000000"))
    pt2 = cipher.decrypt(ct)
    # print("aes cfb plaintext:", pt)
    eq(pt2, pt)

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(CfbTests_test))
    _suite.addTest(unittest.FunctionTestCase(_testsuite))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())