def Poly1305_MAC(object):
    """
    An Poly1305 MAC object.
        Do not instantiate directly. Use the :func:`new` function.

        :ivar digest_size: the size in bytes of the resulting MAC tag
        :vartype digest_size: integer
    
    """
    def __init__(self, r, s, data):
        """
        Parameter r is not 16 bytes long
        """
    def update(self, data):
        """
        Authenticate the next chunk of message.

                Args:
                    data (byte string/byte array/memoryview): The next chunk of data
        
        """
    def copy(self):
        """
        Return the **binary** (non-printable) MAC tag of the message
                authenticated so far.

                :return: The MAC tag digest, computed over the data processed so far.
                         Binary form.
                :rtype: byte string
        
        """
    def hexdigest(self):
        """
        Return the **printable** MAC tag of the message authenticated so far.

                :return: The MAC tag, computed over the data processed so far.
                         Hexadecimal encoded.
                :rtype: string
        
        """
    def verify(self, mac_tag):
        """
        Verify that a given **binary** MAC (computed by another party)
                is valid.

                Args:
                  mac_tag (byte string/byte string/memoryview): the expected MAC of the message.

                Raises:
                    ValueError: if the MAC does not match. It means that the message
                        has been tampered with or that the MAC key is incorrect.
        
        """
    def hexverify(self, hex_mac_tag):
        """
        Verify that a given **printable** MAC (computed by another party)
                is valid.

                Args:
                    hex_mac_tag (string): the expected MAC of the message,
                        as a hexadecimal string.

                Raises:
                    ValueError: if the MAC does not match. It means that the message
                        has been tampered with or that the MAC key is incorrect.
        
        """
def new(**kwargs):
    """
    Create a new Poly1305 MAC object.

        Args:
            key (bytes/bytearray/memoryview):
                The 32-byte key for the Poly1305 object.
            cipher (module from ``Crypto.Cipher``):
                The cipher algorithm to use for deriving the Poly1305
                key pair *(r, s)*.
                It can only be ``Crypto.Cipher.AES`` or ``Crypto.Cipher.ChaCha20``.
            nonce (bytes/bytearray/memoryview):
                Optional. The non-repeatable value to use for the MAC of this message.
                It must be 16 bytes long for ``AES`` and 8 or 12 bytes for ``ChaCha20``.
                If not passed, a random nonce is created; you will find it in the
                ``nonce`` attribute of the new object.
            data (bytes/bytearray/memoryview):
                Optional. The very first chunk of the message to authenticate.
                It is equivalent to an early call to ``update()``.

        Returns:
            A :class:`Poly1305_MAC` object
    
    """
