load("@vendor//Crypto/Hash/HMAC", HMAC="HMAC")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")


def b(s):
    return builtins.bytes(s, encoding="ISO-8859-1")

eq = asserts.eq


def HMAC_test_hex_digest():
    # print("Default hmac-md5: ", HMAC.new(b("Swordfish"), b("hello")).hexdigest())
    eq(HMAC.new(b("Swordfish"), b("hello")).hexdigest(), '81a0430b04a8095d5482ec03706f3faa')
    # print("hmac-sha256", HMAC.new(b("Swordfish"), b("hello"), digestmod=SHA256).hexdigest())
    eq(HMAC.new(b("Swordfish"), b("hello"), digestmod=SHA256).hexdigest(), '4711f1ab6eec04c80df9a24f01d2e4bd33564af6b128a0ffaf967b06547e268a')


def HMAC_test_hexverify():
    hmac = HMAC.new(b("secret"), b("hello"))
    hmac.hexverify('bade63863c61ed0b3165806ecd6acefc')
    wrong_mac = '77726f6e675f6d6163'
    asserts.assert_fails(lambda : hmac.hexverify(wrong_mac), ".*?ValueError")


def HMAC_test_update():
    hmac = HMAC.new(b("1234secret"), b("test"))
    hmac.update(b("999"))
    eq(hmac.hexdigest(), 'c7d49ccaae072a775cd168659c52698f')


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(HMAC_test_hex_digest))
    _suite.addTest(unittest.FunctionTestCase(HMAC_test_hexverify))
    _suite.addTest(unittest.FunctionTestCase(HMAC_test_update))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())