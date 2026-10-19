# Ported from python-jose jose/jwk.py; consistent with its master branch between
# 89c3d41bc6c60ad6f7d135f14dcd963cef633637 and 8572088b76f7c0d31068f38aeca0d4273ae8bc8f
# (no release; the version was not recorded, see jose/__init__.star).

load("@stdlib//larky", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//types", types="types")
load("@vendor//jose/backends/base", Key="Key")
load("@vendor//jose/constants", ALGORITHMS="ALGORITHMS")
load("@vendor//jose/exceptions", JWKError="JWKError")
load("@vendor//jose/backends",
     RSAKey="RSAKey",    # noqa: F401
     AESKey="AESKey",    # noqa: F401
     DIRKey="DIRKey",    # noqa: F401
     HMACKey="HMACKey",  # noqa: F401
     ECKey="ECKey",      # noqa: F401
)


# python-jose's register_key adds to ALGORITHMS.KEYS and ALGORITHMS.SUPPORTED,
# which Larky freezes when the module loads. Here the keys registered by a
# script are kept for that script's evaluation only.
_REGISTERED = "jose.jwk.register_key"


def _registered():
    return larky.evaluation_dict(_REGISTERED)


def get_key(algorithm):
    registered = _registered()
    if algorithm in registered:
        return registered[algorithm]
    if algorithm in ALGORITHMS.KEYS:
        return ALGORITHMS.KEYS[algorithm]
    elif operator.contains(ALGORITHMS.HMAC, algorithm):  # noqa: F811
        return HMACKey
    elif operator.contains(ALGORITHMS.RSA, algorithm):
        return RSAKey
    elif operator.contains(ALGORITHMS.EC, algorithm):
        return ECKey
    elif operator.contains(ALGORITHMS.AES, algorithm):
        return AESKey
    elif algorithm == ALGORITHMS.DIR:
        return DIRKey
    return None


def register_key(algorithm, key_class):
    """Use key_class(key_data, algorithm) for algorithm, which is then
    supported by jwe and jws, for the rest of this evaluation.

    python-jose requires a subclass of jwk.Key; Larky keys are not classes,
    so key_class must be callable.
    """
    if not types.is_string(algorithm):
        fail("TypeError: algorithm must be a string")
    if not types.is_callable(key_class):
        fail("TypeError: Key class is not callable")
    _registered()[algorithm] = key_class
    return True


def is_supported(algorithm):
    """Whether algorithm is in ALGORITHMS.SUPPORTED or was registered."""
    if not types.is_string(algorithm):
        return False
    return operator.contains(ALGORITHMS.SUPPORTED, algorithm) or algorithm in _registered()


def construct(key_data, algorithm=None):
    """
    Construct a Key object for the given algorithm with the given
    key_data.
    """

    # Allow for pulling the algorithm off of the passed in jwk.
    if not algorithm and types.is_dict(key_data):
        algorithm = key_data.get('alg', None)

    if not algorithm:
        fail("JWKError('Unable to find an algorithm (%s) for key: %s')" % (algorithm, key_data))

    key_class = get_key(algorithm)
    if not key_class:
        fail("JWKError('Unable to find a key class for algorithm (%s) for key: %s')" % (algorithm, key_data))
    return key_class(key_data, algorithm)


jwk = larky.struct(
    __name__='jwk',
    construct=construct,
    register_key=register_key,
    is_supported=is_supported,
    get_key=get_key,
)
