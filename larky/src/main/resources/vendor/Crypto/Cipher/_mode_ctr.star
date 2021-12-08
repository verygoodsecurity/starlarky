# -*- coding: utf-8 -*-
#
#  Cipher/mode_ctr.py : CTR mode
#
# ===================================================================
# The contents of this file are dedicated to the public domain.  To
# the extent that dedication to the public domain is not available,
# everyone is granted a worldwide, perpetual, royalty-free,
# non-exclusive license to exercise all rights associated with the
# contents of this file for any purpose whatsoever.
# No rights are reserved.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
# BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
# ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
# ===================================================================

"""
Counter (CTR) mode.
"""
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//builtins","builtins")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@stdlib//larky", larky="larky")
load("@stdlib//struct", struct="struct")
load("@stdlib//types", types="types")
load("@vendor//Crypto/Random", get_random_bytes="get_random_bytes")
load("@vendor//Crypto/Util/py3compat", _copy_bytes="copy_bytes", is_native_int="is_native_int")
load("@vendor//Crypto/Util/number", long_to_bytes="long_to_bytes")

__all__ = ['CtrMode']

_WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION


def _CtrMode(block_cipher, initial_counter_block,
             prefix_len, counter_len, little_endian):
    """*CounTeR (CTR)* mode.

    This mode is very similar to ECB, in that
    encryption of one block is done independently of all other blocks.

    Unlike ECB, the block *position* contributes to the encryption
    and no information leaks about symbol frequency.

    Each message block is associated to a *counter* which
    must be unique across all messages that get encrypted
    with the same key (not just within the same message).
    The counter is as big as the block size.

    Counters can be generated in several ways. The most
    straightword one is to choose an *initial counter block*
    (which can be made public, similarly to the *IV* for the
    other modes) and increment its lowest **m** bits by one
    (modulo *2^m*) for each block. In most cases, **m** is
    chosen to be half the block size.

    See `NIST SP800-38A`_, Section 6.5 (for the mode) and
    Appendix B (for how to manage the *initial counter block*).

    .. _`NIST SP800-38A` : http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf

    :undocumented: __init__
    """
    self = larky.mutablestruct(__class__='CtrMode')

    def encrypt(plaintext, output=None):
        """Encrypt data with the key and the parameters set at initialization.

        A cipher object is stateful: once you have encrypted a message
        you cannot encrypt (or decrypt) another message using the same
        object.

        The data to encrypt can be broken up in two or
        more pieces and `encrypt` can be called multiple times.

        That is, the statement:

            >>> c.encrypt(a) + c.encrypt(b)

        is equivalent to:

             >>> c.encrypt(a+b)

        This function does not add any padding to the plaintext.

        :Parameters:
          plaintext : bytes/bytearray/memoryview
            The piece of data to encrypt.
            It can be of any length.
        :Keywords:
          output : bytearray/memoryview
            The location where the ciphertext must be written to.
            If ``None``, the ciphertext is returned.
        :Return:
          If ``output`` is ``None``, the ciphertext is returned as ``bytes``.
          Otherwise, ``None``.
        """

        if self.encrypt not in self._next:
            fail("TypeError: encrypt() cannot be called after decrypt()")
        self._next = [ self.encrypt ]

        if output == None:
            ciphertext = bytearray()
        else:
            ciphertext = output

            if not types.is_bytearray(output):
                fail("TypeError: output must be a bytearray or " +
                # we do not have memoryview in larky so this is just for
                # compat reasons
                     "a writeable memoryview")

            # noinspection PyTypeChecker
            if len(plaintext) != len(output):
                fail("ValueError: output must have the same length as the " +
                     "input (%d bytes)" % len(plaintext))

        result = self._state.encrypt(plaintext, ciphertext)
        if result:
            if result == 0x60002:
                fail('OverflowError: The counter has wrapped around in CTR mode')
            fail('ValueError: Error %X while decrypting in CTR mode' % result)

        if output != None:
            return

        return ciphertext
    self.encrypt = encrypt

    def decrypt(ciphertext, output=None):
        """Decrypt data with the key and the parameters set at initialization.

        A cipher object is stateful: once you have decrypted a message
        you cannot decrypt (or encrypt) another message with the same
        object.

        The data to decrypt can be broken up in two or
        more pieces and `decrypt` can be called multiple times.

        That is, the statement:

            >>> c.decrypt(a) + c.decrypt(b)

        is equivalent to:

             >>> c.decrypt(a+b)

        This function does not remove any padding from the plaintext.

        :Parameters:
          ciphertext : bytes/bytearray/memoryview
            The piece of data to decrypt.
            It can be of any length.
        :Keywords:
          output : bytearray/memoryview
            The location where the plaintext must be written to.
            If ``None``, the plaintext is returned.
        :Return:
          If ``output`` is ``None``, the plaintext is returned as ``bytes``.
          Otherwise, ``None``.
        """

        if self.decrypt not in self._next:
            fail("TypeError: decrypt() cannot be called after encrypt()")
        self._next = [ self.decrypt ]

        if output == None:
            plaintext = bytearray()
        else:
            plaintext = output

            if not types.is_bytearray(output):
                fail("TypeError: output must be a bytearray or " +
                # we do not have memoryview in larky so this is just for
                # compat reasons
                     "a writeable memoryview")

            # noinspection PyTypeChecker
            if len(ciphertext) != len(output):
                fail("ValueError: output must have the same length as the " +
                     "input (%d bytes)" % len(ciphertext))

        result = self._state.decrypt(ciphertext, plaintext)

        if result:
            if result == 0x60002:
                fail('OverflowError: The counter has wrapped around in CTR mode')
            fail('ValueError: Error %X while decrypting in CTR mode' % result)

        if output != None:
            return

        return plaintext
    self.decrypt = decrypt

    def __init__(block_cipher, initial_counter_block,
                 prefix_len, counter_len, little_endian):
        """Create a new block cipher, configured in CTR mode.

        :Parameters:
          block_cipher : C pointer
            A smart pointer to the low-level block cipher instance.

          initial_counter_block : bytes/bytearray/memoryview
            The initial plaintext to use to generate the key stream.

            It is as large as the cipher block, and it embeds
            the initial value of the counter.

            This value must not be reused.
            It shall contain a nonce or a random component.
            Reusing the *initial counter block* for encryptions
            performed with the same key compromises confidentiality.

          prefix_len : integer
            The amount of bytes at the beginning of the counter block
            that never change.

          counter_len : integer
            The length in bytes of the counter embedded in the counter
            block.

          little_endian : boolean
            True if the counter in the counter block is an integer encoded
            in little endian mode. If False, it is big endian.
        """

        if len(initial_counter_block) == prefix_len + counter_len:
            self.nonce = _copy_bytes(None, prefix_len, initial_counter_block)
            """Nonce; not available if there is a fixed suffix"""

        self._state = _JCrypto.Cipher.CTRMode(block_cipher, initial_counter_block)

        self.block_size = len(initial_counter_block)
        """The block size of the underlying cipher, in bytes."""

        self._next = [self.encrypt, self.decrypt]
        return self
    self = __init__(block_cipher, initial_counter_block, prefix_len, counter_len, little_endian)
    return self


def _create_ctr_cipher(factory, **kwargs):
    """Instantiate a cipher object that performs CTR encryption/decryption.

    :Parameters:
      factory : module
        The underlying block cipher, a module from ``Crypto.Cipher``.

    :Keywords:
      nonce : bytes/bytearray/memoryview
        The fixed part at the beginning of the counter block - the rest is
        the counter number that gets increased when processing the next block.
        The nonce must be such that no two messages are encrypted under the
        same key and the same nonce.

        The nonce must be shorter than the block size (it can have
        zero length; the counter is then as long as the block).

        If this parameter is not present, a random nonce will be created with
        length equal to half the block size. No random nonce shorter than
        64 bits will be created though - you must really think through all
        security consequences of using such a short block size.

      initial_value : posive integer or bytes/bytearray/memoryview
        The initial value for the counter. If not present, the cipher will
        start counting from 0. The value is incremented by one for each block.
        The counter number is encoded in big endian mode.

      counter : object
        Instance of ``Crypto.Util.Counter``, which allows full customization
        of the counter block. This parameter is incompatible to both ``nonce``
        and ``initial_value``.

    Any other keyword will be passed to the underlying block cipher.
    See the relevant documentation for details (at least ``key`` will need
    to be present).
    """

    cipher_state = factory._create_base_cipher(kwargs)

    counter = kwargs.pop("counter", None)
    nonce = kwargs.pop("nonce", None)
    initial_value = kwargs.pop("initial_value", None)
    if kwargs:
        fail('TypeError: Invalid parameters for CTR mode: %s' % str(kwargs))

    if counter != None and (nonce, initial_value) != (None, None):
        fail('TypeError: "counter" and ("nonce", "initial_value") are mutually exclusive')

    if counter == None:
        # Crypto.Util.Counter is not used
        if nonce == None:
            if factory.block_size < 16:
                fail('TypeError: Impossible to create a safe nonce for short block sizes')
            nonce = get_random_bytes(factory.block_size // 2)
        else:
            if len(nonce) >= factory.block_size:
                fail("ValueError: Nonce is too long")

        # What is not nonce is counter
        counter_len = factory.block_size - len(nonce)

        if initial_value == None:
            initial_value = 0

        if is_native_int(initial_value):
            if ((1 << (counter_len * 8)) - 1) < initial_value:
                fail("ValueError: Initial counter value is too large")
            initial_counter_block = bytearray(nonce) + bytearray(long_to_bytes(initial_value, counter_len))
        else:
            if len(initial_value) != counter_len:
                fail(("ValueError: Incorrect length for counter byte " +
                     "string (%d bytes, expected %d)") %
                     (len(initial_value), counter_len))
            initial_counter_block = bytearray(nonce) + bytearray(initial_value)

        return _CtrMode(cipher_state,
                       initial_counter_block,
                       len(nonce),                     # prefix
                       counter_len,
                       False)                          # little_endian

    # Crypto.Util.Counter is used

    # 'counter' used to be a callable object, but now it is
    # just a dictionary for backward compatibility.
    _counter = dict(counter)
    counter_len = _counter.pop("counter_len")
    prefix = _counter.pop("prefix")
    suffix = _counter.pop("suffix")
    initial_value = _counter.pop("initial_value")
    little_endian = _counter.pop("little_endian")
    # fail("TypeError: Incorrect counter object (use Crypto.Util.Counter.new)")

    # Compute initial counter block
    words = []
    for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
        if initial_value <= 0:
            break
        words.append(struct.pack('B', initial_value & 255))
        initial_value >>= 8
    words += [bytes([0x00])] * max(0, counter_len - len(words))
    if not little_endian:
        words.reverse()
    initial_counter_block = prefix + bytearray(r"", encoding='utf-8').join(words) + suffix

    if len(initial_counter_block) != factory.block_size:
        fail(("ValueError: 'Size of the counter block (%d bytes) must match" +
              "block size (%d)") % (len(initial_counter_block),
                                   factory.block_size))

    return _CtrMode(
        cipher_state,
        initial_counter_block,
        len(prefix),
        counter_len,
        little_endian
    )


CtrMode = larky.struct(
    _create_ctr_cipher=_create_ctr_cipher,
)
