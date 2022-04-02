load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//Crypto/Util", Util="Util")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//OpenPGP", OpenPGP="OpenPGP")
load("@vendor//OpenPGP/Crypto", Crypto="Crypto")
load("@vendor//asserts", asserts="asserts")

# fixtures

load("data_test_fixtures", get_file_contents="get_file_contents")


WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION


def oneMessage(pkey, path):
    pkeyM = OpenPGP.Message.parse(
        open(os.path.dirname(__file__) + "/data/" + pkey, "rb").read()
    )
    m = OpenPGP.Message.parse(
        open(os.path.dirname(__file__) + "/data/" + path, "rb").read()
    )
    verify = OpenPGP.Crypto.Wrapper(pkeyM)
    print(m.signatures())
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
        open(os.path.dirname(__file__) + "/data/helloKey.gpg", "rb").read()
    )
    data = OpenPGP.LiteralDataPacket("This is text.", "u", "stuff.txt")
    sign = OpenPGP.Crypto.Wrapper(wkey)
    m = sign.sign(data).to_bytes()
    reparsedM = OpenPGP.Message.parse(m)
    asserts.assert_that(sign.verify(reparsedM)).is_equal_to(reparsedM.signatures())


def TestMessageVerification_testSigningMessagesDSA():
    wkey = OpenPGP.Message.parse(
        open(os.path.dirname(__file__) + "/data/secring.gpg", "rb").read()
    )
    data = OpenPGP.LiteralDataPacket("This is text.", "u", "stuff.txt")
    dsa = OpenPGP.Crypto.Wrapper(wkey).private_key("7F69FA376B020509")
    m = OpenPGP.Crypto.Wrapper(data).sign(dsa, "SHA512", "7F69FA376B020509").to_bytes()
    reparsedM = OpenPGP.Message.parse(m)
    asserts.assert_that(OpenPGP.Crypto.Wrapper(wkey).verify(reparsedM)).is_equal_to(
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
        open(os.path.dirname(__file__) + "/data/" + path, "rb").read()
    )
    verify = OpenPGP.Crypto.Wrapper(m)
    asserts.assert_that(verify.verify(m)).is_equal_to(m.signatures())


def TestKeyVerification_testSigningKeysRSA():
    k = Crypto.PublicKey.RSA.generate(1024)

    nkey = OpenPGP.SecretKeyPacket(
        (
            Crypto.Util.number.long_to_bytes(k.n),
            Crypto.Util.number.long_to_bytes(k.e),
            Crypto.Util.number.long_to_bytes(k.d),
            Crypto.Util.number.long_to_bytes(k.p),
            Crypto.Util.number.long_to_bytes(k.q),
            Crypto.Util.number.long_to_bytes(k.u),
        )
    )

    uid = OpenPGP.UserIDPacket("Test <test@example.com>")

    wkey = OpenPGP.Crypto.Wrapper(nkey)
    m = wkey.sign_key_userid([nkey, uid]).to_bytes()
    reparsedM = OpenPGP.Message.parse(m)

    asserts.assert_that(wkey.verify(reparsedM)).is_equal_to(reparsedM.signatures())


def TestKeyVerification_testHelloKey():
    oneKeyRSA("helloKey.gpg")


def oneSymmetric(pss, cnt, path):
    m = OpenPGP.Message.parse(
        open(os.path.dirname(__file__) + "/data/" + path, "rb").read()
    )
    m2 = OpenPGP.Crypto.Wrapper(m).decrypt_symmetric(pss)
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
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
        open(os.path.dirname(__file__) + "/data/encryptedSecretKey.gpg", "rb").read()
    )
    skey = OpenPGP.Crypto.Wrapper(key[0]).decrypt_secret_key("hello")
    asserts.assert_that(not (not skey)).is_equal_to(True)


def TestDecryption_testDecryptAsymmetric():
    m = OpenPGP.Message.parse(
        open(os.path.dirname(__file__) + "/data/hello.gpg", "rb").read()
    )
    key = OpenPGP.Message.parse(
        open(os.path.dirname(__file__) + "/data/helloKey.gpg", "rb").read()
    )
    m2 = OpenPGP.Crypto.Wrapper(key).decrypt(m)
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if not builtins.isinstance(m2[0], OpenPGP.CompressedDataPacket):
            break
        m2 = m2[0].data
    for p in m2:
        if builtins.isinstance(p, OpenPGP.LiteralDataPacket):
            asserts.assert_that(p.data).is_equal_to(b"hello\n")


def TestEncryption_testEncryptSymmetric():
    data = OpenPGP.LiteralDataPacket("This is text.", "u", "stuff.txt")
    encrypted = OpenPGP.Crypto.Wrapper(OpenPGP.Message([data])).encrypt("secret")
    decrypted = OpenPGP.Crypto.Wrapper(encrypted).decrypt_symmetric("secret")
    asserts.assert_that(decrypted[0].data).is_equal_to(b"This is text.")


def TestEncryption_testEncryptAsymmetric():
    key = OpenPGP.Message.parse(
        open(os.path.dirname(__file__) + "/data/helloKey.gpg", "rb").read()
    )
    data = OpenPGP.LiteralDataPacket("This is text.", "u", "stuff.txt")
    encrypted = OpenPGP.Crypto.Wrapper(OpenPGP.Message([data])).encrypt(key)
    decryptor = OpenPGP.Crypto.Wrapper(key)
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
        unittest.FunctionTestCase(TestMessageVerification_testCompressedSigBzip2)
    )
    _suite.addTest(
        unittest.FunctionTestCase(TestMessageVerification_testSigningMessagesRSA)
    )
    _suite.addTest(
        unittest.FunctionTestCase(TestMessageVerification_testSigningMessagesDSA)
    )
    _suite.addTest(
        unittest.FunctionTestCase(TestMessageVerification_testUncompressedOpsDSA)
    )
    _suite.addTest(
        unittest.FunctionTestCase(TestMessageVerification_testUncompressedOpsDSAsha384)
    )
    _suite.addTest(unittest.FunctionTestCase(TestKeyVerification_testSigningKeysRSA))
    _suite.addTest(unittest.FunctionTestCase(TestKeyVerification_testHelloKey))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecryptAES))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecryptNoMDC))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecrypt3DES))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecryptBlowfish))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecryptCAST5))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecryptSessionKey))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecryptSecretKey))
    _suite.addTest(unittest.FunctionTestCase(TestDecryption_testDecryptAsymmetric))
    _suite.addTest(unittest.FunctionTestCase(TestEncryption_testEncryptSymmetric))
    _suite.addTest(unittest.FunctionTestCase(TestEncryption_testEncryptAsymmetric))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
