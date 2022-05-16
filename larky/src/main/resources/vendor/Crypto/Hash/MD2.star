load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")

# The size of the resulting hash in bytes.
digest_size = 16

# The internal block size of the hash algorithm in bytes.
block_size = 16


def MD2Hash(data=None):
    """
    An MD2 hash object.
        Do not instantiate directly. Use the :func:`new` function.

        :ivar oid: ASN.1 Object ID
        :vartype oid: string

        :ivar block_size: the size in bytes of the internal message block,
                          input to the compression function
        :vartype block_size: integer

        :ivar digest_size: the size in bytes of the resulting hash
        :vartype digest_size: integer

    """
    def __init__(data=None):
        """
        Error %d while instantiating MD2

        """
        _state = _JCrypto.Hash.MD2()
        if data:
            _state.update(data)
        return larky.mutablestruct(__class__="MD2Hash", _state=_state)

    self = __init__(data)

    # The size of the resulting hash in bytes.
    self.digest_size = 16
    # The internal block size of the hash algorithm in bytes.
    self.block_size = 16
    # ASN.1 Object ID
    self.oid = "1.2.840.113549.2.2"

    def update(data):
        """
        Continue hashing of a message by consuming the next chunk of data.

                Args:
                    data (byte string/byte array/memoryview): The next chunk of the message being hashed.

        """
        if not types.is_bytelike(data):
            fail("TypeError: object supporting the buffer API required")
        self._state.update(data)

    self.update = update

    def digest():
        """
        Return the **binary** (non-printable) digest of the message that has been hashed so far.

                :return: The hash digest, computed over the data processed so far.
                         Binary form.
                :rtype: byte string

        """
        return self._state.digest()
    self.digest = digest

    def hexdigest():
        """
        Return the **printable** digest of the message that has been hashed so far.

                :return: The hash digest, computed over the data processed so far.
                         Hexadecimal encoded.
                :rtype: string

        """
        return tostr(hexlify(self.digest()))
    self.hexdigest = hexdigest

    def copy():
        """
        Return a copy ("clone") of the hash object.

                The copy will have the same internal state as the original hash
                object.
                This can be used to efficiently compute the digests of strings that
                share a common initial substring.

                :return: A hash object of the same type

        """
        h = MD2Hash()
        h._state = self._state.copy()
        return h

    self.copy = copy

    def new(data=None):
        """
        Create a new hash object.

            :parameter data:
                Optional. The very first chunk of the message to hash.
                It is equivalent to an early call to :meth:`MD2Hash.update`.
            :type data: bytes/bytearray/memoryview

            :Return: A :class:`MD2Hash` hash object

        """
        return MD2Hash(data)
    self.new = new
    return self

def new(data=None):
    """Create a new hash object.

    :parameter data:
        Optional. The very first chunk of the message to hash.
        It is equivalent to an early call to :meth:`MD2Hash.update`.
    :type data: byte string/byte array/memoryview

    :Return: A :class:`MD2Hash` hash object
    """
    return MD2Hash().new(data)

MD2 = larky.struct(
    digest_size=digest_size,
    block_size=block_size,
    new=new,
    __name__ = 'MD2',
)
