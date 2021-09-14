load("@stdlib//unittest", unittest="unittest")
load("@stdlib//binascii", binascii="binascii")
load("@stdlib//types", types="types")
load("@stdlib//re", re="re")
load("@vendor//asserts", asserts="asserts")
load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/PublicKey/ECC", ECC="ECC")
load("@vendor//Crypto/Math/Numbers", Integer="Integer")
load("@vendor//Crypto/Util/number", bytes_to_long="bytes_to_long", inverse="inverse")


eq = asserts.eq

def ecc_basic_test():
    key = ECC.generate(curve='p256')
    print('ECC key:', key.export_key(format='PEM'))

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(ecc_basic_test))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_suite())