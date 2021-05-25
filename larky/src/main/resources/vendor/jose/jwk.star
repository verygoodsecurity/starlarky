load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")

load("@vendor//Crypto/Cipher/AES", AES="AES")


def get_key(algorithm):
    if algorithm in ["A256GCM", "A256GCMKW"]:
        return AES
    # elif algorithm in ALGORITHMS.HMAC:  # noqa: F811
    #     return HMACKey
    # elif algorithm in ALGORITHMS.RSA:
    #     from jose.backends import RSAKey  # noqa: F811
    #     return RSAKey
    # elif algorithm in ALGORITHMS.EC:
    #     from jose.backends import ECKey  # noqa: F811
    #     return ECKey
    # elif algorithm in ALGORITHMS.AES:
    #     from jose.backends import AESKey  # noqa: F811
    #     return AESKey
    # elif algorithm == ALGORITHMS.DIR:
    #     from jose.backends import DIRKey  # noqa: F811
    #     return DIRKey
    return None


def construct(key_data, algorithm=None):
    """
    Construct a Key object for the given algorithm with the given
    key_data.
    """

    # Allow for pulling the algorithm off of the passed in jwk.
    if not algorithm and types.is_dict(key_data):
        algorithm = key_data.get('alg', None)

    if not algorithm:
        fail('Unable to find an algorithm for key: {}}'.format(key_data))

    key_class = get_key(algorithm)

    if not key_class:
        fail('Unable to find an algorithm for key: {}'.format(key_data))
    return key_class.new(key_data, AES.MODE_GCM)


jwk = larky.struct(
    get_key=get_key,
    construct=construct
)
