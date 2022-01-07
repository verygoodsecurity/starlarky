load("@stdlib//binascii", hexlify="hexlify")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//larky", larky="larky")
load("@stdlib//jcrypto", _JCrypto="jcrypto")

# The size of the resulting hash in bytes.
digest_size = 28

# ASN.1 Object ID
oid = "2.16.840.1.101.3.4.2.7"

# Input block size for HMAC
block_size = 144

def SHA3_224_Hash(data=None, update_after_digest=False):
    """
    A SHA3-224 hash object.
        Do not instantiate directly.
        Use the :func:`new` function.

        :ivar oid: ASN.1 Object ID
        :vartype oid: string

        :ivar digest_size: the size in bytes of the resulting hash
        :vartype digest_size: integer
    
    """
    def __init__(data, update_after_digest):
        """
        Error %d while instantiating SHA-3/224

        """
        self_ = {'_update_after_digest': update_after_digest, '_digest_done': False}
        self_['_state'] = _JCrypto.Hash.SHA3_224()
        if data:
            self_['_state'].update(data)
        return larky.mutablestruct(**self_)

    self = __init__(data, update_after_digest)

    self.digest_size = digest_size
    self.block_size = block_size
    self.oid = oid

    def update(data):
        """
        Continue hashing of a message by consuming the next chunk of data.

                Args:
                    data (byte string/byte array/memoryview): The next chunk of the message being hashed.
        
        """
        if self._digest_done and not self._update_after_digest:
            fail('TypeError("You can only call \'digest\' or \'hexdigest\' on this object")')

        if data == None:
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
        self._digest_done = True
        return self._state.digest()
    self.digest = digest

    def hexdigest():
        """
        Return the **printable** digest of the message that has been hashed so far.

                :return: The hash digest, computed over the data processed so far.
                         Hexadecimal encoded.
                :rtype: string
        
        """
        return codecs.decode(hexlify(self.digest()), encoding='utf-8')
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
        h = SHA3_224_Hash()
        h._state = self._state.copy()
        return h
    self.copy = copy

    def new(data, update_after_digest):
        """
        Create a fresh SHA3-224 hash object.
        """
        return SHA3_224_Hash(data, update_after_digest)
    self.new = new
    
    return self

def new(*args, **kwargs):
    """
    Create a new hash object.

        Args:
            data (byte string/byte array/memoryview):
                The very first chunk of the message to hash.
                It is equivalent to an early call to :meth:`update`.
            update_after_digest (boolean):
                Whether :meth:`digest` can be followed by another :meth:`update`
                (default: ``False``).

        :Return: A :class:`SHA3_224_Hash` hash object
    
    """
    data = kwargs.pop("data", None)
    update_after_digest = kwargs.pop("update_after_digest", False)
    if len(args) == 1:
        if data:
            fail('ValueError("Initial data for hash specified twice")')
        data = args[0]

    if kwargs:
        fail('TypeError("Unknown parameters: ' + str(kwargs) + '")')

    return SHA3_224_Hash().new(data, update_after_digest)

SHA3_224 = larky.struct(
    digest_size=digest_size,
    block_size=block_size,
    new=new,
    __name__ = 'SHA3_224',
)