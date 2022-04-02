load("@stdlib//codecs", codecs="codecs")
load("@stdlib//io/StringIO", StringIO="StringIO")
load("@stdlib//unittest", "unittest")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")
load("@vendor//OpenPGP", OpenPGP="OpenPGP")
load("@vendor//OpenPGP/Crypto", Crypto="Crypto")
load("@vendor//asserts",  "asserts")

load("data_test_fixtures", get_file_contents="get_file_contents")


def PGP_test():
    # the test key below contains both private key and public key data
    message = OpenPGP.Message().parse(get_file_contents("helloKey.gpg"))
    print(message)
    wkey = message[0]
    # wkey should be of <class 'OpenPGP.SecretKeyPacket'>
    print('parsed key:', wkey)

    data = OpenPGP.LiteralDataPacket('This is text.', 'u', 'stuff.txt', 1000000)
    encrypt = Crypto.Wrapper(data)
    encrypted = encrypt.encrypt([wkey])

    print('pgp encrypted:', encrypted)
    print('byte arr:', bytearray([21]))

#     # Now decrypt it with the same key
    decryptor = Crypto.Wrapper(wkey)
    decrypted = decryptor.decrypt(encrypted)

    # print('pgp decrypted:', decrypted)

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(PGP_test))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())