def _HChaCha20(key, nonce):
    """
    Error %d when deriving subkey with HChaCha20
    """
def ChaCha20Cipher(object):
    """
    ChaCha20 (or XChaCha20) cipher object.
        Do not create it directly. Use :py:func:`new` instead.

        :var nonce: The nonce with length 8, 12 or 24 bytes
        :vartype nonce: bytes
    
    """
    def __init__(self, key, nonce):
        """
        Initialize a ChaCha20/XChaCha20 cipher object

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
    def _encrypt(self, plaintext, output):
        """
        Encrypt without FSM checks
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
    def seek(self, position):
        """
        Seek to a certain position in the key stream.

                Args:
                  position (integer):
                    The absolute position within the key stream, in bytes.
        
        """
def _derive_Poly1305_key_pair(key, nonce):
    """
    Derive a tuple (r, s, nonce) for a Poly1305 MAC.

        If nonce is ``None``, a new 12-byte nonce is generated.
    
    """
def new(**kwargs):
    """
    Create a new ChaCha20 or XChaCha20 cipher

        Keyword Args:
            key (bytes/bytearray/memoryview): The secret key to use.
                It must be 32 bytes long.
            nonce (bytes/bytearray/memoryview): A mandatory value that
                must never be reused for any other encryption
                done with this key.

                For ChaCha20, it must be 8 or 12 bytes long.

                For XChaCha20, it must be 24 bytes long.

                If not provided, 8 bytes will be randomly generated
                (you can find them back in the ``nonce`` attribute).

        :Return: a :class:`Crypto.Cipher.ChaCha20.ChaCha20Cipher` object
    
    """
