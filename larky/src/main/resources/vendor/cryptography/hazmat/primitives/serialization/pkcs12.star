# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//builtins", builtins="builtins")
load("@vendor//cryptography/x509", x509="x509")
load("@vendor//cryptography/hazmat/backends", backends="backends")
load("@vendor//cryptography/hazmat/primitives", serialization="serialization")
load("@vendor//cryptography/hazmat/primitives/asymmetric",
     rsa="rsa",
     # dsa="dsa",
     # ec="ec",
     )
load("@vendor//option/result", Error="Error")


_get_backend = backends._get_backend


# type: Union[rsa.RSAPrivateKey, dsa.DSAPrivateKey, ec.EllipticCurvePrivateKey]


def load_key_and_certificates(
    data,
    password,
    backend = None,
):
    backend = _get_backend(backend)
    return backend.load_key_and_certificates_from_pkcs12(data, password)


def serialize_key_and_certificates(
    name,
    key,
    cert,
    cas,
    encryption_algorithm,
):
    if key != None and not any([
        builtins.isinstance(key, rsa.RSAPrivateKey),
        # TODO(mahmoudimus: do this.
        # builtins.isinstance(key, dsa.DSAPrivateKey),
        # builtins.isinstance(key, ec.EllipticCurvePrivateKey),
    ]):
        fail("TypeError: Key must be RSA, DSA, or EllipticCurve private key.")
    if cert != None and not builtins.isinstance(cert, x509.Certificate):
        fail("TypeError: cert must be a certificate")

    if cas != None:
        cas = list(cas)
        if not all([builtins.isinstance(val, x509.Certificate) for val in cas]):
            fail("TypeError: all values in cas must be certificates")

    if not encryption_algorithm:
        fail("Key encryption algorithm must be a " +
             "KeySerializationEncryption instance")

    if key == None and cert == None and not cas:
        fail("ValueError: You must supply at least one of " +
             "key, cert, or cas")

    backend = _get_backend(None)
    return backend.serialize_key_and_certificates_to_pkcs12(
        name, key, cert, cas, encryption_algorithm
    )


pkcs12 = larky.struct(
    __name__='pkcs12',
    load_key_and_certificates=load_key_and_certificates,
    serialize_key_and_certificates=serialize_key_and_certificates,
)
