load("@stdlib//binascii", uh="unhexlify")
load("@stdlib//builtins", "builtins")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//Crypto/Util/Padding", pad="pad", unpad="unpad")


b = builtins.bytearray


def PKCS7_Tests_test1():
    padded = pad(b(r""), 4)
    asserts.assert_that((padded == uh(b(r"04040404")))).is_true()
    padded = pad(b(r""), 4, 'pkcs7')
    asserts.assert_that((padded == uh(b(r"04040404")))).is_true()
    back = unpad(padded, 4)
    asserts.assert_that((back == b(r""))).is_true()

def PKCS7_Tests_test2():
    padded = pad(b(uh(b(r"12345678"))), 4)
    asserts.assert_that((padded == uh(b(r"1234567804040404")))).is_true()
    back = unpad(padded, 4)
    asserts.assert_that((back == uh(b(r"12345678")))).is_true()

def PKCS7_Tests_test3():
    padded = pad(b(uh(b(r"123456"))), 4)
    asserts.assert_that((padded == uh(b(r"12345601")))).is_true()
    back = unpad(padded, 4)
    asserts.assert_that((back == uh(b(r"123456")))).is_true()

def PKCS7_Tests_test4():
    padded = pad(b(uh(b(r"1234567890"))), 4)
    asserts.assert_that((padded == uh(b(r"1234567890030303")))).is_true()
    back = unpad(padded, 4)
    asserts.assert_that((back == uh(b(r"1234567890")))).is_true()

def PKCS7_Tests_testn1():
    asserts.assert_fails(lambda : pad(b(uh(b(r"12"))), 4, 'pkcs8'), ".*?ValueError")

def PKCS7_Tests_testn2():
    asserts.assert_fails(lambda : unpad(b(r"\0\0\0"), 4), ".*?ValueError")
    asserts.assert_fails(lambda : unpad(b(r""), 4), ".*?ValueError")

def PKCS7_Tests_testn3():
    asserts.assert_fails(lambda : unpad(b(r"123456\x02"), 4), ".*?ValueError")
    asserts.assert_fails(lambda : unpad(b(r"123456\x00"), 4), ".*?ValueError")
    asserts.assert_fails(lambda : unpad(b(r"123456\x05\x05\x05\x05\x05"), 4), ".*?ValueError")


def X923_Tests_test1():
    padded = pad(b(r""), 4, 'x923')
    asserts.assert_that((padded == uh(b(r"00000004")))).is_true()
    back = unpad(padded, 4, 'x923')
    asserts.assert_that((back == b(r""))).is_true()

def X923_Tests_test2():
    padded = pad(b(uh(b(r"12345678"))), 4, 'x923')
    asserts.assert_that((padded == uh(b(r"1234567800000004")))).is_true()
    back = unpad(padded, 4, 'x923')
    asserts.assert_that((back == uh(b(r"12345678")))).is_true()

def X923_Tests_test3():
    padded = pad(b(uh(b(r"123456"))), 4, 'x923')
    asserts.assert_that((padded == uh(b(r"12345601")))).is_true()
    back = unpad(padded, 4, 'x923')
    asserts.assert_that((back == uh(b(r"123456")))).is_true()

def X923_Tests_test4():
    padded = pad(b(uh(b(r"1234567890"))), 4, 'x923')
    asserts.assert_that((padded == uh(b(r"1234567890000003")))).is_true()
    back = unpad(padded, 4, 'x923')
    asserts.assert_that((back == uh(b(r"1234567890")))).is_true()

def X923_Tests_testn1():
    asserts.assert_fails(lambda : unpad(b(r"123456\x02"), 4, 'x923'), ".*?ValueError")
    asserts.assert_fails(lambda : unpad(b(r"123456\x00"), 4, 'x923'), ".*?ValueError")
    asserts.assert_fails(lambda : unpad(b(r"123456\x00\x00\x00\x00\x05"), 4, 'x923'), ".*?ValueError")
    asserts.assert_fails(lambda : unpad(b(r""), 4, 'x923'), ".*?ValueError")


def ISO7816_Tests_test1():
    padded = pad(b(r""), 4, 'iso7816')
    asserts.assert_that((padded == uh(b(r"80000000")))).is_true()
    back = unpad(padded, 4, 'iso7816')
    asserts.assert_that((back == b(r""))).is_true()

def ISO7816_Tests_test2():
    padded = pad(b(uh(b(r"12345678"))), 4, 'iso7816')
    asserts.assert_that((padded == uh(b(r"1234567880000000")))).is_true()
    back = unpad(padded, 4, 'iso7816')
    asserts.assert_that((back == uh(b(r"12345678")))).is_true()

def ISO7816_Tests_test3():
    padded = pad(b(uh(b(r"123456"))), 4, 'iso7816')
    asserts.assert_that((padded == uh(b(r"12345680")))).is_true()
        #import pdb; pdb.set_trace()
    back = unpad(padded, 4, 'iso7816')
    asserts.assert_that((back == uh(b(r"123456")))).is_true()

def ISO7816_Tests_test4():
    padded = pad(b(uh(b(r"1234567890"))), 4, 'iso7816')
    asserts.assert_that((padded == uh(b(r"1234567890800000")))).is_true()
    back = unpad(padded, 4, 'iso7816')
    asserts.assert_that((back == uh(b(r"1234567890")))).is_true()

def ISO7816_Tests_testn1():
    asserts.assert_fails(lambda : unpad(b(r"123456\x81"), 4, 'iso7816'), ".*?ValueError")
    asserts.assert_fails(lambda : unpad(b(r""), 4, 'iso7816'), ".*?ValueError")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(PKCS7_Tests_test1))
    _suite.addTest(unittest.FunctionTestCase(PKCS7_Tests_test2))
    _suite.addTest(unittest.FunctionTestCase(PKCS7_Tests_test3))
    _suite.addTest(unittest.FunctionTestCase(PKCS7_Tests_test4))
    _suite.addTest(unittest.FunctionTestCase(PKCS7_Tests_testn1))
    _suite.addTest(unittest.FunctionTestCase(PKCS7_Tests_testn2))
    _suite.addTest(unittest.FunctionTestCase(PKCS7_Tests_testn3))
    _suite.addTest(unittest.FunctionTestCase(X923_Tests_test1))
    _suite.addTest(unittest.FunctionTestCase(X923_Tests_test2))
    _suite.addTest(unittest.FunctionTestCase(X923_Tests_test3))
    _suite.addTest(unittest.FunctionTestCase(X923_Tests_test4))
    _suite.addTest(unittest.FunctionTestCase(X923_Tests_testn1))
    _suite.addTest(unittest.FunctionTestCase(ISO7816_Tests_test1))
    _suite.addTest(unittest.FunctionTestCase(ISO7816_Tests_test2))
    _suite.addTest(unittest.FunctionTestCase(ISO7816_Tests_test3))
    _suite.addTest(unittest.FunctionTestCase(ISO7816_Tests_test4))
    _suite.addTest(unittest.FunctionTestCase(ISO7816_Tests_testn1))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
