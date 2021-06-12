load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//jose/jwe", jwe="jwe")
load("@vendor//jose/backends", AESKey="AESKey")
load("@stdlib//binascii", "binascii")
load("@stdlib//larky", larky="larky")


def test_jwe():
    key = bytes('b11444485bd146cc823ae1bf3fa42209', encoding='utf-8')
    data = jwe.encrypt(bytes('533', encoding='utf-8'), key)
    decrypted_data = jwe.decrypt(data, key)
    asserts.eq(decrypted_data, bytes('533', encoding='utf-8'))

def test_decrypt_jwe():
    jweString = "eyJlbmMiOiJBMjU2R0NNIiwidGFnIjoiVnBXUHlqM0p1Z3RIQllFbkRGUzk4dyIsImFsZyI6IkEyNTZHQ01LVyIsIml2IjoiYXRrWE53ME43VUV1QmNHRCJ9.GinV41Xz8H8Lk4lEauPBU4hBo5tC7M9KFWXHWXy284s.PSgGkgSI4JibSG-w.NSzF.4P_q5iQ4fpeHhEWzHMo0IQ"
    encryptionKey = bytes('b11444485bd146cc823ae1bf3fa42209', encoding='utf-8')
    data = jwe.decrypt(bytes(jweString, encoding='utf-8'), encryptionKey)
    data.unwrap()
    payload = data['payload']
    asserts.assert_that(payload).is_equal_to(bytes('533', encoding='utf-8'))


def test_aes_unwrap_key(kek, output, data, algo):
    kek = binascii.unhexlify(kek)
    data = binascii.unhexlify(data)
    output = binascii.unhexlify(output)
    aes = AESKey(kek, algo)
    asserts.eq(aes.unwrap_key(data), output)


def test_aes_wrap_key():
    # test vector from RFC 3394
    algo = "A128GCMKW"
    kek = binascii.unhexlify('000102030405060708090A0B0C0D0E0F')
    plain = binascii.unhexlify('00112233445566778899AABBCCDDEEFF')
    data = binascii.unhexlify('1FA68B0A8112B447AEF34BD8FB5A7B829D3E862371D2CFE5')
    aes = AESKey(kek, algo)
    asserts.eq(aes.wrap_key(plain), data)


def _testsuite():
    _suite = unittest.TestSuite()
    #_suite.addTest(unittest.FunctionTestCase(test_jwe))
    # _suite.addTest(unittest.FunctionTestCase(test_decrypt_jwe))
    larky.parametrize(
        _suite.addTest,
        unittest.FunctionTestCase,
        'kek,output,data,algo',
        [
            # Test cases from https://tools.ietf.org/html/rfc3394
            ("000102030405060708090A0B0C0D0E0F", "00112233445566778899AABBCCDDEEFF", "1FA68B0A8112B447AEF34BD8FB5A7B829D3E862371D2CFE5", "A128GCMKW"),
            ("000102030405060708090A0B0C0D0E0F1011121314151617", "00112233445566778899AABBCCDDEEFF", "96778B25AE6CA435F92B5B97C050AED2468AB8A17AD84E5D", "A192GCMKW"),
            ("000102030405060708090A0B0C0D0E0F1011121314151617", "00112233445566778899AABBCCDDEEFF0001020304050607", "031D33264E15D33268F24EC260743EDCE1C6C7DDEE725A936BA814915C6762D2", "A192GCMKW"),
            ("000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F", "00112233445566778899AABBCCDDEEFF", "64E8C3F9CE0F5BA263E9777905818A2A93C8191E7D6E8AE7", "A256GCMKW"),
            ("000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F", "00112233445566778899AABBCCDDEEFF0001020304050607", "A8F9BC1612C68B3FF6E6F4FBE30E71E4769C8B80A32CB8958CD5D17D6B254DA1", "A256GCMKW"),
            ("000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F", "00112233445566778899AABBCCDDEEFF000102030405060708090A0B0C0D0E0F", "28C9F404C4B810F4CBCCB35CFB87F8263F5786E2D80ED326CBC7F0E71A99F43BFB988B9B7A02DD21", "A256GCMKW"),
        ]
    )(test_aes_unwrap_key)
    _suite.addTest(unittest.FunctionTestCase(test_aes_wrap_key))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())