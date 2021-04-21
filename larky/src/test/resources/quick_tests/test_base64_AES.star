load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Util/Padding", pad="pad", unpad="unpad")
load("@stdlib//binascii", binascii="binascii")
 
load("@stdlib//json", "json")


def test_AES():
    KEY = "yC/F2oFzd4BD9Mk0/yDtiZz0lnPAhIiAZ7FyOuPGiAM="
    IV = "ssyVg3DduBs9OzCA489fSQ=="
    PAYLOAD = '{"UserDetails":{"UserKey":"1001073293","ExternalUserId":"1001073293","LanguageId":"en-us"},"CardDetails":{"CardNumber":"4242424242424242"},"UserPreferences":{"LanguageId":"en-us"},"Contacts":[{"ContactType":"Email","ContactValue":"naresh.maharjan+202103261101@cashrewards.com"}],"UserAttributes":[]}'
    key_bytes = binascii.a2b_base64(KEY)
    iv_bytes = binascii.a2b_base64(IV)
    cipher = AES.new(key_bytes, AES.MODE_CBC, iv_bytes)
    ciphertext = cipher.encrypt(pad(bytes(PAYLOAD, encoding='utf-8'), AES.block_size))
    ciphertext_string = binascii.b2a_base64(ciphertext).decode("utf-8")
    expected_payload_string = "mXNuKyZ3a15vxEN+NNUvsdgooyAZmQNB8WMBEkl14Jw3QO+DxH1z5JGVFJP+1T1iqUlpyfuagWTMr9TrZl1PfBK1orfcMWvVzXU5URon4f786fBNc5sH6RrAxO1ThQmnNUE6SjxJsMYG9tQB6TuOhdEjb43OzVlwuEadcIFoWgjZLeTQqSzLm7DhwGbTOzUsHaM+gsodaMxGe/p6SX1hTQGK/ut2l8FS+CY1p/4hzZkvIPmf8Q0mEAwUnd4bSTIDAUhyW56FyVn033jAwSlBPGuNlncQ0c5QJW6BaqgGJx1FjHR3TvM3Fw7XJmcwWM0LeCWVxfB5To0UvVNnFzo46ml/3/LNLbflkPVUO+9ffScGm+Ep6jO9mpGjBMTXPCClwniOlatDuLBhZRXaOtv8kA=="
    asserts.assert_that(ciphertext_string.strip()).is_equal_to(expected_payload_string.strip())

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_AES))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())

