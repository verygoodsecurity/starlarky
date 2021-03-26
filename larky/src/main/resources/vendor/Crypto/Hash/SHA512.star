def SHA512Hash(object):
    """
    A SHA-512 hash object (possibly in its truncated version SHA-512/224 or
        SHA-512/256.
        Do not instantiate directly. Use the :func:`new` function.

        :ivar oid: ASN.1 Object ID
        :vartype oid: string

        :ivar block_size: the size in bytes of the internal message block,
                          input to the compression function
        :vartype block_size: integer

        :ivar digest_size: the size in bytes of the resulting hash
        :vartype digest_size: integer
    
    """
    def __init__(self, data, truncate):
        """
        2.16.840.1.101.3.4.2.3
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
    def copy(self):
        """
        Return a copy ("clone") of the hash object.

                The copy will have the same internal state as the original hash
                object.
                This can be used to efficiently compute the digests of strings that
                share a common initial substring.

                :return: A hash object of the same type
        
        """
    def new(self, data=None):
        """
        Create a fresh SHA-512 hash object.
        """
def new(data=None, truncate=None):
    """
    Create a new hash object.

        Args:
          data (bytes/bytearray/memoryview):
            Optional. The very first chunk of the message to hash.
            It is equivalent to an early call to :meth:`SHA512Hash.update`.
          truncate (string):
            Optional. The desired length of the digest. It can be either "224" or
            "256". If not present, the digest is 512 bits long.
            Passing this parameter is **not** equivalent to simply truncating
            the output digest.

        :Return: A :class:`SHA512Hash` hash object
    
    """
def _pbkdf2_hmac_assist(inner, outer, first_digest, iterations):
    """
    Compute the expensive inner loop in PBKDF-HMAC.
    """
