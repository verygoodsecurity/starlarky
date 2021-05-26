load("@vendor//six", six="six")

load("@vendor//utils", base64url_encode="base64url_encode")
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
        fail(" NotImplementedError()")
    self.sign = sign

    def verify(msg, sig):
        fail(" NotImplementedError()")
    self.verify = verify

    def public_key():
        fail(" NotImplementedError()")
    self.public_key = public_key

    def to_pem():
        fail(" NotImplementedError()")
    self.to_pem = to_pem

    def to_dict():
        fail(" NotImplementedError()")
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
        fail(" NotImplementedError()")
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
        fail(" NotImplementedError()")
    self.decrypt = decrypt

    def wrap_key(key_data):
        """
        Wrap the the plain text key data

        Args:
            key_data (bytes): Key data to wrap

        Returns:
            bytes: Wrapped key
        """
        fail(" NotImplementedError()")
    self.wrap_key = wrap_key

    def unwrap_key(wrapped_key):
        """
        Unwrap the the wrapped key data

        Args:
            wrapped_key (bytes): Wrapped key data to unwrap

        Returns:
            bytes: Unwrapped key
        """
        fail(" NotImplementedError()")
    self.unwrap_key = unwrap_key
    return self
def DIRKey(key_data, algorithm):
    self = larky.mutablestruct(__class__='DIRKey')
    def __init__(key_data, algorithm):
        self._key = six.ensure_binary(key_data)
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

