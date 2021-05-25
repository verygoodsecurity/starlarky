load("@stdlib//larky", larky="larky")
load("@stdlib//jcrypto", _JCrypto="jcrypto")


def serialize_compact(payload, key):
    return _JCrypto.JWE.encrypt(payload, key)


def deserialize_compact(data, key):
    return _JCrypto.JWE.decrypt(data, key)


jwe = larky.struct(
    serialize_compact=serialize_compact,
    deserialize_compact=deserialize_compact
)
