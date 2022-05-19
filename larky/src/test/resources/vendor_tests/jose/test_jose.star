load("@stdlib//base64", base64="base64")
load("@stdlib//binascii", binascii="binascii")
load("@stdlib//json", json="json")
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Hash/SHA512", SHA512="SHA512")
load("@vendor//Crypto/Protocol/KDF", PBKDF2="PBKDF2")
load("@vendor//Crypto/Util/Padding", pad="pad", unpad="unpad")
load("@vendor//asserts", asserts="asserts")
load("@vendor//jose/backends", AESKey="AESKey")
load("@vendor//jose/constants", ALGORITHMS="ALGORITHMS")
load("@vendor//jose/jwe", jwe="jwe")
load("@vendor//jose/jwk", jwk="jwk")
load("@vendor//jose/utils", base64url_encode="base64url_encode")



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
    # test vector from RFC 3394
    algo = "A128KW"
    kek = binascii.unhexlify("000102030405060708090A0B0C0D0E0F")
    plain = binascii.unhexlify("00112233445566778899AABBCCDDEEFF")
    data = binascii.unhexlify("1FA68B0A8112B447AEF34BD8FB5A7B829D3E862371D2CFE5")
    aes = AESKey(kek, algo)
    asserts.eq(aes.wrap(plain, algo), data)


def test_vector_RFC_3394_unwrap():
    # test vector from RFC 3394
    algo = "A128KW"
    kek = binascii.unhexlify("000102030405060708090A0B0C0D0E0F")
    cipher = binascii.unhexlify("1FA68B0A8112B447AEF34BD8FB5A7B829D3E862371D2CFE5")
    plain = binascii.unhexlify("00112233445566778899AABBCCDDEEFF")
    aes = AESKey(kek, algo)
    asserts.eq(aes.unwrap(cipher, headers={}, enc_alg=algo), plain)


def test_vector_RFC_5649_7_wrap():
    # test vector from RFC 5649 - 7 octets
    # https://datatracker.ietf.org/doc/html/rfc5649#section-6
    algo = "A192KW"
    kek = binascii.unhexlify("5840DF6E29B02AF1AB493B705BF16EA1AE8338F4DCC176A8")
    cipher = binascii.unhexlify("AFBEB0F07DFBF5419200F2CCB50BB24F")
    plain = binascii.unhexlify("466F7250617369")
    aes = AESKey(kek, algo)
    asserts.eq(aes.wrap(plain, algo, headers={"with_padding": True}), cipher)


def test_vector_RFC_5649_7_unwrap():
    # test vector from RFC 5649 - 7 octets
    # https://datatracker.ietf.org/doc/html/rfc5649#section-6
    algo = "A192KW"
    kek = binascii.unhexlify("5840DF6E29B02AF1AB493B705BF16EA1AE8338F4DCC176A8")
    cipher = binascii.unhexlify("AFBEB0F07DFBF5419200F2CCB50BB24F")
    plain = binascii.unhexlify("466F7250617369")
    aes = AESKey(kek, algo)
    asserts.eq(aes.unwrap(cipher, headers={"with_padding": True}, enc_alg=algo), plain)


def test_vector_RFC_5649_20_wrap():
    # test vector from RFC 5649 - 20 octets
    # https://datatracker.ietf.org/doc/html/rfc5649#section-6
    algo = "A192KW"
    kek = binascii.unhexlify("5840DF6E29B02AF1AB493B705BF16EA1AE8338F4DCC176A8")
    cipher = binascii.unhexlify("138BDEAA9B8FA7FC61F97742E72248EE5AE6AE5360D1AE6A5F54F373FA543B6A")
    plain = binascii.unhexlify("C37B7E6492584340BED12207808941155068F738")
    aes = AESKey(kek, algo)
    asserts.eq(aes.wrap(plain, algo, headers={"with_padding": True}), cipher)


def test_vector_RFC_5649_20_unwrap():
    # test vector from RFC 5649 - 20 octets
    # https://datatracker.ietf.org/doc/html/rfc5649#section-6
    algo = "A192KW"
    kek = binascii.unhexlify("5840DF6E29B02AF1AB493B705BF16EA1AE8338F4DCC176A8")
    cipher = binascii.unhexlify("138BDEAA9B8FA7FC61F97742E72248EE5AE6AE5360D1AE6A5F54F373FA543B6A")
    plain = binascii.unhexlify("C37B7E6492584340BED12207808941155068F738")
    aes = AESKey(kek, algo)
    asserts.eq(aes.unwrap(cipher, headers={"with_padding": True}, enc_alg=algo), plain)


def test_pbkdf2_hmac_aes_key_wrapped():
    encryption_key = bytes('3800321e74ff4334bbb8feb815195592', encoding='utf-8')
    payload = json.decode('{"emailAddress":"","countryCode":"null","header":{"prefixNumber":"544288","eventType":"NWC","instId":"B9","eventId":"e64ef50c-d726-4cdb-a53e-0cb450e74846","version":"1.0","activityType":"CARD_OPEN","cardNumber":"eyJwMnMiOiI2VHBsdGxYc0gxTlZNRTdQSzZ3OUZPdG0xMFh1TDdQcVpxV1laZFltWE9zPSIsInAyYyI6MTAwMCwiZW5jIjoiQTI1NkdDTSIsImFsZyI6IlBCRVMyLUhTNTEyK0EyNTZLVyJ9.PNSr-dFdQLIDPYEywGZElEwCv3b9RVvPUufGE8xPlyB1q5cV5EOF0A==.N7ye54PShluyDCPW.nHpCVtaZbY_KSLB_Lt3_nA==.PfpX0v3M82HeSOTr","eventTimestamp":"2021-07-29T00:27:07.967"},"mobilePhoneNumber":"","expirationDate":"0829","members":[{"lastName":"UAT","isPrimary":true,"firstName":"TEST"}]}')
    header = payload['header']
    jwe_string = header['cardNumber']

    parts = jwe_string.split('.')
    hdrBytes = base64.urlsafe_b64decode(parts[0])
    ekBytes = base64.urlsafe_b64decode(parts[1] + '==')
    ivBytes = base64.urlsafe_b64decode(parts[2])
    ctBytes = base64.urlsafe_b64decode(parts[3] + '==')
    atBytes = base64.urlsafe_b64decode(parts[4])

    json_header = json.loads(hdrBytes.decode("utf-8"))
    salt = base64.urlsafe_b64decode(json_header['p2s'] + '==')
    iteration_count = json_header['p2c']

    password = bytes(encryption_key, encoding='utf-8')
    derived_key = PBKDF2(password, salt, 32, count=iteration_count, hmac_hash_module=SHA512)
    key = jwk.construct(derived_key, 'A256KW')
    cek = key.unwrap(ekBytes, headers=json_header, enc_alg=json_header["enc"])
    # so dangerous..
    cipher = AES.new(cek, AES.MODE_GCM, nonce=ivBytes, mac_len=12)
    d = cipher.decrypt(ctBytes) # does not verify or authenticate the tag..
    asserts.assert_that(base64.b64encode(d)).is_equal_to(b'NTQ0Mjg4MzAxMDAxNjc5Mg==')

def test_sign_with_rsa():
    rsa_private_key = """-----BEGIN RSA PRIVATE KEY-----
                        MIIEogIBAAKCAQEAnzJokBF2RIiyOIS8iCJmUKDvg1kFvs+elNeNWb9s+xfJk/ka
                        pqvs4b5wFzLGmZbmWeWWw4reGyYFk1bmfCKbC+58yciH/Iy2hwOWsvL1MLmQG0lo
                        wD9joD3uuvFbaPecyHQ9VLE7SBx2w8uffn/PNUtu7/h+9vLP+RFOCIkDVdfHRD1F
                        Llx1rtXVecpfXu+RE5fVv+FIitzB3VTE3ZBO/oBYp9K/M9xELQ9G9bKpK3Jddw/Z
                        MJ2rF3Kf6SQHY0907LA9RTIbdg4MCBNdzHeOE42KliOjsxs6ZqFkeIBRuEINlkaC
                        Y0/Ki+q2pEgHwgIdlfCYC+qA0xOJsk01rhV6LQIDAQABAoIBABjKA6gzX+QqGP5e
                        BPF50c7KUKF2UkO1Fq98pSENgs3SlWv6RSEg8mDwg4nJOU2fRr1G1+QZEp7Nm96m
                        kHtR9Cy2dS4CkNJNovqBH4078dwleQAWvT36YOwJ2Mu5W4+LxxkbqJUwh9ehvwic
                        A3jyM/TLiznIyxRvGN1iyXDgzOkn0mAOHL+m+iwiqVb9xS/7NELbOFxYxyzlqmUn
                        HC61t2zLRS2aUCGv+HVfhfdBo7jNsH9NnXqNU2ghV9oi9Ssgv8xyQC4nlRmHe1+s
                        vw4ZFKT1Ybjyhj3rG1LXyDJHmZwc/ktNXSuPHiypwM8KLU6Tx2uBheGwKVciVbbV
                        WCvnbKECgYEA3AI22eXKc7LgjunQNeIn00W6IJV3zDVt2ELMymYTVpUKILpZkBJG
                        l8QrMdkyhdMPcRbQhJL427uNWWkKiuRNVWOwU8Yr6/eOjQ7qBmrNRrjL3STt4uW4
                        pFd1qO9gjvdJ7PdL7sf0LLGeqQ0jfguSCN3L19I33YTsjgyQ8HktTMkCgYEAuT1y
                        fk3rpLzYt1TfV0ZvSHOYcMSJDjcoo9hBJyYjPDsi1YtmoJgP4zalCJLwssZAbENl
                        XbFQ38dYF+EJH20GHRjQooS5ffWkBfSrCpkTfqrno+bBbQE6hKapNeCT/Fubk25Y
                        nHbM/Fswf0JUBDzR6lej4C3oC3QwvsKVIu7tiEUCgYBtdJiPbZh8WUkJMOAKfrpr
                        jOwd297e9NHyXsF+DKygTcPdJnj6iW0fglQvF6zgEXJERCJ3Ypt2zmdzTSQWl6C1
                        08Pc2eHuIpqEKSBbEvWPss6R9hZ35Oworu51nqo+Vl8sCph3cnlTZwbVehjnU6BS
                        0u3gMNDkX4ZE4ZGWYeMcqQKBgCmHmjSw+YSshhXMJGpnsylWKTYXCfcy7JyXLjw9
                        s6acR3oCz9ZvYRh6ttNORkJ+ahEbpw0zZMNW6Owpmqb+BWHen1/gS8numYBwUyyE
                        FzfNzzMS2Ai9PsZgqw0WNXddfmq2UY16oQhu1Veioj10+UcFsQrgn+Z5fTg3XcJA
                        OObVAoGAPk7GL8WELvJGBNYcLFZ4TxgtD7uPxkA+PwMMv/xwHjZjiCKsL5HlYYuD
                        Qgz4dQy0OW02mfC5LDkCHzOO3m5suiodTJpDiU0arcbtXh3QW3IWwKXMwG2pDLsh
                        vyExhzkL/cPs+hXX5Uvuwh3RATzdlGx9Vf6NDaF20EvegzK42wY=
                        -----END RSA PRIVATE KEY-----"""
    header = {
        "typ": "JWT",
        "alg": 'RS256'
    }

    json_header = bytes(json.dumps(header,), 'utf-8')

    headers = base64url_encode(json_header)
    encoded_payload = base64url_encode(bytes(json.dumps({'a': 'b'}), 'utf-8'))

    signing_input = bytes([0x2e]).join([headers, encoded_payload])
    k = jwk.construct(rsa_private_key, 'RS256')
    sign = k.sign(signing_input)
    encoded_signature = base64url_encode(sign)
    
    encoded_string = b".".join([headers, encoded_payload, encoded_signature])

def test_sign_with_ecc():
    es_private_key = """-----BEGIN EC PRIVATE KEY-----
                        MHcCAQEEIDcv6AeZhfUH20LCzlHKr6SZyWK5LnQQrWN5TigDmTcwoAoGCCqGSM49
                        AwEHoUQDQgAEQ4+x/eCyT+7mnjPgT0iIf7PBB2W7YHDi3qvbNMZm+Its/M+6eCGk
                        qhcJT26DWf2EMncfCX4okTxVST/r/ohGXA==
                        -----END EC PRIVATE KEY-----"""
    header = {
        "typ": "JWT",
        "alg": 'ES256'
    }

    json_header = bytes(json.dumps(header), 'utf-8')

    headers = base64url_encode(json_header)
    encoded_payload = base64url_encode(bytes(json.dumps({'a': 'b'}), 'utf-8'))

    signing_input = bytes([0x2e]).join([headers, encoded_payload])
    k = jwk.construct(es_private_key, 'ES256')
    sign = k.sign(signing_input)
    encoded_signature = base64url_encode(sign)
    
    encoded_string = b".".join([headers, encoded_payload, encoded_signature])


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
    _suite.addTest(unittest.FunctionTestCase(test_vector_RFC_3394_unwrap))
    _suite.addTest(unittest.FunctionTestCase(test_vector_RFC_5649_7_wrap))
    _suite.addTest(unittest.FunctionTestCase(test_vector_RFC_5649_7_unwrap))
    _suite.addTest(unittest.FunctionTestCase(test_vector_RFC_5649_20_wrap))
    _suite.addTest(unittest.FunctionTestCase(test_vector_RFC_5649_20_unwrap))
    _suite.addTest(unittest.FunctionTestCase(test_pbkdf2_hmac_aes_key_wrapped))
    _suite.addTest(unittest.FunctionTestCase(test_sign_with_ecc))
    _suite.addTest(unittest.FunctionTestCase(test_sign_with_rsa))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())