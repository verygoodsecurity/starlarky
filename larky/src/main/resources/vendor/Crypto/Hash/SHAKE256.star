load("@stdlib//larky", larky="larky")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//Crypto/Util/py3compat", bord="bord")
load("@stdlib//types", types="types")

def SHAKE256_XOF(data=None):
    """
    A SHAKE256 hash object.
        Do not instantiate directly.
        Use the :func:`new` function.

        :ivar oid: ASN.1 Object ID
        :vartype oid: string
    
    """
    def __init__(data=None):
        _state = _JCrypto.Hash.SHAKE(256)
        _is_squeezing = False
        if data:
            _state.update(data)
        return larky.mutablestruct(
            _state=_state,
            _is_squeezing=_is_squeezing
        )
    self = __init__(data)

    # ASN.1 Object ID
    oid = "2.16.840.1.101.3.4.2.12"

    def update(data):
        """
        Continue hashing of a message by consuming the next chunk of data.

                Args:
                    data (byte string/byte array/memoryview): The next chunk of the message being hashed.
        
        """
        if not types.is_bytelike(data):
            fail('TypeError: data is not byte-like')

        if self._is_squeezing:
            fail('TypeError: You cannot call "update" after the first "read"')

        self._state.update(data)
        return self
    self.update = update

    def read(length):
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
        self._is_squeezing = True
        return self._state.read(length)
    self.read = read

    def new(self, data=None):
        """
        Return a fresh instance of a SHAKE256 object.

            Args:
               data (bytes/bytearray/memoryview):
                The very first chunk of the message to hash.
                It is equivalent to an early call to :meth:`update`.
                Optional.

            :Return: A :class:`SHAKE256_XOF` object
    
        """
        return SHAKE256_XOF(data=data)
    self.new = new
    return self


def new(data=None):
    """Return a fresh instance of a SHAKE128 object.

    Args:
       data (bytes/bytearray/memoryview):
        The very first chunk of the message to hash.
        It is equivalent to an early call to :meth:`update`.
        Optional.

    :Return: A :class:`SHAKE128_XOF` object
    """

    return SHAKE256_XOF(data=data)


SHAKE256 = larky.struct(
    SHAKE256=SHAKE256_XOF,
    new=new,
    __name__ = 'SHAKE256',
)