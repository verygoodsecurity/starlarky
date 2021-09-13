load("@stdlib//builtins","builtins")
load("@stdlib//base64", "base64")
load("@stdlib//unittest","unittest")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Hash/SHAKE128", SHAKE128="SHAKE128")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes")
load("@vendor//asserts","asserts")

def b(s):
    return builtins.bytes(s, encoding="utf-8")

def get_tag_random(tag, length):
    return SHAKE128.new(data=tobytes(tag)).read(length)

key_128 = get_tag_random("key_128", 16)

eq = asserts.eq

def CfbTests_test_verify_ciphertext():
    pt = b("secret")
    cipher = AES.new(b("testtesttesttest"), AES.MODE_CFB, iv=b("0000000000000000"))
    ct = cipher.encrypt(pt)
    # print("aes cfb ciphertext:", base64.b64encode(ct))
    eq(base64.b64encode(ct), b("R5WM2ZTL"))

    cipher = AES.new(b("testtesttesttest"), AES.MODE_CFB, iv=b("0000000000000000"))
    pt2 = cipher.decrypt(ct)
    # print("aes cfb plaintext:", pt)
    eq(pt2, pt)


def CfbTests_test_random_loopback():
    cipher = AES.new(key_128, AES.MODE_CFB)
    pt = get_tag_random("plaintext", 16 * 100)
    random_iv = cipher.iv
    ct = cipher.encrypt(pt)

    cipher = AES.new(key_128, AES.MODE_CFB, iv=random_iv)
    pt2 = cipher.decrypt(ct)
    eq(pt2, pt)


def CfbTests_test_incorrect_iv():
    asserts.assert_fails(lambda : AES.new(key_128, AES.MODE_CFB, iv=b("0000000000000000"),
                      IV=b("0000000000000000")), ".*?TypeError")
    asserts.assert_fails(lambda : AES.new(key_128, AES.MODE_CFB,
                      iv=b("0000")), ".*?ValueError")


def CfbTests_test_incorrect_segment_size():
    asserts.assert_fails(lambda : AES.new(key_128, AES.MODE_CFB,
                    segment_size=3), ".*?ValueError")


def CfbTests_test_unknown_param():
    asserts.assert_fails(lambda : AES.new(key_128, AES.MODE_CFB,
                    some_param=0), ".*?TypeError")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(CfbTests_test_verify_ciphertext))
    _suite.addTest(unittest.FunctionTestCase(CfbTests_test_random_loopback))
    _suite.addTest(unittest.FunctionTestCase(CfbTests_test_incorrect_iv))
    _suite.addTest(unittest.FunctionTestCase(CfbTests_test_incorrect_segment_size))
    _suite.addTest(unittest.FunctionTestCase(CfbTests_test_unknown_param))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())