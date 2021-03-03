def HMAC(object):
    """
    An HMAC hash object.
        Do not instantiate directly. Use the :func:`new` function.

        :ivar digest_size: the size in bytes of the resulting MAC tag
        :vartype digest_size: integer
    
    """
    def __init__(self, key, msg=b"", digestmod=None):
        """
        b
        """
    def update(self, msg):
        """
        Authenticate the next chunk of message.

                Args:
                    data (byte string/byte array/memoryview): The next chunk of data
        
        """
    def _pbkdf2_hmac_assist(self, first_digest, iterations):
        """
        Carry out the expensive inner loop for PBKDF2-HMAC
        """
    def copy(self):
        """
        Return a copy ("clone") of the HMAC object.

                The copy will have the same internal state as the original HMAC
                object.
                This can be used to efficiently compute the MAC tag of byte
                strings that share a common initial substring.

                :return: An :class:`HMAC`
        
        """
    def digest(self):
        """
        Return the **binary** (non-printable) MAC tag of the message
                authenticated so far.

                :return: The MAC tag digest, computed over the data processed so far.
                         Binary form.
                :rtype: byte string
        
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
    def hexdigest(self):
        """
        Return the **printable** MAC tag of the message authenticated so far.

                :return: The MAC tag, computed over the data processed so far.
                         Hexadecimal encoded.
                :rtype: string
        
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
def new(key, msg=b"", digestmod=None):
    """
    Create a new MAC object.

        Args:
            key (bytes/bytearray/memoryview):
                key for the MAC object.
                It must be long enough to match the expected security level of the
                MAC.
            msg (bytes/bytearray/memoryview):
                Optional. The very first chunk of the message to authenticate.
                It is equivalent to an early call to :meth:`HMAC.update`.
            digestmod (module):
                The hash to use to implement the HMAC.
                Default is :mod:`Crypto.Hash.MD5`.

        Returns:
            An :class:`HMAC` object
    
    """
