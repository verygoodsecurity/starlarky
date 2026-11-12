# Ported from python-jose jose/backends/__init__.py; consistent with its master branch between
# 89c3d41bc6c60ad6f7d135f14dcd963cef633637 and 8572088b76f7c0d31068f38aeca0d4273ae8bc8f
# (no release; the version was not recorded, see jose/__init__.star).

load("@vendor//jose/backends/pycrypto_backend",
     _AESKey="AESKey",     # noqa: F401
     _RSAKey="RSAKey",     # noqa: F401
     get_random_bytes="get_random_bytes")  # noqa: F401
load("@vendor//jose/backends/native", _HMACKey="HMACKey")  # noqa: F401
load("@vendor//jose/backends/base", _DIRKey="DIRKey")  # noqa: F401
load("@vendor//jose/backends/pycrypto_backend", _ECKey="ECKey")  # noqa: F401


AESKey = _AESKey

RSAKey = _RSAKey

HMACKey = _HMACKey

DIRKey = _DIRKey

ECKey = _ECKey
