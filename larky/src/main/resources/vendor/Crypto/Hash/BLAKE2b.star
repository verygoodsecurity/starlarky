load("@stdlib//larky", larky="larky")
load("@stdlib//builtins","builtins")
load("@stdlib//types", types="types")
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//Crypto/Util/py3compat", bord="bord", tobytes="tobytes", tostr="tostr")
load("@vendor//Crypto/Random", get_random_bytes="get_random_bytes")

block_size = 64

def _BLAKE2b_Hash(data, key, digest_bytes, update_after_digest):
    """
    A BLAKE2b hash object.
        Do not instantiate directly. Use the :func:`new` function.

        :ivar oid: ASN.1 Object ID
        :vartype oid: string

        :ivar block_size: the size in bytes of the internal message block,
                          input to the compression function
        :vartype block_size: integer

        :ivar digest_size: the size in bytes of the resulting hash
        :vartype digest_size: integer
    
    """
    self = larky.mutablestruct()

    def update(data):
        """
        Continue hashing of a message by consuming the next chunk of data.

                Args:
                    data (bytes/bytearray/memoryview): The next chunk of the message being hashed.
        
        """
        if not data:
           fail("TypeError: object supporting the buffer API required")

        if not types.is_bytelike(data):
            fail("TypeError: data must be byte-like object")

        if self._digest_done and not self._update_after_digest:
            fail("TypeError: You can only call 'digest' or 'hexdigest' on this object")

        self.state.update(data)
        return self
    self.update = update

    def digest():
        """
        Return the **binary** (non-printable) digest of the message that has been hashed so far.

                :return: The hash digest, computed over the data processed so far.
                         Binary form.
                :rtype: byte string
        
        """
        _txt = self.state.digest()
        self._digest_done = True

        return _txt[:self.digest_size]
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

    def verify(mac_tag):
        """
        Verify that a given **binary** MAC (computed by another party)
                is valid.

                Args:
                  mac_tag (bytes/bytearray/memoryview): the expected MAC of the message.

                Raises:
                    ValueError: if the MAC does not match. It means that the message
                        has been tampered with or that the MAC key is incorrect.
        
        """

        secret = get_random_bytes(16)

        mac1 = BLAKE2b.new(digest_bits=160, key=secret, data=mac_tag)
        mac2 = BLAKE2b.new(digest_bits=160, key=secret, data=self.digest())

        if mac1.digest() != mac2.digest():
            fail('ValueError: MAC check failed')
    self.verify = verify

    def hexverify(hex_mac_tag):
        """
        Verify that a given **printable** MAC (computed by another party)
                is valid.

                Args:
                    hex_mac_tag (string): the expected MAC of the message, as a hexadecimal string.

                Raises:
                    ValueError: if the MAC does not match. It means that the message
                        has been tampered with or that the MAC key is incorrect.
        
        """
        self.verify(unhexlify(tobytes(hex_mac_tag)))
    self.hexverify = hexverify

    def new(**kwargs):
        """
        Return a new instance of a BLAKE2b hash object.
                See :func:`new`.
        
        """
        if "digest_bytes" not in kwargs and "digest_bits" not in kwargs:
            kwargs["digest_bytes"] = self.digest_size

        return new(**kwargs)
    self.new = new

    def __init__(data, key, digest_bytes, update_after_digest):
        """
         The size of the resulting hash in bytes.

        """
        # The internal block size of the hash algorithm in bytes.
        self.block_size = block_size

        # The size of the resulting hash in bytes.
        self.digest_size = digest_bytes

        self._update_after_digest = update_after_digest
        self._digest_done = False

        # See https://tools.ietf.org/html/rfc7693
        if digest_bytes in (20, 32, 48, 64) and not key:
            self.oid = "1.3.6.1.4.1.1722.12.2.1." + str(digest_bytes)

        if not types.is_bytelike(key):
            fail("TypeError: key must be byte-like object")

        self.state = _JCrypto.Hash.BLAKE2B(digest_bytes, key)
        if data:
            self.update(data)
        return self
    self = __init__(data, key, digest_bytes, update_after_digest)

    return self


def new(**kwargs):
    """
    Create a new hash object.

        Args:
            data (bytes/bytearray/memoryview):
                Optional. The very first chunk of the message to hash.
                It is equivalent to an early call to :meth:`BLAKE2b_Hash.update`.
            digest_bytes (integer):
                Optional. The size of the digest, in bytes (1 to 64). Default is 64.
            digest_bits (integer):
                Optional and alternative to ``digest_bytes``.
                The size of the digest, in bits (8 to 512, in steps of 8).
                Default is 512.
            key (bytes/bytearray/memoryview):
                Optional. The key to use to compute the MAC (1 to 64 bytes).
                If not specified, no key will be used.
            update_after_digest (boolean):
                Optional. By default, a hash object cannot be updated anymore after
                the digest is computed. When this flag is ``True``, such check
                is no longer enforced.

        Returns:
            A :class:`BLAKE2b_Hash` hash object
    
    """
    data = kwargs.pop("data", None)
    update_after_digest = kwargs.pop("update_after_digest", False)

    digest_bytes = kwargs.pop("digest_bytes", None)
    digest_bits = kwargs.pop("digest_bits", None)

    if None not in (digest_bytes, digest_bits):
        fail("TypeError: Only one digest parameter must be provided")
    if (None, None) == (digest_bytes, digest_bits):
        digest_bytes = 64
    if digest_bytes != None:
        if not ((1 <= digest_bytes) and (digest_bytes <= 64)):
            fail("ValueError: 'digest_bytes' not in range 1..64")
    else:
        if not (8 <= digest_bits) and (digest_bits <= 512) or (digest_bits % 8):
            fail(" ValueError: digest_bytes' not in range 8..512, with steps of 8")
        digest_bytes = digest_bits // 8

    key = kwargs.pop("key", bytes(r"", encoding='utf-8'))
    if len(key) > 64:
        fail("ValueError: BLAKE2b key cannot exceed 64 bytes")

    if kwargs:
        fail("TypeError: Unknown parameters: " + str(kwargs))

    return _BLAKE2b_Hash(data, key, digest_bytes, update_after_digest)


BLAKE2b = larky.struct(
    block_size=block_size,
    new=new,
    __name__ = 'BLAKE2b',
)