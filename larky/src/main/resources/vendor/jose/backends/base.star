# Ported from python-jose jose/backends/base.py; consistent with its master branch between
# 89c3d41bc6c60ad6f7d135f14dcd963cef633637 and 8572088b76f7c0d31068f38aeca0d4273ae8bc8f
# (no release; the version was not recorded, see jose/__init__.star).

load("@vendor//jose/utils", base64url_encode="base64url_encode")
load("@vendor//option/result", Error="Error")
load("@vendor//six", six="six")
load("@stdlib//larky", larky="larky")

def Key(key, algorithm):
    """
    A simple interface for implementing JWK keys.
    """
    self = larky.mutablestruct(__class__='Key')
    def __init__(key, algorithm):
        pass
        return self
    self = __init__(key, algorithm)

    def sign(msg):
        return Error()
    self.sign = sign

    def verify(msg, sig):
        return Error()
    self.verify = verify

    def public_key():
        return Error()
    self.public_key = public_key

    def to_pem():
        return Error()
    self.to_pem = to_pem

    def to_dict():
        return Error()
    self.to_dict = to_dict

    def encrypt(plain_text, aad=None):
        """
        Encrypt the plain text and generate an auth tag if appropriate

        Args:
            plain_text (bytes): Data to encrypt
            aad (bytes, optional): Authenticated Additional Data if key's algorithm supports auth mode

        Returns:
            (bytes, bytes, bytes): IV, cipher text, and auth tag
        """
        return Error()
    self.encrypt = encrypt

    def decrypt(cipher_text, iv=None, aad=None, tag=None):
        """
        Decrypt the cipher text and validate the auth tag if present
        Args:
            cipher_text (bytes): Cipher text to decrypt
            iv (bytes): IV if block mode
            aad (bytes): Additional Authenticated Data to verify if auth mode
            tag (bytes): Authentication tag if auth mode

        Returns:
            bytes: Decrypted value
        """
        return Error()
    self.decrypt = decrypt

    def wrap_key(key_data):
        """
        Wrap the the plain text key data

        Args:
            key_data (bytes): Key data to wrap

        Returns:
            bytes: Wrapped key
        """
        return Error()
    self.wrap_key = wrap_key

    def unwrap_key(wrapped_key):
        """
        Unwrap the the wrapped key data

        Args:
            wrapped_key (bytes): Wrapped key data to unwrap

        Returns:
            bytes: Unwrapped key
        """
        return Error()
    self.unwrap_key = unwrap_key
    return self


def DIRKey(key_data, algorithm):

    self = larky.mutablestruct(__class__='DIRKey')
    def __init__(key_data, algorithm):
        self._key = six.ensure_binary(key_data)
        self._alg = algorithm
        return self
    self = __init__(key_data, algorithm)

    def to_dict():
        return {
            'alg': self._alg,
            'kty': 'oct',
            'k': base64url_encode(self._key),
        }
    self.to_dict = to_dict
    return self

