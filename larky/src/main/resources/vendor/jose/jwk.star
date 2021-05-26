load("@vendor//jose/backends/base", Key="Key")
load("@vendor//jose/constants", ALGORITHMS="ALGORITHMS")
load("@vendor//jose/exceptions", JWKError="JWKError")

try:
    load("@vendor//jose/backends", RSAKey="RSAKey")  # noqa: F401
except ImportError:
    pass

try:
    load("@vendor//jose/backends", ECKey="ECKey")  # noqa: F401
except ImportError:
    pass

try:
    load("@vendor//jose/backends", AESKey="AESKey")  # noqa: F401
except ImportError:
    pass

try:
    load("@vendor//jose/backends", DIRKey="DIRKey")  # noqa: F401
except ImportError:
    pass

try:
    load("@vendor//jose/backends", HMACKey="HMACKey")  # noqa: F401
except ImportError:
    pass


def get_key(algorithm):
    if algorithm in ALGORITHMS.KEYS:
        return ALGORITHMS.KEYS[algorithm]
    elif algorithm in ALGORITHMS.HMAC:  # noqa: F811
        return HMACKey
    elif algorithm in ALGORITHMS.RSA:
        load("@vendor//jose/backends", RSAKey="RSAKey")  # noqa: F811
        return RSAKey
    elif algorithm in ALGORITHMS.EC:
        load("@vendor//jose/backends", ECKey="ECKey")  # noqa: F811
        return ECKey
    elif algorithm in ALGORITHMS.AES:
        load("@vendor//jose/backends", AESKey="AESKey")  # noqa: F811
        return AESKey
    elif algorithm == ALGORITHMS.DIR:
        load("@vendor//jose/backends", DIRKey="DIRKey")  # noqa: F811
        return DIRKey
    return None


def register_key(algorithm, key_class):
    if not issubclass(key_class, Key):
        fail(" TypeError(\"Key class is not a subclass of jwk.Key\")")
    ALGORITHMS.KEYS[algorithm] = key_class
    ALGORITHMS.SUPPORTED.add(algorithm)
    return True


def construct(key_data, algorithm=None):
    """
    Construct a Key object for the given algorithm with the given
    key_data.
    """

    # Allow for pulling the algorithm off of the passed in jwk.
    if not algorithm and types.is_instance(key_data, dict):
        algorithm = key_data.get('alg', None)

    if not algorithm:
        fail(" JWKError('Unable to find an algorithm for key: %s' % key_data)")

    key_class = get_key(algorithm)
    if not key_class:
        fail(" JWKError('Unable to find an algorithm for key: %s' % key_data)")
    return key_class(key_data, algorithm)

