load("@stdlib//unittest","unittest")
load("@stdlib//json","json")
load("@stdlib//types", "types")
load("@stdlib//random", "random")

load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Util/Counter", Counter="Counter")
load("@vendor//asserts","asserts")
    
def test_kateryna_stuff():
    key = b'Sixteen byte key'
    ctr = Counter.new(AES.block_size * 8)
    crypto = AES.new(key, AES.MODE_CTR, counter=ctr)
    encrypted = crypto.encrypt(b"TEST PAYLOAD!")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_kateryna_stuff))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
