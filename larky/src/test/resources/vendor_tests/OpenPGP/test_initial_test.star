load("@stdlib//base64", base64="base64")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//bz2", bz2="bz2")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//hashlib", hashlib="hashlib")
load("@stdlib//io/StringIO", StringIO="StringIO")
load("@stdlib//itertools", itertools="itertools")
load("@stdlib//larky", larky="larky")
load("@stdlib//math", ceil="ceil", floor="floor", log="log")
load("@stdlib//re", re="re")
load("@stdlib//struct", pack="pack", unpack="unpack")
load("@stdlib//types", types="types")
load("@stdlib//unittest", "unittest")
load("@stdlib//zlib", zlib="zlib")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")
load("@vendor//OpenPGP", OpenPGP="OpenPGP")
load("@vendor//OpenPGP/Crypto", Crypto="Crypto")
load("@vendor//asserts",  "asserts")
load("@vendor//option/result", Error="Error")

load("data_test_fixtures", get_file_contents="get_file_contents")

WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION

# TODO(mahmoudimus): OCCASIONALLY - THIS TEST WILL FAIL BECAUSE OF
#  LINE 397 (key = Random.new().read(key_bytes)) in OpenPGP/Crypto.star
#  which reads a random string.
#  The fix requires a loop to re-generate the key that would not fail the
#  bitlength check.
def simple_PGP_test():
    # the test key below contains both private key and public key data
    key = OpenPGP.Message.parse(get_file_contents("helloKey.gpg"))
    data = OpenPGP.LiteralDataPacket("This is text.", "u", "stuff.txt")
    message = OpenPGP.Message([data])
    wrapper = Crypto.Wrapper(message)
    encrypted = wrapper.encrypt(key)
    decryptor = Crypto.Wrapper(key)
    decrypted = decryptor.decrypt(encrypted)
    asserts.assert_that(decrypted[0].data).is_equal_to(b"This is text.")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(simple_PGP_test))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())