load("@vendor//Crypto/Cipher/AES", "AES")
load("@stdlib//binascii", "hexlify")
def process(data):
    key = b"\x01" * 16
    nonce = b"\x02" * 12
    cipher = AES.new(key, AES.MODE_GCM, nonce=nonce)
    ct, tag = cipher.encrypt_and_digest(data)
    return hexlify(ct + tag)
output = process(b"4111111111111111")
