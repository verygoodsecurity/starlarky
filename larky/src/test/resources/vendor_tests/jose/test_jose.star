load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//jose/jwe", jwe="jwe")
load("@vendor//jose/backends", AESKey="AESKey")
load("@vendor//jose/constants", ALGORITHMS="ALGORITHMS")
load("@stdlib//binascii", "binascii")
load("@stdlib//larky", larky="larky")


def test_encrypt_and_decrypt_jwe_with_defaults():
    key = b'b11444485bd146cc823ae1bf3fa42209'
    data = jwe.encrypt(b'533', key)
    decrypted_data = jwe.decrypt(data, key)
    asserts.eq(decrypted_data, b'533')


def test_decrypt_GCM256_AES_wrapped_key_jwe():
    jweString = b"eyJlbmMiOiJBMjU2R0NNIiwidGFnIjoiazhaNnpTNjRJclllaUNpNV9JaWY5QSIsImFsZyI6IkEyNTZHQ01LVyIsIml2IjoieW9sTk8xLVFXSVg3R1poSCJ9.knPF3qV22v0pE-N6oUzlSIoBUEjr_k3sfFyYX-XuSH8.XkADjU0P2phiynPA.cvCd.Wp5oFTRAzEIFN8pImDnhmw"
    encryptionKey = b'96a18c1acc0b48beb9b24479355b70b5'
    payload = jwe.decrypt(jweString, encryptionKey)
    asserts.assert_that(payload).is_equal_to(b'599')


def test_aes_unwrap_key(kek, output, data, algo):
    kek = binascii.unhexlify(kek)
    data = binascii.unhexlify(data)
    output = binascii.unhexlify(output)
    aes = AESKey(kek, algo)
    asserts.eq(aes.unwrap(data, headers={}, enc_alg=algo), output)


def test_vector_RFC_3394_wrap():
    #test vector from RFC 3394
    algo = "A128KW"
    kek = binascii.unhexlify("000102030405060708090A0B0C0D0E0F")
    plain = binascii.unhexlify("00112233445566778899AABBCCDDEEFF")
    data = binascii.unhexlify("1FA68B0A8112B447AEF34BD8FB5A7B829D3E862371D2CFE5")
    aes = AESKey(kek, algo)
    asserts.eq(aes.wrap(plain, algo), data)


def _testsuite():
    _suite = unittest.TestSuite()

    _suite.addTest(unittest.FunctionTestCase(test_encrypt_and_decrypt_jwe_with_defaults))
    _suite.addTest(unittest.FunctionTestCase(test_decrypt_GCM256_AES_wrapped_key_jwe))
    larky.parametrize(
        _suite.addTest,
        unittest.FunctionTestCase,
        'kek,output,data,algo',
        [
            # Test cases from https://tools.ietf.org/html/rfc3394
            ("000102030405060708090A0B0C0D0E0F", "00112233445566778899AABBCCDDEEFF", "1FA68B0A8112B447AEF34BD8FB5A7B829D3E862371D2CFE5", "A128KW"),
            ("000102030405060708090A0B0C0D0E0F1011121314151617", "00112233445566778899AABBCCDDEEFF", "96778B25AE6CA435F92B5B97C050AED2468AB8A17AD84E5D", "A192KW"),
            ("000102030405060708090A0B0C0D0E0F1011121314151617", "00112233445566778899AABBCCDDEEFF0001020304050607", "031D33264E15D33268F24EC260743EDCE1C6C7DDEE725A936BA814915C6762D2", "A192KW"),
            ("000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F", "00112233445566778899AABBCCDDEEFF", "64E8C3F9CE0F5BA263E9777905818A2A93C8191E7D6E8AE7", "A256KW"),
            ("000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F", "00112233445566778899AABBCCDDEEFF0001020304050607", "A8F9BC1612C68B3FF6E6F4FBE30E71E4769C8B80A32CB8958CD5D17D6B254DA1", "A256KW"),
            ("000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F", "00112233445566778899AABBCCDDEEFF000102030405060708090A0B0C0D0E0F", "28C9F404C4B810F4CBCCB35CFB87F8263F5786E2D80ED326CBC7F0E71A99F43BFB988B9B7A02DD21", "A256KW"),
        ]
    )(test_aes_unwrap_key)
    _suite.addTest(unittest.FunctionTestCase(test_vector_RFC_3394_wrap))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())