# Port of python-jose (https://github.com/mpdavis/python-jose).
#
# Upstream versions:
#
# - jwt.star: python-jose 3.5.0 (tag 3.5.0, commit
#   018b310ddb8b50dcfd09a0c152117835a21dd656), jose/jwt.py. See its header.
# - Every other file (jws, jwk, jwe, utils, exceptions, constants, backends):
#   ported in 2021-2022 without a recorded version. They are consistent
#   with python-jose's master branch between commits
#   89c3d41bc6c60ad6f7d135f14dcd963cef633637 (2020-09-07, "Support loading a
#   JWK object directly", which jws.star's Key handling includes) and
#   8572088b76f7c0d31068f38aeca0d4273ae8bc8f (2020-12-07, the parent of
#   3d3d3c2, which removed the PyCrypto backend that
#   backends/pycrypto_backend.star ports). Within that range upstream only
#   changed backends/ecdsa_backend.py, which is not ported. No release
#   matches: 3.2.0 has no jwe.py, backends/native.py or AESKey, and 3.3.0
#   dropped the PyCrypto backend.

load("@vendor//jose/exceptions", JOSEError="JOSEError")  # noqa: F401
load("@vendor//jose/exceptions", JWSError="JWSError")  # noqa: F401
load("@vendor//jose/exceptions", ExpiredSignatureError="ExpiredSignatureError")  # noqa: F401
load("@vendor//jose/exceptions", JWTError="JWTError")  # noqa: F401

# Python reaches these submodules with `from jose import jwt`.
load("@vendor//jose/jwk", _jwk="jwk")
load("@vendor//jose/jws", _jws="jws")
load("@vendor//jose/jwt", _jwt="jwt")

jwk = _jwk
jws = _jws
jwt = _jwt
