load("@stdlib//larky", larky="larky")
load("@stdlib//jcrypto", _JCrypto="jcrypto")


def encrypt(plaintext, key, encryption="A256GCM",
            algorithm="A256GCMKW", zip=None, cty=None, kid=None):
    return _JCrypto.JWE.encrypt(plaintext, key, encryption, algorithm)


def decrypt(jwe_str, key):
    return _JCrypto.JWE.decrypt(jwe_str, key)


jwe = larky.struct(
    encrypt=encrypt,
    decrypt=decrypt
)
