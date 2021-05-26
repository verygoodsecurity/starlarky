try:
    load("@vendor//jose/backends/cryptography_backend", get_random_bytes="get_random_bytes")  # noqa: F401
except ImportError:
    try:
        load("@vendor//jose/backends/pycrypto_backend", get_random_bytes="get_random_bytes")  # noqa: F401
    except ImportError:
        load("@vendor//jose/backends/native", get_random_bytes="get_random_bytes")  # noqa: F401

try:
    load("@vendor//jose/backends/cryptography_backend", RSAKey="RSAKey")  # noqa: F401
except ImportError:
    try:
        load("@vendor//jose/backends/pycrypto_backend", RSAKey="RSAKey")  # noqa: F401

        # time.clock was deprecated in python 3.3 in favor of time.perf_counter
        # and removed in python 3.8. pycrypto was never updated for this. If
        # time has no clock attribute, let it use perf_counter instead to work
        # in 3.8+
        # noinspection PyUnresolvedReferences
        load("@stdlib//time", time="time")
        if not hasattr(time, "clock"):
            time.clock = time.perf_counter

    except ImportError:
        load("@vendor//jose/backends/rsa_backend", RSAKey="RSAKey")  # noqa: F401

try:
    load("@vendor//jose/backends/cryptography_backend", ECKey="ECKey")  # noqa: F401
except ImportError:
    load("@vendor//jose/backends/ecdsa_backend", ECKey="ECKey")  # noqa: F401

try:
    load("@vendor//jose/backends/cryptography_backend", AESKey="AESKey")  # noqa: F401
except ImportError:
    try:
        load("@vendor//jose/backends/pycrypto_backend", AESKey="AESKey")  # noqa: F401
    except ImportError:
        AESKey = None

try:
    load("@vendor//jose/backends/cryptography_backend", HMACKey="HMACKey")  # noqa: F401
except ImportError:
    load("@vendor//jose/backends/native", HMACKey="HMACKey")  # noqa: F401

load("@vendor//base", DIRKey="DIRKey")  # noqa: F401

