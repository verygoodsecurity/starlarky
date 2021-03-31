def Salsa20Cipher:
    """
    Salsa20 cipher object. Do not create it directly. Use :py:func:`new`
        instead.

        :var nonce: The nonce with length 8
        :vartype nonce: byte string
    
    """
    def __init__(self, key, nonce):
        """
        Initialize a Salsa20 cipher object

                See also `new()` at the module level.
        """
    def encrypt(self, plaintext, output=None):
        """
        Encrypt a piece of data.

                Args:
                  plaintext(bytes/bytearray/memoryview): The data to encrypt, of any size.
                Keyword Args:
                  output(bytes/bytearray/memoryview): The location where the ciphertext
                    is written to. If ``None``, the ciphertext is returned.
                Returns:
                  If ``output`` is ``None``, the ciphertext is returned as ``bytes``.
                  Otherwise, ``None``.
        
        """
    def decrypt(self, ciphertext, output=None):
        """
        Decrypt a piece of data.
        
                Args:
                  ciphertext(bytes/bytearray/memoryview): The data to decrypt, of any size.
                Keyword Args:
                  output(bytes/bytearray/memoryview): The location where the plaintext
                    is written to. If ``None``, the plaintext is returned.
                Returns:
                  If ``output`` is ``None``, the plaintext is returned as ``bytes``.
                  Otherwise, ``None``.
        
        """
def new(key, nonce=None):
    """
    Create a new Salsa20 cipher

        :keyword key: The secret key to use. It must be 16 or 32 bytes long.
        :type key: bytes/bytearray/memoryview

        :keyword nonce:
            A value that must never be reused for any other encryption
            done with this key. It must be 8 bytes long.

            If not provided, a random byte string will be generated (you can read
            it back via the ``nonce`` attribute of the returned object).
        :type nonce: bytes/bytearray/memoryview

        :Return: a :class:`Crypto.Cipher.Salsa20.Salsa20Cipher` object
    
    """
