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
     )

# try:
#     load("@vendor//jose/backends", ECKey="ECKey")  # noqa: F401
# except ImportError:
#     pass


def get_key(algorithm):
    if algorithm in ALGORITHMS.KEYS:
        return ALGORITHMS.KEYS[algorithm]
    elif operator.contains(ALGORITHMS.HMAC, algorithm):  # noqa: F811
        return HMACKey
    elif operator.contains(ALGORITHMS.RSA, algorithm):
        return RSAKey
    elif operator.contains(ALGORITHMS.EC, algorithm):
        fail("ECKey is not supported!")
        # return ECKey
    elif operator.contains(ALGORITHMS.AES, algorithm):
        return AESKey
    elif algorithm == ALGORITHMS.DIR:
        return DIRKey
    return None


def register_key(algorithm, key_class):
    # if not issubclass(key_class, Key):
    #     fail(" TypeError(\"Key class is not a subclass of jwk.Key\")")
    ALGORITHMS.KEYS[algorithm] = key_class
    ALGORITHMS.SUPPORTED.add(algorithm)
    return True


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
    construct=construct,
    register_key=register_key,
    get_key=get_key,
)
