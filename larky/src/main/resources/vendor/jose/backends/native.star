load("@stdlib//hashlib", hashlib="hashlib")
load("@stdlib//hmac", hmac="hmac")
load("@stdlib//os", os="os")

load("@vendor//six", six="six")

load("@vendor//jose/backends/base", Key="Key")
load("@vendor//jose/constants", ALGORITHMS="ALGORITHMS")
load("@vendor//jose/exceptions", JWKError="JWKError")
load("@vendor//jose/utils", base64url_decode="base64url_decode", base64url_encode="base64url_encode")
load("@stdlib//builtins","builtins")


def get_random_bytes(num_bytes):
    return bytes(os.urandom(num_bytes))


def HMACKey(key, algorithm):
    """
    Performs signing and verification operations using HMAC
    and the specified hash function.
    """
    HASHES = {
        ALGORITHMS.HS256: hashlib.sha256,
        ALGORITHMS.HS384: hashlib.sha384,
        ALGORITHMS.HS512: hashlib.sha512
    }
    self = larky.mutablestruct(__class__='HMACKey')

    def __init__(key, algorithm):
        if algorithm not in ALGORITHMS.HMAC:
            fail(" JWKError('hash_alg: %s is not a valid hash algorithm' % algorithm)")
        return self
    self = __init__(key, algorithm)

    def _process_jwk(jwk_dict):
        if not jwk_dict.get('kty') == 'oct':
            fail(" JWKError(\"Incorrect key type. Expected: 'oct', Received: %s\" % jwk_dict.get('kty'))")

        k = jwk_dict.get('k')
        k = k.encode('utf-8')
        k = bytes(k)
        k = base64url_decode(k)

        return k
    self._process_jwk = _process_jwk

    def sign(msg):
        return hmac.new(self.prepared_key, msg, self._hash_alg).digest()
    self.sign = sign

    def verify(msg, sig):
        return hmac.compare_digest(sig, self.sign(msg))
    self.verify = verify

    def to_dict():
        return {
            'alg': self._algorithm,
            'kty': 'oct',
            'k': base64url_encode(self.prepared_key).decode('ASCII'),
        }
    self.to_dict = to_dict
    return self

