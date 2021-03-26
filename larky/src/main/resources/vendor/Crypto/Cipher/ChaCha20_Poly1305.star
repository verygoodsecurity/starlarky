def _enum(**enums):
    """
    'Enum'
    """
def ChaCha20Poly1305Cipher(object):
    """
    ChaCha20-Poly1305 and XChaCha20-Poly1305 cipher object.
        Do not create it directly. Use :py:func:`new` instead.

        :var nonce: The nonce with length 8, 12 or 24 bytes
        :vartype nonce: byte string
    
    """
    def __init__(self, key, nonce):
        """
        Initialize a ChaCha20-Poly1305 AEAD cipher object

                See also `new()` at the module level.
        """
    def update(self, data):
        """
        Protect the associated data.

                Associated data (also known as *additional authenticated data* - AAD)
                is the piece of the message that must stay in the clear, while
                still allowing the receiver to verify its integrity.
                An example is packet headers.

                The associated data (possibly split into multiple segments) is
                fed into :meth:`update` before any call to :meth:`decrypt` or :meth:`encrypt`.
                If there is no associated data, :meth:`update` is not called.

                :param bytes/bytearray/memoryview assoc_data:
                    A piece of associated data. There are no restrictions on its size.
        
        """
    def _pad_aad(self):
        """
        b'\x00'
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
    def _compute_mac(self):
        """
        Finalize the cipher (if not done already) and return the MAC.
        """
    def digest(self):
        """
        Compute the *binary* authentication tag (MAC).

                :Return: the MAC tag, as 16 ``bytes``.
        
        """
    def hexdigest(self):
        """
        Compute the *printable* authentication tag (MAC).

                This method is like :meth:`digest`.

                :Return: the MAC tag, as a hexadecimal string.
        
        """
    def verify(self, received_mac_tag):
        """
        Validate the *binary* authentication tag (MAC).

                The receiver invokes this method at the very end, to
                check if the associated data (if any) and the decrypted
                messages are valid.

                :param bytes/bytearray/memoryview received_mac_tag:
                    This is the 16-byte *binary* MAC, as received from the sender.
                :Raises ValueError:
                    if the MAC does not match. The message has been tampered with
                    or the key is incorrect.
        
        """
    def hexverify(self, hex_mac_tag):
        """
        Validate the *printable* authentication tag (MAC).

                This method is like :meth:`verify`.

                :param string hex_mac_tag:
                    This is the *printable* MAC.
                :Raises ValueError:
                    if the MAC does not match. The message has been tampered with
                    or the key is incorrect.
        
        """
    def encrypt_and_digest(self, plaintext):
        """
        Perform :meth:`encrypt` and :meth:`digest` in one step.

                :param plaintext: The data to encrypt, of any size.
                :type plaintext: bytes/bytearray/memoryview
                :return: a tuple with two ``bytes`` objects:

                    - the ciphertext, of equal length as the plaintext
                    - the 16-byte MAC tag
        
        """
    def decrypt_and_verify(self, ciphertext, received_mac_tag):
        """
        Perform :meth:`decrypt` and :meth:`verify` in one step.

                :param ciphertext: The piece of data to decrypt.
                :type ciphertext: bytes/bytearray/memoryview
                :param bytes received_mac_tag:
                    This is the 16-byte *binary* MAC, as received from the sender.
                :return: the decrypted data (as ``bytes``)
                :raises ValueError:
                    if the MAC does not match. The message has been tampered with
                    or the key is incorrect.
        
        """
def new(**kwargs):
    """
    Create a new ChaCha20-Poly1305 or XChaCha20-Poly1305 AEAD cipher.

        :keyword key: The secret key to use. It must be 32 bytes long.
        :type key: byte string

        :keyword nonce:
            A value that must never be reused for any other encryption
            done with this key.

            For ChaCha20-Poly1305, it must be 8 or 12 bytes long.

            For XChaCha20-Poly1305, it must be 24 bytes long.

            If not provided, 12 ``bytes`` will be generated randomly
            (you can find them back in the ``nonce`` attribute).
        :type nonce: bytes, bytearray, memoryview

        :Return: a :class:`Crypto.Cipher.ChaCha20.ChaCha20Poly1305Cipher` object
    
    """
