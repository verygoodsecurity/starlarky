# A CTR counter configuration built at load time (a dict), and a fresh cipher per call.
load("@stdlib//binascii", "binascii")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Util/Counter", Counter="Counter")

_COUNTER = Counter.new(128, initial_value=1)

def digest(data):
    cipher = AES.new(b"0123456789abcdef", AES.MODE_CTR, counter=_COUNTER)
    return str(binascii.hexlify(cipher.encrypt(data)))
