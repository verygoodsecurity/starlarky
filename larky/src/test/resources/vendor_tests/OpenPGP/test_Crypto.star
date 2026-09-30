load("@stdlib//base64", base64="base64")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//bz2", bz2="bz2")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//hashlib", hashlib="hashlib")
load("@stdlib//io/StringIO", StringIO="StringIO")
load("@stdlib//itertools", itertools="itertools")
load("@stdlib//larky", larky="larky")
load("@stdlib//math", ceil="ceil", floor="floor", log="log")
load("@stdlib//re", re="re")
load("@stdlib//struct", pack="pack", unpack="unpack")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//zlib", zlib="zlib")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Cipher/Blowfish", Blowfish="Blowfish")
load("@vendor//Crypto/Cipher/CAST", CAST="CAST")
load("@vendor//Crypto/Cipher/DES3", DES3="DES3")
load("@vendor//Crypto/Cipher/PKCS1_v1_5", PKCS1_v1_5_Cipher="PKCS1_v1_5_Cipher")
load("@vendor//Crypto/Hash", Hash="Hash")
load("@vendor//Crypto/PublicKey/DSA", DSA="DSA")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/Signature", Signature="Signature")
load("@vendor//Crypto/Util/number", number="number")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")
load("@vendor//OpenPGP", OpenPGP="OpenPGP")
load("@vendor//OpenPGP/Crypto", Crypto="Crypto")
load("@vendor//asserts", asserts="asserts")
load("@vendor//option/result", Error="Error")

# fixtures

load("data_test_fixtures", get_file_contents="get_file_contents")


def oneMessage(pkey, path):
    pkeyM = OpenPGP.Message.parse(
        get_file_contents(pkey)
    )
    m = OpenPGP.Message.parse(
        get_file_contents(path)
    )
    verify = Crypto.Wrapper(pkeyM)
    asserts.assert_that(verify.verify(m)).is_equal_to(m.signatures())


def TestMessageVerification_testUncompressedOpsRSA():
    oneMessage("pubring.gpg", "uncompressed-ops-rsa.gpg")


def TestMessageVerification_testCompressedSig():
    oneMessage("pubring.gpg", "compressedsig.gpg")


def TestMessageVerification_testCompressedSigZLIB():
    oneMessage("pubring.gpg", "compressedsig-zlib.gpg")


def TestMessageVerification_testCompressedSigBzip2():
    oneMessage("pubring.gpg", "compressedsig-bzip2.gpg")


def TestMessageVerification_testSigningMessagesRSA():
    wkey = OpenPGP.Message.parse(
        get_file_contents("helloKey.gpg")
    )
    data = OpenPGP.LiteralDataPacket("This is text.", "u", "stuff.txt")
    sign = Crypto.Wrapper(wkey)
    m = sign.sign(data).to_bytes()
    reparsedM = OpenPGP.Message.parse(m)
    asserts.assert_that(sign.verify(reparsedM)).is_equal_to(reparsedM.signatures())


def TestMessageVerification_testSignAndSHA384EncryptDecryptMessage():
    k = RSA.generate(1024)

    wkey = OpenPGP.SecretKeyPacket(
        (
            number.long_to_bytes(k.n),
            number.long_to_bytes(k.e),
            number.long_to_bytes(k.d),
            number.long_to_bytes(k.p),
            number.long_to_bytes(k.q),
            number.long_to_bytes(k.u),
        )
    )
    data = OpenPGP.LiteralDataPacket("This is text.", "u", "stuff.txt")
    pgp = Crypto.Wrapper(wkey)
    signed_m = pgp.sign(data, hash="SHA384").to_bytes()
    reparsedM = OpenPGP.Message.parse(signed_m)
    asserts.assert_that(pgp.verify(reparsedM)).is_equal_to(reparsedM.signatures())
    encrypted = Crypto.Wrapper(signed_m).encrypt(wkey)
    armored_result = OpenPGP.enarmor(encrypted.to_bytes(), marker='MESSAGE')
    decryptor = Crypto.Wrapper(wkey)
    decrypted = decryptor.decrypt(encrypted)
    asserts.assert_that(decrypted[1].data).is_equal_to(b"This is text.")



# RFC 4880 MPIs carry no leading zero bytes, but an RSA signature or encrypted
# session key is a fixed-length string that starts with one about once in 256
# times. OpenPGP.time() is constant, so signing is deterministic: with helloKey,
# "This is text 144." is signed with a leading zero byte.
def TestMessageVerification_testSignatureWithLeadingZeroByte():
    wkey = OpenPGP.Message.parse(get_file_contents("helloKey.gpg"))
    pgp = Crypto.Wrapper(wkey)
    k = pgp.private_key().size_in_bytes()
    data = OpenPGP.LiteralDataPacket("This is text 144.", "u", "stuff.txt")
    reparsedM = OpenPGP.Message.parse(pgp.sign(data).to_bytes())
    # the signature MPI is shorter than the modulus: the case under test
    asserts.assert_that(len(reparsedM.signatures()[0][1][0].data[0])).is_less_than(k)
    asserts.assert_that(pgp.verify(reparsedM)).is_equal_to(reparsedM.signatures())


def _session_key_block(k, seed):
    """A PKCS#1 v1.5 encryption block for a session key, with filler chosen by seed."""
    sk = bytes([(7 * t + 3) % 256 for t in range(32)])
    m = pack("!B", 9) + sk + pack("!H", OpenPGP.checksum(sk))
    ps = bytes([(seed + 13 * t) % 255 + 1 for t in range(k - 3 - len(m))])
    return b"\x00\x02" + ps + b"\x00" + m, sk


def TestDecryption_testSessionKeyWithLeadingZeroByte():
    pgp = Crypto.Wrapper(OpenPGP.Message.parse(get_file_contents("helloKey.gpg")))
    key = pgp.private_key()
    k = key.size_in_bytes()
    # with helloKey's public key, filler seed 34 encrypts to a string starting with 0
    em, sk = _session_key_block(k, 34)
    c = number.long_to_bytes(pow(number.bytes_to_long(em), key.e, key.n), k)
    asserts.assert_that(c[0]).is_equal_to(0)
    # as an MPI (e.g. read from an AsymmetricSessionKeyPacket), without the zero
    asserts.assert_that(pgp.try_decrypt_session(key, c[1:])).is_equal_to((9, sk))
    # encrypting a message produces a session key packet that decrypts, whatever its first byte
    encrypted = Crypto.Wrapper(OpenPGP.LiteralDataPacket("hi", "u", "f.txt")).encrypt(
        OpenPGP.Message.parse(get_file_contents("helloKey.gpg")))
    asserts.assert_that(pgp.decrypt(encrypted)[0].data).is_equal_to(b"hi")


def TestMessageVerification_testSigningMessagesDSA():
    wkey = OpenPGP.Message.parse(
        get_file_contents("secring.gpg")
    )
    data = OpenPGP.LiteralDataPacket("This is text.", "u", "stuff.txt")
    dsa = Crypto.Wrapper(wkey).private_key("7F69FA376B020509")
    m = Crypto.Wrapper(data).sign(dsa, "SHA512", "7F69FA376B020509").to_bytes()
    reparsedM = OpenPGP.Message.parse(m)
    asserts.assert_that(Crypto.Wrapper(wkey).verify(reparsedM)).is_equal_to(
        reparsedM.signatures()
    )


def TestMessageVerification_testUncompressedOpsDSA():
    maxDiff = None
    oneMessage("pubring.gpg", "uncompressed-ops-dsa.gpg")


def TestMessageVerification_testUncompressedOpsDSAsha384():
    maxDiff = None
    oneMessage("pubring.gpg", "uncompressed-ops-dsa-sha384.txt.gpg")


def TestKeyVerification_oneKeyRSA(path):
    m = OpenPGP.Message.parse(
        get_file_contents(path)
    )
    verify = Crypto.Wrapper(m)
    asserts.assert_that(verify.verify(m)).is_equal_to(m.signatures())


def TestKeyVerification_testSigningKeysRSA():
    k = RSA.generate(1024)

    nkey = OpenPGP.SecretKeyPacket(
        (
            number.long_to_bytes(k.n),
            number.long_to_bytes(k.e),
            number.long_to_bytes(k.d),
            number.long_to_bytes(k.p),
            number.long_to_bytes(k.q),
            number.long_to_bytes(k.u),
        )
    )

    uid = OpenPGP.UserIDPacket("Test <test@example.com>")

    wkey = Crypto.Wrapper(nkey)
    m = wkey.sign_key_userid([nkey, uid]).to_bytes()
    reparsedM = OpenPGP.Message.parse(m)
    # print(reparsedM.signatures())
    asserts.assert_that(wkey.verify(reparsedM)).is_equal_to(reparsedM.signatures())


def TestKeyVerification_testHelloKey():
    TestKeyVerification_oneKeyRSA("helloKey.gpg")


def oneSymmetric(pss, cnt, path):
    m = OpenPGP.Message.parse(
        get_file_contents(path)
    )
    m2 = Crypto.Wrapper(m).decrypt_symmetric(pss)
    for _while_ in larky.while_true():
        if not builtins.isinstance(m2[0], OpenPGP.CompressedDataPacket):
            break
        m2 = m2[0].data
    for p in m2:
        if builtins.isinstance(p, OpenPGP.LiteralDataPacket):
            asserts.assert_that(p.data).is_equal_to(cnt)


def TestDecryption_testDecryptAES():
    oneSymmetric("hello", b"PGP\n", "symmetric-aes.gpg")


def TestDecryption_testDecryptNoMDC():
    oneSymmetric("hello", b"PGP\n", "symmetric-no-mdc.gpg")


def TestDecryption_testDecrypt3DES():
    oneSymmetric("hello", b"PGP\n", "symmetric-3des.gpg")


def TestDecryption_testDecryptBlowfish():
    oneSymmetric("hello", b"PGP\n", "symmetric-blowfish.gpg")


def TestDecryption_testDecryptCAST5():
    oneSymmetric("hello", b"PGP\n", "symmetric-cast5.gpg")


def TestDecryption_testDecryptSessionKey():
    oneSymmetric("hello", b"PGP\n", "symmetric-with-session-key.gpg")


def TestDecryption_testDecryptSecretKey():
    key = OpenPGP.Message.parse(
        get_file_contents("encryptedSecretKey.gpg")
    )
    skey = Crypto.Wrapper(key[0]).decrypt_secret_key("hello")
    asserts.assert_that(not (not skey)).is_equal_to(True)


def TestDecryption_testDecryptAsymmetric():
    m = OpenPGP.Message.parse(
        get_file_contents("hello.gpg")
    )
    key = OpenPGP.Message.parse(
        get_file_contents("helloKey.gpg")
    )
    m2 = Crypto.Wrapper(key).decrypt(m)
    for _while_ in larky.while_true():
        if not builtins.isinstance(m2[0], OpenPGP.CompressedDataPacket):
            break
        m2 = m2[0].data
    for p in m2:
        if builtins.isinstance(p, OpenPGP.LiteralDataPacket):
            asserts.assert_that(p.data).is_equal_to(b"hello\n")


def TestEncryption_testEncryptSymmetric():
    data = OpenPGP.LiteralDataPacket("This is text.", "u", "stuff.txt")
    encrypted = Crypto.Wrapper(OpenPGP.Message([data])).encrypt("secret")
    decrypted = Crypto.Wrapper(encrypted).decrypt_symmetric("secret")
    asserts.assert_that(decrypted[0].data).is_equal_to(b"This is text.")


def TestEncryption_testEncryptAsymmetric():
    key = OpenPGP.Message.parse(
        get_file_contents("helloKey.gpg")
    )
    data = OpenPGP.LiteralDataPacket("This is text.", "u", "stuff.txt")
    encrypted = Crypto.Wrapper(OpenPGP.Message([data])).encrypt(key)
    decryptor = Crypto.Wrapper(key)
    decrypted = decryptor.decrypt(encrypted)
    asserts.assert_that(decrypted[0].data).is_equal_to(b"This is text.")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(
        unittest.FunctionTestCase(TestMessageVerification_testUncompressedOpsRSA)
    )
    _suite.addTest(unittest.FunctionTestCase(TestMessageVerification_testCompressedSig))
    _suite.addTest(
        unittest.FunctionTestCase(TestMessageVerification_testCompressedSigZLIB)
    )
    _suite.addTest(
        # not implemented yet.
        unittest.expectedFailure(
            unittest.FunctionTestCase(TestMessageVerification_testCompressedSigBzip2))
    )
    _suite.addTest(
        unittest.FunctionTestCase(TestMessageVerification_testSigningMessagesRSA)
    )
    _suite.addTest(
        unittest.FunctionTestCase(TestMessageVerification_testSignAndSHA384EncryptDecryptMessage)
    )
    _suite.addTest(unittest.FunctionTestCase(TestMessageVerification_testSignatureWithLeadingZeroByte))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testSessionKeyWithLeadingZeroByte))

    # 👇FAILS BUT WILL PASS WITH DSA 👇
    # _suite.addTest(
    #     unittest.FunctionTestCase(TestMessageVerification_testSigningMessagesDSA)
    # )
    # _suite.addTest(
    #     unittest.FunctionTestCase(TestMessageVerification_testUncompressedOpsDSA)
    # )
    # _suite.addTest(
    #     unittest.FunctionTestCase(TestMessageVerification_testUncompressedOpsDSAsha384)
    # )
    # 👆FAILS BUT WILL PASS WITH DSA👆

    _suite.addTest(unittest.FunctionTestCase(TestKeyVerification_testSigningKeysRSA))
    _suite.addTest(unittest.FunctionTestCase(TestKeyVerification_testHelloKey))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecryptAES))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecryptNoMDC))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecrypt3DES))
    _suite.addTest(
        # expected failure (no blowfish support)
        unittest.expectedFailure(
            unittest.FunctionTestCase(TestDecryption_testDecryptBlowfish)))
    _suite.addTest(
        # expected failure (no CAST5 support)
        unittest.expectedFailure(
            unittest.FunctionTestCase(TestDecryption_testDecryptCAST5)))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecryptSessionKey))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecryptSecretKey))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecryptAsymmetric))
    _suite.addTest(unittest.FunctionTestCase(TestEncryption_testEncryptSymmetric))
    _suite.addTest(unittest.FunctionTestCase(TestEncryption_testEncryptAsymmetric))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
