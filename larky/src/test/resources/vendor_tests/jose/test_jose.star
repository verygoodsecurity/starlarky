load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//jose/jwe", jwe="jwe")


def test_jwe():
    key = bytes('b11444485bd146cc823ae1bf3fa42209', encoding='utf-8')
    data = jwe.encrypt(bytes('533', encoding='utf-8'), key)
    print(data)
    decrypted_data = jwe.decrypt(data, key)
    print(decrypted_data)


def test_decrypt_jwe():
    jweString = "eyJlbmMiOiJBMjU2R0NNIiwidGFnIjoiVnBXUHlqM0p1Z3RIQllFbkRGUzk4dyIsImFsZyI6IkEyNTZHQ01LVyIsIml2IjoiYXRrWE53ME43VUV1QmNHRCJ9.GinV41Xz8H8Lk4lEauPBU4hBo5tC7M9KFWXHWXy284s.PSgGkgSI4JibSG-w.NSzF.4P_q5iQ4fpeHhEWzHMo0IQ"
    encryptionKey = bytes('b11444485bd146cc823ae1bf3fa42209', encoding='utf-8')
    data = jwe.decrypt(bytes(jweString, encoding='utf-8'), encryptionKey)
    payload = data['payload']
    asserts.assert_that(payload).is_equal_to(bytes('533', encoding='utf-8'))


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_jwe))
    _suite.addTest(unittest.FunctionTestCase(test_decrypt_jwe))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_jwe))
    _suite.addTest(unittest.FunctionTestCase(test_decrypt_jwe))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())