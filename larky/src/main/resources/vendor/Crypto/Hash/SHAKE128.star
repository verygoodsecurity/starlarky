def SHAKE128_XOF(object):
    """
    A SHAKE128 hash object.
        Do not instantiate directly.
        Use the :func:`new` function.

        :ivar oid: ASN.1 Object ID
        :vartype oid: string
    
    """
    def __init__(self, data=None):
        """
        Error %d while instantiating SHAKE128

        """
    def update(self, data):
        """
        Continue hashing of a message by consuming the next chunk of data.

                Args:
                    data (byte string/byte array/memoryview): The next chunk of the message being hashed.
        
        """
    def read(self, length):
        """

                Compute the next piece of XOF output.

                .. note::
                    You cannot use :meth:`update` anymore after the first call to
                    :meth:`read`.

                Args:
                    length (integer): the amount of bytes this method must return

                :return: the next piece of XOF output (of the given length)
                :rtype: byte string
        
        """
    def new(self, data=None):
        """
        Return a fresh instance of a SHAKE128 object.

            Args:
               data (bytes/bytearray/memoryview):
                The very first chunk of the message to hash.
                It is equivalent to an early call to :meth:`update`.
                Optional.

            :Return: A :class:`SHAKE128_XOF` object
    
        """
