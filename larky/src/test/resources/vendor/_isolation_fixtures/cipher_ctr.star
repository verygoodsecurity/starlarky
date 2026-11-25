# An AES-CTR cipher created at load time: each encrypt advances its keystream.
load("@stdlib//binascii", "binascii")
load("@vendor//Crypto/Cipher/AES", AES="AES")

_CIPHER = AES.new(b"0123456789abcdef", AES.MODE_CTR, nonce=b"01234567")

def digest(data):
    return str(binascii.hexlify(_CIPHER.encrypt(data)))
