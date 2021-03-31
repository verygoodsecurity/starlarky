def BLAKE2s_Hash(object):
    """
    A BLAKE2s hash object.
        Do not instantiate directly. Use the :func:`new` function.

        :ivar oid: ASN.1 Object ID
        :vartype oid: string

        :ivar block_size: the size in bytes of the internal message block,
                          input to the compression function
        :vartype block_size: integer

        :ivar digest_size: the size in bytes of the resulting hash
        :vartype digest_size: integer
    
    """
    def __init__(self, data, key, digest_bytes, update_after_digest):
        """
         The size of the resulting hash in bytes.

        """
    def update(self, data):
        """
        Continue hashing of a message by consuming the next chunk of data.

                Args:
                    data (byte string/byte array/memoryview): The next chunk of the message being hashed.
        
        """
    def digest(self):
        """
        Return the **binary** (non-printable) digest of the message that has been hashed so far.

                :return: The hash digest, computed over the data processed so far.
                         Binary form.
                :rtype: byte string
        
        """
    def hexdigest(self):
        """
        Return the **printable** digest of the message that has been hashed so far.

                :return: The hash digest, computed over the data processed so far.
                         Hexadecimal encoded.
                :rtype: string
        
        """
    def verify(self, mac_tag):
        """
        Verify that a given **binary** MAC (computed by another party)
                is valid.

                Args:
                  mac_tag (byte string/byte array/memoryview): the expected MAC of the message.

                Raises:
                    ValueError: if the MAC does not match. It means that the message
                        has been tampered with or that the MAC key is incorrect.
        
        """
    def hexverify(self, hex_mac_tag):
        """
        Verify that a given **printable** MAC (computed by another party)
                is valid.

                Args:
                    hex_mac_tag (string): the expected MAC of the message, as a hexadecimal string.

                Raises:
                    ValueError: if the MAC does not match. It means that the message
                        has been tampered with or that the MAC key is incorrect.
        
        """
    def new(self, **kwargs):
        """
        Return a new instance of a BLAKE2s hash object.
                See :func:`new`.
        
        """
def new(**kwargs):
    """
    Create a new hash object.

        Args:
            data (byte string/byte array/memoryview):
                Optional. The very first chunk of the message to hash.
                It is equivalent to an early call to :meth:`BLAKE2s_Hash.update`.
            digest_bytes (integer):
                Optional. The size of the digest, in bytes (1 to 32). Default is 32.
            digest_bits (integer):
                Optional and alternative to ``digest_bytes``.
                The size of the digest, in bits (8 to 256, in steps of 8).
                Default is 256.
            key (byte string):
                Optional. The key to use to compute the MAC (1 to 64 bytes).
                If not specified, no key will be used.
            update_after_digest (boolean):
                Optional. By default, a hash object cannot be updated anymore after
                the digest is computed. When this flag is ``True``, such check
                is no longer enforced.

        Returns:
            A :class:`BLAKE2s_Hash` hash object
    
    """
