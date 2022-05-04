load("@stdlib//larky", "larky")
load("@stdlib//builtins", "builtins")
load("@stdlib//json", json="json")
load("@stdlib//base64", base64="base64")
load("@stdlib//jcrypto", _JCrypto="jcrypto")


load("@vendor//Crypto/Cipher/PKCS1_OAEP", PKCS1_OAEP="PKCS1_OAEP")
load("@vendor//Crypto/Cipher/AES", AES="AES")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")

def get_public_keys():
    keys = _JCrypto.Chase.get_keys()
    print(keys)
    return keys

def decrypt(jwe_bytes):
    decrypted = _JCrypto.Chase.decrypt(jwe_bytes)
    return decrypted

jwk = larky.struct(
  get_keys=get_keys,
  decrypt=decrypt
)
