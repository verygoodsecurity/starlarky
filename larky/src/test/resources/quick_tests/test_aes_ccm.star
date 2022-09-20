load("@stdlib//base64", base64="base64")
load("@stdlib//binascii", binascii="binascii")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//json", json="json")
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Cipher/PKCS1_v1_5", PKCS1_v1_5="PKCS1_v1_5_Cipher")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//Crypto/Random", get_random_bytes="get_random_bytes")
load("@vendor//asserts", asserts="asserts")
load("@vgs//vault", vault="vault")


TAG_LENGTH = 8

session_key = b'vbfhg768ghvbfhg768ghvbfhg768gh12'
nonce = b'asd456fgh012'

python_GCM = 'YXNkNDU2ZmdoMDEy4E3epAdcjzv6QciTdQ=='
python_CCM = 'YXNkNDU2ZmdoMDEy+seumP9ZG20KkyyHTg=='


def process():
    # GCM:
    cipher_aes = AES.new(session_key, AES.MODE_GCM, nonce=nonce, mac_len=TAG_LENGTH)
    ciphertext, tag = cipher_aes.encrypt_and_digest(builtins.bytes('12345', 'utf-8'))
    ciphertext_tag = b"".join([cipher_aes.nonce, ciphertext, tag])
    ciphertext_b64_GCM = base64.b64encode(ciphertext_tag).decode('utf-8')

    # CCM:
    cipher_aes = AES.new(session_key, AES.MODE_CCM, nonce=nonce, mac_len=TAG_LENGTH)
    ciphertext, tag = cipher_aes.encrypt_and_digest(builtins.bytes('12345', 'utf-8'))
    ciphertext_tag = b"".join([cipher_aes.nonce, ciphertext, tag])
    ciphertext_b64_CCM = base64.b64encode(ciphertext_tag).decode('utf-8')

    # output = "\n".join([
    #     '>>> Result GCM:   ' + ciphertext_b64_GCM,
    #     '>>> Expected GCM: ' + python_GCM,
    #     '>>> Result CCM:   ' + ciphertext_b64_CCM,
    #     '>>> Expected CCM: ' + python_CCM
    #     ])
    # print('\n\n' + output + '\n')
    asserts.assert_that(ciphertext_b64_GCM).is_equal_to(python_GCM)
    asserts.assert_that(ciphertext_b64_CCM).is_equal_to(python_CCM)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(process))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())