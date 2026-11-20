# An AES-ECB cipher created at load time and used by every call. ECB keeps no chaining state, so
# each call's output depends only on its input; the Java cipher object is still shared.
load("@stdlib//binascii", "binascii")
load("@vendor//Crypto/Cipher/AES", AES="AES")

_CIPHER = AES.new(b"0123456789abcdef", AES.MODE_ECB)

def digest(data):
    return str(binascii.hexlify(_CIPHER.encrypt(data)))
