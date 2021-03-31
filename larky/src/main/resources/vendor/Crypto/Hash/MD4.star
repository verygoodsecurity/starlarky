def MD4Hash(object):
    """
    Class that implements an MD4 hash
    
    """
    def __init__(self, data=None):
        """
        Error %d while instantiating MD4

        """
    def update(self, data):
        """
        Continue hashing of a message by consuming the next chunk of data.

                Repeated calls are equivalent to a single call with the concatenation
                of all the arguments. In other words:

                   >>> m.update(a); m.update(b)

                is equivalent to:

                   >>> m.update(a+b)

                :Parameters:
                  data : byte string/byte array/memoryview
                    The next chunk of the message being hashed.
        
        """
    def digest(self):
        """
        Return the **binary** (non-printable) digest of the message that
                has been hashed so far.

                This method does not change the state of the hash object.
                You can continue updating the object after calling this function.

                :Return: A byte string of `digest_size` bytes. It may contain non-ASCII
                 characters, including null bytes.
        
        """
    def hexdigest(self):
        """
        Return the **printable** digest of the message that has been
                hashed so far.

                This method does not change the state of the hash object.

                :Return: A string of 2* `digest_size` characters. It contains only
                 hexadecimal ASCII digits.
        
        """
    def copy(self):
        """
        Return a copy ("clone") of the hash object.

                The copy will have the same internal state as the original hash
                object.
                This can be used to efficiently compute the digests of strings that
                share a common initial substring.

                :Return: A hash object of the same type
        
        """
    def new(self, data=None):
        """
        Return a fresh instance of the hash object.

            :Parameters:
               data : byte string/byte array/memoryview
                The very first chunk of the message to hash.
                It is equivalent to an early call to `MD4Hash.update()`.
                Optional.

            :Return: A `MD4Hash` object
    
        """
