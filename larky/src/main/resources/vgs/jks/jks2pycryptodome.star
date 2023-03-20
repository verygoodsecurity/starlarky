load("@stdlib//larky", larky="larky")

load("@vendor//Crypto/Util/py3compat", tobytes="tobytes")
load("@vendor//cryptography/hazmat/backends/pycryptodome", backend="backend", RSAPrivateKey="RSAPrivateKey", RSA="RSA", Certificate="Certificate")
load("@vendor//cryptography/utils", utils="utils")

load("@vgs//jks", jks="jks")

def load_key_and_certificates(keystore_data, keystore_password, key_alias, key_password):
    if keystore_password != None:
        utils._check_byteslike("keystore_password", keystore_password)
    key, cert, additional_certificates = jks.JKS.load_key_and_certificates(tobytes(keystore_data), keystore_password, key_alias, key_password)
    b = backend()
    return RSAPrivateKey(b, key, RSA.import_key(key.private_key())), Certificate(b, cert), [Certificate(b, c) for c in additional_certificates]

jks2pycryptodome = larky.struct(
    __name__='jks2pycryptodome',
    load_key_and_certificates=load_key_and_certificates
)
