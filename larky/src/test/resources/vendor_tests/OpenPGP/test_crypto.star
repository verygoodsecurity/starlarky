# load("@vendor//OpenPGP", Crypto="Crypto")
# load("@vendor//OpenPGP", OpenPGP="OpenPGP")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts",  "asserts")


# def PGP_test():

#     wkey = OpenPGP.Message.parse(open('key', 'rb').read())[0]

#     data = OpenPGP.LiteralDataPacket('This is text.', 'u', 'stuff.txt')
#     encrypt = OpenPGP.Crypto.Wrapper(data)
#     encrypted = encrypt.encrypt([wkey])

#     print(list(encrypted))

#     # Now decrypt it with the same key
#     decryptor = OpenPGP.Crypto.Wrapper(wkey)
#     decrypted = decryptor.decrypt(encrypted)

#     print(list(decrypted))

def _testsuite():
    _suite = unittest.TestSuite()
    # _suite.addTest(unittest.FunctionTestCase(PGP_test))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())