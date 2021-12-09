load("@vendor//Crypto/Hash/HMAC", HMAC="HMAC")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//Crypto/Hash/SHA1", SHA1="SHA1")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")
load("@vendor//Crypto/Hash/SHA512", SHA512="SHA512")


def b(s):
    return builtins.bytes(s, encoding="ISO-8859-1")

eq = asserts.eq


def HMAC_test_hex_digest():
    # print("Default hmac-md5: ", HMAC.new(b("Swordfish"), b("hello")).hexdigest())
    eq(HMAC.new(b("Swordfish"), b("hello")).hexdigest(), '81a0430b04a8095d5482ec03706f3faa')
    # print("hmac-sha256", HMAC.new(b("Swordfish"), b("hello"), digestmod=SHA256).hexdigest())
    eq(HMAC.new(b("Swordfish"), b("hello"), digestmod=SHA256).hexdigest(), '4711f1ab6eec04c80df9a24f01d2e4bd33564af6b128a0ffaf967b06547e268a')

    eq(HMAC.new(b("Swordfish"), b("hello"), digestmod=SHA512).hexdigest(), 'dfe0743e40286d8575573bd776e33df4d47e4157b65ed858d5a543fbd670e1705ea940851175b2ffe422f9e5d41f98f5bb199c6d2f1c0dbe36dddb980f16b469')


def HMAC_test_hexverify():
    hmac = HMAC.new(b("secret"), b("hello"))
    hmac.hexverify('bade63863c61ed0b3165806ecd6acefc')
    wrong_mac = '77726f6e675f6d6163'
    asserts.assert_fails(lambda : hmac.hexverify(wrong_mac), ".*?ValueError")


def HMAC_test_update():
    hmac = HMAC.new(b("1234secret"), b("test"))
    hmac.update(b("999"))
    eq(hmac.hexdigest(), 'c7d49ccaae072a775cd168659c52698f')


def HMAC_test_issue_179():
    # https://github.com/verygoodsecurity/starlarky/issues/179
    key = b'aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434ddabede0f3b482cd9aea9434d'
    msg = b'hello world'
    expected = "85a5308073975a335e69381949fa33e269ff3b1a"
    asserts.assert_that(HMAC.new(key, msg, digestmod=SHA1).hexdigest()).is_equal_to(expected)

    # test key length surpasses 64
    key = b'aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434ddabede0f3b482cd9aea9434dt'
    msg = b'hello world'
    expected = "6a176496acfd81da8d2d88514a4ec453a0028fbc"
    asserts.assert_that(HMAC.new(key, msg, digestmod=SHA1).hexdigest()).is_equal_to(expected)

    # manual implementation
    key = b'aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434ddabede0f3b482cd9aea9434dt'
    key_hashed = SHA1.new(key).digest()
    msg = b'hello world'
    expected = "6a176496acfd81da8d2d88514a4ec453a0028fbc"
    asserts.assert_that(HMAC.new(key_hashed, msg, digestmod=SHA1).hexdigest()).is_equal_to(expected)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(HMAC_test_hex_digest))
    _suite.addTest(unittest.FunctionTestCase(HMAC_test_hexverify))
    _suite.addTest(unittest.FunctionTestCase(HMAC_test_update))
    _suite.addTest(unittest.FunctionTestCase(HMAC_test_issue_179))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())