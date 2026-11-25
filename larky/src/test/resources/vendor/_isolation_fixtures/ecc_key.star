# An EC key (with its points) built at load time; each call exports its public key and runs an
# ECDH exchange with it.
load("@stdlib//base64", "base64")
load("@stdlib//binascii", "binascii")
load("@vendor//Crypto/PublicKey/ECC", "ECC")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")
load("@vendor//cryptography/hazmat/primitives", "serialization")
load("@vendor//larky_ecdh", "LarkyECDH")

_CLIENT = ECC.construct(curve="P-256", d=1)
_SERVER = ECC.construct(curve="P-256", d=2)

# Compute the public points while the module is still being loaded (ECC caches them in the key).
_CLIENT.public_key()
_SERVER.public_key()

def _der_b64(key):
    return base64.b64encode(key.export_key(format=serialization.Encoding.DER))

def digest(data):
    ecdh = LarkyECDH()
    ecdh.set_public_key(_der_b64(_SERVER.public_key()), "X509")
    ecdh.set_private_key(_der_b64(_CLIENT), "PKCS8")
    h = SHA256.new(ecdh.exchange())
    h.update(data)
    h.update(_der_b64(_CLIENT.public_key()))
    return h.hexdigest()
