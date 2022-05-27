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
