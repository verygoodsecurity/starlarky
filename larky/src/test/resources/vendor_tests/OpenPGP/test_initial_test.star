load("@stdlib//codecs", codecs="codecs")
load("@stdlib//io/StringIO", StringIO="StringIO")
load("@stdlib//unittest", "unittest")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")
load("@vendor//OpenPGP", OpenPGP="OpenPGP")
load("@vendor//OpenPGP/Crypto", Crypto="Crypto")
load("@vendor//asserts",  "asserts")

load("@stdlib//base64", base64="base64")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//bz2", bz2="bz2")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//hashlib", hashlib="hashlib")
load("@stdlib//itertools", itertools="itertools")
load("@stdlib//larky", larky="larky")
load("@stdlib//math", ceil="ceil", floor="floor", log="log")
load("@stdlib//re", re="re")
load("@stdlib//struct", pack="pack", unpack="unpack")
load("@stdlib//textwrap", textwrap="textwrap")
load("@stdlib//zlib", zlib="zlib")
load("@vendor//option/result", Error="Error")


load("data_test_fixtures", get_file_contents="get_file_contents")

WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION

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