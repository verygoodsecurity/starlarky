def Keccak_Hash(object):
    """
    A Keccak hash object.
        Do not instantiate directly.
        Use the :func:`new` function.

        :ivar digest_size: the size in bytes of the resulting hash
        :vartype digest_size: integer
    
    """
    def __init__(self, data, digest_bytes, update_after_digest):
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
    def new(self, **kwargs):
        """
        Create a fresh Keccak hash object.
        """
def new(**kwargs):
    """
    Create a new hash object.

        Args:
            data (bytes/bytearray/memoryview):
                The very first chunk of the message to hash.
                It is equivalent to an early call to :meth:`Keccak_Hash.update`.
            digest_bytes (integer):
                The size of the digest, in bytes (28, 32, 48, 64).
            digest_bits (integer):
                The size of the digest, in bits (224, 256, 384, 512).
            update_after_digest (boolean):
                Whether :meth:`Keccak.digest` can be followed by another
                :meth:`Keccak.update` (default: ``False``).

        :Return: A :class:`Keccak_Hash` hash object
    
    """
