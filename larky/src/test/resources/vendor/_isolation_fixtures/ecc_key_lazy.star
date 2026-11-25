# An EC key built at load time whose public point is computed lazily by the first call: the
# key's struct is frozen by then, so the call fails, with the cache on or off.
load("@stdlib//base64", "base64")
load("@stdlib//binascii", "binascii")
load("@vendor//Crypto/PublicKey/ECC", "ECC")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")
load("@vendor//cryptography/hazmat/primitives", "serialization")
load("@vendor//larky_ecdh", "LarkyECDH")

_CLIENT = ECC.construct(curve="P-256", d=1)
_SERVER = ECC.construct(curve="P-256", d=2)

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
