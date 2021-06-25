load("@stdlib//larky", "larky")
load("@stdlib//binascii", binascii="binascii")
load("@stdlib//unittest","unittest")
load("@vendor//asserts","asserts")
load("@vendor//aes_keywrap","aes_keywrap")


def test_vector_RFC_3394_unwrap():
    #test vector from RFC 3394
    KEK = binascii.unhexlify("000102030405060708090A0B0C0D0E0F")
    CIPHER = binascii.unhexlify("1FA68B0A8112B447AEF34BD8FB5A7B829D3E862371D2CFE5")
    PLAIN = binascii.unhexlify("00112233445566778899AABBCCDDEEFF")
    asserts.assert_that(aes_keywrap.aes_unwrap_key(KEK, CIPHER)).is_equal_to(PLAIN)

def test_vector_RFC_3394_wrap():
    #test vector from RFC 3394
    KEK = binascii.unhexlify("000102030405060708090A0B0C0D0E0F")
    CIPHER = binascii.unhexlify("1FA68B0A8112B447AEF34BD8FB5A7B829D3E862371D2CFE5")
    PLAIN = binascii.unhexlify("00112233445566778899AABBCCDDEEFF")
    asserts.assert_that(aes_keywrap.aes_wrap_key(KEK,PLAIN)).is_equal_to(CIPHER)

def test_vector_RFC_5649_20_unwrap():
    #test vector from RFC 5649 - 20 octets
    KEK = binascii.unhexlify("5840DF6E29B02AF1AB493B705BF16EA1AE8338F4DCC176A8")
    CIPHER = binascii.unhexlify("138BDEAA9B8FA7FC61F97742E72248EE5AE6AE5360D1AE6A5F54F373FA543B6A")
    PLAIN = binascii.unhexlify("C37B7E6492584340BED12207808941155068F738")
    asserts.assert_that(aes_keywrap.aes_unwrap_key_withpad(KEK, CIPHER)).is_equal_to(PLAIN)

def test_vector_RFC_5649_20_wrap():
    #test vector from RFC 5649 - 20 octets
    KEK = binascii.unhexlify("5840DF6E29B02AF1AB493B705BF16EA1AE8338F4DCC176A8")
    CIPHER = binascii.unhexlify("138BDEAA9B8FA7FC61F97742E72248EE5AE6AE5360D1AE6A5F54F373FA543B6A")
    PLAIN = binascii.unhexlify("C37B7E6492584340BED12207808941155068F738")
    asserts.assert_that(aes_keywrap.aes_wrap_key_withpad(KEK, PLAIN)).is_equal_to(CIPHER)

def test_vector_RFC_5649_7_unwrap():
    #test vector from RFC 5649 - 7 octets
    KEK = binascii.unhexlify("5840DF6E29B02AF1AB493B705BF16EA1AE8338F4DCC176A8")
    CIPHER = binascii.unhexlify("AFBEB0F07DFBF5419200F2CCB50BB24F")
    PLAIN = binascii.unhexlify("466F7250617369")
    asserts.assert_that(aes_keywrap.aes_unwrap_key_withpad(KEK, CIPHER)).is_equal_to(PLAIN)

def test_vector_RFC_5649_7_wrap():
    #test vector from RFC 5649 - 7 octets
    KEK = binascii.unhexlify("5840DF6E29B02AF1AB493B705BF16EA1AE8338F4DCC176A8")
    CIPHER = binascii.unhexlify("AFBEB0F07DFBF5419200F2CCB50BB24F")
    PLAIN = binascii.unhexlify("466F7250617369")
    asserts.assert_that(aes_keywrap.aes_wrap_key_withpad(KEK,PLAIN)).is_equal_to(CIPHER)

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_vector_RFC_3394_unwrap))
    _suite.addTest(unittest.FunctionTestCase(test_vector_RFC_3394_wrap))
    _suite.addTest(unittest.FunctionTestCase(test_vector_RFC_5649_20_unwrap))
    _suite.addTest(unittest.FunctionTestCase(test_vector_RFC_5649_20_wrap))
    _suite.addTest(unittest.FunctionTestCase(test_vector_RFC_5649_7_unwrap))
    _suite.addTest(unittest.FunctionTestCase(test_vector_RFC_5649_7_wrap))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
